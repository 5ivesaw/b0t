package dev.fivesaw.sawbot.common.observation;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
public final class LandmarkSetSnapshot {
    public static final int MAX_LANDMARKS=64; private final List<LandmarkObservation> landmarks;
    public LandmarkSetSnapshot(List<LandmarkObservation> landmarks){if(landmarks==null||landmarks.size()>MAX_LANDMARKS)throw new IllegalArgumentException("landmarks");this.landmarks=Collections.unmodifiableList(new ArrayList<LandmarkObservation>(landmarks));}
    public List<LandmarkObservation> landmarks(){return landmarks;} public int count(){return landmarks.size();}
}
