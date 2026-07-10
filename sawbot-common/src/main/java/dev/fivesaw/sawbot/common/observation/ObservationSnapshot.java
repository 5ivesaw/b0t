package dev.fivesaw.sawbot.common.observation;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import java.util.Arrays;
import java.util.UUID;

public final class ObservationSnapshot {
    public static final int LOCAL_TERRAIN_CELLS = 13 * 9 * 13;
    public static final int MID_RANGE_COLUMNS = 33 * 33;
    public static final int MAX_ENTITIES = 32;
    public static final int MAX_LANDMARKS = 64;
    public static final int MAX_EVENTS = 64;

    private final SchemaVersion schemaVersion;
    private final long clientTick;
    private final long monotonicTimestampNanos;
    private final UUID episodeId;
    private final long sequenceNumber;
    private final String worldIdentifier;
    private final String taskAdapterIdentifier;
    private final byte[] selfState;
    private final byte[] localTerrain;
    private final byte[] midRangeMap;
    private final byte[] entities;
    private final byte[] inventory;
    private final byte[] landmarks;
    private final byte[] events;
    private final byte[] serverTiming;
    private final byte[] taskState;
    private final ActionCommand previousAction;
    private final long sensorValidityFlags;

    public ObservationSnapshot(long clientTick, long monotonicTimestampNanos, UUID episodeId, long sequenceNumber,
                               String worldIdentifier, String taskAdapterIdentifier, byte[] selfState,
                               byte[] localTerrain, byte[] midRangeMap, byte[] entities, byte[] inventory,
                               byte[] landmarks, byte[] events, byte[] serverTiming, byte[] taskState,
                               ActionCommand previousAction, long sensorValidityFlags) {
        if (clientTick < 0 || monotonicTimestampNanos < 0 || sequenceNumber < 0) {
            throw new IllegalArgumentException("tick, timestamp, and sequence must be nonnegative");
        }
        this.schemaVersion = SchemaVersion.OBSERVATION_V0_1;
        this.clientTick = clientTick;
        this.monotonicTimestampNanos = monotonicTimestampNanos;
        this.episodeId = episodeId == null ? new UUID(0L, 0L) : episodeId;
        this.sequenceNumber = sequenceNumber;
        this.worldIdentifier = bounded(worldIdentifier, 96, "worldIdentifier");
        this.taskAdapterIdentifier = bounded(taskAdapterIdentifier, 48, "taskAdapterIdentifier");
        this.selfState = copy(selfState, "selfState");
        this.localTerrain = copy(localTerrain, "localTerrain");
        this.midRangeMap = copy(midRangeMap, "midRangeMap");
        this.entities = copy(entities, "entities");
        this.inventory = copy(inventory, "inventory");
        this.landmarks = copy(landmarks, "landmarks");
        this.events = copy(events, "events");
        this.serverTiming = copy(serverTiming, "serverTiming");
        this.taskState = copy(taskState, "taskState");
        if (previousAction == null) throw new IllegalArgumentException("previousAction");
        this.previousAction = previousAction;
        this.sensorValidityFlags = sensorValidityFlags;
    }

    private static String bounded(String value, int max, String field) {
        if (value == null || value.isEmpty() || value.length() > max) throw new IllegalArgumentException(field);
        return value;
    }
    private static byte[] copy(byte[] value, String field) {
        if (value == null) throw new IllegalArgumentException(field);
        return Arrays.copyOf(value, value.length);
    }

    public SchemaVersion schemaVersion() { return schemaVersion; }
    public long clientTick() { return clientTick; }
    public long monotonicTimestampNanos() { return monotonicTimestampNanos; }
    public UUID episodeId() { return episodeId; }
    public long sequenceNumber() { return sequenceNumber; }
    public String worldIdentifier() { return worldIdentifier; }
    public String taskAdapterIdentifier() { return taskAdapterIdentifier; }
    public byte[] selfState() { return Arrays.copyOf(selfState, selfState.length); }
    public byte[] localTerrain() { return Arrays.copyOf(localTerrain, localTerrain.length); }
    public byte[] midRangeMap() { return Arrays.copyOf(midRangeMap, midRangeMap.length); }
    public byte[] entities() { return Arrays.copyOf(entities, entities.length); }
    public byte[] inventory() { return Arrays.copyOf(inventory, inventory.length); }
    public byte[] landmarks() { return Arrays.copyOf(landmarks, landmarks.length); }
    public byte[] events() { return Arrays.copyOf(events, events.length); }
    public byte[] serverTiming() { return Arrays.copyOf(serverTiming, serverTiming.length); }
    public byte[] taskState() { return Arrays.copyOf(taskState, taskState.length); }
    public ActionCommand previousAction() { return previousAction; }
    public long sensorValidityFlags() { return sensorValidityFlags; }
}
