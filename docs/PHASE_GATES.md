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

Implemented through candidate `0.3.0-alpha.6`:

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

- [x] GitHub CI performs the real Loom/Forge compile and remap for the preceding Phase 2 build; alpha.6 remains to be compiled automatically after push.
- [x] Pre-alpha.6 JAR launches without repeated SawBot errors; alpha.6 focused launch remains pending.
- [x] Every inspector page is reachable and readable at 1366×768.
- [x] Terrain and collision overlays render and move on the block-snapped tensor basis; alpha.6 improves explanatory text.
- [x] Entity boxes and IDs remain stable.
- [x] Box, tracer, and label colour switch immediately with LOS/OCC text, including while selected.
- [x] LOS/OCC updates in both directions when entities move around cover.
- [x] V toggles tracers independently and tracers remain anchored while walking.
- [x] World-spawn landmark resolves to the standable surface instead of underground.
- [x] One `.` press while frozen increments observation sequence exactly once.
- [x] O produces a valid bounded JSON export matching the HUD sequence.
- [x] Difference page responds to known changes.
- [x] F10/F9/F12 safety controls remain authoritative.
- [x] Overlay-off FPS remains equivalent to Phase 1.
- [x] Individual overlays and combined use produced no serious FPS collapse in the target-machine session.
- [x] Five-minute inspector test has no repeated SawBot errors.

**Gate: PASS.**

Evidence: `docs/PHASE2_ACCEPTANCE.md`, target-machine screenshots, exported snapshot, and clean runtime log.

### Runtime presentation decision

The `0.3.0-alpha.4` card interface was rejected on the target machine. SawBot uses the compact text HUD. Explanatory prose belongs in documentation rather than over gameplay.

## Phase 3 — Structured telemetry

Implemented in `0.4.0-alpha.0` and hardened in the Phase 4 candidate:

- [x] Versioned `sawbot.telemetry/0.1` binary contract.
- [x] No frames, screenshots, OCR, or pixel data.
- [x] Exact client-tick human key-state and raw mouse-delta capture.
- [x] Observation/input/outcome alignment.
- [x] Bounded asynchronous writer, per-record compression, CRC32, footer, validation, replay, and recovery.
- [x] Offline complete-write and damaged-prefix recovery fixtures pass.
- [x] Phase 4 adds restartable failure state, per-step encoding rejection, visible error reason, and independent status colours.

Target-machine result: recording reached 117 steps, then entered an error state before the resulting file could be validated. The exact exception was not captured, so the gate remains open. See `docs/PHASE3_RUNTIME_STATUS.md`.

**Gate: PARTIAL — FORMAT/OFFLINE PASS, TARGET WRITER RETEST REQUIRED.**

## Phase 4 — Safe actuator and model bridge

Implemented in candidate `0.5.0-alpha.0`:

- [x] Versioned `sawbot.bridge/0.1` local protocol.
- [x] Non-blocking client-thread publication and background socket worker.
- [x] Bounded observation/action queues and CRC-protected frames.
- [x] Handshake, model identity, nonce, reconnect, and disconnect handling.
- [x] Dummy model plus deterministic actuator demo.
- [x] Strict Action Contract and snapshot-reference validation.
- [x] Configurable age and observation-sequence deadlines.
- [x] Local/private environment guard.
- [x] Legitimate W/A/S/D, jump, sprint, sneak, smooth camera, hotbar, attack, use, drop, and inventory controls.
- [x] Previous applied action inserted into subsequent observations.
- [x] Physical keyboard/mouse takeover.
- [x] F9/F12/world-unload/model-disconnect complete release.
- [x] Compact MODEL inspector page with latency, queue, drop, reconnect, and actuator counters.
- [x] Loopback protocol and actuator behavior covered by offline integration tests.

Runtime acceptance required:

- [ ] Dummy model connects and HUD reaches `READY`.
- [ ] F10 refuses blocked/offline scope and enables only when ready.
- [ ] Every legitimate test command actuates correctly.
- [ ] Camera motion is smooth and bounded.
- [ ] Hotbar changes and one-shot attack/use/drop/inventory actions fire once.
- [ ] Physical input causes immediate manual takeover.
- [ ] Closing the dummy model immediately disables and releases controls.
- [ ] Client thread remains responsive while model is absent/reconnecting.
- [ ] MODEL page latency and counters update.
- [ ] Telemetry restarts and produces a validator-clean `.sbt` file.
- [ ] Overlay rendering no longer tints the hotbar/held item.
- [ ] Focused five-minute test has no repeated SawBot errors.

**Gate: PENDING TARGET-MACHINE PHASE 4 TEST.**
## Phase 5 — First learned behaviour

Implemented in candidate `0.6.0-alpha.0`:

- [x] Explicit user waypoint enters the normal bounded landmark contract.
- [x] Deterministic balanced teacher dataset with 28,000 examples.
- [x] Teacher is absent from the live runtime.
- [x] Exported 18→32→7 MLP checkpoint with fixed feature/action ordering.
- [x] Pure-standard-library local inference process over `sawbot.bridge/0.1`.
- [x] No runtime pathfinder or hidden steering correction.
- [x] 800 held-out rollout starts and retained failure examples.
- [x] Learned success 87.25%; random baseline 3.625%.
- [x] Inference and checkpoint/data/evaluation contracts verified in CI.
- [x] Phase 4 input-ownership, stable bridge-status, takeover-notice, and telemetry-close corrections bundled.

Runtime acceptance is intentionally deferred to the next major integration review while development continues.

**Gate: SOURCE/EVALUATION PASS; INTEGRATED RUNTIME ACCEPTANCE DEFERRED.**

Evidence: `docs/PHASE5_REPORT.md`, `docs/WAYPOINT_MODEL.md`, committed dataset/checkpoint/evaluation, and `verify_phase5.py`.
