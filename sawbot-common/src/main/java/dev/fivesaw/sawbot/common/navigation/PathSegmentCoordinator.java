package dev.fivesaw.sawbot.common.navigation;

import java.util.Collections;
import java.util.List;

/**
 * Maintains the active operation path and a staged replacement path.
 *
 * Replacements are spliced at the player's real current cell when possible.
 * This permits knockback, manual displacement, teleports, and planning-ahead
 * without forcing a return to an obsolete block-centre checkpoint.
 */
public final class PathSegmentCoordinator {
    private MovementPath active;
    private MovementPath staged;
    private int movementIndex;
    private int segmentLength = 24;
    private int splices;
    private int reanchors;
    private int rewinds;
    private int skips;
    private int discardedStaged;
    private int corridorRecoveries;

    public void setSegmentLength(int value) {
        if (value < 4 || value > 128) throw new IllegalArgumentException("segmentLength");
        segmentLength = value;
    }

    public void install(MovementPath path, NavigationCell feet) {
        if (path == null) throw new IllegalArgumentException("path");
        active = path;
        staged = null;
        movementIndex = 0;
        reconcile(feet, 0, Math.min(path.positionCount() - 1, 16));
    }

    public void stage(MovementPath path) {
        if (path == null) throw new IllegalArgumentException("path");
        if (active == null) {
            active = path;
            movementIndex = 0;
            return;
        }
        staged = path;
    }

    public boolean trySplice(NavigationCell feet) {
        if (staged == null || feet == null) return false;
        int stagedPosition = staged.indexOfPosition(feet, 0,
            Math.min(staged.positionCount() - 1, 20));
        if (stagedPosition < 0) {
            NavigationCell currentDestination = currentMovement() == null
                ? feet : currentMovement().to();
            stagedPosition = staged.indexOfPosition(currentDestination, 0,
                Math.min(staged.positionCount() - 1, 24));
        }
        if (stagedPosition < 0) return false;
        active = staged;
        staged = null;
        movementIndex = Math.min(stagedPosition, active.movementCount());
        splices++;
        return true;
    }

    public boolean reconcile(NavigationCell feet, int maximumBacktrackPositions,
                             int maximumForwardPositions) {
        if (active == null || feet == null) return false;
        int positionIndex = Math.min(movementIndex, active.positionCount() - 1);
        int start = Math.max(0, positionIndex - Math.max(0, maximumBacktrackPositions));
        int end = Math.min(active.positionCount() - 1,
            positionIndex + Math.max(0, maximumForwardPositions));
        int matched = active.indexOfPosition(feet, start, end);
        if (matched < 0) return false;
        int old = movementIndex;
        movementIndex = Math.min(matched, active.movementCount());
        if (movementIndex != old) {
            reanchors++;
            if (movementIndex > old) skips += movementIndex - old;
            else rewinds += old - movementIndex;
        }
        return true;
    }

    /**
     * Reconciles to the nearest route position inside a continuous corridor.
     *
     * Exact cell membership is preferred, but knockback and human displacement
     * commonly leave the player beside a path rather than exactly on one of its
     * centres. This bounded projection prevents pointless returns to old nodes
     * while still refusing large deviations that require a new plan.
     */
    public boolean reconcileNearby(double playerX, double playerY, double playerZ,
                                   double maximumDistance,
                                   int maximumBacktrackPositions,
                                   int maximumForwardPositions) {
        if (active == null || maximumDistance <= 0D) return false;
        int positionIndex = Math.min(movementIndex, active.positionCount() - 1);
        int start = Math.max(0, positionIndex - Math.max(0, maximumBacktrackPositions));
        int end = Math.min(active.positionCount() - 1,
            positionIndex + Math.max(0, maximumForwardPositions));
        double maximumSquared = maximumDistance * maximumDistance;
        double bestSquared = Double.POSITIVE_INFINITY;
        int bestIndex = -1;
        for (int index = start; index <= end; index++) {
            NavigationCell candidate = active.position(index);
            double dx = candidate.centerX() - playerX;
            double dz = candidate.centerZ() - playerZ;
            double dy = (candidate.centerY() - playerY) * 0.65D;
            double distanceSquared = dx * dx + dz * dz + dy * dy;
            if (distanceSquared <= maximumSquared
                && distanceSquared < bestSquared - 0.000001D) {
                bestSquared = distanceSquared;
                bestIndex = index;
            }
        }
        if (bestIndex < 0) return false;
        int old = movementIndex;
        movementIndex = Math.min(bestIndex, active.movementCount());
        corridorRecoveries++;
        if (movementIndex != old) {
            reanchors++;
            if (movementIndex > old) skips += movementIndex - old;
            else rewinds += old - movementIndex;
        }
        return true;
    }

    public boolean advanceIfAt(NavigationCell feet) {
        NavigationMovement movement = currentMovement();
        if (movement == null || feet == null || !movement.to().equals(feet)) return false;
        movementIndex++;
        return true;
    }

    public void advance() {
        if (active != null && movementIndex < active.movementCount()) movementIndex++;
    }

    public void discardStaged() {
        if (staged != null) discardedStaged++;
        staged = null;
    }

    public void clear() {
        active = null;
        staged = null;
        movementIndex = 0;
    }

    public MovementPath activePath() { return active; }
    public MovementPath stagedPath() { return staged; }
    public NavigationMovement currentMovement() {
        return active == null || movementIndex >= active.movementCount()
            ? null : active.movement(movementIndex);
    }
    public int movementIndex() { return movementIndex; }
    public int remainingMovements() {
        return active == null ? 0 : Math.max(0, active.movementCount() - movementIndex);
    }
    public boolean finished() {
        return active != null && movementIndex >= active.movementCount();
    }
    public boolean hasActivePath() { return active != null; }
    public boolean hasStagedPath() { return staged != null; }
    public int currentSegmentIndex() { return movementIndex / segmentLength; }
    public int indexInsideSegment() { return movementIndex % segmentLength; }
    public int segmentLength() { return segmentLength; }
    public int totalSegments() {
        if (active == null) return 0;
        return (active.movementCount() + segmentLength - 1) / segmentLength;
    }
    public boolean nextSegmentAvailable() {
        return active != null && currentSegmentIndex() + 1 < totalSegments();
    }
    public List<NavigationCell> activePositions() {
        return active == null ? Collections.<NavigationCell>emptyList() : active.positions();
    }
    public int splices() { return splices; }
    public int reanchors() { return reanchors; }
    public int rewinds() { return rewinds; }
    public int skips() { return skips; }
    public int discardedStaged() { return discardedStaged; }
    public int corridorRecoveries() { return corridorRecoveries; }
}
