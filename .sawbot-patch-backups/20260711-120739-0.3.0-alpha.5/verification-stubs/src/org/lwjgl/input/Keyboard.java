package org.lwjgl.input;
public final class Keyboard {
    public static final int KEY_NONE=0,KEY_B=48,KEY_C=46,KEY_H=35,KEY_M=50,KEY_N=49,KEY_O=24,KEY_P=25,KEY_V=47,
        KEY_LBRACKET=26,KEY_RBRACKET=27,KEY_PERIOD=52,KEY_F7=65,KEY_F9=67,KEY_F10=68,KEY_F12=88;
    private Keyboard(){}
 public static String getKeyName(int code){ if(code==KEY_F10)return "F10"; if(code==KEY_F9)return "F9"; if(code==KEY_F12)return "F12"; if(code==KEY_F7)return "F7"; if(code==KEY_P)return "P"; if(code==KEY_PERIOD)return "."; if(code==KEY_H)return "H"; if(code==KEY_B)return "B"; if(code==KEY_C)return "C"; if(code==KEY_N)return "N"; if(code==KEY_V)return "V"; if(code==KEY_M)return "M"; if(code==KEY_LBRACKET)return "["; if(code==KEY_RBRACKET)return "]"; if(code==KEY_O)return "O"; return Integer.toString(code); } }
