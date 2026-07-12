package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.navigation.ImmutableNavigationGrid;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;

/**
 * Incremental client-thread capture for one navigation plan generation.
 *
 * A small local snapshot is completed first for low-latency provisional motion.
 * A wider goal-corridor snapshot then finishes over a bounded number of cells per
 * tick and is sent to the background worker as the replacement/full segment.
 */
public final class NavigationSnapshotCapture {
    private static final int LOCAL_VERTICAL_MARGIN = 2;
    private static final int FULL_VERTICAL_MARGIN = 4;

    private final WorldNavigationGrid worldGrid;
    private final long generation;
    private final long waypointRevision;
    private final long localRequestId;
    private final long fullRequestId;
    private final NavigationCell start;
    private final NavigationCell finalGoal;
    private final NavigationCell segmentGoalCandidate;
    private final boolean segmentCompletesFinalGoal;
    private final int horizontalRadius;
    private final int verticalRadius;
    private final int maximumExpandedNodes;
    private final float heuristicWeight;
    private final int localRadius;

    private final Bounds localBounds;
    private final Bounds fullBounds;
    private final ImmutableNavigationGrid.Builder localBuilder;
    private final ImmutableNavigationGrid.Builder fullBuilder;
    private int localCursor;
    private int fullCursor;
    private boolean localRequestTaken;
    private boolean fullRequestTaken;
    private String status = "LOCAL_CAPTURE";

    public NavigationSnapshotCapture(WorldNavigationGrid worldGrid,
                                     long generation, long waypointRevision,
                                     NavigationCell start,
                                     NavigationCell finalGoal,
                                     int horizontalRadius, int verticalRadius,
                                     int maximumExpandedNodes,
                                     float heuristicWeight,
                                     int localRadius, int corridorMargin) {
        this(worldGrid, generation, waypointRevision, generation * 2L,
            generation * 2L + 1L, start, finalGoal, horizontalRadius,
            verticalRadius, maximumExpandedNodes, heuristicWeight,
            localRadius, corridorMargin);
    }

    public NavigationSnapshotCapture(WorldNavigationGrid worldGrid,
                                     long generation, long waypointRevision,
                                     long localRequestId, long fullRequestId,
                                     NavigationCell start,
                                     NavigationCell finalGoal,
                                     int horizontalRadius, int verticalRadius,
                                     int maximumExpandedNodes,
                                     float heuristicWeight,
                                     int localRadius, int corridorMargin) {
        if (worldGrid == null || start == null || finalGoal == null) {
            throw new IllegalArgumentException("snapshot capture component");
        }
        this.worldGrid = worldGrid;
        this.generation = generation;
        this.waypointRevision = waypointRevision;
        this.localRequestId = localRequestId;
        this.fullRequestId = fullRequestId;
        this.start = start;
        this.finalGoal = finalGoal;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.maximumExpandedNodes = maximumExpandedNodes;
        this.heuristicWeight = heuristicWeight;
        this.localRadius = localRadius;

        double dx = finalGoal.x() - start.x();
        double dz = finalGoal.z() - start.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double segmentDistance = Math.max(3D, horizontalRadius - Math.max(4, corridorMargin));
        if (distance > segmentDistance) {
            double scale = segmentDistance / distance;
            int x = floor(start.x() + dx * scale);
            int z = floor(start.z() + dz * scale);
            int y = floor(start.y() + (finalGoal.y() - start.y()) * scale);
            this.segmentGoalCandidate = new NavigationCell(x, y, z);
            this.segmentCompletesFinalGoal = false;
        } else {
            this.segmentGoalCandidate = finalGoal;
            this.segmentCompletesFinalGoal = true;
        }

        int localMinY = Math.max(1, start.y() - LOCAL_VERTICAL_MARGIN);
        int localMaxY = Math.min(254, start.y() + LOCAL_VERTICAL_MARGIN);
        this.localBounds = new Bounds(start.x() - localRadius, localMinY,
            start.z() - localRadius, start.x() + localRadius, localMaxY,
            start.z() + localRadius);

        int margin = Math.max(4, corridorMargin);
        int minX = Math.max(start.x() - horizontalRadius,
            Math.min(start.x(), segmentGoalCandidate.x()) - margin);
        int maxX = Math.min(start.x() + horizontalRadius,
            Math.max(start.x(), segmentGoalCandidate.x()) + margin);
        int minZ = Math.max(start.z() - horizontalRadius,
            Math.min(start.z(), segmentGoalCandidate.z()) - margin);
        int maxZ = Math.min(start.z() + horizontalRadius,
            Math.max(start.z(), segmentGoalCandidate.z()) + margin);
        int yMargin = Math.min(verticalRadius, FULL_VERTICAL_MARGIN);
        int minY = Math.max(1,
            Math.min(start.y(), segmentGoalCandidate.y()) - yMargin);
        int maxY = Math.min(254,
            Math.max(start.y(), segmentGoalCandidate.y()) + yMargin);
        this.fullBounds = new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        this.localBuilder = localBounds.builder();
        this.fullBuilder = fullBounds.builder();
    }

    public void tick(int maximumCells) {
        int remaining = Math.max(1, maximumCells);
        while (remaining > 0 && localCursor < localBounds.volume()) {
            sample(localBuilder, localBounds, localCursor++);
            remaining--;
        }
        if (localCursor >= localBounds.volume()) status = "FULL_CAPTURE";
        while (remaining > 0 && fullCursor < fullBounds.volume()) {
            sample(fullBuilder, fullBounds, fullCursor++);
            remaining--;
        }
        if (fullCursor >= fullBounds.volume()) status = "READY";
    }

    private void sample(ImmutableNavigationGrid.Builder builder,
                        Bounds bounds, int cursor) {
        NavigationCell cell = bounds.cell(cursor);
        builder.setStandable(cell.x(), cell.y(), cell.z(),
            worldGrid.isStandable(cell.x(), cell.y(), cell.z()));
    }

    public NavigationPlannerWorker.PlanRequest takeLocalRequest() {
        if (localRequestTaken || localCursor < localBounds.volume()) return null;
        localRequestTaken = true;
        ImmutableNavigationGrid grid = localBuilder.build();
        NavigationCell localStart = grid.nearestStandable(start.x(), start.y(),
            start.z(), 1, 2);
        NavigationCell candidate = localTargetCandidate();
        NavigationCell localGoal = grid.nearestStandable(candidate.x(), candidate.y(),
            candidate.z(), 2, 2);
        if (localStart == null || localGoal == null || localStart.equals(localGoal)) return null;
        boolean reachesFinal = segmentCompletesFinalGoal && localGoal.equals(finalGoal);
        return new NavigationPlannerWorker.PlanRequest(localRequestId,
            waypointRevision, true, reachesFinal, grid, localStart, localGoal,
            localRadius + 1, Math.min(verticalRadius, 4),
            Math.min(maximumExpandedNodes, 1536), heuristicWeight,
            grid.sampledCells());
    }

    public NavigationPlannerWorker.PlanRequest takeFullRequest() {
        if (fullRequestTaken || fullCursor < fullBounds.volume()) return null;
        fullRequestTaken = true;
        ImmutableNavigationGrid grid = fullBuilder.build();
        NavigationCell fullStart = grid.nearestStandable(start.x(), start.y(),
            start.z(), 1, 2);
        NavigationCell fullGoal = grid.nearestStandable(segmentGoalCandidate.x(),
            segmentGoalCandidate.y(), segmentGoalCandidate.z(), 3, 3);
        if (fullStart == null || fullGoal == null || fullStart.equals(fullGoal)) return null;
        boolean complete = segmentCompletesFinalGoal
            && fullGoal.horizontalManhattan(finalGoal) <= 1
            && Math.abs(fullGoal.y() - finalGoal.y()) <= 1;
        return new NavigationPlannerWorker.PlanRequest(fullRequestId,
            waypointRevision, false, complete, grid, fullStart, fullGoal,
            horizontalRadius, verticalRadius, maximumExpandedNodes,
            heuristicWeight, grid.sampledCells());
    }

    private NavigationCell localTargetCandidate() {
        double dx = segmentGoalCandidate.x() - start.x();
        double dz = segmentGoalCandidate.z() - start.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= localRadius - 1D) return segmentGoalCandidate;
        double scale = (localRadius - 1D) / Math.max(0.001D, distance);
        int x = floor(start.x() + dx * scale);
        int z = floor(start.z() + dz * scale);
        int y = floor(start.y() + (segmentGoalCandidate.y() - start.y()) * scale);
        return new NavigationCell(x, y, z);
    }

    private static int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    public long generation() { return generation; }
    public long waypointRevision() { return waypointRevision; }
    public NavigationCell start() { return start; }
    public NavigationCell finalGoal() { return finalGoal; }
    public NavigationCell segmentGoalCandidate() { return segmentGoalCandidate; }
    public boolean segmentCompletesFinalGoal() { return segmentCompletesFinalGoal; }
    public boolean localReady() { return localCursor >= localBounds.volume(); }
    public boolean fullReady() { return fullCursor >= fullBounds.volume(); }
    public int capturedCells() { return localCursor + fullCursor; }
    public int totalCells() { return localBounds.volume() + fullBounds.volume(); }
    public int progressPercent() {
        return totalCells() == 0 ? 100 : Math.min(100,
            (int)((long)capturedCells() * 100L / (long)totalCells()));
    }
    public String status() { return status; }

    private static final class Bounds {
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;
        final int sizeX;
        final int sizeY;
        final int sizeZ;

        Bounds(int minX, int minY, int minZ,
               int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.sizeX = maxX - minX + 1;
            this.sizeY = maxY - minY + 1;
            this.sizeZ = maxZ - minZ + 1;
        }

        int volume() { return sizeX * sizeY * sizeZ; }

        NavigationCell cell(int cursor) {
            int x = cursor % sizeX;
            int remaining = cursor / sizeX;
            int z = remaining % sizeZ;
            int y = remaining / sizeZ;
            return new NavigationCell(minX + x, minY + y, minZ + z);
        }

        ImmutableNavigationGrid.Builder builder() {
            return new ImmutableNavigationGrid.Builder(minX, minY, minZ,
                maxX, maxY, maxZ);
        }
    }
}
