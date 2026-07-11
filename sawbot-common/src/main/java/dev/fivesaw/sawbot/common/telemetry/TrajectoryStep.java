package dev.fivesaw.sawbot.common.telemetry;

import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One observation, the exact following input ticks, and the next observed outcome boundary. */
public final class TrajectoryStep {
    public static final int MAX_OUTCOME_EVENTS = 64;

    private final ObservationSnapshot observation;
    private final ActionSource actionSource;
    private final HumanInputWindow humanInput;
    private final long outcomeSequenceNumber;
    private final long outcomeClientTick;
    private final List<ObservationEvent> outcomeEvents;
    private final boolean incompleteOutcome;

    public TrajectoryStep(ObservationSnapshot observation, ActionSource actionSource,
                          HumanInputWindow humanInput, long outcomeSequenceNumber,
                          long outcomeClientTick, List<ObservationEvent> outcomeEvents,
                          boolean incompleteOutcome) {
        if (observation == null || actionSource == null || humanInput == null || outcomeEvents == null) {
            throw new IllegalArgumentException("trajectory component");
        }
        if (outcomeSequenceNumber < observation.sequenceNumber() || outcomeClientTick < observation.clientTick()) {
            throw new IllegalArgumentException("outcome boundary");
        }
        if (outcomeEvents.size() > MAX_OUTCOME_EVENTS) throw new IllegalArgumentException("outcomeEvents");
        this.observation = observation;
        this.actionSource = actionSource;
        this.humanInput = humanInput;
        this.outcomeSequenceNumber = outcomeSequenceNumber;
        this.outcomeClientTick = outcomeClientTick;
        this.outcomeEvents = Collections.unmodifiableList(new ArrayList<ObservationEvent>(outcomeEvents));
        this.incompleteOutcome = incompleteOutcome;
    }

    public ObservationSnapshot observation() { return observation; }
    public ActionSource actionSource() { return actionSource; }
    public HumanInputWindow humanInput() { return humanInput; }
    public long outcomeSequenceNumber() { return outcomeSequenceNumber; }
    public long outcomeClientTick() { return outcomeClientTick; }
    public List<ObservationEvent> outcomeEvents() { return outcomeEvents; }
    public boolean incompleteOutcome() { return incompleteOutcome; }
}
