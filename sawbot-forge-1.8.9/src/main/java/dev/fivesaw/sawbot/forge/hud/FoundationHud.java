package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import net.minecraft.client.Minecraft;

public final class FoundationHud {
    private static final int WHITE = 0xFFFFFF;
    private static final int MUTED = 0xA0A0A0;
    private static final int SAFE = 0x55FF55;
    private static final int WARNING = 0xFFAA00;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;

    public FoundationHud(Minecraft minecraft, SawBotStateController state, RollingTimingWindow tickTiming) {
        this.minecraft = minecraft;
        this.state = state;
        this.tickTiming = tickTiming;
    }

    public void render(long clientTick) {
        if (minecraft.fontRendererObj == null) return;
        int x = 6;
        int y = 6;
        int statusColour = state.isEnabled() ? WARNING : SAFE;
        draw("SawBotV1  Phase 0", x, y, WHITE); y += 10;
        draw("State: " + state.mode(), x, y, statusColour); y += 10;
        draw("Tick: " + clientTick + "  Handler avg: " + micros(tickTiming.averageNanos()) + " us", x, y, MUTED); y += 10;
        draw("Latest/max: " + micros(tickTiming.latestNanos()) + "/" + micros(tickTiming.maximumNanos()) + " us", x, y, MUTED); y += 10;
        draw("F8 toggle  F9 takeover  F12 emergency", x, y, MUTED);
        if (state.inspectorVisible()) {
            y += 10;
            draw("Inspector: Phase 0 placeholder (no model inputs exist yet)", x, y, WARNING);
        }
        if (state.telemetryRequested()) {
            y += 10;
            draw("Telemetry intent: ON (writer intentionally absent until Phase 3)", x, y, WARNING);
        }
    }

    private void draw(String text, int x, int y, int colour) {
        minecraft.fontRendererObj.drawStringWithShadow(text, x, y, colour);
    }
    private static long micros(long nanos) { return nanos / 1_000L; }
}
