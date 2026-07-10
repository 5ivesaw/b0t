package net.minecraft.client.renderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
public class WorldRenderer {
    public void begin(int mode,VertexFormat format){}
    public WorldRenderer pos(double x,double y,double z){return this;}
    public WorldRenderer color(int r,int g,int b,int a){return this;}
    public void endVertex(){}
}
