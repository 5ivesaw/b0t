package dev.fivesaw.sawbot.forge.inspection;

import dev.fivesaw.sawbot.common.observation.ObservationDiff;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

/** Bounded asynchronous one-snapshot exporter. The worker receives immutable data only. */
public final class SnapshotExportService implements AutoCloseable {
    private static final int MAX_PENDING_EXPORTS = 4;
    private final Path exportDirectory;
    private final Logger logger;
    private final ArrayBlockingQueue<Request> queue = new ArrayBlockingQueue<Request>(MAX_PENDING_EXPORTS);
    private final Thread worker;
    private final Thread shutdownHook;
    private volatile boolean closing;
    private volatile String status = "idle";
    private volatile String latestFile = "";
    private volatile long rejectedCount;

    public SnapshotExportService(File minecraftDirectory, Logger logger) {
        if (minecraftDirectory == null || logger == null) throw new IllegalArgumentException("directory/logger");
        this.exportDirectory = new File(minecraftDirectory, "sawbotv1/exports").toPath();
        this.logger = logger;
        this.worker = new Thread(new Worker(), "SawBotV1-SnapshotExporter");
        this.worker.setDaemon(true);
        this.worker.start();
        this.shutdownHook = new Thread(new Runnable() {
            @Override public void run() { close(); }
        }, "SawBotV1-SnapshotExporter-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public boolean request(ObservationSnapshot snapshot, ObservationDiff diff,
                           BlockInspection block, int selectedTrackingId) {
        if (snapshot == null || closing) {
            status = closing ? "closed" : "no snapshot";
            return false;
        }
        Request request = new Request(snapshot, diff == null ? ObservationDiff.EMPTY : diff,
            block, selectedTrackingId);
        boolean accepted = queue.offer(request);
        if (!accepted) {
            rejectedCount++;
            status = "queue full; export rejected";
        } else {
            status = "queued #" + snapshot.sequenceNumber();
        }
        return accepted;
    }

    public int queueSize() { return queue.size(); }
    public int queueCapacity() { return MAX_PENDING_EXPORTS; }
    public String status() { return status; }
    public String latestFile() { return latestFile; }
    public long rejectedCount() { return rejectedCount; }

    @Override public void close() {
        if (closing) return;
        closing = true;
        worker.interrupt();
        try {
            worker.join(1500L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        status = "closed";
        try {
            if (Thread.currentThread() != shutdownHook) Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown already began; the hook is executing exactly as intended.
        }
    }

    private final class Worker implements Runnable {
        @Override public void run() {
            while (!closing || !queue.isEmpty()) {
                try {
                    Request request = queue.poll(250L, TimeUnit.MILLISECONDS);
                    if (request != null) export(request);
                } catch (InterruptedException interrupted) {
                    if (closing && queue.isEmpty()) break;
                } catch (RuntimeException exception) {
                    status = "export runtime failure";
                    logger.error("SawBotV1 snapshot export failed.", exception);
                }
            }
        }
    }

    private void export(Request request) {
        try {
            Files.createDirectories(exportDirectory);
            String world = sanitize(request.snapshot.worldIdentifier());
            String fileName = "snapshot-" + world + "-seq-" + request.snapshot.sequenceNumber() + ".json";
            Path target = exportDirectory.resolve(fileName);
            Path temporary = exportDirectory.resolve(fileName + ".tmp");
            status = "writing #" + request.snapshot.sequenceNumber();
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                SnapshotJsonWriter.write(request.snapshot, request.diff, request.block,
                    request.selectedTrackingId, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            latestFile = target.toAbsolutePath().toString();
            status = "saved #" + request.snapshot.sequenceNumber();
            logger.info("SawBotV1 exported snapshot to {}.", latestFile);
        } catch (IOException exception) {
            status = "I/O failure";
            logger.error("SawBotV1 snapshot export I/O failure.", exception);
        }
    }

    private static String sanitize(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 48 ? cleaned.substring(0, 48) : cleaned;
    }

    private static final class Request {
        final ObservationSnapshot snapshot;
        final ObservationDiff diff;
        final BlockInspection block;
        final int selectedTrackingId;
        Request(ObservationSnapshot snapshot, ObservationDiff diff, BlockInspection block,
                int selectedTrackingId) {
            this.snapshot = snapshot;
            this.diff = diff;
            this.block = block;
            this.selectedTrackingId = selectedTrackingId;
        }
    }
}
