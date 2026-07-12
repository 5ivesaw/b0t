# Bridging Body v0.2

Version: `1.2.0-alpha.0`

## Purpose

The learned brain selects a bridge destination and requests the `BRIDGING` skill. The
bridging body executes only the mechanical part of that intent. It does not choose a
Bedwars objective, target island, purchase strategy, or risk preference.

The body may also be armed manually with `R` for private/local testing. If ordinary
navigation reaches `NO_PATH` or `BLOCKED`, the runtime can hand off automatically when
a bounded support corridor toward the same `G` waypoint contains a placeable gap.

## Mechanical contract

The specialist performs:

- bounded straight or staircase-diagonal corridor generation
- current-position replanning every few client ticks
- one-cardinal-face placement steps only
- full-solid hotbar block discovery
- original-slot preservation and restoration
- enumerate every legal adjacent attachment support
- sample nine hit vectors on every candidate face
- score visible candidates by yaw, pitch, reach, and bridge direction
- visible camera alignment toward the best legal support face
- normal client reach and ray-trace validation
- one deliberate right-click attempt at a time
- world-state placement confirmation
- cautious sneak-held movement onto confirmed support
- stop on missing blocks, blocked feet/head space, invalid support, lost scope, GUI,
  freeze, disable, takeover, emergency stop, or world unload

It never performs:

- packet placement
- silent server-only rotation
- impossible reach
- placement without a support face
- movement into an unconfirmed support cell
- public-server automation

## Corridor model

A diagonal destination is represented as an alternating cardinal staircase. This is
intentional: every new support block must attach through exactly one legal face. The
corridor is bounded by `maximumSteps` and is rebuilt from the player's actual current
feet cell rather than requiring exact historical node visits.

## State machine

The compact HUD exposes these states:

- `PLAN`: bounded current-position corridor prepared
- `ALIGN`: visible yaw/pitch moving toward a support face
- `PLACE`: one legal placement attempt issued
- `CONFIRM`: waiting for the world to expose the new support block
- `CONFIRMED`: support appeared
- `ADVANCE`: sneak-held movement onto confirmed support
- `COMPLETE`: no missing support remains in the bounded corridor
- `AIM_BLOCKED`: ray trace/reach no longer matches the support face
- `OUT_OF_BLOCKS`: no valid full solid hotbar stack exists
- `BLOCKED`: feet/head or support geometry is not legal

Diagnostics include step/plan size, selected slot, attempts, confirmation wait,
placements, failures, replans, retargets, evaluated/visible placement candidates,
current target support, and reason.

The world-space bridge overlay is lifecycle-owned. It is removed immediately on
completion, navigation handoff, intent removal, waypoint change/clear, disable, F9,
F12, physical takeover, world unload, or runtime failure. Rendering is bounded to 16
markers around the current step. A short text result may remain for two seconds, but a
stale bridge plan cannot remain in the world.

## Runtime priority

1. F12 emergency release
2. F9 or physical takeover
3. environment/world/GUI/freeze guards
4. bridging body when a valid bridge intent and missing support exist
5. navigation body
6. low-level model actuator
7. manual control

Only one controller owns continuous input at a time. Release restores physical key
state and the original selected hotbar slot.
