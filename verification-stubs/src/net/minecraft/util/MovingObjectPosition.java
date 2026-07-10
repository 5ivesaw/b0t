package net.minecraft.util;
import net.minecraft.entity.Entity;
public class MovingObjectPosition {
    public enum MovingObjectType { MISS, BLOCK, ENTITY }
    public MovingObjectType typeOfHit;
    public Entity entityHit;
    private BlockPos blockPos;
    public MovingObjectPosition(MovingObjectType type, BlockPos pos, Entity entity){this.typeOfHit=type;this.blockPos=pos;this.entityHit=entity;}
    public BlockPos getBlockPos(){return blockPos;}
}
