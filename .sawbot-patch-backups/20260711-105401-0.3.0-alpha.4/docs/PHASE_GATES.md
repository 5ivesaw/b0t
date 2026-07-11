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

Accepted on the target machine with candidate `0.2.0-alpha.2`:

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
- [x] GitHub Forge build and installed JAR worked.
- [x] `validity 0x1ff` observed.
- [x] Entity and inventory values reacted to runtime changes.
- [x] Frozen observation number remained fixed while tick/age continued.
- [x] Target-machine sensor samples were approximately 1.2–5.6 ms at 10 Hz.
- [x] No serious FPS collapse or repeated SawBot error was reported.

Evidence: `docs/PHASE1_ACCEPTANCE.md`.

**Gate: PASS.**

## Phase 2 — Sensor inspector

Implemented through candidate `0.3.0-alpha.2`:

- [x] Independently toggleable terrain, collision/support, entity boxes, entity tracers, and landmark overlays.
- [x] Selected block decoding from frozen snapshot coordinates.
- [x] Stable entity selection and cycling.
- [x] Compact body, terrain, entity, inventory, event, difference, and system pages.
- [x] Immutable current-versus-previous snapshot comparison.
- [x] Bounded asynchronous human-readable snapshot export.
- [x] One-observation-step capture while frozen.
- [x] Frozen sensors and map cache do not mutate between explicit steps.
- [x] Export and render timing/status visible.
- [x] Offline Java 8 verification and generated JSON validation.

Runtime acceptance required:

- [ ] GitHub CI performs the real Loom/Forge compile and remap.
- [ ] Updated JAR launches without repeated SawBot errors.
- [ ] Every inspector page is usable at 1366×768.
- [ ] Terrain and collision overlays correspond to selected model cells.
- [x] Entity boxes and IDs remain stable.
- [ ] Box, tracer, and label colour switch immediately with LOS/OCC text, including while selected.
- [x] LOS/OCC updates in both directions when entities move around cover.
- [x] V toggles tracers independently and tracers remain anchored while walking.
- [x] World-spawn landmark resolves to the standable surface instead of underground.
- [x] One `.` press while frozen increments observation sequence exactly once.
- [x] O produces a valid bounded JSON export matching the HUD sequence.
- [x] Difference page responds to known changes.
- [x] F10/F9/F12 safety controls remain authoritative.
- [x] Overlay-off FPS remains equivalent to Phase 1.
- [ ] Individual overlay costs are measured without serious FPS collapse.
- [x] Five-minute inspector test has no repeated SawBot errors.

**Gate: PENDING `0.3.0-alpha.2` VISUAL-COLOUR RETEST.**

No Phase 3 telemetry implementation begins before this checklist passes.
