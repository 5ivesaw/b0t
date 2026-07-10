package dev.fivesaw.sawbot.common.action;

public final class ActionValidator {
    public static final long DEFAULT_MAX_AGE_NANOS = 250_000_000L;
    public static final long DEFAULT_MAX_SEQUENCE_LAG = 3L;

    private ActionValidator() { }

    public static ActionValidation validate(ActionCommand command, long latestObservationSequence,
                                            long nowNanos, long maxAgeNanos, long maxSequenceLag) {
        if (command == null) return ActionValidation.invalid("command is null");
        if (!"sawbot.action/0.1".equals(command.schemaVersion().identifier())) {
            return ActionValidation.invalid("unsupported schema");
        }
        if (command.observationSequenceNumber() < 0 || command.observationSequenceNumber() > latestObservationSequence) {
            return ActionValidation.invalid("invalid observation sequence");
        }
        if (latestObservationSequence - command.observationSequenceNumber() > maxSequenceLag) {
            return ActionValidation.invalid("stale observation sequence");
        }
        long age = nowNanos - command.generatedTimestampNanos();
        if (age < 0 || age > maxAgeNanos) return ActionValidation.invalid("stale or future timestamp");
        if (command.modelVersion() == null || command.modelVersion().isEmpty() || command.modelVersion().length() > 64) {
            return ActionValidation.invalid("invalid model version");
        }
        if (!within(command.forward(), -1f, 1f) || !within(command.strafe(), -1f, 1f)) {
            return ActionValidation.invalid("movement outside range");
        }
        if (!within(command.yawDeltaDegrees(), -45f, 45f) || !within(command.pitchDeltaDegrees(), -30f, 30f)) {
            return ActionValidation.invalid("camera delta outside range");
        }
        float[] probabilities = { command.jumpProbability(), command.sprintProbability(), command.sneakProbability(),
            command.attackProbability(), command.useOrPlaceProbability(), command.dropProbability(),
            command.inventoryToggleProbability(), command.confidence() };
        for (float probability : probabilities) {
            if (!within(probability, 0f, 1f)) return ActionValidation.invalid("probability/confidence outside range");
        }
        if (command.hotbarSlot() < -1 || command.hotbarSlot() > 8) return ActionValidation.invalid("invalid hotbar slot");
        if (command.actionDurationTicks() < 1 || command.actionDurationTicks() > 4) {
            return ActionValidation.invalid("invalid action duration");
        }
        if (command.selectedSkill() == null || command.tacticalObjective() == null || command.abortCondition() == null) {
            return ActionValidation.invalid("missing enum field");
        }
        return ActionValidation.valid();
    }

    private static boolean within(float value, float minimum, float maximum) {
        return !Float.isNaN(value) && !Float.isInfinite(value) && value >= minimum && value <= maximum;
    }
}
