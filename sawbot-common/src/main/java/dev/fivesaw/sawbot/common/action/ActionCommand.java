package dev.fivesaw.sawbot.common.action;

import dev.fivesaw.sawbot.common.versioning.SchemaVersion;

public final class ActionCommand {
    public static final int KEEP_CURRENT_HOTBAR_SLOT = -1;

    private final SchemaVersion schemaVersion;
    private final long observationSequenceNumber;
    private final long generatedTimestampNanos;
    private final String modelVersion;
    private final float forward;
    private final float strafe;
    private final float yawDeltaDegrees;
    private final float pitchDeltaDegrees;
    private final float jumpProbability;
    private final float sprintProbability;
    private final float sneakProbability;
    private final float attackProbability;
    private final float useOrPlaceProbability;
    private final float dropProbability;
    private final float inventoryToggleProbability;
    private final int hotbarSlot;
    private final Skill selectedSkill;
    private final int selectedTargetTrackingId;
    private final int selectedWaypointId;
    private final float confidence;
    private final int actionDurationTicks;
    private final TacticalObjective tacticalObjective;
    private final AbortCondition abortCondition;

    public ActionCommand(long observationSequenceNumber, long generatedTimestampNanos, String modelVersion,
                         float forward, float strafe, float yawDeltaDegrees, float pitchDeltaDegrees,
                         float jumpProbability, float sprintProbability, float sneakProbability,
                         float attackProbability, float useOrPlaceProbability, float dropProbability,
                         float inventoryToggleProbability, int hotbarSlot, Skill selectedSkill,
                         int selectedTargetTrackingId, int selectedWaypointId, float confidence,
                         int actionDurationTicks, TacticalObjective tacticalObjective,
                         AbortCondition abortCondition) {
        this.schemaVersion = SchemaVersion.ACTION_V0_1;
        this.observationSequenceNumber = observationSequenceNumber;
        this.generatedTimestampNanos = generatedTimestampNanos;
        this.modelVersion = modelVersion;
        this.forward = forward;
        this.strafe = strafe;
        this.yawDeltaDegrees = yawDeltaDegrees;
        this.pitchDeltaDegrees = pitchDeltaDegrees;
        this.jumpProbability = jumpProbability;
        this.sprintProbability = sprintProbability;
        this.sneakProbability = sneakProbability;
        this.attackProbability = attackProbability;
        this.useOrPlaceProbability = useOrPlaceProbability;
        this.dropProbability = dropProbability;
        this.inventoryToggleProbability = inventoryToggleProbability;
        this.hotbarSlot = hotbarSlot;
        this.selectedSkill = selectedSkill;
        this.selectedTargetTrackingId = selectedTargetTrackingId;
        this.selectedWaypointId = selectedWaypointId;
        this.confidence = confidence;
        this.actionDurationTicks = actionDurationTicks;
        this.tacticalObjective = tacticalObjective;
        this.abortCondition = abortCondition;
    }

    public static ActionCommand zero(long observationSequenceNumber, long generatedTimestampNanos, String modelVersion) {
        return new ActionCommand(observationSequenceNumber, generatedTimestampNanos, modelVersion,
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
            KEEP_CURRENT_HOTBAR_SLOT, Skill.NONE, -1, -1, 1f, 1,
            TacticalObjective.NONE, AbortCondition.NONE);
    }

    public SchemaVersion schemaVersion() { return schemaVersion; }
    public long observationSequenceNumber() { return observationSequenceNumber; }
    public long generatedTimestampNanos() { return generatedTimestampNanos; }
    public String modelVersion() { return modelVersion; }
    public float forward() { return forward; }
    public float strafe() { return strafe; }
    public float yawDeltaDegrees() { return yawDeltaDegrees; }
    public float pitchDeltaDegrees() { return pitchDeltaDegrees; }
    public float jumpProbability() { return jumpProbability; }
    public float sprintProbability() { return sprintProbability; }
    public float sneakProbability() { return sneakProbability; }
    public float attackProbability() { return attackProbability; }
    public float useOrPlaceProbability() { return useOrPlaceProbability; }
    public float dropProbability() { return dropProbability; }
    public float inventoryToggleProbability() { return inventoryToggleProbability; }
    public int hotbarSlot() { return hotbarSlot; }
    public Skill selectedSkill() { return selectedSkill; }
    public int selectedTargetTrackingId() { return selectedTargetTrackingId; }
    public int selectedWaypointId() { return selectedWaypointId; }
    public float confidence() { return confidence; }
    public int actionDurationTicks() { return actionDurationTicks; }
    public TacticalObjective tacticalObjective() { return tacticalObjective; }
    public AbortCondition abortCondition() { return abortCondition; }
}
