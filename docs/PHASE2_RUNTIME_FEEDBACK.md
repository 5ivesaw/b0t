# Phase 2 Runtime Feedback and Correction

Date: 2026-07-10  
Tested build: `0.3.0-alpha.0`  
First correction tested: `0.3.0-alpha.1`  
Visual-state correction candidate: `0.3.0-alpha.2`

## Confirmed working on target hardware

- F7 inspector and all eight pages.
- B terrain tensor boundary and semantic cells.
- C collision/support overlay.
- N entity boxes, stable tracking IDs, and occluded-entity rendering.
- M landmark rendering.
- P freeze and `.` one-observation stepping.
- O JSON export.
- Difference page updates.
- F9 takeover and F12 emergency release.
- No serious FPS collapse and no repeated SawBot error.

## Runtime defects found

1. Tracer origin used the 10 Hz snapshot player position, so lines visibly jumped around the crosshair while the player moved.
2. LOS/OCC did not reliably refresh after an entity crossed a wall boundary.
3. Entity tracers could not be disabled independently from boxes and labels.
4. Raw world-spawn Y could place the semantic landmark underground.
5. The selected-block interaction was not explained clearly; it is automatic and the yellow outline is the selected block.

## Correction

- Tracer origin now uses the live interpolated client eye position.
- LOS is recomputed on every observation from seven rays to current entity bounding-box sample points.
- V independently toggles tracers; N continues to control boxes and labels.
- At most 16 tracers render per frame.
- The world-spawn landmark resolves to the nearest loaded standable surface and refreshes periodically.
- HUD help explicitly explains automatic yellow block selection.

## Target-machine retest of `0.3.0-alpha.1`

Confirmed:

- Tracers remain anchored while walking.
- V toggles tracers independently.
- LOS and OCC text switch quickly in both directions.
- Tracking IDs remain stable through wall transitions.
- Spawn landmark resolves to the surface.
- Block selection, stepping, export, differences, safety controls, FPS, and error checks pass.

Remaining defect:

- The text changed immediately, but selected-entity yellow and team-based colours masked or weakened the corresponding visual LOS/OCC transition.

## `0.3.0-alpha.2` correction

- Entity box, tracer, and label now use one current-observation visibility style.
- `LOS` is bright green; `OCC` is bright purple; inconsistent flags are orange.
- Selection is a second yellow outline and no longer replaces the visibility colour.
- OCC opacity is raised so the state does not appear to change only faintly.

Phase 2 remains open only for this focused visual-colour retest.
