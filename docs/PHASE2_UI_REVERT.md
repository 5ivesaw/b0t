# Phase 2 UI Revert — 0.3.0-alpha.5

## Reason

The `0.3.0-alpha.4` glass/card HUD occupied too much of the 1366×768 game view and made the engineering checks harder. It was rejected immediately during target-machine testing.

## Restored behavior

- Compact text HUD when the inspector is closed.
- Compact text inspector pages when F7 is open.
- Existing B/C/N/V/M world overlays.
- Green LOS, purple OCC, and separate yellow selection accent.
- P freeze, `.` step, O export, F9 takeover, and F12 emergency behavior unchanged.
- Single-push automatic GitHub release behavior retained.

## Removed

- Glass/card HUD renderer.
- UI motion and theme helpers.
- UI-only verification stubs and tests.
- Decorative interface-design charter.

This release intentionally makes no new sensor or autonomous-control claims.
