package dev.fivesaw.sawbot.forge.navigation;

import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.navigation.NavigationGrid;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.forge.sensors.BlockSemanticClassifier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/** Client-thread-only world adapter with a bounded per-plan standability cache. */
public final class WorldNavigationGrid implements NavigationGrid {
    private static final int MAX_CACHE_ENTRIES = 16384;

    private final World world;
    private final BlockSemanticClassifier classifier;
    private final Map<NavigationCell, Boolean> standableCache = new HashMap<NavigationCell, Boolean>();
    private int worldReads;

    public WorldNavigationGrid(World world) {
        if (world == null) throw new IllegalArgumentException("world");
        this.world = world;
        this.classifier = new BlockSemanticClassifier();
    }

    @Override public boolean isStandable(int x, int y, int z) {
        if (y < 1 || y > 254) return false;
        NavigationCell key = new NavigationCell(x, y, z);
        Boolean cached = standableCache.get(key);
        if (cached != null) return cached.booleanValue();
        boolean value = calculateStandable(x, y, z);
        if (standableCache.size() < MAX_CACHE_ENTRIES) {
            standableCache.put(key, Boolean.valueOf(value));
        }
        return value;
    }

    private boolean calculateStandable(int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = new BlockPos(x, y + 1, z);
        BlockPos support = new BlockPos(x, y - 1, z);
        if (!world.isBlockLoaded(feet) || !world.isBlockLoaded(head) || !world.isBlockLoaded(support)) {
            return false;
        }
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

    public NavigationCell nearestStandable(int x, int y, int z, int horizontalRadius, int verticalRadius) {
        if (isStandable(x, y, z)) return new NavigationCell(x, y, z);
        int maxHorizontal = Math.max(0, horizontalRadius);
        int maxVertical = Math.max(0, verticalRadius);
        for (int radius = 0; radius <= maxHorizontal; radius++) {
            for (int dy = 0; dy <= maxVertical; dy++) {
                int[] yOffsets = dy == 0 ? new int[]{0} : new int[]{dy, -dy};
                for (int yOffset : yOffsets) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                            int candidateY = y + yOffset;
                            if (isStandable(x + dx, candidateY, z + dz)) {
                                return new NavigationCell(x + dx, candidateY, z + dz);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public int cacheSize() { return standableCache.size(); }
    public int worldReads() { return worldReads; }
}
