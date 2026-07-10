package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class LocalTerrainSensor {
    private final BlockSemanticClassifier classifier;
    private short[] previousStateIds;
    private int previousOriginX, previousOriginY, previousOriginZ;
    private byte previousFacing;
    private boolean previousValid;

    public LocalTerrainSensor(BlockSemanticClassifier classifier) { this.classifier = classifier; }

    public LocalTerrainSnapshot capture(EntityPlayerSP player, World world) {
        int originX=MathHelper.floor_double(player.posX), originY=MathHelper.floor_double(player.getEntityBoundingBox().minY), originZ=MathHelper.floor_double(player.posZ);
        byte facing=EgocentricTransform.quadrant(player.rotationYaw);
        short[] ids=new short[LocalTerrainSnapshot.CELL_COUNT]; byte[] categories=new byte[ids.length]; short[] flags=new short[ids.length]; byte[] collision=new byte[ids.length];
        boolean sameBasis=previousValid&&originX==previousOriginX&&originY==previousOriginY&&originZ==previousOriginZ&&facing==previousFacing;
        int changed=0;
        for(int up=-4;up<=4;up++) for(int forward=-6;forward<=6;forward++) for(int right=-6;right<=6;right++){
            int index=LocalTerrainSnapshot.index(right,up,forward);
            BlockPos pos=new BlockPos(originX+EgocentricTransform.worldDx(right,forward,facing),originY+up,originZ+EgocentricTransform.worldDz(right,forward,facing));
            if(!world.isBlockLoaded(pos)){BlockSemanticClassifier.CellClassification unknown=classifier.unknown();ids[index]=unknown.blockStateId;categories[index]=unknown.category;flags[index]=unknown.flags;collision[index]=unknown.collisionHeightClass;continue;}
            IBlockState state=world.getBlockState(pos); short stateId=(short)net.minecraft.block.Block.getStateId(state);
            boolean cellChanged=sameBasis&&previousStateIds[index]!=stateId; if(cellChanged)changed++;
            BlockSemanticClassifier.CellClassification value=classifier.classify(world,pos,state,cellChanged);
            ids[index]=value.blockStateId;categories[index]=value.category;flags[index]=value.flags;collision[index]=value.collisionHeightClass;
        }
        previousStateIds=ids.clone();previousOriginX=originX;previousOriginY=originY;previousOriginZ=originZ;previousFacing=facing;previousValid=true;
        return new LocalTerrainSnapshot(originX,originY,originZ,facing,ids,categories,flags,collision,changed);
    }
}
