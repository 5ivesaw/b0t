package dev.fivesaw.sawbot.common.observation;
public final class LandmarkObservation {
    private final int landmarkId; private final LandmarkType type; private final TeamRelation team;
    private final float right, up, forward, distance, estimatedTravelCost, danger, value, confidence;
    private final boolean reachable;
    public LandmarkObservation(int landmarkId, LandmarkType type, TeamRelation team, float right, float up, float forward,
                               float distance, float estimatedTravelCost, float danger, float value, float confidence, boolean reachable) {
        if (landmarkId < 0 || type == null || team == null) throw new IllegalArgumentException("landmark");
        this.landmarkId=landmarkId; this.type=type; this.team=team; this.right=finite(right); this.up=finite(up); this.forward=finite(forward);
        this.distance=finite(distance); this.estimatedTravelCost=finite(estimatedTravelCost); this.danger=finite(danger); this.value=finite(value); this.confidence=finite(confidence); this.reachable=reachable;
    }
    private static float finite(float v){if(!Float.isFinite(v))throw new IllegalArgumentException("landmark value");return v;}
    public int landmarkId(){return landmarkId;} public LandmarkType type(){return type;} public TeamRelation team(){return team;}
    public float right(){return right;} public float up(){return up;} public float forward(){return forward;} public float distance(){return distance;}
    public float estimatedTravelCost(){return estimatedTravelCost;} public float danger(){return danger;} public float value(){return value;} public float confidence(){return confidence;} public boolean reachable(){return reachable;}
}
