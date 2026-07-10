package net.minecraft.client.renderer;
public class Tessellator {
    private static final Tessellator INSTANCE=new Tessellator();
    private final WorldRenderer renderer=new WorldRenderer();
    public static Tessellator getInstance(){return INSTANCE;}
    public WorldRenderer getWorldRenderer(){return renderer;}
    public void draw(){}
}
