package dev.fivesaw.sawbot.common.combat;

/**
 * Pure deterministic PvP motor planner.
 *
 * It never selects a target or objective. It converts an already-selected nearby target into
 * continuous legal movement intent while respecting live support probes and attack timing.
 */
public final class CombatMovementPlanner {
    private static final float APPROACH_THRESHOLD = 3.05F;
    private static final float RETREAT_THRESHOLD = 1.55F;
    private static final float ATTACK_RANGE = 3.10F;
    private static final float ATTACK_YAW_TOLERANCE = 6.0F;
    private static final float ATTACK_PITCH_TOLERANCE = 8.0F;

    private final int strafeWindowTicks;
    private final int attackCooldownTicks;

    public CombatMovementPlanner(int strafeWindowTicks, int attackCooldownTicks) {
        if (strafeWindowTicks < 4 || attackCooldownTicks < 1) {
            throw new IllegalArgumentException("combat planner configuration");
        }
        this.strafeWindowTicks = strafeWindowTicks;
        this.attackCooldownTicks = attackCooldownTicks;
    }

    public CombatMovementDecision plan(long clientTick, int targetTrackingId,
                                       float distance, float yawError,
                                       float pitchError, boolean lineOfSight,
                                       boolean attackable, int targetHurtTicks,
                                       int ticksSinceAttack,
                                       boolean forwardSafe, boolean backSafe,
                                       boolean leftSafe, boolean rightSafe) {
        if (!Float.isFinite(distance) || distance < 0F
            || !Float.isFinite(yawError) || !Float.isFinite(pitchError)
            || targetTrackingId <= 0 || targetHurtTicks < 0
            || ticksSinceAttack < 0) {
            throw new IllegalArgumentException("combat planner input");
        }
        if (!lineOfSight) {
            return new CombatMovementDecision(0F, 0F, false, false,
                false, "OCCLUDED");
        }

        int strafeDirection = (((clientTick / strafeWindowTicks)
            + targetTrackingId) & 1L) == 0L ? -1 : 1;
        float forward;
        float strafe;
        boolean sprint;
        String mode;

        if (distance > APPROACH_THRESHOLD) {
            forward = 1F;
            strafe = 0.24F * strafeDirection;
            sprint = distance > 4.1F;
            mode = "APPROACH";
        } else if (distance < RETREAT_THRESHOLD) {
            forward = -0.72F;
            strafe = 0.70F * strafeDirection;
            sprint = false;
            mode = "RETREAT";
        } else {
            forward = 0.12F;
            strafe = 0.88F * strafeDirection;
            sprint = false;
            mode = "SPACING";
        }

        boolean guarded = false;
        if (forward > 0F && !forwardSafe) {
            forward = 0F;
            sprint = false;
            guarded = true;
        } else if (forward < 0F && !backSafe) {
            forward = 0F;
            guarded = true;
        }
        if (strafe < 0F && !leftSafe) {
            strafe = rightSafe ? 0.55F : 0F;
            guarded = true;
        } else if (strafe > 0F && !rightSafe) {
            strafe = leftSafe ? -0.55F : 0F;
            guarded = true;
        }
        if (guarded) mode = "EDGE_GUARD";

        boolean attack = attackable && distance <= ATTACK_RANGE
            && Math.abs(yawError) <= ATTACK_YAW_TOLERANCE
            && Math.abs(pitchError) <= ATTACK_PITCH_TOLERANCE
            && targetHurtTicks <= 2
            && ticksSinceAttack >= attackCooldownTicks;
        return new CombatMovementDecision(forward, strafe, sprint, attack,
            guarded, mode);
    }
}
