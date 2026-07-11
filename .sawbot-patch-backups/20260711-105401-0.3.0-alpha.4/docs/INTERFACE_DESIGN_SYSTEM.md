# SawBotV1 Interface Design System

Status: **Locked direction for all new HUD, inspector, control-centre, and training UI work**

SawBotV1 must feel like a deliberately designed professional client, not a pile of debug text. The visual target is a restrained, premium desktop interface inspired by the clarity, spacing, layering, and motion discipline of current macOS and iOS—adapted to Minecraft rather than copied literally.

The design must exceed ordinary utility-client presentation in information hierarchy, consistency, responsiveness, and finish. Functionality remains primary, but engineering information must be understandable at a glance.

## 1. Core principles

1. **Calm density** — show large amounts of state without visual noise.
2. **Immediate hierarchy** — status, risk, selected target, and failures must be recognizable before detailed values.
3. **Progressive disclosure** — the default HUD remains compact; deeper pages and overlays appear only when requested.
4. **One visual language** — runtime HUD, world overlays, standalone tools, and future trainer dashboard share the same tokens and interaction rules.
5. **Motion with purpose** — animation confirms state transitions; it never delays emergency controls or obscures measurements.
6. **Performance is part of appearance** — a polished interface that stutters is a failed interface.
7. **Debug truth over decoration** — visual state must derive from the same immutable observation used by text and exports.

## 2. Visual structure

### Compact status island

The always-visible surface should become a small status island rather than a full text wall. It should contain only:

- SawBot state and emergency status
- Observation freshness
- Active objective/skill when those phases exist
- Critical warning indicator
- Compact timing health

Expanded data belongs in inspector cards opened by F7.

### Inspector workspace

The inspector should use a modular card stack:

- Header with page title, sequence, freeze state, and connection health
- Primary card for the selected page
- Secondary contextual card for selected block/entity
- Bottom key-hint strip that hides automatically after interaction

Cards should align to an 8-pixel spacing system and maintain stable positions between pages so content does not jump unnecessarily.

### World overlays

World overlays must use consistent semantic color and stroke rules:

- LOS: green
- OCC: purple
- Invalid/inconsistent: orange
- Selection: yellow accent independent of semantic state
- Support/safe: cyan
- Hazard/void: red
- Landmark/objective: role-specific accent with readable label backing

Labels should use short chips with a subtle dark backing, not unbounded floating text. Tracers, boxes, labels, and inspector rows must all derive color from the same current observation.

## 3. Design tokens

Runtime code should centralize these values rather than scatter magic constants.

### Spacing

- `space.1 = 2 px`
- `space.2 = 4 px`
- `space.3 = 8 px`
- `space.4 = 12 px`
- `space.5 = 16 px`
- `space.6 = 24 px`

### Corner radii

Minecraft 1.8.9 does not provide native rounded clipping. Rounded panels should therefore be implemented through cached geometry or nine-slice textures, never expensive per-frame pixel construction.

- Small chip: 4 px
- Card: 7 px
- Large panel: 10 px

### Layer opacity

- HUD card background: approximately 72–82% dark opacity
- Secondary card: approximately 62–72%
- Hairline border: 18–28% white opacity
- Selected/active glow: subtle and bounded

Blur is optional and must be disabled by default until measured on Intel HD 520. A clean translucent panel without blur is preferred over a slow imitation of glass.

### Typography

- Use a compact primary UI face with strong small-size legibility.
- Use tabular numerals for timing, sequence, counts, and coordinates where available.
- Use weight and opacity before adding more color.
- Reserve all-caps for short state labels only.
- Never bundle an unlicensed font.

Minecraft's default font remains the compatibility fallback. Future font rendering must be cached and benchmarked before becoming the default.

## 4. Motion system

Animations should use monotonic time and remain independent of server tick rate.

- Hover/selection response: 90–120 ms
- Card expansion/collapse: 140–180 ms
- Page transition: 160–220 ms
- Warning pulse: no faster than 700 ms
- Emergency stop: immediate, with no animation delay

Allowed motion:

- Opacity crossfade
- Short vertical translation
- Width reveal for status chips
- Numeric interpolation only for presentation, never for the underlying measured value

Disallowed motion:

- Constant decorative bouncing
- Large elastic overshoot
- Delayed safety-state changes
- Transitions that make values unreadable

Reduced-motion mode must disable nonessential transitions.

## 5. Interaction quality

- Every toggle gives immediate visual confirmation.
- Conflicting bindings are detected and shown in Controls.
- Inspector pages preserve the selected entity when possible.
- Empty states explain what is missing instead of showing blank panels.
- Errors include actionable context.
- Freeze, step, export, takeover, and emergency stop remain usable even when another panel is open.
- Key hints show the user-facing binding, not a hardcoded key name.

## 6. Performance budget

Target machine: HP EliteBook 840 G3, i5-6200U, Intel HD 520, 8 GB RAM, 1366×768.

Normal compact HUD:

- CPU preparation target: under 0.15 ms average
- Render target: under 0.30 ms average
- Zero unbounded allocation

Expanded inspector without world overlays:

- Render target: under 0.60 ms average

Each world overlay is measured independently. Expensive effects must be separately toggleable. Text layout, panel meshes, and static labels should be cached. The interface must degrade gracefully by removing blur, shadows, and animation before dropping debug correctness.

## 7. Implementation architecture

Future visual work should converge on:

- `UiTheme` — semantic colors, opacity, spacing, radii, typography metrics
- `UiMetrics` — scale and viewport-safe layout
- `GlassPanelRenderer` — cached panel geometry and borders
- `TextRunCache` — bounded text measurement/layout cache
- `MotionValue` — monotonic presentation-only transitions
- `HudCard` — reusable card contract
- `WorldOverlayStyle` — one semantic style source for boxes, tracers, and labels

No class should mix sensor extraction, policy state, and drawing.

## 8. Acceptance standard

A UI change is not accepted merely because it renders. It must demonstrate:

- Correct state and color synchronization
- Stable layout at 1366×768
- Readability over bright and dark worlds
- No key conflict
- No serious FPS loss
- Measured render cost
- Consistent behavior when frozen, disabled, disconnected, or in emergency stop
- Screenshots showing compact, expanded, warning, and empty states

The intended result is a distinctive SawBot interface: premium, restrained, fast, technically honest, and visibly more considered than a generic Minecraft utility HUD.
