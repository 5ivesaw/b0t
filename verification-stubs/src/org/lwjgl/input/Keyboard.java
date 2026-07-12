package org.lwjgl.input;
import java.util.HashMap;
import java.util.Map;
public final class Keyboard {
    public static final int KEY_NONE=0,KEY_G=34,KEY_R=19,KEY_LSHIFT=42,KEY_RSHIFT=54,KEY_B=48,KEY_C=46,KEY_H=35,KEY_K=37,KEY_M=50,KEY_N=49,KEY_O=24,KEY_P=25,KEY_V=47,
        KEY_LBRACKET=26,KEY_RBRACKET=27,KEY_PERIOD=52,KEY_F7=65,KEY_F9=67,KEY_F10=68,KEY_F12=88;
    private static final Map<Integer,Boolean> STATES=new HashMap<Integer,Boolean>();
    private Keyboard(){}
    public static boolean isKeyDown(int key){Boolean value=STATES.get(Integer.valueOf(key));return value!=null&&value.booleanValue();}
    public static void setKeyDownForTest(int key,boolean down){STATES.put(Integer.valueOf(key),Boolean.valueOf(down));}
    public static void clearForTest(){STATES.clear();}
}
