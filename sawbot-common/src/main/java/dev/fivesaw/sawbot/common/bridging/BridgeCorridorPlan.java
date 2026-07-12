package dev.fivesaw.sawbot.common.bridging;

import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded straight/staircase bridge corridor generated from one current position. */
public final class BridgeCorridorPlan {
    private final NavigationCell start;
    private final NavigationCell goal;
    private final List<BridgePlacementStep> steps;
    private final boolean reachesGoal;
    private final String reason;

    BridgeCorridorPlan(NavigationCell start, NavigationCell goal,
                       List<BridgePlacementStep> steps, boolean reachesGoal,
                       String reason) {
        this.start = start;
        this.goal = goal;
        this.steps = Collections.unmodifiableList(new ArrayList<BridgePlacementStep>(steps));
        this.reachesGoal = reachesGoal;
        this.reason = reason == null ? "" : reason;
    }

    public NavigationCell start() { return start; }
    public NavigationCell goal() { return goal; }
    public List<BridgePlacementStep> steps() { return steps; }
    public int size() { return steps.size(); }
    public BridgePlacementStep step(int index) { return steps.get(index); }
    public boolean reachesGoal() { return reachesGoal; }
    public String reason() { return reason; }
}
