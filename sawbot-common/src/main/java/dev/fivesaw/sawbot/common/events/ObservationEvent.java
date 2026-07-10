package dev.fivesaw.sawbot.common.events;
public final class ObservationEvent {
    private final ObservationEventType type; private final long clientTick; private final int trackingId; private final float right, up, forward, magnitude; private final boolean success;
    public ObservationEvent(ObservationEventType type,long clientTick,int trackingId,float right,float up,float forward,float magnitude,boolean success){if(type==null||clientTick<0||!Float.isFinite(right)||!Float.isFinite(up)||!Float.isFinite(forward)||!Float.isFinite(magnitude))throw new IllegalArgumentException("event");this.type=type;this.clientTick=clientTick;this.trackingId=trackingId;this.right=right;this.up=up;this.forward=forward;this.magnitude=magnitude;this.success=success;}
    public ObservationEventType type(){return type;} public long clientTick(){return clientTick;} public int trackingId(){return trackingId;} public float right(){return right;} public float up(){return up;} public float forward(){return forward;} public float magnitude(){return magnitude;} public boolean success(){return success;}
}
