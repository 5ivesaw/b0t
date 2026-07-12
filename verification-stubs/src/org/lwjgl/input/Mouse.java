package org.lwjgl.input;
import java.util.HashMap;
import java.util.Map;
public final class Mouse {
    private static final Map<Integer,Boolean> STATES=new HashMap<Integer,Boolean>();
    private Mouse(){}
    public static boolean isButtonDown(int button){Boolean value=STATES.get(Integer.valueOf(button));return value!=null&&value.booleanValue();}
    public static void setButtonDownForTest(int button,boolean down){STATES.put(Integer.valueOf(button),Boolean.valueOf(down));}
    public static void clearForTest(){STATES.clear();}
}
