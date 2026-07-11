package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.action.*;
import dev.fivesaw.sawbot.common.events.*;
import dev.fivesaw.sawbot.common.observation.*;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import dev.fivesaw.sawbot.forge.client.SawBotKeyBindings;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.SnapshotJsonWriter;
import dev.fivesaw.sawbot.forge.hud.EntityVisualStyle;
import dev.fivesaw.sawbot.forge.hud.KeyLabel;
import dev.fivesaw.sawbot.forge.hud.MotionValue;
import dev.fivesaw.sawbot.forge.hud.UiTheme;
import dev.fivesaw.sawbot.forge.map.LandmarkSensor;
import java.io.StringWriter;
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
        entityVisibilityUpdatesAcrossWallTransition();
        worldSpawnLandmarkResolvesToStandableSurface();
        rollingWindowRemainsBounded();
        keyDefaultsAvoidVanillaFunctionConflicts();
        safetyControllerReleasesEveryHeldControl();
        observationFreezeIsIndependentOfEnableState();
        tracerToggleIsIndependentOfEntityBoxes();
        entityVisualStyleFollowsCurrentVisibility();
        premiumHudThemeIsStable();
        motionValueIsPresentationOnly();
        keyLabelsFollowActualBindings();
        phase1PipelineCreatesBoundedSnapshot();
        frozenPipelinePreservesSnapshot();
        egocentricInverseTransformsAreStable();
        observationDifferenceIsDeterministic();
        blockInspectionUsesSnapshotBasis();
        singleStepRequiresFreezeAndCapturesExactlyOnce();
        snapshotDebugJsonContainsBoundedInputs();
        phase2KeyDefaultsAreStable();
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

    private static void rollingWindowRemainsBounded(){RollingTimingWindow window=new RollingTimingWindow(3);window.add(10);window.add(20);window.add(30);window.add(40);require(window.count()==3,"bounded count");require(window.averageNanos()==30,"ring average");require(window.latestNanos()==40,"latest");require(window.maximumNanos()==40,"maximum");}

    private static void keyDefaultsAvoidVanillaFunctionConflicts(){SawBotKeyBindings keys=new SawBotKeyBindings();require(keys.toggleEnabled.getKeyCode()==org.lwjgl.input.Keyboard.KEY_F10,"enable defaults to F10");require(keys.toggleFreeze.getKeyCode()==org.lwjgl.input.Keyboard.KEY_P,"freeze defaults to P");require(keys.toggleTelemetry.getKeyCode()==org.lwjgl.input.Keyboard.KEY_NONE,"telemetry remains unbound");require(keys.toggleEnabled.getKeyCode()!=63&&keys.toggleEnabled.getKeyCode()!=64&&keys.toggleEnabled.getKeyCode()!=66,"enable avoids F5/F6/F8");require(keys.toggleFreeze.getKeyCode()!=63&&keys.toggleFreeze.getKeyCode()!=64&&keys.toggleFreeze.getKeyCode()!=66,"freeze avoids F5/F6/F8");}
    private static void safetyControllerReleasesEveryHeldControl(){Minecraft minecraft=Minecraft.getMinecraft();int[] keys={1,2,3,4,5,6,7,8,9,10,11};for(int key:keys)KeyBinding.setKeyBindState(key,true);SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());controller.toggleEnabled();require(controller.mode()==SawBotMode.ENABLED,"controller enabled");controller.emergencyStop();require(controller.mode()==SawBotMode.DISABLED,"controller disabled");require("emergency stop".equals(controller.lastStopReason()),"stop reason");for(int key:keys)require(!KeyBinding.isKeyDownForTest(key),"key released "+key);}
    private static void observationFreezeIsIndependentOfEnableState(){Minecraft minecraft=Minecraft.getMinecraft();SawBotStateController controller=new SawBotStateController(minecraft,new TestLogger());require(controller.mode()==SawBotMode.DISABLED,"freeze test starts disabled");require(!controller.observationsFrozen(),"freeze starts off");controller.toggleFrozen();require(controller.observationsFrozen(),"freeze works while disabled");require(controller.mode()==SawBotMode.DISABLED,"freeze does not enable control");require(!controller.mayApplyAutonomousActions(),"disabled frozen state cannot actuate");controller.toggleEnabled();require(controller.mode()==SawBotMode.ENABLED,"enable remains independent");require(controller.observationsFrozen(),"enable preserves frozen snapshot");require(!controller.mayApplyAutonomousActions(),"frozen enabled state cannot actuate");controller.toggleFrozen();require(!controller.observationsFrozen(),"unfreeze works");require(controller.mayApplyAutonomousActions(),"enabled unfrozen state may actuate later");}
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
        EntityObservation los=new EntityObservation(7,70,EntityKind.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,true,false,false,true,false,true,true,1f);
        EntityObservation occ=new EntityObservation(7,70,EntityKind.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,true,false,false,false,true,false,true,1f);
        EntityObservation invalid=new EntityObservation(7,70,EntityKind.PLAYER,TeamRelation.ENEMY,1,0,3,0,0,0,0,0,0,0,0,0.6f,1.8f,20,0,3,0,0,true,false,false,true,true,false,true,1f);
        require(EntityVisualStyle.visibilityRgb(los)==EntityVisualStyle.LOS_RGB,"LOS visual is immediate green");
        require(EntityVisualStyle.visibilityRgb(occ)==EntityVisualStyle.OCCLUDED_RGB,"OCC visual is immediate purple");
        require(EntityVisualStyle.visibilityRgb(invalid)==EntityVisualStyle.INCONSISTENT_RGB,"inconsistent visibility warns orange");
        require("LOS".equals(EntityVisualStyle.visibilityToken(los)),"LOS text and colour share state");
        require("OCC".equals(EntityVisualStyle.visibilityToken(occ)),"OCC text and colour share state");
        require(EntityVisualStyle.visibilityArgb(los)!=(EntityVisualStyle.visibilityArgb(occ)),"visibility colours differ");
    }


    private static void premiumHudThemeIsStable(){
        require(UiTheme.SAFE!=UiTheme.DANGER,"safe and danger colours differ");
        require(UiTheme.CYAN!=UiTheme.PURPLE,"LOS support and OCC colours differ");
        require(((UiTheme.GLASS>>>24)&0xFF)>=200,"glass background remains readable");
        int half=UiTheme.withOpacity(0xC0ABCDEF,0.5f);
        require(((half>>>24)&0xFF)==96,"theme opacity scales alpha");
        require((half&0x00FFFFFF)==0x00ABCDEF,"theme opacity preserves RGB");
        require(UiTheme.RADIUS_PANEL>UiTheme.RADIUS_CARD&&UiTheme.RADIUS_CARD>UiTheme.RADIUS_CHIP,"radius hierarchy");
    }

    private static void motionValueIsPresentationOnly(){
        MotionValue motion=new MotionValue();
        require(motion.update(false,true,1L)==0f,"motion starts closed");
        float opening=motion.update(true,true,50_000_001L);
        require(opening>0f&&opening<1f,"motion opens progressively");
        motion.snap(true,60_000_001L);
        require(motion.value()==1f,"motion can snap without delaying state");
        require(motion.update(false,false,70_000_001L)==0f,"reduced motion closes immediately");
    }

    private static void keyLabelsFollowActualBindings(){
        SawBotKeyBindings keys=new SawBotKeyBindings();
        require("F10".equals(KeyLabel.of(keys.toggleEnabled)),"enable label follows F10 binding");
        require("P".equals(KeyLabel.of(keys.toggleFreeze)),"freeze label follows P binding");
        require(".".equals(KeyLabel.of(keys.stepObservation)),"step label follows period binding");
        require("UNBOUND".equals(KeyLabel.of(keys.toggleTelemetry)),"unbound label is explicit");
    }

    private static void phase1PipelineCreatesBoundedSnapshot(){Minecraft minecraft=Minecraft.getMinecraft();World world=new World();EntityPlayerSP player=new EntityPlayerSP();player.posX=0.5;player.posY=64;player.posZ=0.5;player.onGround=true;world.setBlockStateForTest(new BlockPos(0,63,0),Blocks.wool.getDefaultState());player.inventory.mainInventory[0]=new ItemStack(Items.iron_ingot,4);player.inventory.mainInventory[1]=new ItemStack(new ItemBlock(Blocks.wool),16);EntityPlayer enemy=new EntityPlayer();enemy.posX=3;enemy.posY=64;enemy.posZ=0.5;world.loadedEntityList.add(enemy);minecraft.theWorld=world;minecraft.thePlayer=player;ObservationPipeline pipeline=new ObservationPipeline(minecraft,2);pipeline.tick(1,false);pipeline.tick(2,false);pipelineSnapshot=pipeline.latest();require(pipelineSnapshot!=null,"pipeline snapshot");require(pipelineSnapshot.schemaVersion().equals(SchemaVersion.OBSERVATION_V0_2),"pipeline schema");require(pipelineSnapshot.localTerrain().blockStateIds().length==LocalTerrainSnapshot.CELL_COUNT,"terrain bounded");require(pipelineSnapshot.midRangeMap().relativeSurfaceY().length==MidRangeMapSnapshot.COLUMN_COUNT,"map bounded");require(pipelineSnapshot.entities().count()==1,"entity captured");require(pipelineSnapshot.inventory().iron()==4&&pipelineSnapshot.inventory().wool()==16,"resources captured");require((pipelineSnapshot.sensorValidityFlags()&SensorValidity.ALL_PHASE1)==SensorValidity.ALL_PHASE1,"validity flags");require(pipelineSnapshot.sensorTimings().totalNanos()>=0,"timing captured");}
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
            require(json.contains("\"exportFormat\": \"sawbot.snapshot.debug/0.1\""),"json export format");
            require(json.contains("\"blockStateIds\""),"json terrain arrays");
            require(json.contains("\"relativeSurfaceY\""),"json map arrays");
            require(json.contains("\"slots\""),"json inventory slots");
            require(json.contains("\"previousAction\""),"json action");
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


    private static ObservationSnapshot richSnapshot(){
        SelfState self=new SelfState(18,2,19,4,1,64,3,0.1f,0f,0.2f,0f,0f,0f,15,-5,0,true,false,false,false,false,false,true,false,false,0,1,0,0,0,0,8,1);
        List<EntityObservation> entityList=new ArrayList<EntityObservation>();
        entityList.add(new EntityObservation(4,44,EntityKind.PLAYER,TeamRelation.ENEMY,2,0,4,0.1f,0,0,0,0,0,30,0,0.6f,1.8f,17,5,4.5f,ItemCategory.SWORD.ordinal(),2,true,true,false,true,false,false,true,0.9f));
        EntitySetSnapshot entitySet=new EntitySetSnapshot(entityList,0);
        List<LandmarkObservation> landmarkList=Collections.singletonList(new LandmarkObservation(3,LandmarkType.WORLD_SPAWN,TeamRelation.NEUTRAL,5,0,6,7,8,0.1f,0.3f,1,true));
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
