package dev.fivesaw.sawbot.forge.sensors;

import dev.fivesaw.sawbot.common.events.EventHistorySnapshot;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.events.ObservationEventType;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.EntitySetSnapshot;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EventSensor {
    private final Deque<ObservationEvent> events=new ArrayDeque<ObservationEvent>(EventHistorySnapshot.MAX_EVENTS);
    private long dropped; private float lastCombinedHealth=-1f; private InventorySnapshot lastInventory; private Set<Integer> lastEntityIds=new HashSet<Integer>();
    private final Map<Integer,Integer> lastHurtTimers=new HashMap<Integer,Integer>(); private LocalTerrainSnapshot lastTerrain; private double lastX,lastY,lastZ; private boolean hasPosition;

    public EventHistorySnapshot capture(long tick,SelfState self,InventorySnapshot inventory,EntitySetSnapshot entities,LocalTerrainSnapshot terrain){
        float combined=self.health()+self.absorption();if(lastCombinedHealth>=0f&&combined<lastCombinedHealth)add(new ObservationEvent(ObservationEventType.DAMAGE_RECEIVED,tick,-1,0f,0f,0f,lastCombinedHealth-combined,true));if(lastCombinedHealth<=0f&&lastCombinedHealth>=0f&&self.health()>0f)add(new ObservationEvent(ObservationEventType.RESPAWNED,tick,-1,0f,0f,0f,1f,true));lastCombinedHealth=combined;
        if(lastInventory!=null){resourceDelta(tick,inventory.iron()-lastInventory.iron());resourceDelta(tick,inventory.gold()-lastInventory.gold());resourceDelta(tick,inventory.diamonds()-lastInventory.diamonds());resourceDelta(tick,inventory.emeralds()-lastInventory.emeralds());resourceDelta(tick,inventory.wool()-lastInventory.wool());}lastInventory=inventory;
        Set<Integer> current=new HashSet<Integer>();for(EntityObservation entity:entities.entities()){current.add(Integer.valueOf(entity.trackingId()));if(!lastEntityIds.contains(Integer.valueOf(entity.trackingId())))add(new ObservationEvent(ObservationEventType.ENTITY_ENTERED_RANGE,tick,entity.trackingId(),entity.right(),entity.up(),entity.forward(),entity.distance(),true));Integer oldHurt=lastHurtTimers.put(Integer.valueOf(entity.trackingId()),Integer.valueOf(entity.hurtTimerTicks()));if(oldHurt!=null&&entity.hurtTimerTicks()>oldHurt.intValue()){add(new ObservationEvent(ObservationEventType.ENTITY_HURT_OBSERVED,tick,entity.trackingId(),entity.right(),entity.up(),entity.forward(),1f,true));}}
        for(Integer id:lastEntityIds)if(!current.contains(id))add(new ObservationEvent(ObservationEventType.ENTITY_LEFT_RANGE,tick,id.intValue(),0f,0f,0f,0f,true));lastEntityIds=current;lastHurtTimers.keySet().retainAll(current);
        detectBlockChanges(tick,terrain);if(hasPosition){double dx=self.absoluteX()-lastX,dy=self.absoluteY()-lastY,dz=self.absoluteZ()-lastZ;double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);if(distance>8.0)add(new ObservationEvent(ObservationEventType.SERVER_CORRECTION,tick,-1,0f,0f,0f,(float)distance,true));}lastX=self.absoluteX();lastY=self.absoluteY();lastZ=self.absoluteZ();hasPosition=true;
        return new EventHistorySnapshot(new java.util.ArrayList<ObservationEvent>(events),dropped);
    }
    private void resourceDelta(long tick,int delta){if(delta>0)add(new ObservationEvent(ObservationEventType.ITEM_COLLECTED,tick,-1,0f,0f,0f,delta,true));else if(delta<0)add(new ObservationEvent(ObservationEventType.ITEM_SPENT,tick,-1,0f,0f,0f,-delta,true));}
    private void detectBlockChanges(long tick,LocalTerrainSnapshot terrain){if(lastTerrain!=null&&terrain.originX()==lastTerrain.originX()&&terrain.originY()==lastTerrain.originY()&&terrain.originZ()==lastTerrain.originZ()&&terrain.facingQuadrant()==lastTerrain.facingQuadrant()){short[] now=terrain.blockStateIds(),before=lastTerrain.blockStateIds();for(int i=0;i<now.length;i++){if(now[i]==before[i])continue;ObservationEventType type=before[i]==0&&now[i]!=0?ObservationEventType.BLOCK_PLACED:now[i]==0?ObservationEventType.BLOCK_BROKEN:null;if(type!=null){add(new ObservationEvent(type,tick,-1,0f,0f,0f,1f,true));break;}}}lastTerrain=terrain;}
    private void add(ObservationEvent event){if(events.size()==EventHistorySnapshot.MAX_EVENTS){events.removeFirst();dropped++;}events.addLast(event);}
}
