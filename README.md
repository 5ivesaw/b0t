# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a
visible, inspectable hybrid agent. Minecraft is the body: the mod reads structured
internal state, never screen pixels, and applies only legitimate client controls in
local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Later runtime refinement is
intentionally grouped into a future integration pass while the body architecture is
built out. This repository contains **Phase 12 — Human Motion and Explicit-Target PvP Motor**
(`1.3.0-alpha.0`).

The runtime separates intelligence from mechanics:

- learned brain: goals, priorities, targets, tactics, risk, and specialist selection
- deterministic bodies: navigation, bridging, human motion, local combat mechanics,
  camera, placement geometry, safety, confirmation, recovery, and input ownership

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
- Explicit-target local PvP motor with visible bounded rotation, spacing, strafe,
  edge guarding, legal attack timing, teammate rejection, and full release
- Explicit F9/F12/physical-input ownership priority
- Automatic GitHub build, tag, and release

## Phase 12 body controls

1. Use `[` / `]` to select a tracked player in the inspector.
2. Press `Y` to arm the selected-target combat test intent, then `F10` to enable it.
3. The combat body turns visibly with bounded yaw/pitch acceleration, maintains local
   spacing, strafes only over supported cells, and attacks only an explicit valid target
   inside normal reach and line of sight.
4. Press `Shift+Y` to clear combat intent. The learned brain can instead provide the
   same explicit PVP skill and target tracking ID.
5. Navigation (`G`) and bridging (`R`) remain available and yield to combat only while
   an explicit combat intent is active.
6. `F9`, physical input, or `F12` releases every owned input immediately.

No model process is required for deterministic waypoint navigation. Existing model BAT
files remain available for bridge/protocol and learned-brain development.

## Build

A push to `main` verifies, builds, remaps, tags `v1.3.0-alpha.0`, and publishes the
exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 12 human motion and PvP motor body"
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
| Y | Toggle combat intent for the selected tracked entity |
| Shift+Y | Clear manual combat intent |
| F7 | Compact inspector and route rendering |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE12_REPORT.md`
- `docs/COMBAT_BODY.md`
- `docs/HUMAN_MOTION_PROFILE.md`
- `docs/PHASE11_REPORT.md`
- `docs/REFERENCE_BODY_RESEARCH.md`
- `docs/VISUALIZATION_LIFECYCLE.md`
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
