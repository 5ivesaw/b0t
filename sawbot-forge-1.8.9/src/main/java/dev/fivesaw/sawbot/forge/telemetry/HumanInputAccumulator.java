package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.telemetry.HumanInputSample;
import dev.fivesaw.sawbot.common.telemetry.HumanInputWindow;
import java.util.ArrayList;
import java.util.List;

/** Client-thread-only bounded input accumulation between observation snapshots. */
final class HumanInputAccumulator {
    private final int capacity;
    private final ArrayList<HumanInputSample> samples;
    private int dropped;

    HumanInputAccumulator(int capacity) {
        if (capacity < 2 || capacity > HumanInputWindow.ABSOLUTE_MAX_SAMPLES) {
            throw new IllegalArgumentException("capacity");
        }
        this.capacity = capacity;
        this.samples = new ArrayList<HumanInputSample>(capacity);
    }

    void add(HumanInputSample sample) {
        if (sample == null) return;
        if (samples.size() == capacity) {
            samples.remove(0);
            dropped++;
        }
        samples.add(sample);
    }

    HumanInputWindow drain() {
        HumanInputWindow window = samples.isEmpty() && dropped == 0
            ? HumanInputWindow.EMPTY
            : new HumanInputWindow(new ArrayList<HumanInputSample>(samples), dropped);
        samples.clear();
        dropped = 0;
        return window;
    }

    boolean isEmpty() { return samples.isEmpty() && dropped == 0; }
    int size() { return samples.size(); }
    int dropped() { return dropped; }
    void clear() { samples.clear(); dropped = 0; }
}
