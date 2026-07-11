# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the body: the mod reads structured internal state, never screen pixels, and applies only legitimate client controls in local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Phase 3 structured telemetry and Phase 4 bridge/actuator infrastructure are implemented. This repository contains the **Phase 5 — First Learned Behaviour candidate** (`0.6.0-alpha.0`).

The first exported neural policy navigates toward a user-selected semantic waypoint without a runtime pathfinder or teacher. Its held-out evaluation succeeds in 698/800 scenarios (87.25%) versus 29/800 (3.625%) for the random baseline.

Implemented contracts and systems:

- Observation `sawbot.observation/0.3`
- Action `sawbot.action/0.1`
- Telemetry `sawbot.telemetry/0.1`
- Bridge `sawbot.bridge/0.1`
- Bounded internal sensors, compact inspector, overlays, freeze/step, and export
- Structured human trajectory recording, validation, replay, CRC, and recovery
- Non-blocking local model bridge and client-thread safe actuator
- Environment guard, action deadlines, physical takeover, F9, and F12
- Deterministic teacher dataset, tiny MLP, held-out evaluation, and live learned waypoint model
- Single-push automatic GitHub build/tag/release

There is still no Bedwars strategy, runtime pathfinder, aim helper, scaffold controller, packet advantage, screenshot/OCR pipeline, or public-server automation.

## Phase 5 quick start

1. Aim at a reachable block and press `G` to set user waypoint `#1000`.
2. Run `sawbot-trainer\waypoint\RUN-WAYPOINT-MODEL.bat`.
3. Wait for model state `READY`.
4. Press `F10` to enable.
5. Press `F9` or any physical movement/mouse input for takeover; `F12` is emergency release.
6. Use `Shift+G` to clear the waypoint.

## Build

A push to `main` verifies, builds, remaps, tags `v0.6.0-alpha.0`, and publishes the exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 5 learned waypoint navigation"
git push origin main
```

Toolchain: Gradle 8.8, Architectury Loom `0.10.0.5`, Java 17 for Gradle, Java 8 bytecode, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`.

## Controls

| Key | Function |
|---|---|
| F10 | Enable/disable safe actuator when model is ready |
| F9 | Immediate manual takeover |
| F12 | Emergency release |
| G | Set learned-navigation waypoint above aimed block |
| Shift+G | Clear learned-navigation waypoint |
| F7 | Compact inspector |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE5_REPORT.md`
- `docs/WAYPOINT_MODEL.md`
- `docs/PHASE4_RUNTIME_FINDINGS.md`
- `docs/MODEL_BRIDGE_PROTOCOL.md`
- `docs/PHASE_GATES.md`
- `docs/PROJECT_BRIEF.txt`

## Safety scope

Autonomous input is restricted to single-player and explicitly configured owned/private environments. Public-server automation, anti-cheat bypasses, authentication bypasses, packet advantage, altered reach, impossible placement, teleportation, and silent server-only controls are prohibited.
