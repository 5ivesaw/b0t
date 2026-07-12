# Adaptive Navigation Body v0.2

Version: `0.8.0-alpha.0`

## Purpose

Phase 7 replaces rigid path-node following with a continuously revised route corridor.
The brain still chooses the destination and skill. The navigation body evaluates and
executes movement mechanics at Minecraft's 20 Hz client-tick rate.

A route is no longer interpreted as "touch every block centre in this exact order."
It is a safe global corridor that the local controller may re-anchor within, smooth,
and temporarily detour around while preserving progress toward the selected goal.

## Control rates

- Local geometry validation and steering: every client tick, up to 20 Hz.
- Observation/model decisions: unchanged, normally 10 Hz.
- Rolling current-position route refresh: default every 4 client ticks when useful.
- Anytime A* expansion budget: default 96 nodes per client tick.

The local body therefore continues reacting between neural decisions.

## Anytime global planning

`IncrementalAStarPlanner` remains bounded and deterministic, but now exposes its best
current frontier path while the complete goal route is still being searched. The body
may begin following this provisional route immediately and atomically replace it with
the completed route later.

Movement cost includes:

- cardinal/diagonal distance
- ascent/descent cost
- a small direction-change cost to reduce zig-zag routes
- soft exposed-edge penalties supplied by the world grid

Narrow bridges remain legal; wider supported routes are preferred when their total
cost is competitive.

## Rolling replanning

The active route is retained while a replacement plan searches from the player's
actual current cell. This prevents routine replanning from producing stop/start input
pulses.

A new plan is requested when:

- the waypoint changes
- autonomy is re-enabled after human takeover
- the player moves materially away from the previous planning origin
- the active route becomes provisional
- live block/support validation changes
- the player leaves the route corridor
- every immediate local steering probe is blocked
- progress stalls

A successful replacement is hot-swapped without dropping held movement.

## Path corridor and re-anchoring

`AdaptivePathCursor` projects the player onto nearby route nodes around the existing
cursor. It may advance across several nodes or backtrack a small bounded amount after:

- knockback
- imperfect movement
- falling/landing
- manual displacement followed by re-enable
- skipping a safe straight segment

If corridor deviation exceeds the configured off-route distance, the old route is not
forced. A new route starts from the player's real current position.

## Safe lookahead

The follower selects the farthest future path node reachable by a live-validated
straight corridor, bounded by node count and metric distance. Every sampled feet cell
must remain standable and every cell transition must remain legal.

This allows smooth diagonal/straight travel without cutting through walls, unsupported
gaps, hazards, or illegal diagonal corners.

## 20 Hz reactive steering

For each client tick, the body probes several headings around the desired lookahead
heading. Candidate scoring considers:

- distance from the active route corridor
- remaining distance to the lookahead target
- exposed-edge risk penalty
- steering offset
- current camera turn requirement

The best safe candidate controls the visible camera and sustained movement. If no
candidate is safe, movement is released and a current-position route is requested.

## Live world invalidation

Planner caches are bounded. Future route validation explicitly refreshes the block
cells it depends on, so placed/broken blocks, lost support, liquids, hazards, and
changed diagonal corners can invalidate a route before the player reaches them.

Immediate invalidations stop movement. More distant invalidations trigger a hot
replacement plan while the still-valid prefix continues.

## Human control

F9, F12, physical takeover, GUI pause, disable, freeze, and world unload remain above
navigation in the control priority order. Release restores each binding to its real
hardware state.

After any external release, the next enable always plans from the player's current
position rather than resuming a stale cursor.

## Diagnostics

The compact HUD and MODEL page expose:

- current status: `PLANNING`, `ANYTIME`, `FOLLOW`, `FOLLOW+REPLAN`, `DETOUR`,
  `RECOVER`, `REPLAN`, `ARRIVED`, or `NO_PATH`
- route node, lookahead node, and route size
- corridor deviation and steering offset
- replans, hot swaps, re-anchors, invalidations, off-route replans, local detours,
  and stuck recoveries
- planner expanded/open/known nodes
- planner and live-grid reads/refreshes
- provisional/full route state

The renderer highlights the current node, lookahead node, and provisional route.
