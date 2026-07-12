# Phase 6 Report — Hybrid Navigation Body

Version: `0.7.0-alpha.0`

## Architectural correction

Phase 5 proved that a small action classifier can receive observations and emit valid actions, but target-machine testing showed the wrong abstraction: short predicted motor actions produced pulsed movement, slow robotic rotation, spin loops, weak obstacle handling, and no reliable route reasoning.

Phase 6 moves deterministic mechanics into specialist body controllers while preserving learned decision-making above them.

## Implemented

- pure Java incremental bounded A* in `sawbot-common`
- Forge world-grid adapter with loaded/headroom/support/liquid/hazard checks
- deterministic path follower with sustained key ownership
- responsive bounded camera steering
- step-up jumping and straight-segment sprinting
- stable arrival hysteresis
- stall detection, recovery, and replanning
- route and current-node world rendering
- compact navigation HUD and MODEL-page diagnostics
- direct G-waypoint operation without requiring a model process
- high-level brain intent acceptance through `Skill.NAVIGATION` and waypoint `#1000`
- low-level model movement ignored while the navigation body owns the skill
- physical input restoration on release, fixing normal walking/flying interruption
- five-second F9/F12 notices
- stable offline brain label and slower reconnect default
- telemetry restart simplified to one K press
- malformed telemetry steps are rejected individually without terminating capture

## Verification

Offline verification covers:

- flat deterministic routes
- explicit per-step expansion budgets
- obstacle detours
- one-block ascent
- disconnected/void failure
- hazard, liquid, and support rejection
- body planning, visible turning, sustained forward ownership, and release
- restoration of a physically held key
- existing observation, telemetry, bridge, actuator, packaging, and Phase 5 historical-model contracts

## Scope

This phase is the first mechanical body component. It is not yet a full Baritone replacement and does not include parkour, bridging, block breaking, long-range chunk routing, inventory, shopping, combat, or Bedwars strategy.

## Verification result

- `FoundationContractTest`: 607 checks passed.
- `NavigationBodyContractTest`: 39 checks passed.
- Total Java contract/navigation checks: 646.
- Telemetry framing, CRC, recovery, and replay verification passed.
- Dummy bridge protocol self-test passed.
- Historical Phase 5 checkpoint/data/evaluation verification passed.
- Synthetic release-shaped JAR and complete Phase 6 payload verification passed.
- Workflow YAML, Bash syntax, Python compilation, manifest paths, and release asset lists passed.

The real Forge/Loom build could not run in the isolated build environment because `services.gradle.org` DNS resolution was unavailable. GitHub Actions remains the authoritative real Forge compile, remap, and release gate.
