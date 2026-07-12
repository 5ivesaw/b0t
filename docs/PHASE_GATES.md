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

## Phase 6 — Hybrid navigation body

Implemented in candidate `0.7.0-alpha.0`:

- [x] Brain/body architectural boundary documented.
- [x] Incremental bounded A* with deterministic tie-breaking.
- [x] Standability, support, headroom, liquid, hazard, and diagonal-corner checks.
- [x] Sustained input ownership instead of short action pulses.
- [x] Responsive visible turning, step-up jump, and straight-segment sprint.
- [x] Stable arrival, stuck detection, recovery, and replanning.
- [x] Route/current-node rendering and compact live planner diagnostics.
- [x] G-waypoint navigation can run without an external model.
- [x] High-level brain navigation intent remains supported.
- [x] Synthetic input release restores physical key state.
- [x] F9/F12 notices remain visible for five seconds.
- [x] One-press telemetry retry and per-step encoding isolation.
- [x] Offline planner, world-grid, body, release, and existing contract verification.

**Gate: SOURCE/OFFLINE PASS; TARGET-MACHINE ACCEPTANCE DEFERRED TO THE NEXT MAJOR REVIEW.**

Evidence: `docs/PHASE6_REPORT.md`, `docs/HYBRID_ARCHITECTURE.md`, `docs/NAVIGATION_BODY.md`, and `NavigationBodyContractTest`.

## Phase 7 — Real-time adaptive navigation

Implemented in candidate `0.8.0-alpha.0`:

- [x] Anytime bounded A* exposes a usable best-frontier route during search.
- [x] Active movement continues while replacement planning runs.
- [x] Rolling replans originate from the player's actual current position.
- [x] Manual takeover/disable invalidates stale route ownership.
- [x] Bounded corridor re-anchoring handles displacement and safe node skipping.
- [x] Live route-window refresh detects placed/broken blocks and lost support.
- [x] Safe lookahead smooths routes without wall/void/corner cutting.
- [x] Several immediate steering candidates are evaluated every client tick.
- [x] Faster bounded visible camera response and sustained inputs remain legitimate.
- [x] Edge exposure and direction changes influence route quality as soft costs.
- [x] HUD/renderer expose replans, hot swaps, re-anchors, invalidations, detours,
      deviation, lookahead, provisional routes, and live world refreshes.
- [x] Java contract tests cover frontier paths, re-anchoring, dynamic invalidation,
      current-position resume, route safety, and input restoration.

**Gate: SOURCE/OFFLINE PASS; INTEGRATED RUNTIME ACCEPTANCE DEFERRED TO THE NEXT MAJOR REVIEW.**

Evidence: `docs/PHASE7_REPORT.md`, `docs/ADAPTIVE_NAVIGATION.md`,
`docs/HYBRID_ARCHITECTURE.md`, `docs/NAVIGATION_BODY.md`, and
`NavigationBodyContractTest`.

## Phase 8 — Real-time bridging specialist

Implemented in candidate `0.9.0-alpha.0`:

- [x] Brain/body boundary keeps bridge destination/strategy outside the mechanical
      specialist.
- [x] Bounded straight and staircase-diagonal support corridors replan from the
      player's actual current feet cell.
- [x] Only full solid hotbar `ItemBlock` stacks are eligible; the largest stack is
      selected and the original slot is restored on release.
- [x] Visible yaw/pitch alignment remains bounded per client tick.
- [x] Placement requires a legal adjacent support face, normal reach, and matching
      ray trace.
- [x] Exactly one deliberate client right-click attempt is followed by bounded
      world-state confirmation.
- [x] Forward/sneak advance is allowed only after the target support is observed.
- [x] Navigation may hand off after `NO_PATH`/`BLOCKED`; manual `R` is a private/local
      mechanical test harness only.
- [x] HUD and world renderer expose plan, current support, state, attempts,
      confirmations, placements, failures, replans, retargets, slot, source, and reason.
- [x] F9, F12, physical takeover, disable, freeze, GUI pause, model disconnect,
      exceptions, and world unload release inputs and restore the original slot.
- [x] Corridor/body tests cover straight/diagonal bounds, three-block placement,
      confirmation, block consumption, no-block stop, void stop, slot restoration,
      and complete input release.

**Gate: SOURCE/OFFLINE PASS; INTEGRATED RUNTIME ACCEPTANCE DEFERRED TO THE NEXT MAJOR REVIEW.**

Evidence: `docs/PHASE8_REPORT.md`, `docs/BRIDGING_BODY.md`,
`docs/HYBRID_ARCHITECTURE.md`, and `BridgingBodyContractTest`.

## Phase 9 — Segmented navigation core

Implemented in candidate `1.0.0-alpha.0`:

- [x] Immutable local/full navigation snapshots captured under a client-tick budget.
- [x] One bounded latest-wins background planner with explicit shutdown.
- [x] Weighted A* over explicit traverse, diagonal, ascent, and descent operations.
- [x] Direct validated micro-route while broader snapshots/plans are prepared.
- [x] Active/staged routes, planning ahead, replacement splicing, and segments.
- [x] Rewind/skip and corridor projection from the actual current player position.
- [x] Large displacement and changed geometry trigger current-position replanning.
- [x] Continuous movement ownership, validated lookahead, fast visible turning,
      jump/sprint execution, operation timeout, and stuck recovery.
- [x] Persistent bounded world caches and bounded live near-future refresh.
- [x] Route rendering is inspector-only and marker-capped for low-end hardware.
- [x] 71 dedicated segmented-navigation checks plus all previous contract suites.

**Gate: SOURCE/OFFLINE PASS; TARGET-MACHINE RUNTIME ACCEPTANCE PENDING.**

Evidence: `docs/PHASE9_REPORT.md`, `docs/SEGMENTED_NAVIGATION_CORE.md`,
`docs/BARITONE_ARCHITECTURE_RESEARCH.md`, `docs/NAVIGATION_BODY.md`, and
`SegmentedNavigationContractTest`.


## Phase 10 — Continuous anytime navigation

Implemented in candidate `1.1.0-alpha.0`:

- [x] Direction-aware incremental weighted A* retains alternative arrival headings.
- [x] Search runs in bounded 48-expansion slices on the existing worker.
- [x] Safe best-so-far paths stream before the full search completes.
- [x] Search continues improving after movement begins.
- [x] Rolling searches originate from the player's actual current feet cell.
- [x] Streamed replacements stage and splice into live execution.
- [x] Collision alone never triggers jump.
- [x] Vertical movement is limited to legal cardinal one-block transitions.
- [x] Intermediate route cells no longer pulse W off.
- [x] F7 shows bounded search exploration and best-frontier lines.
- [x] Search/result/debug structures remain strictly bounded and non-blocking.
- [x] Contract tests cover early publication, eventual completion, diagnostic bounds,
      and impossible two-block ascent rejection.

**Gate: SOURCE/OFFLINE PASS; TARGET-MACHINE RUNTIME ACCEPTANCE PENDING.**

Evidence: `docs/PHASE10_REPORT.md`,
`docs/CONTINUOUS_ANYTIME_NAVIGATION.md`, `docs/PHASE9_REPORT.md`,
`docs/BARITONE_ARCHITECTURE_RESEARCH.md`, and
`SegmentedNavigationContractTest`.


## Phase 11 — Reference-driven bodies and visualization lifecycle

Implemented in candidate `1.2.0-alpha.0`:

- pinned reference/license audit for mechanical body systems
- multi-support, multi-hit-vector legal bridge placement selection
- immediate bridge-plan cleanup on ownership and waypoint lifecycle changes
- stale navigation diagnostic suppression
- hard render caps for search, route, and bridge overlays
- bridge face-sampling and overlay-lifecycle contract coverage

Evidence: `docs/PHASE11_REPORT.md`, `docs/REFERENCE_BODY_RESEARCH.md`,
`docs/VISUALIZATION_LIFECYCLE.md`.

Runtime acceptance is intentionally grouped with the later body-integration pass.
