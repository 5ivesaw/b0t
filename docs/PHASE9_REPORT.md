# Phase 9 Report — Segmented Navigation Core

Version: `1.0.0-alpha.0`

## Result

Phase 9 replaces the rigid navigation implementation with a bounded asynchronous
movement-operation system. This is a major body-architecture milestone, not target
runtime acceptance.

## Implemented

- immutable compact navigation grids
- incremental local-first/full-corridor client-thread capture
- single bounded latest-wins planner worker
- weighted movement A* with traverse/diagonal/ascent/descent operations
- direct validated micro-route startup
- active and staged replacement routes
- segment diagnostics and planning ahead
- exact rewind/skip after displacement
- bounded nearby-corridor projection
- replacement splicing at shared safe positions
- persistent bounded world caches
- live active/future operation validation
- immediate and rolling current-position replanning
- continuous key ownership and physical-state restoration
- visible accelerated yaw servo
- validated operation lookahead
- sprint/jump execution and operation timeout
- stuck watchdog and bounded recovery
- route rendering disabled outside the F7 inspector and capped near current operation
- client-runtime shutdown of planner worker

## Performance design

The Minecraft client thread performs only bounded world sampling, small queue
operations, live near-future validation, and input application. A* search is isolated on
one daemon worker and consumes immutable arrays. The default capture budget is reduced
to 220 standability cells per client tick; default search cap is 4096 nodes with a 1.12
weighted heuristic.

## Verification

Offline verification includes:

- all prior foundation, sensor, telemetry, bridge, actuator, and safety contracts
- legacy navigation compatibility contracts
- immutable snapshot copy isolation
- typed ascent/descent operation planning
- obstacle routing under node bounds
- rewind, skip, corridor projection, and stale-route refusal
- replacement path splicing
- local-before-full incremental capture
- real background worker request/result/shutdown
- persistent cache reuse and live refresh
- complete Java 8 lint-clean compilation of common, Forge, stubs, and tests
- release metadata, scripts, workflow, payload, and synthetic JAR checks

## Gate

**SOURCE/OFFLINE PASS; TARGET-MACHINE RUNTIME ACCEPTANCE PENDING.**

Phase 8 bridging remains present but was not accepted by the user because navigation
needed replacement first. A later major integration review should test navigation,
bridging handoff, telemetry, HUD feedback, and takeover together.
