package dev.fivesaw.sawbot.forge.inspection;

import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;

/** Immutable decoded view of one local-terrain cell selected by the crosshair. */
public final class BlockInspection {
    private final int worldX;
    private final int worldY;
    private final int worldZ;
    private final int rightOffset;
    private final int upOffset;
    private final int forwardOffset;
    private final int terrainIndex;
    private final int blockStateId;
    private final BlockSemanticCategory category;
    private final short flags;
    private final int collisionHeightClass;
    private final boolean insideTensor;

    public BlockInspection(int worldX, int worldY, int worldZ,
                           int rightOffset, int upOffset, int forwardOffset,
                           int terrainIndex, int blockStateId,
                           BlockSemanticCategory category, short flags,
                           int collisionHeightClass, boolean insideTensor) {
        if (category == null) throw new IllegalArgumentException("category");
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.rightOffset = rightOffset;
        this.upOffset = upOffset;
        this.forwardOffset = forwardOffset;
        this.terrainIndex = terrainIndex;
        this.blockStateId = blockStateId;
        this.category = category;
        this.flags = flags;
        this.collisionHeightClass = collisionHeightClass;
        this.insideTensor = insideTensor;
    }

    public static BlockInspection outside(int worldX, int worldY, int worldZ,
                                          int rightOffset, int upOffset, int forwardOffset) {
        return new BlockInspection(worldX, worldY, worldZ, rightOffset, upOffset, forwardOffset,
            -1, 0, BlockSemanticCategory.UNKNOWN, (short)0, 0, false);
    }

    public int worldX() { return worldX; }
    public int worldY() { return worldY; }
    public int worldZ() { return worldZ; }
    public int rightOffset() { return rightOffset; }
    public int upOffset() { return upOffset; }
    public int forwardOffset() { return forwardOffset; }
    public int terrainIndex() { return terrainIndex; }
    public int blockStateId() { return blockStateId; }
    public BlockSemanticCategory category() { return category; }
    public short flags() { return flags; }
    public int collisionHeightClass() { return collisionHeightClass; }
    public boolean insideTensor() { return insideTensor; }
}
