package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import dev.fivesaw.sawbot.common.navigation.MovementPath;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationMovement;
import dev.fivesaw.sawbot.common.navigation.NavigationMovementType;
import dev.fivesaw.sawbot.common.navigation.NavigationPath;
import dev.fivesaw.sawbot.common.navigation.NavigationProgressWatchdog;
import dev.fivesaw.sawbot.common.navigation.PathSegmentCoordinator;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.Logger;

/**
 * Phase 9 segmented movement navigator.
 *
 * The learned brain supplies goals. This deterministic body converts those goals
 * into immutable world snapshots, plans movement operations on a bounded worker,
 * keeps current and replacement segments, reconciles against the real player
 * position every tick, and executes controls through a dedicated movement servo.
 */
public final class NavigationBodyController {
    private static final String BODY_VERSION = "navigation-body/1.0";
    private static final int ARRIVAL_STABLE_TICKS = 4;
    private static final int RECONCILE_BACKTRACK_POSITIONS = 5;
    private static final int RECONCILE_FORWARD_POSITIONS = 28;
    private static final int RECOVERY_TICKS = 7;
    private static final int DIRECT_PATH_MAX_CELLS = 8;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final EnvironmentGuard environment;
    private final NavigationWaypointController waypoint;
    private final int horizontalRadius;
    private final int verticalRadius;
    private final int maximumExpandedNodes;
    private final int snapshotCellsPerTick;
    private final int replanIntervalTicks;
    private final int stuckWindowTicks;
    private final float maximumTurnDegreesPerTick;
    private final float arrivalRadius;
    private final int lookaheadNodes;
    private final int pathValidationNodes;
    private final float offRouteDistance;
    private final int localPlanningRadius;
    private final int corridorMargin;
    private final float heuristicWeight;
    private final Logger logger;
    private final NavigationPlannerWorker plannerWorker;
    private final NavigationMovementExecutor movementExecutor;
    private final PathSegmentCoordinator segments = new PathSegmentCoordinator();
    private final NavigationProgressWatchdog progressWatchdog =
        new NavigationProgressWatchdog();

    private WorldNavigationGrid worldGrid;
    private NavigationSnapshotCapture capture;
    private long planGeneration;
    private long plannedWaypointRevision = -1L;
    private long lastAcceptedRequestId = -1L;
    private long lastPlanStartTick;
    private int arrivalTicks;
    private int recoveryTicks;
    private int recoveryDirection = 1;
    private int repeatedBlockages;
    private int replanCount;
    private int hotSwapCount;
    private int routeReanchors;
    private int corridorRecoveries;
    private int routeInvalidations;
    private int offRouteReplans;
    private int localDetours;
    private int stuckRecoveries;
    private int directMicroPlans;
    private int stalePlanResults;
    private int failedPlans;
    private double pathDeviation;
    private float steeringOffsetDegrees;
    private String status = "IDLE";
    private String reason = "startup";
    private String source = "manual";
    private boolean freshPlanRequired = true;
    private boolean brainIntentActive;
    private long brainIntentDeadlineNanos;
    private boolean activePathProvisional;
    private boolean activePathCompletesGoal;
    private ActionCommand previousAppliedAction =
        ActionCommand.zero(0L, 0L, BODY_VERSION);

    /** Compatibility constructor used by existing tests and old config files. */
    public NavigationBodyController(Minecraft minecraft, SawBotStateController state,
                                    EnvironmentGuard environment,
                                    NavigationWaypointController waypoint,
                                    int horizontalRadius, int verticalRadius,
                                    int maximumExpandedNodes, int expansionsPerTick,
                                    int replanIntervalTicks, int stuckWindowTicks,
                                    float maximumTurnDegreesPerTick,
                                    float arrivalRadius, int lookaheadNodes,
                                    float lookaheadDistance,
                                    int pathValidationNodes,
                                    float offRouteDistance,
                                    float reactiveProbeDistance, Logger logger) {
        this(minecraft, state, environment, waypoint, horizontalRadius,
            verticalRadius, maximumExpandedNodes,
            Math.max(320, expansionsPerTick * 5), replanIntervalTicks,
            stuckWindowTicks, maximumTurnDegreesPerTick, arrivalRadius,
            lookaheadNodes, pathValidationNodes, offRouteDistance,
            Math.max(5, Math.min(9, (int)Math.ceil(lookaheadDistance))),
            Math.max(6, Math.min(14, (int)Math.ceil(reactiveProbeDistance * 8F))),
            24, 1.08F, logger);
    }

    public NavigationBodyController(Minecraft minecraft, SawBotStateController state,
                                    EnvironmentGuard environment,
                                    NavigationWaypointController waypoint,
                                    int horizontalRadius, int verticalRadius,
                                    int maximumExpandedNodes,
                                    int snapshotCellsPerTick,
                                    int replanIntervalTicks,
                                    int stuckWindowTicks,
                                    float maximumTurnDegreesPerTick,
                                    float arrivalRadius, int lookaheadNodes,
                                    int pathValidationNodes,
                                    float offRouteDistance,
                                    int localPlanningRadius,
                                    int corridorMargin,
                                    int segmentLength,
                                    float heuristicWeight,
                                    Logger logger) {
        if (minecraft == null || state == null || environment == null
            || waypoint == null || logger == null) {
            throw new IllegalArgumentException("navigation body component");
        }
        if (horizontalRadius < 8 || verticalRadius < 2
            || maximumExpandedNodes < 256 || snapshotCellsPerTick < 64
            || replanIntervalTicks < 2 || stuckWindowTicks < 5
            || maximumTurnDegreesPerTick < 4F || arrivalRadius < 0.25F
            || lookaheadNodes < 1 || pathValidationNodes < 1
            || offRouteDistance < 0.75F || localPlanningRadius < 4
            || corridorMargin < 4 || heuristicWeight < 1F
            || heuristicWeight > 2F) {
            throw new IllegalArgumentException("navigation body configuration");
        }
        this.minecraft = minecraft;
        this.state = state;
        this.environment = environment;
        this.waypoint = waypoint;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.maximumExpandedNodes = maximumExpandedNodes;
        this.snapshotCellsPerTick = snapshotCellsPerTick;
        this.replanIntervalTicks = replanIntervalTicks;
        this.stuckWindowTicks = stuckWindowTicks;
        this.maximumTurnDegreesPerTick = maximumTurnDegreesPerTick;
        this.arrivalRadius = arrivalRadius;
        this.lookaheadNodes = lookaheadNodes;
        this.pathValidationNodes = pathValidationNodes;
        this.offRouteDistance = offRouteDistance;
        this.localPlanningRadius = localPlanningRadius;
        this.corridorMargin = corridorMargin;
        this.heuristicWeight = heuristicWeight;
        this.logger = logger;
        this.plannerWorker = new NavigationPlannerWorker(logger);
        this.movementExecutor = new NavigationMovementExecutor(minecraft);
        this.segments.setSegmentLength(segmentLength);
    }

    /** Records only the high-level navigation intent from the learned brain. */
    public void observeBrainAction(ModelActionEnvelope envelope) {
        if (envelope == null || envelope.command() == null) return;
        ActionCommand command = envelope.command();
        if (command.selectedSkill() == Skill.NAVIGATION
            && command.selectedWaypointId()
                == NavigationWaypointController.USER_WAYPOINT_ID) {
            brainIntentActive = true;
            brainIntentDeadlineNanos = System.nanoTime() + 1_500_000_000L;
            source = "brain";
        }
    }

    public void tick(long clientTick, ObservationSnapshot latest) {
        if (!state.mayApplyAutonomousActions()) {
            movementExecutor.release("disabled/frozen");
            freshPlanRequired = true;
            status = "IDLE";
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }
        if (!environment.isAllowed()) {
            release("environment blocked");
            state.disableAndRelease("environment blocked");
            status = "BLOCKED";
            return;
        }
        if (!waypoint.active()) {
            release("no waypoint");
            clearNavigation("waiting for waypoint");
            status = "WAITING";
            return;
        }
        if (minecraft.currentScreen != null || minecraft.thePlayer == null
            || minecraft.theWorld == null) {
            movementExecutor.release("GUI/world unavailable");
            freshPlanRequired = true;
            status = "PAUSED";
            reason = minecraft.currentScreen != null ? "GUI open"
                : "world unavailable";
            return;
        }

        if (brainIntentActive && System.nanoTime() > brainIntentDeadlineNanos) {
            brainIntentActive = false;
            source = "manual";
        }

        ensureWorldGrid();
        EntityPlayerSP player = minecraft.thePlayer;
        NavigationCell feet = currentStandable(player);
        NavigationCell goal = currentGoal();
        if (feet == null || goal == null) {
            movementExecutor.release("feet/goal not standable");
            status = "BLOCKED";
            reason = feet == null ? "current feet cell unavailable"
                : "goal has no standable neighbour";
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }

        if (arrived(player)) {
            arrivalTicks++;
            movementExecutor.release("waypoint reached");
            status = "ARRIVED";
            reason = "stable within " + one(arrivalRadius) + "m";
            if (arrivalTicks == ARRIVAL_STABLE_TICKS) {
                state.setInspectorNotice("NAV ARRIVED: waypoint #"
                    + NavigationWaypointController.USER_WAYPOINT_ID, 1);
            }
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }
        arrivalTicks = 0;

        boolean waypointChanged = plannedWaypointRevision != waypoint.revision();
        if (waypointChanged || freshPlanRequired) {
            beginPlanning(clientTick, player, feet, goal,
                waypointChanged ? "waypoint changed"
                    : "resume from real position", false);
            freshPlanRequired = false;
        }

        tickCapture();
        acceptPlannerResults(feet);

        if (!segments.hasActivePath()) {
            movementExecutor.release("waiting for movement path");
            status = capture == null ? plannerFailureStatus() : "CAPTURE";
            reason = planningReason();
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }

        boolean reconciled = segments.reconcile(feet,
            RECONCILE_BACKTRACK_POSITIONS, RECONCILE_FORWARD_POSITIONS);
        if (!reconciled) {
            reconciled = segments.reconcileNearby(player.posX, player.posY,
                player.posZ, offRouteDistance, RECONCILE_BACKTRACK_POSITIONS,
                RECONCILE_FORWARD_POSITIONS);
        }
        routeReanchors = segments.reanchors();
        corridorRecoveries = segments.corridorRecoveries();
        if (!reconciled) {
            pathDeviation = nearestPathDistance(player);
            offRouteReplans++;
            beginPlanning(clientTick, player, feet, goal,
                "off active route " + one(pathDeviation) + "m", false);
            movementExecutor.release("off route replanning");
            status = "REPLAN";
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }
        pathDeviation = nearestPathDistance(player);

        if (segments.trySplice(feet)) {
            hotSwapCount++;
            activePathProvisional = segments.activePath() != null
                && !segments.activePath().complete();
            activePathCompletesGoal = segments.activePath() != null
                && segments.activePath().complete();
        }

        if ((clientTick & 1L) == 0L && !validateLiveRoute(clientTick, player,
            feet, goal)) {
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }

        while (segments.advanceIfAt(feet)) {
            if (segments.finished()) break;
        }
        if (segments.finished()) {
            if (arrived(player)) {
                status = "ARRIVED";
                movementExecutor.release("route complete at goal");
            } else {
                beginPlanning(clientTick, player, feet, goal,
                    "segment consumed; continue to final goal", false);
                movementExecutor.release("next segment planning");
                status = "SEGMENT_PLAN";
            }
            previousAppliedAction = bodyAction(latest, false, false, false,
                false, 0F);
            return;
        }

        maybePlanAhead(clientTick, player, feet, goal);
        tickCapture();
        acceptPlannerResults(feet);

        NavigationMovement movement = segments.currentMovement();
        NavigationMovement next = nextMovement();
        if (movement == null) {
            movementExecutor.release("missing current movement");
            status = "REPLAN";
            beginPlanning(clientTick, player, feet, goal,
                "missing movement operation", false);
            return;
        }

        NavigationMovementExecutor.ExecutionFrame frame =
            movementExecutor.execute(player, movement, next, worldGrid,
                maximumTurnDegreesPerTick, recoveryTicks > 0,
                recoveryDirection);
        steeringOffsetDegrees = frame.yawError();
        if (frame.success()) {
            segments.advance();
            repeatedBlockages = 0;
            progressWatchdog.resetWindow(player.posX, player.posZ);
            status = "FOLLOW";
            reason = movement.type() + " complete";
        } else if (frame.blocked()) {
            routeInvalidations++;
            repeatedBlockages++;
            beginPlanning(clientTick, player, feet, goal,
                "live movement blocked: " + frame.reason(), false);
            movementExecutor.release("current movement blocked");
            status = repeatedBlockages >= 2 ? "BLOCKED" : "REPLAN";
            reason = frame.reason();
        } else {
            status = recoveryTicks > 0 ? "RECOVER"
                : (capture != null || plannerWorker.requestQueueSize() > 0
                    || "SEARCHING".equals(plannerWorker.state())
                    ? "FOLLOW+PLAN" : "FOLLOW");
            reason = movement.type() + " " + (segments.movementIndex() + 1)
                + "/" + segments.activePath().movementCount()
                + " seg " + (segments.currentSegmentIndex() + 1)
                + "/" + segments.totalSegments();
        }

        boolean commanded = frame.forward() || frame.left() || frame.right();
        if (progressWatchdog.update(player.posX, player.posZ, commanded,
            stuckWindowTicks, 0.18D)) {
            stuckRecoveries++;
            recoveryDirection = chooseRecoveryDirection(player);
            recoveryTicks = RECOVERY_TICKS;
            beginPlanning(clientTick, player, feet, goal,
                "movement timeout/stuck #" + stuckRecoveries, false);
            state.setInspectorNotice("NAV RECOVER: replanning from current cell", 2);
        }
        if (recoveryTicks > 0) recoveryTicks--;

        previousAppliedAction = bodyAction(latest, frame.forward(),
            frame.left() || frame.right(), frame.jump(), frame.sprint(),
            frame.yawDelta());
    }

    private void ensureWorldGrid() {
        if (worldGrid == null || !worldGrid.matchesWorld(minecraft.theWorld)) {
            worldGrid = new WorldNavigationGrid(minecraft.theWorld);
            capture = null;
            segments.clear();
            freshPlanRequired = true;
        }
    }

    private void beginPlanning(long clientTick, EntityPlayerSP player,
                               NavigationCell start, NavigationCell goal,
                               String cause, boolean keepActivePath) {
        planGeneration++;
        plannedWaypointRevision = waypoint.revision();
        capture = new NavigationSnapshotCapture(worldGrid, planGeneration,
            plannedWaypointRevision, start, goal, horizontalRadius,
            verticalRadius, maximumExpandedNodes, heuristicWeight,
            localPlanningRadius, corridorMargin);
        replanCount++;
        lastPlanStartTick = clientTick;
        reason = cause;
        if (!keepActivePath) {
            segments.clear();
            activePathProvisional = false;
            activePathCompletesGoal = false;
            movementExecutor.release("cold current-position plan");
            MovementPath direct = buildDirectMicroPath(player, start, goal);
            if (direct != null) {
                segments.install(direct, start);
                activePathProvisional = !direct.complete();
                activePathCompletesGoal = direct.complete();
                directMicroPlans++;
                status = "DIRECT+CAPTURE";
            } else {
                status = "CAPTURE";
            }
        } else {
            status = "FOLLOW+CAPTURE";
        }
        progressWatchdog.resetWindow(player.posX, player.posZ);
    }

    private void tickCapture() {
        if (capture == null) return;
        capture.tick(snapshotCellsPerTick);
        NavigationPlannerWorker.PlanRequest local = capture.takeLocalRequest();
        if (local != null && (!segments.hasActivePath()
            || segments.remainingMovements() <= lookaheadNodes)) {
            plannerWorker.submit(local);
        }
        NavigationPlannerWorker.PlanRequest full = capture.takeFullRequest();
        if (full != null) {
            plannerWorker.submit(full);
            capture = null;
        }
    }

    private void acceptPlannerResults(NavigationCell feet) {
        NavigationPlannerWorker.PlanEnvelope envelope = plannerWorker.pollLatest();
        if (envelope == null) return;
        NavigationPlannerWorker.PlanRequest request = envelope.request();
        if (request.waypointRevision() != waypoint.revision()
            || request.requestId() < lastAcceptedRequestId) {
            stalePlanResults++;
            return;
        }
        lastAcceptedRequestId = request.requestId();
        if (!envelope.result().succeeded()) {
            failedPlans++;
            if (!segments.hasActivePath() && !request.provisional()) {
                status = "NO_PATH";
                reason = envelope.result().failureReason();
            }
            return;
        }

        MovementPath path = envelope.result().path();
        if (!segments.hasActivePath()) {
            segments.install(path, feet);
            activePathProvisional = request.provisional() || !path.complete();
            activePathCompletesGoal = path.complete();
            status = request.provisional() ? "LOCAL_PATH" : "FOLLOW";
            reason = "accepted " + path.movementCount() + " operations";
        } else {
            segments.stage(path);
            if (segments.trySplice(feet)) {
                hotSwapCount++;
                activePathProvisional = request.provisional() || !path.complete();
                activePathCompletesGoal = path.complete();
                status = "SPLICE";
                reason = "replacement path spliced at current cell";
            } else {
                status = "FOLLOW+PLAN";
                reason = "replacement staged; waiting for safe overlap";
            }
        }
    }

    private boolean validateLiveRoute(long clientTick, EntityPlayerSP player,
                                      NavigationCell feet, NavigationCell goal) {
        MovementPath path = segments.activePath();
        if (path == null) return false;
        int validationCount = Math.min(pathValidationNodes,
            clientTick % 6L == 0L ? 6 : 2);
        WorldNavigationGrid.MovementValidation validation =
            worldGrid.validateMovementWindow(path, segments.movementIndex(),
                validationCount, true);
        if (validation.valid()) return true;
        routeInvalidations++;
        boolean immediate = validation.invalidMovementIndex()
            <= segments.movementIndex() + 1;
        beginPlanning(clientTick, player, feet, goal,
            validation.reason() + " @" + validation.invalidMovementIndex(),
            !immediate);
        if (immediate) {
            movementExecutor.release("immediate route invalidation");
            status = "REPLAN";
            return false;
        }
        return true;
    }

    private void maybePlanAhead(long clientTick, EntityPlayerSP player,
                                NavigationCell feet, NavigationCell goal) {
        if (capture != null || plannerWorker.requestQueueSize() > 0
            || "SEARCHING".equals(plannerWorker.state())) return;
        if (clientTick - lastPlanStartTick < Math.max(4, replanIntervalTicks)) return;
        boolean needsContinuation = !activePathCompletesGoal
            && segments.remainingMovements() <= Math.max(lookaheadNodes, 8);
        boolean rollingReplacement = segments.remainingMovements() > 0
            && clientTick - lastPlanStartTick >= Math.max(20,
                replanIntervalTicks * 5);
        if (needsContinuation || rollingReplacement) {
            beginPlanning(clientTick, player, feet, goal,
                needsContinuation ? "planning next segment"
                    : "rolling live replacement", true);
        }
    }

    private MovementPath buildDirectMicroPath(EntityPlayerSP player,
                                               NavigationCell start,
                                               NavigationCell goal) {
        double totalDx = goal.centerX() - start.centerX();
        double totalDz = goal.centerZ() - start.centerZ();
        double distance = Math.sqrt(totalDx * totalDx + totalDz * totalDz);
        if (distance < 0.75D) return null;
        double travel = Math.min(distance,
            Math.min(DIRECT_PATH_MAX_CELLS, localPlanningRadius - 1));
        double scale = travel / distance;
        double targetX = start.centerX() + totalDx * scale;
        double targetZ = start.centerZ() + totalDz * scale;
        double targetY = start.centerY()
            + (goal.centerY() - start.centerY()) * scale;
        if (!worldGrid.isCorridorSafe(player.posX, player.posY, player.posZ,
            targetX, targetY, targetZ, false)) return null;

        int steps = Math.max(1, (int)Math.ceil(travel));
        ArrayList<NavigationMovement> movements =
            new ArrayList<NavigationMovement>();
        NavigationCell previous = start;
        float cost = 0F;
        for (int step = 1; step <= steps; step++) {
            double t = (double)step / (double)steps;
            int x = floor(start.centerX() + (targetX - start.centerX()) * t);
            int z = floor(start.centerZ() + (targetZ - start.centerZ()) * t);
            int y = floor(start.centerY() + (targetY - start.centerY()) * t);
            NavigationCell current = worldGrid.nearestStandable(x, y, z, 1, 1);
            if (current == null || current.equals(previous)) continue;
            if (!worldGrid.canTransition(previous, current)) return null;
            NavigationMovementType type = type(previous, current);
            float movementCost = type == NavigationMovementType.DIAGONAL
                ? 1.41421356F : (type == NavigationMovementType.ASCEND
                    ? 1.46F : (type == NavigationMovementType.DESCEND
                        ? 1.16F : 1F));
            movements.add(new NavigationMovement(previous, current, type,
                movementCost, type == NavigationMovementType.ASCEND ? 9 : 6,
                true));
            cost += movementCost;
            previous = current;
        }
        if (movements.isEmpty()) return null;
        boolean complete = previous.horizontalManhattan(goal) <= 1
            && Math.abs(previous.y() - goal.y()) <= 1
            && distance <= travel + 0.5D;
        return new MovementPath(movements, cost, 0, complete,
            planGeneration * 2L - 1L);
    }

    private NavigationCell currentStandable(EntityPlayerSP player) {
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY + 0.01D);
        int z = MathHelper.floor_double(player.posZ);
        return worldGrid.nearestStandable(x, y, z, 1, 2);
    }

    private NavigationCell currentGoal() {
        return worldGrid.nearestStandable(floor(waypoint.x()),
            floor(waypoint.y()), floor(waypoint.z()), 2, 2);
    }

    private NavigationMovement nextMovement() {
        MovementPath path = segments.activePath();
        int index = segments.movementIndex() + 1;
        return path == null || index >= path.movementCount()
            ? null : path.movement(index);
    }

    private int chooseRecoveryDirection(EntityPlayerSP player) {
        float leftYaw = player.rotationYaw - 70F;
        float rightYaw = player.rotationYaw + 70F;
        boolean left = worldGrid.probeDirection(player.posX, player.posY,
            player.posZ, leftYaw, 0.9D, true).safe();
        boolean right = worldGrid.probeDirection(player.posX, player.posY,
            player.posZ, rightYaw, 0.9D, true).safe();
        if (left && !right) return -1;
        if (right && !left) return 1;
        return (stuckRecoveries & 1) == 0 ? -1 : 1;
    }

    private double nearestPathDistance(EntityPlayerSP player) {
        List<NavigationCell> cells = pathCells();
        if (cells.isEmpty()) return Double.POSITIVE_INFINITY;
        int start = Math.max(0, pathIndex() - 5);
        int end = Math.min(cells.size() - 1, pathIndex() + 28);
        double best = Double.POSITIVE_INFINITY;
        for (int index = start; index <= end; index++) {
            NavigationCell cell = cells.get(index);
            double dx = cell.centerX() - player.posX;
            double dz = cell.centerZ() - player.posZ;
            double dy = (cell.centerY() - player.posY) * 0.65D;
            best = Math.min(best, dx * dx + dz * dz + dy * dy);
        }
        return Math.sqrt(best);
    }

    private boolean arrived(EntityPlayerSP player) {
        double dx = waypoint.x() - player.posX;
        double dz = waypoint.z() - player.posZ;
        double dy = Math.abs(waypoint.y() - player.posY);
        return dx * dx + dz * dz <= arrivalRadius * arrivalRadius
            && dy <= 1.25D;
    }

    private String plannerFailureStatus() {
        return "FAILED".equals(plannerWorker.state()) ? "NO_PATH" : "PLAN";
    }

    private String planningReason() {
        if (capture != null) {
            return capture.status() + " " + capture.progressPercent() + "% "
                + capture.capturedCells() + "/" + capture.totalCells();
        }
        return plannerWorker.state() + " q " + plannerWorker.requestQueueSize();
    }

    private void clearNavigation(String why) {
        capture = null;
        segments.clear();
        progressWatchdog.reset();
        movementExecutor.release(why);
        activePathProvisional = false;
        activePathCompletesGoal = false;
        reason = why;
    }

    /** External safety/manual release; the next enable plans from real position. */
    public void release(String why) {
        movementExecutor.release(why);
        capture = null;
        segments.clear();
        progressWatchdog.reset();
        freshPlanRequired = true;
        reason = why == null ? "released" : why;
    }

    public void releaseIfOwned(String why) {
        if (movementExecutor.ownsInputs()) movementExecutor.release(why);
    }

    public void onWorldUnavailable() {
        release("world unavailable");
        if (worldGrid != null) worldGrid.clear();
        worldGrid = null;
        plannedWaypointRevision = -1L;
        planGeneration++;
        status = "IDLE";
    }

    public void shutdown() {
        release("shutdown");
        plannerWorker.shutdown();
    }

    private ActionCommand bodyAction(ObservationSnapshot latest,
                                     boolean forward, boolean strafe,
                                     boolean jump, boolean sprint,
                                     float yawDelta) {
        long sequence = latest == null ? 0L : latest.sequenceNumber();
        float strafeValue = strafe
            ? (recoveryDirection < 0 ? -1F : 1F) : 0F;
        return new ActionCommand(sequence, System.nanoTime(), BODY_VERSION,
            forward ? 1F : 0F, strafeValue, yawDelta, 0F,
            jump ? 1F : 0F, sprint ? 1F : 0F, 0F,
            0F, 0F, 0F, 0F, ActionCommand.KEEP_CURRENT_HOTBAR_SLOT,
            Skill.NAVIGATION, -1,
            NavigationWaypointController.USER_WAYPOINT_ID,
            1F, 1, TacticalObjective.CONTINUE_CURRENT_OBJECTIVE,
            AbortCondition.NONE);
    }

    private static NavigationMovementType type(NavigationCell from,
                                               NavigationCell to) {
        int dy = to.y() - from.y();
        if (dy > 0) return NavigationMovementType.ASCEND;
        if (dy < 0) return NavigationMovementType.DESCEND;
        if (from.x() != to.x() && from.z() != to.z()) {
            return NavigationMovementType.DIAGONAL;
        }
        return NavigationMovementType.TRAVERSE;
    }

    private static int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    private static String one(double value) {
        if (!Double.isFinite(value)) return "inf";
        long scaled = Math.round(Math.abs(value) * 10D);
        return (value < 0D ? "-" : "") + (scaled / 10L)
            + "." + (scaled % 10L);
    }

    public boolean shouldOwnNavigation() { return waypoint.active(); }
    public boolean ownsMovement() { return movementExecutor.ownsInputs(); }
    public String status() { return status; }
    public String reason() { return reason; }
    public String source() { return source; }
    public NavigationPath path() {
        MovementPath movementPath = segments.activePath();
        return movementPath == null ? null
            : new NavigationPath(movementPath.positions(), movementPath.cost(),
                movementPath.expandedNodes());
    }
    public List<NavigationCell> pathCells() {
        MovementPath movementPath = segments.activePath();
        return movementPath == null ? Collections.<NavigationCell>emptyList()
            : movementPath.positions();
    }
    public int pathIndex() { return segments.movementIndex(); }
    public int lookaheadIndex() {
        MovementPath path = segments.activePath();
        if (path == null) return 0;
        return Math.min(path.positionCount() - 1,
            segments.movementIndex() + Math.max(1, lookaheadNodes));
    }
    public boolean provisionalPath() { return activePathProvisional; }
    public double pathDeviation() { return pathDeviation; }
    public float steeringOffsetDegrees() { return steeringOffsetDegrees; }
    public int plannerExpandedNodes() { return plannerWorker.lastExpandedNodes(); }
    public int plannerOpenNodes() { return plannerWorker.requestQueueSize(); }
    public int plannerKnownNodes() { return plannerWorker.lastKnownNodes(); }
    public int gridWorldReads() { return worldGrid == null ? 0 : worldGrid.worldReads(); }
    public int gridCacheHits() { return worldGrid == null ? 0 : worldGrid.cacheHits(); }
    public int gridLiveRefreshes() { return worldGrid == null ? 0 : worldGrid.liveRefreshes(); }
    public int replanCount() { return replanCount; }
    public int hotSwapCount() { return hotSwapCount; }
    public int routeReanchors() { return routeReanchors; }
    public int corridorRecoveries() { return corridorRecoveries; }
    public int routeInvalidations() { return routeInvalidations; }
    public int offRouteReplans() { return offRouteReplans; }
    public int localDetours() { return localDetours; }
    public int stuckRecoveries() { return stuckRecoveries; }
    public int directMicroPlans() { return directMicroPlans; }
    public int stalePlanResults() { return stalePlanResults; }
    public int failedPlans() { return failedPlans; }
    public int captureProgressPercent() { return capture == null ? 100 : capture.progressPercent(); }
    public String plannerState() { return plannerWorker.state(); }
    public long plannerComputeNanos() { return plannerWorker.lastComputeNanos(); }
    public int plannerSubmitted() { return plannerWorker.submitted(); }
    public int plannerCompleted() { return plannerWorker.completed(); }
    public int plannerSuperseded() { return plannerWorker.superseded(); }
    public int remainingMovements() { return segments.remainingMovements(); }
    public int currentSegmentIndex() { return segments.currentSegmentIndex(); }
    public int totalSegments() { return segments.totalSegments(); }
    public boolean nextSegmentAvailable() { return segments.nextSegmentAvailable(); }
    public boolean replacementPending() { return segments.hasStagedPath(); }
    public String currentMovementType() {
        NavigationMovement movement = segments.currentMovement();
        return movement == null ? "NONE" : movement.type().name();
    }
    public ActionCommand previousAppliedAction() { return previousAppliedAction; }
}
