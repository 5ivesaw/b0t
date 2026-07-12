package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.bridging.BridgeCorridorPlan;
import dev.fivesaw.sawbot.common.bridging.BridgeCorridorPlanner;
import dev.fivesaw.sawbot.common.bridging.BridgePlacementStep;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.bridging.BridgingBodyController;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public final class BridgingBodyContractTest {
    private static int checks;

    private BridgingBodyContractTest() { }

    public static void main(String[] args) {
        straightCorridorUsesCardinalAttachments();
        diagonalCorridorNeverEmitsDiagonalJump();
        corridorRespectsHardBound();
        bodyPlacesAndCrossesShortGap();
        bodyReportsOutOfBlocksWithoutUnsafeMovement();
        releaseRestoresOriginalHotbarSlot();
        overlayLifecycleClearsImmediately();
        System.out.println("PASS BridgingBodyContractTest (" + checks + " checks)");
    }

    private static void straightCorridorUsesCardinalAttachments() {
        BridgeCorridorPlan plan = new BridgeCorridorPlanner().plan(
            cell(0, 64, 0), cell(5, 64, 0), 8);
        require(plan.reachesGoal(), "straight corridor reaches target");
        require(plan.size() == 5, "straight corridor exact step count");
        for (int index = 0; index < plan.size(); index++) {
            BridgePlacementStep step = plan.step(index);
            require(step.feetCell().x() == index + 1, "straight feet cell advances east");
            require(step.supportCell().y() == 63, "support is beneath feet");
            require(step.direction().dx() == 1 && step.direction().dz() == 0,
                "straight attachment direction is cardinal east");
        }
    }

    private static void diagonalCorridorNeverEmitsDiagonalJump() {
        BridgeCorridorPlan plan = new BridgeCorridorPlanner().plan(
            cell(0, 64, 0), cell(5, 64, 4), 16);
        require(plan.reachesGoal(), "diagonal staircase reaches target");
        NavigationCell previous = plan.start();
        for (BridgePlacementStep step : plan.steps()) {
            int dx = Math.abs(step.feetCell().x() - previous.x());
            int dz = Math.abs(step.feetCell().z() - previous.z());
            require(dx + dz == 1, "diagonal bridge uses one legal face per step");
            previous = step.feetCell();
        }
        require(previous.equals(cell(5, 64, 4)), "diagonal final cell exact");
    }

    private static void corridorRespectsHardBound() {
        BridgeCorridorPlan plan = new BridgeCorridorPlanner().plan(
            cell(0, 64, 0), cell(30, 64, 0), 12);
        require(!plan.reachesGoal(), "bounded corridor reports partial result");
        require(plan.size() == 12, "bounded corridor never exceeds maximum");
        require(plan.step(11).feetCell().equals(cell(12, 64, 0)),
            "partial corridor advances predictably");
    }

    private static void bodyPlacesAndCrossesShortGap() {
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = new WorldClient();
        world.setBlockStateForTest(new BlockPos(0, 63, 0), Blocks.wool.getDefaultState());
        world.setBlockStateForTest(new BlockPos(4, 63, 0), Blocks.wool.getDefaultState());
        EntityPlayerSP player = player(0.5D, 64D, 0.5D, -90F);
        player.inventory.mainInventory[2] = new ItemStack(new ItemBlock(Blocks.wool), 16);
        player.inventory.currentItem = 0;
        prepare(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(4.5D, 64D, 0.5D), "bridge waypoint set");
        BridgingBodyController body = new BridgingBodyController(minecraft, state, environment,
            waypoint, 12, 6, 3, 4, 38F, 28F, logger);
        state.enable();
        require(body.shouldOwnBridge("NO_PATH"), "automatic bridge handoff sees missing support");

        boolean complete = false;
        for (int tick = 1; tick <= 220; tick++) {
            body.tick(tick, null);
            simulateSneakMovement(minecraft, player);
            if (isSolid(world, new BlockPos(1, 63, 0))
                && isSolid(world, new BlockPos(2, 63, 0))
                && isSolid(world, new BlockPos(3, 63, 0))
                && player.posX >= 3.45D) {
                complete = true;
                break;
            }
        }
        require(complete, "body places and crosses three-block gap");
        require(body.placedBlocks() >= 3, "body confirms placed supports");
        require(body.failedPlacements() == 0, "successful gap has no failed placements");
        require(body.evaluatedPlacementCandidates() > 0,
            "bridge evaluates multiple legal face samples");
        require(body.visiblePlacementCandidates() > 0,
            "bridge finds at least one visible reachable face");
        require(minecraft.playerController.rightClicksForTest() >= 3,
            "one-at-a-time legal controller placements occur");
        require(player.inventory.mainInventory[2].stackSize <= 13,
            "placement consumes selected blocks");
    }

    private static void bodyReportsOutOfBlocksWithoutUnsafeMovement() {
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = new WorldClient();
        world.setBlockStateForTest(new BlockPos(0, 63, 0), Blocks.wool.getDefaultState());
        EntityPlayerSP player = player(0.5D, 64D, 0.5D, -90F);
        prepare(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(3.5D, 64D, 0.5D), "no-block waypoint set");
        BridgingBodyController body = new BridgingBodyController(minecraft, state, environment,
            waypoint, 8, 6, 2, 4, 38F, 28F, logger);
        state.enable();
        body.tick(1L, null);
        require("OUT_OF_BLOCKS".equals(body.status()), "missing blocks is explicit");
        require(!KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode()),
            "missing blocks never commands forward into void");
        require(!body.ownsInputs(), "missing blocks releases synthetic ownership");
    }

    private static void releaseRestoresOriginalHotbarSlot() {
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = new WorldClient();
        world.setBlockStateForTest(new BlockPos(0, 63, 0), Blocks.wool.getDefaultState());
        EntityPlayerSP player = player(0.5D, 64D, 0.5D, -90F);
        player.inventory.mainInventory[6] = new ItemStack(new ItemBlock(Blocks.wool), 8);
        player.inventory.currentItem = 1;
        prepare(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(2.5D, 64D, 0.5D), "slot restore waypoint set");
        BridgingBodyController body = new BridgingBodyController(minecraft, state, environment,
            waypoint, 8, 6, 2, 4, 38F, 28F, logger);
        state.enable();
        body.tick(1L, null);
        require(player.inventory.currentItem == 6, "body selects best bridge block slot");
        body.release("test release");
        require(player.inventory.currentItem == 1, "release restores original hotbar slot");
        require(!KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindSneak.getKeyCode()),
            "release restores sneak state");
    }

    private static void overlayLifecycleClearsImmediately() {
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = new WorldClient();
        world.setBlockStateForTest(new BlockPos(0, 63, 0),
            Blocks.wool.getDefaultState());
        EntityPlayerSP player = player(0.5D, 64D, 0.5D, -90F);
        player.inventory.mainInventory[2] =
            new ItemStack(new ItemBlock(Blocks.wool), 16);
        prepare(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state =
            new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment =
            new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint =
            new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(3.5D, 64D, 0.5D),
            "overlay waypoint set");
        BridgingBodyController body = new BridgingBodyController(
            minecraft, state, environment, waypoint,
            8, 6, 2, 4, 38F, 28F, logger);
        state.enable();
        body.tick(1L, null);
        require(body.shouldRenderOverlay(),
            "active bridge plan renders while specialist is active");
        require(!body.planSteps().isEmpty(),
            "active bridge plan exposes bounded markers");

        body.deactivate("navigation body priority");
        require(!body.shouldRenderOverlay(),
            "priority handoff clears bridge overlay immediately");
        require(body.planSteps().isEmpty(),
            "deactivated bridge exposes no stale plan markers");
        require(!body.shouldDisplayHud(),
            "deactivated bridge removes stale HUD block");

        require(waypoint.setWorldTarget(4.5D, 64D, 0.5D),
            "replacement waypoint set");
        body.tick(2L, null);
        require(body.shouldRenderOverlay(),
            "new waypoint creates a fresh overlay");
        body.onWaypointCleared();
        require(!body.shouldRenderOverlay(),
            "waypoint clear removes overlay immediately");
        require(body.planSteps().isEmpty(),
            "waypoint clear removes old bridge path");
        require(!body.shouldDisplayHud(),
            "waypoint clear removes bridge HUD immediately");
    }

    private static EntityPlayerSP player(double x, double y, double z, float yaw) {
        EntityPlayerSP player = new EntityPlayerSP();
        player.posX = x;
        player.posY = y;
        player.posZ = z;
        player.rotationYaw = yaw;
        player.rotationPitch = 0F;
        player.onGround = true;
        return player;
    }

    private static void prepare(Minecraft minecraft, WorldClient world, EntityPlayerSP player) {
        minecraft.theWorld = world;
        minecraft.thePlayer = player;
        minecraft.currentScreen = null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        Keyboard.clearForTest();
    }

    private static void simulateSneakMovement(Minecraft minecraft, EntityPlayerSP player) {
        boolean forward = KeyBinding.isKeyDownForTest(
            minecraft.gameSettings.keyBindForward.getKeyCode());
        if (!forward) return;
        double speed = 0.12D;
        double radians = Math.toRadians(player.rotationYaw);
        player.motionX = -Math.sin(radians) * speed;
        player.motionZ = Math.cos(radians) * speed;
        player.posX += player.motionX;
        player.posZ += player.motionZ;
        player.lastTickPosX = player.posX - player.motionX;
        player.lastTickPosZ = player.posZ - player.motionZ;
    }

    private static boolean isSolid(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().getMaterial().blocksMovement();
    }

    private static NavigationCell cell(int x, int y, int z) {
        return new NavigationCell(x, y, z);
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static final class TestLogger implements Logger {
        @Override public void info(String message) { }
        @Override public void info(String message, Object value) { }
        @Override public void warn(String message) { }
        @Override public void error(String message, Throwable throwable) { }
    }
}
