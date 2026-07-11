package dev.fivesaw.sawbot.forge.model;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import dev.fivesaw.sawbot.forge.telemetry.TelemetryBinaryCodec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

/** Non-blocking client-thread facade over a reconnecting loopback model transport. */
public final class ModelBridge implements AutoCloseable {
    private static final int OBSERVATION_QUEUE_CAPACITY = 2;
    private static final int ACTION_QUEUE_CAPACITY = 8;
    private static final int SENT_TIMESTAMP_CAPACITY = 32;

    private final String host;
    private final int port;
    private final int connectTimeoutMillis;
    private final int reconnectDelayMillis;
    private final String modVersion;
    private final int decisionRateHz;
    private final Logger logger;
    private final ArrayBlockingQueue<ObservationSnapshot> observationQueue =
        new ArrayBlockingQueue<ObservationSnapshot>(OBSERVATION_QUEUE_CAPACITY);
    private final ArrayBlockingQueue<ModelActionEnvelope> actionQueue =
        new ArrayBlockingQueue<ModelActionEnvelope>(ACTION_QUEUE_CAPACITY);
    private final Thread worker;
    private volatile boolean running = true;
    private volatile BridgeConnectionState state = BridgeConnectionState.DISCONNECTED;
    private volatile String modelVersion = "none";
    private volatile String lastError = "";
    private volatile long lastConnectedNanos;
    private volatile long lastActionReceivedNanos;
    private volatile long latestRoundTripNanos;
    private volatile long sentObservations;
    private volatile long receivedActions;
    private volatile long droppedObservations;
    private volatile long droppedActions;
    private volatile long reconnects;
    private volatile long invalidFrames;
    private volatile long stateChangedNanos = System.nanoTime();

    public ModelBridge(String host, int port, int connectTimeoutMillis, int reconnectDelayMillis,
                       String modVersion, int decisionRateHz, Logger logger) {
        if (host == null || host.isEmpty() || port < 1 || port > 65535 || connectTimeoutMillis < 50
            || reconnectDelayMillis < 100 || modVersion == null || modVersion.isEmpty()
            || decisionRateHz < 1 || decisionRateHz > 20 || logger == null) {
            throw new IllegalArgumentException("model bridge configuration");
        }
        this.host = host;
        this.port = port;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.reconnectDelayMillis = reconnectDelayMillis;
        this.modVersion = modVersion;
        this.decisionRateHz = decisionRateHz;
        this.logger = logger;
        this.worker = new Thread(new Worker(), "SawBotV1-ModelBridge");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** Latest-wins bounded publication from the client thread. */
    public void offerObservation(ObservationSnapshot snapshot) {
        if (snapshot == null || !running) return;
        if (observationQueue.offer(snapshot)) return;
        observationQueue.poll();
        if (!observationQueue.offer(snapshot)) {
            droppedObservations++;
        } else {
            droppedObservations++;
        }
    }

    /** Returns the newest action and discards any superseded actions. */
    public ModelActionEnvelope pollLatestAction() {
        ModelActionEnvelope latest = null;
        ModelActionEnvelope next;
        while ((next = actionQueue.poll()) != null) latest = next;
        return latest;
    }

    private void publishAction(ModelActionEnvelope envelope) {
        if (actionQueue.offer(envelope)) return;
        actionQueue.poll();
        if (!actionQueue.offer(envelope)) {
            droppedActions++;
        } else {
            droppedActions++;
        }
    }

    private final class Worker implements Runnable {
        @Override public void run() {
            while (running) {
                Socket socket = null;
                try {
                    setState(BridgeConnectionState.CONNECTING);
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(1000);
                    DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 65536));
                    DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 65536));
                    long nonce = new Random().nextLong();
                    setState(BridgeConnectionState.HANDSHAKING);
                    ModelProtocol.writeFrame(output, ModelProtocol.TYPE_HELLO,
                        ModelProtocol.encodeHello(modVersion, SchemaVersion.OBSERVATION_V0_3.identifier(),
                            SchemaVersion.ACTION_V0_1.identifier(), nonce, decisionRateHz));
                    ModelProtocol.Frame helloFrame = ModelProtocol.readFrame(input);
                    if (helloFrame.type() != ModelProtocol.TYPE_HELLO_ACK) {
                        throw new IOException("expected HELLO_ACK, received type " + helloFrame.type());
                    }
                    ModelProtocol.HelloAck ack = ModelProtocol.decodeHelloAck(helloFrame.payload(), nonce);
                    modelVersion = ack.modelVersion();
                    lastError = "";
                    lastConnectedNanos = System.nanoTime();
                    reconnects++;
                    setState(BridgeConnectionState.READY);
                    socket.setSoTimeout(100);
                    logger.info("SawBotV1 model bridge connected to " + host + ":" + port
                        + " using model " + modelVersion + ".");
                    runConnected(input, output);
                } catch (SocketTimeoutException timeout) {
                    failConnection("timeout: " + timeout.getMessage());
                } catch (EOFException eof) {
                    failConnection("model disconnected");
                } catch (IOException io) {
                    failConnection(io.getMessage());
                } catch (RuntimeException runtime) {
                    failConnection("runtime: " + runtime.getMessage());
                    logger.error("SawBotV1 model bridge worker failed.", runtime);
                } finally {
                    if (socket != null) {
                        try { socket.close(); } catch (IOException ignored) { }
                    }
                }
                if (!running) break;
                sleep(reconnectDelayMillis);
            }
            setState(BridgeConnectionState.STOPPED);
        }

        private void runConnected(DataInputStream input, DataOutputStream output) throws IOException {
            LinkedHashMap<Long, Long> sentAt = new LinkedHashMap<Long, Long>();
            while (running) {
                ObservationSnapshot snapshot = null;
                try {
                    snapshot = observationQueue.poll(50L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException interrupted) {
                    if (!running) return;
                    Thread.currentThread().interrupt();
                    return;
                }
                if (snapshot != null) {
                    byte[] payload = TelemetryBinaryCodec.encodeObservation(snapshot);
                    ModelProtocol.writeFrame(output, ModelProtocol.TYPE_OBSERVATION, payload);
                    sentAt.put(Long.valueOf(snapshot.sequenceNumber()), Long.valueOf(System.nanoTime()));
                    while (sentAt.size() > SENT_TIMESTAMP_CAPACITY) {
                        Long first = sentAt.keySet().iterator().next();
                        sentAt.remove(first);
                    }
                    sentObservations++;
                }

                try {
                    ModelProtocol.Frame frame = ModelProtocol.readFrame(input);
                    if (frame.type() == ModelProtocol.TYPE_ACTION) {
                        long received = System.nanoTime();
                        ActionCommand command = ModelProtocol.decodeAction(frame.payload(), modelVersion, received);
                        Long sent = sentAt.remove(Long.valueOf(command.observationSequenceNumber()));
                        long roundTrip = sent == null ? 0L : Math.max(0L, received - sent.longValue());
                        latestRoundTripNanos = roundTrip;
                        lastActionReceivedNanos = received;
                        receivedActions++;
                        publishAction(new ModelActionEnvelope(command, received, roundTrip));
                    } else if (frame.type() == ModelProtocol.TYPE_PING) {
                        ModelProtocol.writeFrame(output, ModelProtocol.TYPE_PONG,
                            ModelProtocol.encodePong(frame.payload()));
                    } else if (frame.type() == ModelProtocol.TYPE_GOODBYE) {
                        throw new EOFException("model sent GOODBYE");
                    } else if (frame.type() == ModelProtocol.TYPE_ERROR) {
                        throw new IOException("model reported protocol error");
                    } else {
                        invalidFrames++;
                    }
                } catch (SocketTimeoutException timeout) {
                    // No inbound action this cycle. The client thread never waits on this worker.
                }
            }
        }

        private void failConnection(String message) {
            if (!running) return;
            lastError = message == null || message.isEmpty() ? "connection failure" : message;
            setState(BridgeConnectionState.DISCONNECTED);
        }


        private void setState(BridgeConnectionState next) {
            if (next != state) {
                state = next;
                stateChangedNanos = System.nanoTime();
            }
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isReady() { return state == BridgeConnectionState.READY; }
    public BridgeConnectionState state() { return state; }
    public String modelVersion() { return modelVersion; }
    public String lastError() { return lastError; }
    public long lastConnectedNanos() { return lastConnectedNanos; }
    public long lastActionReceivedNanos() { return lastActionReceivedNanos; }
    public long latestRoundTripNanos() { return latestRoundTripNanos; }
    public long sentObservations() { return sentObservations; }
    public long receivedActions() { return receivedActions; }
    public long droppedObservations() { return droppedObservations; }
    public long droppedActions() { return droppedActions; }
    public long reconnects() { return reconnects; }
    public long invalidFrames() { return invalidFrames; }
    public int observationQueueSize() { return observationQueue.size(); }
    public int actionQueueSize() { return actionQueue.size(); }
    /** Stable HUD label that does not flicker CONNECTING/DISCONNECTED every retry. */
    public String displayState() {
        if (state == BridgeConnectionState.READY) return "READY";
        if (state == BridgeConnectionState.HANDSHAKING) return "HANDSHAKE";
        if (state == BridgeConnectionState.STOPPED) return "STOPPED";
        return "OFFLINE";
    }
    public long stateAgeMillis() { return Math.max(0L, (System.nanoTime() - stateChangedNanos) / 1_000_000L); }
    public String endpoint() { return host + ":" + port; }

    @Override public void close() {
        running = false;
        worker.interrupt();
        observationQueue.clear();
        actionQueue.clear();
        try {
            worker.join(1500L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
