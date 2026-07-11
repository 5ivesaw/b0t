package dev.fivesaw.sawbot.forge.client;

import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.forge.config.SawBotConfig;
import dev.fivesaw.sawbot.forge.hud.FoundationHud;
import dev.fivesaw.sawbot.forge.hud.WorldDebugRenderer;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.SnapshotExportService;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

public final class ClientRuntime {
    private final Minecraft minecraft;
    private final SawBotConfig config;
    private final Logger logger;
    private final SawBotKeyBindings keys;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;
    private final ObservationPipeline observations;
    private final InspectorController inspector;
    private final SnapshotExportService exports;
    private final WorldDebugRenderer worldRenderer;
    private final FoundationHud hud;
    private long clientTick;
    private boolean registered;

    public ClientRuntime(SawBotConfig config, Logger logger) {
        if(config==null||logger==null)throw new IllegalArgumentException("config/logger");
        this.minecraft=Minecraft.getMinecraft();
        this.config=config;
        this.logger=logger;
        this.keys=new SawBotKeyBindings();
        this.state=new SawBotStateController(minecraft,logger);
        this.tickTiming=new RollingTimingWindow(config.timingWindowSize());
        this.observations=new ObservationPipeline(minecraft,config.sensorIntervalTicks());
        this.inspector=new InspectorController(minecraft);
        this.exports=new SnapshotExportService(minecraft.mcDataDir,logger);
        this.worldRenderer=new WorldDebugRenderer(minecraft,state,inspector,config.timingWindowSize());
        this.hud=new FoundationHud(minecraft,state,tickTiming,observations,inspector,exports,worldRenderer);
    }

    public void register() {
        if(registered)throw new IllegalStateException("ClientRuntime already registered");
        keys.register();
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        registered=true;
    }

    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.START)return;
        long start=System.nanoTime();
        try {
            processSafetyKeysFirst();
            if(minecraft.theWorld==null||minecraft.thePlayer==null){
                if(state.isEnabled())state.onWorldUnavailable();
                observations.tick(clientTick,false);
                inspector.update(null,null);
                return;
            }
            clientTick++;
            boolean singleStep=state.consumeObservationStepRequest();
            observations.tick(clientTick,state.observationsFrozen(),singleStep);
            inspector.update(observations.latest(),observations.previous());
            processInspectorActions();
        } catch(RuntimeException exception) {
            state.emergencyStop();
            logger.error("Unhandled SawBotV1 client tick error; emergency stop applied.",exception);
        } finally {
            tickTiming.add(System.nanoTime()-start);
        }
    }

    private void processSafetyKeysFirst() {
        if(keys.emergencyStop.isPressed()){
            state.emergencyStop();
            drainNonSafetyKeyPresses();
            return;
        }
        if(keys.manualTakeover.isPressed()){
            state.manualTakeover();
            drainNonSafetyKeyPresses();
            return;
        }
        if(minecraft.currentScreen!=null)return;
        if(keys.toggleEnabled.isPressed())state.toggleEnabled();
        if(keys.toggleFreeze.isPressed())state.toggleFrozen();
        if(keys.stepObservation.isPressed())state.requestObservationStep();
        if(keys.toggleInspector.isPressed())state.toggleInspector();
        if(keys.toggleTerrainOverlay.isPressed())state.toggleTerrainOverlay();
        if(keys.toggleCollisionOverlay.isPressed())state.toggleCollisionOverlay();
        if(keys.toggleEntityOverlay.isPressed())state.toggleEntityOverlay();
        if(keys.toggleEntityTracers.isPressed())state.toggleEntityTracers();
        if(keys.toggleLandmarkOverlay.isPressed())state.toggleLandmarkOverlay();
        if(keys.toggleTelemetry.isPressed())state.toggleTelemetryRequest();
    }


    private void drainNonSafetyKeyPresses() {
        drain(keys.toggleEnabled);
        drain(keys.toggleInspector);
        drain(keys.toggleFreeze);
        drain(keys.stepObservation);
        drain(keys.cycleInspectorPage);
        drain(keys.toggleTerrainOverlay);
        drain(keys.toggleCollisionOverlay);
        drain(keys.toggleEntityOverlay);
        drain(keys.toggleEntityTracers);
        drain(keys.toggleLandmarkOverlay);
        drain(keys.previousEntity);
        drain(keys.nextEntity);
        drain(keys.exportSnapshot);
        drain(keys.toggleTelemetry);
    }

    private static void drain(net.minecraft.client.settings.KeyBinding keyBinding) {
        while(keyBinding.isPressed()) {
            // Consume queued lower-priority presses so safety actions cannot be undone next tick.
        }
    }

    private void processInspectorActions() {
        if(minecraft.currentScreen!=null)return;
        ObservationSnapshot snapshot=observations.latest();
        if(keys.cycleInspectorPage.isPressed())inspector.cyclePage();
        if(keys.previousEntity.isPressed())inspector.cycleEntity(snapshot,-1);
        if(keys.nextEntity.isPressed())inspector.cycleEntity(snapshot,1);
        if(keys.exportSnapshot.isPressed()){
            boolean accepted=exports.request(snapshot,inspector.latestDiff(),inspector.selectedBlock(),inspector.selectedTrackingId());
            state.setInspectorNotice(accepted?"snapshot export queued":"snapshot export rejected: "+exports.status());
        }
    }

    @SubscribeEvent public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if(config.hudEnabled())hud.render(clientTick);
    }

    @SubscribeEvent public void onRenderWorldLast(RenderWorldLastEvent event) {
        worldRenderer.render(observations.latest(),event.partialTicks);
    }
}
