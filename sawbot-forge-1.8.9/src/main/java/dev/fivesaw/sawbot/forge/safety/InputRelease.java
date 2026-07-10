package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public final class InputRelease {
    private InputRelease() { }

    public static void releaseAll(Minecraft minecraft) {
        if (minecraft == null) return;
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return;
        release(settings.keyBindForward);
        release(settings.keyBindBack);
        release(settings.keyBindLeft);
        release(settings.keyBindRight);
        release(settings.keyBindJump);
        release(settings.keyBindSneak);
        release(settings.keyBindSprint);
        release(settings.keyBindAttack);
        release(settings.keyBindUseItem);
        release(settings.keyBindDrop);
        release(settings.keyBindInventory);
    }

    private static void release(KeyBinding binding) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), false);
    }
}
