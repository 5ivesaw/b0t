package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public final class SawBotStateController {
    private final Minecraft minecraft;
    private final Logger logger;
    private SawBotMode mode = SawBotMode.DISABLED;
    private boolean observationsFrozen;
    private boolean observationStepRequested;
    private boolean observationRefreshRequested;
    private boolean inspectorVisible;
    private boolean terrainOverlayVisible;
    private boolean collisionOverlayVisible;
    private boolean entityOverlayVisible;
    private boolean entityTracersVisible = true;
    private boolean landmarkOverlayVisible;
    private boolean telemetryRequested;
    private String lastStopReason = "startup";
    private String inspectorNotice = "";
    private long inspectorNoticeTimestampNanos;
    private int inspectorNoticeSeverity;
    private static final long NOTICE_LIFETIME_NANOS = 5_000_000_000L;

    public SawBotStateController(Minecraft minecraft, Logger logger) {
        if (minecraft == null || logger == null) throw new IllegalArgumentException("minecraft/logger");
        this.minecraft = minecraft;
        this.logger = logger;
    }

    public void toggleEnabled() {
        if (mode == SawBotMode.DISABLED) enable();
        else disableAndRelease("toggle disable");
    }

    public void enable() {
        mode = SawBotMode.ENABLED;
        lastStopReason = "enabled";
        logger.warn("SawBotV1 Phase 8 hybrid specialist control enabled for the configured private/local scope.");
    }

    /** Snapshot freezing remains independent from autonomous enable/disable state. */
    public void toggleFrozen() {
        observationsFrozen = !observationsFrozen;
        observationStepRequested = false;
        observationRefreshRequested = !observationsFrozen;
        InputRelease.releaseAll(minecraft);
        setInspectorNotice(observationsFrozen ? "snapshot frozen; overlays stay at captured world positions" : "snapshot live; refreshing now");
        logger.info("SawBotV1 observation freeze changed to {}.", Boolean.valueOf(observationsFrozen));
    }

    /** Queue exactly one observation capture on the current client tick while frozen. */
    public boolean requestObservationStep() {
        if (!observationsFrozen) {
            setInspectorNotice("freeze with P before stepping");
            return false;
        }
        observationStepRequested = true;
        setInspectorNotice("single observation step queued");
        return true;
    }

    public boolean consumeObservationStepRequest() {
        boolean requested = observationStepRequested;
        observationStepRequested = false;
        return requested;
    }

    /** Forces an immediate fresh capture on the first live tick after unfreezing. */
    public boolean consumeObservationRefreshRequest() {
        boolean requested = observationRefreshRequested;
        observationRefreshRequested = false;
        return requested;
    }

    public void manualTakeover() { disableAndRelease("manual takeover"); setInspectorNotice("TAKEOVER: manual control restored", 2); }
    public void emergencyStop() { disableAndRelease("emergency stop"); setInspectorNotice("EMERGENCY: all SawBot inputs released", 3); }
    public void onWorldUnavailable() {
        observationsFrozen = false;
        observationStepRequested = false;
        observationRefreshRequested = false;
        disableAndRelease("world unavailable/disconnect");
    }
    public void shutdown() {
        observationsFrozen = false;
        observationStepRequested = false;
        observationRefreshRequested = false;
        disableAndRelease("client shutdown");
    }

    public void disableAndRelease(String reason) {
        mode = SawBotMode.DISABLED;
        observationStepRequested = false;
        lastStopReason = reason == null ? "unspecified" : reason;
        InputRelease.releaseAll(minecraft);
        logger.info("SawBotV1 disabled; inputs released. Reason: {}", lastStopReason);
    }

    public void toggleInspector() { inspectorVisible = !inspectorVisible; }
    public void toggleTerrainOverlay() { terrainOverlayVisible = !terrainOverlayVisible; setInspectorNotice("terrain overlay " + onOff(terrainOverlayVisible)); }
    public void toggleCollisionOverlay() { collisionOverlayVisible = !collisionOverlayVisible; setInspectorNotice("collision overlay " + onOff(collisionOverlayVisible)); }
    public void toggleEntityOverlay() { entityOverlayVisible = !entityOverlayVisible; setInspectorNotice("entity overlay " + onOff(entityOverlayVisible)); }
    public void toggleEntityTracers() { entityTracersVisible = !entityTracersVisible; setInspectorNotice("entity tracers " + onOff(entityTracersVisible)); }
    public void toggleLandmarkOverlay() { landmarkOverlayVisible = !landmarkOverlayVisible; setInspectorNotice("landmark overlay " + onOff(landmarkOverlayVisible)); }
    public void toggleTelemetryRequest() { telemetryRequested = !telemetryRequested; setInspectorNotice("telemetry " + (telemetryRequested ? "START" : "STOP")); }
    public void clearTelemetryRequest() { telemetryRequested = false; }
    public void setInspectorNotice(String notice) { setInspectorNotice(notice, 0); }
    public void setInspectorNotice(String notice, int severity) {
        inspectorNotice = notice == null ? "" : notice;
        inspectorNoticeSeverity = Math.max(0, Math.min(3, severity));
        inspectorNoticeTimestampNanos = System.nanoTime();
    }
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
    public String inspectorNotice() {
        if (inspectorNotice.isEmpty()) return "";
        if (System.nanoTime() - inspectorNoticeTimestampNanos > NOTICE_LIFETIME_NANOS) return "";
        return inspectorNotice;
    }
    public int inspectorNoticeSeverity() { return inspectorNotice().isEmpty() ? 0 : inspectorNoticeSeverity; }
}
