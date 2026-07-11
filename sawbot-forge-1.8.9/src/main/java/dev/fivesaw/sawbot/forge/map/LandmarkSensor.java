package dev.fivesaw.sawbot.forge.map;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkSetSnapshot;
import dev.fivesaw.sawbot.common.observation.LandmarkType;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/** Universal landmarks plus the explicit user waypoint used by learned navigation. */
public final class LandmarkSensor {
    private static final long RESOLVE_INTERVAL_TICKS = 100L;
    private final NavigationWaypointController navigationWaypoint;
    private int cachedSpawnX = Integer.MIN_VALUE;
    private int cachedSpawnY = Integer.MIN_VALUE;
    private int cachedSpawnZ = Integer.MIN_VALUE;
    private double cachedSurfaceY;
    private float cachedConfidence = 0.25F;
    private long lastResolveTick = Long.MIN_VALUE;

    public LandmarkSensor() { this(new NavigationWaypointController(Minecraft.getMinecraft())); }

    public LandmarkSensor(NavigationWaypointController navigationWaypoint) {
        if (navigationWaypoint == null) throw new IllegalArgumentException("navigationWaypoint");
        this.navigationWaypoint = navigationWaypoint;
    }

    public LandmarkSetSnapshot capture(EntityPlayerSP player, World world, long clientTick) {
        List<LandmarkObservation> landmarks = new ArrayList<LandmarkObservation>(2);
        landmarks.add(captureWorldSpawn(player, world, clientTick));
        LandmarkObservation user = navigationWaypoint.capture(player, world);
        if (user != null) landmarks.add(user);
        return new LandmarkSetSnapshot(landmarks);
    }

    private LandmarkObservation captureWorldSpawn(EntityPlayerSP player, World world, long clientTick) {
        BlockPos spawn = world.getSpawnPoint();
        if (spawnChanged(spawn) || clientTick - lastResolveTick >= RESOLVE_INTERVAL_TICKS) {
            resolveSurface(world, spawn, clientTick);
        }
        double x = spawn.getX() + 0.5D;
        double z = spawn.getZ() + 0.5D;
        double dx = x - player.posX;
        double dy = cachedSurfaceY - player.posY;
        double dz = z - player.posZ;
        float right = EgocentricTransform.right(dx, dz, player.rotationYaw);
        float forward = EgocentricTransform.forward(dx, dz, player.rotationYaw);
        float distance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new LandmarkObservation(0, LandmarkType.WORLD_SPAWN,
            TeamRelation.NEUTRAL, right, (float)dy, forward, distance, distance,
            0F, 0.25F, cachedConfidence, true);
    }

    private boolean spawnChanged(BlockPos spawn) {
        return spawn.getX() != cachedSpawnX || spawn.getY() != cachedSpawnY || spawn.getZ() != cachedSpawnZ;
    }

    private void resolveSurface(World world, BlockPos spawn, long clientTick) {
        cachedSpawnX = spawn.getX();
        cachedSpawnY = spawn.getY();
        cachedSpawnZ = spawn.getZ();
        cachedSurfaceY = spawn.getY();
        cachedConfidence = 0.25F;
        lastResolveTick = clientTick;
        if (!world.isBlockLoaded(spawn)) return;

        double bestY = cachedSurfaceY;
        double bestDistance = Double.POSITIVE_INFINITY;
        boolean found = false;
        for (int y = 0; y <= 255; y++) {
            BlockPos blockPos = new BlockPos(spawn.getX(), y, spawn.getZ());
            IBlockState state = world.getBlockState(blockPos);
            AxisAlignedBB collision = state.getBlock().getCollisionBoundingBox(world, blockPos, state);
            if (collision == null || collision.maxY <= collision.minY) continue;
            BlockPos above = new BlockPos(spawn.getX(), y + 1, spawn.getZ());
            IBlockState aboveState = world.getBlockState(above);
            AxisAlignedBB aboveCollision = aboveState.getBlock().getCollisionBoundingBox(world, above, aboveState);
            if (aboveCollision != null) continue;
            double standY = collision.maxY;
            double distance = Math.abs(standY - spawn.getY());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestY = standY;
                found = true;
            }
        }
        if (found) {
            cachedSurfaceY = bestY;
            cachedConfidence = 1F;
        }
    }
}
