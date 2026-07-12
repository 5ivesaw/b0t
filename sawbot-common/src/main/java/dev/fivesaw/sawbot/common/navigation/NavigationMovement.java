package dev.fivesaw.sawbot.common.navigation;

/**
 * Immutable movement operation between two valid player-feet cells.
 *
 * A path is executed as operations rather than as mandatory block-centre
 * checkpoints. The executor may enter through either valid endpoint, skip
 * already completed operations, or replace the remaining path at a safe
 * cancellation boundary.
 */
public final class NavigationMovement {
    private final NavigationCell from;
    private final NavigationCell to;
    private final NavigationMovementType type;
    private final float cost;
    private final int estimatedTicks;
    private final boolean safeToCancel;

    public NavigationMovement(NavigationCell from, NavigationCell to,
                              NavigationMovementType type, float cost,
                              int estimatedTicks, boolean safeToCancel) {
        if (from == null || to == null || type == null) {
            throw new IllegalArgumentException("movement component");
        }
        if (from.equals(to) || !Float.isFinite(cost) || cost <= 0F
            || estimatedTicks < 1) {
            throw new IllegalArgumentException("movement geometry/cost");
        }
        this.from = from;
        this.to = to;
        this.type = type;
        this.cost = cost;
        this.estimatedTicks = estimatedTicks;
        this.safeToCancel = safeToCancel;
    }

    public NavigationCell from() { return from; }
    public NavigationCell to() { return to; }
    public NavigationMovementType type() { return type; }
    public float cost() { return cost; }
    public int estimatedTicks() { return estimatedTicks; }
    public boolean safeToCancel() { return safeToCancel; }

    public boolean requiresJump() { return type == NavigationMovementType.ASCEND; }

    public boolean allowsSprint() {
        return type == NavigationMovementType.TRAVERSE
            || type == NavigationMovementType.DIAGONAL;
    }

    public boolean containsValidCell(NavigationCell cell) {
        return cell != null && (from.equals(cell) || to.equals(cell));
    }

    public int dx() { return to.x() - from.x(); }
    public int dy() { return to.y() - from.y(); }
    public int dz() { return to.z() - from.z(); }

    @Override public String toString() {
        return type + " " + from + " -> " + to;
    }
}
