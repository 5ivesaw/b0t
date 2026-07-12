# Phase 11 Report — Reference-Driven Bodies and Visualization Lifecycle

Version: `1.2.0-alpha.0`

## Goal

Stop treating each mechanical subsystem as an isolated first attempt. Establish a
repeatable reference-review process, fix the bridge face-selection failure visible on
the target machine, and make every debug visualization obey body ownership and expiry.

## Implemented

### Reference review

- Added a pinned source and license audit covering Baritone, mineflayer-pathfinder,
  AltoClef, MightyMiner, and Wurst ScaffoldWalk.
- Defined a required reference matrix for navigation, bridging, human motion, combat,
  inventory/shop, block interaction, and visualization bodies.
- Preserved the Apache-2.0 boundary; no GPL, LGPL, or unlicensed implementation text was
  copied.

### Bridge placement

- Replaced first-match face selection with all-adjacent-support enumeration.
- Samples nine legal hit vectors per candidate face.
- Rejects candidates outside normal reach.
- Requires the ray trace to hit the selected support.
- Scores visible candidates by yaw change, pitch change, distance, and preferred bridge
  direction.
- Records evaluated/visible face counts in the HUD.
- Throttles repeated `AIM_BLOCKED` failures before forcing a replan.
- Clears the bridge plan immediately after completion or ownership loss.

### Visualization lifecycle

- Bridge overlays disappear on navigation handoff, disable, takeover, emergency stop,
  waypoint change/clear, completion, world unload, and disconnection.
- Navigation search diagnostics disappear when the navigator is idle, waiting, paused,
  or arrived.
- Search rendering is capped at 160 edges.
- Route rendering is capped at 48 nearby markers.
- Bridge rendering is capped at 16 markers around the current step.
- Bridge HUD status expires rather than remaining permanently after a stale operation.

### Runtime integration

- Waypoint changes invalidate both old navigation and bridge plans immediately.
- Navigation priority explicitly deactivates the bridge body instead of only releasing
  whichever key happened to be held.
- Navigation release now clears staged diagnostics and reports `IDLE`.

## Verification

- Java 8 lint-clean offline compilation.
- Existing foundation, navigation, segmented-navigation, and bridging contracts.
- New bridge face-sampling assertions.
- New bridge overlay lifecycle assertions for priority handoff and waypoint clear.
- Synthetic JAR validation and release-payload verification.
- Patch base-to-target simulation.

The real Forge/Loom build remains the GitHub Actions gate because the local environment
cannot resolve the Gradle distribution service.
