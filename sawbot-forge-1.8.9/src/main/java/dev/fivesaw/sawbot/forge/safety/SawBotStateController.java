package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public final class SawBotStateController {
    private final Minecraft minecraft;
    private final Logger logger;
    private SawBotMode mode = SawBotMode.DISABLED;
    private boolean observationsFrozen;
    private boolean inspectorVisible;
    private boolean telemetryRequested;
    private String lastStopReason = "startup";

    public SawBotStateController(Minecraft minecraft, Logger logger) {
        if (minecraft == null || logger == null) throw new IllegalArgumentException("minecraft/logger");
        this.minecraft = minecraft;
        this.logger = logger;
    }

    public void toggleEnabled() {
        if (mode == SawBotMode.DISABLED) {
            mode = SawBotMode.ENABLED;
            logger.warn("SawBotV1 enabled in Phase 1. Sensors run, but no model or actuator loop exists yet.");
        } else {
            disableAndRelease("toggle disable");
        }
    }

    /**
     * Snapshot freezing is an inspector concern, not an enabled-state concern.
     * It must work while autonomous control is disabled because Phase 1 sensors
     * intentionally continue running in DISABLED mode for validation.
     */
    public void toggleFrozen() {
        observationsFrozen = !observationsFrozen;
        InputRelease.releaseAll(minecraft);
        logger.info("SawBotV1 observation freeze changed to {}.", Boolean.valueOf(observationsFrozen));
    }

    public void manualTakeover() { disableAndRelease("manual takeover"); }
    public void emergencyStop() { disableAndRelease("emergency stop"); }
    public void onWorldUnavailable() {
        observationsFrozen = false;
        disableAndRelease("world unavailable/disconnect");
    }
    public void shutdown() {
        observationsFrozen = false;
        disableAndRelease("client shutdown");
    }

    public void disableAndRelease(String reason) {
        mode = SawBotMode.DISABLED;
        telemetryRequested = false;
        lastStopReason = reason == null ? "unspecified" : reason;
        InputRelease.releaseAll(minecraft);
        logger.info("SawBotV1 disabled; inputs released. Reason: {}", lastStopReason);
    }

    public void toggleInspector() { inspectorVisible = !inspectorVisible; }
    public void toggleTelemetryRequest() { telemetryRequested = !telemetryRequested; }
    public SawBotMode mode() { return mode; }
    public boolean isEnabled() { return mode == SawBotMode.ENABLED; }
    public boolean observationsFrozen() { return observationsFrozen; }
    public boolean mayApplyAutonomousActions() { return isEnabled() && !observationsFrozen; }
    public boolean inspectorVisible() { return inspectorVisible; }
    public boolean telemetryRequested() { return telemetryRequested; }
    public String lastStopReason() { return lastStopReason; }
}
