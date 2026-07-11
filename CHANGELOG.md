# Changelog

## 0.3.0-alpha.6 — Phase 2 runtime hardening and inspector truthfulness

- Clarified immutable freeze semantics: frozen world overlays stay at the captured world coordinates while Minecraft and the player continue moving.
- Added frozen capture coordinates, distance-from-capture, and a `FROZEN SNAPSHOT` world anchor; `.` explicitly recaptures at the current location.
- Forced an immediate observation capture when unfreezing to remove the brief stale/orange transition.
- Bumped the observation schema to `sawbot.observation/0.3` and debug export to `sawbot.snapshot.debug/0.2`.
- Added a bounded specific `EntityType` vocabulary and dropped-item payload category, replacing vague `LIVING / UNKNOWN` labels for ordinary mobs.
- Made selected-block outlines consistently yellow; tensor membership is now expressed in text rather than by changing the selection colour.
- Made successful export and inspector notices transient while preserving longer error visibility.
- Added complete OpenGL attribute push/restore to prevent overlays from dimming or contaminating the vanilla hotbar/HUD state.
- Added target-machine runtime validation documentation and expanded offline verification to 539 checks.

## 0.3.0-alpha.5 — Immediate debug HUD restoration

- Reverted the `0.3.0-alpha.4` glass/card interface completely after target-machine testing showed that it obscured gameplay and made Phase 2 checks harder.
- Restored the compact, readable Phase 2 text HUD and text inspector exactly as used before the UI experiment.
- Restored the original low-cost world-debug labels while preserving the accepted `alpha.2` immediate green LOS / purple OCC state changes, stable tracking IDs, independent tracer toggle, and landmark fix.
- Removed the decorative UI renderer, motion helpers, theme classes, UI-specific stubs, and the unused interface-design charter.
- Preserved single-push automatic GitHub releases from `alpha.3`.
- No sensor, observation-contract, export, stepping, safety, or autonomous-control behavior was expanded in this release.

## 0.3.0-alpha.4 — Premium compact HUD and inspector workspace

- Replaced the always-visible Phase 2 debug-text wall with a compact glass status island.
- Added a stable eight-page inspector workspace with cards, hierarchy, empty states, page indicators, overlay chips, and contextual controls.
- Added centralized `UiTheme`, cached rounded-card rendering through `GlassUi`, presentation-only `MotionValue`, and actual-binding `KeyLabel` components.
- Added semantic freshness, safety, LOS/OCC, support, danger, and selection styling throughout the interface.
- Added F3-aware compact positioning and a configurable reduced-motion option.
- Added independent HUD render timing to the System inspector page.
- Added dark backed world-label chips and a contextual selected-block label.
- Preserved every accepted Phase 2 sensor, export, freeze, step, overlay, and safety behavior.
- Expanded offline verification from 527 to 541 checks.

## 0.3.0-alpha.3 — Verified single-push releases and interface design charter

- Changed the normal release process to one `git push origin main`; successful CI now creates the version tag and GitHub Release automatically.
- Reused the exact Forge artifact produced by the tested build instead of rebuilding in a separate release workflow.
- Added a second payload verification after artifact transfer, including JAR metadata/class validation and SHA-256 verification.
- Made `sawbotVersion` in `gradle.properties` the single release-version source of truth.
- Added duplicate-version protection so published releases are never silently overwritten.
- Retained a manual recovery workflow for exceptional failures only.
- Added the locked premium interface design system for future HUD, inspector, overlay, control-centre, and trainer UI work.
- Preserved Phase 2 runtime sensor behavior from `0.3.0-alpha.2`.

## 0.3.0-alpha.2 — Immediate LOS/OCC visual-state correction

- Made LOS/OCC visibility the primary colour source for entity boxes, labels, and tracers.
- LOS now renders bright green and OCC renders bright purple on the same observation that changes the text.
- Removed selected-entity yellow as a colour override; selection now uses a separate yellow accent outline.
- Increased occluded box/tracer opacity so OCC state remains obvious without hiding the entity status.
- Added an orange warning state for logically inconsistent LOS/occlusion flags.
- Added team relation to world labels while preserving visibility-colour semantics.
- Added regression checks proving text and visual colour derive from the same current entity observation.

## 0.3.0-alpha.1 — Phase 2 runtime correction

- Recomputed entity line of sight every observation from seven current bounding-box ray samples.
- Fixed stale LOS/OCC transitions when tracked entities move around walls.
- Anchored tracers to the live interpolated player eye position to remove walking jitter.
- Added an independent V tracer toggle while retaining N for entity boxes and labels.
- Capped tracers at 16 even when more entities are tracked.
- Resolved the world-spawn landmark to the nearest loaded standable surface instead of the raw underground save coordinate.
- Clarified that crosshair block selection is automatic and the yellow outline is the selected block.
- Added runtime-regression checks for wall transitions, tracking-ID continuity, surface landmark resolution, and tracer independence.

## 0.3.0-alpha.0 — Phase 2 sensor inspector

- Added independently toggleable terrain, collision/support, entity/LOS, and landmark world overlays.
- Added selected-block decoding and stable tracked-entity selection/cycling.
- Added eight compact inspector pages for body, terrain, entities, inventory, events, difference, and system state.
- Added deterministic current-versus-previous observation comparison.
- Added bounded asynchronous full-snapshot JSON export under `.minecraft/sawbotv1/exports/`.
- Added one-observation-step capture while frozen.
- Prevented frozen sensor/map caches from mutating between explicit steps.
- Added world-render timing and export queue/status instrumentation.
- Drained queued lower-priority key presses after takeover/emergency actions.
- Expanded offline verification to 508 assertions plus full JSON parsing and exact array-bound checks.
- Updated GitHub CI, JAR validation, release assets, documentation, and runtime gates.

## 0.2.0-alpha.2 — Observation freeze state correction

- Separated observation freezing from the autonomous enabled/disabled mode.
- P now freezes snapshots even while SawBot is disabled, matching Phase 1's always-on sensor inspector.
- Added an explicit `observationsFrozen` state and future-safe `mayApplyAutonomousActions()` guard.
- Added a visible `FROZEN` marker beside the observation number.
- Added regression checks for disabled freeze, enabled freeze, unfreeze, and action gating.

## 0.2.0-alpha.1 — Phase 1 key-conflict correction

- Moved enable/disable from F8 to F10 because F8 toggles Minecraft 1.8.9 smooth camera.
- Moved snapshot freeze from F6 to P because F6 opens the obsolete Twitch broadcast dialog.
- Left the future telemetry control unbound because F5 changes perspective.
- Preserved F7 inspector, F9 manual takeover, and F12 emergency stop.
- Added offline assertions that the shipped defaults do not reuse F5, F6, or F8.

## 0.2.0-alpha.0 — Phase 1 internal eyes

- Recorded successful real-client Phase 0 acceptance on the target machine.
- Added immutable Observation Contract `sawbot.observation/0.2`.
- Added self/body state, egocentric velocity/acceleration, collision-box support samples, and void-distance probe.
- Added bounded 13×9×13 egocentric local terrain tensor with semantic flags and compound collision classes.
- Added an incremental 33×33 mid-range map with a 4,096-column bounded world-coordinate cache, cached support-height revalidation, periodic full rescans, and movement/rotation re-projection.
- Added stable loaded-entity tracking with bounded priority, team relation, LOS, occlusion, attackability, and confidence.
- Added fixed 41-slot inventory encoding and iron/gold/diamond/emerald/wool summaries.
- Added bounded landmarks, events, server timing, sensor validity, sequence/age, and per-sensor timing.
- Added F7 sensor inspector and F6 immutable snapshot freeze.
- Replaced false hurt-timer damage attribution with `ENTITY_HURT_OBSERVED`.
- Expanded offline verification to 56 assertions, including cache re-projection, bounded support-height hint scans, hazardous-surface rejection, and conservative team classification.
- Updated GitHub CI/release assets and JAR verification for Phase 1.

## 0.1.0-alpha.0 — Phase 0 foundation

- Locked private-research scope and safety boundary.
- Added Observation Contract v0.1 and Action Contract v0.1.
- Added performance budget, risk register, source audit, telemetry plan, simulator validation plan, and phase gates.
- Added Forge 1.8.9 client entry point, key bindings, safe state controller, emergency key release, minimal HUD, and bounded timing window.
- Added Java 8 offline verifier and contract smoke tests.
- Added standalone control-centre prototype.
- Added GitHub Actions CI and release publishing with validated JARs, sources, and checksums.
- Replaced deprecated ForgeGradle 2.1 with Gradle 8.8 and Architectury Loom while retaining Java 8 output and Forge 1.8.9 runtime compatibility.
