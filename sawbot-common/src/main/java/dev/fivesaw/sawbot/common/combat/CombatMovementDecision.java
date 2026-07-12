package dev.fivesaw.sawbot.common.combat;

/** Immutable local combat-motor output. The tactical brain still selects the target and skill. */
public final class CombatMovementDecision {
    private final float forward;
    private final float strafe;
    private final boolean sprint;
    private final boolean attack;
    private final boolean edgeGuarded;
    private final String mode;

    public CombatMovementDecision(float forward, float strafe, boolean sprint,
                                  boolean attack, boolean edgeGuarded,
                                  String mode) {
        if (!Float.isFinite(forward) || !Float.isFinite(strafe)
            || forward < -1F || forward > 1F || strafe < -1F || strafe > 1F
            || mode == null || mode.isEmpty()) {
            throw new IllegalArgumentException("combat movement decision");
        }
        this.forward = forward;
        this.strafe = strafe;
        this.sprint = sprint;
        this.attack = attack;
        this.edgeGuarded = edgeGuarded;
        this.mode = mode;
    }

    public float forward() { return forward; }
    public float strafe() { return strafe; }
    public boolean sprint() { return sprint; }
    public boolean attack() { return attack; }
    public boolean edgeGuarded() { return edgeGuarded; }
    public String mode() { return mode; }
}
