package net.minecraft.client.settings;
import java.util.HashMap;
import java.util.Map;
public class KeyBinding {
    private static final Map<Integer, Boolean> STATES = new HashMap<Integer, Boolean>();
    private static final Map<Integer, Integer> PULSES = new HashMap<Integer, Integer>();
    private final int key;
    public KeyBinding(String description, int keyCode, String category) { this.key = keyCode; }
    public boolean isPressed() { return false; }
    public boolean isKeyDown() { return isKeyDownForTest(key); }
    public int getKeyCode() { return key; }
    public static void setKeyBindState(int keyCode, boolean state) { STATES.put(keyCode, state); }
    public static void onTick(int keyCode) { Integer count=PULSES.get(keyCode);PULSES.put(keyCode,Integer.valueOf(count==null?1:count.intValue()+1)); }
    public static boolean isKeyDownForTest(int keyCode) { Boolean state = STATES.get(keyCode); return state != null && state.booleanValue(); }
    public static int pulseCountForTest(int keyCode){Integer count=PULSES.get(keyCode);return count==null?0:count.intValue();}
    public static boolean anyKeyDownForTest(){for(Boolean state:STATES.values())if(state.booleanValue())return true;return false;}
    public static void clearForTest(){STATES.clear();PULSES.clear();}
}
