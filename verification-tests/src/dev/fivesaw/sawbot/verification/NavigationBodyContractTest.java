package dev.fivesaw.sawbot.verification;

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
        plannerRoutesAroundObstacle();
        plannerSupportsOneBlockSteps();
        plannerRejectsDisconnectedVoid();
        worldGridRejectsHazardsAndMissingSupport();
        bodyTurnsThenHoldsMovement();
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
            waypoint, 16, 4, 512, 32, 10, 20, 18F, 0.75F, logger);
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
