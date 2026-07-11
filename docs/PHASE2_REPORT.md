# PHASE REPORT — Phase 2 Sensor Inspector

Date: 2026-07-10  
Candidate version: `0.3.0-alpha.6`
Observation schema: `sawbot.observation/0.3`
Debug export format: `sawbot.snapshot.debug/0.2`

## Completed

- Recorded the user's successful Phase 1 runtime acceptance.
- Added independently toggleable world overlays:
  - B — local terrain tensor and semantic cell outlines.
  - C — collision-height classes plus left/centre/right support probes.
  - N — bounded entity boxes, tracking IDs, and team/LOS/occlusion labels.
  - V — independently toggleable sight-line tracers, capped at 16.
  - M — semantic landmark markers and labels.
- Added selected-block inspection using the crosshair and the exact frozen snapshot basis; the yellow outline is the automatic current selection.
- Added selected-entity inspection with stable tracking IDs and `[` / `]` cycling, including occluded loaded entities.
- Added eight compact inspector pages selected with H:
  - Summary
  - Body
  - Terrain
  - Entities
  - Inventory
  - Events
  - Difference
  - System
- Added deterministic immutable current-versus-previous snapshot comparison.
- Added a bounded asynchronous human-readable JSON exporter activated with O.
- Added explicit export queue capacity, rejection count, atomic temporary-file replacement, daemon worker, and JVM shutdown handling.
- Added one-observation-step capture with `.` while frozen.
- Changed frozen-pipeline behaviour so no sensor cache mutates until unfreeze or one explicit step.
- Preserved F10/F9/F12 safety priority and complete input release.
- Drained queued lower-priority key presses after takeover/emergency actions so a simultaneous inspector or enable press cannot fire on the next tick.
- Preserved the Phase 1 observation schema; no neural/model fields were silently changed.
- Added world-render timing to the HUD.
- After first target-machine testing, anchored tracers to the live interpolated eye position to eliminate movement jitter.
- Replaced the single Minecraft visibility result with seven current bounding-box ray samples so LOS/OCC transitions refresh when an entity moves around a wall.
- Resolved the world-spawn landmark to the nearest loaded standable surface rather than the raw underground save coordinate.
- Unified entity box, tracer, and label visibility styling so LOS is immediately green and OCC is immediately purple from the same current observation; selection now uses a separate accent outline.
- Updated GitHub CI, JAR validation, release packaging, version metadata, and reports for Phase 2.

## Files created

Common deterministic comparison:

- `sawbot-common/src/main/java/dev/fivesaw/sawbot/common/observation/ObservationDiff.java`
- `sawbot-common/src/main/java/dev/fivesaw/sawbot/common/observation/ObservationDiffCalculator.java`

Inspector and export runtime:

- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/inspection/InspectorPage.java`
- `.../inspection/BlockInspection.java`
- `.../inspection/InspectorController.java`
- `.../inspection/SnapshotJsonWriter.java`
- `.../inspection/SnapshotExportService.java`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.java`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/tracking/VisibilitySampler.java`

Evidence and report:

- `docs/PHASE1_ACCEPTANCE.md`
- `docs/PHASE2_REPORT.md`
- `docs/PHASE2_RUNTIME_FEEDBACK.md`

## Materially changed

- `EgocentricTransform.java`
- `ObservationPipeline.java`
- `LandmarkSensor.java`
- `EntityTrackerSensor.java`
- `ClientRuntime.java`
- `SawBotKeyBindings.java`
- `SawBotStateController.java`
- `FoundationHud.java`
- `en_US.lang`
- `SawBotMod.java`
- verification stubs and `FoundationContractTest.java`
- `tools/offline-verify.sh`
- `tools/package-release.sh`
- `tools/verify-built-jar.py`
- GitHub CI/release workflows
- root version/build metadata
- README, changelog, phase gates, release guide, and repository tree

The exact package manifest is generated as `docs/PHASE2_FILE_MANIFEST.txt` before ZIP creation.

## Controls

```text
F10       Enable/disable SawBot state
F9        Immediate manual takeover
F12       Emergency release all inputs
F7        Show/hide inspector panel
P         Freeze/unfreeze observation
.         Capture exactly one new observation while frozen
H         Next inspector page
B         Toggle terrain-cell overlay
C         Toggle collision/support overlay
N         Toggle entity boxes and LOS/OCC labels
V         Toggle entity tracers independently
M         Toggle landmark overlay
[ / ]     Previous/next tracked entity
O         Export current immutable snapshot as JSON
```

All controls remain rebindable under `Options → Controls → SawBotV1`.

## Export location

One-shot debug exports are written under:

```text
.minecraft/sawbotv1/exports/
```

Each JSON file contains the complete bounded observation contract, including full local-terrain and mid-range arrays, all tracked entities, all 41 inventory slots, landmarks, bounded events, server timing, previous action, per-sensor timings, difference summary, and current inspector selection.

No screenshot, frame, OCR data, Java object serialization, or unbounded log is written.

## Commands run

```text
bash tools/offline-verify.sh
```

Final-package validation also runs:

```text
bash -n gradlew tools/offline-verify.sh tools/package-release.sh
python3 -m py_compile tools/verify-built-jar.py
JSON parsing and exact array-bound validation for a generated debug snapshot
YAML parsing for both GitHub workflows
clean ZIP extraction and manifest comparison
```

## Tests

- PASS — `FoundationContractTest` — 539 assertions.
- PASS — LOS text, box, label, and tracer resolve from one green visibility style.
- PASS — OCC text, box, label, and tracer resolve from one purple visibility style.
- PASS — inconsistent LOS/occlusion flags resolve to an orange warning style.
- PASS — Phase 0 and Phase 1 contract/safety regression tests.
- PASS — cardinal and continuous inverse egocentric transforms.
- PASS — deterministic observation difference counts.
- PASS — selected-block world-to-tensor conversion across rotated bases.
- PASS — outside-tensor block marking.
- PASS — single-step rejection while live.
- PASS — one queued step is consumed exactly once while frozen.
- PASS — single-step capture bypasses the normal 10 Hz interval exactly once.
- PASS — frozen ticks do not publish or mutate observation state.
- PASS — previous snapshot retention.
- PASS — Phase 2 key uniqueness and non-empty defaults.
- PASS — generated debug JSON parses successfully.
- PASS — JSON terrain array has exactly 1,521 cells.
- PASS — JSON mid-range map has exactly 1,089 columns.
- PASS — JSON inventory has exactly 41 slots.
- PASS — JSON output remains bounded.
- PASS — expanded `mcmod.info` JSON.
- PASS — GitHub repository packaging checks.

## Performance design

- All world overlays are OFF by default.
- Terrain and collision rendering are each capped at 256 boxes per frame.
- Entities remain capped at 32 and landmarks at 64 by the observation contract.
- Tracers are independently toggleable and capped at 16 even when more entities are tracked.
- Debug export queue capacity is four immutable snapshots.
- Export serialization and disk I/O run on one daemon worker, never the Minecraft client thread.
- The worker receives immutable snapshots only and never accesses Minecraft world objects.
- Export files use temporary writes followed by atomic replacement where supported.
- World-render average and maximum time are exposed in the System inspector page.
- Snapshot comparison is O(1,521 + 1,089 + bounded entities + 41 inventory slots) and runs only when a snapshot is published, not every rendered frame.

No real FPS or render-cost claim is made until the target-machine Phase 2 test.

## Important semantics

### Frozen step

`.` does not pause or advance the Minecraft server. It asks the observation pipeline to capture exactly one immutable snapshot on that client tick, increments the observation sequence once, then returns to frozen state. Between explicit steps, no observation sensor or mid-range cache is updated.

### Collision overlay

The overlay shows the bounded collision-height class that the neural policy receives: none, quarter, half, three-quarter, full, other, or compound. It does not pretend that the v0.3 contract contains every raw collision AABB. The selected block's state ID, semantic flags, and collision class are shown verbatim.

### Entity ESP

Occluded loaded entities may be rendered in the controlled private research environment. The overlay still labels LOS versus OCC, and `attackable` remains false through walls.

## Known limitations

- This environment cannot fetch the Loom/Minecraft/Forge dependencies, so the real remapped Forge build must pass on GitHub Actions before runtime testing.
- The one-step feature advances SawBot's observation system once; it does not pause the game world.
- Collision rendering represents the model's collision-height classes, not every source AABB shape.
- Terrain/collision overlays intentionally cap rendered cells to protect Intel HD 520 performance.
- The only current semantic landmark is world spawn; its marker is surface-resolved only while the spawn column is loaded. Bedwars landmarks require the later task adapter.
- Snapshot export is human-readable debug JSON and is not the Phase 3 high-volume trajectory format.
- Comparison is against the immediately previous published snapshot. A selectable arbitrary baseline can be added only if runtime testing demonstrates a concrete need.
- No model, actuator, autonomous movement, Bedwars logic, or telemetry trajectory writer exists in this phase.

## USER CHECKLIST

- [ ] GitHub CI passes offline verification and the real Loom/Forge build.
- [ ] `SawBotV1-0.3.0-alpha.6-mc1.8.9.jar` launches with no repeated SawBot error.
- [ ] F7 opens and closes the Phase 2 panel.
- [ ] H cycles through all eight pages without hiding Minecraft controls.
- [ ] B shows the local tensor boundary and coloured non-air cells.
- [ ] C shows collision-height boxes and three support probe lines.
- [ ] N shows entity boxes and `#trackingId` labels.
- [ ] V disables and re-enables tracers without hiding boxes or labels.
- [ ] Tracers remain visually anchored while walking and strafing.
- [ ] Moving an entity behind a wall visibly changes LOS to OCC while its ID remains stable.
- [ ] The box, label, and tracer switch green ↔ purple on the same update as the LOS/OCC text, including when the entity is selected.
- [ ] `[` and `]` cycle tracked entities, including an occluded one.
- [ ] M shows the world-spawn landmark marker on the standable surface rather than underground.
- [ ] Aiming at a block automatically shows a yellow outline; Summary/Terrain pages display world coordinates, R/U/F offsets, tensor index, state ID, category, flags, and collision class.
- [ ] P freezes the observation and all displayed model inputs.
- [ ] While frozen, one `.` press increments Obs # exactly once; waiting does not increment it again.
- [ ] O creates one valid JSON file under `.minecraft/sawbotv1/exports/`.
- [ ] The exported sequence number matches the HUD observation number used for export.
- [ ] Difference page changes after movement, block changes, entity changes, or inventory changes.
- [ ] F10, F9, and F12 still pass.
- [ ] With every world overlay OFF, normal FPS remains equivalent to Phase 1.
- [ ] With each overlay tested separately, no serious FPS collapse occurs.
- [ ] Five-minute inspector test produces no repeated SawBot error.
- [ ] System page render average/maximum and sensor values are recorded.

## How to test

1. Push the repository and wait for GitHub Actions CI to pass.
2. Publish or download `v0.3.0-alpha.6` and install only that SawBot JAR.
3. Join the same local test world used for Phase 1.
4. Open F7 and cycle H through all pages.
5. Toggle B, C, N, V, and M one at a time before combining them.
6. Aim at full blocks, slabs, stairs, fences, hazards, liquids, and air-adjacent edges.
7. Spawn two entities, note their tracking IDs, move each into and out of cover, toggle V while walking, and use `[` / `]`.
8. Freeze with P, press `.` once, wait several seconds, then press `.` once more.
9. Press O, close or pause the game, and inspect the newest JSON under `.minecraft/sawbotv1/exports/`.
10. Re-run F10/F9/F12 and continue for five minutes while observing FPS and the System page.

## Expected result

The mod remains non-autonomous. It renders and exports exactly the bounded information already available to the observation contract, lets the user preserve and step immutable snapshots, and exposes changes and costs without moving, aiming, attacking, placing, shopping, or playing Bedwars.

## What to send back if it fails

- Screenshot of the incorrect overlay or inspector page.
- Exact key pressed and whether the key appears under `Options → Controls → SawBotV1`.
- `latest.log` from the first SawBot exception through the final `Caused by` line.
- Export status line and the newest file name if O fails.
- The exported JSON file for mismatched snapshot data.
- Obs # before step, immediately after step, and five seconds later.
- Entity tracking ID and LOS/OCC state before and after hiding it.
- Normal FPS, each individual overlay FPS, combined-overlay FPS, and System-page render avg/max.


## 0.3.0-alpha.5 runtime-interface correction

The `0.3.0-alpha.4` card-based HUD was rejected during target-machine testing because it covered too much of the game and slowed manual inspection. `0.3.0-alpha.5` restores the pre-existing compact text HUD and text inspector. Phase 2 sensor behavior, LOS/OCC colour semantics, export, freeze/step, overlay toggles, and safety controls remain unchanged.

## 0.3.0-alpha.6 target-machine hardening

Target-machine feedback confirmed the core Phase 2 pipeline, safety controls, freeze/step behavior, LOS/OCC transitions, export, differences, performance, and automatic GitHub release lane. It also exposed several presentation ambiguities and one possible render-state leak.

Corrections in alpha.6:

- Frozen terrain, entity, and landmark overlays are explicitly identified as world-anchored data from the immutable captured observation; capture coordinates and player distance from the capture are shown.
- A `FROZEN SNAPSHOT` marker identifies the old observation anchor, and `.` captures exactly one new observation at the player's current position.
- Unfreeze requests an immediate observation so the HUD does not briefly show stale-warning colour.
- Observation schema v0.3 adds a bounded specific entity type and dropped-item payload category.
- Crosshair block selection is always yellow, including outside the tensor; membership remains visible in text.
- Export-success and normal inspector notices expire automatically.
- World rendering restores the full OpenGL attribute state to protect the vanilla hotbar and HUD.

The supplied exported snapshot was valid and bounded (`0x1ff`), and its measured total extraction time was approximately 3.08 ms. The supplied log contained no SawBot exception or repeated SawBot error. See `docs/PHASE2_RUNTIME_VALIDATION.md`.
