package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.telemetry.ActionSource;
import dev.fivesaw.sawbot.common.telemetry.HumanInputWindow;
import dev.fivesaw.sawbot.common.telemetry.TrajectoryStep;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/** Client-thread coordinator for structured trajectory capture. */
public final class TelemetryService implements AutoCloseable {
    private final Path telemetryDirectory;
    private final Logger logger;
    private final HumanInputCapture inputCapture;
    private final HumanInputAccumulator inputAccumulator;
    private final int queueCapacity;
    private final int compressionLevel;
    private final Thread shutdownHook;
    private TelemetrySession session;
    private ObservationSnapshot baseline;
    private boolean requested;
    private String status = "idle";
    private String latestFile = "";
    private long lastWrittenSteps;
    private long lastSessionDroppedSteps;
    private long lastEncodingRejectedSteps;
    private String lastFailureMessage = "";
    private boolean failureLatched;
    private long statusTimestampNanos;
    private long lastObservedSequence = -1L;
    private static final long SAVED_STATUS_LIFETIME_NANOS = 3_000_000_000L;

    public TelemetryService(File minecraftDirectory, net.minecraft.client.Minecraft minecraft,
                            int queueCapacity, int inputWindowCapacity, int compressionLevel,
                            Logger logger) {
        if (minecraftDirectory == null || minecraft == null || logger == null) {
            throw new IllegalArgumentException("telemetry component");
        }
        this.telemetryDirectory = new File(minecraftDirectory, "sawbotv1/telemetry").toPath();
        this.logger = logger;
        this.inputCapture = new HumanInputCapture(minecraft);
        this.inputAccumulator = new HumanInputAccumulator(inputWindowCapacity);
        this.queueCapacity = queueCapacity;
        this.compressionLevel = compressionLevel;
        this.shutdownHook = new Thread(new Runnable() {
            @Override public void run() { close(); }
        }, "SawBotV1-Telemetry-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /** Synchronizes the key-requested state without blocking the client thread. */
    public void synchronizeRequested(boolean shouldRecord, ObservationSnapshot current) {
        refreshStatus();
        requested = shouldRecord;
        if (shouldRecord) {
            if (session == null && current != null) {
                if (failureLatched) prepareRetry();
                start(current);
            }
        } else if (session != null && !session.isClosing()) {
            stop("user stop");
        }
        refreshStatus();
    }

    /** Called on the END client tick after Minecraft has updated MouseHelper deltas. */
    public void captureHumanInput(long clientTick) {
        if (!isRecording()) return;
        dev.fivesaw.sawbot.common.telemetry.HumanInputSample sample = inputCapture.capture(clientTick);
        if (sample != null) inputAccumulator.add(sample);
    }

    /** Called after observation capture; pairs the previous observation with following human inputs. */
    public void onObservation(ObservationSnapshot current) {
        if (!isRecording() || current == null) return;
        if (current.sequenceNumber() == lastObservedSequence) return;
        lastObservedSequence = current.sequenceNumber();
        if (baseline == null) {
            baseline = current;
            inputAccumulator.clear();
            return;
        }
        if (current.sequenceNumber() <= baseline.sequenceNumber()) return;

        HumanInputWindow input = inputAccumulator.drain();
        List<ObservationEvent> outcomeEvents = eventsBetween(
            current, baseline.clientTick(), current.clientTick());
        TrajectoryStep step = new TrajectoryStep(baseline, ActionSource.HUMAN, input,
            current.sequenceNumber(), current.clientTick(), outcomeEvents, false);
        session.offer(step);
        baseline = current;
        refreshStatus();
    }

    public void onWorldUnavailable() {
        if (session != null) stop("world unavailable");
        requested = false;
        baseline = null;
        inputAccumulator.clear();
    }

    private void start(ObservationSnapshot initial) {
        if (session != null) return;
        String safeWorld = sanitize(initial.worldIdentifier());
        String base = "trajectory-" + safeWorld + "-episode-"
            + initial.episodeId().toString() + "-" + System.currentTimeMillis();
        Path partial = telemetryDirectory.resolve(base + ".sbt.partial");
        Path complete = telemetryDirectory.resolve(base + ".sbt");
        session = new TelemetrySession(partial, complete, initial, queueCapacity,
            compressionLevel, logger);
        lastWrittenSteps = 0L;
        lastSessionDroppedSteps = 0L;
        lastEncodingRejectedSteps = 0L;
        lastFailureMessage = "";
        baseline = initial;
        lastObservedSequence = initial.sequenceNumber();
        inputAccumulator.clear();
        setStatus("recording");
        logger.info("SawBotV1 structured telemetry started for observation #{}.",
            Long.valueOf(initial.sequenceNumber()));
    }

    private void stop(String reason) {
        TelemetrySession current = session;
        if (current == null) return;
        if (baseline != null && !inputAccumulator.isEmpty()) {
            HumanInputWindow input = inputAccumulator.drain();
            TrajectoryStep tail = new TrajectoryStep(baseline, ActionSource.HUMAN, input,
                baseline.sequenceNumber(), baseline.clientTick(),
                java.util.Collections.<ObservationEvent>emptyList(), true);
            current.offer(tail);
        }
        current.requestClose(reason);
        latestFile = current.partialPath().toAbsolutePath().toString();
        baseline = null;
        inputAccumulator.clear();
        lastObservedSequence = -1L;
        setStatus("finalizing");
        logger.info("SawBotV1 structured telemetry stop requested: {}.", reason);
    }

    private void refreshStatus() {
        TelemetrySession current = session;
        if (current == null) {
            if (!requested && ("saved".equals(status) || "error".equals(status))
                && System.nanoTime() - statusTimestampNanos > SAVED_STATUS_LIFETIME_NANOS) {
                setStatus("idle");
            }
            return;
        }
        if (current.isFailed() && current.isClosed()) {
            latestFile = current.partialPath().toAbsolutePath().toString();
            lastWrittenSteps = current.writtenSteps();
            lastSessionDroppedSteps = current.droppedSteps();
            lastEncodingRejectedSteps = current.encodingRejectedSteps();
            lastFailureMessage = current.failureMessage();
            failureLatched = true;
            session = null;
            requested = false;
            baseline = null;
            inputAccumulator.clear();
            lastObservedSequence = -1L;
            setStatus("error");
        } else if (current.isClosed()) {
            latestFile = current.finalPath().toAbsolutePath().toString();
            lastWrittenSteps = current.writtenSteps();
            lastSessionDroppedSteps = current.droppedSteps();
            lastEncodingRejectedSteps = current.encodingRejectedSteps();
            session = null;
            setStatus("saved");
        } else if (current.isFailed()) {
            setStatus("error");
        } else if (current.isClosing()) setStatus("finalizing");
        else setStatus("recording");
    }

    private static List<ObservationEvent> eventsBetween(ObservationSnapshot snapshot,
                                                         long afterTick, long throughTick) {
        ArrayList<ObservationEvent> result = new ArrayList<ObservationEvent>();
        for (ObservationEvent event : snapshot.events().events()) {
            if (event.clientTick() > afterTick && event.clientTick() <= throughTick) {
                result.add(event);
                if (result.size() == TrajectoryStep.MAX_OUTCOME_EVENTS) break;
            }
        }
        return result;
    }

    private static String sanitize(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }

    private void setStatus(String value) {
        if (!value.equals(status)) { status = value; statusTimestampNanos = System.nanoTime(); }
    }

    public boolean isRecording() { return session != null && !session.isClosing() && !session.isClosed() && !session.isFailed(); }
    public String status() { refreshStatus(); return status; }
    public int queueSize() { return session == null ? 0 : session.queueSize(); }
    public int queueCapacity() { return queueCapacity; }
    public long writtenSteps() { return session == null ? lastWrittenSteps : session.writtenSteps(); }
    public long droppedSteps() { return session == null ? lastSessionDroppedSteps : session.droppedSteps(); }
    public long encodingRejectedSteps() { return session == null ? lastEncodingRejectedSteps : session.encodingRejectedSteps(); }
    public int bufferedInputSamples() { return inputAccumulator.size(); }
    public int droppedInputSamples() { return inputAccumulator.dropped(); }
    public String latestFile() { return latestFile; }
    public String failureMessage() { return session == null ? lastFailureMessage : session.failureMessage(); }
    public boolean failureLatched() { refreshStatus(); return failureLatched; }

    /** Clears a completed failure so the next K press can start a fresh bounded session. */
    public boolean prepareRetry() {
        refreshStatus();
        if (session != null && !session.isClosed()) return false;
        refreshStatus();
        if (session != null) return false;
        failureLatched = false;
        lastFailureMessage = "";
        requested = false;
        setStatus("idle");
        return true;
    }

    @Override public void close() {
        TelemetrySession current = session;
        if (current != null) {
            if (baseline != null && !inputAccumulator.isEmpty()) {
                HumanInputWindow input = inputAccumulator.drain();
                TrajectoryStep tail = new TrajectoryStep(baseline, ActionSource.HUMAN, input,
                    baseline.sequenceNumber(), baseline.clientTick(),
                    java.util.Collections.<ObservationEvent>emptyList(), true);
                current.offer(tail);
            }
            current.closeAndWait("client shutdown", 2000L);
            latestFile = current.finalPath().toAbsolutePath().toString();
            session = null;
        }
        try {
            if (Thread.currentThread() != shutdownHook) Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // Shutdown already started.
        }
    }
}
