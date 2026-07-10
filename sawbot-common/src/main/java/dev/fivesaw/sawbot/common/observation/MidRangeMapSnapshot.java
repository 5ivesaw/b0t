package dev.fivesaw.sawbot.common.observation;

import java.util.Arrays;

public final class MidRangeMapSnapshot {
    public static final int SIZE = 33;
    public static final int COLUMN_COUNT = SIZE * SIZE;
    public static final short UNKNOWN_HEIGHT = Short.MIN_VALUE;
    public static final short FLAG_VOID = 1 << 0;
    public static final short FLAG_OBSTRUCTION = 1 << 1;
    public static final short FLAG_SAFE_LANDING = 1 << 2;
    public static final short FLAG_NARROW_BRIDGE = 1 << 3;
    public static final short FLAG_PLATFORM = 1 << 4;
    public static final short FLAG_LOADED = 1 << 5;
    public static final short FLAG_UNKNOWN = 1 << 6;

    private final int originX;
    private final int originY;
    private final int originZ;
    private final byte facingQuadrant;
    private final short[] relativeSurfaceY;
    private final short[] flags;
    private final short[] ageTicks;
    private final int rowsUpdatedThisTick;

    public MidRangeMapSnapshot(int originX, int originY, int originZ, byte facingQuadrant,
                               short[] relativeSurfaceY, short[] flags, short[] ageTicks,
                               int rowsUpdatedThisTick) {
        if (relativeSurfaceY.length != COLUMN_COUNT || flags.length != COLUMN_COUNT || ageTicks.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("mid-range array length");
        }
        if (facingQuadrant < 0 || facingQuadrant > 3) throw new IllegalArgumentException("facingQuadrant");
        if (rowsUpdatedThisTick < 0 || rowsUpdatedThisTick > SIZE) throw new IllegalArgumentException("rowsUpdatedThisTick");
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.facingQuadrant = facingQuadrant;
        this.relativeSurfaceY = Arrays.copyOf(relativeSurfaceY, COLUMN_COUNT);
        this.flags = Arrays.copyOf(flags, COLUMN_COUNT);
        this.ageTicks = Arrays.copyOf(ageTicks, COLUMN_COUNT);
        this.rowsUpdatedThisTick = rowsUpdatedThisTick;
    }

    public static int index(int rightOffset, int forwardOffset) {
        if (rightOffset < -16 || rightOffset > 16 || forwardOffset < -16 || forwardOffset > 16) throw new IndexOutOfBoundsException("map offset");
        return (forwardOffset + 16) * SIZE + (rightOffset + 16);
    }

    public int originX() { return originX; }
    public int originY() { return originY; }
    public int originZ() { return originZ; }
    public byte facingQuadrant() { return facingQuadrant; }
    public short relativeSurfaceYAt(int index) { return relativeSurfaceY[index]; }
    public short flagsAt(int index) { return flags[index]; }
    public short ageTicksAt(int index) { return ageTicks[index]; }
    public short[] relativeSurfaceY() { return Arrays.copyOf(relativeSurfaceY, relativeSurfaceY.length); }
    public short[] flags() { return Arrays.copyOf(flags, flags.length); }
    public short[] ageTicks() { return Arrays.copyOf(ageTicks, ageTicks.length); }
    public int rowsUpdatedThisTick() { return rowsUpdatedThisTick; }
}
