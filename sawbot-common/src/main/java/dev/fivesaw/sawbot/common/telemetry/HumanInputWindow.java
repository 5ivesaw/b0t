package dev.fivesaw.sawbot.common.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded immutable tick samples causally following one observation. */
public final class HumanInputWindow {
    public static final int ABSOLUTE_MAX_SAMPLES = 64;
    public static final HumanInputWindow EMPTY = new HumanInputWindow(
        Collections.<HumanInputSample>emptyList(), 0);

    private final List<HumanInputSample> samples;
    private final int droppedSamples;

    public HumanInputWindow(List<HumanInputSample> samples, int droppedSamples) {
        if (samples == null || samples.size() > ABSOLUTE_MAX_SAMPLES || droppedSamples < 0) {
            throw new IllegalArgumentException("input window");
        }
        this.samples = Collections.unmodifiableList(new ArrayList<HumanInputSample>(samples));
        this.droppedSamples = droppedSamples;
    }

    public List<HumanInputSample> samples() { return samples; }
    public int count() { return samples.size(); }
    public int droppedSamples() { return droppedSamples; }
}
