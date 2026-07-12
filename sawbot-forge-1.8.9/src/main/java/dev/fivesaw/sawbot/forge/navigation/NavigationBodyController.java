package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import dev.fivesaw.sawbot.common.navigation.AdaptivePathCursor;
import dev.fivesaw.sawbot.common.navigation.IncrementalAStarPlanner;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationPath;
import dev.fivesaw.sawbot.common.navigation.NavigationPlanState;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.Logger;

/**
 * Real-time deterministic navigation body.
 *
 * The route is a continuously revised corridor, not a mandatory sequence of
 * block centres. A 20 Hz local controller re-anchors after displacement, skips
 * safe nodes, validates live geometry, probes several immediate headings, and
 * continues following the current route while an anytime replacement A* search
 * runs incrementally.
 */
public final class NavigationBodyController {
    private static final String BODY_VERSION = "navigation-body/0.2";
    private static final double NODE_REACHED_DISTANCE_SQUARED = 0.72D * 0.72D;
    private static final int ARRIVAL_STABLE_TICKS = 4;
    private static final int RECOVERY_TICKS = 7;
    private static final int REANCHOR_BACKTRACK_NODES = 4;
    private static final int REANCHOR_FORWARD_NODES = 16;
    private static final int LIVE_GRID_REFRESH_TICKS = 2;
    private static final float[] STEERING_OFFSETS = {0F, -12F, 12F, -25F, 25F, -40F, 40F};

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final EnvironmentGuard environment;
    private final NavigationWaypointController waypoint;
    private final IncrementalAStarPlanner planner = new IncrementalAStarPlanner();
    private final int horizontalRadius;
    private final int verticalRadius;
    private final int maximumExpandedNodes;
    private final int expansionsPerTick;
    private final int replanIntervalTicks;
    private final int stuckWindowTicks;
    private final float maximumTurnDegreesPerTick;
    private final float arrivalRadius;
    private final int lookaheadNodes;
    private final float lookaheadDistance;
    private final int pathValidationNodes;
    private final float offRouteDistance;
    private final float reactiveProbeDistance;
    private final Logger logger;

    private WorldNavigationGrid planningGrid;
    private WorldNavigationGrid liveGrid;
    private long liveGridTick = Long.MIN_VALUE;
    private NavigationPath path;
    private int pathIndex;
    private int lookaheadIndex;
    private boolean provisionalPath;
    private boolean replacementPlan;
    private NavigationCell lastPlanStart;
    private long plannedWaypointRevision = -1L;
    private int ticksSincePlanStart;
    private int arrivalTicks;
    private int stuckTicks;
    private int recoveryTicks;
    private int recoveryDirection = 1;
    private int replanCount;
    private int hotSwapCount;
    private int routeReanchors;
    private int routeInvalidations;
    private int offRouteReplans;
    private int localDetours;
    private int stuckRecoveries;
    private double progressX;
    private double progressZ;
    private double pathDeviation;
    private float steeringOffsetDegrees;
    private String status = "IDLE";
    private String reason = "startup";
    private String source = "manual";
    private boolean ownsMovement;
    private boolean freshPlanRequired = true;
    private boolean brainIntentActive;
    private long brainIntentDeadlineNanos;
    private ActionCommand previousAppliedAction = ActionCommand.zero(0L, 0L, BODY_VERSION);

    public NavigationBodyController(Minecraft minecraft, SawBotStateController state,
                                    EnvironmentGuard environment, NavigationWaypointController waypoint,
                                    int horizontalRadius, int verticalRadius,
                                    int maximumExpandedNodes, int expansionsPerTick,
                                    int replanIntervalTicks, int stuckWindowTicks,
                                    float maximumTurnDegreesPerTick, float arrivalRadius,
                                    int lookaheadNodes, float lookaheadDistance,
                                    int pathValidationNodes, float offRouteDistance,
                                    float reactiveProbeDistance, Logger logger) {
        if (minecraft == null || state == null || environment == null || waypoint == null || logger == null) {
            throw new IllegalArgumentException("navigation body component");
        }
        if (horizontalRadius < 4 || verticalRadius < 1 || maximumExpandedNodes < 64
            || expansionsPerTick < 8 || replanIntervalTicks < 2 || stuckWindowTicks < 5
            || maximumTurnDegreesPerTick < 1F || arrivalRadius < 0.25F
            || lookaheadNodes < 1 || lookaheadDistance < 1F || pathValidationNodes < 2
            || offRouteDistance < 0.75F || reactiveProbeDistance < 0.5F) {
            throw new IllegalArgumentException("navigation body configuration");
        }
        this.minecraft = minecraft;
        this.state = state;
        this.environment = environment;
        this.waypoint = waypoint;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.maximumExpandedNodes = maximumExpandedNodes;
        this.expansionsPerTick = expansionsPerTick;
        this.replanIntervalTicks = replanIntervalTicks;
        this.stuckWindowTicks = stuckWindowTicks;
        this.maximumTurnDegreesPerTick = maximumTurnDegreesPerTick;
        this.arrivalRadius = arrivalRadius;
        this.lookaheadNodes = lookaheadNodes;
        this.lookaheadDistance = lookaheadDistance;
        this.pathValidationNodes = pathValidationNodes;
        this.offRouteDistance = offRouteDistance;
        this.reactiveProbeDistance = reactiveProbeDistance;
        this.logger = logger;
    }

    /** Records a high-level brain request without accepting its low-level motor values. */
    public void observeBrainAction(ModelActionEnvelope envelope) {
        if (envelope == null || envelope.command() == null) return;
        ActionCommand command = envelope.command();
        if (command.selectedSkill() == Skill.NAVIGATION
            && command.selectedWaypointId() == NavigationWaypointController.USER_WAYPOINT_ID) {
            brainIntentActive = true;
            brainIntentDeadlineNanos = System.nanoTime() + 1_500_000_000L;
            source = "brain";
        }
    }

    public void tick(long clientTick, ObservationSnapshot latest) {
        if (!state.mayApplyAutonomousActions()) {
            releaseOwnedInputs("disabled/frozen");
            freshPlanRequired = true;
            status = "IDLE";
            return;
        }
        if (!environment.isAllowed()) {
            release("environment blocked");
            state.disableAndRelease("environment blocked");
            status = "BLOCKED";
            return;
        }
        if (!waypoint.active()) {
            releaseOwnedInputs("no waypoint");
            resetPlan("waiting for waypoint");
            status = "WAITING";
            return;
        }
        if (minecraft.currentScreen != null || minecraft.thePlayer == null || minecraft.theWorld == null) {
            releaseOwnedInputs("GUI/world unavailable");
            freshPlanRequired = true;
            status = "PAUSED";
            reason = minecraft.currentScreen != null ? "GUI open" : "world unavailable";
            return;
        }

        if (brainIntentActive && System.nanoTime() > brainIntentDeadlineNanos) {
            brainIntentActive = false;
            source = "manual";
        }

        EntityPlayerSP player = minecraft.thePlayer;
        refreshLiveGrid(clientTick);

        if (arrived(player)) {
            arrivalTicks++;
            releaseOwnedInputs("waypoint reached");
            status = "ARRIVED";
            reason = "stable within " + arrivalRadius + "m";
            if (arrivalTicks == ARRIVAL_STABLE_TICKS) {
                state.setInspectorNotice("NAV ARRIVED: waypoint #"
                    + NavigationWaypointController.USER_WAYPOINT_ID, 1);
            }
            previousAppliedAction = bodyAction(latest, false, false, false, false, 0F);
            return;
        }
        arrivalTicks = 0;

        boolean waypointChanged = plannedWaypointRevision != waypoint.revision();
        if (waypointChanged || freshPlanRequired) {
            startPlan(player, waypointChanged ? "waypoint changed" : "resume from current position", false);
            freshPlanRequired = false;
        }

        advancePlanner(player);
        if (path == null) {
            adoptBestEffortPath(player);
            if (path == null) {
                releaseOwnedInputs("planning");
                status = planner.state() == NavigationPlanState.FAILED ? "NO_PATH" : "PLANNING";
                reason = planner.state() == NavigationPlanState.FAILED
                    ? planner.failureReason()
                    : planner.expandedNodes() + "/" + maximumExpandedNodes + " nodes";
                previousAppliedAction = bodyAction(latest, false, false, false, false, 0F);
                return;
            }
        }

        if (!reanchorToCurrentPosition(player)) {
            offRouteReplans++;
            startPlan(player, "off route " + one(pathDeviation) + "m", false);
            advancePlanner(player);
            adoptBestEffortPath(player);
            if (path == null || !reanchorToCurrentPosition(player)) {
                releaseOwnedInputs("off route replanning");
                status = "REPLAN";
                previousAppliedAction = bodyAction(latest, false, false, false, false, 0F);
                return;
            }
        }

        if (pathIndex >= path.size()) {
            startPlan(player, "route consumed before arrival", false);
            releaseOwnedInputs("route consumed");
            status = "REPLAN";
            return;
        }

        validateAndRefreshRoute(player, clientTick);
        advancePlanner(player);
        if (path == null || pathIndex >= path.size()) {
            releaseOwnedInputs("route invalidated");
            status = "REPLAN";
            return;
        }

        maybeStartRollingReplan(player);
        advancePlanner(player);

        lookaheadIndex = selectLookahead(player);
        NavigationCell target = path.cell(lookaheadIndex);
        double dx = target.centerX() - player.posX;
        double dz = target.centerZ() - player.posZ;
        float directYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        SteeringChoice steering = chooseSteering(player, directYaw, target);
        if (steering == null) {
            routeInvalidations++;
            startPlan(player, "no safe local heading", false);
            releaseOwnedInputs("local corridor blocked");
            status = "REPLAN";
            reason = "all immediate probes blocked";
            return;
        }

        steeringOffsetDegrees = wrapDegrees(steering.yawDegrees - directYaw);
        if (Math.abs(steeringOffsetDegrees) > 1F) localDetours++;
        float yawError = wrapDegrees(steering.yawDegrees - player.rotationYaw);
        float turn = boundedHumanTurn(yawError);
        player.rotationYaw += turn;

        int feetY = MathHelper.floor_double(player.posY + 0.01D);
        boolean stepUp = steering.destination != null && steering.destination.y() > feetY;
        boolean recovery = recoveryTicks > 0;
        boolean forward = Math.abs(yawError) <= (recovery ? 88F : 78F);
        boolean jump = (stepUp || player.isCollidedHorizontally || recovery) && player.onGround;
        boolean sprint = forward && !jump && Math.abs(yawError) < 15F
            && hasStraightSafeRun();
        boolean strafeLeft = recovery && recoveryDirection < 0;
        boolean strafeRight = recovery && recoveryDirection > 0;

        applyMovement(forward, strafeLeft, strafeRight, jump, sprint);
        if (recovery) status = "RECOVER";
        else if (planner.state() == NavigationPlanState.SEARCHING) status = "FOLLOW+REPLAN";
        else if (Math.abs(steeringOffsetDegrees) > 1F) status = "DETOUR";
        else status = provisionalPath ? "ANYTIME" : "FOLLOW";
        reason = "node " + (pathIndex + 1) + " look " + (lookaheadIndex + 1)
            + "/" + path.size() + " dev " + one(pathDeviation) + "m";
        previousAppliedAction = bodyAction(latest, forward, strafeLeft || strafeRight,
            jump, sprint, turn);

        if (recoveryTicks > 0) recoveryTicks--;
        updateProgress(player, forward);
        ticksSincePlanStart++;
    }

    private void refreshLiveGrid(long clientTick) {
        if (liveGrid == null || liveGridTick == Long.MIN_VALUE
            || clientTick - liveGridTick >= LIVE_GRID_REFRESH_TICKS) {
            liveGrid = new WorldNavigationGrid(minecraft.theWorld);
            liveGridTick = clientTick;
        }
    }

    private void startPlan(EntityPlayerSP player, String cause, boolean keepExistingRoute) {
        planningGrid = new WorldNavigationGrid(minecraft.theWorld);
        NavigationCell start = currentStandable(planningGrid, player);
        NavigationCell goal = planningGrid.nearestStandable(
            MathHelper.floor_double(waypoint.x()), MathHelper.floor_double(waypoint.y()),
            MathHelper.floor_double(waypoint.z()), 2, 2);
        plannedWaypointRevision = waypoint.revision();
        replacementPlan = keepExistingRoute && path != null;
        lastPlanStart = start;
        ticksSincePlanStart = 0;
        if (!replacementPlan) {
            path = null;
            pathIndex = 0;
            lookaheadIndex = 0;
            provisionalPath = false;
            releaseOwnedInputs("cold planning");
        }
        if (start == null || goal == null) {
            planner.reset();
            failPlan(start == null ? "start not standable" : "goal not standable", replacementPlan);
            return;
        }
        planner.begin(planningGrid, start, goal, horizontalRadius, verticalRadius,
            maximumExpandedNodes);
        status = replacementPlan ? "FOLLOW+REPLAN" : "PLANNING";
        reason = cause;
        replanCount++;
        progressX = player.posX;
        progressZ = player.posZ;
        stuckTicks = 0;
    }

    private NavigationCell currentStandable(WorldNavigationGrid grid, EntityPlayerSP player) {
        int startX = MathHelper.floor_double(player.posX);
        int startY = MathHelper.floor_double(player.posY + 0.01D);
        int startZ = MathHelper.floor_double(player.posZ);
        return grid.nearestStandable(startX, startY, startZ, 1, 2);
    }

    private void advancePlanner(EntityPlayerSP player) {
        if (planner.state() != NavigationPlanState.SEARCHING) return;
        planner.step(expansionsPerTick);
        if (planner.state() == NavigationPlanState.SUCCEEDED) {
            acceptPlan(planner.path(), player, false);
        } else if (planner.state() == NavigationPlanState.FAILED) {
            failPlan(planner.failureReason(), replacementPlan && path != null);
        } else if (path == null) {
            adoptBestEffortPath(player);
        }
    }

    private void adoptBestEffortPath(EntityPlayerSP player) {
        if (planner.state() != NavigationPlanState.SEARCHING) return;
        NavigationPath partial = planner.bestEffortPath();
        if (partial == null || partial.size() < 2) return;
        if (path == null || !provisionalPath
            || partial.size() >= path.size() + 2) {
            acceptPlan(partial, player, true);
        }
    }

    private void acceptPlan(NavigationPath accepted, EntityPlayerSP player, boolean provisional) {
        boolean replacing = path != null && !provisional;
        path = accepted;
        provisionalPath = provisional;
        AdaptivePathCursor.Projection projection = AdaptivePathCursor.project(path, 0,
            player.posX, player.posY, player.posZ, 0, Math.min(12, path.size() - 1));
        pathIndex = Math.min(path.size() - 1,
            projection.index() + (projection.distanceSquared() < NODE_REACHED_DISTANCE_SQUARED ? 1 : 0));
        lookaheadIndex = pathIndex;
        status = provisional ? "ANYTIME" : "FOLLOW";
        reason = (provisional ? "partial " : "path ") + path.size() + " nodes";
        progressX = player.posX;
        progressZ = player.posZ;
        stuckTicks = 0;
        if (replacing) hotSwapCount++;
        if (!provisional) replacementPlan = false;
    }

    private void failPlan(String failure, boolean keepExistingRoute) {
        if (!keepExistingRoute) {
            path = null;
            pathIndex = 0;
            lookaheadIndex = 0;
            releaseOwnedInputs("no path");
            status = "NO_PATH";
        } else {
            status = "FOLLOW";
        }
        reason = failure;
        replacementPlan = false;
    }

    private boolean reanchorToCurrentPosition(EntityPlayerSP player) {
        if (path == null || path.size() == 0) return false;
        pathIndex = Math.max(0, Math.min(pathIndex, path.size() - 1));
        AdaptivePathCursor.Projection projection = AdaptivePathCursor.project(path, pathIndex,
            player.posX, player.posY, player.posZ,
            REANCHOR_BACKTRACK_NODES, REANCHOR_FORWARD_NODES);
        pathDeviation = Math.sqrt(projection.distanceSquared());
        if (pathDeviation > offRouteDistance) return false;
        int oldIndex = pathIndex;
        pathIndex = projection.index();
        NavigationCell anchor = path.cell(pathIndex);
        double dx = anchor.centerX() - player.posX;
        double dz = anchor.centerZ() - player.posZ;
        double dy = Math.abs(anchor.centerY() - player.posY);
        if (dx * dx + dz * dz <= NODE_REACHED_DISTANCE_SQUARED && dy <= 1.35D
            && pathIndex + 1 < path.size()) {
            pathIndex++;
        }
        if (pathIndex != oldIndex) routeReanchors++;
        return true;
    }

    private void validateAndRefreshRoute(EntityPlayerSP player, long clientTick) {
        if (path == null || liveGrid == null) return;
        int nodes = clientTick % 5L == 0L ? pathValidationNodes : Math.min(4, pathValidationNodes);
        WorldNavigationGrid.PathValidation validation = liveGrid.validatePathWindow(
            path, pathIndex, nodes, true);
        if (validation.valid()) return;
        routeInvalidations++;
        boolean immediate = validation.invalidIndex() <= pathIndex + 2;
        startPlan(player, validation.reason() + " @" + validation.invalidIndex(), !immediate);
        if (immediate) releaseOwnedInputs("immediate route invalidation");
    }

    private void maybeStartRollingReplan(EntityPlayerSP player) {
        if (planner.state() == NavigationPlanState.SEARCHING || path == null
            || ticksSincePlanStart < replanIntervalTicks) return;
        NavigationCell current = currentStandable(liveGrid, player);
        if (current == null) return;
        boolean movedFromPlanOrigin = lastPlanStart == null
            || current.horizontalManhattan(lastPlanStart) >= 2
            || Math.abs(current.y() - lastPlanStart.y()) >= 1;
        if (movedFromPlanOrigin || provisionalPath) {
            startPlan(player, "rolling current-position replan", true);
        }
    }

    private int selectLookahead(final EntityPlayerSP player) {
        int best = pathIndex;
        int last = Math.min(path.size() - 1, pathIndex + lookaheadNodes);
        double maximumDistanceSquared = lookaheadDistance * lookaheadDistance;
        for (int index = pathIndex + 1; index <= last; index++) {
            NavigationCell candidate = path.cell(index);
            double dx = candidate.centerX() - player.posX;
            double dz = candidate.centerZ() - player.posZ;
            if (dx * dx + dz * dz > maximumDistanceSquared) break;
            if (!liveGrid.isCorridorSafe(player.posX, player.posY, player.posZ,
                candidate.centerX(), candidate.centerY(), candidate.centerZ(), false)) break;
            best = index;
        }
        return best;
    }

    private SteeringChoice chooseSteering(EntityPlayerSP player, float directYaw,
                                           NavigationCell target) {
        SteeringChoice best = null;
        double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double probeDistance = reactiveProbeDistance + Math.min(0.65D, speed * 2.5D);
        int corridorEnd = Math.min(path.size() - 1, lookaheadIndex + 2);
        for (float offset : STEERING_OFFSETS) {
            float candidateYaw = directYaw + offset;
            WorldNavigationGrid.MotionProbe probe = liveGrid.probeDirection(
                player.posX, player.posY, player.posZ, candidateYaw, probeDistance, false);
            if (!probe.safe()) continue;
            double corridorDistance = AdaptivePathCursor.nearestHorizontalDistanceSquared(
                path, Math.max(0, pathIndex - 1), corridorEnd,
                probe.targetX(), probe.targetZ());
            double tx = target.centerX() - probe.targetX();
            double tz = target.centerZ() - probe.targetZ();
            double remaining = Math.sqrt(tx * tx + tz * tz);
            float score = (float)(corridorDistance * 2.8D + remaining * 0.12D)
                + Math.abs(offset) * 0.024F
                + probe.riskPenalty() * 1.35F
                + Math.abs(wrapDegrees(candidateYaw - player.rotationYaw)) * 0.0025F;
            if (best == null || score < best.score - 0.0001F) {
                best = new SteeringChoice(candidateYaw, probe.destination(), score);
            }
        }
        return best;
    }

    private float boundedHumanTurn(float yawError) {
        float magnitude = Math.abs(yawError);
        if (magnitude < 0.35F) return yawError;
        float desired = magnitude * 0.68F + 1.2F;
        float bounded = Math.min(maximumTurnDegreesPerTick, Math.max(1.5F, desired));
        return yawError < 0F ? -bounded : bounded;
    }

    private boolean arrived(EntityPlayerSP player) {
        double dx = waypoint.x() - player.posX;
        double dz = waypoint.z() - player.posZ;
        double dy = Math.abs(waypoint.y() - player.posY);
        return dx * dx + dz * dz <= arrivalRadius * arrivalRadius && dy <= 1.25D;
    }

    private boolean hasStraightSafeRun() {
        if (path == null || lookaheadIndex <= pathIndex || lookaheadIndex >= path.size()) return false;
        NavigationCell current = path.cell(Math.max(0, pathIndex - 1));
        NavigationCell next = path.cell(pathIndex);
        int dx = Integer.signum(next.x() - current.x());
        int dz = Integer.signum(next.z() - current.z());
        for (int index = pathIndex + 1; index <= lookaheadIndex; index++) {
            NavigationCell after = path.cell(index);
            NavigationCell before = path.cell(index - 1);
            if (Integer.signum(after.x() - before.x()) != dx
                || Integer.signum(after.z() - before.z()) != dz
                || after.y() != before.y()) return false;
        }
        return true;
    }

    private void updateProgress(EntityPlayerSP player, boolean commandedForward) {
        if (!commandedForward) {
            stuckTicks = 0;
            progressX = player.posX;
            progressZ = player.posZ;
            return;
        }
        stuckTicks++;
        if (stuckTicks < stuckWindowTicks) return;
        double dx = player.posX - progressX;
        double dz = player.posZ - progressZ;
        if (dx * dx + dz * dz < 0.12D * 0.12D) {
            stuckRecoveries++;
            recoveryDirection = (stuckRecoveries & 1) == 0 ? -1 : 1;
            recoveryTicks = RECOVERY_TICKS;
            startPlan(player, "stuck live replan #" + stuckRecoveries, false);
            state.setInspectorNotice("NAV RECOVER: stuck, replanning", 2);
        }
        progressX = player.posX;
        progressZ = player.posZ;
        stuckTicks = 0;
    }

    private void applyMovement(boolean forward, boolean left, boolean right,
                               boolean jump, boolean sprint) {
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return;
        ownsMovement = forward || left || right || jump || sprint;
        set(settings.keyBindForward, forward);
        set(settings.keyBindBack, false);
        set(settings.keyBindLeft, left);
        set(settings.keyBindRight, right);
        set(settings.keyBindJump, jump);
        set(settings.keyBindSprint, sprint);
        set(settings.keyBindSneak, false);
    }

    /** External safety/manual release. The next enable always plans from the real current position. */
    public void release(String why) {
        releaseOwnedInputs(why);
        freshPlanRequired = true;
    }

    private void releaseOwnedInputs(String why) {
        if (ownsMovement && minecraft.gameSettings != null) {
            GameSettings settings = minecraft.gameSettings;
            InputRelease.restorePhysical(settings.keyBindForward);
            InputRelease.restorePhysical(settings.keyBindBack);
            InputRelease.restorePhysical(settings.keyBindLeft);
            InputRelease.restorePhysical(settings.keyBindRight);
            InputRelease.restorePhysical(settings.keyBindJump);
            InputRelease.restorePhysical(settings.keyBindSprint);
            InputRelease.restorePhysical(settings.keyBindSneak);
        }
        ownsMovement = false;
        reason = why == null ? "released" : why;
    }

    public void releaseIfOwned(String why) {
        if (ownsMovement) releaseOwnedInputs(why);
    }

    public void onWorldUnavailable() {
        release("world unavailable");
        resetPlan("world unavailable");
        plannedWaypointRevision = -1L;
    }

    private void resetPlan(String why) {
        planner.reset();
        planningGrid = null;
        liveGrid = null;
        liveGridTick = Long.MIN_VALUE;
        path = null;
        pathIndex = 0;
        lookaheadIndex = 0;
        provisionalPath = false;
        replacementPlan = false;
        arrivalTicks = 0;
        recoveryTicks = 0;
        reason = why;
    }

    private ActionCommand bodyAction(ObservationSnapshot latest, boolean forward,
                                     boolean strafe, boolean jump, boolean sprint,
                                     float yawDelta) {
        long sequence = latest == null ? 0L : latest.sequenceNumber();
        float strafeValue = strafe ? (recoveryDirection < 0 ? -1F : 1F) : 0F;
        return new ActionCommand(sequence, System.nanoTime(), BODY_VERSION,
            forward ? 1F : 0F, strafeValue, yawDelta, 0F,
            jump ? 1F : 0F, sprint ? 1F : 0F, 0F,
            0F, 0F, 0F, 0F, ActionCommand.KEEP_CURRENT_HOTBAR_SLOT,
            Skill.NAVIGATION, -1, NavigationWaypointController.USER_WAYPOINT_ID,
            1F, 1, TacticalObjective.CONTINUE_CURRENT_OBJECTIVE, AbortCondition.NONE);
    }

    private static void set(KeyBinding binding, boolean down) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360F;
        if (wrapped >= 180F) wrapped -= 360F;
        if (wrapped < -180F) wrapped += 360F;
        return wrapped;
    }

    private static String one(double value) {
        long scaled = Math.round(Math.abs(value) * 10D);
        return (value < 0D ? "-" : "") + (scaled / 10L) + "." + (scaled % 10L);
    }

    public boolean shouldOwnNavigation() { return waypoint.active(); }
    public boolean ownsMovement() { return ownsMovement; }
    public String status() { return status; }
    public String reason() { return reason; }
    public String source() { return source; }
    public NavigationPath path() { return path; }
    public List<NavigationCell> pathCells() {
        return path == null ? Collections.<NavigationCell>emptyList() : path.cells();
    }
    public int pathIndex() { return pathIndex; }
    public int lookaheadIndex() { return lookaheadIndex; }
    public boolean provisionalPath() { return provisionalPath; }
    public double pathDeviation() { return pathDeviation; }
    public float steeringOffsetDegrees() { return steeringOffsetDegrees; }
    public int plannerExpandedNodes() { return planner.expandedNodes(); }
    public int plannerOpenNodes() { return planner.openNodes(); }
    public int plannerKnownNodes() { return planner.knownNodes(); }
    public int gridWorldReads() {
        int planningReads = planningGrid == null ? 0 : planningGrid.worldReads();
        int liveReads = liveGrid == null ? 0 : liveGrid.worldReads();
        return planningReads + liveReads;
    }
    public int gridLiveRefreshes() { return liveGrid == null ? 0 : liveGrid.liveRefreshes(); }
    public int replanCount() { return replanCount; }
    public int hotSwapCount() { return hotSwapCount; }
    public int routeReanchors() { return routeReanchors; }
    public int routeInvalidations() { return routeInvalidations; }
    public int offRouteReplans() { return offRouteReplans; }
    public int localDetours() { return localDetours; }
    public int stuckRecoveries() { return stuckRecoveries; }
    public ActionCommand previousAppliedAction() { return previousAppliedAction; }

    private static final class SteeringChoice {
        final float yawDegrees;
        final NavigationCell destination;
        final float score;

        SteeringChoice(float yawDegrees, NavigationCell destination, float score) {
            this.yawDegrees = yawDegrees;
            this.destination = destination;
            this.score = score;
        }
    }
}
