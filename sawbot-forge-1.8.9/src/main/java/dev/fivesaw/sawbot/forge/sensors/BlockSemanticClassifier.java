package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/** Client-thread-only block classifier with cached static semantics and exact collision lists. */
public final class BlockSemanticClassifier {
    private final Map<Integer, StaticClassification> cache = new HashMap<Integer, StaticClassification>();
    private final List<AxisAlignedBB> collisionScratch = new ArrayList<AxisAlignedBB>(4);

    public CellClassification classify(World world, BlockPos position, IBlockState state, boolean recentlyChanged) {
        int stateId = Block.getStateId(state);
        StaticClassification base = cache.get(Integer.valueOf(stateId));
        if (base == null) {
            base = classifyStatic(state);
            cache.put(Integer.valueOf(stateId), base);
        }
        Block block = state.getBlock();
        boolean replaceable = block.isReplaceable(world, position) || block.getMaterial().isReplaceable();
        collectCollisionBoxes(world, position, state);
        short flags = base.flags;
        if (replaceable) flags |= LocalTerrainSnapshot.FLAG_REPLACEABLE;
        else flags &= ~LocalTerrainSnapshot.FLAG_REPLACEABLE;
        if (!collisionScratch.isEmpty() && !base.hazard && !base.liquid) flags |= LocalTerrainSnapshot.FLAG_SAFE_SUPPORT;
        if (!collisionScratch.isEmpty() && !replaceable) flags |= LocalTerrainSnapshot.FLAG_VALID_PLACEMENT_SUPPORT;
        if (recentlyChanged) flags |= LocalTerrainSnapshot.FLAG_RECENTLY_CHANGED;
        flags |= LocalTerrainSnapshot.FLAG_LOADED;
        return new CellClassification((short)stateId, (byte)base.category.ordinal(), flags, collisionClass(collisionScratch));
    }

    public CellClassification unknown() {
        return new CellClassification((short)0, (byte)BlockSemanticCategory.UNKNOWN.ordinal(),
            LocalTerrainSnapshot.FLAG_UNKNOWN, (byte)0);
    }

    private StaticClassification classifyStatic(IBlockState state) {
        Block block = state.getBlock();
        boolean air = block == Blocks.air;
        boolean liquid = block instanceof BlockLiquid || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.lava || block == Blocks.flowing_lava;
        boolean hazard = block == Blocks.lava || block == Blocks.flowing_lava || block == Blocks.fire || block == Blocks.cactus;
        boolean climbable = block == Blocks.ladder || block == Blocks.vine;
        boolean bed = block == Blocks.bed;
        boolean interactable = block instanceof BlockContainer || block == Blocks.crafting_table || block == Blocks.enchanting_table || block == Blocks.anvil || block == Blocks.lever || block == Blocks.stone_button || block == Blocks.wooden_button;
        boolean materialReplaceable = air || block.getMaterial().isReplaceable();
        boolean solid = !air && !liquid && block.getMaterial().blocksMovement();
        boolean full = solid && block.isFullBlock();
        boolean partial = solid && !full;
        BlockSemanticCategory category;
        if (air) category = BlockSemanticCategory.AIR;
        else if (bed) category = BlockSemanticCategory.BED;
        else if (hazard) category = BlockSemanticCategory.HAZARD;
        else if (liquid) category = BlockSemanticCategory.LIQUID;
        else if (climbable) category = BlockSemanticCategory.CLIMBABLE;
        else if (block instanceof BlockContainer) category = BlockSemanticCategory.CONTAINER;
        else if (interactable) category = BlockSemanticCategory.INTERACTABLE;
        else if (full) category = BlockSemanticCategory.SOLID;
        else if (partial) category = BlockSemanticCategory.PARTIAL;
        else if (materialReplaceable) category = BlockSemanticCategory.PLANT;
        else category = BlockSemanticCategory.DECORATION;
        short flags = 0;
        if (solid) flags |= LocalTerrainSnapshot.FLAG_SOLID;
        if (full) flags |= LocalTerrainSnapshot.FLAG_FULL_BLOCK;
        if (partial) flags |= LocalTerrainSnapshot.FLAG_PARTIAL_BLOCK;
        if (materialReplaceable) flags |= LocalTerrainSnapshot.FLAG_REPLACEABLE;
        if (liquid) flags |= LocalTerrainSnapshot.FLAG_LIQUID;
        if (hazard) flags |= LocalTerrainSnapshot.FLAG_HAZARD;
        if (climbable) flags |= LocalTerrainSnapshot.FLAG_CLIMBABLE;
        if (interactable) flags |= LocalTerrainSnapshot.FLAG_INTERACTABLE;
        if (bed) flags |= LocalTerrainSnapshot.FLAG_BED_COMPONENT;
        return new StaticClassification(category, flags, liquid, hazard);
    }

    private void collectCollisionBoxes(World world, BlockPos position, IBlockState state) {
        collisionScratch.clear();
        AxisAlignedBB mask = new AxisAlignedBB(position.getX(), position.getY(), position.getZ(),
            position.getX() + 1.0D, position.getY() + 1.5D, position.getZ() + 1.0D);
        state.getBlock().addCollisionBoxesToList(world, position, state, mask, collisionScratch, (Entity)null);
    }

    /** 0 none, 1 quarter, 2 half, 3 three-quarter, 4 full, 5 other, 6 compound. */
    private static byte collisionClass(List<AxisAlignedBB> boxes) {
        if (boxes.isEmpty()) return 0;
        if (boxes.size() > 1) return 6;
        AxisAlignedBB box = boxes.get(0);
        double height = box.maxY - box.minY;
        if (height <= 0.26D) return 1;
        if (height <= 0.51D) return 2;
        if (height <= 0.76D) return 3;
        if (height >= 0.99D) return 4;
        return 5;
    }

    public static final class CellClassification {
        public final short blockStateId;
        public final byte category;
        public final short flags;
        public final byte collisionHeightClass;
        CellClassification(short blockStateId, byte category, short flags, byte collisionHeightClass) {
            this.blockStateId = blockStateId;
            this.category = category;
            this.flags = flags;
            this.collisionHeightClass = collisionHeightClass;
        }
    }

    private static final class StaticClassification {
        final BlockSemanticCategory category;
        final short flags;
        final boolean liquid;
        final boolean hazard;
        StaticClassification(BlockSemanticCategory category, short flags, boolean liquid, boolean hazard) {
            this.category = category;
            this.flags = flags;
            this.liquid = liquid;
            this.hazard = hazard;
        }
    }
}
