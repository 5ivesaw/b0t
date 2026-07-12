package dev.fivesaw.sawbot.common.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable path and planner diagnostics. */
public final class NavigationPath {
    private final List<NavigationCell> cells;
    private final float cost;
    private final int expandedNodes;

    public NavigationPath(List<NavigationCell> cells, float cost, int expandedNodes) {
        if (cells == null || cells.isEmpty()) throw new IllegalArgumentException("cells");
        this.cells = Collections.unmodifiableList(new ArrayList<NavigationCell>(cells));
        this.cost = cost;
        this.expandedNodes = Math.max(0, expandedNodes);
    }

    public List<NavigationCell> cells() { return cells; }
    public int size() { return cells.size(); }
    public NavigationCell cell(int index) { return cells.get(index); }
    public float cost() { return cost; }
    public int expandedNodes() { return expandedNodes; }
}
