# Phase Gates

## Phase -1 — Research and locked specification

- [x] Private research separated from public-server cheating.
- [x] Screen/OCR pipeline prohibited.
- [x] ESP-like loaded-entity perception explicitly represented.
- [x] Teacher/runtime separation documented.
- [x] Observation and action contracts versioned.
- [x] Hardware, queue, licence, and one-week scope documented.

## Phase 0 — Forge foundation

Engineering:

- [x] Forge 1.8.9 project and GitHub build/release lane.
- [x] Client lifecycle, tick/render events, configuration, key bindings.
- [x] Central safe state and complete emergency input release.
- [x] Compact HUD and bounded timing instrumentation.
- [x] Offline contract/safety verification.

Runtime acceptance on 2026-07-10:

- [x] GitHub build and installable JAR worked.
- [x] Client and local world launched.
- [x] HUD and tick handler worked.
- [x] F10 toggle worked repeatedly.
- [x] F9 manual takeover released control.
- [x] F12 emergency release worked.
- [x] Five-minute idle test showed no repeated SawBot errors.
- [x] User observed no serious FPS reduction.

Evidence: `docs/PHASE0_ACCEPTANCE.md`.

**Gate: PASS.**

## Phase 1 — Internal eyes

Implemented in the `0.2.0-alpha.2` candidate:

- [x] Typed immutable Observation Contract v0.2.
- [x] Self/body state.
- [x] 13×9×13 egocentric local terrain tensor.
- [x] Complex collision-box classification.
- [x] Incremental 33×33 mid-range map with bounded cache.
- [x] Stable bounded loaded-entity tracker.
- [x] Explicit line-of-sight, occlusion, attackability, loaded state, confidence.
- [x] Conservative scoreboard-team relation.
- [x] 41-slot inventory and Bedwars resource summaries.
- [x] Universal landmark, bounded event history, server timing.
- [x] Snapshot sequence/age and per-sensor timing HUD.
- [x] F7 inspector and P immutable freeze independent of control enable state.
- [x] Offline Java 8 verification including freeze/control-state regression checks.

Runtime acceptance still required:

- [ ] GitHub CI performs the real Loom/Forge compile and remap.
- [ ] Updated JAR launches without SawBot errors.
- [ ] Full and partial blocks classify correctly.
- [ ] Slab, stair, and fence support distances are plausible.
- [ ] Void/edge support probes react correctly.
- [ ] Mid-range map fills while stationary and remains populated while moving/turning.
- [ ] Entity IDs remain stable and occlusion changes correctly.
- [ ] Inventory/resource values update correctly.
- [ ] Events occur on the expected snapshots without false damage attribution.
- [ ] P freezes sequence and snapshot values while DISABLED and ENABLED; unfreeze resumes.
- [ ] Sensor extraction times are measured on target hardware.
- [ ] Five-minute movement test has no repeated errors or serious FPS collapse.

**Gate: PENDING USER RUNTIME TEST.**

No Phase 2 implementation begins before this checklist passes.
