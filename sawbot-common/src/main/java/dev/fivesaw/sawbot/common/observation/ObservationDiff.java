package dev.fivesaw.sawbot.common.observation;

/** Immutable, bounded summary of differences between two observation snapshots. */
public final class ObservationDiff {
    public static final ObservationDiff EMPTY = new ObservationDiff(0L, 0L, 0L, 0f, 0f, 0,
        0, 0, 0, 0, 0, 0L);

    private final long fromSequence;
    private final long toSequence;
    private final long clientTickDelta;
    private final float positionDistance;
    private final float yawDeltaDegrees;
    private final int terrainChangedCells;
    private final int mapChangedColumns;
    private final int entitiesAdded;
    private final int entitiesRemoved;
    private final int entitiesChanged;
    private final int inventoryChangedSlots;
    private final long validityChangedBits;

    public ObservationDiff(long fromSequence, long toSequence, long clientTickDelta,
                           float positionDistance, float yawDeltaDegrees,
                           int terrainChangedCells, int mapChangedColumns,
                           int entitiesAdded, int entitiesRemoved, int entitiesChanged,
                           int inventoryChangedSlots, long validityChangedBits) {
        if (fromSequence < 0L || toSequence < 0L || clientTickDelta < 0L) {
            throw new IllegalArgumentException("sequence/tick delta");
        }
        if (!Float.isFinite(positionDistance) || !Float.isFinite(yawDeltaDegrees)) {
            throw new IllegalArgumentException("nonfinite diff");
        }
        if (terrainChangedCells < 0 || mapChangedColumns < 0 || entitiesAdded < 0
            || entitiesRemoved < 0 || entitiesChanged < 0 || inventoryChangedSlots < 0) {
            throw new IllegalArgumentException("negative diff count");
        }
        this.fromSequence = fromSequence;
        this.toSequence = toSequence;
        this.clientTickDelta = clientTickDelta;
        this.positionDistance = positionDistance;
        this.yawDeltaDegrees = yawDeltaDegrees;
        this.terrainChangedCells = terrainChangedCells;
        this.mapChangedColumns = mapChangedColumns;
        this.entitiesAdded = entitiesAdded;
        this.entitiesRemoved = entitiesRemoved;
        this.entitiesChanged = entitiesChanged;
        this.inventoryChangedSlots = inventoryChangedSlots;
        this.validityChangedBits = validityChangedBits;
    }

    public long fromSequence() { return fromSequence; }
    public long toSequence() { return toSequence; }
    public long clientTickDelta() { return clientTickDelta; }
    public float positionDistance() { return positionDistance; }
    public float yawDeltaDegrees() { return yawDeltaDegrees; }
    public int terrainChangedCells() { return terrainChangedCells; }
    public int mapChangedColumns() { return mapChangedColumns; }
    public int entitiesAdded() { return entitiesAdded; }
    public int entitiesRemoved() { return entitiesRemoved; }
    public int entitiesChanged() { return entitiesChanged; }
    public int inventoryChangedSlots() { return inventoryChangedSlots; }
    public long validityChangedBits() { return validityChangedBits; }

    public boolean isEmpty() {
        return positionDistance == 0f && yawDeltaDegrees == 0f && terrainChangedCells == 0
            && mapChangedColumns == 0 && entitiesAdded == 0 && entitiesRemoved == 0
            && entitiesChanged == 0 && inventoryChangedSlots == 0 && validityChangedBits == 0L;
    }
}
