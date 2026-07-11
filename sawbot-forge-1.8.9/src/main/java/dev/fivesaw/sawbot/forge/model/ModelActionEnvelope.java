package dev.fivesaw.sawbot.forge.model;

import dev.fivesaw.sawbot.common.action.ActionCommand;

public final class ModelActionEnvelope {
    private final ActionCommand command;
    private final long receivedTimestampNanos;
    private final long roundTripNanos;

    public ModelActionEnvelope(ActionCommand command, long receivedTimestampNanos, long roundTripNanos) {
        if (command == null || receivedTimestampNanos < 0L || roundTripNanos < 0L) {
            throw new IllegalArgumentException("model action envelope");
        }
        this.command = command;
        this.receivedTimestampNanos = receivedTimestampNanos;
        this.roundTripNanos = roundTripNanos;
    }

    public ActionCommand command() { return command; }
    public long receivedTimestampNanos() { return receivedTimestampNanos; }
    public long roundTripNanos() { return roundTripNanos; }
}
