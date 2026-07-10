package dev.fivesaw.sawbot.forge.map;

import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkSetSnapshot;
import dev.fivesaw.sawbot.common.observation.LandmarkType;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import java.util.Collections;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class LandmarkSensor {
    public LandmarkSetSnapshot capture(EntityPlayerSP player,World world){BlockPos spawn=world.getSpawnPoint();double dx=spawn.getX()+0.5-player.posX,dy=spawn.getY()-player.posY,dz=spawn.getZ()+0.5-player.posZ;float right=EgocentricTransform.right(dx,dz,player.rotationYaw),forward=EgocentricTransform.forward(dx,dz,player.rotationYaw);float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);LandmarkObservation landmark=new LandmarkObservation(0,LandmarkType.WORLD_SPAWN,TeamRelation.NEUTRAL,right,(float)dy,forward,distance,distance,0f,0.25f,1f,true);return new LandmarkSetSnapshot(Collections.singletonList(landmark));}
}
