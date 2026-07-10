package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.action.*;
import dev.fivesaw.sawbot.common.events.*;
import dev.fivesaw.sawbot.common.observation.*;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import dev.fivesaw.sawbot.forge.client.SawBotKeyBindings;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotMode;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import dev.fivesaw.sawbot.forge.sensors.MidRangeMapSensor;
import dev.fivesaw.sawbot.forge.tracking.EntityTrackerSensor;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

public final class FoundationContractTest {
    private static int checks;
    public static void main(String[] args) {
        schemaIdentifiersAreStable();
        actionValidationAcceptsCanonicalZero();
        actionValidationRejectsStaleAndNonFiniteCommands();
        actionValidationRejectsOutOfRangeCommands();
        egocentricCardinalTransformIsStable();
        terrainIndexAndCopiesAreStable();
        snapshotRejectsMissingComponent();
        entityAndInventoryCollectionsAreImmutable();
        midRangeIndexIsStable();
        midRangeCacheSurvivesMovementAndRotation();
        midRangeUsesBoundedHintScans();
        midRangeHazardsAreNotSafeLanding();
        entityTeamClassificationIsConservative();
        rollingWindowRemainsBounded();
        keyDefaultsAvoidVanillaFunctionConflicts();
        safetyControllerReleasesEveryHeldControl();
        phase1PipelineCreatesBoundedSnapshot();
        frozenPipelinePreservesSnapshot();
        System.out.println("PASS FoundationContractTest (" + checks + " checks)");
    }

    private static void schemaIdentifiersAreStable() {
        require("sawbot.observation/0.1".equals(SchemaVersion.OBSERVATION_V0_1.identifier()), "legacy observation schema");
        require("sawbot.observation/0.2".equals(SchemaVersion.OBSERVATION_V0_2.identifier()), "phase1 observation schema");
        require("sawbot.action/0.1".equals(SchemaVersion.ACTION_V0_1.identifier()), "action schema");
    }
    private static void actionValidationAcceptsCanonicalZero() { long now=1_000_000_000L;ActionCommand valid=ActionCommand.zero(10L,now-10_000_000L,"dummy/0");require(ActionValidator.validate(valid,10L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"valid zero action"); }
    private static void actionValidationRejectsStaleAndNonFiniteCommands() { long now=1_000_000_000L;ActionCommand valid=ActionCommand.zero(10L,now-10_000_000L,"dummy/0");require(!ActionValidator.validate(valid,14L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"stale sequence");ActionCommand bad=new ActionCommand(10L,now,"dummy/0",Float.NaN,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,-1,Skill.NONE,-1,-1,1f,1,TacticalObjective.NONE,AbortCondition.NONE);require(!ActionValidator.validate(bad,10L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"NaN rejected"); }
    private static void actionValidationRejectsOutOfRangeCommands(){long now=2_000_000_000L;ActionCommand bad=new ActionCommand(20L,now,"dummy/0",1.01f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,9,Skill.NONE,-1,-1,1f,5,TacticalObjective.NONE,AbortCondition.NONE);require(!ActionValidator.validate(bad,20L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"range rejected");}
    private static void egocentricCardinalTransformIsStable(){require(EgocentricTransform.quadrant(0f)==0,"south quadrant");require(EgocentricTransform.worldDx(1,0,(byte)0)==-1,"south right");require(EgocentricTransform.worldDz(0,1,(byte)0)==1,"south forward");require(EgocentricTransform.worldDx(0,1,(byte)1)==-1,"west forward");require(EgocentricTransform.worldDz(1,0,(byte)1)==-1,"west right");}
    private static void terrainIndexAndCopiesAreStable(){short[] ids=new short[LocalTerrainSnapshot.CELL_COUNT];byte[] categories=new byte[ids.length];short[] flags=new short[ids.length];byte[] collision=new byte[ids.length];int center=LocalTerrainSnapshot.index(0,0,0);ids[center]=7;LocalTerrainSnapshot terrain=new LocalTerrainSnapshot(1,2,3,(byte)0,ids,categories,flags,collision,0);ids[center]=9;require(terrain.blockStateIdAt(center)==7,"terrain constructor copy");short[] copy=terrain.blockStateIds();copy[center]=11;require(terrain.blockStateIdAt(center)==7,"terrain accessor copy");}
    private static void snapshotRejectsMissingComponent(){boolean rejected=false;try{new ObservationSnapshot(1,2,UUID.randomUUID(),1,"world","universal",null,terrain(),midRange(),entities(),inventory(),landmarks(),events(),timing(),TaskStateSnapshot.UNIVERSAL,ActionCommand.zero(0,2,"none"),SensorValidity.ALL_PHASE1,sensorTimings());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"missing component rejected");}
    private static void entityAndInventoryCollectionsAreImmutable(){List<EntityObservation> source=new ArrayList<EntityObservation>();source.add(new EntityObservation(1,2,EntityKind.PLAYER,TeamRelation.ENEMY,1,0,2,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,2,0,0,true,false,false,true,false,true,true,1));EntitySetSnapshot set=new EntitySetSnapshot(source,0);source.clear();require(set.count()==1,"entity source copy");boolean blocked=false;try{set.entities().clear();}catch(UnsupportedOperationException expected){blocked=true;}require(blocked,"entity list immutable");require(inventory().slots().size()==41,"inventory fixed slot count");}
    private static void midRangeIndexIsStable(){require(MidRangeMapSnapshot.index(-16,-16)==0,"midrange first");require(MidRangeMapSnapshot.index(16,16)==MidRangeMapSnapshot.COLUMN_COUNT-1,"midrange last");}
    private static void midRangeCacheSurvivesMovementAndRotation(){World world=new World();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)world.setBlockStateForTest(new BlockPos(x,63,z),Blocks.wool.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);int before=knownColumns(sensor.snapshot());player.posX=1.5;player.rotationYaw=90f;sensor.update(player,world,18);int after=knownColumns(sensor.snapshot());require(before>900,"midrange fills incrementally");require(after>700,"midrange cache reprojects after movement");}
    private static int knownColumns(MidRangeMapSnapshot snapshot){int count=0;for(short flags:snapshot.flags())if((flags&MidRangeMapSnapshot.FLAG_UNKNOWN)==0)count++;return count;}
    private static void midRangeUsesBoundedHintScans(){World world=new World();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)world.setBlockStateForTest(new BlockPos(x,63,z),Blocks.wool.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);world.resetGetBlockStateCallsForTest();sensor.update(player,world,18);require(world.getBlockStateCallsForTest()<800,"midrange hint scan stays bounded");}
    private static void midRangeHazardsAreNotSafeLanding(){World world=new World();world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.cactus.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);short flags=sensor.snapshot().flags()[MidRangeMapSnapshot.index(0,0)];require((flags&MidRangeMapSnapshot.FLAG_VOID)==0,"cactus column is a surface");require((flags&MidRangeMapSnapshot.FLAG_SAFE_LANDING)==0,"cactus is not safe landing");}
    private static void entityTeamClassificationIsConservative(){World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posY=64;EntityPlayer other=new EntityPlayer();other.posX=2;other.posY=64;world.loadedEntityList.add(other);EntityTrackerSensor tracker=new EntityTrackerSensor();EntitySetSnapshot unknown=tracker.capture(player,world,1);require(unknown.entities().get(0).teamRelation()==TeamRelation.UNKNOWN,"players without teams remain unknown");Team a=new Team();Team b=new Team();player.setTeamForTest(a);other.setTeamForTest(a);require(tracker.capture(player,world,2).entities().get(0).teamRelation()==TeamRelation.TEAMMATE,"same scoreboard team");other.setTeamForTest(b);require(tracker.capture(player,world,3).entities().get(0).teamRelation()==TeamRelation.ENEMY,"different scoreboard teams");}
    private static void rollingWindowRemainsBounded(){RollingTimingWindow window=new RollingTimingWindow(3);window.add(10);window.add(20);window.add(30);window.add(40);require(window.count()==3,"bounded count");require(window.averageNanos()==30,"ring average");require(window.latestNanos()==40,"latest");require(window.maximumNanos()==40,"maximum");}

    private static void keyDefaultsAvoidVanillaFunctionConflicts(){SawBotKeyBindings keys=new SawBotKeyBindings();require(keys.toggleEnabled.getKeyCode()==org.lwjgl.input.Keyboard.KEY_F10,"enable defaults to F10");require(keys.toggleFreeze.getKeyCode()==org.lwjgl.input.Keyboard.KEY_P,"freeze defaults to P");require(keys.toggleTelemetry.getKeyCode()==org.lwjgl.input.Keyboard.KEY_NONE,"telemetry remains unbound");require(keys.toggleEnabled.getKeyCode()!=63&&keys.toggleEnabled.getKeyCode()!=64&&keys.toggleEnabled.getKeyCode()!=66,"enable avoids F5/F6/F8");require(keys.toggleFreeze.getKeyCode()!=63&&keys.toggleFreeze.getKeyCode()!=64&&keys.toggleFreeze.getKeyCode()!=66,"freeze avoids F5/F6/F8");}
    private static void safetyControllerReleasesEveryHeldControl(){Minecraft minecraft=Minecraft.getMinecraft();int[] keys={1,2,3,4,5,6,7,8,9,10,11};for(int key:keys)KeyBinding.setKeyBindState(key,true);SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());controller.toggleEnabled();require(controller.mode()==SawBotMode.ENABLED,"controller enabled");controller.emergencyStop();require(controller.mode()==SawBotMode.DISABLED,"controller disabled");require("emergency stop".equals(controller.lastStopReason()),"stop reason");for(int key:keys)require(!KeyBinding.isKeyDownForTest(key),"key released "+key);}
    private static ObservationSnapshot pipelineSnapshot;
    private static void phase1PipelineCreatesBoundedSnapshot(){Minecraft minecraft=Minecraft.getMinecraft();World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;player.onGround=true;world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.wool.getDefaultState());player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);player.inventory.mainInventory[1]=new ItemStack(new ItemBlock(Blocks.wool),16);EntityPlayer enemy=new EntityPlayer();enemy.posX=3;enemy.posY=64;enemy.posZ=0.5;world.loadedEntityList.add(enemy);minecraft.theWorld=world;minecraft.thePlayer=player;ObservationPipeline pipeline=new ObservationPipeline(minecraft,2);pipeline.tick(1,false);pipeline.tick(2,false);pipelineSnapshot=pipeline.latest();require(pipelineSnapshot!=null,"pipeline snapshot");require(pipelineSnapshot.schemaVersion().equals(SchemaVersion.OBSERVATION_V0_2),"pipeline schema");require(pipelineSnapshot.localTerrain().blockStateIds().length==LocalTerrainSnapshot.CELL_COUNT,"terrain bounded");require(pipelineSnapshot.midRangeMap().relativeSurfaceY().length==MidRangeMapSnapshot.COLUMN_COUNT,"map bounded");require(pipelineSnapshot.entities().count()==1,"entity captured");require(pipelineSnapshot.inventory().iron()==4&&pipelineSnapshot.inventory().wool()==16,"resources captured");require((pipelineSnapshot.sensorValidityFlags()&SensorValidity.ALL_PHASE1)==SensorValidity.ALL_PHASE1,"validity flags");require(pipelineSnapshot.sensorTimings().totalNanos()>=0,"timing captured");}
    private static void frozenPipelinePreservesSnapshot(){Minecraft minecraft=Minecraft.getMinecraft();ObservationPipeline pipeline=new ObservationPipeline(minecraft,1);pipeline.tick(10,false);ObservationSnapshot before=pipeline.latest();require(before!=null,"freeze baseline");pipeline.tick(11,true);require(pipeline.latest().sequenceNumber()==before.sequenceNumber(),"frozen sequence stable");}

    private static LocalTerrainSnapshot terrain(){return new LocalTerrainSnapshot(0,0,0,(byte)0,new short[LocalTerrainSnapshot.CELL_COUNT],new byte[LocalTerrainSnapshot.CELL_COUNT],new short[LocalTerrainSnapshot.CELL_COUNT],new byte[LocalTerrainSnapshot.CELL_COUNT],0);}
    private static MidRangeMapSnapshot midRange(){return new MidRangeMapSnapshot(0,0,0,(byte)0,new short[MidRangeMapSnapshot.COLUMN_COUNT],new short[MidRangeMapSnapshot.COLUMN_COUNT],new short[MidRangeMapSnapshot.COLUMN_COUNT],0);}
    private static EntitySetSnapshot entities(){return new EntitySetSnapshot(Collections.<EntityObservation>emptyList(),0);}
    private static InventorySnapshot inventory(){List<ItemSlotObservation> slots=new ArrayList<ItemSlotObservation>();for(int i=0;i<41;i++)slots.add(new ItemSlotObservation(i,0,0,0,0,0,ItemCategory.EMPTY));return new InventorySnapshot(slots,0,"NONE",0,0,0,0,0);}
    private static LandmarkSetSnapshot landmarks(){return new LandmarkSetSnapshot(Collections.<LandmarkObservation>emptyList());}
    private static EventHistorySnapshot events(){return new EventHistorySnapshot(Collections.<ObservationEvent>emptyList(),0);}
    private static ServerTimingSnapshot timing(){return new ServerTimingSnapshot(0,0,65535,65535,65535,65535,false);}
    private static SensorTimings sensorTimings(){return new SensorTimings(0,0,0,0,0,0,0,0,0);}
    private static void require(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    private static final class TestLogger implements Logger{public void info(String message){}public void info(String message,Object value){}public void warn(String message){}public void error(String message,Throwable throwable){}}
}
