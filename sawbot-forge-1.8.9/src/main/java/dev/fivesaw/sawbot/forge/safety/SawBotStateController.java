package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public final class SawBotStateController {
    private final Minecraft minecraft;
    private final Logger logger;
    private SawBotMode mode = SawBotMode.DISABLED;
    private boolean observationsFrozen;
    private boolean observationStepRequested;
    private boolean inspectorVisible;
    private boolean terrainOverlayVisible;
    private boolean collisionOverlayVisible;
    private boolean entityOverlayVisible;
    private boolean entityTracersVisible = true;
    private boolean landmarkOverlayVisible;
    private boolean telemetryRequested;
    private String lastStopReason = "startup";
    private String inspectorNotice = "";

    public SawBotStateController(Minecraft minecraft, Logger logger) {
        if (minecraft == null || logger == null) throw new IllegalArgumentException("minecraft/logger");
        this.minecraft = minecraft;
        this.logger = logger;
    }

    public void toggleEnabled() {
        if (mode == SawBotMode.DISABLED) {
            mode = SawBotMode.ENABLED;
            inspectorNotice = "SawBot enabled - sensors only";
            logger.warn("SawBotV1 enabled in Phase 2. Sensors and inspectors run, but no model or actuator loop exists yet.");
        } else {
            disableAndRelease("toggle disable");
        }
    }

    /** Snapshot freezing remains independent from autonomous enable/disable state. */
    public void toggleFrozen() {
        observationsFrozen = !observationsFrozen;
        observationStepRequested = false;
        InputRelease.releaseAll(minecraft);
        inspectorNotice = observationsFrozen ? "snapshot frozen" : "snapshot live";
        logger.info("SawBotV1 observation freeze changed to {}.", Boolean.valueOf(observationsFrozen));
    }

    /** Queue exactly one observation capture on the current client tick while frozen. */
    public boolean requestObservationStep() {
        if (!observationsFrozen) {
            inspectorNotice = "freeze with P before stepping";
            return false;
        }
        observationStepRequested = true;
        inspectorNotice = "single observation step queued";
        return true;
    }

    public boolean consumeObservationStepRequest() {
        boolean requested = observationStepRequested;
        observationStepRequested = false;
        return requested;
    }

    public void manualTakeover() { disableAndRelease("manual takeover"); inspectorNotice = "manual takeover - inputs released"; }
    public void emergencyStop() { disableAndRelease("emergency stop"); inspectorNotice = "emergency stop - inputs released"; }
    public void onWorldUnavailable() {
        observationsFrozen = false;
        observationStepRequested = false;
        disableAndRelease("world unavailable/disconnect");
    }
    public void shutdown() {
        observationsFrozen = false;
        observationStepRequested = false;
        disableAndRelease("client shutdown");
    }

    public void disableAndRelease(String reason) {
        mode = SawBotMode.DISABLED;
        telemetryRequested = false;
        observationStepRequested = false;
        lastStopReason = reason == null ? "unspecified" : reason;
        InputRelease.releaseAll(minecraft);
        inspectorNotice = "SawBot disabled - " + lastStopReason;
        logger.info("SawBotV1 disabled; inputs released. Reason: {}", lastStopReason);
    }

    public void toggleInspector() {
        inspectorVisible = !inspectorVisible;
        inspectorNotice = inspectorVisible ? "inspector opened" : "inspector closed";
    }
    public void toggleTerrainOverlay() { terrainOverlayVisible = !terrainOverlayVisible; inspectorNotice = "terrain overlay " + onOff(terrainOverlayVisible); }
    public void toggleCollisionOverlay() { collisionOverlayVisible = !collisionOverlayVisible; inspectorNotice = "collision overlay " + onOff(collisionOverlayVisible); }
    public void toggleEntityOverlay() { entityOverlayVisible = !entityOverlayVisible; inspectorNotice = "entity overlay " + onOff(entityOverlayVisible); }
    public void toggleEntityTracers() { entityTracersVisible = !entityTracersVisible; inspectorNotice = "entity tracers " + onOff(entityTracersVisible); }
    public void toggleLandmarkOverlay() { landmarkOverlayVisible = !landmarkOverlayVisible; inspectorNotice = "landmark overlay " + onOff(landmarkOverlayVisible); }
    public void toggleTelemetryRequest() { telemetryRequested = !telemetryRequested; }
    public void setInspectorNotice(String notice) { inspectorNotice = notice == null ? "" : notice; }
    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

    public SawBotMode mode() { return mode; }
    public boolean isEnabled() { return mode == SawBotMode.ENABLED; }
    public boolean observationsFrozen() { return observationsFrozen; }
    public boolean mayApplyAutonomousActions() { return isEnabled() && !observationsFrozen; }
    public boolean inspectorVisible() { return inspectorVisible; }
    public boolean terrainOverlayVisible() { return terrainOverlayVisible; }
    public boolean collisionOverlayVisible() { return collisionOverlayVisible; }
    public boolean entityOverlayVisible() { return entityOverlayVisible; }
    public boolean entityTracersVisible() { return entityTracersVisible; }
    public boolean landmarkOverlayVisible() { return landmarkOverlayVisible; }
    public boolean telemetryRequested() { return telemetryRequested; }
    public String lastStopReason() { return lastStopReason; }
    public String inspectorNotice() { return inspectorNotice; }
}
