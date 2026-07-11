package net.minecraft.client.gui;
import net.minecraft.client.Minecraft;
public final class ScaledResolution {
    private final int width;
    private final int height;
    public ScaledResolution(Minecraft minecraft){
        this.width=Math.max(1,minecraft.displayWidth/2);
        this.height=Math.max(1,minecraft.displayHeight/2);
    }
    public int getScaledWidth(){return width;}
    public int getScaledHeight(){return height;}
}
