# Observation Contract v0.3

Identifier: `sawbot.observation/0.3`
Status: Phase 2 runtime candidate
Compatibility: additive breaking revision of v0.2; consumers must reject mismatched schemas

## Invariants

- Snapshots are created only on Minecraft's client thread.
- Published snapshots contain no live Minecraft `World`, `Entity`, `ItemStack`, `Block`, mutable collection, or mutable array reference.
- Arrays are fixed-size and defensively copied.
- Lists are bounded and unmodifiable.
- Coordinates intended for policy input are egocentric; absolute coordinates remain available for tooling and map adapters.
- No screenshot, framebuffer, OCR, image, video, or armour-colour pixel input exists.
- A snapshot is identified by world/episode, monotonically increasing sequence number, client tick, and monotonic timestamp.
- Schema meaning changes require a version bump and migration note.

## Root `ObservationSnapshot`

| Field | Type | Constraint |
|---|---|---|
| `schemaVersion` | enum | always `sawbot.observation/0.3` |
| `clientTick` | int64 | non-negative |
| `monotonicTimestampNanos` | int64 | non-negative, monotonic-clock domain |
| `episodeId` | UUID | changes when the client world identity changes |
| `sequenceNumber` | int64 | starts at 1 per episode and increases per published snapshot |
| `worldIdentifier` | string | bounded to 96 characters; sanitized world name and dimension |
| `taskAdapterIdentifier` | string | bounded to 48 characters; `universal/0.1` in Phase 1 |
| `selfState` | `SelfState` | required |
| `localTerrain` | `LocalTerrainSnapshot` | required, 1,521 cells |
| `midRangeMap` | `MidRangeMapSnapshot` | required, 1,089 columns |
| `entities` | `EntitySetSnapshot` | required, maximum 32 |
| `inventory` | `InventorySnapshot` | required, exactly 41 slots |
| `landmarks` | `LandmarkSetSnapshot` | required, maximum 64 |
| `events` | `EventHistorySnapshot` | required, maximum 64 |
| `serverTiming` | `ServerTimingSnapshot` | required |
| `taskState` | `TaskStateSnapshot` | required; universal placeholder in Phase 1 |
| `previousAction` | `ActionCommand` | required; zero action until Phase 4 |
| `sensorValidityFlags` | uint64 bit mask | explicit validity per sensor group |
| `sensorTimings` | `SensorTimings` | nanoseconds per sensor group and total |

Default publication interval is two client ticks, approximately 10 snapshots per second at 20 TPS. It is configurable from 1–20 ticks. Frozen mode retains the last immutable snapshot and does not increment the sequence number.

## Self state

Includes health, absorption, hunger, armour, absolute position, egocentric velocity and acceleration estimates, yaw, pitch, fall distance, grounded/collision/liquid/ladder/inside-block flags, sprint/sneak/use state, airborne ticks, hurt timer, selected slot, active-potion count, three foot-support distances, and centre distance-to-support/void probe.

Support probes use Minecraft collision boxes rather than block-name assumptions, allowing slabs, stairs, fences, and other partial geometry to contribute their actual top surfaces.

## Local terrain tensor

Shape: `13 right × 9 up × 13 forward = 1,521 cells`.

The tensor is player-centred and rotated into one of four cardinal egocentric quadrants. Each cell contains:

- Minecraft block-state ID stored as 16-bit bits.
- Stable semantic category enum.
- Bit flags: solid, full, partial, replaceable, liquid, hazard, climbable, interactable, bed, safe support, valid placement support, recently changed, loaded, unknown.
- Collision class: `0 none`, `1 quarter`, `2 half`, `3 three-quarter`, `4 full`, `5 other single box`, `6 compound/multiple boxes`.

Static semantic classifications are cached by state ID. Dynamic replaceability and collision geometry are evaluated against the actual world position. Complex block collision lists are collected through Minecraft's collision API.

## Mid-range map

Shape: `33 × 33 = 1,089 columns`, egocentric around the player.

Each column contains relative walkable surface height, flags, and sample age. Flags currently represent loaded/unknown, void, overhead obstruction, safe landing, narrow-bridge hint, and platform hint.

The map updates two rows per client tick. Samples are stored in a bounded 4,096-entry world-coordinate LRU cache and reprojected when the player moves or rotates, so the map is not discarded on every block step or cardinal turn. A complete stationary sweep takes at most 17 client ticks.

## Entities

Maximum 32 observations. Candidates within 64 blocks are deterministically sorted by semantic priority and stable short-lived tracking ID. Tracks are evicted after a 40-tick absence.

Each observation contains:

- Tracking ID and Minecraft entity ID.
- Broad `EntityKind`, bounded specific `EntityType`, and team relationship. Vanilla types are derived from Minecraft’s registered entity identity, not localized display text.
- Optional `payloadItemCategory` for dropped-item entities; empty for non-payload entities.
- Egocentric relative position and relative velocity.
- Bounding-box width and height.
- Health, armour summary, held-item category, hurt timer.
- Grounded, sprinting, sneaking.
- `lineOfSight`, `occluded`, `attackable`, `loaded`, `trackingConfidence`.

`attackable` requires a living, loaded, non-dead target, current line of sight, and distance no greater than 3.1 blocks. Players without scoreboard-team information are `UNKNOWN`, not automatically classified as enemies. Exact state for occluded loaded entities remains available for controlled private research, while `occluded=true` and `attackable=false` prevent the state from being mistaken for a mechanically valid through-wall attack.

## Inventory

Exactly 41 slots:

- Main inventory/hotbar: 0–35.
- Armour: 36–39.
- Cursor stack: 40.

Each slot has stable item category, numeric item ID, metadata, bounded count, durability class, and enchantment summary bits. The snapshot also contains selected hotbar slot, bounded open-container type, and aggregate iron/gold/diamond/emerald/wool counts. Arbitrary display names are excluded.

## Landmarks and task state

Phase 1 emits the local world spawn as a universal semantic landmark. Bedwars landmarks are deliberately deferred to the Bedwars task adapter. The task state is `universal/0.1`, inactive.

## Events

Maximum 64; overflow drops the oldest event and increments a dropped counter.

Implemented Phase 1 events include damage received, resource collected/spent, entity entered/left range, observed entity hurt-timer transition, local block placed/broken heuristic, large position correction heuristic, and respawn.

Important attribution rule: `ENTITY_HURT_OBSERVED` does **not** claim SawBot caused the damage. `DAMAGE_DEALT` and `HIT_CONFIRMED` remain contract values for a later packet/interaction-correlated implementation and are not fabricated from a target hurt timer.

## Server timing

Includes ping, smoothed ping jitter, ticks since a classified enemy observation, ticks since confirmed hit, ticks since placement acknowledgement, and ticks since server correction. Unknown ages use the bounded sentinel `65535`; ping has a separate validity flag. Phase 1 does not yet observe placement acknowledgements or produce hit confirmations, so those fields honestly remain unknown.

## Validity bits

- bit 0: self
- bit 1: local terrain
- bit 2: mid-range map
- bit 3: entities
- bit 4: inventory
- bit 5: landmarks
- bit 6: events
- bit 7: server timing
- bit 8: task state

## Phase 1/2 acceptance requirements

- Correct full and partial block classifications in a real client.
- Correct void/support behavior at edges, slabs, stairs, and fences.
- Stable entity IDs while entities remain loaded.
- Conservative team classification and explicit occlusion.
- Inventory changes reflected on the expected snapshot.
- Event timing inspected against known actions.
- Snapshot freeze preserves immutable values and sequence number.
- Snapshot age and each sensor's extraction time visible.
- No client-thread exception or unsafe worker access.
- Measured target-hardware cost recorded before Phase 2.

## v0.2 → v0.3 migration

Version 0.3 adds two bounded entity fields: `EntityType type` and `int payloadItemCategory`. It does not change tensor dimensions, inventory dimensions, entity limits, coordinate conventions, action semantics, or sensor-validity bit assignments. Because persistent and IPC consumers must never guess missing fields, v0.2 and v0.3 are intentionally schema-incompatible and a mismatched consumer must reject the snapshot.

## Phase 2 tooling note

Inspector page, overlay visibility, crosshair selection, export queue status, and snapshot differences are development-tool state and are deliberately excluded from the model input.

The human-readable `sawbot.snapshot.debug/0.2` export serializes every bounded v0.3 field without Java serialization, including specific entity type and dropped-item payload category. It is for individual inspection only and is not the Phase 3 high-volume trajectory format.
