# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the agent's body. The mod reads structured internal game state, never screen pixels, and later actuates only the same legitimate client controls available to a player.

## Current gate

**Phase 0 and Phase 1 passed real runtime acceptance on the target machine.** This repository contains the **Phase 2 — Sensor Inspector automation candidate** (`0.3.0-alpha.6`). It retains the accepted Phase 2 sensors, compact text HUD, immediate LOS/OCC colour correction, and single-push GitHub releases. Alpha.6 adds specific entity types, clearer frozen-snapshot semantics, immediate unfreeze refresh, transient export notices, yellow block selection, and complete OpenGL state restoration.

Implemented foundation:

- Immutable Observation Contract `sawbot.observation/0.3`
- Self/body state, support probes, and void distance
- Bounded 13×9×13 egocentric terrain tensor
- Incremental 33×33 mid-range map
- Stable loaded-entity tracking with LOS, occlusion, attackability, and team relation
- Immediate green LOS / purple OCC box, label, and tracer styling from the current observation
- Separate yellow selection accent that never masks visibility state
- Fixed 41-slot inventory/resource representation
- Bounded landmarks, events, timing, validity, and performance fields
- Immutable freeze independent of enable/disable state
- Independently toggleable terrain, collision/support, entity/LOS, and landmark overlays
- Eight compact text-inspector pages
- Selected-block and stable tracked-entity inspection
- Current-versus-previous snapshot comparison
- Bounded asynchronous one-shot JSON export
- One-observation-step capture while frozen
- Conflict-free F10/F9/F12 safety controls

The repository still contains **no neural model, autonomous actuator loop, Bedwars policy, runtime pathfinder, aim assist, scaffold controller, packet advantage, screenshot/OCR pipeline, or public-server automation**.

The complete locked brief is preserved in [`docs/PROJECT_BRIEF.txt`](docs/PROJECT_BRIEF.txt). Runtime evidence and gate reports are in:

- [`docs/PHASE0_ACCEPTANCE.md`](docs/PHASE0_ACCEPTANCE.md)
- [`docs/PHASE1_ACCEPTANCE.md`](docs/PHASE1_ACCEPTANCE.md)
- [`docs/PHASE2_REPORT.md`](docs/PHASE2_REPORT.md)
- [`docs/PHASE2_RUNTIME_VALIDATION.md`](docs/PHASE2_RUNTIME_VALIDATION.md)
- [`docs/PHASE_GATES.md`](docs/PHASE_GATES.md)

## GitHub CI and automatic release

A push to `main` is now the complete release operation. The workflow resolves the version from `gradle.properties`, runs offline verification, builds the real remapped Forge 1.8.9 JAR, validates the transferred release payload, creates the tag, and publishes the GitHub Release automatically. Pull requests and other branches build without publishing.

```powershell
git add -A
git commit -m "Describe the SawBotV1 update"
git push origin main
```

For this repository state the automatic release is `v0.3.0-alpha.6`. Published versions are immutable; a duplicate version fails clearly instead of overwriting an existing release. `Manual Release Recovery` remains available only as a fallback.

The runtime inspector intentionally remains a compact engineering text overlay. Decorative dashboard work is deferred unless it improves testing without obscuring Minecraft.

## Toolchain

- Gradle 8.8
- Architectury Loom `0.10.0.5`
- Architectury Pack200 `0.1.3`
- Java 17 for Gradle
- Java 8 toolchain/bytecode for Minecraft
- Minecraft Forge `1.8.9-11.15.1.2318-1.8.9`
- MCP mappings `stable_22`

## Local build

Install JDK 17 and JDK 8, then run:

```powershell
.\gradlew.bat clean ciBuild
```

The final remapped mod is written to:

```text
sawbot-forge-1.8.9/build/libs/SawBotV1-0.3.0-alpha.6-mc1.8.9.jar
```

Launch a development client with:

```powershell
.\gradlew.bat runClient
```

## Offline verification

```bash
bash tools/offline-verify.sh
```

This verifies Java 8 source compatibility against narrow APIs, runs 539 assertions, emits and parses a complete debug snapshot, and validates repository/release structure. It does not replace the real GitHub Forge/Loom build or in-client acceptance test.

## Controls

All bindings are configurable under **Options → Controls → SawBotV1**.

| Key | Function |
|---|---|
| F10 | Enable/disable SawBot state |
| F9 | Immediate manual takeover |
| F12 | Emergency release all controlled inputs |
| F7 | Show/hide inspector panel |
| P | Freeze/unfreeze immutable observation |
| . | Capture one observation while frozen |
| H | Next inspector page |
| B | Toggle terrain-cell overlay |
| C | Toggle collision/support overlay |
| N | Toggle entity boxes and LOS/OCC labels |
| V | Toggle entity tracers independently |
| M | Toggle landmark overlay |
| [ / ] | Previous/next tracked entity |
| O | Export current snapshot as JSON |

Debug exports are written under:

```text
.minecraft/sawbotv1/exports/
```

Phase 2 remains non-autonomous: it must not move, aim, attack, place, shop, or play Bedwars.

## Safety scope

Autonomous testing is restricted to single-player worlds, LAN tests, owned private servers, and purpose-built local arenas. The project prohibits anti-cheat bypasses, public-server automation, authentication bypasses, packet manipulation for advantage, altered reach, impossible placement, and silent server-only controls.
