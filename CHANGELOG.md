# Changelog

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
