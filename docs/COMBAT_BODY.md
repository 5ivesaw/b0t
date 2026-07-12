# Explicit-Target Combat Body v0.1

Version: `1.3.0-alpha.0`

## Responsibility boundary

The combat body is a mechanical specialist. It does not decide whether a fight is wise,
which opponent matters, whether to defend or rush, or when to abandon an objective.
Those decisions remain with the learned brain or the explicit private test harness.

Accepted intent:

- `Skill.PVP`
- a positive tracked-entity ID
- optional tactical objective metadata

The body never replaces a missing target with another entity.

## Target validation

Before claiming controls, the body requires:

- local/private environment guard passes
- current world, player, and observation exist
- selected tracking ID is still present
- kind is `PLAYER`
- relation is neither `SELF` nor `TEAMMATE`
- observation and live Minecraft entity both indicate a valid living target
- target remains inside the bounded local pursuit radius

Targets outside the local radius are reported as `OUT_OF_RANGE`; navigation remains a
separate specialist selected by the brain.

## Motion states

- `ALIGN`: turn visibly before translating when angular error is large
- `APPROACH`: move toward a distant local target, with bounded sprint use
- `SPACING`: retain melee distance while alternating deterministic strafe windows
- `RETREAT`: back away and strafe when excessively close
- `EDGE_GUARD`: cancel or redirect a movement component whose projected support is absent
- `OCCLUDED`: hold no blind combat movement or attack

The planner is deterministic for a given tick, target ID, geometry, and body state so it
can be tested and later imitated or tuned by learned policies without hidden randomness.

## Camera and attack

`HumanRotationController` changes the actual visible player yaw and pitch. Each axis has
configured rate limits and acceleration continuity. Target aim uses a bounded short motion
prediction and a point near the upper body rather than an instantaneous silent rotation.

An attack is allowed only when:

- observation LOS and live `canEntityBeSeen` both pass
- live distance is inside configured normal range
- yaw and pitch error are inside fixed alignment tolerances
- target hurt time permits another meaningful hit
- local attack cooldown has elapsed

The body then invokes the normal Minecraft client player-controller attack and visible
hand swing exactly once for that tick.

## Input ownership

The body owns only the ordinary forward, back, left, right, and sprint bindings while its
intent is active. Release restores physical hardware state. It releases on all safety,
ownership, target, lifecycle, and runtime-failure paths.

## Controls

1. Open the inspector with F7.
2. Select a tracked entity with `[` or `]`.
3. Press Y to arm that exact tracking ID.
4. Press F10 to enable autonomous execution in an allowed local/private environment.
5. Shift+Y clears combat intent. F9 or F12 releases immediately.

This manual control exists only as a mechanical test harness. The runtime brain interface
uses the same explicit target contract.
