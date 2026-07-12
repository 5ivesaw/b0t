package dev.fivesaw.sawbot.common.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Deterministic bounded A* over player-feet cells.
 *
 * The planner is stepped with an explicit expansion budget so Minecraft's client
 * thread never performs an unbounded route search in one tick.
 */
public final class IncrementalAStarPlanner {
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 0}, {0, -1}, {-1, 0},
        {1, 1}, {1, -1}, {-1, -1}, {-1, 1}
    };
    private static final float DIAGONAL_COST = 1.41421356F;

    private final PriorityQueue<Node> open = new PriorityQueue<Node>(64, new Comparator<Node>() {
        @Override public int compare(Node left, Node right) {
            int f = Float.compare(left.f, right.f);
            if (f != 0) return f;
            int h = Float.compare(left.h, right.h);
            if (h != 0) return h;
            return Long.compare(left.order, right.order);
        }
    });
    private final Map<NavigationCell, Node> best = new HashMap<NavigationCell, Node>();
    private NavigationGrid grid;
    private NavigationCell start;
    private NavigationCell goal;
    private NavigationPlanState state = NavigationPlanState.IDLE;
    private NavigationPath path;
    private int horizontalRadius;
    private int verticalRadius;
    private int maximumExpandedNodes;
    private int expandedNodes;
    private long insertionOrder;
    private String failureReason = "idle";

    public void begin(NavigationGrid grid, NavigationCell start, NavigationCell goal,
                      int horizontalRadius, int verticalRadius, int maximumExpandedNodes) {
        if (grid == null || start == null || goal == null) throw new IllegalArgumentException("planner component");
        if (horizontalRadius < 2 || horizontalRadius > 128
            || verticalRadius < 1 || verticalRadius > 32
            || maximumExpandedNodes < 32 || maximumExpandedNodes > 65536) {
            throw new IllegalArgumentException("planner bounds");
        }
        this.grid = grid;
        this.start = start;
        this.goal = goal;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.maximumExpandedNodes = maximumExpandedNodes;
        this.expandedNodes = 0;
        this.insertionOrder = 0L;
        this.path = null;
        this.failureReason = "searching";
        this.open.clear();
        this.best.clear();

        if (!grid.isStandable(start.x(), start.y(), start.z())) {
            fail("start not standable");
            return;
        }
        if (!grid.isStandable(goal.x(), goal.y(), goal.z())) {
            fail("goal not standable");
            return;
        }

        float h = heuristic(start, goal);
        Node first = new Node(start, null, 0F, h, insertionOrder++);
        open.add(first);
        best.put(start, first);
        state = NavigationPlanState.SEARCHING;
    }

    /** Expands at most {@code budget} nodes and returns the current state. */
    public NavigationPlanState step(int budget) {
        if (state != NavigationPlanState.SEARCHING) return state;
        int remaining = Math.max(1, budget);
        while (remaining-- > 0 && state == NavigationPlanState.SEARCHING) {
            Node current = pollCurrentBest();
            if (current == null) {
                fail("open set exhausted");
                break;
            }
            current.closed = true;
            expandedNodes++;
            if (current.cell.equals(goal)) {
                succeed(current);
                break;
            }
            if (expandedNodes >= maximumExpandedNodes) {
                fail("node budget exhausted");
                break;
            }
            expand(current);
        }
        return state;
    }

    private Node pollCurrentBest() {
        while (!open.isEmpty()) {
            Node candidate = open.poll();
            Node currentBest = best.get(candidate.cell);
            if (candidate == currentBest && !candidate.closed) return candidate;
        }
        return null;
    }

    private void expand(Node current) {
        for (int index = 0; index < DIRECTIONS.length; index++) {
            int dx = DIRECTIONS[index][0];
            int dz = DIRECTIONS[index][1];
            boolean diagonal = dx != 0 && dz != 0;
            int nextY = resolveY(current.cell.x() + dx, current.cell.y(), current.cell.z() + dz);
            if (nextY == Integer.MIN_VALUE) continue;
            if (diagonal && !diagonalClear(current.cell, dx, dz, nextY)) continue;

            NavigationCell next = new NavigationCell(
                current.cell.x() + dx, nextY, current.cell.z() + dz);
            if (!insideBounds(next)) continue;

            float moveCost = diagonal ? DIAGONAL_COST : 1F;
            int verticalDelta = nextY - current.cell.y();
            if (verticalDelta > 0) moveCost += 0.45F;
            else if (verticalDelta < 0) moveCost += 0.15F;
            float candidateG = current.g + moveCost;

            Node previous = best.get(next);
            if (previous != null && candidateG >= previous.g - 0.0001F) continue;
            float h = heuristic(next, goal);
            Node candidate = new Node(next, current, candidateG, h, insertionOrder++);
            best.put(next, candidate);
            open.add(candidate);
        }
    }

    private int resolveY(int x, int currentY, int z) {
        if (grid.isStandable(x, currentY, z)) return currentY;
        if (grid.isStandable(x, currentY + 1, z)) return currentY + 1;
        if (grid.isStandable(x, currentY - 1, z)) return currentY - 1;
        return Integer.MIN_VALUE;
    }

    private boolean diagonalClear(NavigationCell current, int dx, int dz, int targetY) {
        return canOccupyCardinal(current.x() + dx, current.y(), current.z(), targetY)
            && canOccupyCardinal(current.x(), current.y(), current.z() + dz, targetY);
    }

    private boolean canOccupyCardinal(int x, int currentY, int z, int targetY) {
        if (grid.isStandable(x, targetY, z)) return true;
        return grid.isStandable(x, currentY, z);
    }

    private boolean insideBounds(NavigationCell cell) {
        return Math.abs(cell.x() - start.x()) <= horizontalRadius
            && Math.abs(cell.z() - start.z()) <= horizontalRadius
            && Math.abs(cell.y() - start.y()) <= verticalRadius;
    }

    private void succeed(Node terminal) {
        ArrayList<NavigationCell> reverse = new ArrayList<NavigationCell>();
        for (Node node = terminal; node != null; node = node.parent) reverse.add(node.cell);
        Collections.reverse(reverse);
        path = new NavigationPath(reverse, terminal.g, expandedNodes);
        state = NavigationPlanState.SUCCEEDED;
        failureReason = "none";
    }

    private void fail(String reason) {
        state = NavigationPlanState.FAILED;
        failureReason = reason == null ? "failed" : reason;
        path = null;
        open.clear();
    }

    private static float heuristic(NavigationCell from, NavigationCell to) {
        int dx = Math.abs(from.x() - to.x());
        int dz = Math.abs(from.z() - to.z());
        int diagonal = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diagonal;
        return diagonal * DIAGONAL_COST + straight + Math.abs(from.y() - to.y()) * 0.65F;
    }

    public void reset() {
        grid = null;
        start = null;
        goal = null;
        open.clear();
        best.clear();
        path = null;
        state = NavigationPlanState.IDLE;
        expandedNodes = 0;
        failureReason = "idle";
    }

    public NavigationPlanState state() { return state; }
    public NavigationPath path() { return path; }
    public int expandedNodes() { return expandedNodes; }
    public int openNodes() { return open.size(); }
    public int knownNodes() { return best.size(); }
    public String failureReason() { return failureReason; }

    private static final class Node {
        final NavigationCell cell;
        final Node parent;
        final float g;
        final float h;
        final float f;
        final long order;
        boolean closed;

        Node(NavigationCell cell, Node parent, float g, float h, long order) {
            this.cell = cell;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.order = order;
        }
    }
}
