package dev.fivesaw.sawbot.common.observation;

public final class SensorValidity {
    public static final long SELF = 1L << 0;
    public static final long LOCAL_TERRAIN = 1L << 1;
    public static final long MID_RANGE_MAP = 1L << 2;
    public static final long ENTITIES = 1L << 3;
    public static final long INVENTORY = 1L << 4;
    public static final long LANDMARKS = 1L << 5;
    public static final long EVENTS = 1L << 6;
    public static final long SERVER_TIMING = 1L << 7;
    public static final long TASK_STATE = 1L << 8;
    public static final long ALL_PHASE1 = SELF | LOCAL_TERRAIN | MID_RANGE_MAP | ENTITIES |
        INVENTORY | LANDMARKS | EVENTS | SERVER_TIMING | TASK_STATE;

    private SensorValidity() { }
}
