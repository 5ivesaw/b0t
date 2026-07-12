package dev.fivesaw.sawbot.common.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Weighted A* over explicit movement primitives.
 *
 * This planner is intentionally world-agnostic and consumes only an immutable
 * {@link NavigationGrid}. It is safe to run on the bounded planner worker.
 */
public final class MovementAStarPlanner {
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 0}, {0, -1}, {-1, 0},
        {1, 1}, {1, -1}, {-1, -1}, {-1, 1}
    };
    private static final float SQRT_TWO = 1.41421356F;
    private static final float TURN_PENALTY = 0.06F;

    public MovementPlanResult plan(NavigationGrid grid, NavigationCell start,
                                   NavigationCell goal, int horizontalRadius,
                                   int verticalRadius, int maximumExpandedNodes,
                                   float heuristicWeight, long requestId,
                                   boolean complete) {
        long started = System.nanoTime();
        if (grid == null || start == null || goal == null) {
            throw new IllegalArgumentException("planner component");
        }
        if (horizontalRadius < 2 || horizontalRadius > 128
            || verticalRadius < 1 || verticalRadius > 32
            || maximumExpandedNodes < 32 || maximumExpandedNodes > 65536
            || heuristicWeight < 1F || heuristicWeight > 2F) {
            throw new IllegalArgumentException("planner bounds");
        }
        if (!grid.isStandable(start.x(), start.y(), start.z())) {
            return MovementPlanResult.failure("start not standable", 0, 0,
                System.nanoTime() - started);
        }
        if (!grid.isStandable(goal.x(), goal.y(), goal.z())) {
            return MovementPlanResult.failure("goal not standable", 0, 0,
                System.nanoTime() - started);
        }

        PriorityQueue<Node> open = new PriorityQueue<Node>(128,
            new NodeComparator());
        Map<NavigationCell, Node> best = new HashMap<NavigationCell, Node>();
        long order = 0L;
        Node first = new Node(start, null, null, 0F,
            heuristic(start, goal), order++, 0, 0);
        open.add(first);
        best.put(start, first);
        Node bestFrontier = first;
        int expanded = 0;

        while (!open.isEmpty() && expanded < maximumExpandedNodes) {
            Node current = pollBest(open, best);
            if (current == null) break;
            current.closed = true;
            expanded++;
            if (betterFrontier(current, bestFrontier)) bestFrontier = current;
            if (current.cell.equals(goal)) {
                MovementPath path = buildPath(current, expanded, true, requestId);
                return MovementPlanResult.success(path, expanded, best.size(),
                    System.nanoTime() - started);
            }

            for (int[] direction : DIRECTIONS) {
                int dx = direction[0];
                int dz = direction[1];
                boolean diagonal = dx != 0 && dz != 0;
                int x = current.cell.x() + dx;
                int z = current.cell.z() + dz;
                int nextY = resolveY(grid, x, current.cell.y(), z);
                if (nextY == Integer.MIN_VALUE) continue;
                if (diagonal && nextY != current.cell.y()) continue;
                NavigationCell next = new NavigationCell(x, nextY, z);
                if (!insideBounds(start, next, horizontalRadius, verticalRadius)
                    || !grid.canTransition(current.cell, next)) continue;

                NavigationMovementType type = movementType(dx, nextY - current.cell.y(), dz);
                float moveCost = movementCost(type);
                if (current.parent != null
                    && (current.stepDx != dx || current.stepDz != dz)) {
                    moveCost += TURN_PENALTY;
                }
                float risk = grid.traversalPenalty(next.x(), next.y(), next.z());
                if (!Float.isFinite(risk)) continue;
                moveCost += Math.max(0F, risk);
                float candidateG = current.g + moveCost;
                Node previous = best.get(next);
                if (previous != null && candidateG >= previous.g - 0.0001F) continue;

                NavigationMovement movement = new NavigationMovement(current.cell,
                    next, type, moveCost, estimatedTicks(type), true);
                float h = heuristic(next, goal);
                Node candidate = new Node(next, current, movement, candidateG,
                    h, order++, dx, dz);
                candidate.weightedF = candidateG + h * heuristicWeight;
                best.put(next, candidate);
                open.add(candidate);
                if (betterFrontier(candidate, bestFrontier)) bestFrontier = candidate;
            }
        }

        if (!complete && bestFrontier != null && bestFrontier.parent != null) {
            MovementPath path = buildPath(bestFrontier, expanded, false, requestId);
            return MovementPlanResult.success(path, expanded, best.size(),
                System.nanoTime() - started);
        }
        String failure = expanded >= maximumExpandedNodes
            ? "node budget exhausted" : "open set exhausted";
        return MovementPlanResult.failure(failure, expanded, best.size(),
            System.nanoTime() - started);
    }

    private static Node pollBest(PriorityQueue<Node> open,
                                 Map<NavigationCell, Node> best) {
        while (!open.isEmpty()) {
            Node candidate = open.poll();
            if (best.get(candidate.cell) == candidate && !candidate.closed) {
                return candidate;
            }
        }
        return null;
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

    private static int resolveY(NavigationGrid grid, int x, int currentY, int z) {
        if (grid.isStandable(x, currentY, z)) return currentY;
        if (grid.isStandable(x, currentY + 1, z)) return currentY + 1;
        if (grid.isStandable(x, currentY - 1, z)) return currentY - 1;
        return Integer.MIN_VALUE;
    }

    private static boolean insideBounds(NavigationCell start, NavigationCell cell,
                                        int horizontalRadius, int verticalRadius) {
        return Math.abs(cell.x() - start.x()) <= horizontalRadius
            && Math.abs(cell.z() - start.z()) <= horizontalRadius
            && Math.abs(cell.y() - start.y()) <= verticalRadius;
    }

    private static NavigationMovementType movementType(int dx, int dy, int dz) {
        if (dy > 0) return NavigationMovementType.ASCEND;
        if (dy < 0) return NavigationMovementType.DESCEND;
        if (dx != 0 && dz != 0) return NavigationMovementType.DIAGONAL;
        return NavigationMovementType.TRAVERSE;
    }

    private static float movementCost(NavigationMovementType type) {
        switch (type) {
            case DIAGONAL: return SQRT_TWO;
            case ASCEND: return 1.46F;
            case DESCEND: return 1.16F;
            default: return 1F;
        }
    }

    private static int estimatedTicks(NavigationMovementType type) {
        switch (type) {
            case ASCEND: return 9;
            case DESCEND: return 7;
            case DIAGONAL: return 6;
            default: return 5;
        }
    }

    private static float heuristic(NavigationCell from, NavigationCell to) {
        int dx = Math.abs(from.x() - to.x());
        int dz = Math.abs(from.z() - to.z());
        int diagonal = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diagonal;
        return diagonal * SQRT_TWO + straight
            + Math.abs(from.y() - to.y()) * 0.72F;
    }

    private static MovementPath buildPath(Node terminal, int expanded,
                                          boolean complete, long requestId) {
        ArrayList<NavigationMovement> reverse = new ArrayList<NavigationMovement>();
        for (Node node = terminal; node != null && node.movement != null;
             node = node.parent) {
            reverse.add(node.movement);
        }
        Collections.reverse(reverse);
        return new MovementPath(reverse, terminal.g, expanded, complete, requestId);
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

    private static final class Node {
        final NavigationCell cell;
        final Node parent;
        final NavigationMovement movement;
        final float g;
        final float h;
        final long order;
        final int stepDx;
        final int stepDz;
        float weightedF;
        boolean closed;

        Node(NavigationCell cell, Node parent, NavigationMovement movement,
             float g, float h, long order, int stepDx, int stepDz) {
            this.cell = cell;
            this.parent = parent;
            this.movement = movement;
            this.g = g;
            this.h = h;
            this.order = order;
            this.stepDx = stepDx;
            this.stepDz = stepDz;
            this.weightedF = g + h;
        }
    }
}
