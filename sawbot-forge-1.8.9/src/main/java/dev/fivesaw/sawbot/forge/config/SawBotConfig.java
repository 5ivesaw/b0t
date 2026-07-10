package dev.fivesaw.sawbot.forge.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.Logger;

public final class SawBotConfig {
    private final boolean hudEnabled;
    private final int timingWindowSize;

    private SawBotConfig(boolean hudEnabled, int timingWindowSize) {
        this.hudEnabled = hudEnabled;
        this.timingWindowSize = timingWindowSize;
    }

    public static SawBotConfig load(File file, Logger logger) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            boolean hudEnabled = configuration.getBoolean("hudEnabled", "phase0", true,
                "Show the compact SawBotV1 foundation HUD.");
            int window = configuration.getInt("timingWindowSize", "phase0", 256, 32, 1024,
                "Bounded number of Phase 0 timing samples.");
            return new SawBotConfig(hudEnabled, window);
        } catch (RuntimeException exception) {
            logger.error("Failed to load SawBotV1 configuration; using safe defaults.", exception);
            return new SawBotConfig(true, 256);
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
    }

    public boolean hudEnabled() { return hudEnabled; }
    public int timingWindowSize() { return timingWindowSize; }
}
