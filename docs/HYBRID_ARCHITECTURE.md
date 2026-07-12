# Hybrid Brain-and-Body Architecture

SawBotV1 uses a hierarchical hybrid design.

## Learned brain

Learned systems select goals, priorities, targets, tactics, risk, and which specialist
skill should run. A brain may request navigation to waypoint `#1000`, defend a base,
retreat, select an opponent, or choose a bridge destination.

The brain does not need to rediscover graph search, collision geometry, key ownership,
or safe input release.

## Deterministic specialist bodies

Specialists execute known mechanics:

- adaptive local/global route planning
- sustained movement and responsive camera control
- support, collision, liquid, hazard, and void checks
- jumping, recovery, and live route invalidation
- legal bridging placement, confirmation, and cautious advance mechanics
- future inventory/shop interaction
- future combat motor execution

A specialist never chooses Bedwars strategy by itself. It executes a bounded intent
from the user, test harness, or learned brain.

## Runtime priority

1. F12 emergency release
2. F9 or physical human takeover
3. environment, world, GUI, and freeze guards
4. active deterministic specialist
5. fallback low-level Action Contract actuator
6. idle/manual control

Only the owning controller changes a binding. Release restores physical hardware state.

## Navigation body v0.2

The navigation specialist combines:

- bounded anytime A*
- a continuously revised route corridor
- current-position rolling replanning
- active-route hot swapping
- live block/support invalidation
- bounded path re-anchoring after displacement
- safe path smoothing and lookahead
- multi-candidate 20 Hz local steering
- sustained movement, faster bounded camera response, jumping, sprinting, and recovery

This is intentionally analogous to a brain commanding a capable body: the brain says
where and why; the specialist decides how to execute the movement reliably.


## Bridging body v0.1

The bridging specialist accepts a destination/skill intent and performs bounded
mechanical execution: cardinal/staircase corridor generation, block-slot selection,
visible aim, normal-reach support-face placement, world confirmation, sneak-held
advance, replanning, and complete release. It never chooses the strategic destination.
