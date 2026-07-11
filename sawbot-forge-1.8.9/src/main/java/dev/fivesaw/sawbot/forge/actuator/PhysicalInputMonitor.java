package dev.fivesaw.sawbot.forge.actuator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Reads physical hardware state rather than synthetic KeyBinding state. */
public final class PhysicalInputMonitor {
    private final Minecraft minecraft;

    public PhysicalInputMonitor(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    public boolean hasTakeoverInput() {
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return false;
        return physical(settings.keyBindForward)
            || physical(settings.keyBindBack)
            || physical(settings.keyBindLeft)
            || physical(settings.keyBindRight)
            || physical(settings.keyBindJump)
            || physical(settings.keyBindSneak)
            || physical(settings.keyBindSprint)
            || physical(settings.keyBindDrop)
            || Mouse.isButtonDown(0)
            || Mouse.isButtonDown(1)
            || (minecraft.mouseHelper != null
                && (minecraft.mouseHelper.deltaX != 0 || minecraft.mouseHelper.deltaY != 0));
    }

    private static boolean physical(KeyBinding binding) {
        if (binding == null) return false;
        int code = binding.getKeyCode();
        return code > 0 && Keyboard.isKeyDown(code);
    }
}
