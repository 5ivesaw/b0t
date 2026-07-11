package dev.fivesaw.sawbot.forge.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

public final class SawBotConfig {
    private final boolean hudEnabled;
    private final int timingWindowSize;
    private final int sensorIntervalTicks;
    private final int telemetryQueueCapacity;
    private final int telemetryInputWindowCapacity;
    private final int telemetryCompressionLevel;

    private SawBotConfig(boolean hudEnabled, int timingWindowSize, int sensorIntervalTicks,
                         int telemetryQueueCapacity, int telemetryInputWindowCapacity,
                         int telemetryCompressionLevel) {
        this.hudEnabled = hudEnabled;
        this.timingWindowSize = timingWindowSize;
        this.sensorIntervalTicks = sensorIntervalTicks;
        this.telemetryQueueCapacity = telemetryQueueCapacity;
        this.telemetryInputWindowCapacity = telemetryInputWindowCapacity;
        this.telemetryCompressionLevel = telemetryCompressionLevel;
    }

    public static SawBotConfig load(File file, Logger logger) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            boolean hudEnabled = configuration.getBoolean("hudEnabled", "runtime", true,
                "Show the compact SawBotV1 sensor, telemetry, and safety HUD.");
            int window = configuration.getInt("timingWindowSize", "runtime", 256, 32, 1024,
                "Bounded number of client-handler timing samples.");
            int interval = configuration.getInt("sensorIntervalTicks", "runtime", 2, 1, 20,
                "Client ticks between immutable observation snapshots; 2 equals 10 Hz.");
            int telemetryQueue = configuration.getInt("telemetryQueueCapacity", "telemetry", 64, 8, 256,
                "Maximum immutable trajectory steps waiting for the background writer.");
            int inputWindow = configuration.getInt("telemetryInputWindowCapacity", "telemetry", 32, 2, 64,
                "Maximum exact client-tick input samples associated with one observation.");
            int compression = configuration.getInt("telemetryCompressionLevel", "telemetry", 1, 0, 6,
                "Per-record DEFLATE level; 1 minimizes CPU cost on low-end hardware.");
            return new SawBotConfig(hudEnabled, window, interval, telemetryQueue, inputWindow, compression);
        } catch (RuntimeException exception) {
            logger.error("Failed to load SawBotV1 configuration; using safe defaults.", exception);
            return new SawBotConfig(true, 256, 2, 64, 32, 1);
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
    }

    public boolean hudEnabled() { return hudEnabled; }
    public int timingWindowSize() { return timingWindowSize; }
    public int sensorIntervalTicks() { return sensorIntervalTicks; }
    public int telemetryQueueCapacity() { return telemetryQueueCapacity; }
    public int telemetryInputWindowCapacity() { return telemetryInputWindowCapacity; }
    public int telemetryCompressionLevel() { return telemetryCompressionLevel; }
}
