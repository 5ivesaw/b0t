package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.telemetry.TrajectoryStep;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import org.apache.logging.log4j.Logger;

/** One bounded asynchronous trajectory-writing session. */
final class TelemetrySession {
    private final Path partialPath;
    private final Path finalPath;
    private final ObservationSnapshot initialSnapshot;
    private final int compressionLevel;
    private final Logger logger;
    private final ArrayBlockingQueue<TrajectoryStep> queue;
    private final Thread worker;
    private volatile boolean closeRequested;
    private volatile boolean closed;
    private volatile boolean failed;
    private volatile String failureMessage = "";
    private volatile String terminalReason = "user stop";
    private volatile long writtenSteps;
    private volatile long droppedSteps;
    private volatile long encodingRejectedSteps;
    private volatile long lastWriteNanos;

    TelemetrySession(Path partialPath, Path finalPath, ObservationSnapshot initialSnapshot,
                     int queueCapacity, int compressionLevel, Logger logger) {
        if (partialPath == null || finalPath == null || initialSnapshot == null || logger == null) {
            throw new IllegalArgumentException("session component");
        }
        if (queueCapacity < 8 || queueCapacity > 256) throw new IllegalArgumentException("queueCapacity");
        this.partialPath = partialPath;
        this.finalPath = finalPath;
        this.initialSnapshot = initialSnapshot;
        this.compressionLevel = compressionLevel;
        this.logger = logger;
        this.queue = new ArrayBlockingQueue<TrajectoryStep>(queueCapacity);
        this.worker = new Thread(new Worker(), "SawBotV1-TelemetryWriter");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    boolean offer(TrajectoryStep step) {
        if (step == null || closeRequested || failed) return false;
        boolean accepted = queue.offer(step);
        if (!accepted) droppedSteps++;
        return accepted;
    }

    void requestClose(String reason) {
        terminalReason = reason == null || reason.isEmpty() ? "unknown" : reason;
        closeRequested = true;
        worker.interrupt();
    }

    void closeAndWait(String reason, long timeoutMillis) {
        requestClose(reason);
        try {
            worker.join(Math.max(0L, timeoutMillis));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    int queueSize() { return queue.size(); }
    int queueCapacity() { return queue.remainingCapacity() + queue.size(); }
    boolean isClosing() { return closeRequested; }
    boolean isClosed() { return closed; }
    boolean isFailed() { return failed; }
    String failureMessage() { return failureMessage; }
    long writtenSteps() { return writtenSteps; }
    long droppedSteps() { return droppedSteps; }
    long encodingRejectedSteps() { return encodingRejectedSteps; }
    long lastWriteNanos() { return lastWriteNanos; }
    Path finalPath() { return finalPath; }
    Path partialPath() { return partialPath; }

    private final class Worker implements Runnable {
        @Override public void run() {
            CRC32 rolling = new CRC32();
            long ordinal = 0L;
            try {
                Files.createDirectories(partialPath.getParent());
                try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(partialPath), 65536)) {
                    TelemetryFileFormat.writeHeader(output, System.currentTimeMillis(), initialSnapshot);
                    int consecutiveEncodingFailures = 0;
                    while (!closeRequested || !queue.isEmpty()) {
                        TrajectoryStep step;
                        try {
                            step = queue.poll(250L, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException interrupted) {
                            if (closeRequested && queue.isEmpty()) break;
                            continue;
                        }
                        if (step == null) continue;
                        byte[] payload;
                        try {
                            payload = TelemetryBinaryCodec.encodeStep(step);
                            consecutiveEncodingFailures = 0;
                        } catch (RuntimeException encodingFailure) {
                            encodingRejectedSteps++;
                            droppedSteps++;
                            consecutiveEncodingFailures++;
                            logger.error("SawBotV1 telemetry rejected observation #"
                                + step.observation().sequenceNumber() + "; capture continues.", encodingFailure);
                            if (consecutiveEncodingFailures >= 4) {
                                throw new IllegalStateException("four consecutive telemetry encoding failures", encodingFailure);
                            }
                            continue;
                        }
                        TelemetryFileFormat.EncodedRecord record = TelemetryFileFormat.encodeRecord(
                            TelemetryFileFormat.RECORD_STEP, ordinal++,
                            step.observation().sequenceNumber(), payload, compressionLevel);
                        TelemetryFileFormat.writeRecord(output, record);
                        rolling.update(payload, 0, payload.length);
                        writtenSteps++;
                        lastWriteNanos = System.nanoTime();
                    }

                    byte[] footerPayload = TelemetryBinaryCodec.encodeFooter(
                        writtenSteps, droppedSteps, (int) rolling.getValue(), terminalReason);
                    TelemetryFileFormat.EncodedRecord footer = TelemetryFileFormat.encodeRecord(
                        TelemetryFileFormat.RECORD_FOOTER, ordinal, -1L, footerPayload, compressionLevel);
                    TelemetryFileFormat.writeRecord(output, footer);
                    output.flush();
                }
                try {
                    Files.move(partialPath, finalPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(partialPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.info("SawBotV1 telemetry session saved to "
                    + finalPath.toAbsolutePath().toString() + " with " + writtenSteps + " steps.");
            } catch (IOException exception) {
                fail("I/O failure", exception);
            } catch (RuntimeException exception) {
                fail("runtime failure", exception);
            } finally {
                closed = true;
            }
        }

        private void fail(String message, Throwable throwable) {
            failed = true;
            String detail = throwable.getMessage();
            failureMessage = message + ": " + (detail == null || detail.isEmpty()
                ? throwable.getClass().getSimpleName() : detail);
            logger.error("SawBotV1 telemetry writer failed; partial file retained at "
                + partialPath.toAbsolutePath().toString(), throwable);
        }
    }
}
