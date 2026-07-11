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

## Phase 1 observation lifecycle

At each client tick, `ObservationPipeline` verifies world identity, incrementally refreshes two mid-range map rows, and—at the configured interval—captures all live Minecraft state on the client thread. Sensor results are converted immediately into bounded common-module value objects. The volatile `latest` reference publishes one complete immutable graph to the HUD and future workers; no partially assembled snapshot is observable.

`LocalTerrainSensor` builds a cardinally egocentric 13×9×13 tensor. `BlockSemanticClassifier` caches static state semantics but computes dynamic replaceability and collision lists at the actual block position. `MidRangeMapSensor` stores absolute world-column samples in a bounded LRU cache and reprojects them into a 33×33 egocentric output, avoiding full invalidation on movement and rotation. `EntityTrackerSensor` assigns stable short-lived IDs and uses conservative scoreboard-team semantics. Inventory, events, landmark, and server timing sensors remain bounded.

### ADR-0006: Observation schema v0.2 uses typed immutable components

**Status:** Accepted

The Phase 0 placeholder snapshot was insufficient for runtime validation. Schema v0.2 introduces fixed arrays, bounded lists, explicit validity bits, and sensor timing. Constructors reject missing/non-finite state and defensively copy mutable inputs. This is a deliberate schema bump; v0.1 remains identified only for migration/testing history.

### ADR-0007: Mid-range spatial cache is world-coordinate, output is egocentric

**Status:** Accepted

Resetting a 33×33 map whenever the player crosses a block or changes cardinal direction would leave a moving agent with mostly unknown state. Phase 1 therefore maintains at most 4,096 absolute X/Z samples and reprojects them into the current egocentric frame. Two rows are refreshed per tick, keeping work bounded while preserving useful older samples with explicit ages.

### ADR-0008: Observed hurt state is not damage attribution

**Status:** Accepted

A target hurt timer can change because of another player, fall damage, fire, or other causes. Phase 1 emits `ENTITY_HURT_OBSERVED` and leaves `HIT_CONFIRMED`/`DAMAGE_DEALT` unknown until proper interaction and packet/event correlation exists. This avoids training on fabricated labels.

## Phase 2 decision — inspector state is outside the model contract

Phase 2 adds selected block/entity state, HUD pages, visual-overlay toggles, snapshot comparison, and debug-export status without changing Observation Contract v0.2. These are tooling concerns, not neural inputs. Keeping them outside `ObservationSnapshot` prevents debug UI choices from contaminating training data or forcing a schema bump.

`ObservationDiffCalculator` lives in `sawbot-common` because it is a deterministic pure operation over immutable snapshots. Minecraft crosshair selection, rendering, and file export remain in the Forge module.

Frozen mode now halts every observation sensor and the mid-range cache. A single explicit step captures once on the current client tick and returns to frozen mode. This is an observation-system step, not a world/server pause.

Snapshot debug export uses one bounded background worker and a four-item queue. Only immutable snapshots and immutable inspector metadata cross the thread boundary. The worker never reads Minecraft world objects.

## ADR-0009 — Unified premium interface design system

**Status:** Accepted

**Decision:** The in-game engineering HUD remains a compact text inspector until a future interface change proves that it improves inspection without obscuring gameplay. Decorative UI is not an architectural requirement.

**Reason:** The Phase 0–2 text HUD proved the sensor architecture but is not an acceptable long-term product interface. A unified system is needed to achieve high information density, stable hierarchy, immediate state synchronization, and premium interaction quality while remaining viable on Intel HD 520.

**Boundaries:** This decision does not alter observation/action contracts and does not permit visual interpolation to modify measured values. Emergency controls remain immediate. Blur, custom fonts, shadows, and animation are optional effects gated by benchmark results.

## Phase 3 telemetry lifecycle

Structured telemetry is deliberately downstream of immutable observation publication. `ClientRuntime` captures legitimate human key state and raw `MouseHelper` deltas at client-tick END, then associates those samples with the observation that preceded them. When the next observation arrives, `TelemetryService` closes the causal window as `observation -> following input ticks -> next observed outcome boundary`.

`TelemetrySession` owns one bounded `ArrayBlockingQueue` and one daemon writer thread. The client thread performs a non-blocking `offer` only. The worker receives immutable `TrajectoryStep` objects, performs deterministic binary encoding and per-record DEFLATE compression, writes through a 64 KiB buffer, appends CRC-protected framing, and finalizes `.sbt.partial` to `.sbt` only after a clean footer. No worker touches Minecraft world objects.

Manual takeover and emergency release remain control-safety operations and do not silently stop human telemetry. World unload changes the world contract and therefore requests a bounded session close. A crash or interrupted write leaves the `.partial` file intact for offline validation/recovery.

### ADR-0010: Causal input windows instead of same-instant labels

**Status:** Accepted

Pairing only the key/mouse state observed at snapshot time would misrepresent actions between 10 Hz observations and worsen human reaction-delay alignment. Each trajectory step therefore stores all bounded client-tick input samples captured after observation `O_n` and before `O_(n+1)`, then records `O_(n+1)` and its bounded events as the outcome boundary.

### ADR-0011: Recoverable record framing rather than Java serialization or high-volume JSON

**Status:** Accepted

The Phase 3 `.sbt` format is explicitly little-endian and versioned. Every independently compressed record stores lengths and CRC32; a clean footer stores step count, dropped-step count, terminal reason, and a rolling CRC across step payloads. This supports corruption detection, valid-prefix recovery, bounded memory, and future migration without exposing Java object graphs or relying on JSON as the sole training format.

## Phase 4 bridge and actuator boundary

`ModelBridge` owns all socket I/O on a daemon worker. The client thread publishes immutable `ObservationSnapshot` references with non-blocking bounded queue operations and polls the newest decoded `ModelActionEnvelope`. The worker never reads Minecraft world/entity objects.

`SafeActionActuator` is client-thread-only. Its authority is limited to ordinary key bindings, hotbar selection, and bounded visible rotation changes. `EnvironmentGuard`, `ActionContextValidator`, `PhysicalInputMonitor`, and `SawBotStateController` form independent safety gates. Any disconnect, blocked environment, stale action, human input, F9/F12, GUI conflict, or world unload invokes complete input release.

The dummy model is a protocol/actuator fixture. A future learned model must speak the same contract and receives no hidden actuator correction, runtime pathfinding, aim help, or task logic.


## Phase 5 learned-policy boundary

`NavigationWaypointController` creates one explicit semantic landmark on the client thread. The observation pipeline serializes it like every other landmark. The separate `waypoint_model.py` process decodes only the bridge payload, computes a fixed feature vector, runs the exported MLP, and returns an ordinary action referencing waypoint `#1000`.

The live model has no Minecraft imports, world handle, collision query, route search, or teacher dependency. `SafeActionActuator` remains the only component allowed to mutate legitimate controls. It tracks ownership of synthetic continuous keys so an idle/disabled bridge cannot clear human input.

### ADR-0012: First learned behavior is an exported tiny MLP, not a runtime teacher

**Status:** Accepted

The first policy is intentionally small and inspectable: 18 normalized features, 32 `tanh` units, and 7 logits. It proves the complete data → train → evaluate → export → local bridge → safe actuator path before bridging or Bedwars logic is attempted. Reproducibility assets and failure examples are versioned; the release build verifies rather than retrains the model.
