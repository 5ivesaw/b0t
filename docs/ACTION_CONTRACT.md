# Action Contract v0.1

Contract identifier: `sawbot.action/0.1`

An `ActionCommand` is generated for one observation sequence. The actuator validates structure, range, age, state, and world/session identity before applying legitimate controls.

## Fields

| Field | Type | Range/meaning |
|---|---|---|
| `schemaVersion` | UTF-8 enum | `sawbot.action/0.1` |
| `observationSequenceNumber` | `i64` nonnegative | exact source snapshot |
| `generatedTimestampNanos` | `i64` | model monotonic clock translated/validated by bridge |
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

## Actuator authority

Allowed: legitimate movement keys, smooth mouse-equivalent camera deltas, jump/sprint/sneak, attack/use, hotbar selection, GUI toggle when explicitly safe, and complete input release.

Forbidden: teleport, reach changes, impossible placement, server-only rotation, packet advantage, strategic target correction, runtime pathfinding/aiming/scaffold, or silently rescuing a bad neural action.
