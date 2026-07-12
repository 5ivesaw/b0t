package dev.fivesaw.sawbot.common.navigation;

/** Client-thread world adapter consumed by the bounded incremental planner. */
public interface NavigationGrid {
    boolean isStandable(int x, int y, int z);

    /** Additional soft cost; it may prefer wider/safer cells without forbidding narrow routes. */
    default float traversalPenalty(int x, int y, int z) { return 0F; }

    /** Transition-level legality hook used for live corner and geometry validation. */
    default boolean canTransition(NavigationCell from, NavigationCell to) { return true; }
}
