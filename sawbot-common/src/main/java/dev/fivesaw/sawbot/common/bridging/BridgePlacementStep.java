package dev.fivesaw.sawbot.common.bridging;

import dev.fivesaw.sawbot.common.navigation.NavigationCell;

/** Immutable desired player-feet cell and the support block that may need placing beneath it. */
public final class BridgePlacementStep {
    private final int index;
    private final NavigationCell feetCell;
    private final NavigationCell supportCell;
    private final BridgeDirection direction;

    public BridgePlacementStep(int index, NavigationCell feetCell, BridgeDirection direction) {
        if (index < 0 || feetCell == null || direction == null) {
            throw new IllegalArgumentException("bridge step");
        }
        this.index = index;
        this.feetCell = feetCell;
        this.supportCell = new NavigationCell(feetCell.x(), feetCell.y() - 1, feetCell.z());
        this.direction = direction;
    }

    public int index() { return index; }
    public NavigationCell feetCell() { return feetCell; }
    public NavigationCell supportCell() { return supportCell; }
    public BridgeDirection direction() { return direction; }

    @Override public String toString() {
        return index + ":" + feetCell + "/" + direction;
    }
}
