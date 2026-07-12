# Phase 7 Report — Real-Time Adaptive Navigation

Version: `0.8.0-alpha.0`

## Result

Phase 7 converts the Phase 6 static route follower into a receding-horizon navigation
body designed for fast games. Global search, local steering, route validation, input
ownership, and recovery remain deterministic specialist mechanics. The neural brain
selects the destination and tactical intent.

## Main implementation

- Anytime bounded A* publishes a safe best-frontier route before full completion.
- Active movement continues while a replacement route is searched.
- Rolling plans begin from the player's actual current position.
- External release/takeover invalidates stale route ownership.
- A route cursor re-anchors after displacement instead of requiring exact node visits.
- Safe lookahead skips unnecessary block centres.
- Live path-window refresh detects block/support/hazard changes.
- Multiple local steering headings are evaluated every client tick.
- Exposed-edge and turn penalties improve route quality without forbidding narrow paths.
- Faster bounded visible camera response replaces the slow Phase 6 steering profile.
- Sustained input ownership remains in place; no repeated short W pulses are used.
- Compact live diagnostics and route rendering expose all adaptive decisions.

## Configuration defaults

- search radius: 40 horizontal / 10 vertical blocks
- maximum expanded nodes: 6,144
- expansions per client tick: 96
- rolling replan interval: 4 ticks
- maximum yaw change: 32 degrees per tick
- lookahead: 7 nodes / 5 metres
- live path validation: 12 nodes
- off-route threshold: 2.35 metres
- local steering probe: 1.25 metres plus bounded speed compensation

Every value is bounded in Forge configuration.

## Verification

Offline Java verification covers:

- deterministic complete A* routes
- anytime frontier routes
- route-cursor re-anchoring and node skipping
- obstacle routing and one-block steps
- disconnected-void rejection
- hazard and missing-support rejection
- live support-change invalidation
- responsive turn plus sustained forward ownership
- fresh current-position planning after release/displacement
- restoration of real physical keyboard state

Repository verification additionally checks:

- Java 8 compilation with warnings as errors
- required adaptive classes in the release JAR contract
- no worker-thread access to Minecraft world objects
- Phase 7 CI/release assets and version metadata
- existing telemetry, bridge, actuator, model-baseline, inspector, and safety contracts

## Honest status

This is a significantly more adaptive navigation body, not a complete Baritone
replacement and not yet a Bedwars-complete movement stack. Long-distance chunk-aware
planning, parkour, ladders, doors, block breaking, bridging, entity-aware collision,
combat movement, and tactical replanning remain separate future specialists.
