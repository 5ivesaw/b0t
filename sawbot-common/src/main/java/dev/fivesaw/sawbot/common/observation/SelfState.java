package dev.fivesaw.sawbot.common.observation;

public final class SelfState {
    private final float health, absorption, hunger, armour;
    private final double absoluteX, absoluteY, absoluteZ;
    private final float velocityRight, velocityUp, velocityForward;
    private final float accelerationRight, accelerationUp, accelerationForward;
    private final float yawDegrees, pitchDegrees, fallDistance;
    private final boolean onGround, horizontalCollision, verticalCollision, inLiquid, onLadder, insideBlock;
    private final boolean sprinting, sneaking, usingItem;
    private final int airborneTicks, hurtTimerTicks, selectedSlot;
    private final float supportDistanceLeft, supportDistanceCenter, supportDistanceRight, distanceToVoid;
    private final int activePotionCount;

    public SelfState(float health, float absorption, float hunger, float armour,
                     double absoluteX, double absoluteY, double absoluteZ,
                     float velocityRight, float velocityUp, float velocityForward,
                     float accelerationRight, float accelerationUp, float accelerationForward,
                     float yawDegrees, float pitchDegrees, float fallDistance,
                     boolean onGround, boolean horizontalCollision, boolean verticalCollision,
                     boolean inLiquid, boolean onLadder, boolean insideBlock,
                     boolean sprinting, boolean sneaking, boolean usingItem,
                     int airborneTicks, int hurtTimerTicks, int selectedSlot,
                     float supportDistanceLeft, float supportDistanceCenter, float supportDistanceRight,
                     float distanceToVoid, int activePotionCount) {
        this.health = finite(health, "health"); this.absorption = finite(absorption, "absorption");
        this.hunger = finite(hunger, "hunger"); this.armour = finite(armour, "armour");
        this.absoluteX = finite(absoluteX, "absoluteX"); this.absoluteY = finite(absoluteY, "absoluteY"); this.absoluteZ = finite(absoluteZ, "absoluteZ");
        this.velocityRight = finite(velocityRight, "velocityRight"); this.velocityUp = finite(velocityUp, "velocityUp"); this.velocityForward = finite(velocityForward, "velocityForward");
        this.accelerationRight = finite(accelerationRight, "accelerationRight"); this.accelerationUp = finite(accelerationUp, "accelerationUp"); this.accelerationForward = finite(accelerationForward, "accelerationForward");
        this.yawDegrees = finite(yawDegrees, "yawDegrees"); this.pitchDegrees = finite(pitchDegrees, "pitchDegrees"); this.fallDistance = finite(fallDistance, "fallDistance");
        if (airborneTicks < 0 || hurtTimerTicks < 0 || selectedSlot < 0 || selectedSlot > 8 || activePotionCount < 0) throw new IllegalArgumentException("integer self state");
        this.onGround = onGround; this.horizontalCollision = horizontalCollision; this.verticalCollision = verticalCollision;
        this.inLiquid = inLiquid; this.onLadder = onLadder; this.insideBlock = insideBlock;
        this.sprinting = sprinting; this.sneaking = sneaking; this.usingItem = usingItem;
        this.airborneTicks = airborneTicks; this.hurtTimerTicks = hurtTimerTicks; this.selectedSlot = selectedSlot;
        this.supportDistanceLeft = finite(supportDistanceLeft, "supportDistanceLeft"); this.supportDistanceCenter = finite(supportDistanceCenter, "supportDistanceCenter");
        this.supportDistanceRight = finite(supportDistanceRight, "supportDistanceRight"); this.distanceToVoid = finite(distanceToVoid, "distanceToVoid");
        this.activePotionCount = activePotionCount;
    }
    private static float finite(float v, String n) { if (!Float.isFinite(v)) throw new IllegalArgumentException(n); return v; }
    private static double finite(double v, String n) { if (!Double.isFinite(v)) throw new IllegalArgumentException(n); return v; }
    public float health() { return health; } public float absorption() { return absorption; } public float hunger() { return hunger; } public float armour() { return armour; }
    public double absoluteX() { return absoluteX; } public double absoluteY() { return absoluteY; } public double absoluteZ() { return absoluteZ; }
    public float velocityRight() { return velocityRight; } public float velocityUp() { return velocityUp; } public float velocityForward() { return velocityForward; }
    public float accelerationRight() { return accelerationRight; } public float accelerationUp() { return accelerationUp; } public float accelerationForward() { return accelerationForward; }
    public float yawDegrees() { return yawDegrees; } public float pitchDegrees() { return pitchDegrees; } public float fallDistance() { return fallDistance; }
    public boolean onGround() { return onGround; } public boolean horizontalCollision() { return horizontalCollision; } public boolean verticalCollision() { return verticalCollision; }
    public boolean inLiquid() { return inLiquid; } public boolean onLadder() { return onLadder; } public boolean insideBlock() { return insideBlock; }
    public boolean sprinting() { return sprinting; } public boolean sneaking() { return sneaking; } public boolean usingItem() { return usingItem; }
    public int airborneTicks() { return airborneTicks; } public int hurtTimerTicks() { return hurtTimerTicks; } public int selectedSlot() { return selectedSlot; }
    public float supportDistanceLeft() { return supportDistanceLeft; } public float supportDistanceCenter() { return supportDistanceCenter; }
    public float supportDistanceRight() { return supportDistanceRight; } public float distanceToVoid() { return distanceToVoid; }
    public int activePotionCount() { return activePotionCount; }
}
