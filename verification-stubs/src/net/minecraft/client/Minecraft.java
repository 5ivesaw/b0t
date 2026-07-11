package net.minecraft.client;
import java.io.File;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.util.MouseHelper;
public class Minecraft {
    private static final Minecraft I=new Minecraft();
    public GameSettings gameSettings=new GameSettings();
    public World theWorld;
    public EntityPlayerSP thePlayer;
    public FontRenderer fontRendererObj=new FontRenderer();
    public GuiScreen currentScreen;
    public MovingObjectPosition objectMouseOver;
    public MouseHelper mouseHelper=new MouseHelper();
    public File mcDataDir=new File(System.getProperty("java.io.tmpdir"),"sawbot-test-mc");
    private final NetHandlerPlayClient net=new NetHandlerPlayClient();
    private final RenderManager renderManager=new RenderManager();
    public static Minecraft getMinecraft(){return I;}
    public NetHandlerPlayClient getNetHandler(){return net;}
    public RenderManager getRenderManager(){return renderManager;}
}
