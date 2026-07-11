package dev.fivesaw.sawbot.forge.hud;

/** Small monotonic presentation-only transition. It never delays state changes. */
public final class MotionValue {
    private float value;
    private long lastNanos;

    public MotionValue() { this(0F); }

    public MotionValue(float initialValue) {
        value = clamp(initialValue);
    }

    public float update(boolean active, boolean animationsEnabled, long nowNanos) {
        float target = active ? 1F : 0F;
        if (!animationsEnabled) {
            value = target;
            lastNanos = nowNanos;
            return value;
        }
        if (lastNanos == 0L) {
            lastNanos = nowNanos;
            value = target;
            return value;
        }
        long elapsedNanos = Math.max(0L, Math.min(100_000_000L, nowNanos - lastNanos));
        lastNanos = nowNanos;
        float elapsedSeconds = elapsedNanos / 1_000_000_000F;
        float response = active ? 11.5F : 14F;
        value += (target - value) * Math.min(1F, elapsedSeconds * response);
        if (Math.abs(target - value) < 0.002F) value = target;
        return value;
    }

    public void snap(boolean active, long nowNanos) {
        value = active ? 1F : 0F;
        lastNanos = nowNanos;
    }

    public float value() { return value; }

    private static float clamp(float value) { return Math.max(0F, Math.min(1F, value)); }
}
