package dev.fivesaw.sawbot.common.observation;

public final class EntityObservation {
    private final int trackingId;
    private final int minecraftEntityId;
    private final EntityKind kind;
    private final TeamRelation teamRelation;
    private final float right;
    private final float up;
    private final float forward;
    private final float velocityRight;
    private final float velocityUp;
    private final float velocityForward;
    private final float accelerationRight;
    private final float accelerationUp;
    private final float accelerationForward;
    private final float yawDegrees;
    private final float pitchDegrees;
    private final float width;
    private final float height;
    private final float health;
    private final float armour;
    private final float distance;
    private final float trackingConfidence;
    private final int heldItemCategory;
    private final int hurtTimerTicks;
    private final boolean onGround;
    private final boolean sprinting;
    private final boolean sneaking;
    private final boolean lineOfSight;
    private final boolean occluded;
    private final boolean attackable;
    private final boolean loaded;

    public EntityObservation(int trackingId, int minecraftEntityId, EntityKind kind, TeamRelation teamRelation,
                             float right, float up, float forward,
                             float velocityRight, float velocityUp, float velocityForward,
                             float accelerationRight, float accelerationUp, float accelerationForward,
                             float yawDegrees, float pitchDegrees,
                             float width, float height, float health, float armour, float distance,
                             int heldItemCategory, int hurtTimerTicks,
                             boolean onGround, boolean sprinting, boolean sneaking,
                             boolean lineOfSight, boolean occluded, boolean attackable, boolean loaded,
                             float trackingConfidence) {
        if (trackingId <= 0 || kind == null || teamRelation == null || heldItemCategory < 0 || hurtTimerTicks < 0) {
            throw new IllegalArgumentException("entity identity");
        }
        this.trackingId = trackingId;
        this.minecraftEntityId = minecraftEntityId;
        this.kind = kind;
        this.teamRelation = teamRelation;
        this.right = finite(right);
        this.up = finite(up);
        this.forward = finite(forward);
        this.velocityRight = finite(velocityRight);
        this.velocityUp = finite(velocityUp);
        this.velocityForward = finite(velocityForward);
        this.accelerationRight = finite(accelerationRight);
        this.accelerationUp = finite(accelerationUp);
        this.accelerationForward = finite(accelerationForward);
        this.yawDegrees = finite(yawDegrees);
        this.pitchDegrees = finite(pitchDegrees);
        this.width = finite(width);
        this.height = finite(height);
        this.health = finite(health);
        this.armour = finite(armour);
        this.distance = finite(distance);
        this.heldItemCategory = heldItemCategory;
        this.hurtTimerTicks = hurtTimerTicks;
        this.onGround = onGround;
        this.sprinting = sprinting;
        this.sneaking = sneaking;
        this.lineOfSight = lineOfSight;
        this.occluded = occluded;
        this.attackable = attackable;
        this.loaded = loaded;
        this.trackingConfidence = finite(trackingConfidence);
        if (trackingConfidence < 0f || trackingConfidence > 1f) throw new IllegalArgumentException("trackingConfidence");
    }

    private static float finite(float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("nonfinite entity value");
        return value;
    }

    public int trackingId() { return trackingId; }
    public int minecraftEntityId() { return minecraftEntityId; }
    public EntityKind kind() { return kind; }
    public TeamRelation teamRelation() { return teamRelation; }
    public float right() { return right; }
    public float up() { return up; }
    public float forward() { return forward; }
    public float velocityRight() { return velocityRight; }
    public float velocityUp() { return velocityUp; }
    public float velocityForward() { return velocityForward; }
    public float accelerationRight() { return accelerationRight; }
    public float accelerationUp() { return accelerationUp; }
    public float accelerationForward() { return accelerationForward; }
    public float yawDegrees() { return yawDegrees; }
    public float pitchDegrees() { return pitchDegrees; }
    public float width() { return width; }
    public float height() { return height; }
    public float health() { return health; }
    public float armour() { return armour; }
    public float distance() { return distance; }
    public int heldItemCategory() { return heldItemCategory; }
    public int hurtTimerTicks() { return hurtTimerTicks; }
    public boolean onGround() { return onGround; }
    public boolean sprinting() { return sprinting; }
    public boolean sneaking() { return sneaking; }
    public boolean lineOfSight() { return lineOfSight; }
    public boolean occluded() { return occluded; }
    public boolean attackable() { return attackable; }
    public boolean loaded() { return loaded; }
    public float trackingConfidence() { return trackingConfidence; }
}
