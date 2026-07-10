package dev.fivesaw.sawbot.forge.performance;

public final class RollingTimingWindow {
    private final long[] samples;
    private int nextIndex;
    private int count;
    private long sum;

    public RollingTimingWindow(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        samples = new long[capacity];
    }

    public void add(long nanos) {
        if (nanos < 0) throw new IllegalArgumentException("nanos");
        if (count == samples.length) sum -= samples[nextIndex];
        else count++;
        samples[nextIndex] = nanos;
        sum += nanos;
        nextIndex = (nextIndex + 1) % samples.length;
    }

    public int count() { return count; }
    public int capacity() { return samples.length; }
    public long averageNanos() { return count == 0 ? 0L : sum / count; }
    public long latestNanos() {
        if (count == 0) return 0L;
        int index = nextIndex == 0 ? samples.length - 1 : nextIndex - 1;
        return samples[index];
    }
    public long maximumNanos() {
        long maximum = 0L;
        for (int i = 0; i < count; i++) maximum = Math.max(maximum, samples[i]);
        return maximum;
    }
}
