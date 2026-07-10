# Observation Contract v0.1

Contract identifier: `sawbot.observation/0.1`

The wire contract is deterministic, bounded, little-endian, and independent of Forge classes. Arrays use fixed capacity plus an explicit count. Relative geometry uses the player's yaw-aligned egocentric frame: +Z forward, +X right, +Y up. Floating values are IEEE-754 `f32` unless marked `f64`. Invalid numeric values are never encoded as NaN; validity is represented by flags and canonical zero values.

## Top-level `ObservationSnapshot`

| Field | Portable type | Rule |
|---|---|---|
| `schemaVersion` | UTF-8 enum, max 32 bytes | exactly `sawbot.observation/0.1` |
| `clientTick` | `i64` | monotonically nondecreasing within world session |
| `monotonicTimestampNanos` | `i64` | monotonic process clock, never wall clock |
| `episodeId` | 16 bytes | UUID bytes; all zero when no episode |
| `sequenceNumber` | `i64` nonnegative | strictly increases for published snapshots |
| `worldIdentifier` | UTF-8, max 96 bytes | privacy-safe adapter/world hash, not arbitrary path |
| `taskAdapterIdentifier` | UTF-8, max 48 bytes | `universal` in Phase 1; later `bedwars/0.x` |
| `selfState` | `SelfState` | one record |
| `localTerrain` | `LocalTerrain` | exactly 13×9×13 cells in v0.1 |
| `midRangeMap` | `MidRangeMap` | exactly 33×33 columns in v0.1 |
| `entities` | `EntityObservation[32] + count` | deterministic priority/order |
| `inventory` | `InventoryState` | 45 player slots + armour/cursor metadata |
| `landmarks` | `LandmarkObservation[64] + count` | bounded and confidence-labelled |
| `events` | `EventObservation[64] + count` | oldest-to-newest among retained events |
| `serverTiming` | `ServerTiming` | observations only, no packet manipulation |
| `taskState` | bounded tagged record, max 2048 bytes | universal empty state until adapter exists |
| `previousAction` | `ActionCommandSummary` | last accepted or canonical zero action |
| `sensorValidityFlags` | `u64` bitset | group validity/partial/unknown flags |

## `SelfState`

Uses relative policy features plus tooling-only absolute coordinates. Policy exporters may mask absolute coordinates.

- `health`, `absorption`, `hunger`: `f32`
- `armourPoints`, `armourToughnessEquivalent`: `f32`
- `absoluteX/Y/Z`: `f64`, tooling channel
- `velocityX/Y/Z`, `accelerationX/Y/Z`: `f32`, egocentric
- `yawDegrees`, `pitchDegrees`: `f32`, normalized yaw `[-180,180)`, pitch `[-90,90]`
- booleans: `onGround`, `horizontalCollision`, `verticalCollision`, `inLiquid`, `onLadder`, `insideBlock`, `sprinting`, `sneaking`, `usingItem`
- `airborneTicks`, `hurtTimerTicks`, `attackCooldownTicks`, `currentActionDurationTicks`: `u16`
- `fallDistance`, `supportDistanceLeft/Center/Right`, `distanceToVoid`: `f32`, clamped `[0,64]`, validity flags when unknown
- `selectedSlot`: `u8`, range `0..8`
- `recentServerCorrectionTicks`: `u16`, `65535` means none in retained horizon
- potion effects: maximum 16 records ordered by stable effect ID

## `LocalTerrain`

Dimensions are `[x=13][y=9][z=13]`, centered on the player-foot block with offsets X `-6..+6`, Y `-4..+4`, Z `-6..+6`, then yaw-rotated to the egocentric frame by deterministic nearest cardinal orientation in v0.1. Exact continuous yaw remains in self state.

Each cell uses a compact record:

- `blockCategory`: `u16` stable semantic ID
- `flags`: `u16` bitset for solid, fullBlock, partialBlock, replaceable, liquid, hazard, climbable, interactable, bedComponent, safeSupport, validPlacementSupport, recentlyChanged, loaded, unknown
- `collisionHeightClass`: `u8` (`NONE`, `QUARTER`, `HALF`, `THREE_QUARTER`, `FULL`, `COMPLEX`)
- `teamColourCategory`: `u8`
- `breakTimeCategory`: `u8`
- `shapeIndex`: `u8` into a versioned collision-shape table; `0` means none/unknown

Classification is cached by block state. Collision geometry, not visual model bounds, is authoritative.

## `MidRangeMap`

33×33 egocentric columns, offset `-16..+16`. Each column contains highest walkable relative Y (`i8`), and bit/quantized fields for void, obstruction, safe landing, narrow bridge, platform/island, bed/generator/shop region, team ownership, recent change, known enemy occupancy, route confidence, and loaded/unknown.

## Entities

Maximum 32. Sort key: task relevance descending, threat descending, distance ascending, stable tracking ID ascending. A tracked entity keeps its ID through short occlusion/unload grace; ID reuse is forbidden within an episode.

Every entity includes type/category, stable tracking ID, team relationship, relative position/velocity/acceleration, bounding box, health/armour/held item when available, movement flags, timers, distance, reach relationship, `lineOfSight`, `occluded`, `attackable`, `loaded`, edge/bed distances, recent combat timing, and `trackingConfidence` `[0,1]`.

## Inventory

Stable item categories only. Each slot includes category ID, stack count, durability class, enchantment summary bits, and usability flags. Strings and display names are excluded from policy input.

## Events

Maximum 64, retained by monotonic timestamp. Each contains event type, age in ticks, tracking ID or `-1`, relative position, magnitude, success state, and bounded task metadata. Overflow drops oldest and increments `eventsDroppedSinceEpisodeStart`.

## Determinism and immutability

- Identical client state and adapter configuration must encode byte-identically.
- Constructors defensively copy all arrays.
- Published snapshot objects expose copies or read-only scalar accessors.
- No live `World`, `Entity`, `ItemStack`, `Block`, collection, or mutable buffer reference crosses the client-thread boundary.
- Semantic meaning changes require a minor/major schema bump and migration note.
