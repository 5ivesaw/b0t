# Phase 2 Runtime Interface Refresh

Version: `0.3.0-alpha.4`

Status: **source-complete, offline-verified, pending real-client visual acceptance**

## Purpose

The original Phase 2 HUD proved the sensor pipeline, but presented it as a large wall of raw debug text. This update keeps every accepted sensor, safety control, inspector page, export path, and world overlay while replacing that presentation with a compact progressive interface.

The implementation follows `docs/INTERFACE_DESIGN_SYSTEM.md` and is deliberately designed around the target HP EliteBook 840 G3 rather than relying on shaders, blur, external fonts, or GPU-heavy effects.

## Implemented runtime UI

### Compact status island

The always-visible surface now contains only:

- SawBot safety state
- frozen/live observation state
- observation sequence and age
- sensor extraction cost
- client tick
- critical freshness colour

The previous multi-line debug wall is removed from the default state.

### Inspector workspace

F7 opens a stable glass-card workspace with:

- eight inspector pages
- page counter and page navigation indicator
- page-specific cards and rows
- selected block/entity context
- real keybinding labels
- independently visible overlay toggles
- freeze, step, export, target-selection, and takeover hints
- empty-state explanations
- HUD, sensor, handler, and world-overlay timing

### Visual system

- Centralized semantic tokens in `UiTheme`
- Cached rounded-corner inset geometry in `GlassUi`
- Bounded translucent cards and borders
- Status-aware chips and progress indicators
- Green LOS, purple OCC, cyan support, yellow selection, red danger
- Monotonic presentation-only transitions through `MotionValue`
- Configurable reduced-motion mode through `hudAnimationsEnabled=false`
- F3-aware compact HUD relocation

### World overlays

Floating labels now use a dark backed chip with a semantic accent rail instead of unbounded text. The selected block also receives a contextual chip while the inspector is open.

## Architectural separation

The update does not move sensor extraction, policy state, or action logic into UI classes.

- `FoundationHud`: layout and page composition
- `GlassUi`: reusable drawing primitives
- `UiTheme`: visual constants
- `MotionValue`: presentation-only animation state
- `KeyLabel`: current user-facing key names
- `WorldDebugRenderer`: world-space debug geometry and backed labels

No neural, actuator, Bedwars, teacher, packet-manipulation, or screen-processing code was added.

## Performance design

- No blur or framebuffer effects
- No external fonts
- No per-frame world scans
- Rounded geometry uses precomputed corner insets
- Inspector-only detailed string work
- Compact HUD remains small when F7 is closed
- HUD render timing is measured independently and shown on the System page
- Animations can be disabled without changing safety or observation state

## Offline verification

The Java 8 verifier currently passes 541 assertions, including:

- existing contracts, sensors, safety, freeze, stepping, export, and LOS/OCC tests
- semantic UI colour separation
- opacity preservation
- visual radius hierarchy
- presentation-only motion convergence and immediate reduced-motion snapping
- key labels resolving from actual configured bindings
- required premium UI classes in the release JAR contract

## Real-client acceptance checklist

- [ ] Compact status island replaces the old text wall with F7 closed
- [ ] F7 opens and closes smoothly without delaying state changes
- [ ] All eight pages remain readable at 1366×768
- [ ] H changes the highlighted page and page contents
- [ ] Overlay chips accurately reflect B/C/N/V/M state
- [ ] Key hints reflect any rebinding made in Minecraft Controls
- [ ] P, `.`, O, F9, and F12 remain immediate
- [ ] Selected block/entity cards update correctly
- [ ] World labels have dark backings and semantic accent rails
- [ ] Selected-block world label appears only while inspector is open
- [ ] F3 no longer directly covers the compact status island
- [ ] System page shows HUD average/max render time
- [ ] HUD average remains below 0.60 ms with inspector open on the target machine
- [ ] No serious FPS collapse or repeated log error during a five-minute test

## What to report if a visual problem occurs

Provide:

1. A screenshot at 1366×768
2. Minecraft GUI Scale setting
3. Whether F3 was enabled
4. Inspector page name
5. HUD average/max from the System page
6. Exact control or overlay combination
7. Relevant SawBot errors from `latest.log`, if any
