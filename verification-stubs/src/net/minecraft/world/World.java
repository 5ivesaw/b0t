package net.minecraft.world;
import java.util.*; import net.minecraft.block.state.IBlockState; import net.minecraft.entity.Entity; import net.minecraft.init.Blocks; import net.minecraft.util.AxisAlignedBB; import net.minecraft.util.BlockPos; import net.minecraft.util.MovingObjectPosition; import net.minecraft.util.Vec3; import net.minecraft.world.storage.WorldInfo;
public class World {
    public final List<Object> loadedEntityList=new ArrayList<Object>(); public final WorldProvider provider=new WorldProvider(); private final Map<BlockPos,IBlockState> blocks=new HashMap<BlockPos,IBlockState>(); private int getBlockStateCalls;
    public boolean isBlockLoaded(BlockPos pos){return true;} public IBlockState getBlockState(BlockPos pos){getBlockStateCalls++;IBlockState s=blocks.get(pos);return s==null?Blocks.air.getDefaultState():s;}
    public void resetGetBlockStateCallsForTest(){getBlockStateCalls=0;} public int getBlockStateCallsForTest(){return getBlockStateCalls;}
    public BlockPos getSpawnPoint(){return new BlockPos(0,64,0);} public WorldInfo getWorldInfo(){return new WorldInfo();} public void setBlockStateForTest(BlockPos p,IBlockState s){blocks.put(p,s);}
    public List<AxisAlignedBB> getCollisionBoxes(AxisAlignedBB mask){List<AxisAlignedBB> result=new ArrayList<AxisAlignedBB>();for(Map.Entry<BlockPos,IBlockState> e:blocks.entrySet()){IBlockState state=e.getValue();state.getBlock().addCollisionBoxesToList(this,e.getKey(),state,mask,result,(Entity)null);}return result;}
    public List<AxisAlignedBB> getCollidingBoundingBoxes(Entity entity,AxisAlignedBB mask){return getCollisionBoxes(mask);}
    public MovingObjectPosition rayTraceBlocks(Vec3 start,Vec3 end,boolean stopOnLiquid,boolean ignoreBlockWithoutBoundingBox,boolean returnLastUncollidableBlock){
        double dx=end.xCoord-start.xCoord,dy=end.yCoord-start.yCoord,dz=end.zCoord-start.zCoord;
        int steps=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)*20.0));
        for(int i=1;i<steps;i++){
            double t=(double)i/(double)steps; double x=start.xCoord+dx*t,y=start.yCoord+dy*t,z=start.zCoord+dz*t;
            BlockPos pos=new BlockPos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z)); IBlockState state=getBlockState(pos);
            AxisAlignedBB box=state.getBlock().getCollisionBoundingBox(this,pos,state);
            if(box!=null&&x>=box.minX&&x<=box.maxX&&y>=box.minY&&y<=box.maxY&&z>=box.minZ&&z<=box.maxZ)return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK,pos,null);
        }
        return null;
    }
}
