package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

public final class SawBotStateController {
    private final Minecraft minecraft;
    private final Logger logger;
    private SawBotMode mode = SawBotMode.DISABLED;
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
            logger.warn("SawBotV1 enabled in Phase 0. No model or actuator loop exists yet.");
        } else {
            disableAndRelease("toggle disable");
        }
    }

    public void toggleFrozen() {
        if (mode == SawBotMode.DISABLED) return;
        mode = mode == SawBotMode.FROZEN ? SawBotMode.ENABLED : SawBotMode.FROZEN;
        InputRelease.releaseAll(minecraft);
    }

    public void manualTakeover() { disableAndRelease("manual takeover"); }
    public void emergencyStop() { disableAndRelease("emergency stop"); }
    public void onWorldUnavailable() { disableAndRelease("world unavailable/disconnect"); }
    public void shutdown() { disableAndRelease("client shutdown"); }

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
    public boolean isEnabled() { return mode != SawBotMode.DISABLED; }
    public boolean inspectorVisible() { return inspectorVisible; }
    public boolean telemetryRequested() { return telemetryRequested; }
    public String lastStopReason() { return lastStopReason; }
}
