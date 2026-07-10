package dev.fivesaw.sawbot.common.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EntitySetSnapshot {
    public static final int MAX_ENTITIES = 32;
    private final List<EntityObservation> entities;
    private final int droppedCount;
    public EntitySetSnapshot(List<EntityObservation> entities, int droppedCount) {
        if (entities == null || entities.size() > MAX_ENTITIES || droppedCount < 0) throw new IllegalArgumentException("entities");
        this.entities = Collections.unmodifiableList(new ArrayList<EntityObservation>(entities));
        this.droppedCount = droppedCount;
    }
    public List<EntityObservation> entities() { return entities; }
    public int count() { return entities.size(); }
    public int droppedCount() { return droppedCount; }
}
