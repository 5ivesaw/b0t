# Phase 1 Runtime Acceptance

Date accepted: 2026-07-10  
Accepted candidate: `0.2.0-alpha.2`  
Target: HP EliteBook 840 G3, Windows, visible Minecraft Forge 1.8.9 client

## User-confirmed runtime evidence

- [x] GitHub produced an installable Phase 1 JAR and Minecraft launched with it.
- [x] The observation sequence advanced at the intended approximately 10 Hz rate.
- [x] `validity 0x1ff` was visible, indicating all nine Phase 1 sensor groups reported valid.
- [x] Self position, yaw, health, selected slot, support values, and void distance were displayed.
- [x] Terrain and mid-range map values updated while moving and turning.
- [x] Entity count changed from zero to one and two as a cow and dropped item entered the loaded observation set.
- [x] Inventory wool/resource summaries reacted to changed inventory contents.
- [x] F10 enable/disable, F9 manual takeover, and F12 emergency release worked.
- [x] P froze the immutable observation while SawBot was disabled.
- [x] While frozen, the observation number remained fixed and `FROZEN` was shown.
- [x] Minecraft tick and snapshot age continued increasing while frozen, as designed.
- [x] P unfreezing resumed observation publication.
- [x] No serious FPS collapse or repeated SawBot exception was reported.

## Point-in-time performance evidence

The supplied screenshots showed total sensor extraction values ranging approximately from **1.2 ms to 5.6 ms** at the 10 Hz snapshot rate. Terrain extraction was the dominant component, ranging approximately from **0.36 ms to 3.69 ms** in the captured scenes. Entity extraction remained below approximately **0.10 ms** in the small test setup.

These values are screenshot samples rather than p95/p99 benchmark statistics. They are sufficient for this gate because the user also confirmed no serious FPS loss during the runtime test.

## Clarified freeze semantics

Freezing does not pause Minecraft or the integrated server. It preserves one immutable SawBot observation for inspection:

- `Obs #` remains fixed.
- Encoded terrain, entity, inventory, body, event, and timing values remain fixed.
- Snapshot `age` increases because the preserved snapshot gets older.
- Minecraft `Tick` continues because the client remains alive and safety/unfreeze controls must continue working.

## Gate result

**PASS — Phase 1 is accepted.**

Phase 2 sensor-inspector implementation may proceed. This acceptance does not validate Phase 2 world overlays, one-step capture, JSON export, or snapshot comparison; those require the Phase 2 runtime checklist.
