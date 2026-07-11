package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.action.*;
import dev.fivesaw.sawbot.common.events.*;
import dev.fivesaw.sawbot.common.observation.*;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import dev.fivesaw.sawbot.common.telemetry.*;
import dev.fivesaw.sawbot.forge.client.SawBotKeyBindings;
import dev.fivesaw.sawbot.forge.actuator.*;
import dev.fivesaw.sawbot.forge.model.*;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.SnapshotJsonWriter;
import dev.fivesaw.sawbot.forge.hud.EntityVisualStyle;
import dev.fivesaw.sawbot.forge.map.LandmarkSensor;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import java.io.File;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import dev.fivesaw.sawbot.forge.safety.SawBotMode;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import dev.fivesaw.sawbot.forge.sensors.MidRangeMapSensor;
import dev.fivesaw.sawbot.forge.tracking.EntityTrackerSensor;
import dev.fivesaw.sawbot.forge.tracking.EntityTypeClassifier;
import dev.fivesaw.sawbot.forge.telemetry.TelemetryBinaryCodec;
import dev.fivesaw.sawbot.forge.telemetry.TelemetryService;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
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
        entityTypeClassificationIsSpecific();
        entityVisibilityUpdatesAcrossWallTransition();
        worldSpawnLandmarkResolvesToStandableSurface();
        navigationWaypointAppearsAsSemanticLandmark();
        rollingWindowRemainsBounded();
        keyDefaultsAvoidVanillaFunctionConflicts();
        safetyControllerReleasesEveryHeldControl();
        observationFreezeIsIndependentOfEnableState();
        unfreezeRequestsImmediateRefresh();
        tracerToggleIsIndependentOfEntityBoxes();
        entityVisualStyleFollowsCurrentVisibility();
        phase1PipelineCreatesBoundedSnapshot();
        frozenPipelinePreservesSnapshot();
        egocentricInverseTransformsAreStable();
        observationDifferenceIsDeterministic();
        blockInspectionUsesSnapshotBasis();
        singleStepRequiresFreezeAndCapturesExactlyOnce();
        snapshotDebugJsonContainsBoundedInputs();
        phase2KeyDefaultsAreStable();
        telemetrySchemaAndInputWindowAreStable();
        telemetryBinaryCodecIsDeterministic();
        telemetryObservationPayloadIsVersioned();
        telemetryServiceWritesCompleteTrajectory();
        telemetryFailureCanBeRetried();
        modelProtocolFrameRoundTripIsBounded();
        modelProtocolActionDecodeIsDeterministic();
        modelBridgeConnectsWithoutBlockingClientThread();
        actionContextRejectsMissingReferences();
        environmentGuardBlocksPublicServers();
        safeActuatorUsesLegitimateControlsAndReleases();
        disabledActuatorPreservesHumanInput();
        physicalInputArmClearsStaleMouseDelta();
        System.out.println("PASS FoundationContractTest (" + checks + " checks)");
    }

    private static void schemaIdentifiersAreStable() {
        require("sawbot.observation/0.1".equals(SchemaVersion.OBSERVATION_V0_1.identifier()), "legacy observation schema");
        require("sawbot.observation/0.2".equals(SchemaVersion.OBSERVATION_V0_2.identifier()), "phase1 observation schema");
        require("sawbot.observation/0.3".equals(SchemaVersion.OBSERVATION_V0_3.identifier()), "specific entity type schema");
        require("sawbot.action/0.1".equals(SchemaVersion.ACTION_V0_1.identifier()), "action schema");
        require("sawbot.telemetry/0.1".equals(SchemaVersion.TELEMETRY_V0_1.identifier()), "telemetry schema");
    }
    private static void actionValidationAcceptsCanonicalZero() { long now=1_000_000_000L;ActionCommand valid=ActionCommand.zero(10L,now-10_000_000L,"dummy/0");require(ActionValidator.validate(valid,10L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"valid zero action"); }
    private static void actionValidationRejectsStaleAndNonFiniteCommands() { long now=1_000_000_000L;ActionCommand valid=ActionCommand.zero(10L,now-10_000_000L,"dummy/0");require(!ActionValidator.validate(valid,14L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"stale sequence");ActionCommand bad=new ActionCommand(10L,now,"dummy/0",Float.NaN,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,-1,Skill.NONE,-1,-1,1f,1,TacticalObjective.NONE,AbortCondition.NONE);require(!ActionValidator.validate(bad,10L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"NaN rejected"); }
    private static void actionValidationRejectsOutOfRangeCommands(){long now=2_000_000_000L;ActionCommand bad=new ActionCommand(20L,now,"dummy/0",1.01f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,9,Skill.NONE,-1,-1,1f,5,TacticalObjective.NONE,AbortCondition.NONE);require(!ActionValidator.validate(bad,20L,now,ActionValidator.DEFAULT_MAX_AGE_NANOS,3L).isValid(),"range rejected");}
    private static void egocentricCardinalTransformIsStable(){require(EgocentricTransform.quadrant(0f)==0,"south quadrant");require(EgocentricTransform.worldDx(1,0,(byte)0)==-1,"south right");require(EgocentricTransform.worldDz(0,1,(byte)0)==1,"south forward");require(EgocentricTransform.worldDx(0,1,(byte)1)==-1,"west forward");require(EgocentricTransform.worldDz(1,0,(byte)1)==-1,"west right");}
    private static void terrainIndexAndCopiesAreStable(){short[] ids=new short[LocalTerrainSnapshot.CELL_COUNT];byte[] categories=new byte[ids.length];short[] flags=new short[ids.length];byte[] collision=new byte[ids.length];int center=LocalTerrainSnapshot.index(0,0,0);ids[center]=7;LocalTerrainSnapshot terrain=new LocalTerrainSnapshot(1,2,3,(byte)0,ids,categories,flags,collision,0);ids[center]=9;require(terrain.blockStateIdAt(center)==7,"terrain constructor copy");short[] copy=terrain.blockStateIds();copy[center]=11;require(terrain.blockStateIdAt(center)==7,"terrain accessor copy");}
    private static void snapshotRejectsMissingComponent(){boolean rejected=false;try{new ObservationSnapshot(1,2,UUID.randomUUID(),1,"world","universal",null,terrain(),midRange(),entities(),inventory(),landmarks(),events(),timing(),TaskStateSnapshot.UNIVERSAL,ActionCommand.zero(0,2,"none"),SensorValidity.ALL_PHASE1,sensorTimings());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"missing component rejected");}
    private static void entityAndInventoryCollectionsAreImmutable(){List<EntityObservation> source=new ArrayList<EntityObservation>();source.add(new EntityObservation(1,2,EntityKind.PLAYER,EntityType.PLAYER,TeamRelation.ENEMY,1,0,2,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,2,0,0,0,true,false,false,true,false,true,true,1));EntitySetSnapshot set=new EntitySetSnapshot(source,0);source.clear();require(set.count()==1,"entity source copy");boolean blocked=false;try{set.entities().clear();}catch(UnsupportedOperationException expected){blocked=true;}require(blocked,"entity list immutable");require(inventory().slots().size()==41,"inventory fixed slot count");}
    private static void midRangeIndexIsStable(){require(MidRangeMapSnapshot.index(-16,-16)==0,"midrange first");require(MidRangeMapSnapshot.index(16,16)==MidRangeMapSnapshot.COLUMN_COUNT-1,"midrange last");}
    private static void midRangeCacheSurvivesMovementAndRotation(){World world=new World();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)world.setBlockStateForTest(new BlockPos(x,63,z),Blocks.wool.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);int before=knownColumns(sensor.snapshot());player.posX=1.5;player.rotationYaw=90f;sensor.update(player,world,18);int after=knownColumns(sensor.snapshot());require(before>900,"midrange fills incrementally");require(after>700,"midrange cache reprojects after movement");}
    private static int knownColumns(MidRangeMapSnapshot snapshot){int count=0;for(short flags:snapshot.flags())if((flags&MidRangeMapSnapshot.FLAG_UNKNOWN)==0)count++;return count;}
    private static void midRangeUsesBoundedHintScans(){World world=new World();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)world.setBlockStateForTest(new BlockPos(x,63,z),Blocks.wool.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);world.resetGetBlockStateCallsForTest();sensor.update(player,world,18);require(world.getBlockStateCallsForTest()<800,"midrange hint scan stays bounded");}
    private static void midRangeHazardsAreNotSafeLanding(){World world=new World();world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.cactus.getDefaultState());EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;MidRangeMapSensor sensor=new MidRangeMapSensor();for(int tick=1;tick<=17;tick++)sensor.update(player,world,tick);short flags=sensor.snapshot().flags()[MidRangeMapSnapshot.index(0,0)];require((flags&MidRangeMapSnapshot.FLAG_VOID)==0,"cactus column is a surface");require((flags&MidRangeMapSnapshot.FLAG_SAFE_LANDING)==0,"cactus is not safe landing");}
    private static void entityTeamClassificationIsConservative(){World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posY=64;EntityPlayer other=new EntityPlayer();other.posX=2;other.posY=64;world.loadedEntityList.add(other);EntityTrackerSensor tracker=new EntityTrackerSensor();EntitySetSnapshot unknown=tracker.capture(player,world,1);require(unknown.entities().get(0).teamRelation()==TeamRelation.UNKNOWN,"players without teams remain unknown");Team a=new Team();Team b=new Team();player.setTeamForTest(a);other.setTeamForTest(a);require(tracker.capture(player,world,2).entities().get(0).teamRelation()==TeamRelation.TEAMMATE,"same scoreboard team");other.setTeamForTest(b);require(tracker.capture(player,world,3).entities().get(0).teamRelation()==TeamRelation.ENEMY,"different scoreboard teams");}
    private static void entityTypeClassificationIsSpecific(){
        EntityLivingBase cow=new EntityLivingBase();
        EntityList.setEntityStringForTest(cow,"Cow");
        require(EntityTypeClassifier.classify(cow)==EntityType.COW,"cow type");
        EntityLivingBase spider=new EntityLivingBase();
        EntityList.setEntityStringForTest(spider,"Spider");
        require(EntityTypeClassifier.classify(spider)==EntityType.SPIDER,"spider type");
        EntityLivingBase horse=new EntityLivingBase();
        EntityList.setEntityStringForTest(horse,"EntityHorse");
        require(EntityTypeClassifier.classify(horse)==EntityType.HORSE,"legacy horse registry type");
        Entity crystal=new Entity();
        EntityList.setEntityStringForTest(crystal,"EnderCrystal");
        require(EntityTypeClassifier.classify(crystal)==EntityType.ENDER_CRYSTAL,"ender crystal type");
        EntityItem item=new EntityItem(new ItemStack(Items.gold_ingot,1));
        require(EntityTypeClassifier.classify(item)==EntityType.DROPPED_ITEM,"dropped item type");
        Entity unknown=new Entity();
        require(EntityTypeClassifier.classify(unknown)==EntityType.OTHER,"unknown entity fallback");
    }

    private static void entityVisibilityUpdatesAcrossWallTransition(){
        World world=new World();
        EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;
        EntityPlayer other=new EntityPlayer();other.posX=4.5;other.posY=64;other.posZ=0.5;world.loadedEntityList.add(other);
        world.setBlockStateForTest(new BlockPos(2,64,0),Blocks.wool.getDefaultState());
        world.setBlockStateForTest(new BlockPos(2,65,0),Blocks.wool.getDefaultState());
        EntityTrackerSensor tracker=new EntityTrackerSensor();
        EntityObservation blocked=tracker.capture(player,world,1).entities().get(0);
        require(!blocked.lineOfSight()&&blocked.occluded(),"wall blocks entity LOS");
        int trackingId=blocked.trackingId();
        world.setBlockStateForTest(new BlockPos(2,64,0),Blocks.air.getDefaultState());
        world.setBlockStateForTest(new BlockPos(2,65,0),Blocks.air.getDefaultState());
        EntityObservation visible=tracker.capture(player,world,2).entities().get(0);
        require(visible.lineOfSight()&&!visible.occluded(),"LOS updates after wall clears");
        require(visible.trackingId()==trackingId,"LOS transition preserves tracking id");
    }
    private static void worldSpawnLandmarkResolvesToStandableSurface(){
        World world=new World();world.setBlockStateForTest(new BlockPos(0,3,0),Blocks.wool.getDefaultState());
        EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=4;player.posZ=0.5;
        LandmarkSensor sensor=new LandmarkSensor();
        LandmarkObservation landmark=sensor.capture(player,world,1).landmarks().get(0);
        require(Math.abs(landmark.up())<0.0001f,"spawn landmark uses top surface");
        require(landmark.confidence()==1f,"resolved spawn landmark confidence");
        world.resetGetBlockStateCallsForTest();
        LandmarkObservation cached=sensor.capture(player,world,2).landmarks().get(0);
        require(world.getBlockStateCallsForTest()==0,"spawn surface cache avoids repeated scans");
        require(Math.abs(cached.up())<0.0001f,"cached spawn surface remains stable");
    }

    private static void navigationWaypointAppearsAsSemanticLandmark(){
        Minecraft minecraft=Minecraft.getMinecraft();
        World world=new World();
        EntityPlayerSP player=new EntityPlayerSP();
        player.posX=0.5;player.posY=64;player.posZ=0.5;player.rotationYaw=0f;
        minecraft.theWorld=world;minecraft.thePlayer=player;
        minecraft.objectMouseOver=new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK,new BlockPos(0,63,5),null);
        NavigationWaypointController controller=new NavigationWaypointController(minecraft);
        require(controller.setFromCrosshair(),"waypoint set from crosshair");
        LandmarkObservation landmark=controller.capture(player,world);
        require(landmark!=null&&landmark.landmarkId()==NavigationWaypointController.USER_WAYPOINT_ID,"waypoint landmark id");
        require(landmark.type()==LandmarkType.STAGING_AREA,"waypoint semantic type");
        require(landmark.forward()>4f&&Math.abs(landmark.right())<0.1f,"waypoint egocentric position");
        require(controller.clear()&&controller.capture(player,world)==null,"waypoint clear");
    }

    private static void rollingWindowRemainsBounded(){RollingTimingWindow window=new RollingTimingWindow(3);window.add(10);window.add(20);window.add(30);window.add(40);require(window.count()==3,"bounded count");require(window.averageNanos()==30,"ring average");require(window.latestNanos()==40,"latest");require(window.maximumNanos()==40,"maximum");}

    private static void keyDefaultsAvoidVanillaFunctionConflicts(){SawBotKeyBindings keys=new SawBotKeyBindings();require(keys.toggleEnabled.getKeyCode()==org.lwjgl.input.Keyboard.KEY_F10,"enable defaults to F10");require(keys.toggleFreeze.getKeyCode()==org.lwjgl.input.Keyboard.KEY_P,"freeze defaults to P");require(keys.toggleTelemetry.getKeyCode()==org.lwjgl.input.Keyboard.KEY_K,"telemetry defaults to K");require(keys.toggleEnabled.getKeyCode()!=63&&keys.toggleEnabled.getKeyCode()!=64&&keys.toggleEnabled.getKeyCode()!=66,"enable avoids F5/F6/F8");require(keys.toggleFreeze.getKeyCode()!=63&&keys.toggleFreeze.getKeyCode()!=64&&keys.toggleFreeze.getKeyCode()!=66,"freeze avoids F5/F6/F8");}
    private static void safetyControllerReleasesEveryHeldControl(){Minecraft minecraft=Minecraft.getMinecraft();int[] keys={1,2,3,4,5,6,7,8,9,10,11};for(int key:keys)KeyBinding.setKeyBindState(key,true);SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());controller.toggleEnabled();require(controller.mode()==SawBotMode.ENABLED,"controller enabled");controller.emergencyStop();require(controller.mode()==SawBotMode.DISABLED,"controller disabled");require("emergency stop".equals(controller.lastStopReason()),"stop reason");for(int key:keys)require(!KeyBinding.isKeyDownForTest(key),"key released "+key);}
    private static void observationFreezeIsIndependentOfEnableState(){Minecraft minecraft=Minecraft.getMinecraft();SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());require(controller.mode()==SawBotMode.DISABLED,"freeze test starts disabled");require(!controller.observationsFrozen(),"freeze starts off");controller.toggleFrozen();require(controller.observationsFrozen(),"freeze works while disabled");require(controller.mode()==SawBotMode.DISABLED,"freeze does not enable control");require(!controller.mayApplyAutonomousActions(),"disabled frozen state cannot actuate");controller.toggleEnabled();require(controller.mode()==SawBotMode.ENABLED,"enable remains independent");require(controller.observationsFrozen(),"enable preserves frozen snapshot");require(!controller.mayApplyAutonomousActions(),"frozen enabled state cannot actuate");controller.toggleFrozen();require(!controller.observationsFrozen(),"unfreeze works");require(controller.mayApplyAutonomousActions(),"enabled unfrozen state may actuate later");}
    private static void unfreezeRequestsImmediateRefresh(){
        SawBotStateController controller=new SawBotStateController(Minecraft.getMinecraft(),new TestLogger());
        controller.toggleFrozen();
        require(!controller.consumeObservationRefreshRequest(),"freeze does not request live refresh");
        controller.toggleFrozen();
        require(controller.consumeObservationRefreshRequest(),"unfreeze requests immediate refresh");
        require(!controller.consumeObservationRefreshRequest(),"refresh request consumed once");
    }

    private static void tracerToggleIsIndependentOfEntityBoxes(){
        SawBotStateController controller=new SawBotStateController(Minecraft.getMinecraft(),new TestLogger());
        require(controller.entityTracersVisible(),"entity tracers start visible");
        require(!controller.entityOverlayVisible(),"entity boxes start hidden");
        controller.toggleEntityOverlay();
        controller.toggleEntityTracers();
        require(controller.entityOverlayVisible(),"entity box toggle remains on");
        require(!controller.entityTracersVisible(),"entity tracers can be disabled independently");
    }

    private static ObservationSnapshot pipelineSnapshot;

    private static void entityVisualStyleFollowsCurrentVisibility(){
        EntityObservation los=new EntityObservation(7,70,EntityKind.PLAYER,EntityType.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,0,true,false,false,true,false,true,true,1f);
        EntityObservation occ=new EntityObservation(7,70,EntityKind.PLAYER,EntityType.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,0,true,false,false,false,true,false,true,1f);
        EntityObservation invalid=new EntityObservation(7,70,EntityKind.PLAYER,EntityType.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,0,true,false,false,true,true,false,true,1f);
        require(EntityVisualStyle.visibilityRgb(los)==EntityVisualStyle.LOS_RGB,"LOS visual is immediate green");
        require(EntityVisualStyle.visibilityRgb(occ)==EntityVisualStyle.OCCLUDED_RGB,"OCC visual is immediate purple");
        require(EntityVisualStyle.visibilityRgb(invalid)==EntityVisualStyle.INCONSISTENT_RGB,"inconsistent visibility warns orange");
        require("LOS".equals(EntityVisualStyle.visibilityToken(los)),"LOS text and colour share state");
        require("OCC".equals(EntityVisualStyle.visibilityToken(occ)),"OCC text and colour share state");
        require(EntityVisualStyle.visibilityArgb(los)!=(EntityVisualStyle.visibilityArgb(occ)),"visibility colours differ");
    }

    private static void phase1PipelineCreatesBoundedSnapshot(){Minecraft minecraft=Minecraft.getMinecraft();World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;player.onGround=true;world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.wool.getDefaultState());player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);player.inventory.mainInventory[1]=new ItemStack(new ItemBlock(Blocks.wool),16);EntityPlayer enemy=new EntityPlayer();enemy.posX=3;enemy.posY=64;enemy.posZ=0.5;world.loadedEntityList.add(enemy);minecraft.theWorld=world;minecraft.thePlayer=player;ObservationPipeline pipeline=new ObservationPipeline(minecraft,2);pipeline.tick(1,false);pipeline.tick(2,false);pipelineSnapshot=pipeline.latest();require(pipelineSnapshot!=null,"pipeline snapshot");require(pipelineSnapshot.schemaVersion().equals(SchemaVersion.OBSERVATION_V0_3),"pipeline schema");require(pipelineSnapshot.localTerrain().blockStateIds().length==LocalTerrainSnapshot.CELL_COUNT,"terrain bounded");require(pipelineSnapshot.midRangeMap().relativeSurfaceY().length==MidRangeMapSnapshot.COLUMN_COUNT,"map bounded");require(pipelineSnapshot.entities().count()==1,"entity captured");require(pipelineSnapshot.inventory().iron()==4&&pipelineSnapshot.inventory().wool()==16,"resources captured");require((pipelineSnapshot.sensorValidityFlags()&SensorValidity.ALL_PHASE1)==SensorValidity.ALL_PHASE1,"validity flags");require(pipelineSnapshot.sensorTimings().totalNanos()>=0,"timing captured");}
    private static void frozenPipelinePreservesSnapshot(){Minecraft minecraft=Minecraft.getMinecraft();ObservationPipeline pipeline=new ObservationPipeline(minecraft,1);pipeline.tick(10,false);ObservationSnapshot before=pipeline.latest();require(before!=null,"freeze baseline");pipeline.tick(11,true);require(pipeline.latest().sequenceNumber()==before.sequenceNumber(),"frozen sequence stable");}


    private static void egocentricInverseTransformsAreStable(){
        for(byte quadrant=0;quadrant<4;quadrant++){
            for(int right=-3;right<=3;right++)for(int forward=-3;forward<=3;forward++){
                int dx=EgocentricTransform.worldDx(right,forward,quadrant);
                int dz=EgocentricTransform.worldDz(right,forward,quadrant);
                require(EgocentricTransform.rightFromWorldDelta(dx,dz,quadrant)==right,"inverse right q"+quadrant);
                require(EgocentricTransform.forwardFromWorldDelta(dx,dz,quadrant)==forward,"inverse forward q"+quadrant);
            }
        }
        float right=2.5f,forward=-1.25f,yaw=37f;
        double dx=EgocentricTransform.worldDx(right,forward,yaw),dz=EgocentricTransform.worldDz(right,forward,yaw);
        require(Math.abs(EgocentricTransform.right(dx,dz,yaw)-right)<0.0001f,"continuous inverse right");
        require(Math.abs(EgocentricTransform.forward(dx,dz,yaw)-forward)<0.0001f,"continuous inverse forward");
    }

    private static void observationDifferenceIsDeterministic(){
        ObservationSnapshot before=snapshotAt(10,1,0,0,0,terrainWithCenter((short)1),inventory());
        List<ItemSlotObservation> slots=new ArrayList<ItemSlotObservation>(inventory().slots());
        slots.set(0,new ItemSlotObservation(0,5,0,3,0,0,ItemCategory.BLOCK));
        InventorySnapshot changedInventory=new InventorySnapshot(slots,0,"NONE",0,0,0,0,3);
        ObservationSnapshot after=snapshotAt(12,2,3,4,0,terrainWithCenter((short)2),changedInventory);
        ObservationDiff diff=ObservationDiffCalculator.compare(before,after);
        require(diff.fromSequence()==1&&diff.toSequence()==2,"diff sequence");
        require(diff.clientTickDelta()==2,"diff tick delta");
        require(Math.abs(diff.positionDistance()-5f)<0.0001f,"diff position");
        require(diff.terrainChangedCells()==1,"diff terrain cell");
        require(diff.inventoryChangedSlots()==1,"diff inventory slot");
        require(diff.entitiesAdded()==0&&diff.entitiesRemoved()==0,"diff entity stability");
    }

    private static void blockInspectionUsesSnapshotBasis(){
        short[] ids=new short[LocalTerrainSnapshot.CELL_COUNT];byte[] categories=new byte[ids.length];short[] flags=new short[ids.length];byte[] collision=new byte[ids.length];
        int index=LocalTerrainSnapshot.index(2,1,-3);ids[index]=42;categories[index]=(byte)BlockSemanticCategory.PARTIAL.ordinal();flags[index]=LocalTerrainSnapshot.FLAG_PARTIAL_BLOCK;collision[index]=2;
        LocalTerrainSnapshot terrain=new LocalTerrainSnapshot(100,64,200,(byte)1,ids,categories,flags,collision,0);
        BlockPos position=new BlockPos(103,65,198);
        BlockInspection inspected=InspectorController.inspectBlock(terrain,position);
        require(inspected.insideTensor(),"block inside tensor");
        require(inspected.rightOffset()==2&&inspected.upOffset()==1&&inspected.forwardOffset()==-3,"block offsets");
        require(inspected.terrainIndex()==index&&inspected.blockStateId()==42,"block decoded");
        require(inspected.category()==BlockSemanticCategory.PARTIAL&&inspected.collisionHeightClass()==2,"block semantics");
        BlockInspection outside=InspectorController.inspectBlock(terrain,new BlockPos(500,80,500));
        require(!outside.insideTensor()&&outside.terrainIndex()==-1,"outside block marked");
    }

    private static void singleStepRequiresFreezeAndCapturesExactlyOnce(){
        Minecraft minecraft=Minecraft.getMinecraft();
        SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());
        require(!controller.requestObservationStep(),"step rejected while live");
        controller.toggleFrozen();
        require(controller.requestObservationStep(),"step accepted while frozen");
        require(controller.consumeObservationStepRequest(),"step consumed once");
        require(!controller.consumeObservationStepRequest(),"step not repeated");
        World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posY=64;world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.wool.getDefaultState());minecraft.theWorld=world;minecraft.thePlayer=player;
        ObservationPipeline pipeline=new ObservationPipeline(minecraft,20);
        pipeline.tick(100,true,true);ObservationSnapshot first=pipeline.latest();
        require(first!=null&&first.sequenceNumber()==1,"single step captures despite interval");
        pipeline.tick(101,true,false);require(pipeline.latest().sequenceNumber()==1,"frozen tick does not capture");
        pipeline.tick(102,true,true);require(pipeline.latest().sequenceNumber()==2,"second step increments once");
        require(pipeline.previous()!=null&&pipeline.previous().sequenceNumber()==1,"previous snapshot retained");
    }

    private static void snapshotDebugJsonContainsBoundedInputs(){
        try{
            ObservationSnapshot snapshot=richSnapshot();
            StringWriter writer=new StringWriter();
            SnapshotJsonWriter.write(snapshot,ObservationDiff.EMPTY,null,-1,writer);
            String json=writer.toString();
            require(json.contains("\"exportFormat\": \"sawbot.snapshot.debug/0.2\""),"json export format");
            require(json.contains("\"blockStateIds\""),"json terrain arrays");
            require(json.contains("\"relativeSurfaceY\""),"json map arrays");
            require(json.contains("\"slots\""),"json inventory slots");
            require(json.contains("\"previousAction\""),"json action");
            require(json.contains("\"type\": \"PLAYER\""),"json specific entity type");
            require(json.contains("\"payloadItemCategory\""),"json payload item category");
            require(json.length()<200000,"json remains bounded");
            String fixture=System.getProperty("sawbot.fixture");
            if(fixture!=null&&!fixture.isEmpty())java.nio.file.Files.write(java.nio.file.Paths.get(fixture),json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(Exception exception){throw new AssertionError("snapshot JSON",exception);}
    }

    private static void phase2KeyDefaultsAreStable(){
        SawBotKeyBindings keys=new SawBotKeyBindings();
        Set<Integer> codes=new HashSet<Integer>();
        int[] values={keys.stepObservation.getKeyCode(),keys.cycleInspectorPage.getKeyCode(),keys.toggleTerrainOverlay.getKeyCode(),keys.toggleCollisionOverlay.getKeyCode(),keys.toggleEntityOverlay.getKeyCode(),keys.toggleEntityTracers.getKeyCode(),keys.toggleLandmarkOverlay.getKeyCode(),keys.previousEntity.getKeyCode(),keys.nextEntity.getKeyCode(),keys.exportSnapshot.getKeyCode()};
        for(int value:values){require(value!=org.lwjgl.input.Keyboard.KEY_NONE,"phase2 key is bound");require(codes.add(Integer.valueOf(value)),"phase2 key unique");}
    }

    private static void telemetrySchemaAndInputWindowAreStable(){
        HumanInputSample sample=new HumanInputSample(40,5000,HumanInputSample.FORWARD|HumanInputSample.ATTACK,7,-3,2,false);
        require(sample.keyDown(HumanInputSample.FORWARD)&&sample.keyDown(HumanInputSample.ATTACK),"telemetry key bits");
        require(sample.mouseDeltaX()==7&&sample.mouseDeltaY()==-3,"telemetry mouse deltas");
        List<HumanInputSample> source=new ArrayList<HumanInputSample>();source.add(sample);
        HumanInputWindow window=new HumanInputWindow(source,2);source.clear();
        require(window.count()==1&&window.droppedSamples()==2,"input window copy and drop count");
        boolean immutable=false;try{window.samples().clear();}catch(UnsupportedOperationException expected){immutable=true;}
        require(immutable,"input window immutable");
    }

    private static void telemetryBinaryCodecIsDeterministic(){
        ObservationSnapshot observation=richSnapshot();
        HumanInputSample sample=new HumanInputSample(21,2000,HumanInputSample.FORWARD|HumanInputSample.JUMP,4,-2,3,false);
        HumanInputWindow input=new HumanInputWindow(Collections.singletonList(sample),0);
        TrajectoryStep step=new TrajectoryStep(observation,ActionSource.HUMAN,input,8,22,Collections.<ObservationEvent>emptyList(),false);
        byte[] first=TelemetryBinaryCodec.encodeStep(step);
        byte[] second=TelemetryBinaryCodec.encodeStep(step);
        require(Arrays.equals(first,second),"telemetry codec deterministic");
        require(first.length>1000&&first.length<100000,"telemetry payload bounded");
        require((first[0]&255)==0x53&&(first[1]&255)==0x54&&(first[2]&255)==0x50&&(first[3]&255)==0x31,"telemetry payload magic");
    }

    private static void telemetryServiceWritesCompleteTrajectory(){
        try{
            File root=new File(System.getProperty("java.io.tmpdir"),"sawbot-telemetry-test-"+System.nanoTime());
            Minecraft minecraft=Minecraft.getMinecraft();
            minecraft.mcDataDir=root;
            if(minecraft.theWorld==null)minecraft.theWorld=new World();
            if(minecraft.thePlayer==null)minecraft.thePlayer=new EntityPlayerSP();
            minecraft.thePlayer.inventory.currentItem=2;
            TelemetryService service=new TelemetryService(root,minecraft,8,8,1,new TestLogger());
            ObservationSnapshot first=snapshotAt(30,10,0,64,0,terrain(),inventory());
            ObservationSnapshot second=snapshotAt(32,11,1,64,0,terrain(),inventory());
            service.synchronizeRequested(true,first);
            KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindForward.getKeyCode(),true);
            minecraft.mouseHelper.deltaX=6;minecraft.mouseHelper.deltaY=-4;
            service.captureHumanInput(31);
            KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindForward.getKeyCode(),false);
            service.onObservation(second);
            service.synchronizeRequested(false,second);
            long deadline=System.currentTimeMillis()+3000L;
            while("finalizing".equals(service.status())&&System.currentTimeMillis()<deadline)Thread.sleep(20L);
            service.close();
            File directory=new File(root,"sawbotv1/telemetry");
            File[] complete=directory.listFiles(new java.io.FilenameFilter(){public boolean accept(File dir,String name){return name.endsWith(".sbt");}});
            require(complete!=null&&complete.length==1,"telemetry complete file written");
            byte[] bytes=java.nio.file.Files.readAllBytes(complete[0].toPath());
            require(bytes.length>100&&bytes[0]=='S'&&bytes[1]=='B'&&bytes[2]=='T',"telemetry file magic");
            require(!new File(complete[0].getAbsolutePath()+".partial").exists(),"telemetry partial renamed");
            String fixture=System.getProperty("sawbot.telemetry.fixture");
            if(fixture!=null&&!fixture.isEmpty())java.nio.file.Files.copy(complete[0].toPath(),java.nio.file.Paths.get(fixture),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception exception){throw new AssertionError("telemetry service",exception);}
    }


    private static void telemetryObservationPayloadIsVersioned(){
        byte[] payload=TelemetryBinaryCodec.encodeObservation(richSnapshot());
        require(payload.length>1000&&payload.length<ModelProtocol.MAX_PAYLOAD_BYTES,"bridge observation payload bounded");
        require((payload[0]&255)==0x4F&&(payload[1]&255)==0x42&&(payload[2]&255)==0x53&&(payload[3]&255)==0x31,"bridge observation payload magic OBS1");
        String fixture=System.getProperty("sawbot.observation.fixture");
        if(fixture!=null&&!fixture.isEmpty()){
            try{java.nio.file.Files.write(new File(fixture).toPath(),payload);}
            catch(Exception exception){throw new AssertionError("observation fixture",exception);}
        }
    }

    private static void modelProtocolFrameRoundTripIsBounded(){
        try{
            byte[] payload=new byte[]{1,2,3,4,5};
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            ModelProtocol.writeFrame(new DataOutputStream(bytes),ModelProtocol.TYPE_PING,payload);
            ModelProtocol.Frame frame=ModelProtocol.readFrame(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            require(frame.type()==ModelProtocol.TYPE_PING,"bridge frame type round trip");
            require(Arrays.equals(payload,frame.payload()),"bridge frame payload round trip");
            byte[] corrupt=bytes.toByteArray();corrupt[corrupt.length-1]^=1;
            boolean rejected=false;try{ModelProtocol.readFrame(new DataInputStream(new ByteArrayInputStream(corrupt)));}catch(java.io.IOException expected){rejected=true;}
            require(rejected,"bridge CRC rejects corruption");
            boolean oversize=false;try{ModelProtocol.writeFrame(new DataOutputStream(new ByteArrayOutputStream()),ModelProtocol.TYPE_ACTION,new byte[ModelProtocol.MAX_PAYLOAD_BYTES+1]);}catch(IllegalArgumentException expected){oversize=true;}
            require(oversize,"bridge payload bound enforced");
        }catch(Exception exception){throw new AssertionError("bridge frame",exception);}
    }

    private static void modelProtocolActionDecodeIsDeterministic(){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            DataOutputStream out=new DataOutputStream(bytes);
            out.writeLong(7L);
            out.writeFloat(1F);out.writeFloat(-0.5F);out.writeFloat(12F);out.writeFloat(-6F);
            out.writeFloat(1F);out.writeFloat(1F);out.writeFloat(0F);out.writeFloat(1F);
            out.writeFloat(0F);out.writeFloat(0F);out.writeFloat(0F);
            out.writeByte(4);out.writeByte(Skill.NONE.ordinal());out.writeInt(4);out.writeInt(3);
            out.writeFloat(0.75F);out.writeByte(3);out.writeByte(TacticalObjective.NONE.ordinal());out.writeByte(AbortCondition.NONE.ordinal());out.flush();
            ActionCommand command=ModelProtocol.decodeAction(bytes.toByteArray(),"dummy/phase4",5000L);
            require(command.observationSequenceNumber()==7L&&command.generatedTimestampNanos()==5000L,"decoded action identity");
            require(command.forward()==1F&&command.strafe()==-0.5F,"decoded movement axes");
            require(command.yawDeltaDegrees()==12F&&command.pitchDeltaDegrees()==-6F,"decoded camera axes");
            require(command.jumpProbability()==1F&&command.sprintProbability()==1F&&command.attackProbability()==1F,"decoded buttons");
            require(command.hotbarSlot()==4&&command.selectedTargetTrackingId()==4&&command.selectedWaypointId()==3,"decoded references");
            require(command.actionDurationTicks()==3&&"dummy/phase4".equals(command.modelVersion()),"decoded duration/model");
        }catch(Exception exception){throw new AssertionError("bridge action decode",exception);}
    }


    private static void modelBridgeConnectsWithoutBlockingClientThread(){
        ServerSocket server=null;ModelBridge bridge=null;
        try{
            server=new ServerSocket(0);final ServerSocket acceptedServer=server;
            final Throwable[] failure=new Throwable[1];
            Thread model=new Thread(new Runnable(){@Override public void run(){
                try{
                    Socket socket=acceptedServer.accept();
                    socket.setSoTimeout(3000);
                    DataInputStream input=new DataInputStream(socket.getInputStream());
                    DataOutputStream output=new DataOutputStream(socket.getOutputStream());
                    ModelProtocol.Frame hello=ModelProtocol.readFrame(input);
                    if(hello.type()!=ModelProtocol.TYPE_HELLO)throw new AssertionError("hello frame type");
                    DataInputStream helloIn=new DataInputStream(new ByteArrayInputStream(hello.payload()));
                    readBoundedUtf8ForTest(helloIn);readBoundedUtf8ForTest(helloIn);readBoundedUtf8ForTest(helloIn);readBoundedUtf8ForTest(helloIn);
                    long nonce=helloIn.readLong();helloIn.readUnsignedShort();
                    ByteArrayOutputStream ackBytes=new ByteArrayOutputStream();DataOutputStream ack=new DataOutputStream(ackBytes);
                    writeBoundedUtf8ForTest(ack,ModelProtocol.IDENTIFIER);writeBoundedUtf8ForTest(ack,"dummy/integration");ack.writeLong(nonce);ack.writeInt(1);ack.flush();
                    ModelProtocol.writeFrame(output,ModelProtocol.TYPE_HELLO_ACK,ackBytes.toByteArray());
                    ModelProtocol.Frame observation=ModelProtocol.readFrame(input);
                    if(observation.type()!=ModelProtocol.TYPE_OBSERVATION)throw new AssertionError("observation frame type");
                    ByteBuffer obs=ByteBuffer.wrap(observation.payload()).order(ByteOrder.LITTLE_ENDIAN);
                    if(obs.getInt()!=0x3153424F)throw new AssertionError("observation magic");
                    obs.getShort();obs.getShort();long sequence=obs.getLong();
                    ByteArrayOutputStream actionBytes=new ByteArrayOutputStream();DataOutputStream action=new DataOutputStream(actionBytes);
                    action.writeLong(sequence);action.writeFloat(0.5F);action.writeFloat(0F);action.writeFloat(5F);action.writeFloat(0F);
                    for(int i=0;i<7;i++)action.writeFloat(0F);
                    action.writeByte(-1);action.writeByte(Skill.NONE.ordinal());action.writeInt(-1);action.writeInt(-1);action.writeFloat(1F);action.writeByte(1);action.writeByte(TacticalObjective.NONE.ordinal());action.writeByte(AbortCondition.NONE.ordinal());action.flush();
                    ModelProtocol.writeFrame(output,ModelProtocol.TYPE_ACTION,actionBytes.toByteArray());
                    Thread.sleep(100L);socket.close();
                }catch(Throwable throwable){failure[0]=throwable;}
            }},"SawBot-ModelBridge-Test");
            model.setDaemon(true);model.start();
            bridge=new ModelBridge("127.0.0.1",server.getLocalPort(),500,100,"test/mod",10,new TestLogger());
            long readyDeadline=System.currentTimeMillis()+3000L;
            while(!bridge.isReady()&&System.currentTimeMillis()<readyDeadline)Thread.sleep(10L);
            require(bridge.isReady(),"model bridge reaches READY");
            long offerStart=System.nanoTime();bridge.offerObservation(richSnapshot());long offerElapsed=System.nanoTime()-offerStart;
            require(offerElapsed<20_000_000L,"client observation publication non-blocking");
            ModelActionEnvelope envelope=null;long actionDeadline=System.currentTimeMillis()+3000L;
            while(envelope==null&&System.currentTimeMillis()<actionDeadline){envelope=bridge.pollLatestAction();if(envelope==null)Thread.sleep(10L);}
            require(envelope!=null,"model bridge receives action");
            require(envelope.command().observationSequenceNumber()==richSnapshot().sequenceNumber(),"bridge action aligns observation sequence");
            require("dummy/integration".equals(envelope.command().modelVersion()),"bridge model identity");
            require(bridge.sentObservations()>=1&&bridge.receivedActions()>=1,"bridge transport counters");
            model.join(3000L);if(failure[0]!=null)throw new AssertionError("model server",failure[0]);
        }catch(Exception exception){throw new AssertionError("model bridge integration",exception);}
        finally{if(bridge!=null)bridge.close();if(server!=null)try{server.close();}catch(Exception ignored){}}
    }

    private static String readBoundedUtf8ForTest(DataInputStream input)throws Exception{int length=input.readUnsignedShort();byte[] bytes=new byte[length];input.readFully(bytes);return new String(bytes,StandardCharsets.UTF_8);}
    private static void writeBoundedUtf8ForTest(DataOutputStream output,String value)throws Exception{byte[] bytes=value.getBytes(StandardCharsets.UTF_8);output.writeShort(bytes.length);output.write(bytes);}


    private static void actionContextRejectsMissingReferences(){
        ObservationSnapshot snapshot=richSnapshot();
        long now=10_000_000L;
        ActionCommand valid=new ActionCommand(snapshot.sequenceNumber(),now,"test/0",0,0,0,0,0,0,0,0,0,0,0,-1,Skill.NONE,4,3,1,1,TacticalObjective.NONE,AbortCondition.NONE);
        require(ActionContextValidator.validate(valid,snapshot,now,1_000_000L,3).isValid(),"available target and waypoint accepted");
        ActionCommand missingTarget=new ActionCommand(snapshot.sequenceNumber(),now,"test/0",0,0,0,0,0,0,0,0,0,0,0,-1,Skill.NONE,999,3,1,1,TacticalObjective.NONE,AbortCondition.NONE);
        require(!ActionContextValidator.validate(missingTarget,snapshot,now,1_000_000L,3).isValid(),"missing target rejected");
        ActionCommand missingWaypoint=new ActionCommand(snapshot.sequenceNumber(),now,"test/0",0,0,0,0,0,0,0,0,0,0,0,-1,Skill.NONE,4,999,1,1,TacticalObjective.NONE,AbortCondition.NONE);
        require(!ActionContextValidator.validate(missingWaypoint,snapshot,now,1_000_000L,3).isValid(),"missing waypoint rejected");
    }

    private static void environmentGuardBlocksPublicServers(){
        Minecraft minecraft=Minecraft.getMinecraft();
        minecraft.theWorld=new World();minecraft.thePlayer=new EntityPlayerSP();
        minecraft.setSingleplayerForTest(true);
        EnvironmentGuard guard=new EnvironmentGuard(minecraft,true,"127.0.0.1,localhost,192.168.1.20");
        require(guard.isAllowed()&&"SINGLEPLAYER".equals(guard.description()),"singleplayer allowed");
        minecraft.setSingleplayerForTest(false);minecraft.setServerDataForTest(new ServerData("Hypixel","mc.hypixel.net",false));
        require(!guard.isAllowed()&&guard.description().startsWith("BLOCKED:"),"public server blocked");
        minecraft.setServerDataForTest(new ServerData("LAN","192.168.1.20:25565",false));
        require(guard.isAllowed()&&guard.description().startsWith("PRIVATE:"),"explicit private host allowed");
        minecraft.setSingleplayerForTest(true);minecraft.setServerDataForTest(null);
    }

    private static void safeActuatorUsesLegitimateControlsAndReleases(){
        Minecraft minecraft=Minecraft.getMinecraft();
        minecraft.theWorld=new World();minecraft.thePlayer=new EntityPlayerSP();minecraft.thePlayer.posY=64;
        minecraft.setSingleplayerForTest(true);minecraft.currentScreen=null;
        KeyBinding.clearForTest();
        SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());
        EnvironmentGuard guard=new EnvironmentGuard(minecraft,true,"localhost");
        SafeActionActuator actuator=new SafeActionActuator(minecraft,controller,guard,500,3,new TestLogger());
        controller.enable();
        ObservationSnapshot snapshot=richSnapshot();
        long now=System.nanoTime();
        ActionCommand command=new ActionCommand(snapshot.sequenceNumber(),now,"dummy/phase4",1F,-1F,20F,10F,1F,1F,1F,1F,1F,1F,1F,2,Skill.NONE,4,3,1F,2,TacticalObjective.NONE,AbortCondition.NONE);
        actuator.tick(snapshot,new ModelActionEnvelope(command,now,12_000_000L));
        require(KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode()),"actuator forward key");
        require(KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindLeft.getKeyCode()),"actuator left key");
        require(KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindJump.getKeyCode()),"actuator jump key");
        require(KeyBinding.pulseCountForTest(minecraft.gameSettings.keyBindAttack.getKeyCode())==1,"actuator attack pulse");
        require(KeyBinding.pulseCountForTest(minecraft.gameSettings.keyBindUseItem.getKeyCode())==1,"actuator use pulse");
        require(KeyBinding.pulseCountForTest(minecraft.gameSettings.keyBindDrop.getKeyCode())==1,"actuator drop pulse");
        require(KeyBinding.pulseCountForTest(minecraft.gameSettings.keyBindInventory.getKeyCode())==1,"actuator inventory pulse");
        require(minecraft.thePlayer.inventory.currentItem==2,"actuator hotbar slot");
        require(Math.abs(minecraft.thePlayer.rotationYaw-10F)<0.001F&&Math.abs(minecraft.thePlayer.rotationPitch-5F)<0.001F,"camera smoothing first tick");
        actuator.tick(snapshot,null);
        require(Math.abs(minecraft.thePlayer.rotationYaw-20F)<0.001F&&Math.abs(minecraft.thePlayer.rotationPitch-10F)<0.001F,"camera smoothing completes");
        require(!KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode()),"duration releases movement");
        controller.emergencyStop();actuator.release("emergency test");
        require(!KeyBinding.anyKeyDownForTest(),"emergency releases every binding");
        require(actuator.acceptedActions()==1&&actuator.rejectedActions()==0,"actuator counters");
        require(actuator.lastActionRoundTripNanos()==12_000_000L,"actuator latency retained");

        KeyBinding.clearForTest();minecraft.currentScreen=new net.minecraft.client.gui.GuiScreen();controller.enable();
        long guiNow=System.nanoTime();
        ActionCommand guiToggle=new ActionCommand(snapshot.sequenceNumber(),guiNow,"dummy/phase4",0,0,0,0,0,0,0,0,0,0,1F,-1,Skill.NONE,-1,-1,1F,1,TacticalObjective.NONE,AbortCondition.NONE);
        actuator.tick(snapshot,new ModelActionEnvelope(guiToggle,guiNow,1L));
        require(KeyBinding.pulseCountForTest(minecraft.gameSettings.keyBindInventory.getKeyCode())==1,"safe GUI inventory toggle");
        minecraft.currentScreen=null;

        KeyBinding.clearForTest();controller.enable();
        SafeActionActuator expiring=new SafeActionActuator(minecraft,controller,guard,50,3,new TestLogger());
        long expiryNow=System.nanoTime();
        ActionCommand longAction=new ActionCommand(snapshot.sequenceNumber(),expiryNow,"dummy/phase4",1F,0,0,0,0,0,0,0,0,0,0,-1,Skill.NONE,-1,-1,1F,4,TacticalObjective.NONE,AbortCondition.NONE);
        expiring.tick(snapshot,new ModelActionEnvelope(longAction,expiryNow,1L));
        try{Thread.sleep(70L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}
        expiring.tick(snapshot,null);
        require(expiring.expiredActions()==1,"actuator deadline expires active action");
        require(!KeyBinding.anyKeyDownForTest(),"expired action releases bindings");
    }


    private static void disabledActuatorPreservesHumanInput(){
        Minecraft minecraft=Minecraft.getMinecraft();
        minecraft.currentScreen=null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());
        EnvironmentGuard guard=new EnvironmentGuard(minecraft,true,"");
        SafeActionActuator actuator=new SafeActionActuator(minecraft,controller,guard,250,3,new TestLogger());
        KeyBinding.setKeyBindState(minecraft.gameSettings.keyBindForward.getKeyCode(),true);
        actuator.tick(richSnapshot(),null);
        require(KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode()),"disabled actuator preserves human forward key");
        KeyBinding.clearForTest();
    }

    private static void physicalInputArmClearsStaleMouseDelta(){
        Minecraft minecraft=Minecraft.getMinecraft();
        minecraft.mouseHelper.deltaX=9;minecraft.mouseHelper.deltaY=-4;
        PhysicalInputMonitor monitor=new PhysicalInputMonitor(minecraft);
        monitor.arm();
        require(minecraft.mouseHelper.deltaX==0&&minecraft.mouseHelper.deltaY==0,"physical input arm clears stale mouse delta");
        require(!monitor.hasTakeoverInput(),"arming grace ignores stale mouse input");
    }

    private static void telemetryFailureCanBeRetried(){
        TelemetryService service=null;
        try{
            File root=new File(System.getProperty("java.io.tmpdir"),"sawbot-telemetry-retry-"+System.nanoTime());
            require(root.mkdirs(),"telemetry retry root created");
            File blocker=new File(root,"sawbotv1");
            java.nio.file.Files.write(blocker.toPath(),new byte[]{1});
            Minecraft minecraft=Minecraft.getMinecraft();minecraft.mcDataDir=root;
            minecraft.theWorld=new World();minecraft.thePlayer=new EntityPlayerSP();
            service=new TelemetryService(root,minecraft,8,8,1,new TestLogger());
            ObservationSnapshot first=snapshotAt(40,20,0,64,0,terrain(),inventory());
            ObservationSnapshot second=snapshotAt(42,21,1,64,0,terrain(),inventory());
            service.synchronizeRequested(true,first);
            long errorDeadline=System.currentTimeMillis()+3000L;
            while(!"error".equals(service.status())&&System.currentTimeMillis()<errorDeadline)Thread.sleep(20L);
            require("error".equals(service.status())&&service.failureLatched(),"telemetry I/O failure becomes visible and latched");
            require(!service.failureMessage().isEmpty(),"telemetry failure reason retained");
            service.synchronizeRequested(false,first);
            require(service.prepareRetry(),"telemetry completed failure can reset");
            require(blocker.delete(),"telemetry blocker removed");
            service.synchronizeRequested(true,first);
            require(service.isRecording(),"telemetry restarts after failure reset");
            minecraft.mouseHelper.deltaX=2;minecraft.mouseHelper.deltaY=1;service.captureHumanInput(41);service.onObservation(second);
            service.synchronizeRequested(false,second);
            long saveDeadline=System.currentTimeMillis()+3000L;
            while("finalizing".equals(service.status())&&System.currentTimeMillis()<saveDeadline)Thread.sleep(20L);
            require("saved".equals(service.status()),"telemetry retry saves clean session");
            File directory=new File(root,"sawbotv1/telemetry");
            File[] complete=directory.listFiles(new java.io.FilenameFilter(){public boolean accept(File dir,String name){return name.endsWith(".sbt");}});
            require(complete!=null&&complete.length==1,"telemetry retry complete file exists");
        }catch(Exception exception){throw new AssertionError("telemetry retry",exception);}
        finally{if(service!=null)service.close();}
    }


    private static ObservationSnapshot richSnapshot(){
        SelfState self=new SelfState(18,2,19,4,1,64,3,0.1f,0f,0.2f,0f,0f,0f,15,-5,0,true,false,false,false,false,false,true,false,false,0,1,0,0,0,0,8,1);
        List<EntityObservation> entityList=new ArrayList<EntityObservation>();
        entityList.add(new EntityObservation(4,44,EntityKind.PLAYER,EntityType.PLAYER,TeamRelation.ENEMY,2,0,4,0.1f,0,0,0,0,0,30,0,0.6f,1.8f,17,5,4.5f,ItemCategory.SWORD.ordinal(),ItemCategory.EMPTY.ordinal(),2,true,true,false,true,false,false,true,0.9f));
        EntitySetSnapshot entitySet=new EntitySetSnapshot(entityList,0);
        List<LandmarkObservation> landmarkList=new ArrayList<LandmarkObservation>();
        landmarkList.add(new LandmarkObservation(3,LandmarkType.WORLD_SPAWN,TeamRelation.NEUTRAL,5,0,6,7,8,0.1f,0.3f,1,true));
        landmarkList.add(new LandmarkObservation(NavigationWaypointController.USER_WAYPOINT_ID,LandmarkType.STAGING_AREA,TeamRelation.NEUTRAL,2,0,5,5.4f,5.4f,0,1,1,true));
        List<ObservationEvent> eventList=Collections.singletonList(new ObservationEvent(ObservationEventType.ENTITY_ENTERED_RANGE,20,4,2,0,4,1,true));
        return new ObservationSnapshot(20,1007,new UUID(1,2),7,"world:test","universal/0.1",self,terrainWithCenter((short)9),midRange(),entitySet,inventory(),new LandmarkSetSnapshot(landmarkList),new EventHistorySnapshot(eventList,0),timing(),TaskStateSnapshot.UNIVERSAL,ActionCommand.zero(6,1007,"none"),SensorValidity.ALL_PHASE1,sensorTimings());
    }

    private static LocalTerrainSnapshot terrainWithCenter(short stateId){short[] ids=new short[LocalTerrainSnapshot.CELL_COUNT];byte[] categories=new byte[ids.length];short[] flags=new short[ids.length];byte[] collision=new byte[ids.length];ids[LocalTerrainSnapshot.index(0,0,0)]=stateId;return new LocalTerrainSnapshot(0,0,0,(byte)0,ids,categories,flags,collision,0);}
    private static ObservationSnapshot snapshotAt(long tick,long sequence,double x,double y,double z,LocalTerrainSnapshot terrain,InventorySnapshot inventory){SelfState self=new SelfState(20,0,20,0,x,y,z,0,0,0,0,0,0,0,0,0,true,false,false,false,false,false,false,false,false,0,0,0,0,0,0,0,0);return new ObservationSnapshot(tick,1000+sequence,new UUID(1,2),sequence,"world:test","universal/0.1",self,terrain,midRange(),entities(),inventory,landmarks(),events(),timing(),TaskStateSnapshot.UNIVERSAL,ActionCommand.zero(Math.max(0,sequence-1),1000+sequence,"none"),SensorValidity.ALL_PHASE1,sensorTimings());}

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
