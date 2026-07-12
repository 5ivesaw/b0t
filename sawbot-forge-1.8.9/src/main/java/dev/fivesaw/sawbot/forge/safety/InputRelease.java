package dev.fivesaw.sawbot.forge.safety;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Restores each binding to its real hardware state instead of forcing user input false. */
public final class InputRelease {
    private InputRelease() { }

    public static void releaseAll(Minecraft minecraft) {
        if (minecraft == null) return;
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return;
        restorePhysical(settings.keyBindForward);
        restorePhysical(settings.keyBindBack);
        restorePhysical(settings.keyBindLeft);
        restorePhysical(settings.keyBindRight);
        restorePhysical(settings.keyBindJump);
        restorePhysical(settings.keyBindSneak);
        restorePhysical(settings.keyBindSprint);
        restorePhysical(settings.keyBindAttack);
        restorePhysical(settings.keyBindUseItem);
        restorePhysical(settings.keyBindDrop);
        restorePhysical(settings.keyBindInventory);
    }

    public static void restorePhysical(KeyBinding binding) {
        if (binding == null) return;
        int code = binding.getKeyCode();
        boolean physical;
        if (code >= 0) physical = Keyboard.isKeyDown(code);
        else physical = Mouse.isButtonDown(code + 100);
        KeyBinding.setKeyBindState(code, physical);
    }
}
