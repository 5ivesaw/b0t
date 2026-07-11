package net.minecraft.client.renderer;
public final class GlStateManager {
    private GlStateManager(){}
    public static void pushMatrix(){}
    public static void popMatrix(){}
    public static void translate(double x,double y,double z){}
    public static void rotate(float angle,float x,float y,float z){}
    public static void scale(float x,float y,float z){}
    public static void enableBlend(){}
    public static void disableBlend(){}
    public static void tryBlendFuncSeparate(int src,int dst,int srcAlpha,int dstAlpha){}
    public static void disableTexture2D(){}
    public static void enableTexture2D(){}
    public static void disableLighting(){}
    public static void enableLighting(){}
    public static void depthMask(boolean value){}
    public static void disableDepth(){}
    public static void enableDepth(){}
    public static void enableAlpha(){}
    public static void enableCull(){}
    public static void color(float red,float green,float blue,float alpha){}
}
