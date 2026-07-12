# Continuous Anytime Navigation

Version: `1.1.0-alpha.0`

SawBot navigation is a receding-horizon body controller. The learned brain owns the
objective; the deterministic navigation body continuously chooses and refines legal
movement operations while the player is already moving.

## Search contract

- Minecraft world access remains on the client thread.
- Client-thread capture produces bounded immutable navigation grids.
- The planner worker expands weighted A* in 48-node slices.
- Direction is part of the search state. Reaching one cell from different headings
  remains a distinct candidate instead of being discarded prematurely.
- Each slice may publish a safe best-so-far movement path.
- Search continues after a provisional path is published.
- Later improvements are staged and spliced into the active path at a shared current
  or near-future cell.
- Completed and partial paths are explicitly distinguished.
- One request, one worker, bounded queues, bounded node count, bounded debug edges,
  and explicit shutdown are retained.

## Receding horizon

The most recent immutable corridor is reused for rolling searches from the player's
actual feet cell. A new bounded search is submitted after the configured replan
interval when the worker is free. This means the body does not wait for an entire
new world capture merely because the player advanced, was knocked, or was moved.

A new capture is required only when:

- the player leaves the retained immutable corridor;
- the waypoint changes;
- live validation detects changed support/headroom/transition geometry;
- the current segment cannot continue safely.

## Movement legality

- Vertical movement is limited to one block per operation.
- Vertical diagonal operations are rejected.
- A two-block wall is never treated as a jumpable ascent.
- Collision alone never presses jump.
- Jump is pressed only for a planned legal one-block ascent or bounded stuck recovery.
- Every near-future operation is refreshed against the live world before execution.
- Unsupported, hazardous, liquid, blocked-headroom, and illegal corner transitions
  remain excluded.

## Continuous execution

Route cells are not mandatory stop points. Straight and gentle-turn sequences aim
through a validated lookahead corridor. Intermediate proximity no longer releases W;
operations advance from the player's real feet cell and movement stays held through
compatible operations.

## Inspector rendering

With F7 enabled, the world overlay draws:

- low-opacity bounded search exploration edges;
- a bright current best frontier edge;
- the accepted route;
- current and lookahead operations.

Search rendering is capped at 384 retained edges and is disabled when the inspector
is hidden.

## Performance boundary

- 48 expansions per worker slice.
- Maximum 384 retained search edges.
- Latest-wins request queue of one.
- Bounded result queue of six.
- No planner wait on the Minecraft client thread.
- Existing cached world reads and incremental snapshot capture remain in force.
