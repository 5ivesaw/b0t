# Phase 12 Report — Human Motion and Explicit-Target PvP Motor

Version: `1.3.0-alpha.0`

## Goal

Add the first deterministic combat motor without allowing the body layer to choose an
opponent or tactic. The learned brain or explicit private test harness supplies a stable
tracking ID. The body performs only local, legal, visible execution and releases control
completely whenever ownership ends.

## Implemented

### Reference review

- Pinned mineflayer-pvp for explicit target ownership, local range separation, attack
  cadence, camera-before-attack, and target-loss release.
- Reused the existing pinned AltoClef concepts for progress/ownership windows and the
  Baritone/navigation concepts for delegating distant pursuit to a separate specialist.
- Preserved the MIT/LGPL clean-room boundary; no donor implementation text was copied.

### Explicit target boundary

- Combat starts only from a `Skill.PVP` action with a positive target tracking ID or the
  manual Y-key harness applied to the inspector-selected entity.
- The body never scans for, ranks, or automatically selects the nearest player.
- Non-player, dead, self, teammate, missing, stale, and out-of-radius targets are rejected
  or returned to the brain for navigation rather than silently substituted.
- A brain skill switch immediately removes brain combat intent.

### Human-motion motor

- Added a visible yaw/pitch servo with maximum velocity, acceleration continuity, pitch
  clamping, and no silent server-only rotation.
- Added predicted body-height aim from current entity position and motion.
- Added deterministic approach, spacing, retreat, and alternating strafe windows.
- Every movement direction is checked against live standability before its key is held.
- Large aim error pauses translation until the visible camera has substantially aligned.

### Legal attack execution

- Attacks require current observation LOS and Minecraft visibility, the configured normal
  local attack range, yaw/pitch alignment, a valid attackable target, hurt-time recovery,
  and the configured cooldown.
- Execution uses the ordinary client player controller and visible hand swing.
- No altered reach, packets, impossible aim, hidden target substitution, velocity
  cancellation, or anti-cheat behavior is present.

### Ownership and diagnostics

- Combat has priority over bridge/navigation only while an explicit combat intent exists.
- F9, F12, physical input, disable, freeze, GUI/world loss, target loss, model disconnect,
  skill switch, and exceptions restore every physical movement binding.
- The compact HUD exposes source, selected tracking ID, mode, distance, rotation error,
  attack count, edge guards, lost/rejected targets, switches, and the current reason.
- Y toggles the selected-target private test intent; Shift+Y clears it.

### Verification

- Added `CombatBodyContractTest` with 47 checks covering spacing, retreat, approach,
  cooldown, occlusion, edge guarding, bounded rotation, explicit target ownership,
  legal attack execution, brain intent switching, teammate rejection, target loss, world-unload intent clearing, and complete input release.
- Stabilized the existing asynchronous support-break navigation test by allowing its
  planner worker one deterministic scheduling window per synthetic tick. The original
  invalidation, replan, and arrival assertions remain unchanged.
- Full Java 8 lint-clean offline verification, telemetry/model fixtures, synthetic JAR,
  release packaging, hashes, and payload validation pass.

Runtime acceptance remains grouped with the later integrated body review, as requested.
