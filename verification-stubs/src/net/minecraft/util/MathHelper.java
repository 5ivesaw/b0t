package net.minecraft.util;
public final class MathHelper {
    private MathHelper(){}
    public static int floor_double(double v){int i=(int)v;return v<i?i-1:i;}
    public static float clamp_float(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    public static float wrapAngleTo180_float(float value){value%=360.0F;if(value>=180.0F)value-=360.0F;if(value<-180.0F)value+=360.0F;return value;}
}
