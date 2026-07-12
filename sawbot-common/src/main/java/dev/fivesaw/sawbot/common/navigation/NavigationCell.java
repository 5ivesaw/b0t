package dev.fivesaw.sawbot.common.navigation;

/** Immutable integer player-feet cell used by the deterministic navigation body. */
public final class NavigationCell {
    private final int x;
    private final int y;
    private final int z;

    public NavigationCell(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public double centerX() { return x + 0.5D; }
    public double centerY() { return y; }
    public double centerZ() { return z + 0.5D; }

    public int horizontalManhattan(NavigationCell other) {
        return Math.abs(x - other.x) + Math.abs(z - other.z);
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof NavigationCell)) return false;
        NavigationCell other = (NavigationCell)value;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override public String toString() {
        return x + "," + y + "," + z;
    }
}
