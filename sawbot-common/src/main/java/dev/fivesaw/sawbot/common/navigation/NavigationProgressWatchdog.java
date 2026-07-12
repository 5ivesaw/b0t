package dev.fivesaw.sawbot.common.navigation;

/** Bounded progress watchdog for one movement operation. */
public final class NavigationProgressWatchdog {
    private double anchorX;
    private double anchorZ;
    private int commandedTicks;
    private int stuckEvents;
    private boolean initialized;

    public boolean update(double x, double z, boolean commandedMovement,
                          int windowTicks, double minimumProgress) {
        if (!commandedMovement) {
            resetWindow(x, z);
            return false;
        }
        if (!initialized) resetWindow(x, z);
        commandedTicks++;
        if (commandedTicks < Math.max(2, windowTicks)) return false;
        double dx = x - anchorX;
        double dz = z - anchorZ;
        boolean stuck = dx * dx + dz * dz
            < minimumProgress * minimumProgress;
        resetWindow(x, z);
        if (stuck) stuckEvents++;
        return stuck;
    }

    public void resetWindow(double x, double z) {
        anchorX = x;
        anchorZ = z;
        commandedTicks = 0;
        initialized = true;
    }

    public void reset() {
        anchorX = 0D;
        anchorZ = 0D;
        commandedTicks = 0;
        initialized = false;
    }

    public int stuckEvents() { return stuckEvents; }
}
