package net.minecraft.entity; import java.util.UUID; import net.minecraft.util.AxisAlignedBB;
public class Entity {
    private static int next; private final int id=next++;
    public double posX,posY,posZ,lastTickPosX,lastTickPosY,lastTickPosZ,motionX,motionY,motionZ;
    public float rotationYaw,rotationPitch; public boolean onGround,isDead;
    public int getEntityId(){return id;} public UUID getUniqueID(){return new UUID(0,id);}
    public AxisAlignedBB getEntityBoundingBox(){return new AxisAlignedBB(posX-.3,posY,posZ-.3,posX+.3,posY+1.8,posZ+.3);}
    public boolean isSprinting(){return false;} public boolean isSneaking(){return false;} public float getEyeHeight(){return 1.62f;}
}
