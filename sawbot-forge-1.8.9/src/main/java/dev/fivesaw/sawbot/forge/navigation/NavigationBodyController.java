package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
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
 * Deterministic navigation body. The brain chooses a waypoint; this controller
 * plans, turns, holds movement, jumps, detects stalls, and releases only inputs
 * it owns.
 */
public final class NavigationBodyController {
    private static final String BODY_VERSION = "navigation-body/0.1";
    private static final double NODE_REACHED_DISTANCE_SQUARED = 0.55D * 0.55D;
    private static final int ARRIVAL_STABLE_TICKS = 5;
    private static final int RECOVERY_TICKS = 8;

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
    private final Logger logger;

    private WorldNavigationGrid grid;
    private NavigationPath path;
    private int pathIndex;
    private long plannedWaypointRevision = -1L;
    private int replanCooldown;
    private int arrivalTicks;
    private int stuckTicks;
    private int recoveryTicks;
    private int recoveryDirection = 1;
    private int replanCount;
    private int stuckRecoveries;
    private double progressX;
    private double progressZ;
    private String status = "IDLE";
    private String reason = "startup";
    private String source = "manual";
    private boolean ownsMovement;
    private boolean brainIntentActive;
    private long brainIntentDeadlineNanos;
    private ActionCommand previousAppliedAction = ActionCommand.zero(0L, 0L, BODY_VERSION);

    public NavigationBodyController(Minecraft minecraft, SawBotStateController state,
                                    EnvironmentGuard environment, NavigationWaypointController waypoint,
                                    int horizontalRadius, int verticalRadius,
                                    int maximumExpandedNodes, int expansionsPerTick,
                                    int replanIntervalTicks, int stuckWindowTicks,
                                    float maximumTurnDegreesPerTick, float arrivalRadius,
                                    Logger logger) {
        if (minecraft == null || state == null || environment == null || waypoint == null || logger == null) {
            throw new IllegalArgumentException("navigation body component");
        }
        if (horizontalRadius < 4 || verticalRadius < 1 || maximumExpandedNodes < 64
            || expansionsPerTick < 8 || replanIntervalTicks < 2 || stuckWindowTicks < 5
            || maximumTurnDegreesPerTick < 1F || arrivalRadius < 0.25F) {
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
            releaseIfOwned("disabled/frozen");
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
            releaseIfOwned("no waypoint");
            resetPlan("waiting for waypoint");
            status = "WAITING";
            return;
        }
        if (minecraft.currentScreen != null || minecraft.thePlayer == null || minecraft.theWorld == null) {
            releaseIfOwned("GUI/world unavailable");
            status = "PAUSED";
            reason = minecraft.currentScreen != null ? "GUI open" : "world unavailable";
            return;
        }

        if (brainIntentActive && System.nanoTime() > brainIntentDeadlineNanos) {
            brainIntentActive = false;
            source = "manual";
        }

        EntityPlayerSP player = minecraft.thePlayer;
        if (arrived(player)) {
            arrivalTicks++;
            releaseIfOwned("waypoint reached");
            status = "ARRIVED";
            reason = "within arrival radius";
            if (arrivalTicks == ARRIVAL_STABLE_TICKS) {
                state.setInspectorNotice("NAV ARRIVED: waypoint #" + NavigationWaypointController.USER_WAYPOINT_ID, 1);
            }
            previousAppliedAction = bodyAction(latest, false, false, false, 0F);
            return;
        }
        arrivalTicks = 0;

        if (plannedWaypointRevision != waypoint.revision()) {
            beginPlan(player, "waypoint changed");
        }
        if (planner.state() == NavigationPlanState.SEARCHING) {
            releaseIfOwned("planning");
            planner.step(expansionsPerTick);
            status = "PLANNING";
            reason = planner.expandedNodes() + "/" + maximumExpandedNodes + " nodes";
            if (planner.state() == NavigationPlanState.SUCCEEDED) acceptPlan(player);
            else if (planner.state() == NavigationPlanState.FAILED) failPlan(planner.failureReason());
            return;
        }

        if (path == null || pathIndex >= path.size()) {
            if (replanCooldown > 0) {
                replanCooldown--;
                releaseIfOwned("replan cooldown");
                status = "NO_PATH";
                return;
            }
            beginPlan(player, "path missing");
            return;
        }

        advancePathIndex(player);
        if (pathIndex >= path.size()) {
            beginPlan(player, "path consumed before arrival");
            return;
        }

        NavigationCell next = path.cell(pathIndex);
        double dx = next.centerX() - player.posX;
        double dz = next.centerZ() - player.posZ;
        float targetYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        float yawError = wrapDegrees(targetYaw - player.rotationYaw);
        float turn = clamp(yawError * 0.48F, -maximumTurnDegreesPerTick, maximumTurnDegreesPerTick);
        player.rotationYaw += turn;

        boolean stepUp = next.y() > MathHelper.floor_double(player.posY + 0.01D);
        boolean recovery = recoveryTicks > 0;
        boolean forward = Math.abs(yawError) <= (recovery ? 85F : 58F);
        boolean jump = (stepUp || player.isCollidedHorizontally || recovery) && player.onGround;
        boolean sprint = forward && !jump && Math.abs(yawError) < 18F && hasStraightRun();
        boolean strafeLeft = recovery && recoveryDirection < 0;
        boolean strafeRight = recovery && recoveryDirection > 0;

        applyMovement(forward, strafeLeft, strafeRight, jump, sprint);
        status = recovery ? "RECOVER" : "FOLLOW";
        reason = "node " + (pathIndex + 1) + "/" + path.size();
        previousAppliedAction = bodyAction(latest, forward, jump, sprint, turn);

        if (recoveryTicks > 0) recoveryTicks--;
        updateProgress(player, forward, clientTick);
    }

    private void beginPlan(EntityPlayerSP player, String cause) {
        releaseIfOwned("planning");
        grid = new WorldNavigationGrid(minecraft.theWorld);
        int startX = MathHelper.floor_double(player.posX);
        int startY = MathHelper.floor_double(player.posY + 0.01D);
        int startZ = MathHelper.floor_double(player.posZ);
        NavigationCell start = grid.nearestStandable(startX, startY, startZ, 1, 2);
        NavigationCell goal = grid.nearestStandable(
            MathHelper.floor_double(waypoint.x()), MathHelper.floor_double(waypoint.y()),
            MathHelper.floor_double(waypoint.z()), 2, 2);
        plannedWaypointRevision = waypoint.revision();
        path = null;
        pathIndex = 0;
        if (start == null || goal == null) {
            failPlan(start == null ? "start not standable" : "goal not standable");
            return;
        }
        planner.begin(grid, start, goal, horizontalRadius, verticalRadius, maximumExpandedNodes);
        status = planner.state() == NavigationPlanState.SEARCHING ? "PLANNING" : "NO_PATH";
        reason = cause;
        replanCount++;
        progressX = player.posX;
        progressZ = player.posZ;
        stuckTicks = 0;
    }

    private void acceptPlan(EntityPlayerSP player) {
        path = planner.path();
        pathIndex = path.size() > 1 ? 1 : 0;
        status = "FOLLOW";
        reason = "path " + path.size() + " nodes";
        progressX = player.posX;
        progressZ = player.posZ;
        stuckTicks = 0;
        replanCooldown = 0;
    }

    private void failPlan(String failure) {
        path = null;
        pathIndex = 0;
        status = "NO_PATH";
        reason = failure;
        replanCooldown = replanIntervalTicks;
        releaseIfOwned("no path");
    }

    private void advancePathIndex(EntityPlayerSP player) {
        while (pathIndex < path.size()) {
            NavigationCell cell = path.cell(pathIndex);
            double dx = cell.centerX() - player.posX;
            double dz = cell.centerZ() - player.posZ;
            double dy = Math.abs(cell.centerY() - player.posY);
            if (dx * dx + dz * dz > NODE_REACHED_DISTANCE_SQUARED || dy > 1.25D) break;
            pathIndex++;
        }
    }

    private boolean arrived(EntityPlayerSP player) {
        double dx = waypoint.x() - player.posX;
        double dz = waypoint.z() - player.posZ;
        double dy = Math.abs(waypoint.y() - player.posY);
        return dx * dx + dz * dz <= arrivalRadius * arrivalRadius && dy <= 1.25D;
    }

    private boolean hasStraightRun() {
        if (path == null || pathIndex + 1 >= path.size()) return false;
        NavigationCell current = path.cell(Math.max(0, pathIndex - 1));
        NavigationCell next = path.cell(pathIndex);
        NavigationCell after = path.cell(pathIndex + 1);
        int dx1 = Integer.signum(next.x() - current.x());
        int dz1 = Integer.signum(next.z() - current.z());
        int dx2 = Integer.signum(after.x() - next.x());
        int dz2 = Integer.signum(after.z() - next.z());
        return dx1 == dx2 && dz1 == dz2 && next.y() == after.y();
    }

    private void updateProgress(EntityPlayerSP player, boolean commandedForward, long clientTick) {
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
            beginPlan(player, "stuck replan #" + stuckRecoveries);
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

    public void release(String why) {
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
        if (ownsMovement) release(why);
    }

    public void onWorldUnavailable() {
        release("world unavailable");
        resetPlan("world unavailable");
        plannedWaypointRevision = -1L;
    }

    private void resetPlan(String why) {
        planner.reset();
        grid = null;
        path = null;
        pathIndex = 0;
        arrivalTicks = 0;
        recoveryTicks = 0;
        reason = why;
    }

    private ActionCommand bodyAction(ObservationSnapshot latest, boolean forward,
                                     boolean jump, boolean sprint, float yawDelta) {
        long sequence = latest == null ? 0L : latest.sequenceNumber();
        return new ActionCommand(sequence, System.nanoTime(), BODY_VERSION,
            forward ? 1F : 0F, 0F, yawDelta, 0F,
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

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
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
    public int plannerExpandedNodes() { return planner.expandedNodes(); }
    public int plannerOpenNodes() { return planner.openNodes(); }
    public int plannerKnownNodes() { return planner.knownNodes(); }
    public int gridWorldReads() { return grid == null ? 0 : grid.worldReads(); }
    public int replanCount() { return replanCount; }
    public int stuckRecoveries() { return stuckRecoveries; }
    public ActionCommand previousAppliedAction() { return previousAppliedAction; }
}
