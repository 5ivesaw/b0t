package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.navigation.AdaptivePathCursor;
import dev.fivesaw.sawbot.common.navigation.IncrementalAStarPlanner;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationGrid;
import dev.fivesaw.sawbot.common.navigation.NavigationPath;
import dev.fivesaw.sawbot.common.navigation.NavigationPlanState;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.navigation.NavigationBodyController;
import dev.fivesaw.sawbot.forge.navigation.WorldNavigationGrid;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

public final class NavigationBodyContractTest {
    private static int checks;

    private NavigationBodyContractTest() { }

    public static void main(String[] args) {
        flatPathIsIncrementalAndDeterministic();
        anytimePlannerExposesPartialRoute();
        pathCursorReanchorsInsideCorridor();
        plannerRoutesAroundObstacle();
        plannerSupportsOneBlockSteps();
        plannerRejectsDisconnectedVoid();
        worldGridRejectsHazardsAndMissingSupport();
        liveRouteValidationDetectsChangedSupport();
        bodyTurnsThenHoldsMovement();
        bodyReplansFromCurrentPositionAfterRelease();
        adaptiveBodyReachesWaypointWithSustainedInput();
        adaptiveBodyReplansAfterExternalDisplacement();
        adaptiveBodyReroutesAfterSupportBreak();
        inputReleaseRestoresPhysicalKeyboardState();
        System.out.println("PASS NavigationBodyContractTest (" + checks + " checks)");
    }

    private static void flatPathIsIncrementalAndDeterministic() {
        SetGrid grid = new SetGrid();
        for (int x = 0; x <= 6; x++) grid.add(x, 64, 0);
        IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
        planner.begin(grid, cell(0, 64, 0), cell(6, 64, 0), 16, 4, 256);
        require(planner.state() == NavigationPlanState.SEARCHING, "flat path starts searching");
        planner.step(1);
        require(planner.expandedNodes() == 1, "one-step budget expands one node");
        while (planner.state() == NavigationPlanState.SEARCHING) planner.step(2);
        require(planner.state() == NavigationPlanState.SUCCEEDED, "flat path succeeds");
        NavigationPath path = planner.path();
        require(path.cell(0).equals(cell(0, 64, 0)), "flat path start");
        require(path.cell(path.size() - 1).equals(cell(6, 64, 0)), "flat path goal");
        require(path.size() == 7, "flat path exact node count");
        require(path.expandedNodes() <= 16, "flat path bounded expansions");
    }


    private static void anytimePlannerExposesPartialRoute() {
        SetGrid grid = new SetGrid();
        for (int x = 0; x <= 20; x++) grid.add(x, 64, 0);
        IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
        planner.begin(grid, cell(0, 64, 0), cell(20, 64, 0), 32, 4, 512);
        planner.step(1);
        NavigationPath partial = planner.bestEffortPath();
        require(partial != null, "anytime planner exposes frontier route");
        require(partial.size() >= 2, "frontier route permits immediate movement");
        require(partial.cell(0).equals(cell(0, 64, 0)), "frontier route keeps start");
        require(partial.cell(partial.size() - 1).x() > 0, "frontier route advances toward goal");
    }

    private static void pathCursorReanchorsInsideCorridor() {
        java.util.ArrayList<NavigationCell> cells = new java.util.ArrayList<NavigationCell>();
        for (int x = 0; x <= 8; x++) cells.add(cell(x, 64, 0));
        NavigationPath path = new NavigationPath(cells, 8F, 9);
        AdaptivePathCursor.Projection projection = AdaptivePathCursor.project(
            path, 1, 5.45D, 64D, 0.52D, 2, 7);
        require(projection.index() == 5, "cursor reanchors to actual current position");
        require(projection.indexDelta() == 4, "cursor reports skipped mandatory nodes");
        require(projection.distanceSquared() < 0.02D, "cursor corridor deviation remains small");
    }

    private static void plannerRoutesAroundObstacle() {
        SetGrid grid = new SetGrid();
        for (int x = 0; x <= 4; x++) {
            for (int z = -2; z <= 2; z++) grid.add(x, 64, z);
        }
        grid.remove(1, 64, 0);
        grid.remove(2, 64, 0);
        grid.remove(3, 64, 0);
        IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
        planner.begin(grid, cell(0, 64, 0), cell(4, 64, 0), 16, 4, 512);
        while (planner.state() == NavigationPlanState.SEARCHING) planner.step(8);
        require(planner.state() == NavigationPlanState.SUCCEEDED, "obstacle route succeeds");
        for (NavigationCell cell : planner.path().cells()) {
            require(grid.isStandable(cell.x(), cell.y(), cell.z()), "path stays standable");
            require(!(cell.z() == 0 && cell.x() >= 1 && cell.x() <= 3), "path avoids wall");
        }
    }

    private static void plannerSupportsOneBlockSteps() {
        SetGrid grid = new SetGrid();
        grid.add(0, 64, 0);
        grid.add(1, 65, 0);
        grid.add(2, 65, 0);
        IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
        planner.begin(grid, cell(0, 64, 0), cell(2, 65, 0), 8, 4, 128);
        while (planner.state() == NavigationPlanState.SEARCHING) planner.step(4);
        require(planner.state() == NavigationPlanState.SUCCEEDED, "step-up route succeeds");
        require(planner.path().cells().contains(cell(1, 65, 0)), "step-up node retained");
    }

    private static void plannerRejectsDisconnectedVoid() {
        SetGrid grid = new SetGrid();
        grid.add(0, 64, 0);
        grid.add(4, 64, 0);
        IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
        planner.begin(grid, cell(0, 64, 0), cell(4, 64, 0), 8, 4, 64);
        while (planner.state() == NavigationPlanState.SEARCHING) planner.step(8);
        require(planner.state() == NavigationPlanState.FAILED, "disconnected void fails");
        require(planner.path() == null, "failed route has no path");
    }

    private static void worldGridRejectsHazardsAndMissingSupport() {
        World world = new World();
        world.setBlockStateForTest(new BlockPos(0, 63, 0), Blocks.wool.getDefaultState());
        world.setBlockStateForTest(new BlockPos(1, 63, 0), Blocks.lava.getDefaultState());
        world.setBlockStateForTest(new BlockPos(2, 64, 0), Blocks.cactus.getDefaultState());
        WorldNavigationGrid grid = new WorldNavigationGrid(world);
        require(grid.isStandable(0, 64, 0), "solid safe floor standable");
        require(!grid.isStandable(1, 64, 0), "liquid support rejected");
        require(!grid.isStandable(2, 64, 0), "hazard in feet cell rejected");
        require(!grid.isStandable(3, 64, 0), "missing support rejected");
        require(grid.cacheSize() == 4, "world standability cache bounded and used");
    }


    private static void liveRouteValidationDetectsChangedSupport() {
        World world = new World();
        java.util.ArrayList<NavigationCell> cells = new java.util.ArrayList<NavigationCell>();
        for (int x = 0; x <= 4; x++) {
            world.setBlockStateForTest(new BlockPos(x, 63, 0), Blocks.wool.getDefaultState());
            cells.add(cell(x, 64, 0));
        }
        NavigationPath path = new NavigationPath(cells, 4F, 5);
        WorldNavigationGrid grid = new WorldNavigationGrid(world);
        require(grid.validatePathWindow(path, 0, 4, true).valid(), "live route initially valid");
        world.setBlockStateForTest(new BlockPos(2, 63, 0), Blocks.air.getDefaultState());
        WorldNavigationGrid.PathValidation changed = grid.validatePathWindow(path, 0, 4, true);
        require(!changed.valid(), "live route notices broken support");
        require(changed.invalidIndex() == 2, "live route reports changed node index");
    }

    private static void bodyTurnsThenHoldsMovement() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = new World();
        for (int x = -2; x <= 8; x++) {
            for (int z = -3; z <= 3; z++) {
                world.setBlockStateForTest(new BlockPos(x, 63, z), Blocks.wool.getDefaultState());
            }
        }
        EntityPlayerSP player = new EntityPlayerSP();
        player.posX = 0.5D;
        player.posY = 64D;
        player.posZ = 0.5D;
        player.rotationYaw = 0F;
        player.onGround = true;
        minecraft.theWorld = world;
        minecraft.thePlayer = player;
        minecraft.currentScreen = null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        Keyboard.clearForTest();

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(5.5D, 64D, 0.5D), "test waypoint set");
        NavigationBodyController body = new NavigationBodyController(minecraft, state, environment,
            waypoint, 16, 4, 512, 32, 4, 80, 32F, 0.75F,
            6, 4.5F, 8, 2.35F, 1.25F, logger);
        state.enable();
        boolean heldForward = false;
        for (int tick = 1; tick <= 40; tick++) {
            body.tick(tick, null);
            heldForward |= KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode());
        }
        require(body.path() != null, "body produced path");
        require(player.rotationYaw < -20F, "body visibly turns toward east target");
        require(heldForward, "body sustains forward binding after alignment");
        require(body.replanCount() >= 1, "body reports plan count");
        state.disableAndRelease("test complete");
        body.tick(41, null);
        require(!KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode()), "body releases owned forward input");
    }


    private static void bodyReplansFromCurrentPositionAfterRelease() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = new World();
        for (int x = -2; x <= 12; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockStateForTest(new BlockPos(x, 63, z), Blocks.wool.getDefaultState());
            }
        }
        EntityPlayerSP player = new EntityPlayerSP();
        player.posX = 0.5D;
        player.posY = 64D;
        player.posZ = 0.5D;
        player.rotationYaw = -90F;
        player.onGround = true;
        minecraft.theWorld = world;
        minecraft.thePlayer = player;
        minecraft.currentScreen = null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        Keyboard.clearForTest();

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(10.5D, 64D, 0.5D), "replan waypoint set");
        NavigationBodyController body = new NavigationBodyController(minecraft, state, environment,
            waypoint, 20, 4, 1024, 64, 4, 80, 32F, 0.75F,
            7, 5F, 10, 2.35F, 1.25F, logger);
        state.enable();
        for (int tick = 1; tick <= 6; tick++) body.tick(tick, null);
        require(body.path() != null, "initial adaptive path exists");

        state.disableAndRelease("manual test takeover");
        body.release("manual test takeover");
        player.posX = 6.5D;
        player.posZ = 1.5D;
        state.enable();
        for (int tick = 7; tick <= 14; tick++) body.tick(tick, null);
        require(body.path() != null, "resume creates current-position path");
        NavigationCell restarted = body.path().cell(0);
        require(Math.abs(restarted.x() - 6) <= 1, "resume route starts near displaced player");
        require(body.replanCount() >= 2, "resume increments real-time replan count");
    }


    private static void adaptiveBodyReachesWaypointWithSustainedInput() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = flatWorld(-4, 20, -8, 8, 63);
        EntityPlayerSP player = testPlayer(0.5D, 64D, 0.5D, -90F);
        prepareMinecraft(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(12.5D, 64D, 4.5D), "adaptive target set");
        NavigationBodyController body = new NavigationBodyController(minecraft, state, environment,
            waypoint, 24, 6, 2048, 16, 4, 60, 32F, 0.80F,
            7, 5F, 12, 2.35F, 1.25F, logger);
        state.enable();
        int consecutiveForward = 0;
        int longestForward = 0;
        int previousHotSwaps = 0;
        boolean previousForward = false;
        boolean hotSwapWhileMoving = false;
        boolean arrived = false;
        for (int tick = 1; tick <= 260; tick++) {
            body.tick(tick, null);
            boolean forward = KeyBinding.isKeyDownForTest(
                minecraft.gameSettings.keyBindForward.getKeyCode());
            if (forward) {
                consecutiveForward++;
                longestForward = Math.max(longestForward, consecutiveForward);
            } else {
                consecutiveForward = 0;
            }
            if (body.hotSwapCount() > previousHotSwaps && forward && previousForward) {
                hotSwapWhileMoving = true;
            }
            previousHotSwaps = body.hotSwapCount();
            previousForward = forward;
            simulateMovement(minecraft, player);
            if ("ARRIVED".equals(body.status())) {
                arrived = true;
                break;
            }
        }
        require(arrived, "adaptive body reaches diagonal waypoint");
        require(longestForward >= 6, "movement is sustained rather than pulsed");
        require(body.hotSwapCount() >= 1, "rolling plans hot-swap while moving");
        require(hotSwapWhileMoving, "hot-swap preserves continuous movement ownership");
    }

    private static void adaptiveBodyReplansAfterExternalDisplacement() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = flatWorld(-8, 30, -12, 12, 63);
        EntityPlayerSP player = testPlayer(0.5D, 64D, 0.5D, -90F);
        prepareMinecraft(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(24.5D, 64D, 0.5D), "displacement target set");
        NavigationBodyController body = new NavigationBodyController(minecraft, state, environment,
            waypoint, 36, 6, 4096, 24, 4, 60, 32F, 0.80F,
            7, 5F, 14, 2.35F, 1.25F, logger);
        state.enable();

        int beforeDisplacementReplans = 0;
        boolean replannedFromDisplacedPosition = false;
        boolean arrived = false;
        for (int tick = 1; tick <= 360; tick++) {
            body.tick(tick, null);
            if (tick == 35) {
                beforeDisplacementReplans = body.replanCount();
                player.posX = 8.5D;
                player.posZ = 7.5D;
                player.motionX = 0D;
                player.motionZ = 0D;
            }
            if (tick > 35 && body.replanCount() > beforeDisplacementReplans
                && body.path() != null && body.path().size() > 0) {
                NavigationCell start = body.path().cell(0);
                if (Math.abs(start.x() - 8) <= 1 && Math.abs(start.z() - 7) <= 1) {
                    replannedFromDisplacedPosition = true;
                }
            }
            simulateMovement(minecraft, player);
            if ("ARRIVED".equals(body.status())) {
                arrived = true;
                break;
            }
        }
        require(replannedFromDisplacedPosition,
            "external displacement replans from actual current position");
        require(body.offRouteReplans() >= 1 || body.routeReanchors() >= 1,
            "external displacement is detected by live corridor tracking");
        require(arrived, "adaptive body reaches goal after external displacement");
    }

    private static void adaptiveBodyReroutesAfterSupportBreak() {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = flatWorld(-3, 18, -5, 5, 63);
        EntityPlayerSP player = testPlayer(0.5D, 64D, 0.5D, -90F);
        prepareMinecraft(minecraft, world, player);

        TestLogger logger = new TestLogger();
        SawBotStateController state = new SawBotStateController(minecraft, logger);
        EnvironmentGuard environment = new EnvironmentGuard(minecraft, true, "localhost");
        NavigationWaypointController waypoint = new NavigationWaypointController(minecraft);
        require(waypoint.setWorldTarget(14.5D, 64D, 0.5D), "dynamic target set");
        NavigationBodyController body = new NavigationBodyController(minecraft, state, environment,
            waypoint, 24, 6, 3072, 24, 4, 60, 32F, 0.80F,
            7, 5F, 14, 2.35F, 1.25F, logger);
        state.enable();
        boolean arrived = false;
        for (int tick = 1; tick <= 320; tick++) {
            body.tick(tick, null);
            if (tick == 22) {
                world.setBlockStateForTest(new BlockPos(7, 63, 0), Blocks.air.getDefaultState());
                world.setBlockStateForTest(new BlockPos(8, 63, 0), Blocks.air.getDefaultState());
            }
            simulateMovement(minecraft, player);
            allowPlannerWorker();
            if ("ARRIVED".equals(body.status())) {
                arrived = true;
                break;
            }
        }
        require(body.routeInvalidations() >= 1, "live support break invalidates route");
        require(body.replanCount() >= 2, "support break triggers replacement plan");
        require(arrived, "adaptive body routes around changed support");
    }

    private static World flatWorld(int minX, int maxX, int minZ, int maxZ, int floorY) {
        World world = new World();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockStateForTest(new BlockPos(x, floorY, z), Blocks.wool.getDefaultState());
            }
        }
        return world;
    }

    private static EntityPlayerSP testPlayer(double x, double y, double z, float yaw) {
        EntityPlayerSP player = new EntityPlayerSP();
        player.posX = x;
        player.posY = y;
        player.posZ = z;
        player.rotationYaw = yaw;
        player.onGround = true;
        return player;
    }

    private static void prepareMinecraft(Minecraft minecraft, World world, EntityPlayerSP player) {
        minecraft.theWorld = world;
        minecraft.thePlayer = player;
        minecraft.currentScreen = null;
        minecraft.setSingleplayerForTest(true);
        KeyBinding.clearForTest();
        Keyboard.clearForTest();
    }

    private static void simulateMovement(Minecraft minecraft, EntityPlayerSP player) {
        boolean forward = KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindForward.getKeyCode());
        boolean back = KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindBack.getKeyCode());
        boolean left = KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindLeft.getKeyCode());
        boolean right = KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindRight.getKeyCode());
        boolean sprint = KeyBinding.isKeyDownForTest(minecraft.gameSettings.keyBindSprint.getKeyCode());
        double forwardAmount = (forward ? 1D : 0D) - (back ? 1D : 0D);
        double strafeAmount = (right ? 1D : 0D) - (left ? 1D : 0D);
        double length = Math.sqrt(forwardAmount * forwardAmount + strafeAmount * strafeAmount);
        if (length > 1D) {
            forwardAmount /= length;
            strafeAmount /= length;
        }
        double speed = sprint ? 0.28D : 0.20D;
        double radians = Math.toRadians(player.rotationYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = Math.cos(radians);
        double rightZ = Math.sin(radians);
        player.motionX = (forwardX * forwardAmount + rightX * strafeAmount) * speed;
        player.motionZ = (forwardZ * forwardAmount + rightZ * strafeAmount) * speed;
        player.posX += player.motionX;
        player.posZ += player.motionZ;
        player.lastTickPosX = player.posX - player.motionX;
        player.lastTickPosZ = player.posZ - player.motionZ;
        player.onGround = true;
    }

    private static void allowPlannerWorker() {
        try {
            Thread.sleep(1L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("planner-worker test interrupted", interrupted);
        }
    }

    private static void inputReleaseRestoresPhysicalKeyboardState() {
        Minecraft minecraft = Minecraft.getMinecraft();
        int code = minecraft.gameSettings.keyBindForward.getKeyCode();
        KeyBinding.clearForTest();
        Keyboard.clearForTest();
        Keyboard.setKeyDownForTest(code, true);
        KeyBinding.setKeyBindState(code, false);
        InputRelease.restorePhysical(minecraft.gameSettings.keyBindForward);
        require(KeyBinding.isKeyDownForTest(code), "release restores held physical key");
        Keyboard.setKeyDownForTest(code, false);
        InputRelease.restorePhysical(minecraft.gameSettings.keyBindForward);
        require(!KeyBinding.isKeyDownForTest(code), "release clears unheld physical key");
    }

    private static NavigationCell cell(int x, int y, int z) {
        return new NavigationCell(x, y, z);
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static final class SetGrid implements NavigationGrid {
        private final Set<NavigationCell> cells = new HashSet<NavigationCell>();
        void add(int x, int y, int z) { cells.add(cell(x, y, z)); }
        void remove(int x, int y, int z) { cells.remove(cell(x, y, z)); }
        @Override public boolean isStandable(int x, int y, int z) { return cells.contains(cell(x, y, z)); }
    }

    private static final class TestLogger implements Logger {
        @Override public void info(String message) { }
        @Override public void info(String message, Object value) { }
        @Override public void warn(String message) { }
        @Override public void error(String message, Throwable throwable) { }
    }
}
