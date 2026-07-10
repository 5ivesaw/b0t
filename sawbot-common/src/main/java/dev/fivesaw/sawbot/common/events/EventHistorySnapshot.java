package dev.fivesaw.sawbot.common.events;
import java.util.ArrayList; import java.util.Collections; import java.util.List;
public final class EventHistorySnapshot { public static final int MAX_EVENTS=64; private final List<ObservationEvent> events; private final long dropped;
 public EventHistorySnapshot(List<ObservationEvent> events,long dropped){if(events==null||events.size()>MAX_EVENTS||dropped<0)throw new IllegalArgumentException("events");this.events=Collections.unmodifiableList(new ArrayList<ObservationEvent>(events));this.dropped=dropped;}
 public List<ObservationEvent> events(){return events;} public int count(){return events.size();} public long dropped(){return dropped;}}
