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
    private final int navigationHorizontalRadius;
    private final int navigationVerticalRadius;
    private final int navigationMaximumExpandedNodes;
    private final int navigationExpansionsPerTick;
    private final int navigationReplanIntervalTicks;
    private final int navigationStuckWindowTicks;
    private final float navigationMaximumTurnDegreesPerTick;
    private final float navigationArrivalRadius;
    private final int navigationLookaheadNodes;
    private final float navigationLookaheadDistance;
    private final int navigationPathValidationNodes;
    private final float navigationOffRouteDistance;
    private final float navigationReactiveProbeDistance;

    private SawBotConfig(boolean hudEnabled, int timingWindowSize, int sensorIntervalTicks,
                         int telemetryQueueCapacity, int telemetryInputWindowCapacity,
                         int telemetryCompressionLevel, String modelHost, int modelPort,
                         int modelConnectTimeoutMillis, int modelReconnectDelayMillis,
                         int modelDecisionRateHz, int actionMaximumAgeMillis,
                         int actionMaximumSequenceLag, boolean actuatorAllowSingleplayer,
                         String actuatorAllowedServers, boolean physicalInputTakeover,
                         int navigationHorizontalRadius, int navigationVerticalRadius,
                         int navigationMaximumExpandedNodes, int navigationExpansionsPerTick,
                         int navigationReplanIntervalTicks, int navigationStuckWindowTicks,
                         float navigationMaximumTurnDegreesPerTick, float navigationArrivalRadius,
                         int navigationLookaheadNodes, float navigationLookaheadDistance,
                         int navigationPathValidationNodes, float navigationOffRouteDistance,
                         float navigationReactiveProbeDistance) {
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
        this.navigationHorizontalRadius = navigationHorizontalRadius;
        this.navigationVerticalRadius = navigationVerticalRadius;
        this.navigationMaximumExpandedNodes = navigationMaximumExpandedNodes;
        this.navigationExpansionsPerTick = navigationExpansionsPerTick;
        this.navigationReplanIntervalTicks = navigationReplanIntervalTicks;
        this.navigationStuckWindowTicks = navigationStuckWindowTicks;
        this.navigationMaximumTurnDegreesPerTick = navigationMaximumTurnDegreesPerTick;
        this.navigationArrivalRadius = navigationArrivalRadius;
        this.navigationLookaheadNodes = navigationLookaheadNodes;
        this.navigationLookaheadDistance = navigationLookaheadDistance;
        this.navigationPathValidationNodes = navigationPathValidationNodes;
        this.navigationOffRouteDistance = navigationOffRouteDistance;
        this.navigationReactiveProbeDistance = navigationReactiveProbeDistance;
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
            int reconnectDelay = configuration.getInt("reconnectDelayMillis", "modelBridge", 3000, 100, 10000,
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
            int navigationRadius = configuration.getInt("horizontalRadius", "navigation", 40, 8, 96,
                "Maximum horizontal anytime-A* search radius around the player.");
            int navigationVertical = configuration.getInt("verticalRadius", "navigation", 10, 2, 20,
                "Maximum vertical route-search range around the player.");
            int navigationNodes = configuration.getInt("maximumExpandedNodes", "navigation", 6144, 256, 32768,
                "Hard node-expansion cap for one current-position route search.");
            int navigationBudget = configuration.getInt("expansionsPerTick", "navigation", 96, 8, 384,
                "Client-thread anytime-A* expansions permitted per tick.");
            int navigationReplan = configuration.getInt("replanIntervalTicks", "navigation", 4, 2, 40,
                "Ticks between rolling current-position route refreshes while the existing route remains active.");
            int navigationStuck = configuration.getInt("stuckWindowTicks", "navigation", 16, 8, 80,
                "Ticks of commanded movement used by deterministic stuck detection.");
            float navigationTurn = parseBoundedFloat(configuration.getString("maximumTurnDegreesPerTick",
                "navigation", "32.0", "Maximum visible camera yaw change applied by the 20 Hz local controller."),
                32F, 4F, 60F);
            float navigationArrival = parseBoundedFloat(configuration.getString("arrivalRadius",
                "navigation", "0.80", "Horizontal distance considered a stable waypoint arrival."),
                0.80F, 0.35F, 1.5F);
            int lookaheadNodes = configuration.getInt("lookaheadNodes", "navigation", 7, 1, 16,
                "Maximum future route nodes considered as one safe movement corridor.");
            float lookaheadDistance = parseBoundedFloat(configuration.getString("lookaheadDistance",
                "navigation", "5.0", "Maximum metres of safe route smoothing/lookahead."),
                5F, 1F, 10F);
            int pathValidationNodes = configuration.getInt("pathValidationNodes", "navigation", 12, 2, 32,
                "Future route nodes live-refreshed for block, support, hazard, and corner changes.");
            float offRouteDistance = parseBoundedFloat(configuration.getString("offRouteDistance",
                "navigation", "2.35", "Maximum corridor deviation before an immediate current-position replan."),
                2.35F, 0.75F, 6F);
            float reactiveProbeDistance = parseBoundedFloat(configuration.getString("reactiveProbeDistance",
                "navigation", "1.25", "Distance sampled for each 20 Hz local steering candidate."),
                1.25F, 0.5F, 3F);
            return new SawBotConfig(hudEnabled, window, interval, telemetryQueue, inputWindow,
                compression, host, port, connectTimeout, reconnectDelay, decisionRate, actionAge,
                sequenceLag, allowSingleplayer, allowedServers, physicalTakeover, navigationRadius,
                navigationVertical, navigationNodes, navigationBudget, navigationReplan,
                navigationStuck, navigationTurn, navigationArrival, lookaheadNodes,
                lookaheadDistance, pathValidationNodes, offRouteDistance, reactiveProbeDistance);
        } catch (RuntimeException exception) {
            logger.error("Failed to load SawBotV1 configuration; using safe defaults.", exception);
            return defaults();
        } finally {
            if (configuration.hasChanged()) configuration.save();
        }
    }

    private static float parseBoundedFloat(String value, float fallback, float minimum, float maximum) {
        try {
            return Math.max(minimum, Math.min(maximum, Float.parseFloat(value)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static SawBotConfig defaults() {
        return new SawBotConfig(true, 256, 2, 64, 32, 1, "127.0.0.1", 25189,
            500, 3000, 10, 250, 3, true, "127.0.0.1,localhost", true,
            40, 10, 6144, 96, 4, 16, 32F, 0.80F,
            7, 5F, 12, 2.35F, 1.25F);
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
    public int navigationHorizontalRadius() { return navigationHorizontalRadius; }
    public int navigationVerticalRadius() { return navigationVerticalRadius; }
    public int navigationMaximumExpandedNodes() { return navigationMaximumExpandedNodes; }
    public int navigationExpansionsPerTick() { return navigationExpansionsPerTick; }
    public int navigationReplanIntervalTicks() { return navigationReplanIntervalTicks; }
    public int navigationStuckWindowTicks() { return navigationStuckWindowTicks; }
    public float navigationMaximumTurnDegreesPerTick() { return navigationMaximumTurnDegreesPerTick; }
    public float navigationArrivalRadius() { return navigationArrivalRadius; }
    public int navigationLookaheadNodes() { return navigationLookaheadNodes; }
    public float navigationLookaheadDistance() { return navigationLookaheadDistance; }
    public int navigationPathValidationNodes() { return navigationPathValidationNodes; }
    public float navigationOffRouteDistance() { return navigationOffRouteDistance; }
    public float navigationReactiveProbeDistance() { return navigationReactiveProbeDistance; }
}
