package dev.fivesaw.sawbot.forge.navigation;

/** Fast bounded yaw servo with acceleration limiting and dead-zone snap. */
public final class NavigationCameraController {
    private float previousTurn;

    public float step(float currentYaw, float desiredYaw,
                      float configuredMaximumDegreesPerTick) {
        float error = wrapDegrees(desiredYaw - currentYaw);
        float magnitude = Math.abs(error);
        if (magnitude <= 0.45F) {
            previousTurn = error;
            return error;
        }

        float dynamicMaximum = Math.min(configuredMaximumDegreesPerTick,
            10F + magnitude * 0.82F);
        float desiredTurn = Math.min(dynamicMaximum,
            Math.max(2.4F, magnitude * 0.78F + 1.8F));
        if (error < 0F) desiredTurn = -desiredTurn;

        float accelerationLimit = magnitude > 90F ? 24F : 14F;
        float delta = desiredTurn - previousTurn;
        if (delta > accelerationLimit) desiredTurn = previousTurn + accelerationLimit;
        else if (delta < -accelerationLimit) desiredTurn = previousTurn - accelerationLimit;

        if (Math.abs(desiredTurn) > magnitude) desiredTurn = error;
        previousTurn = desiredTurn;
        return desiredTurn;
    }

    public void reset() { previousTurn = 0F; }
    public float previousTurn() { return previousTurn; }

    public static float wrapDegrees(float value) {
        float wrapped = value % 360F;
        if (wrapped >= 180F) wrapped -= 360F;
        if (wrapped < -180F) wrapped += 360F;
        return wrapped;
    }
}
