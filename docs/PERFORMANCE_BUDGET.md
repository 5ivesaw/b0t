# Performance Budget v0.2

Primary target: HP EliteBook 840 G3, i5-6200U (2C/4T), Intel HD 520, 8 GB RAM, 1366x768, Windows, visible Minecraft 1.8.9.

All figures below are acceptance targets, not measured claims. Phase reports must record mean, p95, p99, maximum, sample count, world/scenario, render distance, FPS cap, JVM flags, and enabled overlays.

## Client-thread budget at 20 TPS

| Component | Mean target | p95 target | Hard warning |
|---|---:|---:|---:|
| Phase 0 tick/key/state handling | 0.05 ms | 0.15 ms | 0.50 ms |
| Self state | 0.05 ms | 0.10 ms | 0.30 ms |
| Local terrain incremental extraction | 0.45 ms | 0.90 ms | 2.00 ms |
| Mid-range map update (amortized) | 0.20 ms | 0.50 ms | 1.50 ms |
| Entity tracking | 0.15 ms | 0.35 ms | 1.00 ms |
| Inventory + events + timing | 0.10 ms | 0.25 ms | 0.75 ms |
| Immutable snapshot assembly | 0.15 ms | 0.35 ms | 1.00 ms |
| Action application | 0.03 ms | 0.10 ms | 0.30 ms |
| Normal HUD | 0.10 ms | 0.35 ms | 1.00 ms |
| Total normal mod client-thread work | 1.00 ms | 2.00 ms | 4.00 ms |

Debug renderers are budgeted separately and individually toggleable. Full collision/tensor inspection may exceed normal HUD budget but must not be enabled by default.

## Background and inference budget

- Motor inference: 10 Hz initial, p50 <= 25 ms, p95 <= 60 ms, deadline 90 ms.
- Tactical inference: 2–4 Hz, p95 <= 80 ms, deadline 150 ms.
- Snapshot serialization: p95 <= 1.5 ms on worker after immutable handoff.
- IPC round trip on localhost: p95 <= 8 ms excluding inference.
- Telemetry writer: no client-thread disk I/O; sustained queue occupancy below 50%.
- Initial exported model: target <= 3 million parameters and <= 16 MB artifact before increasing.

## Bounded capacities

| Structure | Capacity | Overflow policy |
|---|---:|---|
| Recent events in observation | 64 | drop oldest, increment dropped counter |
| Tracked entities | 32 | deterministic priority; expose omitted count |
| Landmarks | 64 | task-adapter priority; expose omitted count |
| Outbound inference snapshots | 8 | keep newest; drop oldest unconsumed |
| Inbound actions | 8 | discard stale sequence first |
| Telemetry policy steps | 2048 | drop newest only after warning; mark episode incomplete |
| Timing samples per metric | 256 | ring overwrite |
| Human-readable debug exports | 32 queued | reject new export with visible warning |

## Memory budget

- Minecraft JVM maximum for target machine: begin at 2.5–3.0 GB, never assume more than 3.5 GB.
- SawBot steady-state Java heap overhead in normal HUD mode: target <= 128 MB.
- Observation snapshots in queues: target <= 32 MB total.
- Every cache must expose size and have an eviction rule.


## Phase 0 accepted measurement

The user-confirmed Phase 0 runtime screenshot on 2026-07-10 showed approximately 10 microseconds average and 1,289 microseconds maximum for the foundation client handler at that captured moment, with no serious observed FPS loss during a five-minute idle test. These values are evidence for Phase 0 only and are not substituted for Phase 1 per-sensor percentiles.

## Phase 1 implementation bounds

- Observation publication defaults to 10 Hz.
- Local terrain is fixed at 1,521 cells per publication.
- Mid-range map storage is fixed at 1,089 output columns and a 4,096-column LRU cache; only two map rows are refreshed per client tick. Recent known surfaces are revalidated in a ±4-block band, with a complete bounded column rescan forced at least every 100 client ticks.
- Entities are capped at 32, events at 64, landmarks at 64, inventory slots at 41.
- All extraction remains on the client thread; no worker reads live Minecraft objects.
- F7 textual inspector is optional. Phase 2 world overlays do not exist yet.

## Phase 2 inspector budget

| Component | Default | Hard bound | Target on HP 840 G3 |
|---|---:|---:|---:|
| World overlays | Off | Four independent toggles | Zero cost when off except one branch |
| Terrain outlines | Off | 256 boxes/frame | Measure; disable independently if >2 ms avg |
| Collision outlines | Off | 256 boxes/frame | Measure; disable independently if >2 ms avg |
| Entity boxes/labels | Off | 32 entities | Measure; target <1 ms in MVP arena |
| Landmark markers | Off | 64 landmarks | Target <0.5 ms |
| Snapshot comparison | Snapshot publication only | 1,521 terrain + 1,089 map + 32 entities + 41 slots | Target <1 ms at 10 Hz |
| Debug export queue | User-triggered | 4 immutable requests | Reject newest when full |
| Export workers | One daemon | Exactly one | No Minecraft world access |
| Debug JSON | One snapshot/file | Bounded by observation contract | Never used as Phase 3 trajectory format |

The System inspector page reports world-render average/maximum time. Runtime acceptance must compare overlay-off FPS with Phase 1 and measure each overlay separately before combined use.

## Phase 4 bridge and actuator budgets

| Component | Bound | Client-thread rule |
|---|---:|---|
| Observation publication | queue capacity 2 | `offer` only; never wait for socket/model |
| Action reception | queue capacity 8 | poll newest; discard superseded actions |
| Bridge payload | 262,144 bytes | encode bounded immutable snapshot only |
| Connect/read timeout | 500 ms / 100 ms defaults | worker thread only |
| Action age | 250 ms default | reject and release after deadline |
| Source lag | 3 observations default | reject stale policy output |
| Action duration | 1–4 ticks | bounded camera/key authority |
| Sent timestamp cache | 32 entries | bounded latency bookkeeping |

The target client-thread actuator budget is below 100 microseconds per tick excluding ordinary Minecraft key handling. Socket and model inference time are not part of the client-thread critical path.
