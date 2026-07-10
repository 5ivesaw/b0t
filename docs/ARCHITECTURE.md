# Architecture

## Runtime data flow

`Minecraft client thread -> sensor snapshot -> HUD / telemetry / inference bridge -> deadline-bound ActionCommand -> safe actuator`

Only the client thread reads live Minecraft objects. Every downstream consumer receives immutable, bounded data. The client never waits indefinitely for I/O or inference. Missing, malformed, stale, or late actions resolve to a zero-action state and input release.

## Module boundary

- `sawbot-common`: Java 8, no Forge dependency. Versioned observation/action/protocol value types.
- `sawbot-forge-1.8.9`: visible client integration, sensors, actuator, HUD, telemetry, safety, and transport.
- `sawbot-trainer`: later Python training process; absent from runtime packaging.
- `sawbot-sim`: later mechanics-only simulator with differential tests.
- `sawbot-arenas`: private maps and reset logic.
- `sawbot-tools`: validators, inspectors, and benchmarks.
- `prototypes`: standalone development UI, never authoritative for runtime state.

## Phase 0 lifecycle

`SawBotMod` loads configuration and creates one `ClientRuntime`. The runtime registers on both Forge and FML event buses. Tick handling processes safety keys before any future action logic. `SawBotStateController` owns enabled/frozen/telemetry/inspector intent and releases every movement/combat key on disable, takeover, emergency stop, disconnect, and shutdown. `RollingTimingWindow` stores a fixed number of samples; the HUD renders only a small text block.

## Architectural decisions

### ADR-0001: Multi-project Gradle with only two active Java modules

The requested repository tree is preserved, but trainer/simulator/tool directories are not added to the Gradle graph yet. The Forge build lane is deliberately isolated, so Phase 0 minimizes build surface. This changes no future contract and avoids pretending incomplete modules are runnable.

### ADR-0002: Apache-2.0 project code; clean-room donor use

No utility-client or pathfinder source is copied. GPL projects are concept-only donors. Direct reuse requires a new written decision, exact commit pin, compatible licensing analysis, attribution, and tests.

### ADR-0003: Explicit dual-JDK build lane

Forge 1.8.9 output targets Java 8, while the maintained Gradle/Loom build process runs on Java 17. This lane remains isolated from later Python tooling.

### ADR-0004: Contract-first, no neural code in Phase 0

Observation and action names, bounds, ordering, validity semantics, and stale-message behaviour are documented before sensors or models. Neural training is blocked until Phase 2 sensor inspection passes.

## ADR-0005 — Modern GitHub build lane for legacy Forge 1.8.9

**Status:** Accepted

**Decision:** Build the Forge subproject with Gradle 8.8, Architectury Loom `0.10.0.5`, and the Architectury Pack200 adapter. Gradle runs on Java 17 while Java compilation uses a Java 8 toolchain.

**Reason:** ForgeGradle 2.1 and Gradle 2.x are legacy infrastructure with deteriorating compatibility on current hosted CI and dependency repositories. Architectury Loom is actively used by maintained Minecraft 1.8.9 Forge templates and can produce a remapped Forge artifact while allowing a modern Gradle runtime.

**Boundaries:** This is a build-system migration only. It does not alter the observation/action contracts, runtime safety requirements, task scope, source package structure, or Minecraft/Forge target.

**Consequences:** Local contributors need Java 17 for Gradle and Java 8 for compilation/runtime testing. CI installs both explicitly. Release artifacts are validated before publication.
