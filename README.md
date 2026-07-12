# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the body: the mod reads structured internal state, never screen pixels, and applies only legitimate client controls in local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Phase 3 telemetry, Phase 4 bridge/actuator infrastructure, and the Phase 5 learned-action experiment remain preserved. This repository contains **Phase 6 — Hybrid Navigation Body** (`0.7.0-alpha.0`).

The runtime now separates intelligence from mechanics:

- learned brain: goals, priorities, targets, tactics, risk, and specialist selection
- deterministic body: pathfinding, movement holding, camera control, jumping, safety, stuck recovery, and input ownership

The first deterministic body performs bounded incremental waypoint navigation. `G` sets waypoint `#1000`; F10 starts navigation without requiring the external model. A connected brain may request the same skill through the existing Action Contract.

Implemented contracts and systems:

- Observation `sawbot.observation/0.3`
- Action `sawbot.action/0.1`
- Telemetry `sawbot.telemetry/0.1`
- Bridge `sawbot.bridge/0.1`
- Bounded sensors, inspector, overlays, freeze/step, and export
- Structured telemetry validation, replay, CRC, and recovery
- Non-blocking model bridge and safe fallback actuator
- Incremental A*, sustained movement, visible turning, step-up, arrival, stuck recovery, and path rendering
- Physical-input restoration and explicit F9/F12 feedback
- Automatic GitHub build, tag, and release

## Phase 6 direct navigation

1. Aim at a reachable block and press `G`.
2. Press `F10`.
3. The deterministic body plans and follows the visible route.
4. Press `F9`, provide physical input, or press `F12` to return control immediately.
5. Use `Shift+G` to clear the goal.

The Phase 5 model remains a historical experiment, not the primary navigator.

## Build

A push to `main` verifies, builds, remaps, tags `v0.7.0-alpha.0`, and publishes the exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 6 hybrid navigation body"
git push origin main
```

Toolchain: Gradle 8.8, Architectury Loom `0.10.0.5`, Java 17 for Gradle, Java 8 bytecode, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`.

## Controls

| Key | Function |
|---|---|
| F10 | Enable/disable the selected body or connected brain |
| F9 | Immediate manual takeover |
| F12 | Emergency release |
| G | Set deterministic-navigation waypoint above aimed block |
| Shift+G | Clear navigation waypoint |
| F7 | Compact inspector |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE6_REPORT.md`
- `docs/HYBRID_ARCHITECTURE.md`
- `docs/NAVIGATION_BODY.md`
- `docs/PHASE5_REPORT.md`
- `docs/WAYPOINT_MODEL.md`
- `docs/PHASE4_RUNTIME_FINDINGS.md`
- `docs/MODEL_BRIDGE_PROTOCOL.md`
- `docs/PHASE_GATES.md`
- `docs/PROJECT_BRIEF.txt`

## Safety scope

Autonomous input is restricted to single-player and explicitly configured owned/private environments. Public-server automation, anti-cheat bypasses, authentication bypasses, packet advantage, altered reach, impossible placement, teleportation, and silent server-only controls are prohibited.
