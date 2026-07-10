package net.minecraft.client.settings;
import java.util.HashMap;
import java.util.Map;
public class KeyBinding {
    private static final Map<Integer, Boolean> STATES = new HashMap<Integer, Boolean>();
    private final int key;
    public KeyBinding(String description, int keyCode, String category) { this.key = keyCode; }
    public boolean isPressed() { return false; }
    public int getKeyCode() { return key; }
    public static void setKeyBindState(int keyCode, boolean state) { STATES.put(keyCode, state); }
    public static boolean isKeyDownForTest(int keyCode) { Boolean state = STATES.get(keyCode); return state != null && state.booleanValue(); }
}
