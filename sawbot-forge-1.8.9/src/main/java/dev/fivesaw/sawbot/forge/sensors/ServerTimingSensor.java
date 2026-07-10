package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.events.EventHistorySnapshot;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.events.ObservationEventType;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.EntitySetSnapshot;
import dev.fivesaw.sawbot.common.observation.ServerTimingSnapshot;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;

public final class ServerTimingSensor {
    private int previousPing=-1; private int jitter; private int ticksSinceEnemy=65535;
    public ServerTimingSnapshot capture(Minecraft minecraft,EntityPlayerSP player,EntitySetSnapshot entities,EventHistorySnapshot events,long tick){int ping=0;boolean valid=false;if(minecraft.getNetHandler()!=null){NetworkPlayerInfo info=minecraft.getNetHandler().getPlayerInfo(player.getUniqueID());if(info!=null){ping=Math.max(0,info.getResponseTime());valid=true;if(previousPing>=0)jitter=(jitter*3+Math.abs(ping-previousPing))/4;previousPing=ping;}}
        boolean enemy=false;for(EntityObservation entity:entities.entities())if(entity.teamRelation()==TeamRelation.ENEMY){enemy=true;break;}ticksSinceEnemy=enemy?0:increment(ticksSinceEnemy);int hit=age(events,ObservationEventType.HIT_CONFIRMED,tick),correction=age(events,ObservationEventType.SERVER_CORRECTION,tick);return new ServerTimingSnapshot(ping,jitter,ticksSinceEnemy,hit,65535,correction,valid);}
    private static int age(EventHistorySnapshot history,ObservationEventType type,long tick){java.util.List<ObservationEvent> values=history.events();for(int i=values.size()-1;i>=0;i--){ObservationEvent event=values.get(i);if(event.type()==type)return (int)Math.min(65535,Math.max(0,tick-event.clientTick()));}return 65535;}
    private static int increment(int value){return value>=65535?65535:value+1;}
}
