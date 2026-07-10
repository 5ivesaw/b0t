package dev.fivesaw.sawbot.common.observation;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.events.EventHistorySnapshot;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import java.util.UUID;

public final class ObservationSnapshot {
    private final SchemaVersion schemaVersion;
    private final long clientTick;
    private final long monotonicTimestampNanos;
    private final UUID episodeId;
    private final long sequenceNumber;
    private final String worldIdentifier;
    private final String taskAdapterIdentifier;
    private final SelfState selfState;
    private final LocalTerrainSnapshot localTerrain;
    private final MidRangeMapSnapshot midRangeMap;
    private final EntitySetSnapshot entities;
    private final InventorySnapshot inventory;
    private final LandmarkSetSnapshot landmarks;
    private final EventHistorySnapshot events;
    private final ServerTimingSnapshot serverTiming;
    private final TaskStateSnapshot taskState;
    private final ActionCommand previousAction;
    private final long sensorValidityFlags;
    private final SensorTimings sensorTimings;

    public ObservationSnapshot(long clientTick, long monotonicTimestampNanos, UUID episodeId, long sequenceNumber,
                               String worldIdentifier, String taskAdapterIdentifier, SelfState selfState,
                               LocalTerrainSnapshot localTerrain, MidRangeMapSnapshot midRangeMap,
                               EntitySetSnapshot entities, InventorySnapshot inventory,
                               LandmarkSetSnapshot landmarks, EventHistorySnapshot events,
                               ServerTimingSnapshot serverTiming, TaskStateSnapshot taskState,
                               ActionCommand previousAction, long sensorValidityFlags, SensorTimings sensorTimings) {
        if (clientTick < 0 || monotonicTimestampNanos < 0 || sequenceNumber < 0) throw new IllegalArgumentException("tick/timestamp/sequence");
        this.schemaVersion = SchemaVersion.OBSERVATION_V0_2;
        this.clientTick = clientTick;
        this.monotonicTimestampNanos = monotonicTimestampNanos;
        this.episodeId = episodeId == null ? new UUID(0L, 0L) : episodeId;
        this.sequenceNumber = sequenceNumber;
        this.worldIdentifier = bounded(worldIdentifier, 96, "worldIdentifier");
        this.taskAdapterIdentifier = bounded(taskAdapterIdentifier, 48, "taskAdapterIdentifier");
        if (selfState == null || localTerrain == null || midRangeMap == null || entities == null || inventory == null || landmarks == null || events == null || serverTiming == null || taskState == null || previousAction == null || sensorTimings == null) {
            throw new IllegalArgumentException("snapshot component");
        }
        this.selfState = selfState; this.localTerrain = localTerrain; this.midRangeMap = midRangeMap;
        this.entities = entities; this.inventory = inventory; this.landmarks = landmarks; this.events = events;
        this.serverTiming = serverTiming; this.taskState = taskState; this.previousAction = previousAction;
        this.sensorValidityFlags = sensorValidityFlags; this.sensorTimings = sensorTimings;
    }
    private static String bounded(String value,int max,String field){if(value==null||value.isEmpty()||value.length()>max)throw new IllegalArgumentException(field);return value;}
    public SchemaVersion schemaVersion(){return schemaVersion;} public long clientTick(){return clientTick;} public long monotonicTimestampNanos(){return monotonicTimestampNanos;}
    public UUID episodeId(){return episodeId;} public long sequenceNumber(){return sequenceNumber;} public String worldIdentifier(){return worldIdentifier;} public String taskAdapterIdentifier(){return taskAdapterIdentifier;}
    public SelfState selfState(){return selfState;} public LocalTerrainSnapshot localTerrain(){return localTerrain;} public MidRangeMapSnapshot midRangeMap(){return midRangeMap;}
    public EntitySetSnapshot entities(){return entities;} public InventorySnapshot inventory(){return inventory;} public LandmarkSetSnapshot landmarks(){return landmarks;}
    public EventHistorySnapshot events(){return events;} public ServerTimingSnapshot serverTiming(){return serverTiming;} public TaskStateSnapshot taskState(){return taskState;}
    public ActionCommand previousAction(){return previousAction;} public long sensorValidityFlags(){return sensorValidityFlags;} public SensorTimings sensorTimings(){return sensorTimings;}
}
