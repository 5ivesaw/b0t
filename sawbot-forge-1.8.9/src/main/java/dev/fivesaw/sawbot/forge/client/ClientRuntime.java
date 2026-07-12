package dev.fivesaw.sawbot.forge.client;

import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.forge.SawBotMod;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.actuator.PhysicalInputMonitor;
import dev.fivesaw.sawbot.forge.actuator.SafeActionActuator;
import dev.fivesaw.sawbot.forge.config.SawBotConfig;
import dev.fivesaw.sawbot.forge.hud.FoundationHud;
import dev.fivesaw.sawbot.forge.hud.WorldDebugRenderer;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.SnapshotExportService;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.model.ModelBridge;
import dev.fivesaw.sawbot.forge.navigation.NavigationBodyController;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import dev.fivesaw.sawbot.forge.telemetry.TelemetryService;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.apache.logging.log4j.Logger;

public final class ClientRuntime {
    private final Minecraft minecraft;
    private final SawBotConfig config;
    private final Logger logger;
    private final SawBotKeyBindings keys;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;
    private final NavigationWaypointController navigationWaypoint;
    private final ObservationPipeline observations;
    private final InspectorController inspector;
    private final SnapshotExportService exports;
    private final TelemetryService telemetry;
    private final EnvironmentGuard environment;
    private final PhysicalInputMonitor physicalInput;
    private final ModelBridge modelBridge;
    private final SafeActionActuator actuator;
    private final NavigationBodyController navigationBody;
    private final WorldDebugRenderer worldRenderer;
    private final FoundationHud hud;
    private long clientTick;
    private long lastPublishedObservationSequence = -1L;
    private boolean registered;

    public ClientRuntime(SawBotConfig config, Logger logger) {
        if (config == null || logger == null) throw new IllegalArgumentException("config/logger");
        this.minecraft = Minecraft.getMinecraft();
        this.config = config;
        this.logger = logger;
        this.keys = new SawBotKeyBindings();
        this.state = new SawBotStateController(minecraft, logger);
        this.tickTiming = new RollingTimingWindow(config.timingWindowSize());
        this.navigationWaypoint = new NavigationWaypointController(minecraft);
        this.observations = new ObservationPipeline(minecraft, config.sensorIntervalTicks(), navigationWaypoint);
        this.inspector = new InspectorController(minecraft);
        this.exports = new SnapshotExportService(minecraft.mcDataDir, logger);
        this.telemetry = new TelemetryService(minecraft.mcDataDir, minecraft,
            config.telemetryQueueCapacity(), config.telemetryInputWindowCapacity(),
            config.telemetryCompressionLevel(), logger);
        this.environment = new EnvironmentGuard(minecraft, config.actuatorAllowSingleplayer(),
            config.actuatorAllowedServers());
        this.physicalInput = new PhysicalInputMonitor(minecraft);
        this.modelBridge = new ModelBridge(config.modelHost(), config.modelPort(),
            config.modelConnectTimeoutMillis(), config.modelReconnectDelayMillis(),
            SawBotMod.VERSION, config.modelDecisionRateHz(), logger);
        this.actuator = new SafeActionActuator(minecraft, state, environment,
            config.actionMaximumAgeMillis(), config.actionMaximumSequenceLag(), logger);
        this.navigationBody = new NavigationBodyController(minecraft, state, environment,
            navigationWaypoint, config.navigationHorizontalRadius(), config.navigationVerticalRadius(),
            config.navigationMaximumExpandedNodes(), config.navigationExpansionsPerTick(),
            config.navigationReplanIntervalTicks(), config.navigationStuckWindowTicks(),
            config.navigationMaximumTurnDegreesPerTick(), config.navigationArrivalRadius(),
            config.navigationLookaheadNodes(), config.navigationLookaheadDistance(),
            config.navigationPathValidationNodes(), config.navigationOffRouteDistance(),
            config.navigationReactiveProbeDistance(), logger);
        this.worldRenderer = new WorldDebugRenderer(minecraft, state, inspector, navigationBody,
            config.timingWindowSize());
        this.hud = new FoundationHud(minecraft, state, tickTiming, observations, inspector,
            exports, telemetry, modelBridge, actuator, navigationBody, worldRenderer, navigationWaypoint);
    }

    public void register() {
        if (registered) throw new IllegalStateException("ClientRuntime already registered");
        keys.register();
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        registered = true;
    }

    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            telemetry.captureHumanInput(clientTick);
            return;
        }
        if (event.phase != TickEvent.Phase.START) return;
        long start = System.nanoTime();
        try {
            processSafetyKeysFirst();
            if (minecraft.theWorld == null || minecraft.thePlayer == null) {
                if (state.isEnabled()) state.onWorldUnavailable();
                actuator.release("world unavailable");
                navigationBody.onWorldUnavailable();
                state.clearTelemetryRequest();
                telemetry.onWorldUnavailable();
                navigationWaypoint.onWorldUnavailable();
                observations.tick(clientTick, false);
                inspector.update(null, null);
                lastPublishedObservationSequence = -1L;
                return;
            }
            clientTick++;
            if (config.physicalInputTakeover() && state.isEnabled()
                && physicalInput.hasTakeoverInput()) {
                state.manualTakeover();
                actuator.release("physical human input");
                navigationBody.release("physical human input");
                state.setInspectorNotice("TAKEOVER: physical input", 2);
            }

            boolean singleStep = state.consumeObservationStepRequest();
            boolean immediateRefresh = state.consumeObservationRefreshRequest();
            observations.tick(clientTick, state.observationsFrozen(), singleStep || immediateRefresh);
            ObservationSnapshot latest = observations.latest();
            inspector.update(latest, observations.previous());

            if (latest != null && latest.sequenceNumber() != lastPublishedObservationSequence) {
                modelBridge.offerObservation(latest);
                lastPublishedObservationSequence = latest.sequenceNumber();
            }

            telemetry.synchronizeRequested(state.telemetryRequested(), latest);
            telemetry.onObservation(latest);

            ModelActionEnvelope action = modelBridge.pollLatestAction();
            navigationBody.observeBrainAction(action);
            if (state.isEnabled() && navigationBody.shouldOwnNavigation()) {
                if (actuator.ownsContinuousInputs() || actuator.activeAction() != null) {
                    actuator.release("navigation body priority");
                }
                navigationBody.tick(clientTick, latest);
                observations.setPreviousAppliedAction(navigationBody.previousAppliedAction());
            } else {
                navigationBody.tick(clientTick, latest);
                if (state.isEnabled() && !modelBridge.isReady()) {
                    state.disableAndRelease("model disconnected");
                    actuator.release("model disconnected");
                    state.setInspectorNotice("model offline; set G waypoint or start brain", 2);
                } else {
                    actuator.tick(latest, action);
                    observations.setPreviousAppliedAction(actuator.previousAppliedAction());
                }
            }
            processInspectorActions();
        } catch (RuntimeException exception) {
            state.emergencyStop();
            actuator.release("client tick exception");
            navigationBody.release("client tick exception");
            state.clearTelemetryRequest();
            telemetry.onWorldUnavailable();
            logger.error("Unhandled SawBotV1 client tick error; emergency stop applied.", exception);
        } finally {
            tickTiming.add(System.nanoTime() - start);
        }
    }

    private void processSafetyKeysFirst() {
        if (keys.emergencyStop.isPressed()) {
            state.emergencyStop();
            actuator.release("emergency stop");
            navigationBody.release("emergency stop");
            drainNonSafetyKeyPresses();
            return;
        }
        if (keys.manualTakeover.isPressed()) {
            state.manualTakeover();
            actuator.release("manual takeover");
            navigationBody.release("manual takeover");
            drainNonSafetyKeyPresses();
            return;
        }
        if (minecraft.currentScreen != null) return;
        if (keys.toggleEnabled.isPressed()) {
            if (state.isEnabled()) {
                state.disableAndRelease("toggle disable");
                actuator.release("toggle disable");
                navigationBody.release("toggle disable");
                state.setInspectorNotice("SAWBOT DISABLED: manual control", 1);
            } else if (!environment.isAllowed()) {
                state.setInspectorNotice("actuator blocked: " + environment.description(), 3);
            } else if (navigationWaypoint.active()) {
                physicalInput.arm();
                state.enable();
                state.setInspectorNotice("NAV ENABLED: deterministic body", 1);
            } else if (!modelBridge.isReady()) {
                state.setInspectorNotice("set G waypoint or start brain at " + modelBridge.endpoint(), 2);
            } else {
                physicalInput.arm();
                state.enable();
                state.setInspectorNotice("BRAIN ENABLED: " + modelBridge.modelVersion(), 1);
            }
        }
        if (keys.toggleFreeze.isPressed()) {
            state.toggleFrozen();
            actuator.release("observation freeze changed");
            navigationBody.release("observation freeze changed");
        }
        if (keys.stepObservation.isPressed()) state.requestObservationStep();
        if (keys.toggleInspector.isPressed()) state.toggleInspector();
        if (keys.toggleTerrainOverlay.isPressed()) state.toggleTerrainOverlay();
        if (keys.toggleCollisionOverlay.isPressed()) state.toggleCollisionOverlay();
        if (keys.toggleEntityOverlay.isPressed()) state.toggleEntityOverlay();
        if (keys.toggleEntityTracers.isPressed()) state.toggleEntityTracers();
        if (keys.toggleLandmarkOverlay.isPressed()) state.toggleLandmarkOverlay();
        if (keys.toggleTelemetry.isPressed()) {
            state.toggleTelemetryRequest();
        }
        if (keys.setNavigationWaypoint.isPressed()) {
            boolean clear = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            if (clear) {
                navigationWaypoint.clear();
                state.setInspectorNotice("waypoint cleared", 1);
            } else if (navigationWaypoint.setFromCrosshair()) {
                state.setInspectorNotice("waypoint #" + NavigationWaypointController.USER_WAYPOINT_ID
                    + " set " + navigationWaypoint.compactPosition(), 1);
            } else {
                state.setInspectorNotice("aim at a block, then press G", 2);
            }
        }
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
        drain(keys.setNavigationWaypoint);
    }

    private static void drain(net.minecraft.client.settings.KeyBinding keyBinding) {
        while (keyBinding.isPressed()) {
            // Consume queued lower-priority presses so safety actions cannot be undone next tick.
        }
    }

    private void processInspectorActions() {
        if (minecraft.currentScreen != null) return;
        ObservationSnapshot snapshot = observations.latest();
        if (keys.cycleInspectorPage.isPressed()) inspector.cyclePage();
        if (keys.previousEntity.isPressed()) inspector.cycleEntity(snapshot, -1);
        if (keys.nextEntity.isPressed()) inspector.cycleEntity(snapshot, 1);
        if (keys.exportSnapshot.isPressed()) {
            boolean accepted = exports.request(snapshot, inspector.latestDiff(), inspector.selectedBlock(),
                inspector.selectedTrackingId());
            state.setInspectorNotice(accepted ? "snapshot export queued"
                : "snapshot export rejected: " + exports.status());
        }
    }

    @SubscribeEvent public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (config.hudEnabled()) hud.render(clientTick);
    }

    @SubscribeEvent public void onRenderWorldLast(RenderWorldLastEvent event) {
        worldRenderer.render(observations.latest(), event.partialTicks);
    }
}
