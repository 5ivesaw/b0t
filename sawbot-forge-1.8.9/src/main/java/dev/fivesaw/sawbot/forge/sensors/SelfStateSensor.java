package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.SelfState;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class SelfStateSensor {
    private double previousMotionX;
    private double previousMotionY;
    private double previousMotionZ;
    private boolean hasPreviousVelocity;
    private int airborneTicks;

    public SelfState capture(EntityPlayerSP player, World world) {
        if (player.onGround) airborneTicks = 0;
        else if (airborneTicks < 65535) airborneTicks++;

        float rightVelocity = EgocentricTransform.right(player.motionX, player.motionZ, player.rotationYaw);
        float forwardVelocity = EgocentricTransform.forward(player.motionX, player.motionZ, player.rotationYaw);
        float accelerationRight = hasPreviousVelocity
            ? EgocentricTransform.right(player.motionX - previousMotionX, player.motionZ - previousMotionZ, player.rotationYaw) : 0f;
        float accelerationForward = hasPreviousVelocity
            ? EgocentricTransform.forward(player.motionX - previousMotionX, player.motionZ - previousMotionZ, player.rotationYaw) : 0f;
        float accelerationUp = hasPreviousVelocity ? (float)(player.motionY - previousMotionY) : 0f;
        previousMotionX = player.motionX;
        previousMotionY = player.motionY;
        previousMotionZ = player.motionZ;
        hasPreviousVelocity = true;

        AxisAlignedBB box = player.getEntityBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double halfWidth = Math.max(0.05D, (box.maxX - box.minX) * 0.42D);
        double radians = Math.toRadians(player.rotationYaw);
        double rightX = -Math.cos(radians);
        double rightZ = -Math.sin(radians);
        float leftSupport = supportDistance(world, centerX - rightX * halfWidth, centerZ - rightZ * halfWidth, box.minY, 8);
        float centerSupport = supportDistance(world, centerX, centerZ, box.minY, 8);
        float rightSupport = supportDistance(world, centerX + rightX * halfWidth, centerZ + rightZ * halfWidth, box.minY, 8);
        float voidDistance = supportDistance(world, centerX, centerZ, box.minY, 64);
        Collection<PotionEffect> effects = player.getActivePotionEffects();

        return new SelfState(player.getHealth(), player.getAbsorptionAmount(), player.getFoodStats().getFoodLevel(), player.getTotalArmorValue(),
            player.posX, player.posY, player.posZ, rightVelocity, (float)player.motionY, forwardVelocity,
            accelerationRight, accelerationUp, accelerationForward, normalizeYaw(player.rotationYaw),
            MathHelper.clamp_float(player.rotationPitch, -90f, 90f), player.fallDistance,
            player.onGround, player.isCollidedHorizontally, player.isCollidedVertically,
            player.isInWater() || player.isInLava(), player.isOnLadder(), player.isEntityInsideOpaqueBlock(),
            player.isSprinting(), player.isSneaking(), player.isUsingItem(), airborneTicks, Math.max(0, player.hurtTime),
            player.inventory.currentItem, leftSupport, centerSupport, rightSupport, voidDistance,
            effects == null ? 0 : effects.size());
    }

    private static float supportDistance(World world, double x, double z, double feetY, int maxDistance) {
        double epsilon = 0.025D;
        AxisAlignedBB probe = new AxisAlignedBB(x - epsilon, Math.max(0.0D, feetY - maxDistance), z - epsilon,
            x + epsilon, feetY + 0.01D, z + epsilon);
        List<?> boxes = world.getCollisionBoxes(probe);
        double highestTop = Double.NEGATIVE_INFINITY;
        for (Object value : boxes) {
            if (!(value instanceof AxisAlignedBB)) continue;
            AxisAlignedBB collision = (AxisAlignedBB)value;
            if (collision.maxY <= feetY + 0.011D && collision.maxY > highestTop) highestTop = collision.maxY;
        }
        if (highestTop == Double.NEGATIVE_INFINITY) return (float)maxDistance;
        return (float)Math.min(maxDistance, Math.max(0.0D, feetY - highestTop));
    }

    private static float normalizeYaw(float yaw) {
        float value = yaw % 360f;
        if (value >= 180f) value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }
}
