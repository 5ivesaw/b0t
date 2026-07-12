# Segmented Navigation Core

Version: `1.0.0-alpha.0`

## Purpose

Phase 9 replaces the earlier fixed cell-list navigator with a continuously maintained
movement-operation pipeline. It is designed for rapidly changing private Minecraft
combat environments while remaining deterministic, inspectable, bounded, and safe.

## Data flow

```text
brain/test waypoint
        |
actual player + goal cells
        |
client-thread immutable snapshot capture
        |
bounded latest-wins worker queue
        |
weighted movement A*
        |
provisional/current/replacement segments
        |
live operation validator
        |
movement + camera specialist
        |
legitimate Minecraft bindings
```

## Low-latency behavior

A safe direct micro-route can start immediately. The local snapshot finishes first and
may replace it. The full corridor snapshot and route are calculated afterward without
stopping the current safe path. Future planning begins before the active segment is
consumed.

## Adaptation rules

Every tick, the body compares the real player position with a bounded window around the
active operation:

- exact path cell: continue or advance
- earlier valid cell: rewind
- later valid cell: skip completed operations
- beside route inside corridor tolerance: project/rejoin without returning to an old
  centre
- outside corridor: release stale movement and plan from current position
- staged route overlap: splice replacement at a safe shared position
- changed immediate operation: stop and replan
- changed future operation: continue current safe movement while replacement plans
- no measurable progress: bounded recovery and current-position replan

## Search model

The clean-room movement planner uses weighted A* with deterministic ordering. Costs
include movement length, vertical operations, turns, and edge exposure. Search has
strict radius, vertical, and node bounds. Long destinations are divided into bounded
segments, each planned in a corridor around the direction of the final goal.

## Known Phase 9 limits

This core does not yet support arbitrary block breaking, doors, ladders, parkour,
large drops, water movement, vertical towering, or moving-entity avoidance. Bridging is
owned by the separate Phase 8 specialist. Bedwars tactical choices remain brain work.
