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
    private final String modelHost;
    private final int modelPort;
    private final int modelConnectTimeoutMillis;
    private final int modelReconnectDelayMillis;
    private final int modelDecisionRateHz;
    private final int actionMaximumAgeMillis;
    private final int actionMaximumSequenceLag;
    private final boolean actuatorAllowSingleplayer;
    private final String actuatorAllowedServers;
    private final boolean physicalInputTakeover;

    private SawBotConfig(boolean hudEnabled, int timingWindowSize, int sensorIntervalTicks,
                         int telemetryQueueCapacity, int telemetryInputWindowCapacity,
                         int telemetryCompressionLevel, String modelHost, int modelPort,
                         int modelConnectTimeoutMillis, int modelReconnectDelayMillis,
                         int modelDecisionRateHz, int actionMaximumAgeMillis,
                         int actionMaximumSequenceLag, boolean actuatorAllowSingleplayer,
                         String actuatorAllowedServers, boolean physicalInputTakeover) {
        this.hudEnabled = hudEnabled;
        this.timingWindowSize = timingWindowSize;
        this.sensorIntervalTicks = sensorIntervalTicks;
        this.telemetryQueueCapacity = telemetryQueueCapacity;
        this.telemetryInputWindowCapacity = telemetryInputWindowCapacity;
        this.telemetryCompressionLevel = telemetryCompressionLevel;
        this.modelHost = modelHost;
        this.modelPort = modelPort;
        this.modelConnectTimeoutMillis = modelConnectTimeoutMillis;
        this.modelReconnectDelayMillis = modelReconnectDelayMillis;
        this.modelDecisionRateHz = modelDecisionRateHz;
        this.actionMaximumAgeMillis = actionMaximumAgeMillis;
        this.actionMaximumSequenceLag = actionMaximumSequenceLag;
        this.actuatorAllowSingleplayer = actuatorAllowSingleplayer;
        this.actuatorAllowedServers = actuatorAllowedServers;
        this.physicalInputTakeover = physicalInputTakeover;
    }

    public static SawBotConfig load(File file, Logger logger) {
        Configuration configuration = new Configuration(file);
        try {
            configuration.load();
            boolean hudEnabled = configuration.getBoolean("hudEnabled", "runtime", true,
                "Show the compact SawBotV1 engineering HUD.");
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
            String host = configuration.getString("host", "modelBridge", "127.0.0.1",
                "Loopback host for the local model process.");
            int port = configuration.getInt("port", "modelBridge", 25189, 1024, 65535,
                "TCP port for sawbot.bridge/0.1.");
            int connectTimeout = configuration.getInt("connectTimeoutMillis", "modelBridge", 500, 50, 5000,
                "Background socket connection timeout; never blocks the client thread.");
            int reconnectDelay = configuration.getInt("reconnectDelayMillis", "modelBridge", 1000, 100, 10000,
                "Delay between local model reconnect attempts.");
            int decisionRate = configuration.getInt("decisionRateHz", "modelBridge", 10, 1, 20,
                "Maximum observation publication rate expected by the local model.");
            int actionAge = configuration.getInt("maximumActionAgeMillis", "actuator", 250, 50, 2000,
                "Reject model actions older than this local receive-time deadline.");
            int sequenceLag = configuration.getInt("maximumObservationSequenceLag", "actuator", 3, 0, 20,
                "Reject actions generated from observations farther behind than this.");
            boolean allowSingleplayer = configuration.getBoolean("allowSingleplayer", "actuator", true,
                "Allow autonomous input in integrated single-player worlds.");
            String allowedServers = configuration.getString("allowedPrivateServers", "actuator",
                "127.0.0.1,localhost", "Comma-separated exact private/LAN hosts. Public servers are blocked by default.");
            boolean physicalTakeover = configuration.getBoolean("physicalInputTakeover", "actuator", true,
                "Any physical movement/mouse/click input immediately returns control to the human.");
            return new SawBotConfig(hudEnabled, window, interval, telemetryQueue, inputWindow,
                compression, host, port, connectTimeout, reconnectDelay, decisionRate, actionAge,
                sequenceLag, allowSingleplayer, allowedServers, physicalTakeover);
        } catch (RuntimeException exception) {
            logger.error("Failed to load SawBotV1 configuration; using safe defaults.", exception);
            return defaults();
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
    }

    private static SawBotConfig defaults() {
        return new SawBotConfig(true, 256, 2, 64, 32, 1, "127.0.0.1", 25189,
            500, 1000, 10, 250, 3, true, "127.0.0.1,localhost", true);
    }

    public boolean hudEnabled() { return hudEnabled; }
    public int timingWindowSize() { return timingWindowSize; }
    public int sensorIntervalTicks() { return sensorIntervalTicks; }
    public int telemetryQueueCapacity() { return telemetryQueueCapacity; }
    public int telemetryInputWindowCapacity() { return telemetryInputWindowCapacity; }
    public int telemetryCompressionLevel() { return telemetryCompressionLevel; }
    public String modelHost() { return modelHost; }
    public int modelPort() { return modelPort; }
    public int modelConnectTimeoutMillis() { return modelConnectTimeoutMillis; }
    public int modelReconnectDelayMillis() { return modelReconnectDelayMillis; }
    public int modelDecisionRateHz() { return modelDecisionRateHz; }
    public int actionMaximumAgeMillis() { return actionMaximumAgeMillis; }
    public int actionMaximumSequenceLag() { return actionMaximumSequenceLag; }
    public boolean actuatorAllowSingleplayer() { return actuatorAllowSingleplayer; }
    public String actuatorAllowedServers() { return actuatorAllowedServers; }
    public boolean physicalInputTakeover() { return physicalInputTakeover; }
}
