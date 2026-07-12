# Action Contract v0.1

Contract identifier: `sawbot.action/0.1`

An `ActionCommand` is generated for one observation sequence. The actuator validates structure, range, age, state, and world/session identity before applying legitimate controls.

## Fields

| Field | Type | Range/meaning |
|---|---|---|
| `schemaVersion` | UTF-8 enum | `sawbot.action/0.1` |
| `observationSequenceNumber` | `i64` nonnegative | exact source snapshot |
| `generatedTimestampNanos` | `i64` | local receive timestamp assigned by the bridge; model clock is not trusted |
| `modelVersion` | UTF-8 max 64 | checkpoint/export identifier |
| `forward` | `f32` | `[-1,+1]` |
| `strafe` | `f32` | `[-1,+1]`, +1 right |
| `yawDeltaDegrees` | `f32` | `[-45,+45]` per command |
| `pitchDeltaDegrees` | `f32` | `[-30,+30]` per command |
| `jumpProbability` | `f32` | `[0,1]` |
| `sprintProbability` | `f32` | `[0,1]` |
| `sneakProbability` | `f32` | `[0,1]` |
| `attackProbability` | `f32` | `[0,1]` |
| `useOrPlaceProbability` | `f32` | `[0,1]` |
| `dropProbability` | `f32` | `[0,1]` |
| `inventoryToggleProbability` | `f32` | `[0,1]`; ignored when unsafe/inapplicable |
| `hotbarSlot` | `i8` | `-1` keep current, otherwise `0..8` |
| `selectedSkill` | enum `u8` | `NONE`, `NAVIGATION`, `BRIDGING`, `PVP`, `EDGE_RECOVERY`, `BLOCK_INTERACTION`, `BED_BREAKING`, `INVENTORY`, `SHOPPING`, `DEFENSIVE_PLACEMENT` |
| `selectedTargetTrackingId` | `i32` | `-1` none; must exist in source snapshot |
| `selectedWaypointId` | `i32` | `-1` none; must exist in source snapshot |
| `confidence` | `f32` | `[0,1]` |
| `actionDurationTicks` | `u8` | `1..4` in v0.1 |
| `tacticalObjective` | enum `u8` | bounded objective enum; `NONE` valid |
| `abortCondition` | enum `u8` | `NONE`, `TIMEOUT`, `TARGET_LOST`, `THREAT_INCREASE`, `LOW_HEALTH`, `NO_ROUTE`, `NO_RESOURCE`, `SKILL_FAILURE`, `MANUAL_TAKEOVER` |

Probabilities use threshold `>=0.5` in the initial actuator. Later stochastic evaluation must be explicit and seeded; the actuator never samples silently.

## Validation

Reject the entire command when any float is NaN/infinite, schema/model metadata is malformed, observation sequence is from a different session, the command references an unavailable target/waypoint, or age exceeds the configured deadline. Numeric values outside allowed ranges are rejected in strict mode; only mouse deltas may be safety-clamped after strict validation to account for per-tick limits.

Default stale policy:

- Motor decision rate: 10 Hz.
- Maximum sequence lag: 3 published snapshots.
- Maximum action age: 250 ms.
- Missing valid action by deadline: apply canonical zero action and release synthetic controls.

## Actuator and specialist authority

The v0.1 command supports two execution modes:

1. **Direct fallback motor command.** When no deterministic specialist owns the selected skill, the safe actuator may apply legitimate movement keys, smooth visible camera deltas, jump/sprint/sneak, attack/use, hotbar selection, a GUI toggle when explicitly safe, and complete input release.
2. **Hierarchical specialist intent.** When `selectedSkill` names an implemented specialist and its required target or waypoint is valid, the specialist owns low-level execution. For example, `NAVIGATION + selectedWaypointId` invokes the navigation body; direct movement and camera fields from that same command are ignored rather than blended into the controller.

A deterministic specialist may perform bounded route search, collision/void checks, path following, camera interpolation, jump timing, legal placement geometry, inventory mechanics, and stuck recovery. It may not invent strategy, replace the objective, select an unrelated target, or conceal the source of the intent.

Always forbidden: teleportation, reach changes, impossible placement, server-only or silent rotation, packet advantage, public-server automation, anti-cheat bypass, or strategic target correction inside a mechanical body.

## Phase 4 execution semantics

The socket worker decodes actions but never touches Minecraft state. The client thread validates and applies accepted actions. Continuous controls use `KeyBinding.setKeyBindState`; attack, use/place, drop, and inventory use ordinary one-shot key ticks; hotbar selection updates the player inventory and controller; yaw/pitch deltas are divided across `actionDurationTicks`.

A command is rejected or released when SawBot is disabled, the snapshot is frozen, the environment is blocked, the model disconnects, a GUI blocks the command, the command is stale, a referenced target/waypoint is missing, or a physical human input is detected.

The bridge assigns `generatedTimestampNanos` at local receipt. Therefore the age deadline is a deterministic local queue/actuation deadline, while observation sequence lag measures policy staleness relative to the world snapshot.
