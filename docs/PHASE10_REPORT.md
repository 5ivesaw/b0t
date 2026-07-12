# Phase 10 Report — Continuous Anytime Navigation

Version: `1.1.0-alpha.0`

## Reason for the overhaul

Target-machine testing showed that Phase 9 still behaved too much like a completed
route follower. It could attempt blind collision jumps, wait for replacement planning,
and present only one chosen route instead of visibly improving alternatives while
moving.

## Implemented

- Added `AnytimeMovementSearch`, an incremental direction-aware weighted-A* search.
- Added safe best-so-far path publication every bounded search slice.
- Added continued search after provisional movement begins.
- Added rolling replanning from the player's actual current cell using the retained
  immutable corridor.
- Added streamed replacement paths and live splicing.
- Added bounded search-frontier diagnostics for the F7 world inspector.
- Removed collision-triggered blind jumping.
- Added strict one-block vertical transition rules.
- Prevented intermediate operation proximity from pulsing movement off.
- Preserved live route validation, displacement reconciliation, stuck recovery,
  private-environment gating, and immediate input release.

## Verification

Offline verification covers:

- early provisional publication before search completion;
- eventual completion of the same incremental search;
- hard node and search-debug bounds;
- refusal of an impossible two-block ascent;
- routing around blocked cells;
- typed ascent/descent operations;
- bounded asynchronous worker publication and shutdown;
- current/staged path rewind, skip, corridor recovery, and splicing;
- immutable snapshot capture and persistent world cache behavior;
- all existing observation, telemetry, model bridge, actuator, bridging, packaging,
  and safety contracts.

The real Forge build remains gated by GitHub Actions because external Gradle artifact
resolution is not guaranteed in the local artifact environment.
