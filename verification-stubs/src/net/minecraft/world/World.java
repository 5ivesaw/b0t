package net.minecraft.world;
import java.util.*; import net.minecraft.block.state.IBlockState; import net.minecraft.entity.Entity; import net.minecraft.init.Blocks; import net.minecraft.util.AxisAlignedBB; import net.minecraft.util.BlockPos; import net.minecraft.world.storage.WorldInfo;
public class World {
    public final List<Object> loadedEntityList=new ArrayList<Object>(); public final WorldProvider provider=new WorldProvider(); private final Map<BlockPos,IBlockState> blocks=new HashMap<BlockPos,IBlockState>(); private int getBlockStateCalls;
    public boolean isBlockLoaded(BlockPos pos){return true;} public IBlockState getBlockState(BlockPos pos){getBlockStateCalls++;IBlockState s=blocks.get(pos);return s==null?Blocks.air.getDefaultState():s;}
    public void resetGetBlockStateCallsForTest(){getBlockStateCalls=0;} public int getBlockStateCallsForTest(){return getBlockStateCalls;}
    public BlockPos getSpawnPoint(){return new BlockPos(0,64,0);} public WorldInfo getWorldInfo(){return new WorldInfo();} public void setBlockStateForTest(BlockPos p,IBlockState s){blocks.put(p,s);}
    public List<AxisAlignedBB> getCollisionBoxes(AxisAlignedBB mask){List<AxisAlignedBB> result=new ArrayList<AxisAlignedBB>();for(Map.Entry<BlockPos,IBlockState> e:blocks.entrySet()){IBlockState state=e.getValue();state.getBlock().addCollisionBoxesToList(this,e.getKey(),state,mask,result,(Entity)null);}return result;}
    public List<AxisAlignedBB> getCollidingBoundingBoxes(Entity entity,AxisAlignedBB mask){return getCollisionBoxes(mask);}
}
