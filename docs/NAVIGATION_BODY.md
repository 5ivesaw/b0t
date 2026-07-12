# Deterministic Navigation Body v0.1

## Goal input

`G` places user waypoint `#1000` above the aimed block. A learned brain may request the same waypoint with `Skill.NAVIGATION` and `selectedWaypointId=1000`.

F10 may enable this body whenever the waypoint exists, even if the external brain is offline. This direct mode is an engineering test path, not a strategic policy.

## Planner

The route planner searches player-feet cells. A cell is usable only when:

- feet and head cells have no collision
- feet/head are not liquid or hazardous
- the cell below provides safe non-liquid, non-hazard support
- the cell is loaded

The planner supports cardinal and diagonal movement, one-block ascent/descent, deterministic tie-breaking, and a hard node budget. Search work is spread across client ticks.

## Follower

The follower continuously owns movement until release instead of sending repeated short W actions. It turns toward the next node, waits for adequate alignment, sprints on straight safe segments, jumps for step-ups or horizontal collision, and advances through reached nodes.

Five consecutive in-radius ticks are required for `ARRIVED`.

## Recovery

If commanded movement produces less than 0.12 metres of horizontal progress during the configured stuck window, the body:

1. releases its owned inputs
2. replans from the current player cell
3. enters a short jump/alternating-strafe recovery
4. increments visible replan and stuck counters

## Visibility

The normal HUD displays:

- `PLANNING`, `FOLLOW`, `RECOVER`, `ARRIVED`, or `NO_PATH`
- path index and path length
- planner expanded/open nodes
- block-state read count
- replan and stuck counts
- current reason

The world renderer draws the complete path and highlights the current node.
