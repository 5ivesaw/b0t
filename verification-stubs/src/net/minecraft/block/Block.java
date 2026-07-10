package net.minecraft.block;
import java.util.List; import net.minecraft.block.material.Material; import net.minecraft.block.state.IBlockState; import net.minecraft.entity.Entity; import net.minecraft.util.AxisAlignedBB; import net.minecraft.util.BlockPos; import net.minecraft.world.World;
public class Block {
    private static int nextId; private final int id=nextId++; private final Material material; private final boolean full;
    public Block(){this(Material.rock,true);} public Block(Material material,boolean full){this.material=material;this.full=full;}
    public static int getStateId(IBlockState state){return state.getBlock().id;} public Material getMaterial(){return material;} public boolean isFullBlock(){return full;}
    public boolean isReplaceable(World world,BlockPos pos){return material.isReplaceable();}
    public AxisAlignedBB getCollisionBoundingBox(World world,BlockPos pos,IBlockState state){return material.blocksMovement()?new AxisAlignedBB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1):null;}
    public void addCollisionBoxesToList(World world,BlockPos pos,IBlockState state,AxisAlignedBB mask,List<AxisAlignedBB> list,Entity entity){AxisAlignedBB box=getCollisionBoundingBox(world,pos,state);if(box!=null&&mask.intersectsWith(box))list.add(box);}
    public IBlockState getDefaultState(){return new IBlockState(this);}
}
