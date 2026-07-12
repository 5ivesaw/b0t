package dev.fivesaw.sawbot.forge.combat;

/** Bounded visible yaw/pitch servo with velocity and acceleration continuity. */
public final class HumanRotationController {
    private float previousYawStep;
    private float previousPitchStep;

    public RotationStep step(float currentYaw, float currentPitch,
                             float desiredYaw, float desiredPitch,
                             float maximumYawStep, float maximumPitchStep) {
        float yawError = wrapDegrees(desiredYaw - currentYaw);
        float pitchError = clamp(desiredPitch - currentPitch, -180F, 180F);
        float yawStep = axisStep(yawError, previousYawStep,
            maximumYawStep, 12F, 0.35F);
        float pitchStep = axisStep(pitchError, previousPitchStep,
            maximumPitchStep, 8F, 0.28F);
        previousYawStep = yawStep;
        previousPitchStep = pitchStep;
        return new RotationStep(yawStep, pitchStep, yawError, pitchError);
    }

    private static float axisStep(float error, float previous, float maximum,
                                  float acceleration, float deadZone) {
        float magnitude = Math.abs(error);
        if (magnitude <= deadZone) return error;
        float desired = Math.min(maximum,
            Math.max(0.9F, magnitude * 0.62F + 0.75F));
        if (error < 0F) desired = -desired;
        float delta = desired - previous;
        if (delta > acceleration) desired = previous + acceleration;
        else if (delta < -acceleration) desired = previous - acceleration;
        if (Math.abs(desired) > magnitude) desired = error;
        return desired;
    }

    public void reset() {
        previousYawStep = 0F;
        previousPitchStep = 0F;
    }

    public float previousYawStep() { return previousYawStep; }
    public float previousPitchStep() { return previousPitchStep; }

    public static float wrapDegrees(float value) {
        float wrapped = value % 360F;
        if (wrapped >= 180F) wrapped -= 360F;
        if (wrapped < -180F) wrapped += 360F;
        return wrapped;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class RotationStep {
        private final float yawStep;
        private final float pitchStep;
        private final float yawError;
        private final float pitchError;

        RotationStep(float yawStep, float pitchStep,
                     float yawError, float pitchError) {
            this.yawStep = yawStep;
            this.pitchStep = pitchStep;
            this.yawError = yawError;
            this.pitchError = pitchError;
        }

        public float yawStep() { return yawStep; }
        public float pitchStep() { return pitchStep; }
        public float yawError() { return yawError; }
        public float pitchError() { return pitchError; }
    }
}
