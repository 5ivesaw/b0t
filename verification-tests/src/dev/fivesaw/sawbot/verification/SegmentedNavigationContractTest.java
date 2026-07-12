package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.navigation.ImmutableNavigationGrid;
import dev.fivesaw.sawbot.common.navigation.MovementAStarPlanner;
import dev.fivesaw.sawbot.common.navigation.MovementPath;
import dev.fivesaw.sawbot.common.navigation.MovementPlanResult;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationMovement;
import dev.fivesaw.sawbot.common.navigation.NavigationMovementType;
import dev.fivesaw.sawbot.common.navigation.PathSegmentCoordinator;
import dev.fivesaw.sawbot.forge.navigation.NavigationPlannerWorker;
import dev.fivesaw.sawbot.forge.navigation.NavigationSnapshotCapture;
import dev.fivesaw.sawbot.forge.navigation.WorldNavigationGrid;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

/** Contract tests for the Phase 9 immutable planner and segmented executor core. */
public final class SegmentedNavigationContractTest {
    private static int checks;

    private SegmentedNavigationContractTest() { }

    public static void main(String[] args) throws Exception {
        immutableGridCopiesBuilderState();
        movementPlannerProducesTypedOperations();
        movementPlannerRoutesAroundUnsafeCells();
        coordinatorRewindsSkipsAndUsesCorridorProjection();
        coordinatorSplicesReplacementAtSharedPosition();
        snapshotCaptureProducesLocalThenFullRequests();
        plannerWorkerProcessesImmutableRequestOffThread();
        worldGridReusesPersistentCache();
        System.out.println("PASS SegmentedNavigationContractTest (" + checks + " checks)");
    }

    private static void immutableGridCopiesBuilderState() {
        ImmutableNavigationGrid.Builder builder = new ImmutableNavigationGrid.Builder(
            -1, 63, -1, 3, 66, 2);
        for (int x = -1; x <= 3; x++) {
            for (int z = -1; z <= 2; z++) builder.setStandable(x, 64, z, true);
        }
        ImmutableNavigationGrid first = builder.build();
        builder.setStandable(1, 64, 0, false);
        ImmutableNavigationGrid second = builder.build();
        require(first.isStandable(1, 64, 0), "built grid is detached from builder mutation");
        require(!second.isStandable(1, 64, 0), "later build sees later builder mutation");
        require(first.sampledCells() == 20, "immutable grid records sample count");
        require(first.traversalPenalty(1, 64, 0) == 0F, "interior cell has no edge penalty");
        require(first.traversalPenalty(-1, 64, -1) > 0F, "exposed cell has risk penalty");
    }

    private static void movementPlannerProducesTypedOperations() {
        ImmutableNavigationGrid.Builder builder = new ImmutableNavigationGrid.Builder(
            0, 63, -1, 5, 66, 2);
        set(builder, 0, 64, 0);
        set(builder, 1, 64, 0);
        set(builder, 2, 65, 0);
        set(builder, 3, 65, 0);
        set(builder, 4, 64, 0);
        ImmutableNavigationGrid grid = builder.build();
        MovementPlanResult result = new MovementAStarPlanner().plan(grid,
            cell(0, 64, 0), cell(4, 64, 0), 8, 3, 256, 1.05F, 11L, true);
        require(result.succeeded(), "typed movement route succeeds");
        MovementPath path = result.path();
        require(path.complete(), "typed route is complete");
        require(path.start().equals(cell(0, 64, 0)), "typed route start preserved");
        require(path.destination().equals(cell(4, 64, 0)), "typed route goal preserved");
        boolean ascend = false;
        boolean descend = false;
        for (NavigationMovement movement : path.movements()) {
            ascend |= movement.type() == NavigationMovementType.ASCEND;
            descend |= movement.type() == NavigationMovementType.DESCEND;
            require(movement.safeToCancel(), "normal movement exposes safe cancel boundary");
        }
        require(ascend, "route contains ascent primitive");
        require(descend, "route contains descent primitive");
    }

    private static void movementPlannerRoutesAroundUnsafeCells() {
        ImmutableNavigationGrid.Builder builder = new ImmutableNavigationGrid.Builder(
            0, 63, -3, 8, 66, 3);
        for (int x = 0; x <= 8; x++) {
            for (int z = -3; z <= 3; z++) {
                if (!(z == 0 && x >= 2 && x <= 6)) set(builder, x, 64, z);
            }
        }
        ImmutableNavigationGrid grid = builder.build();
        MovementPlanResult result = new MovementAStarPlanner().plan(grid,
            cell(0, 64, 0), cell(8, 64, 0), 12, 3, 2048, 1.10F, 12L, true);
        require(result.succeeded(), "worker planner routes around obstacle band");
        require(result.expandedNodes() <= 2048, "worker planner obeys node cap");
        for (NavigationCell position : result.path().positions()) {
            require(grid.isStandable(position.x(), position.y(), position.z()),
                "planned operation remains on immutable safe cell");
            require(!(position.z() == 0 && position.x() >= 2 && position.x() <= 6),
                "planned operation avoids blocked band");
        }
    }

    private static void coordinatorRewindsSkipsAndUsesCorridorProjection() {
        MovementPath path = straightPath(0, 8, 30L);
        PathSegmentCoordinator coordinator = new PathSegmentCoordinator();
        coordinator.install(path, cell(0, 64, 0));
        require(coordinator.reconcile(cell(5, 64, 0), 2, 8),
            "coordinator skips to displaced forward position");
        require(coordinator.movementIndex() == 5, "forward skip updates operation index");
        require(coordinator.skips() == 5, "forward skip metric recorded");
        require(coordinator.reconcile(cell(3, 64, 0), 4, 8),
            "coordinator rewinds after server correction");
        require(coordinator.movementIndex() == 3, "rewind updates operation index");
        require(coordinator.rewinds() == 2, "rewind metric recorded");
        require(coordinator.reconcileNearby(4.5D, 64D, 1.35D, 1.5D, 4, 8),
            "near-route displacement remains in continuous corridor");
        require(coordinator.corridorRecoveries() == 1,
            "corridor recovery metric recorded");
        require(!coordinator.reconcileNearby(4.5D, 64D, 4.5D, 1.5D, 4, 8),
            "large displacement refuses rigid stale route");
    }

    private static void coordinatorSplicesReplacementAtSharedPosition() {
        MovementPath first = straightPath(0, 8, 40L);
        ArrayList<NavigationMovement> replacement = new ArrayList<NavigationMovement>();
        NavigationCell previous = cell(4, 64, 0);
        for (int z = 1; z <= 5; z++) {
            NavigationCell next = cell(4, 64, z);
            replacement.add(new NavigationMovement(previous, next,
                NavigationMovementType.TRAVERSE, 1F, 5, true));
            previous = next;
        }
        MovementPath second = new MovementPath(replacement, 5F, 12, true, 41L);
        PathSegmentCoordinator coordinator = new PathSegmentCoordinator();
        coordinator.install(first, cell(0, 64, 0));
        coordinator.reconcile(cell(3, 64, 0), 2, 8);
        coordinator.stage(second);
        require(coordinator.trySplice(cell(4, 64, 0)),
            "replacement path splices at shared safe position");
        require(coordinator.activePath() == second, "replacement becomes active");
        require(!coordinator.hasStagedPath(), "staged path cleared after splice");
        require(coordinator.splices() == 1, "splice metric recorded");
    }

    private static void snapshotCaptureProducesLocalThenFullRequests() {
        World world = flatWorld(-12, 24, -12, 12, 63);
        WorldNavigationGrid live = new WorldNavigationGrid(world);
        NavigationSnapshotCapture capture = new NavigationSnapshotCapture(live,
            7L, 3L, cell(0, 64, 0), cell(20, 64, 0), 24, 6,
            3072, 1.10F, 6, 8);
        int ticks = 0;
        while (!capture.localReady() && ticks++ < 100) capture.tick(64);
        require(capture.localReady(), "bounded local snapshot finishes first");
        NavigationPlannerWorker.PlanRequest local = capture.takeLocalRequest();
        require(local != null, "local snapshot emits provisional request");
        require(local.provisional(), "local request is marked provisional");
        require(local.grid().sampledCells() > 0, "local request owns immutable samples");
        require(capture.takeLocalRequest() == null, "local request is emitted once");
        while (!capture.fullReady() && ticks++ < 1000) capture.tick(128);
        require(capture.fullReady(), "full corridor snapshot finishes incrementally");
        NavigationPlannerWorker.PlanRequest full = capture.takeFullRequest();
        require(full != null, "full snapshot emits replacement request");
        require(!full.provisional(), "full request is non-provisional");
        require(full.requestId() > local.requestId(), "full request supersedes local request");
        require(capture.progressPercent() == 100, "capture reaches one hundred percent");
    }

    private static void plannerWorkerProcessesImmutableRequestOffThread()
            throws InterruptedException {
        ImmutableNavigationGrid.Builder builder = new ImmutableNavigationGrid.Builder(
            0, 63, -2, 18, 66, 2);
        for (int x = 0; x <= 18; x++) {
            for (int z = -2; z <= 2; z++) set(builder, x, 64, z);
        }
        ImmutableNavigationGrid grid = builder.build();
        NavigationPlannerWorker worker = new NavigationPlannerWorker(new TestLogger());
        try {
            NavigationPlannerWorker.PlanRequest request =
                new NavigationPlannerWorker.PlanRequest(90L, 1L, false, true,
                    grid, cell(0, 64, 0), cell(18, 64, 0), 24, 4,
                    2048, 1.10F, grid.sampledCells());
            require(worker.submit(request), "bounded worker accepts immutable request");
            NavigationPlannerWorker.PlanEnvelope envelope = null;
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (envelope == null && System.nanoTime() < deadline) {
                Thread.sleep(5L);
                envelope = worker.pollLatest();
            }
            require(envelope != null, "worker returns result without client blocking");
            require(envelope.result().succeeded(), "worker route succeeds");
            require(envelope.request().requestId() == 90L, "worker keeps request identity");
            require(worker.completed() == 1, "worker completion metric increments");
            require(worker.lastComputeNanos() > 0L, "worker reports compute time");
        } finally {
            worker.shutdown();
        }
        require("STOPPED".equals(worker.state()), "worker shuts down explicitly");
    }

    private static void worldGridReusesPersistentCache() {
        World world = flatWorld(-2, 2, -2, 2, 63);
        WorldNavigationGrid grid = new WorldNavigationGrid(world);
        require(grid.isStandable(0, 64, 0), "live grid sees safe cell");
        int reads = grid.worldReads();
        require(grid.isStandable(0, 64, 0), "cached live cell remains safe");
        require(grid.worldReads() == reads, "repeat standability check avoids world reads");
        require(grid.cacheHits() >= 1, "cache hit metric increments");
        world.setBlockStateForTest(new BlockPos(0, 63, 0), Blocks.air.getDefaultState());
        require(!grid.refreshStandable(0, 64, 0), "explicit live refresh sees changed support");
        require(grid.liveRefreshes() == 1, "live refresh metric increments");
    }

    private static MovementPath straightPath(int startX, int endX, long requestId) {
        List<NavigationMovement> movements = new ArrayList<NavigationMovement>();
        NavigationCell previous = cell(startX, 64, 0);
        for (int x = startX + 1; x <= endX; x++) {
            NavigationCell next = cell(x, 64, 0);
            movements.add(new NavigationMovement(previous, next,
                NavigationMovementType.TRAVERSE, 1F, 5, true));
            previous = next;
        }
        return new MovementPath(movements, movements.size(), movements.size(),
            true, requestId);
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

    private static void set(ImmutableNavigationGrid.Builder builder,
                            int x, int y, int z) {
        builder.setStandable(x, y, z, true);
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
