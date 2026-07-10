package dev.fivesaw.sawbot.forge.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class SawBotKeyBindings {
    private static final String CATEGORY = "key.categories.sawbotv1";

    public final KeyBinding toggleEnabled = new KeyBinding("key.sawbotv1.toggle", Keyboard.KEY_F10, CATEGORY);
    public final KeyBinding manualTakeover = new KeyBinding("key.sawbotv1.takeover", Keyboard.KEY_F9, CATEGORY);
    public final KeyBinding emergencyStop = new KeyBinding("key.sawbotv1.emergency", Keyboard.KEY_F12, CATEGORY);
    public final KeyBinding toggleInspector = new KeyBinding("key.sawbotv1.inspector", Keyboard.KEY_F7, CATEGORY);
    public final KeyBinding toggleFreeze = new KeyBinding("key.sawbotv1.freeze", Keyboard.KEY_P, CATEGORY);
    public final KeyBinding stepObservation = new KeyBinding("key.sawbotv1.step", Keyboard.KEY_PERIOD, CATEGORY);
    public final KeyBinding cycleInspectorPage = new KeyBinding("key.sawbotv1.page", Keyboard.KEY_H, CATEGORY);
    public final KeyBinding toggleTerrainOverlay = new KeyBinding("key.sawbotv1.terrain", Keyboard.KEY_B, CATEGORY);
    public final KeyBinding toggleCollisionOverlay = new KeyBinding("key.sawbotv1.collision", Keyboard.KEY_C, CATEGORY);
    public final KeyBinding toggleEntityOverlay = new KeyBinding("key.sawbotv1.entities", Keyboard.KEY_N, CATEGORY);
    public final KeyBinding toggleEntityTracers = new KeyBinding("key.sawbotv1.tracers", Keyboard.KEY_V, CATEGORY);
    public final KeyBinding toggleLandmarkOverlay = new KeyBinding("key.sawbotv1.landmarks", Keyboard.KEY_M, CATEGORY);
    public final KeyBinding previousEntity = new KeyBinding("key.sawbotv1.previousEntity", Keyboard.KEY_LBRACKET, CATEGORY);
    public final KeyBinding nextEntity = new KeyBinding("key.sawbotv1.nextEntity", Keyboard.KEY_RBRACKET, CATEGORY);
    public final KeyBinding exportSnapshot = new KeyBinding("key.sawbotv1.export", Keyboard.KEY_O, CATEGORY);
    public final KeyBinding toggleTelemetry = new KeyBinding("key.sawbotv1.telemetry", Keyboard.KEY_NONE, CATEGORY);

    public void register() {
        ClientRegistry.registerKeyBinding(toggleEnabled);
        ClientRegistry.registerKeyBinding(manualTakeover);
        ClientRegistry.registerKeyBinding(emergencyStop);
        ClientRegistry.registerKeyBinding(toggleInspector);
        ClientRegistry.registerKeyBinding(toggleFreeze);
        ClientRegistry.registerKeyBinding(stepObservation);
        ClientRegistry.registerKeyBinding(cycleInspectorPage);
        ClientRegistry.registerKeyBinding(toggleTerrainOverlay);
        ClientRegistry.registerKeyBinding(toggleCollisionOverlay);
        ClientRegistry.registerKeyBinding(toggleEntityOverlay);
        ClientRegistry.registerKeyBinding(toggleEntityTracers);
        ClientRegistry.registerKeyBinding(toggleLandmarkOverlay);
        ClientRegistry.registerKeyBinding(previousEntity);
        ClientRegistry.registerKeyBinding(nextEntity);
        ClientRegistry.registerKeyBinding(exportSnapshot);
        ClientRegistry.registerKeyBinding(toggleTelemetry);
    }
}
