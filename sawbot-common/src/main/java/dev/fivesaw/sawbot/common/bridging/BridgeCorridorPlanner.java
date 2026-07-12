package dev.fivesaw.sawbot.common.bridging;

import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic bounded 2D Bresenham corridor.
 *
 * The planner never emits diagonal placement jumps. A diagonal target becomes an
 * alternating cardinal staircase so every new support block can attach to the
 * previous support block through one legal face.
 */
public final class BridgeCorridorPlanner {
    public BridgeCorridorPlan plan(NavigationCell start, NavigationCell goal, int maximumSteps) {
        if (start == null || goal == null) throw new IllegalArgumentException("start/goal");
        if (maximumSteps < 1) throw new IllegalArgumentException("maximumSteps");
        if (Math.abs(goal.y() - start.y()) > 1) {
            return new BridgeCorridorPlan(start, goal,
                Collections.<BridgePlacementStep>emptyList(), false,
                "target elevation exceeds bridge body");
        }

        int x = start.x();
        int z = start.z();
        int targetX = goal.x();
        int targetZ = goal.z();
        int dx = Math.abs(targetX - x);
        int dz = Math.abs(targetZ - z);
        int sx = x < targetX ? 1 : -1;
        int sz = z < targetZ ? 1 : -1;
        int error = dx - dz;
        List<BridgePlacementStep> steps = new ArrayList<BridgePlacementStep>();

        while ((x != targetX || z != targetZ) && steps.size() < maximumSteps) {
            int twiceError = error * 2;
            int nextX = x;
            int nextZ = z;

            // Exactly one axis advances each step. When both are eligible, choose
            // the axis with the larger remaining projection; ties alternate by
            // accumulated error, producing a stable staircase diagonal.
            if (twiceError > -dz && (twiceError >= dx || Math.abs(targetX - x) >= Math.abs(targetZ - z))) {
                error -= dz;
                nextX += sx;
            } else if (twiceError < dx) {
                error += dx;
                nextZ += sz;
            } else {
                error -= dz;
                nextX += sx;
            }

            BridgeDirection direction = BridgeDirection.fromDelta(nextX - x, nextZ - z);
            x = nextX;
            z = nextZ;
            NavigationCell feet = new NavigationCell(x, start.y(), z);
            steps.add(new BridgePlacementStep(steps.size(), feet, direction));
        }

        boolean reachesGoal = x == targetX && z == targetZ;
        return new BridgeCorridorPlan(start, goal, steps, reachesGoal,
            reachesGoal ? "complete corridor" : "bounded partial corridor");
    }
}
