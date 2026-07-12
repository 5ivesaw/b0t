package dev.fivesaw.sawbot.forge.map;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkType;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/** Client-thread semantic target used by deterministic bodies and learned brains. */
public final class NavigationWaypointController {
    public static final int USER_WAYPOINT_ID = 1000;

    private final Minecraft minecraft;
    private boolean active;
    private int dimensionId;
    private double x;
    private double y;
    private double z;
    private long revision;

    public NavigationWaypointController(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    /** Sets the waypoint one block above the block currently under the crosshair. */
    public boolean setFromCrosshair() {
        if (minecraft.theWorld == null || minecraft.thePlayer == null) return false;
        MovingObjectPosition hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        BlockPos block = hit.getBlockPos();
        if (block == null) return false;
        dimensionId = minecraft.theWorld.provider.getDimensionId();
        x = block.getX() + 0.5D;
        y = block.getY() + 1.0D;
        z = block.getZ() + 0.5D;
        active = true;
        revision++;
        return true;
    }

    /** Sets an explicit world-space navigation target for a brain or test harness. */
    public boolean setWorldTarget(double x, double y, double z) {
        if (minecraft.theWorld == null) return false;
        this.dimensionId = minecraft.theWorld.provider.getDimensionId();
        this.x = x;
        this.y = y;
        this.z = z;
        this.active = true;
        this.revision++;
        return true;
    }

    public boolean clear() {
        boolean changed = active;
        active = false;
        revision++;
        return changed;
    }

    public void onWorldUnavailable() {
        active = false;
        revision++;
    }

    public LandmarkObservation capture(EntityPlayerSP player, World world) {
        if (!active || player == null || world == null
            || world.provider.getDimensionId() != dimensionId) return null;
        double dx = x - player.posX;
        double dy = y - player.posY;
        double dz = z - player.posZ;
        float right = EgocentricTransform.right(dx, dz, player.rotationYaw);
        float forward = EgocentricTransform.forward(dx, dz, player.rotationYaw);
        float distance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new LandmarkObservation(USER_WAYPOINT_ID, LandmarkType.STAGING_AREA,
            TeamRelation.NEUTRAL, right, (float)dy, forward, distance, distance,
            0F, 1F, 1F, true);
    }

    public boolean active() { return active; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public long revision() { return revision; }

    public String compactPosition() {
        if (!active) return "none";
        return one(x) + "," + one(y) + "," + one(z);
    }

    private static String one(double value) {
        long scaled = Math.round(Math.abs(value) * 10.0D);
        return (value < 0D ? "-" : "") + (scaled / 10L) + "." + (scaled % 10L);
    }
}
