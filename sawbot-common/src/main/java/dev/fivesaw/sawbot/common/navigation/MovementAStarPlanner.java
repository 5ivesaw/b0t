package dev.fivesaw.sawbot.common.navigation;

/**
 * Synchronous compatibility facade over the incremental anytime search.
 * Runtime planning uses {@link AnytimeMovementSearch} directly so best-so-far
 * paths can be streamed while search continues.
 */
public final class MovementAStarPlanner {
    private static final int EXPANSIONS_PER_SLICE = 192;

    public MovementPlanResult plan(NavigationGrid grid, NavigationCell start,
                                   NavigationCell goal, int horizontalRadius,
                                   int verticalRadius, int maximumExpandedNodes,
                                   float heuristicWeight, long requestId,
                                   boolean complete) {
        AnytimeMovementSearch search = new AnytimeMovementSearch(grid, start,
            goal, horizontalRadius, verticalRadius, maximumExpandedNodes,
            heuristicWeight, requestId, complete);
        MovementPlanResult latest = null;
        while (!search.finished()) {
            AnytimeMovementSearch.SearchUpdate update =
                search.step(EXPANSIONS_PER_SLICE);
            if (update.publish() && update.result() != null) {
                latest = update.result();
            }
        }
        AnytimeMovementSearch.SearchUpdate terminal = search.step(1);
        if (terminal.result() != null) latest = terminal.result();
        if (latest == null) {
            return MovementPlanResult.failure(search.terminalReason(),
                search.expandedNodes(), search.knownNodes(), 0L);
        }
        if (complete && !search.goalReached()) {
            return MovementPlanResult.failure(search.terminalReason(),
                search.expandedNodes(), search.knownNodes(),
                latest.computeNanos());
        }
        return latest;
    }
}
