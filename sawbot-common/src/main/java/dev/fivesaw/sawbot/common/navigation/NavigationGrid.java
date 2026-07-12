package dev.fivesaw.sawbot.common.navigation;

/** Client-thread world adapter consumed by the bounded incremental planner. */
public interface NavigationGrid {
    boolean isStandable(int x, int y, int z);
}
