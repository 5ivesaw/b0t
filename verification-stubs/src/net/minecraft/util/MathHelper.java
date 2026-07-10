package net.minecraft.util;
public final class MathHelper { public static int floor_double(double v){int i=(int)v;return v<i?i-1:i;} public static float clamp_float(float v,float min,float max){return Math.max(min,Math.min(max,v));} }
