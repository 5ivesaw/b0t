# Deterministic Navigation Body v1.2

Version: `1.2.0-alpha.0`

## Responsibility boundary

The neural brain or local test harness chooses a semantic destination. The navigation
body owns only mechanical execution: route search, route maintenance, movement,
camera control, jump/sprint timing, live safety validation, recovery, and release.

`G` sets waypoint `#1000`. A learned brain may request the same waypoint with
`Skill.NAVIGATION` and `selectedWaypointId=1000`.

## Planner pipeline

Minecraft world access remains on the client thread:

1. resolve actual standable player and goal cells
2. create a validated direct micro-route when a short corridor is already known safe
3. capture a small local immutable grid under a strict per-tick cell budget
4. submit a provisional local request through a bounded queue
5. publish safe best-so-far routes in 48-expansion worker slices
6. continue the same search while the executor begins moving
7. continue a corridor-shaped full snapshot incrementally
8. submit the replacement/full segment to one daemon planner worker
9. reuse the immutable corridor for rolling searches from the live current cell

The worker consumes arrays only. It never receives `World`, player, block-state, or
other Minecraft objects.

## Movement operations

Routes contain explicit operations rather than mandatory centre checkpoints:

- `TRAVERSE`
- `DIAGONAL`
- `ASCEND`
- `DESCEND`

Each operation has start/destination cells, cost, estimated ticks, cancellation safety,
and execution properties. Diagonal operations require both cardinal corner cells to be
safe. Ascent and descent are bounded to one block.

## Current and replacement segments

The coordinator retains:

- active operation path
- current operation index
- staged replacement path
- segment boundaries and remaining operations

A replacement may splice at the player's actual cell or a shared safe future position.
When server correction or displacement moves the player backward/forward on the route,
the cursor rewinds or skips. Nearby displacement may project into a bounded route
corridor. Large displacement discards stale execution and replans from the real cell.

## Continuous execution

The executor does not tap W for every observation. It owns movement bindings across
client ticks until an operation completes or safety releases ownership. Straight and
gentle-turn operations use a live-validated next-operation lookahead point, so movement
flows through a corridor instead of stopping at every block centre.

Camera changes remain visible and bounded. Sprint is used only on low-risk aligned
continuations. Only a planned legal one-block ascent or bounded recovery may request
jump; collision by itself never does. Every movement has a
bounded timeout in addition to the progress watchdog.

## Live world changes

Only the active and near-future operation window is refreshed from the world. If
support, headroom, hazard, or transition geometry changes:

- immediate invalidation releases movement and replans
- later invalidation starts a replacement calculation while the safe current operation
  continues

Persistent bounded LRU caches avoid rereading unchanged cells during repeated snapshots.

## Performance rules

- no graph search on the Minecraft client thread
- one planner worker only
- one queued latest request; obsolete requests are superseded
- six-result bounded output queue for streamed improvements
- immutable worker input
- bounded per-tick snapshot capture
- bounded direction-aware incremental A* nodes and spatial radius
- 48 worker expansions per publication slice
- at most 160 rendered search-debug edges; larger worker history is never walked by the renderer
- corridor-shaped long-range snapshots
- small local request before full request
- bounded live validation window
- route world rendering only while F7 inspector is open
- at most 48 route markers rendered near the active operation
- explicit worker shutdown on runtime shutdown
- route/search diagnostics clear immediately on release, goal change, pause, arrival, or ownership loss

## Release and takeover

F9, F12, physical input, disable, observation freeze, GUI opening, environment denial,
world unload, and runtime shutdown release owned inputs. Physical keyboard state is
restored rather than blindly cleared. The next enable plans from the actual current
position.
