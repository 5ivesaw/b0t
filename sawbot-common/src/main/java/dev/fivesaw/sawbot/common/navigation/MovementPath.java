package dev.fivesaw.sawbot.common.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable operation path plus planning diagnostics. */
public final class MovementPath {
    private final List<NavigationMovement> movements;
    private final List<NavigationCell> positions;
    private final float cost;
    private final int expandedNodes;
    private final boolean complete;
    private final long requestId;

    public MovementPath(List<NavigationMovement> movements, float cost,
                        int expandedNodes, boolean complete, long requestId) {
        if (movements == null || movements.isEmpty()) {
            throw new IllegalArgumentException("movements");
        }
        ArrayList<NavigationMovement> movementCopy =
            new ArrayList<NavigationMovement>(movements);
        ArrayList<NavigationCell> positionCopy =
            new ArrayList<NavigationCell>(movementCopy.size() + 1);
        NavigationCell expected = movementCopy.get(0).from();
        positionCopy.add(expected);
        for (NavigationMovement movement : movementCopy) {
            if (!movement.from().equals(expected)) {
                throw new IllegalArgumentException("disconnected movement path");
            }
            expected = movement.to();
            positionCopy.add(expected);
        }
        this.movements = Collections.unmodifiableList(movementCopy);
        this.positions = Collections.unmodifiableList(positionCopy);
        this.cost = cost;
        this.expandedNodes = Math.max(0, expandedNodes);
        this.complete = complete;
        this.requestId = requestId;
    }

    public List<NavigationMovement> movements() { return movements; }
    public List<NavigationCell> positions() { return positions; }
    public int movementCount() { return movements.size(); }
    public int positionCount() { return positions.size(); }
    public NavigationMovement movement(int index) { return movements.get(index); }
    public NavigationCell position(int index) { return positions.get(index); }
    public NavigationCell start() { return positions.get(0); }
    public NavigationCell destination() { return positions.get(positions.size() - 1); }
    public float cost() { return cost; }
    public int expandedNodes() { return expandedNodes; }
    public boolean complete() { return complete; }
    public long requestId() { return requestId; }

    public int indexOfPosition(NavigationCell cell, int startInclusive, int endInclusive) {
        if (cell == null) return -1;
        int start = Math.max(0, startInclusive);
        int end = Math.min(positions.size() - 1, endInclusive);
        for (int index = start; index <= end; index++) {
            if (positions.get(index).equals(cell)) return index;
        }
        return -1;
    }

    public MovementPath slice(int movementStartInclusive, int movementEndExclusive) {
        int start = Math.max(0, movementStartInclusive);
        int end = Math.min(movements.size(), movementEndExclusive);
        if (start >= end) throw new IllegalArgumentException("empty path slice");
        ArrayList<NavigationMovement> slice =
            new ArrayList<NavigationMovement>(movements.subList(start, end));
        float sliceCost = 0F;
        for (NavigationMovement movement : slice) sliceCost += movement.cost();
        return new MovementPath(slice, sliceCost, expandedNodes,
            complete && end == movements.size(), requestId);
    }
}
