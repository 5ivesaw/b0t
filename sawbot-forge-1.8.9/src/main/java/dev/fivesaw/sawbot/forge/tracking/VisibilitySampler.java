package dev.fivesaw.sawbot.forge.tracking;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Recomputes visibility from current world geometry on every observation capture.
 * Multiple target samples prevent a single blocked centre ray from becoming a
 * sticky or misleading LOS result near wall edges.
 */
public final class VisibilitySampler {
    private static final double INSET = 0.08D;

    public boolean hasLineOfSight(EntityPlayerSP player, Entity entity, World world) {
        if (player == null || entity == null || world == null || entity.isDead) return false;
        Vec3 eye = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double minX = box.minX + Math.min(INSET, Math.max(0D, (box.maxX - box.minX) * 0.25D));
        double maxX = box.maxX - Math.min(INSET, Math.max(0D, (box.maxX - box.minX) * 0.25D));
        double minY = box.minY + Math.min(INSET, Math.max(0D, (box.maxY - box.minY) * 0.10D));
        double maxY = box.maxY - Math.min(INSET, Math.max(0D, (box.maxY - box.minY) * 0.10D));
        double minZ = box.minZ + Math.min(INSET, Math.max(0D, (box.maxZ - box.minZ) * 0.25D));
        double maxZ = box.maxZ - Math.min(INSET, Math.max(0D, (box.maxZ - box.minZ) * 0.25D));

        return clear(world, eye, new Vec3(centerX, centerY, centerZ))
            || clear(world, eye, new Vec3(centerX, maxY, centerZ))
            || clear(world, eye, new Vec3(centerX, minY, centerZ))
            || clear(world, eye, new Vec3(minX, centerY, minZ))
            || clear(world, eye, new Vec3(minX, centerY, maxZ))
            || clear(world, eye, new Vec3(maxX, centerY, minZ))
            || clear(world, eye, new Vec3(maxX, centerY, maxZ));
    }

    private static boolean clear(World world, Vec3 eye, Vec3 target) {
        MovingObjectPosition hit = world.rayTraceBlocks(eye, target, false, true, false);
        return hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS;
    }
}
