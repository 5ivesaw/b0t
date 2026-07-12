package dev.fivesaw.sawbot.common.navigation;

/** One bounded planner exploration edge for optional inspector rendering. */
public final class SearchDebugEdge {
    private final NavigationCell from;
    private final NavigationCell to;
    private final boolean frontier;

    public SearchDebugEdge(NavigationCell from, NavigationCell to,
                           boolean frontier) {
        if (from == null || to == null || from.equals(to)) {
            throw new IllegalArgumentException("search edge");
        }
        this.from = from;
        this.to = to;
        this.frontier = frontier;
    }

    public NavigationCell from() { return from; }
    public NavigationCell to() { return to; }
    public boolean frontier() { return frontier; }
}
