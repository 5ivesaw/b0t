package dev.fivesaw.sawbot.common.observation;

import java.util.Arrays;

public final class LocalTerrainSnapshot {
    public static final int WIDTH = 13;
    public static final int HEIGHT = 9;
    public static final int DEPTH = 13;
    public static final int CELL_COUNT = WIDTH * HEIGHT * DEPTH;

    public static final short FLAG_SOLID = 1 << 0;
    public static final short FLAG_FULL_BLOCK = 1 << 1;
    public static final short FLAG_PARTIAL_BLOCK = 1 << 2;
    public static final short FLAG_REPLACEABLE = 1 << 3;
    public static final short FLAG_LIQUID = 1 << 4;
    public static final short FLAG_HAZARD = 1 << 5;
    public static final short FLAG_CLIMBABLE = 1 << 6;
    public static final short FLAG_INTERACTABLE = 1 << 7;
    public static final short FLAG_BED_COMPONENT = 1 << 8;
    public static final short FLAG_SAFE_SUPPORT = 1 << 9;
    public static final short FLAG_VALID_PLACEMENT_SUPPORT = 1 << 10;
    public static final short FLAG_RECENTLY_CHANGED = 1 << 11;
    public static final short FLAG_LOADED = 1 << 12;
    public static final short FLAG_UNKNOWN = 1 << 13;

    private final int originX;
    private final int originY;
    private final int originZ;
    private final byte facingQuadrant;
    private final short[] blockStateIds;
    private final byte[] categories;
    private final short[] flags;
    private final byte[] collisionHeightClasses;
    private final int changedCellCount;

    public LocalTerrainSnapshot(int originX, int originY, int originZ, byte facingQuadrant,
                                short[] blockStateIds, byte[] categories, short[] flags,
                                byte[] collisionHeightClasses, int changedCellCount) {
        requireLength(blockStateIds, CELL_COUNT, "blockStateIds");
        requireLength(categories, CELL_COUNT, "categories");
        requireLength(flags, CELL_COUNT, "flags");
        requireLength(collisionHeightClasses, CELL_COUNT, "collisionHeightClasses");
        if (facingQuadrant < 0 || facingQuadrant > 3) throw new IllegalArgumentException("facingQuadrant");
        if (changedCellCount < 0 || changedCellCount > CELL_COUNT) throw new IllegalArgumentException("changedCellCount");
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.facingQuadrant = facingQuadrant;
        this.blockStateIds = Arrays.copyOf(blockStateIds, CELL_COUNT);
        this.categories = Arrays.copyOf(categories, CELL_COUNT);
        this.flags = Arrays.copyOf(flags, CELL_COUNT);
        this.collisionHeightClasses = Arrays.copyOf(collisionHeightClasses, CELL_COUNT);
        this.changedCellCount = changedCellCount;
    }

    private static void requireLength(Object array, int expected, String field) {
        int length;
        if (array instanceof short[]) length = ((short[]) array).length;
        else if (array instanceof byte[]) length = ((byte[]) array).length;
        else throw new IllegalArgumentException(field);
        if (length != expected) throw new IllegalArgumentException(field + " length " + length + " != " + expected);
    }

    public static int index(int rightOffset, int upOffset, int forwardOffset) {
        if (rightOffset < -6 || rightOffset > 6 || upOffset < -4 || upOffset > 4 || forwardOffset < -6 || forwardOffset > 6) {
            throw new IndexOutOfBoundsException("terrain offset");
        }
        return ((upOffset + 4) * DEPTH + (forwardOffset + 6)) * WIDTH + (rightOffset + 6);
    }

    public int originX() { return originX; }
    public int originY() { return originY; }
    public int originZ() { return originZ; }
    public byte facingQuadrant() { return facingQuadrant; }
    public short[] blockStateIds() { return Arrays.copyOf(blockStateIds, blockStateIds.length); }
    public byte[] categories() { return Arrays.copyOf(categories, categories.length); }
    public short[] flags() { return Arrays.copyOf(flags, flags.length); }
    public byte[] collisionHeightClasses() { return Arrays.copyOf(collisionHeightClasses, collisionHeightClasses.length); }
    public short blockStateIdAt(int index) { return blockStateIds[index]; }
    public byte categoryAt(int index) { return categories[index]; }
    public short flagsAt(int index) { return flags[index]; }
    public byte collisionHeightClassAt(int index) { return collisionHeightClasses[index]; }
    public int changedCellCount() { return changedCellCount; }
}
