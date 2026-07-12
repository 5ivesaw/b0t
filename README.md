# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the body: the mod reads structured internal state, never screen pixels, and applies only legitimate client controls in local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Phase 3 telemetry, Phase 4 bridge/actuator infrastructure, and the Phase 5 learned-action experiment remain preserved. This repository contains **Phase 7 — Real-Time Adaptive Navigation** (`0.8.0-alpha.0`).

The runtime separates intelligence from mechanics:

- learned brain: goals, priorities, targets, tactics, risk, and specialist selection
- deterministic body: pathfinding, real-time movement control, camera control, jumping, safety, stuck recovery, and input ownership

The navigation body no longer treats a route as a list of mandatory block centres. It continuously re-anchors to the player's actual position, follows a safe route corridor, skips unnecessary nodes, validates live geometry, probes several local headings each tick, and hot-swaps rolling current-position replans without routine stop/start movement.

Implemented contracts and systems:

- Observation `sawbot.observation/0.3`
- Action `sawbot.action/0.1`
- Telemetry `sawbot.telemetry/0.1`
- Bridge `sawbot.bridge/0.1`
- Bounded sensors, inspector, overlays, freeze/step, and export
- Structured telemetry validation, replay, CRC, and recovery
- Non-blocking model bridge and safe fallback actuator
- Anytime bounded A* and provisional frontier routes
- 20 Hz live corridor validation and local steering candidates
- Current-position rolling replanning and active-route hot swapping
- Sustained movement, responsive visible turning, jumping, sprinting, and recovery
- Physical-input restoration and explicit F9/F12 feedback
- Automatic GitHub build, tag, and release

## Phase 7 direct navigation

1. Aim at a reachable block and press `G`.
2. Press `F10`.
3. The navigation specialist plans and follows the visible adaptive corridor.
4. Press `F9`, provide physical input, or press `F12` to return control immediately.
5. Re-enable after moving manually: the next route starts from the new current position.
6. Use `Shift+G` to clear the goal.

The Phase 5 model remains a historical experiment. Future learned brains select high-level goals while deterministic specialists execute mechanics.

## Build

A push to `main` verifies, builds, remaps, tags `v0.8.0-alpha.0`, and publishes the exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 7 real-time adaptive navigation"
git push origin main
```

Toolchain: Gradle 8.8, Architectury Loom `0.10.0.5`, Java 17 for Gradle, Java 8 bytecode, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`.

## Controls

| Key | Function |
|---|---|
| F10 | Enable/disable the selected body or connected brain |
| F9 | Immediate manual takeover |
| F12 | Emergency release |
| G | Set navigation waypoint above aimed block |
| Shift+G | Clear navigation waypoint |
| F7 | Compact inspector |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE7_REPORT.md`
- `docs/ADAPTIVE_NAVIGATION.md`
- `docs/HYBRID_ARCHITECTURE.md`
- `docs/NAVIGATION_BODY.md`
- `docs/PHASE6_REPORT.md`
- `docs/PHASE5_REPORT.md`
- `docs/WAYPOINT_MODEL.md`
- `docs/PHASE4_RUNTIME_FINDINGS.md`
- `docs/MODEL_BRIDGE_PROTOCOL.md`
- `docs/PHASE_GATES.md`
- `docs/PROJECT_BRIEF.txt`

## Safety scope

Autonomous input is restricted to single-player and explicitly configured owned/private environments. Public-server automation, anti-cheat bypasses, authentication bypasses, packet advantage, altered reach, impossible placement, teleportation, and silent server-only controls are prohibited.
