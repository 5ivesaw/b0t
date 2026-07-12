# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a
visible, inspectable hybrid agent. Minecraft is the body: the mod reads structured
internal state, never screen pixels, and applies only legitimate client controls in
local/private test environments.

## Current gate

Phases 0–2 passed target-machine runtime acceptance. Later runtime refinement is
intentionally grouped into a future integration pass while the body architecture is
built out. This repository contains **Phase 8 — Real-Time Bridging Specialist**
(`0.9.0-alpha.0`).

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
- Anytime bounded A*, live route validation, rolling current-position replanning,
  sustained movement, visible turning, jumping, and recovery
- Legal short-gap bridging with block selection, visible alignment, normal reach and
  ray-trace validation, deliberate placement, world confirmation, cautious advance,
  and full release
- Explicit F9/F12/physical-input ownership priority
- Automatic GitHub build, tag, and release

## Phase 8 body controls

1. Aim at a reachable block and press `G` to set the destination.
2. Press `F10` to enable the body.
3. Navigation follows live terrain toward the destination.
4. If navigation reaches a bounded unsupported gap and reports `NO_PATH`/`BLOCKED`,
   the bridging specialist may take ownership, place confirmed support, cross, and
   return control to navigation.
5. `R` explicitly arms/disarms bridging for private/local mechanical testing.
6. `Shift+R` clears manual bridge intent.
7. `F9`, physical input, or `F12` releases every owned input immediately.

The brain chooses whether and where to bridge. The body never chooses the Bedwars
objective or strategic destination.

## Build

A push to `main` verifies, builds, remaps, tags `v0.9.0-alpha.0`, and publishes the
exact tested release artifact.

```powershell
git add -A
git commit -m "Implement Phase 8 real-time bridging specialist"
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
| F7 | Compact inspector |
| H | Next inspector page |
| P / . | Freeze/unfreeze / single observation step |
| B / C / N / V / M | Terrain / collision / entities / tracers / landmarks |
| [ / ] | Previous/next tracked entity |
| O | Export snapshot JSON |
| K | Start/stop structured telemetry |

## Reports

- `docs/PHASE8_REPORT.md`
- `docs/BRIDGING_BODY.md`
- `docs/PHASE7_REPORT.md`
- `docs/ADAPTIVE_NAVIGATION.md`
- `docs/HYBRID_ARCHITECTURE.md`
- `docs/NAVIGATION_BODY.md`
- `docs/PHASE_GATES.md`
- `docs/PROJECT_BRIEF.txt`

## Safety scope

Autonomous input is restricted to single-player and explicitly configured
owned/private environments. Public-server automation, anti-cheat bypasses,
authentication bypasses, packet advantage, altered reach, impossible placement,
teleportation, and silent server-only controls are prohibited.
