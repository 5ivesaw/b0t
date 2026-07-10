package dev.fivesaw.sawbot.common.observation;
public final class ServerTimingSnapshot {
    private final int estimatedPingMillis, pingJitterMillis, ticksSinceEnemyUpdate, ticksSinceHitConfirmation, ticksSincePlacementAcknowledgement, ticksSinceServerCorrection;
    private final boolean pingValid;
    public ServerTimingSnapshot(int estimatedPingMillis,int pingJitterMillis,int ticksSinceEnemyUpdate,int ticksSinceHitConfirmation,int ticksSincePlacementAcknowledgement,int ticksSinceServerCorrection,boolean pingValid){
        if(estimatedPingMillis<0||pingJitterMillis<0||ticksSinceEnemyUpdate<0||ticksSinceHitConfirmation<0||ticksSincePlacementAcknowledgement<0||ticksSinceServerCorrection<0)throw new IllegalArgumentException("timing");
        this.estimatedPingMillis=estimatedPingMillis;this.pingJitterMillis=pingJitterMillis;this.ticksSinceEnemyUpdate=ticksSinceEnemyUpdate;this.ticksSinceHitConfirmation=ticksSinceHitConfirmation;this.ticksSincePlacementAcknowledgement=ticksSincePlacementAcknowledgement;this.ticksSinceServerCorrection=ticksSinceServerCorrection;this.pingValid=pingValid;}
    public int estimatedPingMillis(){return estimatedPingMillis;} public int pingJitterMillis(){return pingJitterMillis;} public int ticksSinceEnemyUpdate(){return ticksSinceEnemyUpdate;} public int ticksSinceHitConfirmation(){return ticksSinceHitConfirmation;} public int ticksSincePlacementAcknowledgement(){return ticksSincePlacementAcknowledgement;} public int ticksSinceServerCorrection(){return ticksSinceServerCorrection;} public boolean pingValid(){return pingValid;}
}
