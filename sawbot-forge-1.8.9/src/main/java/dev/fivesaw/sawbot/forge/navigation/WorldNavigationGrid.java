package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.navigation.MovementPath;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationGrid;
import dev.fivesaw.sawbot.common.navigation.NavigationMovement;
import dev.fivesaw.sawbot.common.navigation.NavigationPath;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.forge.sensors.BlockSemanticClassifier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Persistent client-thread world cache used by snapshot capture and live motion.
 *
 * Unlike the previous navigator, this object is not recreated every few ticks.
 * World reads are retained in bounded LRU caches, while only the current and
 * near-future movement cells are explicitly refreshed after world changes.
 */
public final class WorldNavigationGrid implements NavigationGrid {
    private static final int MAX_CACHE_ENTRIES = 32768;
    private static final int MAX_PENALTY_CACHE_ENTRIES = 16384;
    private static final float EXPOSED_SIDE_PENALTY = 0.14F;

    private final World world;
    private final BlockSemanticClassifier classifier;
    private final Map<NavigationCell, Boolean> standableCache =
        new BoundedMap<NavigationCell, Boolean>(MAX_CACHE_ENTRIES);
    private final Map<NavigationCell, Float> penaltyCache =
        new BoundedMap<NavigationCell, Float>(MAX_PENALTY_CACHE_ENTRIES);
    private int worldReads;
    private int cacheHits;
    private int liveRefreshes;
    private int invalidations;

    public WorldNavigationGrid(World world) {
        if (world == null) throw new IllegalArgumentException("world");
        this.world = world;
        this.classifier = new BlockSemanticClassifier();
    }

    public boolean matchesWorld(World candidate) { return world == candidate; }

    @Override public boolean isStandable(int x, int y, int z) {
        if (y < 1 || y > 254) return false;
        NavigationCell key = new NavigationCell(x, y, z);
        Boolean cached = standableCache.get(key);
        if (cached != null) {
            cacheHits++;
            return cached.booleanValue();
        }
        boolean value = calculateStandable(x, y, z);
        standableCache.put(key, Boolean.valueOf(value));
        return value;
    }

    public boolean refreshStandable(int x, int y, int z) {
        NavigationCell key = new NavigationCell(x, y, z);
        standableCache.remove(key);
        penaltyCache.remove(key);
        liveRefreshes++;
        return isStandable(x, y, z);
    }

    public void invalidateAround(int x, int y, int z, int radius) {
        int bounded = Math.max(0, Math.min(3, radius));
        for (int dx = -bounded; dx <= bounded; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -bounded; dz <= bounded; dz++) {
                    NavigationCell key = new NavigationCell(x + dx, y + dy, z + dz);
                    standableCache.remove(key);
                    penaltyCache.remove(key);
                }
            }
        }
        invalidations++;
    }

    @Override public float traversalPenalty(int x, int y, int z) {
        NavigationCell key = new NavigationCell(x, y, z);
        Float cached = penaltyCache.get(key);
        if (cached != null) {
            cacheHits++;
            return cached.floatValue();
        }
        int exposed = 0;
        if (!isStandable(x + 1, y, z)) exposed++;
        if (!isStandable(x - 1, y, z)) exposed++;
        if (!isStandable(x, y, z + 1)) exposed++;
        if (!isStandable(x, y, z - 1)) exposed++;
        float value = exposed * EXPOSED_SIDE_PENALTY;
        penaltyCache.put(key, Float.valueOf(value));
        return value;
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
        if (dx != 0 && dz != 0) {
            if (dy != 0) return false;
            if (!isStandable(from.x() + dx, from.y(), from.z())
                || !isStandable(from.x(), from.y(), from.z() + dz)) return false;
        }
        return true;
    }

    private boolean calculateStandable(int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = new BlockPos(x, y + 1, z);
        BlockPos support = new BlockPos(x, y - 1, z);
        if (!world.isBlockLoaded(feet) || !world.isBlockLoaded(head)
            || !world.isBlockLoaded(support)) return false;
        BlockSemanticClassifier.CellClassification feetCell = classify(feet);
        BlockSemanticClassifier.CellClassification headCell = classify(head);
        BlockSemanticClassifier.CellClassification supportCell = classify(support);
        return passable(feetCell) && passable(headCell)
            && (supportCell.flags & LocalTerrainSnapshot.FLAG_SAFE_SUPPORT) != 0
            && (supportCell.flags & LocalTerrainSnapshot.FLAG_HAZARD) == 0
            && (supportCell.flags & LocalTerrainSnapshot.FLAG_LIQUID) == 0;
    }

    private BlockSemanticClassifier.CellClassification classify(BlockPos position) {
        IBlockState state = world.getBlockState(position);
        worldReads++;
        return classifier.classify(world, position, state, false);
    }

    private static boolean passable(BlockSemanticClassifier.CellClassification cell) {
        return cell.collisionHeightClass == 0
            && (cell.flags & LocalTerrainSnapshot.FLAG_LIQUID) == 0
            && (cell.flags & LocalTerrainSnapshot.FLAG_HAZARD) == 0;
    }

    public NavigationCell nearestStandable(int x, int y, int z,
                                           int horizontalRadius,
                                           int verticalRadius) {
        if (isStandable(x, y, z)) return new NavigationCell(x, y, z);
        int maxHorizontal = Math.max(0, horizontalRadius);
        int maxVertical = Math.max(0, verticalRadius);
        for (int radius = 0; radius <= maxHorizontal; radius++) {
            for (int dy = 0; dy <= maxVertical; dy++) {
                NavigationCell found = scanRing(x, y + dy, z, radius);
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

    /** Legacy cell-path validation retained for Phase 7/8 compatibility. */
    public PathValidation validatePathWindow(NavigationPath path, int startIndex,
                                             int maximumNodes, boolean refresh) {
        if (path == null || path.size() == 0) {
            return new PathValidation(false, startIndex, "missing path");
        }
        int start = Math.max(0, Math.min(startIndex, path.size() - 1));
        int end = Math.min(path.size() - 1, start + Math.max(1, maximumNodes));
        NavigationCell previous = path.cell(start);
        if (!sample(previous, refresh)) {
            return new PathValidation(false, start, "current route cell changed");
        }
        for (int index = start + 1; index <= end; index++) {
            NavigationCell current = path.cell(index);
            if (!sample(current, refresh)) {
                return new PathValidation(false, index, "route cell changed");
            }
            if (!canTransition(previous, current)) {
                return new PathValidation(false, index, "route transition changed");
            }
            previous = current;
        }
        return new PathValidation(true, -1, "ok");
    }

    /** Live-validates only the active and bounded near-future operations. */
    public MovementValidation validateMovementWindow(MovementPath path,
                                                       int movementIndex,
                                                       int maximumMovements,
                                                       boolean refresh) {
        if (path == null || path.movementCount() == 0) {
            return new MovementValidation(false, movementIndex, "missing movement path");
        }
        int start = Math.max(0, Math.min(movementIndex, path.movementCount() - 1));
        int end = Math.min(path.movementCount() - 1,
            start + Math.max(0, maximumMovements - 1));
        for (int index = start; index <= end; index++) {
            NavigationMovement movement = path.movement(index);
            if (!sample(movement.to(), refresh)) {
                return new MovementValidation(false, index,
                    "destination support/headroom changed");
            }
            if (!canTransition(movement.from(), movement.to())) {
                return new MovementValidation(false, index,
                    "movement geometry changed");
            }
        }
        return new MovementValidation(true, -1, "ok");
    }

    private boolean sample(NavigationCell cell, boolean refresh) {
        return refresh ? refreshStandable(cell.x(), cell.y(), cell.z())
            : isStandable(cell.x(), cell.y(), cell.z());
    }

    /**
     * Tests a straight local corridor. This is used only for immediate steering
     * and direct micro-paths; long-range planning uses immutable snapshots.
     */
    public boolean isCorridorSafe(double fromX, double fromY, double fromZ,
                                  double toX, double toY, double toZ,
                                  boolean refresh) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int)Math.ceil(horizontalDistance / 0.32D));
        NavigationCell previous = null;
        for (int step = 0; step <= steps; step++) {
            double t = (double)step / (double)steps;
            int x = floor(fromX + dx * t);
            int z = floor(fromZ + dz * t);
            int expectedY = floor(fromY + dy * t + 0.01D);
            NavigationCell current = nearestStandableLive(x, expectedY, z, refresh);
            if (current == null) return false;
            if (previous != null && !previous.equals(current)) {
                int stepX = Math.abs(current.x() - previous.x());
                int stepZ = Math.abs(current.z() - previous.z());
                if (stepX > 1 || stepZ > 1 || !canTransition(previous, current)) return false;
            }
            previous = current;
        }
        return true;
    }

    public MotionProbe probeDirection(double x, double y, double z,
                                      float yawDegrees, double distance,
                                      boolean refresh) {
        double radians = Math.toRadians(yawDegrees);
        double targetX = x - Math.sin(radians) * distance;
        double targetZ = z + Math.cos(radians) * distance;
        NavigationCell destination = nearestStandableLive(
            floor(targetX), floor(y + 0.01D), floor(targetZ), refresh);
        if (destination == null) return MotionProbe.blocked(yawDegrees, targetX, targetZ);
        if (!isCorridorSafe(x, y, z, targetX, destination.centerY(), targetZ, refresh)) {
            return MotionProbe.blocked(yawDegrees, targetX, targetZ);
        }
        return new MotionProbe(true, yawDegrees, targetX, targetZ,
            destination, traversalPenalty(destination.x(), destination.y(), destination.z()));
    }

    private NavigationCell nearestStandableLive(int x, int y, int z, boolean refresh) {
        int[] offsets = {0, 1, -1};
        for (int offset : offsets) {
            boolean standable = refresh ? refreshStandable(x, y + offset, z)
                : isStandable(x, y + offset, z);
            if (standable) return new NavigationCell(x, y + offset, z);
        }
        return null;
    }

    private static int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    public int cacheSize() { return standableCache.size(); }
    public int worldReads() { return worldReads; }
    public int cacheHits() { return cacheHits; }
    public int liveRefreshes() { return liveRefreshes; }
    public int invalidations() { return invalidations; }

    public void clear() {
        standableCache.clear();
        penaltyCache.clear();
    }

    public static final class PathValidation {
        private final boolean valid;
        private final int invalidIndex;
        private final String reason;

        PathValidation(boolean valid, int invalidIndex, String reason) {
            this.valid = valid;
            this.invalidIndex = invalidIndex;
            this.reason = reason;
        }

        public boolean valid() { return valid; }
        public int invalidIndex() { return invalidIndex; }
        public String reason() { return reason; }
    }

    public static final class MovementValidation {
        private final boolean valid;
        private final int invalidMovementIndex;
        private final String reason;

        MovementValidation(boolean valid, int invalidMovementIndex, String reason) {
            this.valid = valid;
            this.invalidMovementIndex = invalidMovementIndex;
            this.reason = reason;
        }

        public boolean valid() { return valid; }
        public int invalidMovementIndex() { return invalidMovementIndex; }
        public String reason() { return reason; }
    }

    public static final class MotionProbe {
        private final boolean safe;
        private final float yawDegrees;
        private final double targetX;
        private final double targetZ;
        private final NavigationCell destination;
        private final float riskPenalty;

        MotionProbe(boolean safe, float yawDegrees, double targetX, double targetZ,
                    NavigationCell destination, float riskPenalty) {
            this.safe = safe;
            this.yawDegrees = yawDegrees;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.destination = destination;
            this.riskPenalty = riskPenalty;
        }

        static MotionProbe blocked(float yawDegrees, double targetX, double targetZ) {
            return new MotionProbe(false, yawDegrees, targetX, targetZ, null,
                Float.POSITIVE_INFINITY);
        }

        public boolean safe() { return safe; }
        public float yawDegrees() { return yawDegrees; }
        public double targetX() { return targetX; }
        public double targetZ() { return targetZ; }
        public NavigationCell destination() { return destination; }
        public float riskPenalty() { return riskPenalty; }
    }

    private static final class BoundedMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int maximumEntries;

        BoundedMap(int maximumEntries) {
            super(256, 0.75F, true);
            this.maximumEntries = maximumEntries;
        }

        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maximumEntries;
        }
    }
}
