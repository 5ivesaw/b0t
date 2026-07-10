# Performance Budget v0.1

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
