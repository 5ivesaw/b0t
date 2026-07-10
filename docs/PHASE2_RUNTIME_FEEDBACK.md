# Phase 2 Runtime Feedback and Correction

Date: 2026-07-10  
Tested build: `0.3.0-alpha.0`  
Correction candidate: `0.3.0-alpha.1`

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

Phase 2 remains open until these corrections pass the target-machine retest.
