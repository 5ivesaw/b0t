package dev.fivesaw.sawbot.common.observation;
public final class SensorTimings {
    private final long selfNanos, terrainNanos, midRangeNanos, entitiesNanos, inventoryNanos, landmarksNanos, eventsNanos, serverTimingNanos, totalNanos;
    public SensorTimings(long selfNanos,long terrainNanos,long midRangeNanos,long entitiesNanos,long inventoryNanos,long landmarksNanos,long eventsNanos,long serverTimingNanos,long totalNanos){
        if(selfNanos<0||terrainNanos<0||midRangeNanos<0||entitiesNanos<0||inventoryNanos<0||landmarksNanos<0||eventsNanos<0||serverTimingNanos<0||totalNanos<0)throw new IllegalArgumentException("timings");
        this.selfNanos=selfNanos;this.terrainNanos=terrainNanos;this.midRangeNanos=midRangeNanos;this.entitiesNanos=entitiesNanos;this.inventoryNanos=inventoryNanos;this.landmarksNanos=landmarksNanos;this.eventsNanos=eventsNanos;this.serverTimingNanos=serverTimingNanos;this.totalNanos=totalNanos;}
    public long selfNanos(){return selfNanos;} public long terrainNanos(){return terrainNanos;} public long midRangeNanos(){return midRangeNanos;} public long entitiesNanos(){return entitiesNanos;} public long inventoryNanos(){return inventoryNanos;} public long landmarksNanos(){return landmarksNanos;} public long eventsNanos(){return eventsNanos;} public long serverTimingNanos(){return serverTimingNanos;} public long totalNanos(){return totalNanos;}
}
