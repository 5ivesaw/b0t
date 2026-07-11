package dev.fivesaw.sawbot.forge.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

public final class SawBotConfig {
    private final boolean hudEnabled;
    private final int timingWindowSize;
    private final int sensorIntervalTicks;

    private SawBotConfig(boolean hudEnabled, int timingWindowSize, int sensorIntervalTicks) {
        this.hudEnabled = hudEnabled;
        this.timingWindowSize = timingWindowSize;
        this.sensorIntervalTicks = sensorIntervalTicks;
    }

    public static SawBotConfig load(File file, Logger logger) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            boolean hudEnabled = configuration.getBoolean("hudEnabled", "phase1", true,
                "Show the compact SawBotV1 sensor and safety HUD.");
            int window = configuration.getInt("timingWindowSize", "phase1", 256, 32, 1024,
                "Bounded number of client-handler timing samples.");
            int interval = configuration.getInt("sensorIntervalTicks", "phase1", 2, 1, 20,
                "Client ticks between immutable observation snapshots; 2 equals 10 Hz.");
            return new SawBotConfig(hudEnabled, window, interval);
        } catch (RuntimeException exception) {
            logger.error("Failed to load SawBotV1 configuration; using safe defaults.", exception);
            return new SawBotConfig(true, 256, 2);
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
    }

    public boolean hudEnabled() { return hudEnabled; }
    public int timingWindowSize() { return timingWindowSize; }
    public int sensorIntervalTicks() { return sensorIntervalTicks; }
}
