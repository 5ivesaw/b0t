package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationMovement;
import dev.fivesaw.sawbot.common.navigation.NavigationMovementType;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;

/**
 * Low-level movement and camera body specialist.
 *
 * It owns only the keys needed by the current movement operation. It never
 * chooses a route or goal and always restores real physical key state on release.
 * Straight and gentle-turn operations use a validated look-ahead point so the
 * player flows through a route corridor without pulsing movement at every cell.
 * Collision alone never triggers a blind jump; only a planned legal one-block
 * ascent or bounded recovery may press jump.
 */
public final class NavigationMovementExecutor {
    private static final double DESTINATION_RADIUS_SQUARED = 0.48D * 0.48D;
    private static final int MINIMUM_OPERATION_TIMEOUT_TICKS = 36;

    private final Minecraft minecraft;
    private final NavigationCameraController camera = new NavigationCameraController();
    private NavigationMovement activeMovement;
    private int ticksOnMovement;
    private boolean ownsInputs;
    private boolean forward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean sprint;
    private float lastYawError;
    private float lastYawDelta;
    private String state = "IDLE";

    public NavigationMovementExecutor(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    public ExecutionFrame execute(EntityPlayerSP player,
                                  NavigationMovement movement,
                                  NavigationMovement nextMovement,
                                  WorldNavigationGrid grid,
                                  float maximumTurnDegreesPerTick,
                                  boolean recovery,
                                  int recoveryDirection) {
        if (player == null || movement == null || grid == null) {
            release("missing movement component");
            return ExecutionFrame.blocked("missing movement component");
        }
        if (activeMovement != movement) {
            activeMovement = movement;
            ticksOnMovement = 0;
            camera.reset();
        }
        ticksOnMovement++;
        int timeout = Math.max(MINIMUM_OPERATION_TIMEOUT_TICKS,
            movement.estimatedTicks() + 28);
        if (ticksOnMovement > timeout) {
            apply(false, false, false, false, false);
            state = "TIMEOUT";
            return ExecutionFrame.blocked("movement operation timeout");
        }

        NavigationCell target = movement.to();
        double targetDx = target.centerX() - player.posX;
        double targetDz = target.centerZ() - player.posZ;
        double horizontalDistanceSquared = targetDx * targetDx + targetDz * targetDz;
        double verticalDistance = Math.abs(target.centerY() - player.posY);
        NavigationCell feet = feetCell(player);
        boolean finalOperation = nextMovement == null;
        if (target.equals(feet)
            || (finalOperation
                && horizontalDistanceSquared <= DESTINATION_RADIUS_SQUARED
                && verticalDistance <= 0.85D)) {
            apply(false, false, false, false, false);
            state = "SUCCESS";
            activeMovement = null;
            ticksOnMovement = 0;
            return new ExecutionFrame(true, false, false, false, false,
                false, false, 0F, 0F, "destination reached");
        }

        AimPoint aim = selectAimPoint(player, movement, nextMovement, grid);
        double aimDx = aim.x - player.posX;
        double aimDz = aim.z - player.posZ;
        float desiredYaw = (float)Math.toDegrees(Math.atan2(-aimDx, aimDz));
        lastYawError = NavigationCameraController.wrapDegrees(desiredYaw - player.rotationYaw);
        lastYawDelta = camera.step(player.rotationYaw, desiredYaw,
            maximumTurnDegreesPerTick);
        player.rotationYaw += lastYawDelta;
        float remainingYawError = NavigationCameraController.wrapDegrees(
            desiredYaw - player.rotationYaw);

        double probeDistance = Math.min(1.35D,
            Math.max(0.55D, Math.sqrt(horizontalDistanceSquared)));
        if (movement.type() == NavigationMovementType.ASCEND) {
            if (movement.dy() != 1 || (movement.dx() != 0 && movement.dz() != 0)
                || !grid.refreshStandable(target.x(), target.y(), target.z())
                || !grid.canTransition(movement.from(), target)) {
                apply(false, false, false, false, false);
                state = "ASCEND_BLOCKED";
                return ExecutionFrame.blocked(
                    "ascend is not a legal one-block transition");
            }
        } else {
            WorldNavigationGrid.MotionProbe probe = grid.probeDirection(
                player.posX, player.posY, player.posZ, desiredYaw,
                probeDistance, false);
            if (!probe.safe()) {
                apply(false, false, false, false, false);
                state = "LOCAL_BLOCKED";
                return ExecutionFrame.blocked("immediate corridor blocked");
            }
        }

        boolean aligned = Math.abs(remainingYawError) <= (recovery ? 82F : 58F);
        boolean useForward = aligned;
        boolean useLeft = recovery && recoveryDirection < 0;
        boolean useRight = recovery && recoveryDirection > 0;
        boolean useJump = player.onGround
            && (movement.requiresJump() || recovery);
        boolean straightContinuation = sameHeading(movement, nextMovement);
        float risk = grid.traversalPenalty(target.x(), target.y(), target.z());
        boolean useSprint = useForward && !useJump && movement.allowsSprint()
            && straightContinuation && Math.abs(remainingYawError) < 10F
            && risk <= 0.28F && horizontalDistanceSquared > 1.35D * 1.35D;

        apply(useForward, useLeft, useRight, useJump, useSprint);
        state = recovery ? "RECOVERY"
            : (aim.lookahead ? movement.type().name() + "+LOOKAHEAD"
                : movement.type().name());
        return new ExecutionFrame(false, false, useForward, useLeft, useRight,
            useJump, useSprint, lastYawDelta, remainingYawError, state);
    }

    private AimPoint selectAimPoint(EntityPlayerSP player,
                                    NavigationMovement movement,
                                    NavigationMovement next,
                                    WorldNavigationGrid grid) {
        NavigationCell currentTarget = movement.to();
        if (next == null || !currentTarget.equals(next.from())
            || movement.dy() != 0 || next.dy() != 0
            || headingDifference(movement, next) > 46F) {
            return AimPoint.current(currentTarget);
        }
        NavigationCell future = next.to();
        if (!grid.isCorridorSafe(player.posX, player.posY, player.posZ,
            future.centerX(), future.centerY(), future.centerZ(), false)) {
            return AimPoint.current(currentTarget);
        }
        return new AimPoint(future.centerX(), future.centerY(), future.centerZ(), true);
    }

    private static float headingDifference(NavigationMovement current,
                                           NavigationMovement next) {
        float currentYaw = (float)Math.toDegrees(Math.atan2(-current.dx(), current.dz()));
        float nextYaw = (float)Math.toDegrees(Math.atan2(-next.dx(), next.dz()));
        return Math.abs(NavigationCameraController.wrapDegrees(nextYaw - currentYaw));
    }

    private static boolean sameHeading(NavigationMovement movement,
                                       NavigationMovement next) {
        if (next == null || next.type() == NavigationMovementType.ASCEND
            || next.type() == NavigationMovementType.DESCEND) return false;
        return Integer.signum(movement.dx()) == Integer.signum(next.dx())
            && Integer.signum(movement.dz()) == Integer.signum(next.dz())
            && movement.dy() == 0 && next.dy() == 0;
    }

    private void apply(boolean forward, boolean left, boolean right,
                       boolean jump, boolean sprint) {
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return;
        this.forward = forward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sprint = sprint;
        ownsInputs = forward || left || right || jump || sprint;
        set(settings.keyBindForward, forward);
        set(settings.keyBindBack, false);
        set(settings.keyBindLeft, left);
        set(settings.keyBindRight, right);
        set(settings.keyBindJump, jump);
        set(settings.keyBindSprint, sprint);
        set(settings.keyBindSneak, false);
    }

    public void release(String why) {
        if (minecraft.gameSettings != null && ownsInputs) {
            GameSettings settings = minecraft.gameSettings;
            InputRelease.restorePhysical(settings.keyBindForward);
            InputRelease.restorePhysical(settings.keyBindBack);
            InputRelease.restorePhysical(settings.keyBindLeft);
            InputRelease.restorePhysical(settings.keyBindRight);
            InputRelease.restorePhysical(settings.keyBindJump);
            InputRelease.restorePhysical(settings.keyBindSprint);
            InputRelease.restorePhysical(settings.keyBindSneak);
        }
        ownsInputs = false;
        forward = false;
        left = false;
        right = false;
        jump = false;
        sprint = false;
        activeMovement = null;
        ticksOnMovement = 0;
        state = why == null ? "RELEASED" : why;
        camera.reset();
    }

    private static NavigationCell feetCell(EntityPlayerSP player) {
        return new NavigationCell(MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posY + 0.01D),
            MathHelper.floor_double(player.posZ));
    }

    private static void set(KeyBinding binding, boolean down) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    public boolean ownsInputs() { return ownsInputs; }
    public boolean forward() { return forward; }
    public boolean left() { return left; }
    public boolean right() { return right; }
    public boolean jump() { return jump; }
    public boolean sprint() { return sprint; }
    public float lastYawError() { return lastYawError; }
    public float lastYawDelta() { return lastYawDelta; }
    public int ticksOnMovement() { return ticksOnMovement; }
    public String state() { return state; }

    private static final class AimPoint {
        final double x;
        final double y;
        final double z;
        final boolean lookahead;

        AimPoint(double x, double y, double z, boolean lookahead) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lookahead = lookahead;
        }

        static AimPoint current(NavigationCell cell) {
            return new AimPoint(cell.centerX(), cell.centerY(), cell.centerZ(), false);
        }
    }

    public static final class ExecutionFrame {
        private final boolean success;
        private final boolean blocked;
        private final boolean forward;
        private final boolean left;
        private final boolean right;
        private final boolean jump;
        private final boolean sprint;
        private final float yawDelta;
        private final float yawError;
        private final String reason;

        ExecutionFrame(boolean success, boolean blocked, boolean forward,
                       boolean left, boolean right, boolean jump,
                       boolean sprint, float yawDelta, float yawError,
                       String reason) {
            this.success = success;
            this.blocked = blocked;
            this.forward = forward;
            this.left = left;
            this.right = right;
            this.jump = jump;
            this.sprint = sprint;
            this.yawDelta = yawDelta;
            this.yawError = yawError;
            this.reason = reason;
        }

        static ExecutionFrame blocked(String reason) {
            return new ExecutionFrame(false, true, false, false, false,
                false, false, 0F, 0F, reason);
        }

        public boolean success() { return success; }
        public boolean blocked() { return blocked; }
        public boolean forward() { return forward; }
        public boolean left() { return left; }
        public boolean right() { return right; }
        public boolean jump() { return jump; }
        public boolean sprint() { return sprint; }
        public float yawDelta() { return yawDelta; }
        public float yawError() { return yawError; }
        public String reason() { return reason; }
    }
}
