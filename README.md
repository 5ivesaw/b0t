# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the agent's body. The mod reads structured internal game state, never screen pixels, and actuates only legitimate client controls inside local/private test environments.

## Current gate

**Phases 0, 1, and 2 passed target-machine runtime acceptance.** Phase 3's binary format and offline validation pass, but its target-machine writer needs one focused retest after the bundled reliability fixes. This repository contains the **Phase 4 — Safe Actuator and Local Model Bridge candidate** (`0.5.0-alpha.0`).

Implemented:

- Observation Contract `sawbot.observation/0.3`
- Action Contract `sawbot.action/0.1`
- Telemetry Contract `sawbot.telemetry/0.1`
- Local bridge protocol `sawbot.bridge/0.1`
- Bounded self, terrain, map, entity, inventory, event, timing, and landmark observations
- Compact text inspector, frozen snapshots, single-step capture, overlays, and JSON export
- Structured human gameplay telemetry with validation, replay, CRC, and recovery
- Background local model transport with bounded queues and latency metrics
- Strict client-thread safe actuator for legitimate controls
- Environment guard, action deadlines, physical takeover, disconnect release, F9, and F12
- Deterministic dummy model and actuator demo
- Single-push automatic GitHub build/tag/release

The repository still contains **no learned neural policy, Bedwars strategy, handcrafted runtime pathfinder, aim helper, scaffold controller, packet advantage, screenshot/OCR pipeline, or public-server automation**.

Key reports:

- [`docs/PHASE0_ACCEPTANCE.md`](docs/PHASE0_ACCEPTANCE.md)
- [`docs/PHASE1_ACCEPTANCE.md`](docs/PHASE1_ACCEPTANCE.md)
- [`docs/PHASE2_ACCEPTANCE.md`](docs/PHASE2_ACCEPTANCE.md)
- [`docs/PHASE3_RUNTIME_STATUS.md`](docs/PHASE3_RUNTIME_STATUS.md)
- [`docs/PHASE4_REPORT.md`](docs/PHASE4_REPORT.md)
- [`docs/MODEL_BRIDGE_PROTOCOL.md`](docs/MODEL_BRIDGE_PROTOCOL.md)
- [`docs/PHASE_GATES.md`](docs/PHASE_GATES.md)

The locked project brief remains in [`docs/PROJECT_BRIEF.txt`](docs/PROJECT_BRIEF.txt).

## Build and automatic release

A push to `main` runs offline verification, builds and remaps the real Forge 1.8.9 JAR, validates the release payload, creates tag `v0.5.0-alpha.0`, and publishes the GitHub Release.

```powershell
git add -A
git commit -m "Implement Phase 4 actuator and model bridge"
git push origin main
```

## Toolchain

- Gradle 8.8
- Architectury Loom `0.10.0.5`
- Java 17 for Gradle
- Java 8 bytecode/toolchain for Minecraft
- Forge `1.8.9-11.15.1.2318-1.8.9`
- MCP `stable_22`

Build:

```powershell
.\gradlew.bat clean ciBuild
```

Installable JAR:

```text
sawbot-forge-1.8.9/build/libs/SawBotV1-0.5.0-alpha.0-mc1.8.9.jar
```

Offline verification:

```bash
bash tools/offline-verify.sh
```

## Controls

All bindings are configurable under **Options → Controls → SawBotV1**.

| Key | Function |
|---|---|
| F10 | Enable/disable safe actuator; requires allowed scope and model `READY` |
| F9 | Immediate manual takeover |
| F12 | Emergency release all controlled inputs |
| F7 | Show/hide compact inspector |
| P | Freeze/unfreeze immutable observation |
| . | Capture one observation while frozen |
| H | Next inspector page, including MODEL |
| B | Terrain overlay |
| C | Collision/support overlay |
| N | Entity boxes and LOS/OCC labels |
| V | Entity tracers |
| M | Landmark overlay |
| [ / ] | Previous/next tracked entity |
| O | Export current snapshot JSON |
| K | Start/stop structured telemetry |

## Phase 4 dummy model

Start the interactive bridge server before enabling SawBot:

```text
sawbot-tools\dummy-model\RUN-DUMMY-MODEL.bat
```

Or run the deterministic control sequence:

```text
sawbot-tools\dummy-model\RUN-ACTUATOR-DEMO.bat
```

Interactive commands include movement, jump/sprint/sneak, smooth yaw/pitch, hotbar selection, attack, use, drop, inventory, idle, demo, status, and quit. The dummy model is testing infrastructure, not a neural policy.

## Runtime paths

Snapshot exports:

```text
.minecraft/sawbotv1/exports/
```

Structured telemetry:

```text
.minecraft/sawbotv1/telemetry/
```

Validate the newest trajectory on the configured Windows test machine with:

```text
tools\TEST-LATEST-TELEMETRY.bat
```

## Safety scope

Autonomous input is restricted to single-player, LAN/private hosts explicitly configured by the user, and owned test arenas. Public-server automation, anti-cheat bypasses, authentication bypasses, packet advantage, altered reach, impossible placement, teleportation, and silent server-only controls are prohibited.
