# Human Motion Profile v0.1

Version: `1.3.0-alpha.0`

## Purpose

SawBot's body should be mechanically capable without using impossible motion. This profile
defines bounded visible camera and continuous-key behavior shared by the Phase 12 combat
motor and future specialist bodies.

## Rotation

- yaw and pitch update the visible client player state
- no silent packet-only rotations
- configured per-tick maximum yaw and pitch changes
- separate acceleration limits prevent instant reversals
- dead zones converge cleanly without persistent oscillation
- pitch is clamped to the normal visible range
- large aim error can block translation and attack until alignment improves

The deterministic servo is intentionally inspectable. It is not an attempt to spoof an
anti-cheat system or manufacture fake human input.

## Translation

- continuous key ownership replaces one-tick movement pulses
- forward/back and left/right are mutually exclusive per axis
- sprint is allowed only for an appropriate supported approach
- local direction probes reject unsupported projected cells
- a blocked component is cancelled or redirected; it is never carried over as a stuck key
- F9, F12, physical takeover, lifecycle release, or controller handoff restores hardware
  state immediately

## Combat cadence

- strafe direction changes in deterministic bounded windows, not every tick
- approach, spacing, and retreat use separate movement profiles
- attack cadence respects cooldown and target hurt time
- the camera remains visible throughout aim and attack

## Performance budget

The profile performs constant-size arithmetic and four nearby support probes per active
combat tick. It starts no thread, scans no world volume, and adds no unbounded history or
render collection. Normal HUD output remains compact for the 1366x768 low-end target.

## Future use

Later learned brains may choose aggression, target, desired spacing, risk, and tactical
style. The deterministic body will continue to enforce legal reach, support, visible
rotation, key ownership, interruption, and release.
