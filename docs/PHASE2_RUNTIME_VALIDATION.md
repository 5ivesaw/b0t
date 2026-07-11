# Phase 2 Target-Machine Runtime Validation — alpha.6

Version: `0.3.0-alpha.6`
Target: HP EliteBook 840 G3, i5-6200U, Intel HD 520, 8 GB RAM, Minecraft Forge 1.8.9

## Accepted target-machine behavior

The user confirmed all of the following on the real target client:

- Compact text HUD restored and updating.
- No SawBot crash during the test session.
- F10 enable/disable, F9 takeover, and F12 emergency release work.
- F7 inspector toggle and all eight H pages work and loop.
- P freezes the observation sequence while Minecraft continues running.
- `.` captures exactly one new observation and resets its age.
- B terrain, C collision/support, N entity, V tracer, and M landmark toggles work.
- Entity tracking IDs remain selectable with `[` and `]`.
- LOS/OCC transitions and green/purple status changes work quickly.
- Snapshot export and snapshot-difference pages work.
- No serious FPS collapse was observed.
- GitHub automatically built, tagged, and published the release after a push to `main`.

## Evidence inspected

The supplied snapshot was structurally valid with `sensorValidityFlagsHex = 0x1ff`. It contained 19 bounded entity observations, including 13 broad living entities and 6 dropped items; 13 were LOS and 6 were occluded. The sensor total was approximately 3.08 ms, with terrain and mid-range map extraction each around 1.25–1.31 ms. This is inside the Phase 1/2 10 Hz extraction budget on the target machine.

The supplied log contained no SawBot exception, stack trace, or repeated SawBot error. The only `ERROR` entries were Minecraft 1.8.9 Realms 404/malformed-response messages, unrelated to SawBot.

## Findings and corrections

### Frozen overlays were mistaken for sensor failure

Screenshots showing terrain/entity overlays at an old location also showed `FROZEN` with snapshot ages between roughly 55 and 109 seconds. Frozen snapshots are intentionally world-anchored: terrain, entities, and landmarks remain at the positions captured by that immutable snapshot even if the player walks away.

Alpha.6 makes this explicit:

- HUD displays the frozen capture coordinates.
- HUD displays distance moved away from the capture position.
- A `FROZEN SNAPSHOT #...` world marker is rendered at the capture anchor.
- The Entities page explains that `.` recaptures at the current location.
- Frozen tracers originate from the frozen eye position, keeping the visualization internally consistent.
- Unfreezing forces an immediate capture, avoiding a stale orange flash.

### Entity labels were too broad and misleading

Alpha.5 exposed `LIVING` plus `TeamRelation.UNKNOWN` for ordinary mobs. The relation was technically honest but visually misleading, and `LIVING` was not a sufficient entity type for the observation contract.

Alpha.6 bumps the observation schema to `sawbot.observation/0.3` and adds:

- Stable bounded `EntityType` values such as `COW`, `SPIDER`, `ZOMBIE`, `SKELETON`, and `DROPPED_ITEM`.
- A payload item category for dropped-item entities.
- Player team relation only where it is semantically useful in world labels.
- Specific entity type and payload information in the HUD and JSON export.

### Block-selection colour contradicted the instructions

The selected block was red when it was outside the frozen/current tensor. Alpha.6 always renders crosshair block selection in yellow. Tensor membership remains explicit in text instead of overloading selection colour.

### Export and notice text never expired

Successful export and transient inspector notices now automatically clear after a short interval. Errors remain visible longer. The latest exported file remains available on the System page.

### Possible HUD dimming from render-state leakage

World debug rendering now pushes and restores the complete OpenGL attribute state and explicitly restores white colour, alpha, culling, depth, texture, lighting, blending, and line width. This prevents collision or entity overlays from leaking state into the vanilla hotbar/HUD renderer.

## Remaining landmark scope

`WORLD_SPAWN` is a universal test landmark only. It is not a Bedwars bed, team spawn, generator, or user-defined waypoint. Bedwars semantic landmarks and manually authored arena landmarks belong to the Bedwars task adapter and are intentionally deferred.
