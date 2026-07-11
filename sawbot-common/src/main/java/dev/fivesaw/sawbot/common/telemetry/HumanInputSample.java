package dev.fivesaw.sawbot.common.telemetry;

/** One exact client-tick input sample captured without screenshots or video. */
public final class HumanInputSample {
    public static final int FORWARD = 1 << 0;
    public static final int BACK = 1 << 1;
    public static final int LEFT = 1 << 2;
    public static final int RIGHT = 1 << 3;
    public static final int JUMP = 1 << 4;
    public static final int SPRINT = 1 << 5;
    public static final int SNEAK = 1 << 6;
    public static final int ATTACK = 1 << 7;
    public static final int USE = 1 << 8;
    public static final int DROP = 1 << 9;
    public static final int INVENTORY = 1 << 10;

    private final long clientTick;
    private final long monotonicTimestampNanos;
    private final int keyBits;
    private final int mouseDeltaX;
    private final int mouseDeltaY;
    private final int selectedSlot;
    private final boolean guiOpen;

    public HumanInputSample(long clientTick, long monotonicTimestampNanos, int keyBits,
                            int mouseDeltaX, int mouseDeltaY, int selectedSlot, boolean guiOpen) {
        if (clientTick < 0 || monotonicTimestampNanos < 0) throw new IllegalArgumentException("tick/timestamp");
        if ((keyBits & ~0x7FF) != 0) throw new IllegalArgumentException("keyBits");
        if (selectedSlot < 0 || selectedSlot > 8) throw new IllegalArgumentException("selectedSlot");
        this.clientTick = clientTick;
        this.monotonicTimestampNanos = monotonicTimestampNanos;
        this.keyBits = keyBits;
        this.mouseDeltaX = mouseDeltaX;
        this.mouseDeltaY = mouseDeltaY;
        this.selectedSlot = selectedSlot;
        this.guiOpen = guiOpen;
    }

    public long clientTick() { return clientTick; }
    public long monotonicTimestampNanos() { return monotonicTimestampNanos; }
    public int keyBits() { return keyBits; }
    public int mouseDeltaX() { return mouseDeltaX; }
    public int mouseDeltaY() { return mouseDeltaY; }
    public int selectedSlot() { return selectedSlot; }
    public boolean guiOpen() { return guiOpen; }
    public boolean keyDown(int mask) { return (keyBits & mask) != 0; }
}
