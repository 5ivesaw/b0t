package net.minecraft.util;
import net.minecraft.entity.Entity;
public class MovingObjectPosition {
    public enum MovingObjectType { MISS, BLOCK, ENTITY }
    public MovingObjectType typeOfHit;
    public Entity entityHit;
    public EnumFacing sideHit;
    public Vec3 hitVec;
    private BlockPos blockPos;
    public MovingObjectPosition(MovingObjectType type, BlockPos pos, Entity entity){this(type,pos,entity,null,null);}
    public MovingObjectPosition(MovingObjectType type, BlockPos pos, Entity entity, EnumFacing side, Vec3 hit){this.typeOfHit=type;this.blockPos=pos;this.entityHit=entity;this.sideHit=side;this.hitVec=hit;}
    public BlockPos getBlockPos(){return blockPos;}
}
