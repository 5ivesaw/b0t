package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.navigation.ImmutableNavigationGrid;
import dev.fivesaw.sawbot.common.navigation.MovementAStarPlanner;
import dev.fivesaw.sawbot.common.navigation.MovementPlanResult;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

/**
 * Single bounded latest-wins planner worker.
 *
 * It never sees Minecraft objects. Every request owns an immutable world copy,
 * and the client thread only performs non-blocking offer/poll operations.
 */
public final class NavigationPlannerWorker {
    private final ArrayBlockingQueue<PlanRequest> requests =
        new ArrayBlockingQueue<PlanRequest>(1);
    private final ArrayBlockingQueue<PlanEnvelope> results =
        new ArrayBlockingQueue<PlanEnvelope>(2);
    private final MovementAStarPlanner planner = new MovementAStarPlanner();
    private final Logger logger;
    private final Thread worker;
    private volatile boolean running = true;
    private volatile long latestSubmittedId = -1L;
    private volatile String state = "IDLE";
    private volatile int submitted;
    private volatile int completed;
    private volatile int superseded;
    private volatile int droppedResults;
    private volatile long lastComputeNanos;
    private volatile int lastExpandedNodes;
    private volatile int lastKnownNodes;

    public NavigationPlannerWorker(Logger logger) {
        if (logger == null) throw new IllegalArgumentException("logger");
        this.logger = logger;
        this.worker = new Thread(new Runnable() {
            @Override public void run() { runWorker(); }
        }, "SawBotV1-navigation-planner");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public boolean submit(PlanRequest request) {
        if (request == null || !running) return false;
        latestSubmittedId = request.requestId();
        if (!requests.offer(request)) {
            PlanRequest removed = requests.poll();
            if (removed != null) superseded++;
            if (!requests.offer(request)) return false;
        }
        submitted++;
        state = "QUEUED";
        return true;
    }

    public PlanEnvelope pollLatest() {
        PlanEnvelope latest = null;
        PlanEnvelope candidate;
        while ((candidate = results.poll()) != null) latest = candidate;
        return latest;
    }

    private void runWorker() {
        while (running) {
            try {
                PlanRequest request = requests.poll(250L, TimeUnit.MILLISECONDS);
                if (request == null) continue;
                if (request.requestId() != latestSubmittedId) {
                    superseded++;
                    continue;
                }
                state = "SEARCHING";
                MovementPlanResult result = planner.plan(request.grid(),
                    request.start(), request.goal(), request.horizontalRadius(),
                    request.verticalRadius(), request.maximumExpandedNodes(),
                    request.heuristicWeight(), request.requestId(),
                    request.completeGoal());
                lastComputeNanos = result.computeNanos();
                lastExpandedNodes = result.expandedNodes();
                lastKnownNodes = result.knownNodes();
                if (request.requestId() != latestSubmittedId) {
                    superseded++;
                    continue;
                }
                PlanEnvelope envelope = new PlanEnvelope(request, result);
                if (!results.offer(envelope)) {
                    results.poll();
                    droppedResults++;
                    if (!results.offer(envelope)) {
                        droppedResults++;
                    }
                }
                completed++;
                state = result.succeeded() ? "READY" : "FAILED";
            } catch (InterruptedException interrupted) {
                if (!running) break;
            } catch (RuntimeException exception) {
                state = "ERROR";
                logger.error("SawBot navigation planner worker failed a request.", exception);
            }
        }
        state = "STOPPED";
    }

    public void shutdown() {
        running = false;
        requests.clear();
        worker.interrupt();
        try {
            worker.join(750L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public String state() { return state; }
    public int requestQueueSize() { return requests.size(); }
    public int resultQueueSize() { return results.size(); }
    public int submitted() { return submitted; }
    public int completed() { return completed; }
    public int superseded() { return superseded; }
    public int droppedResults() { return droppedResults; }
    public long lastComputeNanos() { return lastComputeNanos; }
    public int lastExpandedNodes() { return lastExpandedNodes; }
    public int lastKnownNodes() { return lastKnownNodes; }

    public static final class PlanRequest {
        private final long requestId;
        private final long waypointRevision;
        private final boolean provisional;
        private final boolean completeGoal;
        private final ImmutableNavigationGrid grid;
        private final NavigationCell start;
        private final NavigationCell goal;
        private final int horizontalRadius;
        private final int verticalRadius;
        private final int maximumExpandedNodes;
        private final float heuristicWeight;
        private final int capturedCells;

        public PlanRequest(long requestId, long waypointRevision,
                           boolean provisional, boolean completeGoal,
                           ImmutableNavigationGrid grid,
                           NavigationCell start, NavigationCell goal,
                           int horizontalRadius, int verticalRadius,
                           int maximumExpandedNodes, float heuristicWeight,
                           int capturedCells) {
            if (requestId < 0L || grid == null || start == null || goal == null) {
                throw new IllegalArgumentException("plan request");
            }
            this.requestId = requestId;
            this.waypointRevision = waypointRevision;
            this.provisional = provisional;
            this.completeGoal = completeGoal;
            this.grid = grid;
            this.start = start;
            this.goal = goal;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            this.maximumExpandedNodes = maximumExpandedNodes;
            this.heuristicWeight = heuristicWeight;
            this.capturedCells = Math.max(0, capturedCells);
        }

        public long requestId() { return requestId; }
        public long waypointRevision() { return waypointRevision; }
        public boolean provisional() { return provisional; }
        public boolean completeGoal() { return completeGoal; }
        public ImmutableNavigationGrid grid() { return grid; }
        public NavigationCell start() { return start; }
        public NavigationCell goal() { return goal; }
        public int horizontalRadius() { return horizontalRadius; }
        public int verticalRadius() { return verticalRadius; }
        public int maximumExpandedNodes() { return maximumExpandedNodes; }
        public float heuristicWeight() { return heuristicWeight; }
        public int capturedCells() { return capturedCells; }
    }

    public static final class PlanEnvelope {
        private final PlanRequest request;
        private final MovementPlanResult result;

        PlanEnvelope(PlanRequest request, MovementPlanResult result) {
            this.request = request;
            this.result = result;
        }

        public PlanRequest request() { return request; }
        public MovementPlanResult result() { return result; }
    }
}
