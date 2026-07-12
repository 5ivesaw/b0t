package dev.fivesaw.sawbot.common.navigation;

/** Immutable result produced by the background movement planner. */
public final class MovementPlanResult {
    private final MovementPath path;
    private final String failureReason;
    private final int expandedNodes;
    private final int knownNodes;
    private final long computeNanos;

    private MovementPlanResult(MovementPath path, String failureReason,
                               int expandedNodes, int knownNodes,
                               long computeNanos) {
        this.path = path;
        this.failureReason = failureReason == null ? "none" : failureReason;
        this.expandedNodes = Math.max(0, expandedNodes);
        this.knownNodes = Math.max(0, knownNodes);
        this.computeNanos = Math.max(0L, computeNanos);
    }

    public static MovementPlanResult success(MovementPath path, int expandedNodes,
                                             int knownNodes, long computeNanos) {
        if (path == null) throw new IllegalArgumentException("path");
        return new MovementPlanResult(path, "none", expandedNodes, knownNodes,
            computeNanos);
    }

    public static MovementPlanResult failure(String reason, int expandedNodes,
                                             int knownNodes, long computeNanos) {
        return new MovementPlanResult(null, reason, expandedNodes, knownNodes,
            computeNanos);
    }

    public boolean succeeded() { return path != null; }
    public MovementPath path() { return path; }
    public String failureReason() { return failureReason; }
    public int expandedNodes() { return expandedNodes; }
    public int knownNodes() { return knownNodes; }
    public long computeNanos() { return computeNanos; }
}
