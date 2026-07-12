package dev.fivesaw.sawbot.common.navigation;

/**
 * Geometry-only route cursor used by the real-time navigation body.
 *
 * It deliberately treats a path as a corridor rather than a list of mandatory
 * block centres: the cursor can re-anchor to nearby future nodes after knockback,
 * manual displacement, or imperfect movement and can select a farther safe
 * look-ahead node when a corridor validator allows it.
 */
public final class AdaptivePathCursor {
    private AdaptivePathCursor() { }

    public static Projection project(NavigationPath path, int currentIndex,
                                     double x, double y, double z,
                                     int maximumBacktrackNodes,
                                     int maximumForwardNodes) {
        if (path == null) throw new IllegalArgumentException("path");
        if (currentIndex < 0 || currentIndex >= path.size()) {
            throw new IllegalArgumentException("currentIndex");
        }
        int start = Math.max(0, currentIndex - Math.max(0, maximumBacktrackNodes));
        int end = Math.min(path.size() - 1,
            currentIndex + Math.max(0, maximumForwardNodes));
        int bestIndex = currentIndex;
        double bestDistanceSquared = distanceSquared(path.cell(currentIndex), x, y, z);
        for (int index = start; index <= end; index++) {
            double distanceSquared = distanceSquared(path.cell(index), x, y, z);
            if (distanceSquared < bestDistanceSquared - 1.0E-9D
                || (Math.abs(distanceSquared - bestDistanceSquared) <= 1.0E-9D
                    && index > bestIndex)) {
                bestIndex = index;
                bestDistanceSquared = distanceSquared;
            }
        }
        return new Projection(bestIndex, bestDistanceSquared,
            bestIndex - currentIndex);
    }

    public static int farthestSafeLookahead(NavigationPath path, int currentIndex,
                                             int maximumNodes,
                                             double maximumHorizontalDistance,
                                             CorridorValidator validator) {
        if (path == null || validator == null) throw new IllegalArgumentException("path/validator");
        if (currentIndex < 0 || currentIndex >= path.size()) {
            throw new IllegalArgumentException("currentIndex");
        }
        int last = Math.min(path.size() - 1,
            currentIndex + Math.max(0, maximumNodes));
        NavigationCell origin = path.cell(currentIndex);
        int best = currentIndex;
        double maximumDistanceSquared = maximumHorizontalDistance * maximumHorizontalDistance;
        for (int index = currentIndex + 1; index <= last; index++) {
            NavigationCell candidate = path.cell(index);
            double dx = candidate.centerX() - origin.centerX();
            double dz = candidate.centerZ() - origin.centerZ();
            if (dx * dx + dz * dz > maximumDistanceSquared) break;
            if (!validator.isSafe(origin, candidate)) break;
            best = index;
        }
        return best;
    }

    public static double nearestHorizontalDistanceSquared(NavigationPath path,
                                                           int fromIndex,
                                                           int toIndex,
                                                           double x,
                                                           double z) {
        if (path == null) throw new IllegalArgumentException("path");
        int start = Math.max(0, Math.min(fromIndex, path.size() - 1));
        int end = Math.max(start, Math.min(toIndex, path.size() - 1));
        double best = Double.POSITIVE_INFINITY;
        for (int index = start; index <= end; index++) {
            NavigationCell cell = path.cell(index);
            double dx = cell.centerX() - x;
            double dz = cell.centerZ() - z;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }

    private static double distanceSquared(NavigationCell cell,
                                          double x, double y, double z) {
        double dx = cell.centerX() - x;
        double dy = cell.centerY() - y;
        double dz = cell.centerZ() - z;
        return dx * dx + dy * dy * 1.5D + dz * dz;
    }

    public interface CorridorValidator {
        boolean isSafe(NavigationCell from, NavigationCell to);
    }

    public static final class Projection {
        private final int index;
        private final double distanceSquared;
        private final int indexDelta;

        Projection(int index, double distanceSquared, int indexDelta) {
            this.index = index;
            this.distanceSquared = distanceSquared;
            this.indexDelta = indexDelta;
        }

        public int index() { return index; }
        public double distanceSquared() { return distanceSquared; }
        public int indexDelta() { return indexDelta; }
    }
}
