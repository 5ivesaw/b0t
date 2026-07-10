# PHASE REPORT — Phase 1 Internal Eyes

Date: 2026-07-10  
Candidate version: `0.2.0-alpha.2`  
Observation schema: `sawbot.observation/0.2`

## Runtime corrections from target-machine testing

The first Phase 1 runtime test exposed a default-key collision: F6 opened Minecraft 1.8.9's Twitch broadcast dialog. Review also found F8 and F5 are owned by vanilla smooth-camera and perspective controls, so the corrected mapping uses F10 for enable/disable, P for freeze, leaves telemetry unbound, and keeps F7/F9/F12.

The next target-machine test exposed a separate state-model defect: snapshot freeze was represented as a third autonomous mode and `toggleFrozen()` returned immediately while SawBot was disabled. Phase 1 sensors intentionally continue while autonomous control is disabled, so this made P appear broken during normal sensor inspection. Candidate `0.2.0-alpha.2` separates `observationsFrozen` from ENABLED/DISABLED. P now freezes snapshots in either control state, releases all held inputs, shows `FROZEN` beside the observation number, and blocks future autonomous action application until unfrozen.

## Completed

- Recorded and accepted the user's successful Phase 0 runtime test.
- Replaced the placeholder observation graph with typed immutable Java contracts.
- Added client-thread self-state extraction, including egocentric motion and collision-box support probes.
- Added a deterministic 13×9×13 player-centred terrain tensor.
- Added static semantic caching plus dynamic replaceability and exact multi-box collision classification.
- Added an incremental 33×33 mid-range map with a bounded 4,096-column LRU cache, cached support-height revalidation, periodic full-column rescans, and movement/cardinal-rotation re-projection.
- Added deterministic loaded-entity tracking with stable IDs, explicit LOS/occlusion/attackability, conservative team relations, and bounded omission count.
- Added fixed 41-slot inventory encoding and aggregate Bedwars resource counts.
- Added universal spawn landmark, bounded event history, ping/jitter and timing ages.
- Prevented target hurt-timer changes from being falsely labelled as SawBot damage; they are emitted as `ENTITY_HURT_OBSERVED` only.
- Added immutable snapshot sequence, timestamps, episode/world identity, validity flags, and per-sensor timing.
- Added F7 sensor inspector details and P snapshot freeze behavior independent of autonomous enable/disable state.
- Kept F10/F9/F12 safety behavior and input release intact; freezing also releases every controlled input.
- Updated GitHub CI, JAR verification, artifacts, and release publishing for `0.2.0-alpha.2`.

## Files created

Common observation/event contracts under:

- `sawbot-common/src/main/java/dev/fivesaw/sawbot/common/observation/`
- `sawbot-common/src/main/java/dev/fivesaw/sawbot/common/events/`

Forge sensors:

- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/sensors/BlockSemanticClassifier.java`
- `.../SelfStateSensor.java`
- `.../LocalTerrainSensor.java`
- `.../MidRangeMapSensor.java`
- `.../InventorySensor.java`
- `.../ItemClassifier.java`
- `.../EventSensor.java`
- `.../ServerTimingSensor.java`
- `.../ObservationPipeline.java`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/tracking/EntityTrackerSensor.java`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/map/LandmarkSensor.java`

Documentation/evidence:

- `docs/PHASE0_ACCEPTANCE.md`
- `docs/PHASE1_REPORT.md`
- `docs/OBSERVATION_CONTRACT.md`
- `docs/PHASE_GATES.md`

## Materially changed

- `ObservationSnapshot.java`
- `SchemaVersion.java`
- `ClientRuntime.java`
- `FoundationHud.java`
- `SawBotConfig.java`
- `SawBotStateController.java`
- `mcmod.info`
- verification stubs and `FoundationContractTest.java`
- root Gradle version metadata
- GitHub CI/release workflows
- release packaging/JAR verifier
- README, changelog, upload and release guides

The complete exact file list is generated as `docs/PHASE1_FILE_MANIFEST.txt` before packaging.

## Commands run

```text
bash tools/offline-verify.sh
```

Additional final-package checks run before release ZIP creation:

```text
bash -n gradlew tools/offline-verify.sh tools/package-release.sh
python3 -m py_compile tools/verify-built-jar.py
YAML parsing for both GitHub workflows
HTML parsing for prototypes/control-center.html
clean extraction and manifest comparison of the final ZIP
```

## Tests

- PASS — `FoundationContractTest` — 71 assertions.
- PASS — stable observation/action schema identifiers.
- PASS — action age, sequence, finite-value, and range validation.
- PASS — egocentric cardinal transforms.
- PASS — local terrain indexing and defensive copying.
- PASS — snapshot null-component rejection.
- PASS — entity and inventory immutability/bounds.
- PASS — mid-range indexing.
- PASS — incremental mid-range fill and cache re-projection after movement/rotation.
- PASS — repeated mid-range scans use a bounded support-height hint path.
- PASS — cactus is represented as a surface but never flagged as a safe landing.
- PASS — players without scoreboard teams remain `UNKNOWN`.
- PASS — same/different scoreboard teams map to teammate/enemy.
- PASS — bounded timing ring.
- PASS — complete emergency input release.
- PASS — end-to-end stub observation pipeline produces a bounded v0.2 snapshot.
- PASS — frozen pipeline preserves the last sequence.
- PASS — expanded `mcmod.info` parses as JSON.
- PASS — GitHub packaging-file checks.

## Performance

Measured in the offline verification environment:

- Java 8-targeted compilation and the 71 assertion suite complete successfully.
- All production queues/collections introduced in this phase are bounded: 32 entities, 64 landmarks, 64 events, 4,096 cached mid-range columns, fixed terrain/map arrays.
- Default snapshot rate is 10 Hz; the mid-range map updates two rows per client tick. Recent surface columns use a ±4-block support-height revalidation band and receive a full bounded rescan at least every 100 client ticks.

Not yet measured here:

- Real Forge sensor mean/p95/p99 cost.
- FPS impact on the HP EliteBook while moving through varied terrain.
- Entity-heavy world cost.

The in-game HUD exposes total and per-group sensor microseconds so the user test supplies those measurements. No performance claim is made before that test.

## Known limitations

- This environment cannot download the Loom/Minecraft/Forge dependencies, so the real remapped Forge JAR build is delegated to GitHub Actions and is not falsely reported as locally passed.
- Phase 1 renders textual inspector data, not Phase 2's full cell/entity world overlays.
- Landmark extraction only emits world spawn; semantic Bedwars landmarks require a later task adapter.
- Block placed/broken events are local-tensor change heuristics, not packet acknowledgements.
- Large position correction is currently a distance heuristic.
- `HIT_CONFIRMED`, `DAMAGE_DEALT`, and placement acknowledgement remain unknown until properly correlated; no false attribution is emitted.
- Mid-range full surface search is bounded to 8 blocks above and 24 below the player's current support level. Cached columns use a faster hint band between periodic full rescans, so a newly created disconnected surface more than four blocks above the prior surface may take up to 100 client ticks to replace the cached result.
- Team relation is deliberately `UNKNOWN` when scoreboard teams are absent.
- No model, actuator, autonomous movement, Bedwars logic, or telemetry writer exists in this phase.

## USER CHECKLIST

- [ ] GitHub CI passes both offline verification and the real Forge build.
- [ ] `SawBotV1-0.2.0-alpha.2-mc1.8.9.jar` launches.
- [ ] F7 opens/closes the Phase 1 inspector.
- [ ] Observation sequence advances about 10 times per second.
- [ ] Observation age normally remains below 300 ms.
- [ ] XYZ, yaw, health, wool, entity count, event count, and ping look plausible.
- [ ] Standing on full blocks shows near-zero centre support distance.
- [ ] Slabs, stairs, and fences produce plausible support values rather than full-cube assumptions.
- [ ] Walking to an edge increases one or more L/C/R support distances; standing over void reaches the capped value.
- [ ] Turning and moving does not permanently empty the mid-range map.
- [ ] A nearby entity increments entity count; hiding it behind blocks changes LOS/occlusion.
- [ ] Entity tracking ID remains stable while it stays loaded.
- [ ] Inventory iron/gold/diamond/emerald/wool counts update after changes.
- [ ] Placing/breaking a nearby block creates a bounded event without repeated spam.
- [ ] P freezes the observation sequence and values; pressing P again resumes updates.
- [ ] F10, F9, and F12 still pass.
- [ ] Five-minute movement test has no repeated SawBot error and no serious FPS collapse.
- [ ] Sensor total and per-group timing values are recorded.

## How to test

1. Push this repository update to GitHub and wait for **CI** to pass.
2. Trigger the **Release** workflow with version `0.2.0-alpha.2`, or push tag `v0.2.0-alpha.2`.
3. Download the installable JAR from the release and replace the old SawBotV1 JAR in the Forge 1.8.9 `mods` folder.
4. Join a local test world containing full blocks, slabs, stairs, fences, an edge/void, and a few item stacks.
5. Press F7 and observe sequence, age, sensor timings, support distances, terrain change count, inventory, entities, and events.
6. Move and turn for at least one minute. Place and break blocks near the player and change inventory resources.
7. Put another player or mob in range when possible; move it behind a wall and back into view.
8. Press P, note the observation number, move/change the world, and verify the number remains fixed. Press P again and verify it resumes.
9. Re-run F10, F9, and F12 safety checks.
10. Continue moving for five minutes, close Minecraft, and inspect `logs/latest.log`.

## Expected result

The mod remains non-autonomous. It publishes bounded internal-state snapshots and displays their core values/timings. P freezes the last immutable snapshot regardless of whether the control state is ENABLED or DISABLED. It does not move, aim, attack, place, shop, or play Bedwars. Safety keys remain authoritative.

## What to send back if it fails

- The failed GitHub Actions step and full error section.
- `latest.log` from the first SawBot exception through its final `Caused by` line.
- Screenshot for incorrect HUD values only.
- Exact block/entity/inventory setup and reproduction steps.
- The displayed observation sequence, age, total sensor time, and each per-sensor time.
- Normal FPS versus F7 inspector FPS.
- Which key or field failed and whether the failure persists after rejoining the world.
