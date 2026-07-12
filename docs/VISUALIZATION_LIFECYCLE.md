# Visualization Lifecycle

Version: `1.2.0-alpha.0`

Debug rendering is diagnostic state, not permanent world decoration. Every overlay must
have an owner, an active condition, a hard render bound, and an immediate cleanup path.

## Bridge overlay

The bridge plan is renderable only while the bridge body has a current plan and is in an
active placement/execution state.

The overlay is cleared immediately on:

- navigation taking ownership
- bridge intent becoming inactive
- F9 takeover
- F12 emergency release
- F10 disable
- physical-input takeover
- waypoint change or clear
- world unload
- bridge completion
- model/body disconnection
- runtime exception

A short text status may remain for two seconds after a completed or blocked operation,
but the world-space plan itself is removed immediately.

## Navigation overlay

Search exploration and route markers are inspector-only and are hidden when navigation
is idle, waiting, paused, or arrived.

Hard render caps:

- search edges: 160
- route markers: 48
- bridge markers: 16

Only the current route window and current bridge window are rendered. Rendering does not
walk the full search history or complete long path every frame.

## Render-state safety

Every world overlay restores matrix, texture, depth, lighting, blend, alpha, culling,
line width, and color state after rendering. Overlay cleanup is independent from OpenGL
cleanup: a released body must clear its data even if the renderer is not called again.

## HUD lifecycle

The bridge HUD appears while the body owns input, has explicit intent, or has a recent
two-second status. A deactivated body returns to `IDLE` and disappears from the HUD.
Persistent counters remain available on the inspector pages without forcing a permanent
three-line bridge block on ordinary gameplay.
