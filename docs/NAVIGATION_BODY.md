# Deterministic Navigation Body v0.2

The current implementation is described in `ADAPTIVE_NAVIGATION.md`.

## Goal input

`G` places user waypoint `#1000` above the aimed block. A learned brain may request the
same waypoint with `Skill.NAVIGATION` and `selectedWaypointId=1000`.

F10 may enable the navigation specialist whenever the waypoint exists. This direct mode
is an engineering path; Bedwars strategy remains outside the body.

## Global planner

The bounded anytime A* searches standable player-feet cells. It supports cardinal and
diagonal travel, one-block ascent/descent, deterministic tie-breaking, turn cost,
exposed-edge cost, and hard search bounds.

Search is incremental. A safe frontier route may be followed before the complete route
finishes. A replacement route is hot-swapped while movement continues.

## Local controller

Every client tick the body:

1. projects the real player position onto the current route corridor
2. live-validates future route geometry
3. chooses the farthest safe lookahead node
4. probes several immediate safe headings
5. applies bounded visible camera movement
6. holds movement continuously
7. decides jump/sprint/recovery mechanics
8. measures progress and triggers replanning when necessary

The player is not required to touch every path-node centre.

## Recovery and release

Stalls trigger an alternating strafe/jump recovery and current-position replanning.
F9, F12, physical input, disable, GUI pause, freeze, and world unload release owned
inputs. The next enable never blindly resumes the old path.

## Visibility

The HUD exposes route state, lookahead, deviation, replans, swaps, re-anchors,
invalidations, detours, live reads, and recovery counters. The world overlay distinguishes
the current node, active lookahead, and provisional anytime route.
