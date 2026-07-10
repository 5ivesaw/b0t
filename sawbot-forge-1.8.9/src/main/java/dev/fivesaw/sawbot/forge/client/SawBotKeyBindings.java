package dev.fivesaw.sawbot.forge.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class SawBotKeyBindings {
    private static final String CATEGORY = "key.categories.sawbotv1";

    public final KeyBinding toggleEnabled = new KeyBinding("key.sawbotv1.toggle", Keyboard.KEY_F8, CATEGORY);
    public final KeyBinding manualTakeover = new KeyBinding("key.sawbotv1.takeover", Keyboard.KEY_F9, CATEGORY);
    public final KeyBinding emergencyStop = new KeyBinding("key.sawbotv1.emergency", Keyboard.KEY_F12, CATEGORY);
    public final KeyBinding toggleInspector = new KeyBinding("key.sawbotv1.inspector", Keyboard.KEY_F7, CATEGORY);
    public final KeyBinding toggleFreeze = new KeyBinding("key.sawbotv1.freeze", Keyboard.KEY_F6, CATEGORY);
    public final KeyBinding toggleTelemetry = new KeyBinding("key.sawbotv1.telemetry", Keyboard.KEY_F5, CATEGORY);

    public void register() {
        ClientRegistry.registerKeyBinding(toggleEnabled);
        ClientRegistry.registerKeyBinding(manualTakeover);
        ClientRegistry.registerKeyBinding(emergencyStop);
        ClientRegistry.registerKeyBinding(toggleInspector);
        ClientRegistry.registerKeyBinding(toggleFreeze);
        ClientRegistry.registerKeyBinding(toggleTelemetry);
    }
}
