package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.events.EventHistorySnapshot;
import dev.fivesaw.sawbot.common.observation.EntitySetSnapshot;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.LandmarkSetSnapshot;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.MidRangeMapSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.common.observation.SensorValidity;
import dev.fivesaw.sawbot.common.observation.ServerTimingSnapshot;
import dev.fivesaw.sawbot.common.observation.TaskStateSnapshot;
import dev.fivesaw.sawbot.forge.map.LandmarkSensor;
import dev.fivesaw.sawbot.forge.tracking.EntityTrackerSensor;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;

public final class ObservationPipeline {
    private final Minecraft minecraft;
    private final int intervalTicks;
    private final BlockSemanticClassifier classifier=new BlockSemanticClassifier();
    private final InventorySensor inventorySensor=new InventorySensor();
    private final LandmarkSensor landmarkSensor=new LandmarkSensor();
    private SelfStateSensor selfSensor;
    private LocalTerrainSensor terrainSensor;
    private MidRangeMapSensor midRangeSensor;
    private EntityTrackerSensor entitySensor;
    private EventSensor eventSensor;
    private ServerTimingSensor timingSensor;
    private UUID episodeId=new UUID(0L,0L);
    private String worldIdentifier="none";
    private long sequenceNumber;
    private volatile ObservationSnapshot latest;
    private volatile ObservationSnapshot previous;
    private ActionCommand previousAppliedAction = ActionCommand.zero(0L, 0L, "none/0");

    public ObservationPipeline(Minecraft minecraft,int intervalTicks){if(minecraft==null||intervalTicks<1||intervalTicks>20)throw new IllegalArgumentException("pipeline");this.minecraft=minecraft;this.intervalTicks=intervalTicks;resetSensors();}

    public void tick(long clientTick, boolean frozen) { tick(clientTick, frozen, false); }

    public void tick(long clientTick, boolean frozen, boolean singleStep) {
        EntityPlayerSP player = minecraft.thePlayer;
        World world = minecraft.theWorld;
        if (player == null || world == null) { clearWorld(); return; }
        String identity = worldIdentifier(world);
        if (!identity.equals(worldIdentifier)) {
            worldIdentifier = identity; episodeId = UUID.randomUUID(); sequenceNumber = 0; resetSensors();
        }
        if (frozen && !singleStep) return;
        midRangeSensor.update(player, world, clientTick);
        if (!singleStep && clientTick % intervalTicks != 0) return;
        capture(clientTick, player, world);
    }

    private void capture(long clientTick,EntityPlayerSP player,World world){long totalStart=System.nanoTime();long start=totalStart;SelfState self=selfSensor.capture(player,world);long selfNanos=elapsed(start);
        start=System.nanoTime();LocalTerrainSnapshot terrain=terrainSensor.capture(player,world);long terrainNanos=elapsed(start);
        start=System.nanoTime();MidRangeMapSnapshot midRange=midRangeSensor.snapshot();long midRangeNanos=elapsed(start);
        start=System.nanoTime();EntitySetSnapshot entities=entitySensor.capture(player,world,clientTick);long entitiesNanos=elapsed(start);
        start=System.nanoTime();InventorySnapshot inventory=inventorySensor.capture(minecraft,player);long inventoryNanos=elapsed(start);
        start=System.nanoTime();LandmarkSetSnapshot landmarks=landmarkSensor.capture(player,world,clientTick);long landmarksNanos=elapsed(start);
        start=System.nanoTime();EventHistorySnapshot events=eventSensor.capture(clientTick,self,inventory,entities,terrain);long eventsNanos=elapsed(start);
        start=System.nanoTime();ServerTimingSnapshot serverTiming=timingSensor.capture(minecraft,player,entities,events,clientTick);long serverTimingNanos=elapsed(start);
        long timestamp=System.nanoTime();long nextSequence=++sequenceNumber;ActionCommand previousAction=previousAppliedAction;long totalNanos=elapsed(totalStart);
        SensorTimings timings=new SensorTimings(selfNanos,terrainNanos,midRangeNanos,entitiesNanos,inventoryNanos,landmarksNanos,eventsNanos,serverTimingNanos,totalNanos);
        ObservationSnapshot next = new ObservationSnapshot(clientTick,timestamp,episodeId,nextSequence,worldIdentifier,"universal/0.1",self,terrain,midRange,entities,inventory,landmarks,events,serverTiming,TaskStateSnapshot.UNIVERSAL,previousAction,SensorValidity.ALL_PHASE1,timings);
        this.previous = latest;
        latest = next;
    }

    private void resetSensors(){selfSensor=new SelfStateSensor();terrainSensor=new LocalTerrainSensor(classifier);midRangeSensor=new MidRangeMapSensor();entitySensor=new EntityTrackerSensor();eventSensor=new EventSensor();timingSensor=new ServerTimingSensor();previous=null;latest=null;previousAppliedAction=ActionCommand.zero(0L,0L,"none/0");}
    private void clearWorld(){if(!"none".equals(worldIdentifier)){worldIdentifier="none";episodeId=new UUID(0L,0L);sequenceNumber=0;resetSensors();}}
    private static long elapsed(long start){return Math.max(0,System.nanoTime()-start);}
    private static String worldIdentifier(World world){String name=world.getWorldInfo()==null?"unknown":world.getWorldInfo().getWorldName();if(name==null||name.isEmpty())name="unknown";name=name.replaceAll("[^A-Za-z0-9._-]","_");if(name.length()>64)name=name.substring(0,64);return "world:"+name+":dim:"+world.provider.getDimensionId();}
    public ObservationSnapshot latest(){return latest;}
    public ObservationSnapshot previous(){return previous;}
    public long snapshotAgeMillis(){ObservationSnapshot value=latest;if(value==null)return Long.MAX_VALUE;return Math.max(0,(System.nanoTime()-value.monotonicTimestampNanos())/1_000_000L);}
    public int intervalTicks(){return intervalTicks;}
    public void setPreviousAppliedAction(ActionCommand action){if(action!=null)previousAppliedAction=action;}
}
