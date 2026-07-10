package dev.fivesaw.sawbot.forge.client;

import dev.fivesaw.sawbot.forge.config.SawBotConfig;
import dev.fivesaw.sawbot.forge.hud.FoundationHud;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotMode;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

public final class ClientRuntime {
    private final Minecraft minecraft;private final SawBotConfig config;private final Logger logger;private final SawBotKeyBindings keys;private final SawBotStateController state;private final RollingTimingWindow tickTiming;private final ObservationPipeline observations;private final FoundationHud hud;private long clientTick;private boolean registered;
    public ClientRuntime(SawBotConfig config,Logger logger){if(config==null||logger==null)throw new IllegalArgumentException("config/logger");this.minecraft=Minecraft.getMinecraft();this.config=config;this.logger=logger;this.keys=new SawBotKeyBindings();this.state=new SawBotStateController(minecraft,logger);this.tickTiming=new RollingTimingWindow(config.timingWindowSize());this.observations=new ObservationPipeline(minecraft,config.sensorIntervalTicks());this.hud=new FoundationHud(minecraft,state,tickTiming,observations);}
    public void register(){if(registered)throw new IllegalStateException("ClientRuntime already registered");keys.register();FMLCommonHandler.instance().bus().register(this);MinecraftForge.EVENT_BUS.register(this);registered=true;}
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.START)return;long start=System.nanoTime();try{processSafetyKeysFirst();if(minecraft.theWorld==null||minecraft.thePlayer==null){if(state.isEnabled())state.onWorldUnavailable();observations.tick(clientTick,false);return;}clientTick++;observations.tick(clientTick,state.mode()==SawBotMode.FROZEN);}catch(RuntimeException exception){state.emergencyStop();logger.error("Unhandled SawBotV1 client tick error; emergency stop applied.",exception);}finally{tickTiming.add(System.nanoTime()-start);}}
    private void processSafetyKeysFirst(){if(keys.emergencyStop.isPressed()){state.emergencyStop();return;}if(keys.manualTakeover.isPressed()){state.manualTakeover();return;}if(keys.toggleEnabled.isPressed())state.toggleEnabled();if(keys.toggleFreeze.isPressed())state.toggleFrozen();if(keys.toggleInspector.isPressed())state.toggleInspector();if(keys.toggleTelemetry.isPressed())state.toggleTelemetryRequest();}
    @SubscribeEvent public void onRenderOverlay(RenderGameOverlayEvent.Text event){if(config.hudEnabled())hud.render(clientTick);}
}
