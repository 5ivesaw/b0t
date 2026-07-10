package dev.fivesaw.sawbot.forge.hud;

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
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class FoundationHud {
    private static final int WHITE=0xFFFFFF,MUTED=0xA0A0A0,SAFE=0x55FF55,WARNING=0xFFAA00,INFO=0x55FFFF,ERROR=0xFF5555;
    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;
    private final ObservationPipeline observations;
    private final InspectorController inspector;
    private final SnapshotExportService exports;
    private final WorldDebugRenderer worldRenderer;

    public FoundationHud(Minecraft minecraft, SawBotStateController state,
                         RollingTimingWindow tickTiming, ObservationPipeline observations,
                         InspectorController inspector, SnapshotExportService exports,
                         WorldDebugRenderer worldRenderer) {
        this.minecraft=minecraft; this.state=state; this.tickTiming=tickTiming; this.observations=observations;
        this.inspector=inspector; this.exports=exports; this.worldRenderer=worldRenderer;
    }

    public void render(long clientTick) {
        if(minecraft.fontRendererObj==null)return;
        int x=6,y=6;
        int statusColour=state.isEnabled()?WARNING:SAFE;
        draw("SawBotV1  Phase 2",x,y,WHITE); y+=10;
        draw("State: "+state.mode(),x,y,statusColour); y+=10;
        ObservationSnapshot snapshot=observations.latest();
        if(snapshot==null){draw("Eyes: waiting for a world snapshot",x,y,WARNING);y+=10;}
        else{
            long age=observations.snapshotAgeMillis();
            String freezeSuffix=state.observationsFrozen()?"  FROZEN":"";
            draw("Tick "+clientTick+"  Obs #"+snapshot.sequenceNumber()+"  age "+age+" ms"+freezeSuffix,x,y,
                state.observationsFrozen()?INFO:(age>300?WARNING:MUTED)); y+=10;
            draw("Eyes "+micros(snapshot.sensorTimings().totalNanos())+" us  entities "+snapshot.entities().count()+"  events "+snapshot.events().count(),x,y,MUTED); y+=10;
            draw("HP "+one(snapshot.selfState().health())+"  wool "+snapshot.inventory().wool()+"  ping "+(snapshot.serverTiming().pingValid()?snapshot.serverTiming().estimatedPingMillis()+" ms":"unknown"),x,y,MUTED); y+=10;
        }
        draw("Handler avg/max "+micros(tickTiming.averageNanos())+"/"+micros(tickTiming.maximumNanos())+" us  render "+micros(worldRenderer.averageRenderNanos())+"/"+micros(worldRenderer.maximumRenderNanos())+" us",x,y,MUTED); y+=10;
        draw("P freeze  . step  F7 panel  H page  O export",x,y,MUTED); y+=10;
        draw("B terrain  C collision  N entities  M landmarks  [/] select",x,y,MUTED); y+=10;
        draw("F10 toggle  F9 takeover  F12 emergency",x,y,MUTED);

        if(!state.inspectorNotice().isEmpty()){y+=10;draw(state.inspectorNotice(),x,y,INFO);}
        if(state.inspectorVisible()&&snapshot!=null){
            y+=12;
            draw("Inspector "+inspector.page()+"  validity 0x"+Long.toHexString(snapshot.sensorValidityFlags()),x,y,INFO); y+=10;
            draw("Overlay T/C/E/L "+bit(state.terrainOverlayVisible())+"/"+bit(state.collisionOverlayVisible())+"/"+bit(state.entityOverlayVisible())+"/"+bit(state.landmarkOverlayVisible()),x,y,MUTED); y+=10;
            y=renderPage(snapshot,x,y);
        }
        if(!"idle".equals(exports.status())){y+=10;draw("Export: "+exports.status()+"  queue "+exports.queueSize()+"/"+exports.queueCapacity(),x,y,exports.status().contains("failure")||exports.status().contains("rejected")?ERROR:INFO);}
        if(state.telemetryRequested()){y+=10;draw("Telemetry intent ON; writer remains locked until Phase 3",x,y,WARNING);}
    }

    private int renderPage(ObservationSnapshot snapshot,int x,int y){
        switch(inspector.page()){
            case BODY:return renderBody(snapshot,x,y);
            case TERRAIN:return renderTerrain(snapshot,x,y);
            case ENTITIES:return renderEntities(snapshot,x,y);
            case INVENTORY:return renderInventory(snapshot,x,y);
            case EVENTS:return renderEvents(snapshot,x,y);
            case DIFFERENCE:return renderDifference(snapshot,x,y);
            case SYSTEM:return renderSystem(snapshot,x,y);
            case SUMMARY:
            default:return renderSummary(snapshot,x,y);
        }
    }

    private int renderSummary(ObservationSnapshot snapshot,int x,int y){
        SelfState s=snapshot.selfState();
        draw("XYZ "+one((float)s.absoluteX())+" "+one((float)s.absoluteY())+" "+one((float)s.absoluteZ())+"  yaw/pitch "+one(s.yawDegrees())+"/"+one(s.pitchDegrees()),x,y,WHITE); y+=10;
        draw("Support L/C/R "+one(s.supportDistanceLeft())+"/"+one(s.supportDistanceCenter())+"/"+one(s.supportDistanceRight())+"  void "+one(s.distanceToVoid()),x,y,WHITE); y+=10;
        draw("Terrain changed "+snapshot.localTerrain().changedCellCount()+"  facing "+snapshot.localTerrain().facingQuadrant()+"  map rows/tick "+snapshot.midRangeMap().rowsUpdatedThisTick(),x,y,WHITE); y+=10;
        draw("Inventory Fe/Au/D/E/W "+snapshot.inventory().iron()+"/"+snapshot.inventory().gold()+"/"+snapshot.inventory().diamonds()+"/"+snapshot.inventory().emeralds()+"/"+snapshot.inventory().wool(),x,y,WHITE); y+=10;
        EntityObservation selected=inspector.selectedEntity(snapshot);
        draw(selected==null?"Selected entity: none":"Selected entity #"+selected.trackingId()+" "+selected.kind()+" "+(selected.lineOfSight()?"LOS":"OCC")+" distance "+one(selected.distance()),x,y,selected==null?MUTED:WHITE); y+=10;
        BlockInspection block=inspector.selectedBlock();
        draw(block==null?"Selected block: aim at a block":"Selected block "+block.worldX()+","+block.worldY()+","+block.worldZ()+" "+block.category()+" "+(block.insideTensor()?"cell "+block.terrainIndex():"outside tensor"),x,y,block==null?MUTED:WHITE); y+=10;
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
        draw("origin "+terrain.originX()+","+terrain.originY()+","+terrain.originZ()+" quadrant "+terrain.facingQuadrant()+" cells "+LocalTerrainSnapshot.CELL_COUNT,x,y,WHITE);y+=10;
        draw("AIR "+counts[BlockSemanticCategory.AIR.ordinal()]+" SOLID "+counts[BlockSemanticCategory.SOLID.ordinal()]+" PARTIAL "+counts[BlockSemanticCategory.PARTIAL.ordinal()]+" UNKNOWN "+counts[BlockSemanticCategory.UNKNOWN.ordinal()],x,y,WHITE);y+=10;
        draw("LIQUID "+counts[BlockSemanticCategory.LIQUID.ordinal()]+" HAZARD "+counts[BlockSemanticCategory.HAZARD.ordinal()]+" CLIMB "+counts[BlockSemanticCategory.CLIMBABLE.ordinal()]+" BED "+counts[BlockSemanticCategory.BED.ordinal()],x,y,WHITE);y+=10;
        draw("CONTAINER "+counts[BlockSemanticCategory.CONTAINER.ordinal()]+" INTERACT "+counts[BlockSemanticCategory.INTERACTABLE.ordinal()]+" PLANT "+counts[BlockSemanticCategory.PLANT.ordinal()]+" DECOR "+counts[BlockSemanticCategory.DECORATION.ordinal()],x,y,WHITE);y+=10;
        BlockInspection block=inspector.selectedBlock();
        if(block==null){draw("Aim at a block to decode its model cell.",x,y,MUTED);y+=10;return y;}
        draw("world "+block.worldX()+","+block.worldY()+","+block.worldZ()+" offset R/U/F "+block.rightOffset()+"/"+block.upOffset()+"/"+block.forwardOffset(),x,y,WHITE);y+=10;
        draw("inside "+block.insideTensor()+" index "+block.terrainIndex()+" stateId "+block.blockStateId()+" category "+block.category(),x,y,WHITE);y+=10;
        draw("flags 0x"+Integer.toHexString(block.flags()&0xFFFF)+" collisionClass "+block.collisionHeightClass()+" changedTotal "+terrain.changedCellCount(),x,y,WHITE);y+=10;
        return y;
    }

    private int renderEntities(ObservationSnapshot snapshot,int x,int y){
        EntityObservation e=inspector.selectedEntity(snapshot);
        draw("tracked "+snapshot.entities().count()+" droppedByBound "+snapshot.entities().droppedCount()+"  use [ and ]",x,y,WHITE);y+=10;
        if(e==null){draw("No selected entity.",x,y,MUTED);y+=10;return y;}
        draw("#"+e.trackingId()+" mc#"+e.minecraftEntityId()+" "+e.kind()+" team "+e.teamRelation()+" conf "+one(e.trackingConfidence()),x,y,WHITE);y+=10;
        draw("R/U/F "+one(e.right())+"/"+one(e.up())+"/"+one(e.forward())+" distance "+one(e.distance()),x,y,WHITE);y+=10;
        draw("velocity "+three(e.velocityRight())+"/"+three(e.velocityUp())+"/"+three(e.velocityForward()),x,y,WHITE);y+=10;
        draw("accel "+three(e.accelerationRight())+"/"+three(e.accelerationUp())+"/"+three(e.accelerationForward()),x,y,WHITE);y+=10;
        draw("yaw/pitch "+one(e.yawDegrees())+"/"+one(e.pitchDegrees())+" size "+one(e.width())+"x"+one(e.height()),x,y,WHITE);y+=10;
        draw("health/armour "+one(e.health())+"/"+one(e.armour())+" heldCat "+e.heldItemCategory()+" hurt "+e.hurtTimerTicks(),x,y,WHITE);y+=10;
        draw("ground/sprint/sneak "+bits(e.onGround(),e.sprinting(),e.sneaking())+" LOS/OCC/attack/load "+bits(e.lineOfSight(),e.occluded(),e.attackable(),e.loaded()),x,y,WHITE);y+=10;
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
        draw("events "+events.size()+" dropped "+snapshot.events().dropped()+"  newest first below",x,y,WHITE);y+=10;
        int shown=0;
        for(int index=events.size()-1;index>=0&&shown<10;index--,shown++){
            ObservationEvent event=events.get(index);
            draw("t"+event.clientTick()+" "+event.type()+" id "+event.trackingId()+" mag "+one(event.magnitude())+" ok "+bit(event.success()),x,y,shown<3?WHITE:MUTED);y+=10;
        }
        if(events.isEmpty()){draw("No events in the bounded history.",x,y,MUTED);y+=10;}
        return y;
    }

    private int renderDifference(ObservationSnapshot snapshot,int x,int y){
        ObservationDiff d=inspector.latestDiff();
        draw("compare #"+d.fromSequence()+" -> #"+d.toSequence()+" tick delta "+d.clientTickDelta(),x,y,WHITE);y+=10;
        draw("movement "+three(d.positionDistance())+" yaw delta "+one(d.yawDeltaDegrees()),x,y,WHITE);y+=10;
        draw("terrain cells "+d.terrainChangedCells()+" map columns "+d.mapChangedColumns()+" inventory slots "+d.inventoryChangedSlots(),x,y,WHITE);y+=10;
        draw("entities +"+d.entitiesAdded()+" -"+d.entitiesRemoved()+" changed "+d.entitiesChanged(),x,y,WHITE);y+=10;
        draw("validity xor 0x"+Long.toHexString(d.validityChangedBits())+" empty "+d.isEmpty(),x,y,WHITE);y+=10;
        draw("Current snapshot #"+snapshot.sequenceNumber()+" is compared with the immediately previous captured snapshot.",x,y,MUTED);y+=10;
        return y;
    }

    private int renderSystem(ObservationSnapshot snapshot,int x,int y){
        SensorTimings t=snapshot.sensorTimings();
        draw("schema "+snapshot.schemaVersion()+" task "+snapshot.taskAdapterIdentifier()+" episode "+shortId(snapshot.episodeId().toString()),x,y,WHITE);y+=10;
        draw("us self "+micros(t.selfNanos())+" terrain "+micros(t.terrainNanos())+" map "+micros(t.midRangeNanos())+" entity "+micros(t.entitiesNanos()),x,y,WHITE);y+=10;
        draw("us inv "+micros(t.inventoryNanos())+" landmark "+micros(t.landmarksNanos())+" events "+micros(t.eventsNanos())+" timing "+micros(t.serverTimingNanos()),x,y,WHITE);y+=10;
        draw("total "+micros(t.totalNanos())+" us interval "+observations.intervalTicks()+" ticks snapshot age "+observations.snapshotAgeMillis()+" ms",x,y,WHITE);y+=10;
        draw("handler avg/max "+micros(tickTiming.averageNanos())+"/"+micros(tickTiming.maximumNanos())+" us",x,y,WHITE);y+=10;
        draw("world-render avg/max "+micros(worldRenderer.averageRenderNanos())+"/"+micros(worldRenderer.maximumRenderNanos())+" us",x,y,WHITE);y+=10;
        draw("export "+exports.status()+" queue "+exports.queueSize()+"/"+exports.queueCapacity()+" rejected "+exports.rejectedCount(),x,y,WHITE);y+=10;
        if(!exports.latestFile().isEmpty()){draw("last file "+tail(exports.latestFile(),100),x,y,MUTED);y+=10;}
        return y;
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
