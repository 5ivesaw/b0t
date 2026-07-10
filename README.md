# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the agent's body. The mod reads structured internal game state, never screen pixels, and later actuates only the same legitimate client controls available to a player.

## Current gate

**Phase 0 passed real runtime acceptance on the target machine.** The repository now contains the **Phase 1 — Internal Eyes candidate** (`0.2.0-alpha.1`). Phase 1 must pass its GitHub build and in-client sensor checklist before Phase 2 begins.

Implemented Phase 1 state:

- Immutable Observation Contract `sawbot.observation/0.2`
- Self/body state and support/void probes
- Bounded 13×9×13 egocentric local terrain tensor
- Incremental 33×33 mid-range map with bounded cache, support-height fast path, and periodic full rescans
- Loaded-entity tracking with stable IDs, LOS, occlusion, attackability, and conservative team relation
- Fixed inventory/resource encoding
- Bounded landmarks, events, server timing, validity flags, and per-sensor timings
- F7 textual sensor inspector and P immutable snapshot freeze
- Conflict-free F10/F9/F12 safety controls

The repository still contains **no neural model, autonomous actuator loop, Bedwars policy, runtime pathfinder, aim assist, scaffold controller, packet advantage, screenshot/OCR pipeline, or public-server automation**.

The complete locked brief is preserved in [`docs/PROJECT_BRIEF.txt`](docs/PROJECT_BRIEF.txt). Phase reports and gates are in [`docs/PHASE0_ACCEPTANCE.md`](docs/PHASE0_ACCEPTANCE.md), [`docs/PHASE1_REPORT.md`](docs/PHASE1_REPORT.md), and [`docs/PHASE_GATES.md`](docs/PHASE_GATES.md).

## GitHub CI and release

Every push or pull request runs:

1. Java 8-targeted offline contract/sensor verification.
2. The 56 assertion foundation/Phase 1 test suite.
3. A real remapped Forge 1.8.9 build using the online GitHub runner.
4. Release-JAR structure/version validation.
5. Workflow artifact packaging.

To publish the Phase 1 candidate from GitHub:

1. Open **Actions → Release → Run workflow**.
2. Enter `0.2.0-alpha.1`.
3. Keep prerelease enabled.
4. Run the workflow.

The workflow creates tag `v0.2.0-alpha.1` and publishes the installable JAR, sources, checksums, Phase 1 report, Phase 0 acceptance evidence, and release guide.

## Toolchain

- Gradle 8.8
- Architectury Loom `0.10.0.5`
- Architectury Pack200 `0.1.3`
- Java 17 for Gradle
- Java 8 toolchain/bytecode for Minecraft
- Minecraft Forge `1.8.9-11.15.1.2318-1.8.9`
- MCP mappings `stable_22`


### Key-conflict correction in 0.2.0-alpha.1

Minecraft 1.8.9 already owns F5 for perspective, F6 for Twitch broadcast, and F8 for smooth camera. The Phase 1 defaults were moved so SawBot no longer triggers those vanilla actions. All keys remain rebindable under Options → Controls → SawBotV1.

## Local build

Install JDK 17 and JDK 8, then run:

```powershell
.\gradlew.bat clean ciBuild
```

The final remapped mod is written to:

```text
sawbot-forge-1.8.9/build/libs/SawBotV1-0.2.0-alpha.1-mc1.8.9.jar
```

Launch a development client with:

```powershell
.\gradlew.bat runClient
```

## Offline verification

```bash
bash tools/offline-verify.sh
```

This checks source-level Java 8 compatibility against narrow API stubs and runs 56 assertions. It does not replace the real GitHub Forge/Loom build or in-client acceptance test.

## Keys

- Telemetry intent is intentionally unbound until Phase 3
- `P`: freeze/unfreeze the immutable observation snapshot while SawBot is enabled
- `F7`: toggle the textual Phase 1 sensor inspector
- `F10`: enable/disable SawBot state
- `F9`: immediate manual takeover and release
- `F12`: emergency release all controlled inputs

Phase 1 sensors can be inspected, but no action-driving model exists and the mod must not move the player.

## Safety scope

Autonomous testing is restricted to single-player worlds, LAN tests, owned private servers, and purpose-built local arenas. The project prohibits anti-cheat bypasses, public-server automation, authentication bypasses, packet manipulation for advantage, altered reach, impossible placement, and silent server-only controls.
