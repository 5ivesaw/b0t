package dev.fivesaw.sawbot.common.observation;

public final class EgocentricTransform {
    private EgocentricTransform() { }
    public static byte quadrant(float yawDegrees) {
        int value = (int)Math.floor((yawDegrees / 90.0f) + 0.5f);
        return (byte)(value & 3);
    }
    public static int worldDx(int right, int forward, byte quadrant) {
        switch (quadrant & 3) { case 0: return -right; case 1: return -forward; case 2: return right; default: return forward; }
    }
    public static int worldDz(int right, int forward, byte quadrant) {
        switch (quadrant & 3) { case 0: return forward; case 1: return -right; case 2: return -forward; default: return right; }
    }
    public static float right(double dx, double dz, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return (float)(-dx * Math.cos(radians) - dz * Math.sin(radians));
    }
    public static float forward(double dx, double dz, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return (float)(-dx * Math.sin(radians) + dz * Math.cos(radians));
    }
}
