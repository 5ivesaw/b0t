package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import dev.fivesaw.sawbot.common.combat.CombatMovementDecision;
import dev.fivesaw.sawbot.common.combat.CombatMovementPlanner;
import dev.fivesaw.sawbot.common.events.EventHistorySnapshot;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.EntitySetSnapshot;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemCategory;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkSetSnapshot;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.MidRangeMapSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.common.observation.SensorValidity;
import dev.fivesaw.sawbot.common.observation.ServerTimingSnapshot;
import dev.fivesaw.sawbot.common.observation.TaskStateSnapshot;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.combat.CombatBodyController;
import dev.fivesaw.sawbot.forge.combat.HumanRotationController;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.tracking.EntityTrackerSensor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public final class CombatBodyContractTest {
    private static int checks;

    private CombatBodyContractTest() { }

    public static void main(String[] args) {
        movementPlannerMaintainsSpacingAndTiming();
        movementPlannerGuardsMissingSupport();
        humanRotationIsBoundedAndContinuous();
        bodyNeverAutoSelectsTarget();
        manualTargetTurnsAndAttacksLegally();
        brainTargetIntentIsExplicitAndSwitchable();
        teammateTargetIsRejected();
        lostTargetReleasesOwnedInputs();
        worldUnloadClearsStaleIntent();
        System.out.println("PASS CombatBodyContractTest (" + checks + " checks)");
    }

    private static void movementPlannerMaintainsSpacingAndTiming() {
        CombatMovementPlanner planner = new CombatMovementPlanner(16, 4);
        CombatMovementDecision approach = planner.plan(1L, 3, 5F, 0F, 0F,
            true, false, 0, 20, true, true, true, true);
        require(approach.forward() > 0.9F, "distant target produces approach");
        require(approach.sprint(), "long approach permits sprint");
        require("APPROACH".equals(approach.mode()), "approach mode exposed");

        CombatMovementDecision retreat = planner.plan(2L, 3, 1.1F, 0F, 0F,
            true, true, 0, 20, true, true, true, true);
        require(retreat.forward() < 0F, "too-close target produces retreat");
        require(!retreat.sprint(), "retreat does not sprint");

        CombatMovementDecision attack = planner.plan(20L, 3, 2.6F, 2F, 2F,
            true, true, 0, 4, true, true, true, true);
        require(attack.attack(), "aligned in-range target permits timed attack");
        CombatMovementDecision cooldown = planner.plan(21L, 3, 2.6F, 2F, 2F,
            true, true, 0, 3, true, true, true, true);
        require(!cooldown.attack(), "attack cooldown blocks early repeat");
        CombatMovementDecision occluded = planner.plan(22L, 3, 2.6F, 0F, 0F,
            false, true, 0, 20, true, true, true, true);
        require(!occluded.attack() && occluded.forward() == 0F,
            "occluded target produces no blind motor action");
    }

    private static void movementPlannerGuardsMissingSupport() {
        CombatMovementPlanner planner = new CombatMovementPlanner(8, 4);
        CombatMovementDecision guarded = planner.plan(8L, 2, 5F, 0F, 0F,
            true, false, 0, 20, false, true, false, true);
        require(guarded.edgeGuarded(), "missing support activates edge guard");
        require(guarded.forward() == 0F, "edge guard cancels unsupported advance");
        require(guarded.strafe() >= 0F, "unsafe strafe redirects toward support");
        require("EDGE_GUARD".equals(guarded.mode()), "edge guard mode exposed");
    }

    private static void humanRotationIsBoundedAndContinuous() {
        HumanRotationController controller = new HumanRotationController();
        HumanRotationController.RotationStep first = controller.step(0F, 0F,
            150F, 60F, 36F, 22F);
        require(Math.abs(first.yawStep()) <= 12.001F,
            "first yaw step respects acceleration limit");
        require(Math.abs(first.pitchStep()) <= 8.001F,
            "first pitch step respects acceleration limit");
        HumanRotationController.RotationStep second = controller.step(
            first.yawStep(), first.pitchStep(), 150F, 60F, 36F, 22F);
        require(Math.abs(second.yawStep() - first.yawStep()) <= 12.001F,
            "yaw acceleration stays continuous");
        require(Math.abs(second.pitchStep() - first.pitchStep()) <= 8.001F,
            "pitch acceleration stays continuous");
        require(Math.abs(second.yawStep()) <= 36.001F,
            "yaw rate remains configured-bounded");
        require(Math.abs(second.pitchStep()) <= 22.001F,
            "pitch rate remains configured-bounded");
    }

    private static void bodyNeverAutoSelectsTarget() {
        Fixture fixture = fixture(2.8D, false);
        int attacksBefore = fixture.minecraft.playerController.attacksForTest();
        fixture.state.enable();
        fixture.body.tick(1L, fixture.snapshot(1L));
        require(!fixture.body.hasIntent(), "body has no implicit target intent");
        require(fixture.body.activeTargetTrackingId() < 0,
            "body never chooses nearest entity automatically");
        require(fixture.minecraft.playerController.attacksForTest() == attacksBefore,
            "body does not attack without explicit target");
        require(!movementHeld(fixture.minecraft),
            "body does not claim movement without explicit target");
    }

    private static void manualTargetTurnsAndAttacksLegally() {
        Fixture fixture = fixture(2.8D, false);
        EntityObservation target = fixture.targetObservation(1L);
        require(fixture.body.toggleManualIntent(target.trackingId()),
            "manual selected-target harness arms");
        fixture.state.enable();
        int attacksBefore = fixture.minecraft.playerController.attacksForTest();
        boolean engaged = false;
        for (int tick = 1; tick <= 16; tick++) {
            fixture.target.hurtTime = Math.max(0, fixture.target.hurtTime - 1);
            fixture.body.tick(tick, fixture.snapshot(tick));
            engaged |= "ENGAGE".equals(fixture.body.status());
        }
        require(engaged, "combat body enters engage state");
        require("manual".equals(fixture.body.source()),
            "manual target source remains visible");
        require(fixture.body.activeTargetTrackingId() == target.trackingId(),
            "selected tracking id remains authoritative");
        require(fixture.minecraft.playerController.attacksForTest() > attacksBefore,
            "legal aligned attack reaches Minecraft controller");
        require(fixture.minecraft.playerController.lastAttackTargetForTest()
            == fixture.target, "attack targets selected Minecraft entity");
        require(fixture.player.swingsForTest() > 0,
            "visible hand swing accompanies attack");
        require(Math.abs(fixture.body.yawError()) <= 6.1F,
            "visible rotation converges within attack tolerance");
    }

    private static void brainTargetIntentIsExplicitAndSwitchable() {
        Fixture fixture = fixture(3.0D, false);
        EntityObservation target = fixture.targetObservation(1L);
        ActionCommand pvp = new ActionCommand(1L, 1L, "brain-test/0.1",
            0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
            ActionCommand.KEEP_CURRENT_HOTBAR_SLOT, Skill.PVP,
            target.trackingId(), -1, 1F, 1,
            TacticalObjective.INTERCEPT_INCOMING_PLAYER,
            AbortCondition.NONE);
        fixture.body.observeBrainAction(new ModelActionEnvelope(pvp, 1L, 1L));
        require(fixture.body.hasIntent(), "brain PVP action creates explicit intent");
        require(fixture.body.brainTargetTrackingId() == target.trackingId(),
            "brain-selected tracking id is retained");
        fixture.state.enable();
        fixture.body.tick(2L, fixture.snapshot(2L));
        require("brain".equals(fixture.body.source()),
            "brain source exposed by combat body");

        ActionCommand navigate = new ActionCommand(2L, 2L, "brain-test/0.1",
            0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F,
            ActionCommand.KEEP_CURRENT_HOTBAR_SLOT, Skill.NAVIGATION,
            -1, 1000, 1F, 1, TacticalObjective.RUSH_ENEMY_BED,
            AbortCondition.NONE);
        fixture.body.observeBrainAction(new ModelActionEnvelope(navigate, 2L, 1L));
        require(!fixture.body.hasIntent(),
            "explicit brain skill switch releases combat intent");
    }

    private static void teammateTargetIsRejected() {
        Fixture fixture = fixture(2.5D, true);
        EntityObservation target = fixture.targetObservation(1L);
        fixture.body.toggleManualIntent(target.trackingId());
        fixture.state.enable();
        int attacksBefore = fixture.minecraft.playerController.attacksForTest();
        fixture.body.tick(1L, fixture.snapshot(1L));
        require("REJECTED".equals(fixture.body.status()),
            "teammate target is rejected");
        require(fixture.body.rejectedTargets() >= 1,
            "rejected target counter increments");
        require(fixture.minecraft.playerController.attacksForTest() == attacksBefore,
            "teammate is never attacked");
        require(!movementHeld(fixture.minecraft),
            "rejected target releases movement");
    }

    private static void lostTargetReleasesOwnedInputs() {
        Fixture fixture = fixture(5.0D, false);
        EntityObservation target = fixture.targetObservation(1L);
        fixture.body.toggleManualIntent(target.trackingId());
        fixture.state.enable();
        fixture.body.tick(1L, fixture.snapshot(1L));
        require(movementHeld(fixture.minecraft),
            "approach owns a movement binding");
        fixture.world.loadedEntityList.remove(fixture.target);
        fixture.body.tick(2L, fixture.snapshot(2L));
        require("WAIT_TARGET".equals(fixture.body.status()),
            "lost selected target enters wait state");
        require(fixture.body.lostTargets() >= 1,
            "lost target counter increments");
        require(!movementHeld(fixture.minecraft),
            "lost target restores all movement bindings");
        fixture.body.release("test complete");
        require(!fixture.body.ownsInputs(),
            "explicit release clears combat ownership");
    }

    private static void worldUnloadClearsStaleIntent() {
        Fixture fixture = fixture(2.8D, false);
        EntityObservation target = fixture.targetObservation(1L);
        fixture.body.toggleManualIntent(target.trackingId());
        require(fixture.body.hasIntent(), "manual intent armed before unload");
        fixture.body.onWorldUnavailable();
        require(!fixture.body.hasIntent(),
            "world unload clears stale manual and brain targets");
        require(fixture.body.activeTargetTrackingId() < 0,
            "world unload clears active tracking id");
        require(!movementHeld(fixture.minecraft),
            "world unload restores movement bindings");
    }

    private static Fixture fixture(double targetX, boolean teammate) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = flatWorld(-5, 12, -6, 6, 63);
        EntityPlayerSP player = new EntityPlayerSP();
        player.posX = 0.5D;
        player.posY = 64D;
        player.posZ = 0.5D;
        player.rotationYaw = -90F;
        player.rotationPitch = 0F;
        player.onGround = true;
        EntityPlayer target = new EntityPlayer();
        target.posX = targetX;
        target.posY = 64D;
        target.posZ = 0.5D;
        target.onGround = true;
        Team selfTeam = new Team();
        Team targetTeam = teammate ? selfTeam : new Team();
        player.setTeamForTest(selfTeam);
        target.setTeamForTest(targetTeam);
        world.loadedEntityList.add(target);
        minecraft.theWorld = world;
        minecraft.thePlayer = player;
        minecraft.currentScreen = null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        Keyboard.clearForTest();

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true,
            "localhost");
        CombatBodyController body = new CombatBodyController(minecraft, state,
            environment, 8F, 3.05F, 16, 4, 36F, 22F, logger);
        return new Fixture(minecraft, world, player, target, state, body,
            new EntityTrackerSensor());
    }

    private static World flatWorld(int minX, int maxX, int minZ, int maxZ,
                                   int floorY) {
        World world = new World();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockStateForTest(new BlockPos(x, floorY, z),
                    Blocks.wool.getDefaultState());
            }
        }
        return world;
    }

    private static boolean movementHeld(Minecraft minecraft) {
        return KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindForward.getKeyCode())
            || KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindBack.getKeyCode())
            || KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindLeft.getKeyCode())
            || KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindRight.getKeyCode())
            || KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindSprint.getKeyCode());
    }

    private static ObservationSnapshot snapshot(long tick,
                                                EntityPlayerSP player,
                                                EntitySetSnapshot entities) {
        SelfState self = new SelfState(20F, 0F, 20F, 0F,
            player.posX, player.posY, player.posZ,
            0F, 0F, 0F, 0F, 0F, 0F,
            player.rotationYaw, player.rotationPitch, player.fallDistance,
            player.onGround, player.isCollidedHorizontally,
            player.isCollidedVertically, false, false, false,
            false, false, false, 0, 0, 0,
            2F, 2F, 2F, 8F, 0);
        return new ObservationSnapshot(tick, 1000L + tick,
            new UUID(12L, 34L), tick, "world:combat-test", "universal/0.1",
            self, terrain(), midRange(), entities, inventory(), landmarks(),
            events(), timing(), TaskStateSnapshot.UNIVERSAL,
            ActionCommand.zero(Math.max(0L, tick - 1L), 999L + tick, "none"),
            SensorValidity.ALL_PHASE1, sensorTimings());
    }

    private static LocalTerrainSnapshot terrain() {
        return new LocalTerrainSnapshot(0, 64, 0, (byte)0,
            new short[LocalTerrainSnapshot.CELL_COUNT],
            new byte[LocalTerrainSnapshot.CELL_COUNT],
            new short[LocalTerrainSnapshot.CELL_COUNT],
            new byte[LocalTerrainSnapshot.CELL_COUNT], 0);
    }

    private static MidRangeMapSnapshot midRange() {
        return new MidRangeMapSnapshot(0, 0, 64, (byte)0,
            new short[MidRangeMapSnapshot.COLUMN_COUNT],
            new short[MidRangeMapSnapshot.COLUMN_COUNT],
            new short[MidRangeMapSnapshot.COLUMN_COUNT], 0);
    }

    private static InventorySnapshot inventory() {
        List<ItemSlotObservation> slots = new ArrayList<ItemSlotObservation>();
        for (int index = 0; index < 41; index++) {
            slots.add(new ItemSlotObservation(index, 0, 0, 0, 0, 0,
                ItemCategory.EMPTY));
        }
        return new InventorySnapshot(slots, 0, "NONE", 0, 0, 0, 0, 0);
    }

    private static LandmarkSetSnapshot landmarks() {
        return new LandmarkSetSnapshot(
            Collections.<LandmarkObservation>emptyList());
    }

    private static EventHistorySnapshot events() {
        return new EventHistorySnapshot(
            Collections.<ObservationEvent>emptyList(), 0);
    }

    private static ServerTimingSnapshot timing() {
        return new ServerTimingSnapshot(0, 0, 65535, 65535, 65535, 65535,
            false);
    }

    private static SensorTimings sensorTimings() {
        return new SensorTimings(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static final class Fixture {
        final Minecraft minecraft;
        final World world;
        final EntityPlayerSP player;
        final EntityPlayer target;
        final SawBotStateController state;
        final CombatBodyController body;
        final EntityTrackerSensor tracker;

        Fixture(Minecraft minecraft, World world, EntityPlayerSP player,
                EntityPlayer target, SawBotStateController state,
                CombatBodyController body, EntityTrackerSensor tracker) {
            this.minecraft = minecraft;
            this.world = world;
            this.player = player;
            this.target = target;
            this.state = state;
            this.body = body;
            this.tracker = tracker;
        }

        ObservationSnapshot snapshot(long tick) {
            return CombatBodyContractTest.snapshot(tick, player,
                tracker.capture(player, world, tick));
        }

        EntityObservation targetObservation(long tick) {
            EntitySetSnapshot entities = tracker.capture(player, world, tick);
            for (EntityObservation observation : entities.entities()) {
                if (observation.minecraftEntityId() == target.getEntityId()) {
                    return observation;
                }
            }
            throw new AssertionError("target observation missing");
        }
    }

    private static final class TestLogger implements Logger {
        @Override public void info(String message) { }
        @Override public void info(String message, Object value) { }
        @Override public void warn(String message) { }
        @Override public void error(String message, Throwable throwable) { }
    }
}
