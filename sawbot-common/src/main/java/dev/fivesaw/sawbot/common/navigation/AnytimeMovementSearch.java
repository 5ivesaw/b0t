package dev.fivesaw.sawbot.common.navigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Incremental weighted-A* movement search.
 *
 * The search keeps direction in its state, so reaching one cell from different
 * headings remains a real alternative. It can publish a safe best-so-far path
 * after each bounded expansion slice while continuing to search for a better or
 * complete route. No Minecraft object is referenced here.
 */
public final class AnytimeMovementSearch {
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 1}, {1, 0}, {1, -1},
        {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}
    };
    private static final float SQRT_TWO = 1.41421356F;
    private static final float BASE_TURN_PENALTY = 0.075F;
    private static final int MAX_DEBUG_EDGES = 384;
    private static final int MIN_PUBLISH_EXPANSIONS = 48;

    private final NavigationGrid grid;
    private final NavigationCell start;
    private final NavigationCell goal;
    private final int horizontalRadius;
    private final int verticalRadius;
    private final int maximumExpandedNodes;
    private final float heuristicWeight;
    private final long requestId;
    private final boolean completeGoal;
    private final long startedNanos;
    private final PriorityQueue<Node> open = new PriorityQueue<Node>(256,
        new NodeComparator());
    private final Map<NodeKey, Node> best = new HashMap<NodeKey, Node>();
    private final ArrayDeque<SearchDebugEdge> debugEdges =
        new ArrayDeque<SearchDebugEdge>(MAX_DEBUG_EDGES);

    private long order;
    private int expanded;
    private int lastPublishedExpanded;
    private float lastPublishedHeuristic = Float.POSITIVE_INFINITY;
    private Node bestFrontier;
    private Node goalNode;
    private boolean finished;
    private String terminalReason = "searching";

    public AnytimeMovementSearch(NavigationGrid grid, NavigationCell start,
                                  NavigationCell goal, int horizontalRadius,
                                  int verticalRadius, int maximumExpandedNodes,
                                  float heuristicWeight, long requestId,
                                  boolean completeGoal) {
        if (grid == null || start == null || goal == null) {
            throw new IllegalArgumentException("planner component");
        }
        if (horizontalRadius < 2 || horizontalRadius > 128
            || verticalRadius < 1 || verticalRadius > 32
            || maximumExpandedNodes < 32 || maximumExpandedNodes > 65536
            || heuristicWeight < 1F || heuristicWeight > 2F
            || requestId < 0L) {
            throw new IllegalArgumentException("planner bounds");
        }
        this.grid = grid;
        this.start = start;
        this.goal = goal;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
        this.maximumExpandedNodes = maximumExpandedNodes;
        this.heuristicWeight = heuristicWeight;
        this.requestId = requestId;
        this.completeGoal = completeGoal;
        this.startedNanos = System.nanoTime();

        if (!grid.isStandable(start.x(), start.y(), start.z())) {
            finished = true;
            terminalReason = "start not standable";
            return;
        }
        if (!grid.isStandable(goal.x(), goal.y(), goal.z())) {
            finished = true;
            terminalReason = "goal not standable";
            return;
        }

        Node first = new Node(new NodeKey(start, -1), null, null, 0F,
            heuristic(start, goal), order++);
        first.weightedF = first.h * heuristicWeight;
        open.add(first);
        best.put(first.key, first);
        bestFrontier = first;
    }

    /** Performs at most {@code expansionBudget} node expansions. */
    public SearchUpdate step(int expansionBudget) {
        if (expansionBudget < 1) throw new IllegalArgumentException("expansionBudget");
        if (finished) return terminalUpdate(false);

        int target = Math.min(maximumExpandedNodes,
            expanded + expansionBudget);
        while (!open.isEmpty() && expanded < target
            && expanded < maximumExpandedNodes) {
            Node current = pollBest();
            if (current == null) break;
            current.closed = true;
            expanded++;
            if (betterFrontier(current, bestFrontier)) bestFrontier = current;
            if (current.key.cell.equals(goal)) {
                goalNode = current;
                finished = true;
                terminalReason = "goal reached";
                break;
            }
            expand(current);
        }

        if (!finished && open.isEmpty()) {
            finished = true;
            terminalReason = "open set exhausted";
        } else if (!finished && expanded >= maximumExpandedNodes) {
            finished = true;
            terminalReason = "node budget exhausted";
        }

        Node candidate = goalNode != null ? goalNode : bestFrontier;
        boolean publish = shouldPublish(candidate);
        MovementPlanResult result = null;
        if (publish && candidate != null && candidate.parent != null) {
            boolean complete = goalNode != null && completeGoal;
            MovementPath path = buildPath(candidate, complete);
            result = MovementPlanResult.success(path, expanded, best.size(),
                System.nanoTime() - startedNanos);
            lastPublishedExpanded = expanded;
            lastPublishedHeuristic = candidate.h;
        } else if (finished && (candidate == null || candidate.parent == null)) {
            result = MovementPlanResult.failure(terminalReason, expanded,
                best.size(), System.nanoTime() - startedNanos);
            publish = true;
        }
        return new SearchUpdate(result, publish, finished, terminalReason);
    }

    private void expand(Node current) {
        for (int heading = 0; heading < DIRECTIONS.length; heading++) {
            int dx = DIRECTIONS[heading][0];
            int dz = DIRECTIONS[heading][1];
            boolean diagonal = dx != 0 && dz != 0;
            int x = current.key.cell.x() + dx;
            int z = current.key.cell.z() + dz;
            int nextY = resolveY(grid, x, current.key.cell.y(), z);
            if (nextY == Integer.MIN_VALUE) continue;
            int dy = nextY - current.key.cell.y();
            if (diagonal && dy != 0) continue;
            NavigationCell nextCell = new NavigationCell(x, nextY, z);
            if (!insideBounds(start, nextCell, horizontalRadius, verticalRadius)
                || !grid.canTransition(current.key.cell, nextCell)) continue;

            NavigationMovementType type = movementType(dx, dy, dz);
            float risk = grid.traversalPenalty(nextCell.x(), nextCell.y(),
                nextCell.z());
            if (!Float.isFinite(risk)) continue;
            float moveCost = movementCost(type) + Math.max(0F, risk);
            if (current.key.heading >= 0 && current.key.heading != heading) {
                int turnSteps = circularHeadingDistance(current.key.heading, heading);
                moveCost += BASE_TURN_PENALTY * turnSteps;
            }
            float candidateG = current.g + moveCost;
            NodeKey key = new NodeKey(nextCell, heading);
            Node previous = best.get(key);
            if (previous != null && candidateG >= previous.g - 0.0001F) continue;

            NavigationMovement movement = new NavigationMovement(
                current.key.cell, nextCell, type, moveCost,
                estimatedTicks(type), true);
            float h = heuristic(nextCell, goal);
            Node candidate = new Node(key, current, movement, candidateG, h,
                order++);
            candidate.weightedF = candidateG + h * heuristicWeight;
            best.put(key, candidate);
            open.add(candidate);
            addDebugEdge(current.key.cell, nextCell, false);
            if (betterFrontier(candidate, bestFrontier)) {
                bestFrontier = candidate;
            }
        }
    }

    private boolean shouldPublish(Node candidate) {
        if (candidate == null || candidate.parent == null) return finished;
        if (goalNode != null) return true;
        if (expanded - lastPublishedExpanded < MIN_PUBLISH_EXPANSIONS) return false;
        return !Float.isFinite(lastPublishedHeuristic)
            || candidate.h <= lastPublishedHeuristic - 0.35F
            || expanded - lastPublishedExpanded >= 192
            || finished;
    }

    private SearchUpdate terminalUpdate(boolean published) {
        Node candidate = goalNode != null ? goalNode : bestFrontier;
        if (candidate != null && candidate.parent != null) {
            MovementPath path = buildPath(candidate,
                goalNode != null && completeGoal);
            return new SearchUpdate(MovementPlanResult.success(path, expanded,
                best.size(), System.nanoTime() - startedNanos), true, true,
                terminalReason);
        }
        return new SearchUpdate(MovementPlanResult.failure(terminalReason,
            expanded, best.size(), System.nanoTime() - startedNanos),
            true, true, terminalReason);
    }

    private Node pollBest() {
        while (!open.isEmpty()) {
            Node candidate = open.poll();
            if (best.get(candidate.key) == candidate && !candidate.closed) {
                return candidate;
            }
        }
        return null;
    }

    private MovementPath buildPath(Node terminal, boolean complete) {
        ArrayList<NavigationMovement> reverse =
            new ArrayList<NavigationMovement>();
        for (Node node = terminal; node != null && node.movement != null;
             node = node.parent) {
            reverse.add(node.movement);
        }
        Collections.reverse(reverse);
        return new MovementPath(reverse, terminal.g, expanded, complete,
            requestId);
    }

    private void addDebugEdge(NavigationCell from, NavigationCell to,
                              boolean frontier) {
        if (debugEdges.size() >= MAX_DEBUG_EDGES) debugEdges.removeFirst();
        debugEdges.addLast(new SearchDebugEdge(from, to, frontier));
    }

    public List<SearchDebugEdge> debugEdges() {
        ArrayList<SearchDebugEdge> copy =
            new ArrayList<SearchDebugEdge>(debugEdges.size());
        for (SearchDebugEdge edge : debugEdges) {
            copy.add(new SearchDebugEdge(edge.from(), edge.to(), false));
        }
        Node frontier = bestFrontier;
        if (frontier != null && frontier.parent != null) {
            copy.add(new SearchDebugEdge(frontier.parent.key.cell,
                frontier.key.cell, true));
        }
        return Collections.unmodifiableList(copy);
    }

    public boolean finished() { return finished; }
    public boolean goalReached() { return goalNode != null; }
    public int expandedNodes() { return expanded; }
    public int knownNodes() { return best.size(); }
    public String terminalReason() { return terminalReason; }

    private static int resolveY(NavigationGrid grid, int x, int currentY,
                                int z) {
        if (grid.isStandable(x, currentY, z)) return currentY;
        if (grid.isStandable(x, currentY + 1, z)) return currentY + 1;
        if (grid.isStandable(x, currentY - 1, z)) return currentY - 1;
        return Integer.MIN_VALUE;
    }

    private static boolean insideBounds(NavigationCell origin,
                                        NavigationCell cell,
                                        int horizontalRadius,
                                        int verticalRadius) {
        return Math.abs(cell.x() - origin.x()) <= horizontalRadius
            && Math.abs(cell.z() - origin.z()) <= horizontalRadius
            && Math.abs(cell.y() - origin.y()) <= verticalRadius;
    }

    private static NavigationMovementType movementType(int dx, int dy,
                                                       int dz) {
        if (dy > 0) return NavigationMovementType.ASCEND;
        if (dy < 0) return NavigationMovementType.DESCEND;
        if (dx != 0 && dz != 0) return NavigationMovementType.DIAGONAL;
        return NavigationMovementType.TRAVERSE;
    }

    private static float movementCost(NavigationMovementType type) {
        switch (type) {
            case DIAGONAL: return SQRT_TWO;
            case ASCEND: return 1.62F;
            case DESCEND: return 1.18F;
            default: return 1F;
        }
    }

    private static int estimatedTicks(NavigationMovementType type) {
        switch (type) {
            case ASCEND: return 10;
            case DESCEND: return 7;
            case DIAGONAL: return 6;
            default: return 5;
        }
    }

    private static int circularHeadingDistance(int left, int right) {
        int distance = Math.abs(left - right);
        return Math.min(distance, DIRECTIONS.length - distance);
    }

    private static float heuristic(NavigationCell from, NavigationCell to) {
        int dx = Math.abs(from.x() - to.x());
        int dz = Math.abs(from.z() - to.z());
        int diagonal = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diagonal;
        return diagonal * SQRT_TWO + straight
            + Math.abs(from.y() - to.y()) * 0.85F;
    }

    private static boolean betterFrontier(Node candidate, Node current) {
        return current == null
            || candidate.h < current.h - 0.0001F
            || (Math.abs(candidate.h - current.h) <= 0.0001F
                && candidate.weightedF < current.weightedF - 0.0001F)
            || (Math.abs(candidate.h - current.h) <= 0.0001F
                && Math.abs(candidate.weightedF - current.weightedF) <= 0.0001F
                && candidate.order < current.order);
    }

    private static final class NodeComparator implements Comparator<Node> {
        @Override public int compare(Node left, Node right) {
            int f = Float.compare(left.weightedF, right.weightedF);
            if (f != 0) return f;
            int h = Float.compare(left.h, right.h);
            if (h != 0) return h;
            return Long.compare(left.order, right.order);
        }
    }

    private static final class NodeKey {
        final NavigationCell cell;
        final int heading;

        NodeKey(NavigationCell cell, int heading) {
            this.cell = cell;
            this.heading = heading;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof NodeKey)) return false;
            NodeKey key = (NodeKey)other;
            return heading == key.heading && cell.equals(key.cell);
        }

        @Override public int hashCode() {
            return cell.hashCode() * 31 + heading;
        }
    }

    private static final class Node {
        final NodeKey key;
        final Node parent;
        final NavigationMovement movement;
        final float g;
        final float h;
        final long order;
        float weightedF;
        boolean closed;

        Node(NodeKey key, Node parent, NavigationMovement movement,
             float g, float h, long order) {
            this.key = key;
            this.parent = parent;
            this.movement = movement;
            this.g = g;
            this.h = h;
            this.order = order;
            this.weightedF = g + h;
        }
    }

    public static final class SearchUpdate {
        private final MovementPlanResult result;
        private final boolean publish;
        private final boolean terminal;
        private final String reason;

        SearchUpdate(MovementPlanResult result, boolean publish,
                     boolean terminal, String reason) {
            this.result = result;
            this.publish = publish;
            this.terminal = terminal;
            this.reason = reason;
        }

        public MovementPlanResult result() { return result; }
        public boolean publish() { return publish; }
        public boolean terminal() { return terminal; }
        public String reason() { return reason; }
    }
}
