package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.telemetry.HumanInputSample;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/** Reads legitimate human controls and raw MouseHelper deltas on the client thread. */
final class HumanInputCapture {
    private final Minecraft minecraft;

    HumanInputCapture(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    HumanInputSample capture(long clientTick) {
        if (minecraft.thePlayer == null) return null;
        GameSettings settings = minecraft.gameSettings;
        int bits = 0;
        if (down(settings.keyBindForward)) bits |= HumanInputSample.FORWARD;
        if (down(settings.keyBindBack)) bits |= HumanInputSample.BACK;
        if (down(settings.keyBindLeft)) bits |= HumanInputSample.LEFT;
        if (down(settings.keyBindRight)) bits |= HumanInputSample.RIGHT;
        if (down(settings.keyBindJump)) bits |= HumanInputSample.JUMP;
        if (down(settings.keyBindSprint)) bits |= HumanInputSample.SPRINT;
        if (down(settings.keyBindSneak)) bits |= HumanInputSample.SNEAK;
        if (down(settings.keyBindAttack)) bits |= HumanInputSample.ATTACK;
        if (down(settings.keyBindUseItem)) bits |= HumanInputSample.USE;
        if (down(settings.keyBindDrop)) bits |= HumanInputSample.DROP;
        if (down(settings.keyBindInventory)) bits |= HumanInputSample.INVENTORY;
        int dx = minecraft.mouseHelper == null ? 0 : minecraft.mouseHelper.deltaX;
        int dy = minecraft.mouseHelper == null ? 0 : minecraft.mouseHelper.deltaY;
        return new HumanInputSample(clientTick, System.nanoTime(), bits, dx, dy,
            minecraft.thePlayer.inventory.currentItem, minecraft.currentScreen != null);
    }

    private static boolean down(KeyBinding binding) {
        return binding != null && binding.isKeyDown();
    }
}
