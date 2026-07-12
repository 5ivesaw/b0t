package dev.fivesaw.sawbot.common.bridging;

/** Cardinal block-to-block bridge direction. Diagonal bridges are alternating cardinal steps. */
public enum BridgeDirection {
    NORTH(0, -1),
    SOUTH(0, 1),
    WEST(-1, 0),
    EAST(1, 0);

    private final int dx;
    private final int dz;

    BridgeDirection(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() { return dx; }
    public int dz() { return dz; }

    public static BridgeDirection fromDelta(int dx, int dz) {
        if (dx == 1 && dz == 0) return EAST;
        if (dx == -1 && dz == 0) return WEST;
        if (dx == 0 && dz == 1) return SOUTH;
        if (dx == 0 && dz == -1) return NORTH;
        throw new IllegalArgumentException("bridge direction must be one cardinal block");
    }
}
