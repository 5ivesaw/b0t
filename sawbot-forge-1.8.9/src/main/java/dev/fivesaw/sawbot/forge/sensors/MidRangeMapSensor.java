package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.MidRangeMapSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Incremental 33x33 egocentric map backed by a bounded world-coordinate cache.
 * Moving or rotating the player reprojects existing samples instead of discarding them.
 */
public final class MidRangeMapSensor {
    private static final int RADIUS = 16;
    private static final int ROWS_PER_TICK = 2;
    private static final int MAX_CACHE_COLUMNS = 4096;
    private static final int SEARCH_UP = 8;
    private static final int SEARCH_DOWN = 24;
    private static final int HINT_RADIUS = 4;
    private static final int FULL_RESCAN_INTERVAL_TICKS = 100;

    private final Map<Long, ColumnSample> cache = new LinkedHashMap<Long, ColumnSample>(512, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, ColumnSample> eldest) {
            return size() > MAX_CACHE_COLUMNS;
        }
    };
    private final List<AxisAlignedBB> collisionScratch = new ArrayList<AxisAlignedBB>(4);
    private int rowCursor;
    private int originX;
    private int originY;
    private int originZ;
    private byte facing;
    private long lastClientTick;
    private boolean initialized;

    public void update(EntityPlayerSP player, World world, long clientTick) {
        originX = MathHelper.floor_double(player.posX);
        originY = MathHelper.floor_double(player.getEntityBoundingBox().minY);
        originZ = MathHelper.floor_double(player.posZ);
        facing = EgocentricTransform.quadrant(player.rotationYaw);
        lastClientTick = clientTick;
        initialized = true;

        for (int row = 0; row < ROWS_PER_TICK; row++) {
            int forward = (rowCursor % MidRangeMapSnapshot.SIZE) - RADIUS;
            rowCursor = (rowCursor + 1) % MidRangeMapSnapshot.SIZE;
            for (int right = -RADIUS; right <= RADIUS; right++) {
                int worldX = originX + EgocentricTransform.worldDx(right, forward, facing);
                int worldZ = originZ + EgocentricTransform.worldDz(right, forward, facing);
                Long key = Long.valueOf(columnKey(worldX, worldZ));
                ColumnSample previous = cache.get(key);
                cache.put(key, scanColumn(world, worldX, worldZ, originY, clientTick, previous));
            }
        }
    }

    public MidRangeMapSnapshot snapshot() {
        short[] heights = new short[MidRangeMapSnapshot.COLUMN_COUNT];
        short[] flags = new short[MidRangeMapSnapshot.COLUMN_COUNT];
        short[] ages = new short[MidRangeMapSnapshot.COLUMN_COUNT];
        for (int forward = -RADIUS; forward <= RADIUS; forward++) {
            for (int right = -RADIUS; right <= RADIUS; right++) {
                int index = MidRangeMapSnapshot.index(right, forward);
                int worldX = originX + EgocentricTransform.worldDx(right, forward, facing);
                int worldZ = originZ + EgocentricTransform.worldDz(right, forward, facing);
                ColumnSample sample = cache.get(Long.valueOf(columnKey(worldX, worldZ)));
                if (!initialized || sample == null) {
                    heights[index] = MidRangeMapSnapshot.UNKNOWN_HEIGHT;
                    flags[index] = MidRangeMapSnapshot.FLAG_UNKNOWN;
                    ages[index] = Short.MAX_VALUE;
                    continue;
                }
                heights[index] = Double.isNaN(sample.surfaceY)
                    ? MidRangeMapSnapshot.UNKNOWN_HEIGHT
                    : clampShort((int)Math.round(sample.surfaceY - originY));
                flags[index] = sample.flags;
                long age = Math.max(0L, lastClientTick - sample.fullScanTick);
                ages[index] = (short)Math.min(Short.MAX_VALUE, age);
            }
        }
        markShapeHints(heights, flags);
        return new MidRangeMapSnapshot(originX, originY, originZ, facing, heights, flags, ages, ROWS_PER_TICK);
    }

    private ColumnSample scanColumn(World world, int worldX, int worldZ, int referenceY,
                                    long clientTick, ColumnSample previous) {
        BlockPos loadProbe = new BlockPos(worldX, referenceY, worldZ);
        if (!world.isBlockLoaded(loadProbe)) {
            return ColumnSample.unknown(clientTick);
        }

        int fullTop = Math.min(255, referenceY + SEARCH_UP);
        int fullBottom = Math.max(0, referenceY - SEARCH_DOWN);
        Surface surface = null;
        boolean fullScan = previous == null
            || !previous.hasSurface()
            || clientTick - previous.fullScanTick >= FULL_RESCAN_INTERVAL_TICKS;

        // Most terrain is static. Revalidate a small vertical band around a recent
        // support surface and periodically force a complete bounded scan. If the hinted
        // surface disappears, immediately fall back to the complete scan.
        if (!fullScan) {
            int hintTop = Math.min(fullTop, previous.supportBlockY + HINT_RADIUS);
            int hintBottom = Math.max(fullBottom, previous.supportBlockY - HINT_RADIUS);
            surface = findSurface(world, worldX, worldZ, hintTop, hintBottom);
            if (surface == null) fullScan = true;
        }
        if (fullScan) {
            surface = findSurface(world, worldX, worldZ, fullTop, fullBottom);
        }

        short valueFlags = MidRangeMapSnapshot.FLAG_LOADED;
        if (surface == null) {
            return new ColumnSample(Double.NaN, Integer.MIN_VALUE,
                (short)(valueFlags | MidRangeMapSnapshot.FLAG_VOID), clientTick, clientTick);
        }

        if (!isHazardSurface(surface.block)) {
            valueFlags |= MidRangeMapSnapshot.FLAG_SAFE_LANDING;
        }
        AxisAlignedBB clearance = new AxisAlignedBB(worldX + 0.08D, surface.topY, worldZ + 0.08D,
            worldX + 0.92D, surface.topY + 1.8D, worldZ + 0.92D);
        if (hasBlockCollision(world, clearance, surface.blockY, Math.min(255, surface.blockY + 3))) {
            valueFlags |= MidRangeMapSnapshot.FLAG_OBSTRUCTION;
        }
        long fullScanTick = fullScan ? clientTick : previous.fullScanTick;
        return new ColumnSample(surface.topY, surface.blockY, valueFlags, clientTick, fullScanTick);
    }

    private Surface findSurface(World world, int worldX, int worldZ, int top, int bottom) {
        if (top < bottom) return null;
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(worldX, y, worldZ);
            IBlockState state = world.getBlockState(pos);
            collectCollisionBoxes(world, pos, state, collisionScratch);
            if (collisionScratch.isEmpty()) continue;
            double highestTop = Double.NEGATIVE_INFINITY;
            for (AxisAlignedBB box : collisionScratch) {
                if (box.maxY > highestTop) highestTop = box.maxY;
            }
            if (highestTop != Double.NEGATIVE_INFINITY) {
                return new Surface(highestTop, y, state.getBlock());
            }
        }
        return null;
    }

    private static boolean isHazardSurface(Block block) {
        return block == Blocks.cactus || block == Blocks.lava || block == Blocks.flowing_lava || block == Blocks.fire;
    }

    private boolean hasBlockCollision(World world, AxisAlignedBB mask, int minY, int maxY) {
        int x = MathHelper.floor_double((mask.minX + mask.maxX) * 0.5D);
        int z = MathHelper.floor_double((mask.minZ + mask.maxZ) * 0.5D);
        for (int y = minY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            collisionScratch.clear();
            state.getBlock().addCollisionBoxesToList(world, pos, state, mask, collisionScratch, (Entity)null);
            if (!collisionScratch.isEmpty()) return true;
        }
        return false;
    }

    private static void collectCollisionBoxes(World world, BlockPos pos, IBlockState state, List<AxisAlignedBB> target) {
        target.clear();
        AxisAlignedBB mask = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1.0D, pos.getY() + 1.5D, pos.getZ() + 1.0D);
        Block block = state.getBlock();
        block.addCollisionBoxesToList(world, pos, state, mask, target, (Entity)null);
    }

    private static void markShapeHints(short[] heights, short[] flags) {
        for (int forward = -15; forward <= 15; forward++) {
            for (int right = -15; right <= 15; right++) {
                int index = MidRangeMapSnapshot.index(right, forward);
                short value = (short)(flags[index] & ~(MidRangeMapSnapshot.FLAG_NARROW_BRIDGE | MidRangeMapSnapshot.FLAG_PLATFORM));
                if ((value & MidRangeMapSnapshot.FLAG_SAFE_LANDING) != 0 && heights[index] != MidRangeMapSnapshot.UNKNOWN_HEIGHT) {
                    int neighbours = 0;
                    if (compatible(heights, flags, right + 1, forward, heights[index])) neighbours++;
                    if (compatible(heights, flags, right - 1, forward, heights[index])) neighbours++;
                    if (compatible(heights, flags, right, forward + 1, heights[index])) neighbours++;
                    if (compatible(heights, flags, right, forward - 1, heights[index])) neighbours++;
                    if (neighbours <= 2) value |= MidRangeMapSnapshot.FLAG_NARROW_BRIDGE;
                    else if (neighbours == 4) value |= MidRangeMapSnapshot.FLAG_PLATFORM;
                }
                flags[index] = value;
            }
        }
    }

    private static boolean compatible(short[] heights, short[] flags, int right, int forward, short height) {
        int index = MidRangeMapSnapshot.index(right, forward);
        return (flags[index] & MidRangeMapSnapshot.FLAG_SAFE_LANDING) != 0
            && heights[index] != MidRangeMapSnapshot.UNKNOWN_HEIGHT
            && Math.abs(heights[index] - height) <= 1;
    }

    private static long columnKey(int x, int z) {
        return ((long)x << 32) ^ (z & 0xffffffffL);
    }

    private static short clampShort(int value) {
        return (short)Math.max(Short.MIN_VALUE + 1, Math.min(Short.MAX_VALUE, value));
    }

    private static final class Surface {
        final double topY;
        final int blockY;
        final Block block;

        Surface(double topY, int blockY, Block block) {
            this.topY = topY;
            this.blockY = blockY;
            this.block = block;
        }
    }

    private static final class ColumnSample {
        final double surfaceY;
        final int supportBlockY;
        final short flags;
        final long clientTick;
        final long fullScanTick;

        ColumnSample(double surfaceY, int supportBlockY, short flags, long clientTick, long fullScanTick) {
            this.surfaceY = surfaceY;
            this.supportBlockY = supportBlockY;
            this.flags = flags;
            this.clientTick = clientTick;
            this.fullScanTick = fullScanTick;
        }

        boolean hasSurface() {
            return !Double.isNaN(surfaceY) && supportBlockY != Integer.MIN_VALUE;
        }

        static ColumnSample unknown(long clientTick) {
            return new ColumnSample(Double.NaN, Integer.MIN_VALUE,
                MidRangeMapSnapshot.FLAG_UNKNOWN, clientTick, clientTick);
        }
    }
}
