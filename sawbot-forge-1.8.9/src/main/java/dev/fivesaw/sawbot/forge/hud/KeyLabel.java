package dev.fivesaw.sawbot.forge.hud;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/** Resolves the actual user-facing binding rather than hardcoding default keys. */
public final class KeyLabel {
    private KeyLabel() { }

    public static String of(KeyBinding binding) {
        if (binding == null) return "?";
        int code = binding.getKeyCode();
        if (code == Keyboard.KEY_NONE) return "UNBOUND";
        if (code < 0) return "MOUSE " + (code + 101);
        if (code == Keyboard.KEY_PERIOD) return ".";
        if (code == Keyboard.KEY_LBRACKET) return "[";
        if (code == Keyboard.KEY_RBRACKET) return "]";
        String name = Keyboard.getKeyName(code);
        return name == null || name.isEmpty() ? Integer.toString(code) : name;
    }
}
