package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationDiff;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.InspectorPage;
import dev.fivesaw.sawbot.forge.inspection.SnapshotExportService;
import dev.fivesaw.sawbot.forge.model.ModelBridge;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.actuator.SafeActionActuator;
import dev.fivesaw.sawbot.forge.bridging.BridgingBodyController;
import dev.fivesaw.sawbot.forge.navigation.NavigationBodyController;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import dev.fivesaw.sawbot.forge.telemetry.TelemetryService;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Compact engineering HUD. Long explanations belong in documentation, not over gameplay. */
public final class FoundationHud {
    private static final int WHITE=0xFFFFFF,MUTED=0xA0A0A0,SAFE=0x55FF55,WARNING=0xFFAA00,INFO=0x55FFFF,ERROR=0xFF5555,MODEL=0xFF77FF,ACTION=0xFFCC55;
    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;
    private final ObservationPipeline observations;
    private final InspectorController inspector;
    private final SnapshotExportService exports;
    private final TelemetryService telemetry;
    private final ModelBridge modelBridge;
    private final SafeActionActuator actuator;
    private final NavigationBodyController navigationBody;
    private final BridgingBodyController bridgingBody;
    private final WorldDebugRenderer worldRenderer;
    private final NavigationWaypointController navigationWaypoint;

    public FoundationHud(Minecraft minecraft, SawBotStateController state,
                         RollingTimingWindow tickTiming, ObservationPipeline observations,
                         InspectorController inspector, SnapshotExportService exports,
                         TelemetryService telemetry, ModelBridge modelBridge,
                         SafeActionActuator actuator, NavigationBodyController navigationBody,
                         BridgingBodyController bridgingBody, WorldDebugRenderer worldRenderer,
                         NavigationWaypointController navigationWaypoint) {
        this.minecraft=minecraft; this.state=state; this.tickTiming=tickTiming; this.observations=observations;
        this.inspector=inspector; this.exports=exports; this.telemetry=telemetry; this.modelBridge=modelBridge;
        this.actuator=actuator; this.navigationBody=navigationBody; this.bridgingBody=bridgingBody;
        this.worldRenderer=worldRenderer; this.navigationWaypoint=navigationWaypoint;
    }

    public void render(long clientTick) {
        if(minecraft.fontRendererObj==null)return;
        int x=6,y=6;
        int statusColour=state.isEnabled()?WARNING:SAFE;
        draw("SawBotV1  Phase 9 SEGMENTED NAVIGATION",x,y,WHITE); y+=10;
        draw("State: "+state.mode()+"  scope "+actuator.environmentDescription(),x,y,statusColour); y+=10;
        ObservationSnapshot snapshot=observations.latest();
        if(snapshot==null){draw("Eyes: waiting",x,y,WARNING);y+=10;}
        else{
            long age=observations.snapshotAgeMillis();
            String freezeSuffix=state.observationsFrozen()?"  FROZEN":"";
            draw("Tick "+clientTick+"  Obs #"+snapshot.sequenceNumber()+"  age "+age+" ms"+freezeSuffix,x,y,
                state.observationsFrozen()?INFO:(age>300?WARNING:MUTED)); y+=10;
            if(state.observationsFrozen()){
                draw("Frozen "+one((float)snapshot.selfState().absoluteX())+","+one((float)snapshot.selfState().absoluteY())+","+one((float)snapshot.selfState().absoluteZ())
                    +"  moved "+one(distanceFromSnapshot(snapshot))+"m",x,y,distanceFromSnapshot(snapshot)>2F?WARNING:INFO); y+=10;
            }
            draw("Eyes "+micros(snapshot.sensorTimings().totalNanos())+" us  entities "+snapshot.entities().count()+"  events "+snapshot.events().count(),x,y,MUTED); y+=10;
            draw("HP "+one(snapshot.selfState().health())+"  wool "+snapshot.inventory().wool()+"  ping "+(snapshot.serverTiming().pingValid()?snapshot.serverTiming().estimatedPingMillis()+" ms":"unknown"),x,y,MUTED); y+=10;
        }
        draw("Handler "+micros(tickTiming.averageNanos())+"/"+micros(tickTiming.maximumNanos())+" us  render "+micros(worldRenderer.averageRenderNanos())+"/"+micros(worldRenderer.maximumRenderNanos())+" us",x,y,MUTED); y+=10;
        draw("Brain "+modelBridge.displayState()+"  "+modelBridge.modelVersion()+"  rtt "+millis(modelBridge.latestRoundTripNanos())+" ms  rx "+modelBridge.receivedActions(),x,y,modelBridge.isReady()?MODEL:MUTED); y+=10;
        if(navigationWaypoint.active()||!"IDLE".equals(navigationBody.status())){
            draw("Nav "+navigationBody.status()+"  "+navigationBody.source()+"  "+navigationBody.currentMovementType()+"  op/rem "+navigationBody.pathIndex()+"/"+navigationBody.remainingMovements()+"  seg "+(navigationBody.currentSegmentIndex()+1)+"/"+navigationBody.totalSegments(),x,y,navColour()); y+=10;
            draw("Nav plan/splice/reanchor/corridor "+navigationBody.replanCount()+"/"+navigationBody.hotSwapCount()+"/"+navigationBody.routeReanchors()+"/"+navigationBody.corridorRecoveries()+"  invalid/stuck "+navigationBody.routeInvalidations()+"/"+navigationBody.stuckRecoveries(),x,y,MUTED); y+=10;
            draw("Nav "+tail(navigationBody.reason(),52)+"  worker "+navigationBody.plannerState()+" "+millis(navigationBody.plannerComputeNanos())+"ms cap "+navigationBody.captureProgressPercent()+"% reads/hit "+navigationBody.gridWorldReads()+"/"+navigationBody.gridCacheHits(),x,y,MUTED); y+=10;
        }
        if(bridgingBody.manualIntent()||bridgingBody.brainIntent()||bridgingBody.ownsInputs()||!"IDLE".equals(bridgingBody.status())){
            draw("Bridge "+bridgingBody.status()+"  "+bridgingBody.source()+"  step "+bridgingBody.stepIndex()+"/"+bridgingBody.planSize()+"  slot "+slotDisplay(bridgingBody.selectedBlockSlot()),x,y,bridgeColour()); y+=10;
            draw("Bridge placed/fail/replan/retarget "+bridgingBody.placedBlocks()+"/"+bridgingBody.failedPlacements()+"/"+bridgingBody.replans()+"/"+bridgingBody.retargets()+"  try/wait "+bridgingBody.placementAttempts()+"/"+bridgingBody.confirmationTicks(),x,y,MUTED); y+=10;
            draw("Bridge "+tail(bridgingBody.reason(),72),x,y,MUTED); y+=10;
        }
        if(state.isEnabled()||actuator.activeAction()!=null||actuator.rejectedActions()>0){
            draw("Actuator "+actuator.status()+"  "+actionCompact(actuator.activeAction())+"  "+tail(actuator.lastReason(),42),x,y,"APPLY".equals(actuator.status())?ACTION:MUTED); y+=10;
        }
        draw("P freeze  . step  F7 panel  H page  O export  K telemetry",x,y,MUTED); y+=10;
        draw("B terrain  C collision  N entities  V tracers  M landmarks  G waypoint",x,y,MUTED); y+=10;
        draw("R bridge intent  Shift+R clear bridge intent",x,y,MUTED); y+=10;
        draw("[/] entity  F10 toggle  F9 takeover  F12 emergency",x,y,MUTED);
        if(navigationWaypoint.active()){y+=10;draw("Waypoint #"+NavigationWaypointController.USER_WAYPOINT_ID+"  "+navigationWaypoint.compactPosition()+"  Shift+G clear",x,y,ACTION);}

        String telemetryStatus=telemetry.status();
        if(!"idle".equals(telemetryStatus)){
            y+=10;
            int colour="error".equals(telemetryStatus)?ERROR:MODEL;
            String suffix="error".equals(telemetryStatus)?"  "+tail(telemetry.failureMessage(),58):"  steps "+telemetry.writtenSteps()+"  q "+telemetry.queueSize()+"/"+telemetry.queueCapacity()+"  drop "+telemetry.droppedSteps();
            draw("Telemetry "+telemetryStatus+suffix,x,y,colour);
        }
        String notice=state.inspectorNotice();
        if(!notice.isEmpty()){
            int severity=state.inspectorNoticeSeverity();
            int noticeColour=severity>=3?ERROR:(severity==2?WARNING:(severity==1?ACTION:INFO));
            y+=10;draw(notice,x,y,noticeColour);
        }
        if(state.inspectorVisible()&&snapshot!=null){
            y+=12;
            draw("Inspector "+(inspector.page().ordinal()+1)+"/"+InspectorPage.values().length+" "+inspector.page()+"  validity 0x"+Long.toHexString(snapshot.sensorValidityFlags()),x,y,INFO); y+=10;
            draw("Overlay T/C/E/TR/L "+bit(state.terrainOverlayVisible())+"/"+bit(state.collisionOverlayVisible())+"/"+bit(state.entityOverlayVisible())+"/"+bit(state.entityTracersVisible())+"/"+bit(state.landmarkOverlayVisible()),x,y,MUTED); y+=10;
            y=renderPage(snapshot,x,y);
        }
        if(!"idle".equals(exports.status())){y+=10;draw("Export "+exports.status()+"  q "+exports.queueSize()+"/"+exports.queueCapacity(),x,y,exports.status().contains("failure")||exports.status().contains("rejected")?ERROR:INFO);}
    }

    private int renderPage(ObservationSnapshot snapshot,int x,int y){
        switch(inspector.page()){
            case BODY:return renderBody(snapshot,x,y);
            case TERRAIN:return renderTerrain(snapshot,x,y);
            case ENTITIES:return renderEntities(snapshot,x,y);
            case INVENTORY:return renderInventory(snapshot,x,y);
            case EVENTS:return renderEvents(snapshot,x,y);
            case DIFFERENCE:return renderDifference(snapshot,x,y);
            case MODEL:return renderModel(snapshot,x,y);
            case SYSTEM:return renderSystem(snapshot,x,y);
            case SUMMARY:
            default:return renderSummary(snapshot,x,y);
        }
    }

    private int renderSummary(ObservationSnapshot snapshot,int x,int y){
        SelfState s=snapshot.selfState();
        draw("XYZ "+one((float)s.absoluteX())+" "+one((float)s.absoluteY())+" "+one((float)s.absoluteZ())+"  yaw/pitch "+one(s.yawDegrees())+"/"+one(s.pitchDegrees()),x,y,WHITE); y+=10;
        draw("Support "+one(s.supportDistanceLeft())+"/"+one(s.supportDistanceCenter())+"/"+one(s.supportDistanceRight())+"  void "+one(s.distanceToVoid()),x,y,WHITE); y+=10;
        draw("Terrain changed "+snapshot.localTerrain().changedCellCount()+"  facing "+snapshot.localTerrain().facingQuadrant()+"  map rows "+snapshot.midRangeMap().rowsUpdatedThisTick(),x,y,WHITE); y+=10;
        draw("Inventory Fe/Au/D/E/W "+snapshot.inventory().iron()+"/"+snapshot.inventory().gold()+"/"+snapshot.inventory().diamonds()+"/"+snapshot.inventory().emeralds()+"/"+snapshot.inventory().wool(),x,y,WHITE); y+=10;
        EntityObservation selected=inspector.selectedEntity(snapshot);
        draw(selected==null?"Selected entity: none":"Selected #"+selected.trackingId()+" "+selected.type()+" "+EntityVisualStyle.visibilityToken(selected)+" "+one(selected.distance())+"m",x,y,selected==null?MUTED:EntityVisualStyle.visibilityArgb(selected)); y+=10;
        BlockInspection block=inspector.selectedBlock();
        draw(block==null?"Selected block: none":"Selected block "+block.worldX()+","+block.worldY()+","+block.worldZ()+" "+block.category()+" "+(block.insideTensor()?"cell "+block.terrainIndex():"outside tensor"),x,y,block==null?MUTED:0xFFFF55); y+=10;
        return y;
    }

    private int renderBody(ObservationSnapshot snapshot,int x,int y){
        SelfState s=snapshot.selfState();
        draw("health/absorb/hunger/armour "+one(s.health())+"/"+one(s.absorption())+"/"+one(s.hunger())+"/"+one(s.armour()),x,y,WHITE);y+=10;
        draw("pos "+three(s.absoluteX())+" "+three(s.absoluteY())+" "+three(s.absoluteZ()),x,y,WHITE);y+=10;
        draw("vel R/U/F "+three(s.velocityRight())+"/"+three(s.velocityUp())+"/"+three(s.velocityForward()),x,y,WHITE);y+=10;
        draw("acc R/U/F "+three(s.accelerationRight())+"/"+three(s.accelerationUp())+"/"+three(s.accelerationForward()),x,y,WHITE);y+=10;
        draw("yaw/pitch/fall "+one(s.yawDegrees())+"/"+one(s.pitchDegrees())+"/"+one(s.fallDistance()),x,y,WHITE);y+=10;
        draw("ground/hCol/vCol/liquid/ladder/inside "+bits(s.onGround(),s.horizontalCollision(),s.verticalCollision(),s.inLiquid(),s.onLadder(),s.insideBlock()),x,y,WHITE);y+=10;
        draw("sprint/sneak/use "+bits(s.sprinting(),s.sneaking(),s.usingItem())+"  air "+s.airborneTicks()+" hurt "+s.hurtTimerTicks(),x,y,WHITE);y+=10;
        draw("slot "+s.selectedSlot()+" potions "+s.activePotionCount()+" support "+one(s.supportDistanceLeft())+"/"+one(s.supportDistanceCenter())+"/"+one(s.supportDistanceRight())+" void "+one(s.distanceToVoid()),x,y,WHITE);y+=10;
        return y;
    }

    private int renderTerrain(ObservationSnapshot snapshot,int x,int y){
        LocalTerrainSnapshot terrain=snapshot.localTerrain();
        int[] counts=new int[BlockSemanticCategory.values().length];
        for(byte raw:terrain.categories()){int index=raw&0xFF;if(index<counts.length)counts[index]++;}
        draw("anchor "+terrain.originX()+","+terrain.originY()+","+terrain.originZ()+"  quadrant "+terrain.facingQuadrant()+"  cells "+LocalTerrainSnapshot.CELL_COUNT,x,y,WHITE);y+=10;
        draw("AIR "+counts[BlockSemanticCategory.AIR.ordinal()]+" SOLID "+counts[BlockSemanticCategory.SOLID.ordinal()]+" PARTIAL "+counts[BlockSemanticCategory.PARTIAL.ordinal()]+" UNKNOWN "+counts[BlockSemanticCategory.UNKNOWN.ordinal()],x,y,WHITE);y+=10;
        draw("LIQUID "+counts[BlockSemanticCategory.LIQUID.ordinal()]+" HAZARD "+counts[BlockSemanticCategory.HAZARD.ordinal()]+" CLIMB "+counts[BlockSemanticCategory.CLIMBABLE.ordinal()]+" BED "+counts[BlockSemanticCategory.BED.ordinal()],x,y,WHITE);y+=10;
        draw("CONTAINER "+counts[BlockSemanticCategory.CONTAINER.ordinal()]+" INTERACT "+counts[BlockSemanticCategory.INTERACTABLE.ordinal()]+" PLANT "+counts[BlockSemanticCategory.PLANT.ordinal()]+" DECOR "+counts[BlockSemanticCategory.DECORATION.ordinal()],x,y,WHITE);y+=10;
        BlockInspection block=inspector.selectedBlock();
        if(block==null){draw("block none",x,y,MUTED);y+=10;return y;}
        draw("world "+block.worldX()+","+block.worldY()+","+block.worldZ()+"  R/U/F "+block.rightOffset()+"/"+block.upOffset()+"/"+block.forwardOffset(),x,y,WHITE);y+=10;
        draw("inside "+block.insideTensor()+" index "+block.terrainIndex()+" stateId "+block.blockStateId()+" category "+block.category(),x,y,WHITE);y+=10;
        draw("flags 0x"+Integer.toHexString(block.flags()&0xFFFF)+" collision "+block.collisionHeightClass()+" changed "+terrain.changedCellCount(),x,y,WHITE);y+=10;
        return y;
    }

    private int renderEntities(ObservationSnapshot snapshot,int x,int y){
        EntityObservation e=inspector.selectedEntity(snapshot);
        draw("tracked "+snapshot.entities().count()+" dropped "+snapshot.entities().droppedCount(),x,y,WHITE);y+=10;
        if(e==null){draw("selected none",x,y,MUTED);y+=10;return y;}
        String relation=e.kind()==dev.fivesaw.sawbot.common.observation.EntityKind.PLAYER?" team "+e.teamRelation():"";
        draw("#"+e.trackingId()+" mc#"+e.minecraftEntityId()+" "+e.type()+" ("+e.kind()+") "+EntityVisualStyle.visibilityToken(e)+relation+" conf "+one(e.trackingConfidence()),x,y,EntityVisualStyle.visibilityArgb(e));y+=10;
        draw("R/U/F "+one(e.right())+"/"+one(e.up())+"/"+one(e.forward())+" distance "+one(e.distance()),x,y,WHITE);y+=10;
        draw("velocity "+three(e.velocityRight())+"/"+three(e.velocityUp())+"/"+three(e.velocityForward()),x,y,WHITE);y+=10;
        draw("accel "+three(e.accelerationRight())+"/"+three(e.accelerationUp())+"/"+three(e.accelerationForward()),x,y,WHITE);y+=10;
        draw("yaw/pitch "+one(e.yawDegrees())+"/"+one(e.pitchDegrees())+" size "+one(e.width())+"x"+one(e.height()),x,y,WHITE);y+=10;
        draw("health/armour "+one(e.health())+"/"+one(e.armour())+" held "+itemCategoryName(e.heldItemCategory())+" payload "+itemCategoryName(e.payloadItemCategory())+" hurt "+e.hurtTimerTicks(),x,y,WHITE);y+=10;
        draw("ground/sprint/sneak "+bits(e.onGround(),e.sprinting(),e.sneaking())+" LOS/OCC/attack/load "+bits(e.lineOfSight(),e.occluded(),e.attackable(),e.loaded()),x,y,EntityVisualStyle.visibilityArgb(e));y+=10;
        StringBuilder ids=new StringBuilder("IDs ");for(EntityObservation item:snapshot.entities().entities()){if(ids.length()>90)break;ids.append(item.trackingId()).append(item.lineOfSight()?"L ":"O ");}
        draw(ids.toString(),x,y,MUTED);y+=10;return y;
    }

    private int renderInventory(ObservationSnapshot snapshot,int x,int y){
        InventorySnapshot inv=snapshot.inventory();
        draw("selected "+inv.selectedSlot()+" container "+inv.openContainerType()+" Fe/Au/D/E/W "+inv.iron()+"/"+inv.gold()+"/"+inv.diamonds()+"/"+inv.emeralds()+"/"+inv.wool(),x,y,WHITE);y+=10;
        List<ItemSlotObservation> slots=inv.slots();
        for(int row=0;row<9;row++){
            StringBuilder line=new StringBuilder();
            int start=row*5;
            for(int i=start;i<Math.min(start+5,slots.size());i++){
                ItemSlotObservation slot=slots.get(i);
                if(line.length()>0)line.append(" | ");
                line.append(slot.slotIndex()).append(':').append(shortCategory(slot)).append('x').append(slot.count());
            }
            draw(line.toString(),x,y,row<2?WHITE:MUTED);y+=10;
        }
        return y;
    }

    private int renderEvents(ObservationSnapshot snapshot,int x,int y){
        List<ObservationEvent> events=snapshot.events().events();
        draw("events "+events.size()+" dropped "+snapshot.events().dropped(),x,y,WHITE);y+=10;
        int shown=0;
        for(int index=events.size()-1;index>=0&&shown<10;index--,shown++){
            ObservationEvent event=events.get(index);
            draw("t"+event.clientTick()+" "+event.type()+" id "+event.trackingId()+" mag "+one(event.magnitude())+" ok "+bit(event.success()),x,y,shown<3?WHITE:MUTED);y+=10;
        }
        if(events.isEmpty()){draw("none",x,y,MUTED);y+=10;}
        return y;
    }

    private int renderDifference(ObservationSnapshot snapshot,int x,int y){
        ObservationDiff d=inspector.latestDiff();
        draw("#"+d.fromSequence()+" -> #"+d.toSequence()+" tick +"+d.clientTickDelta(),x,y,WHITE);y+=10;
        draw("movement "+three(d.positionDistance())+" yaw "+one(d.yawDeltaDegrees()),x,y,WHITE);y+=10;
        draw("terrain "+d.terrainChangedCells()+" map "+d.mapChangedColumns()+" inventory "+d.inventoryChangedSlots(),x,y,WHITE);y+=10;
        draw("entities +"+d.entitiesAdded()+" -"+d.entitiesRemoved()+" changed "+d.entitiesChanged(),x,y,WHITE);y+=10;
        draw("validity xor 0x"+Long.toHexString(d.validityChangedBits())+" empty "+d.isEmpty(),x,y,WHITE);y+=10;
        return y;
    }

    private int renderModel(ObservationSnapshot snapshot,int x,int y){
        ActionCommand action=bridgingBody.ownsInputs()?bridgingBody.previousAppliedAction()
            :(navigationBody.shouldOwnNavigation()?navigationBody.previousAppliedAction():actuator.activeAction());
        if(action==null)action=actuator.previousAppliedAction();
        draw("brain "+modelBridge.displayState()+" endpoint "+modelBridge.endpoint()+" model "+modelBridge.modelVersion(),x,y,modelBridge.isReady()?MODEL:MUTED);y+=10;
        draw("tx/rx "+modelBridge.sentObservations()+"/"+modelBridge.receivedActions()+" q "+modelBridge.observationQueueSize()+"/2 "+modelBridge.actionQueueSize()+"/8 rtt "+millis(modelBridge.latestRoundTripNanos())+" ms",x,y,WHITE);y+=10;
        draw("bridge drop obs/action "+modelBridge.droppedObservations()+"/"+modelBridge.droppedActions()+" reconnect "+modelBridge.reconnects()+" invalid "+modelBridge.invalidFrames(),x,y,WHITE);y+=10;
        draw("nav body "+navigationBody.status()+" source "+navigationBody.source()+" own "+bit(navigationBody.ownsMovement())+" movement "+navigationBody.currentMovementType()+" op/rem "+navigationBody.pathIndex()+"/"+navigationBody.remainingMovements(),x,y,navColour());y+=10;
        draw("segment "+(navigationBody.currentSegmentIndex()+1)+"/"+navigationBody.totalSegments()+" next "+bit(navigationBody.nextSegmentAvailable())+" replacement "+bit(navigationBody.replacementPending())+" provisional "+bit(navigationBody.provisionalPath()),x,y,WHITE);y+=10;
        draw("planner "+navigationBody.plannerState()+" submitted/completed/superseded "+navigationBody.plannerSubmitted()+"/"+navigationBody.plannerCompleted()+"/"+navigationBody.plannerSuperseded()+" compute "+millis(navigationBody.plannerComputeNanos())+" ms",x,y,WHITE);y+=10;
        draw("expanded/known "+navigationBody.plannerExpandedNodes()+"/"+navigationBody.plannerKnownNodes()+" capture "+navigationBody.captureProgressPercent()+"% reads/cache/live "+navigationBody.gridWorldReads()+"/"+navigationBody.gridCacheHits()+"/"+navigationBody.gridLiveRefreshes(),x,y,MUTED);y+=10;
        draw("replan/splice/reanchor/corridor "+navigationBody.replanCount()+"/"+navigationBody.hotSwapCount()+"/"+navigationBody.routeReanchors()+"/"+navigationBody.corridorRecoveries()+" invalid/off/stuck/fail "+navigationBody.routeInvalidations()+"/"+navigationBody.offRouteReplans()+"/"+navigationBody.stuckRecoveries()+"/"+navigationBody.failedPlans(),x,y,MUTED);y+=10;
        draw("corridor dev "+one((float)navigationBody.pathDeviation())+"m yaw error "+one(navigationBody.steeringOffsetDegrees())+" stale "+navigationBody.stalePlanResults()+" reason "+tail(navigationBody.reason(),44),x,y,MUTED);y+=10;
        draw("bridge body "+bridgingBody.status()+" source "+bridgingBody.source()+" own "+bit(bridgingBody.ownsInputs())+" step/size "+bridgingBody.stepIndex()+"/"+bridgingBody.planSize()+" slot "+slotDisplay(bridgingBody.selectedBlockSlot()),x,y,bridgeColour());y+=10;
        draw("bridge placed/fail/replan/retarget "+bridgingBody.placedBlocks()+"/"+bridgingBody.failedPlacements()+"/"+bridgingBody.replans()+"/"+bridgingBody.retargets()+" attempt/wait "+bridgingBody.placementAttempts()+"/"+bridgingBody.confirmationTicks(),x,y,MUTED);y+=10;
        draw("bridge reason "+tail(bridgingBody.reason(),72),x,y,MUTED);y+=10;
        draw("fallback actuator "+actuator.status()+" own "+bit(actuator.ownsContinuousInputs())+" accepted/rejected/expired "+actuator.acceptedActions()+"/"+actuator.rejectedActions()+"/"+actuator.expiredActions(),x,y,ACTION);y+=10;
        draw("executed #"+action.observationSequenceNumber()+" controller "+action.modelVersion()+" confidence "+one(action.confidence()),x,y,WHITE);y+=10;
        draw("move F/S "+one(action.forward())+"/"+one(action.strafe())+" camera Y/P "+one(action.yawDeltaDegrees())+"/"+one(action.pitchDeltaDegrees()),x,y,WHITE);y+=10;
        draw("skill "+action.selectedSkill()+" target "+action.selectedTargetTrackingId()+" waypoint "+action.selectedWaypointId()+" objective "+action.tacticalObjective(),x,y,WHITE);y+=10;
        return y;
    }

    private int renderSystem(ObservationSnapshot snapshot,int x,int y){
        SensorTimings t=snapshot.sensorTimings();
        draw("schema "+snapshot.schemaVersion()+" task "+snapshot.taskAdapterIdentifier()+" episode "+shortId(snapshot.episodeId().toString()),x,y,WHITE);y+=10;
        draw("us self "+micros(t.selfNanos())+" terrain "+micros(t.terrainNanos())+" map "+micros(t.midRangeNanos())+" entity "+micros(t.entitiesNanos()),x,y,WHITE);y+=10;
        draw("us inv "+micros(t.inventoryNanos())+" landmark "+micros(t.landmarksNanos())+" events "+micros(t.eventsNanos())+" timing "+micros(t.serverTimingNanos()),x,y,WHITE);y+=10;
        draw("total "+micros(t.totalNanos())+" us interval "+observations.intervalTicks()+" age "+observations.snapshotAgeMillis()+" ms",x,y,WHITE);y+=10;
        draw("handler "+micros(tickTiming.averageNanos())+"/"+micros(tickTiming.maximumNanos())+" us  render "+micros(worldRenderer.averageRenderNanos())+"/"+micros(worldRenderer.maximumRenderNanos())+" us",x,y,WHITE);y+=10;
        draw("telemetry "+telemetry.status()+" steps "+telemetry.writtenSteps()+" q "+telemetry.queueSize()+"/"+telemetry.queueCapacity()+" drop/enc "+telemetry.droppedSteps()+"/"+telemetry.encodingRejectedSteps(),x,y,telemetry.isRecording()?MODEL:("error".equals(telemetry.status())?ERROR:WHITE));y+=10;
        if(!telemetry.failureMessage().isEmpty()){draw("telemetry error "+tail(telemetry.failureMessage(),100),x,y,ERROR);y+=10;}
        draw("input buffered/dropped "+telemetry.bufferedInputSamples()+"/"+telemetry.droppedInputSamples(),x,y,WHITE);y+=10;
        if(!telemetry.latestFile().isEmpty()){draw("trajectory "+tail(telemetry.latestFile(),100),x,y,MUTED);y+=10;}
        String exportStatus=exports.status();
        draw("export "+exportStatus+" q "+exports.queueSize()+"/"+exports.queueCapacity()+" rejected "+exports.rejectedCount(),x,y,WHITE);y+=10;
        if(!exports.latestFile().isEmpty()){draw("snapshot "+tail(exports.latestFile(),100),x,y,MUTED);y+=10;}
        return y;
    }

    private float distanceFromSnapshot(ObservationSnapshot snapshot){
        if(minecraft.thePlayer==null)return 0F;
        double dx=minecraft.thePlayer.posX-snapshot.selfState().absoluteX();
        double dy=minecraft.thePlayer.posY-snapshot.selfState().absoluteY();
        double dz=minecraft.thePlayer.posZ-snapshot.selfState().absoluteZ();
        return (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
    }

    private static String actionCompact(ActionCommand action){
        if(action==null)return"none";
        StringBuilder bits=new StringBuilder();
        if(action.jumpProbability()>=0.5F)bits.append('J');
        if(action.sprintProbability()>=0.5F)bits.append('S');
        if(action.sneakProbability()>=0.5F)bits.append('N');
        if(action.attackProbability()>=0.5F)bits.append('A');
        if(action.useOrPlaceProbability()>=0.5F)bits.append('U');
        if(action.dropProbability()>=0.5F)bits.append('D');
        return "#"+action.observationSequenceNumber()+" f"+one(action.forward())+" s"+one(action.strafe())+" y"+one(action.yawDeltaDegrees())+" "+(bits.length()==0?"-":bits.toString());
    }
    private int navColour(){
        String value=navigationBody.status();
        if("ARRIVED".equals(value))return SAFE;
        if("NO_PATH".equals(value)||"BLOCKED".equals(value))return ERROR;
        if("RECOVER".equals(value)||"PLANNING".equals(value)||"REPLAN".equals(value)||"FOLLOW+REPLAN".equals(value)||"ANYTIME".equals(value))return WARNING;
        return ACTION;
    }
    private int bridgeColour(){
        String value=bridgingBody.status();
        if("COMPLETE".equals(value)||"CONFIRMED".equals(value))return SAFE;
        if("BLOCKED".equals(value)||"OUT_OF_BLOCKS".equals(value)||"AIM_BLOCKED".equals(value))return ERROR;
        if("ALIGN".equals(value)||"PLACE".equals(value)||"CONFIRM".equals(value)||"PLAN".equals(value))return WARNING;
        return ACTION;
    }
    private static String slotDisplay(int zeroBased){return zeroBased<0?"-":Integer.toString(zeroBased+1);}
    private static long millis(long nanos){return nanos<=0L?0L:nanos/1_000_000L;}

    private static String itemCategoryName(int ordinal){
        dev.fivesaw.sawbot.common.observation.ItemCategory[] values=dev.fivesaw.sawbot.common.observation.ItemCategory.values();
        return ordinal>=0&&ordinal<values.length?values[ordinal].name():"INVALID("+ordinal+")";
    }

    private static String shortCategory(ItemSlotObservation slot){String value=slot.category().name();return value.length()>7?value.substring(0,7):value;}
    private static String bits(boolean... values){StringBuilder out=new StringBuilder();for(boolean value:values)out.append(value?'1':'0');return out.toString();}
    private static String bit(boolean value){return value?"1":"0";}
    private static String shortId(String value){return value.length()>12?value.substring(0,12):value;}
    private static String tail(String value,int max){return value.length()<=max?value:"..."+value.substring(value.length()-max+3);}
    private void draw(String text,int x,int y,int colour){minecraft.fontRendererObj.drawStringWithShadow(text,x,y,colour);}
    private static long micros(long nanos){return nanos/1_000L;}
    private static String one(float value){if(Float.isNaN(value))return"NaN";if(Float.isInfinite(value))return value>0?"Inf":"-Inf";boolean negative=value<0f;float absolute=Math.abs(value);long scaled=Math.round(absolute*10f);return(negative?"-":"")+(scaled/10L)+"."+(scaled%10L);}
    private static String three(double value){boolean negative=value<0;double absolute=Math.abs(value);long scaled=Math.round(absolute*1000.0);return(negative?"-":"")+(scaled/1000L)+"."+pad3(scaled%1000L);}
    private static String pad3(long value){if(value<10)return"00"+value;if(value<100)return"0"+value;return Long.toString(value);}
}
