package dev.fivesaw.sawbot.common.navigation;

import java.util.Arrays;

/**
 * Compact immutable navigation snapshot.
 *
 * Minecraft world access happens only while the client-thread builder is being
 * filled. Background planners consume only the copied arrays in this class.
 */
public final class ImmutableNavigationGrid implements NavigationGrid {
    private static final byte STANDABLE = 1;
    private static final float EXPOSED_SIDE_PENALTY = 0.14F;

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final byte[] flags;
    private final float[] penalties;
    private final int sampledCells;

    private ImmutableNavigationGrid(int minX, int minY, int minZ,
                                    int sizeX, int sizeY, int sizeZ,
                                    byte[] flags, float[] penalties,
                                    int sampledCells) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.flags = flags;
        this.penalties = penalties;
        this.sampledCells = sampledCells;
    }

    @Override public boolean isStandable(int x, int y, int z) {
        int index = index(x, y, z);
        return index >= 0 && (flags[index] & STANDABLE) != 0;
    }

    @Override public float traversalPenalty(int x, int y, int z) {
        int index = index(x, y, z);
        return index < 0 ? Float.POSITIVE_INFINITY : penalties[index];
    }

    @Override public boolean canTransition(NavigationCell from, NavigationCell to) {
        if (from == null || to == null) return false;
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        int dz = to.z() - from.z();
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || Math.abs(dy) > 1
            || (dx == 0 && dz == 0) || !isStandable(to.x(), to.y(), to.z())) {
            return false;
        }
        if (dy != 0 && dx != 0 && dz != 0) return false;
        if (dy > 0 && dy != 1) return false;
        if (dy < 0 && dy != -1) return false;
        if (dx != 0 && dz != 0) {
            boolean xClear = isStandable(from.x() + dx, from.y(), from.z());
            boolean zClear = isStandable(from.x(), from.y(), from.z() + dz);
            if (!xClear || !zClear) return false;
        }
        return true;
    }

    public NavigationCell nearestStandable(int x, int y, int z,
                                            int horizontalRadius,
                                            int verticalRadius) {
        if (isStandable(x, y, z)) return new NavigationCell(x, y, z);
        for (int radius = 0; radius <= Math.max(0, horizontalRadius); radius++) {
            for (int dy = 0; dy <= Math.max(0, verticalRadius); dy++) {
                int candidateY = y + dy;
                NavigationCell found = scanRing(x, candidateY, z, radius);
                if (found != null) return found;
                if (dy > 0) {
                    found = scanRing(x, y - dy, z, radius);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private NavigationCell scanRing(int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                if (isStandable(x + dx, y, z + dz)) {
                    return new NavigationCell(x + dx, y, z + dz);
                }
            }
        }
        return null;
    }

    private int index(int x, int y, int z) {
        int localX = x - minX;
        int localY = y - minY;
        int localZ = z - minZ;
        if (localX < 0 || localX >= sizeX || localY < 0 || localY >= sizeY
            || localZ < 0 || localZ >= sizeZ) return -1;
        return (localY * sizeZ + localZ) * sizeX + localX;
    }

    public boolean contains(int x, int y, int z) { return index(x, y, z) >= 0; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return minX + sizeX - 1; }
    public int maxY() { return minY + sizeY - 1; }
    public int maxZ() { return minZ + sizeZ - 1; }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int sampledCells() { return sampledCells; }

    public static final class Builder {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final byte[] flags;
        private int sampledCells;

        public Builder(int minX, int minY, int minZ,
                       int maxX, int maxY, int maxZ) {
            if (maxX < minX || maxY < minY || maxZ < minZ) {
                throw new IllegalArgumentException("snapshot bounds");
            }
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = maxX - minX + 1;
            this.sizeY = maxY - minY + 1;
            this.sizeZ = maxZ - minZ + 1;
            long count = (long)sizeX * (long)sizeY * (long)sizeZ;
            if (count > 750000L) throw new IllegalArgumentException("snapshot too large");
            this.flags = new byte[(int)count];
        }

        public void setStandable(int x, int y, int z, boolean standable) {
            int index = index(x, y, z);
            if (index < 0) throw new IllegalArgumentException("cell outside snapshot");
            flags[index] = standable ? STANDABLE : 0;
            sampledCells++;
        }

        public ImmutableNavigationGrid build() {
            byte[] immutableFlags = Arrays.copyOf(flags, flags.length);
            float[] penalties = new float[immutableFlags.length];
            for (int localY = 0; localY < sizeY; localY++) {
                for (int localZ = 0; localZ < sizeZ; localZ++) {
                    for (int localX = 0; localX < sizeX; localX++) {
                        int index = (localY * sizeZ + localZ) * sizeX + localX;
                        if ((immutableFlags[index] & STANDABLE) == 0) continue;
                        int exposed = 0;
                        if (!standable(immutableFlags, localX + 1, localY, localZ)) exposed++;
                        if (!standable(immutableFlags, localX - 1, localY, localZ)) exposed++;
                        if (!standable(immutableFlags, localX, localY, localZ + 1)) exposed++;
                        if (!standable(immutableFlags, localX, localY, localZ - 1)) exposed++;
                        penalties[index] = exposed * EXPOSED_SIDE_PENALTY;
                    }
                }
            }
            return new ImmutableNavigationGrid(minX, minY, minZ,
                sizeX, sizeY, sizeZ, immutableFlags, penalties, sampledCells);
        }

        private int index(int x, int y, int z) {
            int localX = x - minX;
            int localY = y - minY;
            int localZ = z - minZ;
            if (localX < 0 || localX >= sizeX || localY < 0 || localY >= sizeY
                || localZ < 0 || localZ >= sizeZ) return -1;
            return (localY * sizeZ + localZ) * sizeX + localX;
        }

        private boolean standable(byte[] source, int localX, int localY, int localZ) {
            if (localX < 0 || localX >= sizeX || localY < 0 || localY >= sizeY
                || localZ < 0 || localZ >= sizeZ) return false;
            int index = (localY * sizeZ + localZ) * sizeX + localX;
            return (source[index] & STANDABLE) != 0;
        }
    }
}
