# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a
visible, inspectable hybrid agent. Minecraft is the body: the mod reads structured
internal state, never screen pixels, and applies only legitimate client controls in
local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Later runtime refinement is
intentionally grouped into a future integration pass while the body architecture is
built out. This repository contains **Phase 10 — Continuous Anytime Navigation**
(`1.1.0-alpha.0`).

The runtime separates intelligence from mechanics:

- learned brain: goals, priorities, targets, tactics, risk, and specialist selection
- deterministic bodies: navigation, bridging, movement, camera, placement geometry,
  safety, confirmation, recovery, and input ownership

Implemented contracts and systems:

- Observation `sawbot.observation/0.3`
- Action `sawbot.action/0.1`
- Telemetry `sawbot.telemetry/0.1`
- Brain bridge `sawbot.bridge/0.1`
- Bounded sensors, inspector, overlays, freeze/step, and export
- Structured telemetry validation, replay, CRC, and recovery
- Non-blocking local model transport and safe fallback actuator
- Immutable client-thread navigation snapshots and one bounded anytime planner worker
- Direction-aware weighted A* expanded in small slices with safe best-so-far path streaming
- Rolling current-position replanning, current/replacement segments, safe splicing,
  rewind/skip, corridor projection, live invalidation, and timeouts
- Continuous movement ownership without per-cell W pulsing, fast visible camera control,
  legal one-block ascent, validated lookahead, sprint execution, and stuck recovery
- Legal short-gap bridging with block selection, visible alignment, normal reach and
  ray-trace validation, deliberate placement, world confirmation, cautious advance,
  and full release
- Explicit F9/F12/physical-input ownership priority
- Automatic GitHub build, tag, and release

## Phase 10 navigation controls

1. Aim at a reachable block and press `G` to set destination waypoint `#1000`.
2. Press `F10` to enable the deterministic navigation body.
3. The body begins with an immediate validated micro-route when possible, captures a
   small local snapshot, begins following a safe best-so-far route, and keeps improving
   wider alternatives in the background.
4. Knockback, manual displacement, server correction, terrain change, or reaching a
   later operation causes bounded route reconciliation, splicing, or replanning from
   the actual player position instead of returning to obsolete nodes.
5. When navigation reaches an unsupported bounded gap, the existing bridging body may
   take ownership, confirm support, cross, then return control to navigation.
6. `F9`, physical input, or `F12` releases every owned input immediately.

No model process is required for deterministic waypoint navigation. Existing model BAT
files remain available for bridge/protocol and learned-brain development.

## Build

A push to `main` verifies, builds, remaps, tags `v1.1.0-alpha.0`, and publishes the
exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 10 continuous anytime navigation"
git push origin main
```

Toolchain: Gradle 8.8, Architectury Loom `0.10.0.5`, Java 17 for Gradle,
Java 8 bytecode, Forge `1.8.9-11.15.1.2318-1.8.9`, MCP `stable_22`.

## Controls

| Key | Function |
|---|---|
| F10 | Enable/disable the selected body or connected brain |
| F9 | Immediate manual takeover |
| F12 | Emergency release |
| G | Set navigation/bridge destination above aimed block |
| Shift+G | Clear destination |
| R | Toggle manual bridging specialist intent |
| Shift+R | Clear manual bridging intent |
| F7 | Compact inspector and route rendering |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE10_REPORT.md`
- `docs/CONTINUOUS_ANYTIME_NAVIGATION.md`
- `docs/PHASE9_REPORT.md`
- `docs/SEGMENTED_NAVIGATION_CORE.md`
- `docs/BARITONE_ARCHITECTURE_RESEARCH.md`
- `docs/NAVIGATION_BODY.md`
- `docs/PHASE8_REPORT.md`
- `docs/BRIDGING_BODY.md`
- `docs/HYBRID_ARCHITECTURE.md`
- `docs/PHASE_GATES.md`
- `docs/PROJECT_BRIEF.txt`

## Safety scope

Autonomous input is restricted to single-player and explicitly configured
owned/private environments. Public-server automation, anti-cheat bypasses,
authentication bypasses, packet advantage, altered reach, impossible placement,
teleportation, and silent server-only controls are prohibited.
