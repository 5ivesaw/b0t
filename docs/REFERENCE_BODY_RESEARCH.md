# Reference Body Research

Version: `1.3.0-alpha.0`

SawBotV1 now requires a reference review before a deterministic body subsystem is
expanded. The purpose is not to paste an existing client into the project. The purpose
is to compare proven movement, pathing, placement, task, and recovery designs before
writing an independent Minecraft 1.8.9 implementation.

## License boundary

SawBotV1 remains Apache-2.0. This release does not copy source from GPL, LGPL, or
unlicensed projects.

- MIT/Apache sources may inform direct clean adaptations when their copyright and
  license notices are preserved.
- LGPL sources are treated as architecture references unless a separately linked,
  compliant library integration is deliberately designed.
- GPL and unlicensed sources are architecture-only references. Their implementation
  text is not copied.
- Anti-cheat bypass, packet spoofing, impossible placement, silent rotation, and public
  server automation are outside SawBotV1's scope even when a reference project contains
  them.

## Reviewed systems

### cabaletta/baritone

Pinned reference commit:
`054092e44eec61f6ef3818a2b4b7c56df90daf76`

Reviewed concepts:

- current and next path executors
- asynchronous planning and planning ahead
- safe cancellation points
- path splicing and displacement reconciliation
- valid-position sets per movement
- movement timeout and cost revalidation
- explicit input ownership and release
- bounded path/search rendering

License: LGPL-3.0. No Baritone Java source is copied into SawBotV1.

### PrismarineJS/mineflayer-pathfinder

Reviewed `lib/movements.js` blob:
`40412d6004c3554dd20eec8be464169a32c332af`

Reviewed concepts:

- cardinal and diagonal neighbor generation
- movement-specific costs
- entity intersection costs
- replaceable, physical, liquid, climbable, and hazard classification
- scaffolding inventory selection
- exclusion costs for walking, breaking, and placing
- explicit one-block ascent geometry

License: MIT. This release uses independently written Java mechanics and records the
reference for future compatible adaptations.

### gaucho-matrero/altoclef

Reviewed `MovementProgressChecker.java` blob:
`749b0499a57ff3fa2f32e30441a992c3cb8d089c`

Reviewed concepts:

- progress windows rather than one-tick failure decisions
- different progress signals for movement and block interaction
- retry budgets
- resetting progress when another legitimate task temporarily owns control
- high-level task arbitration above the low-level path body

License: MIT.

### PrismarineJS/mineflayer-pvp

Pinned reference commit:
`82e19b6ac66555f81d1e8e2f749233b4ae0bac4f`

Reviewed `src/PVP.ts` blob:
`539ba6a23d74cfe7776ddabfb2c1204a8bf2c126`

Reviewed concepts:

- target ownership is explicit rather than nearest-entity auto-selection
- follow distance and attack distance are separate concerns
- target disappearance stops combat ownership
- range transitions update attack timing rather than firing blindly
- camera alignment precedes attack execution
- combat delegates long-range pursuit to a pathing system

License: MIT. SawBotV1's implementation is independently written Java for Minecraft
1.8.9 and preserves the reference record; no TypeScript source was copied.

### JellyLabScripts/MightyMiner

Reviewed repository: `JellyLabScripts/MightyMiner`

Reviewed README blob:
`871c5de3becd7d0272e246f3e68f2893bcf4583c`

MightyMiner is relevant because it is a practical Minecraft 1.8.9 automation project
with route movement, rotation, mining, and recovery behavior. Its repository does not
provide a usable license grant in the checked `LICENSE` file, so it is reference-only.
No MightyMiner source is copied.

### Wurst-Imperium/Wurst7 ScaffoldWalk

Reviewed `ScaffoldWalkHack.java` blob:
`60abee145ba36b3dfde31590ab29350f11ff30d6`

Reviewed concepts:

- search multiple attachment supports rather than one exact face
- reject non-solid or falling placement blocks
- preserve and restore the selected hotbar slot
- test reach before placement
- separate target-cell search from support-face selection

License: GPL-3.0. Only the general mechanical ideas were reviewed. No Wurst source is copied.

## Phase 11 applications

This release applies the reference review to two immediate failures:

1. Bridge placement now evaluates every legal adjacent support, samples multiple hit
   points per face, filters by normal reach and ray trace, and chooses the lowest-cost
   visible candidate instead of trusting one exact face center.
2. Debug visualization now has explicit ownership and lifetime rules. Old bridge plans,
   old path frontiers, and completed routes cannot remain rendered after their body
   releases ownership.

## Phase 12 applications

This release applies the reference process to human motion and local combat. The direct
comparison set is mineflayer-pvp for target/range/cadence separation, AltoClef for
progress windows and task ownership, and Baritone/SawBot navigation for handing distant
pursuit back to a separate movement specialist.

1. Combat target ownership is explicit. The user or learned brain supplies one tracking
   ID; the motor never scans for a substitute or chooses tactics.
2. Long-range pursuit remains navigation's responsibility. The combat body operates only
   inside a bounded local radius and reports when the brain must navigate closer.
3. Camera alignment, attack range, LOS, hurt-time recovery, and cooldown are separate
   gates rather than one combined "attack nearby" switch.
4. Continuous key ownership, support probes, and full release are inherited from the
   established navigation/body architecture.
5. Human-motion execution remains visible and bounded; no silent rotation or altered
   reach is introduced.

## Required reference matrix for future bodies

Before implementation, each body must document at least two relevant references and its
clean-room/license decision.

| Body | Required comparison areas |
|---|---|
| Navigation | movement primitives, replanning, path splicing, entity costs |
| Bridging | support search, hit vectors, confirmation, slot restoration |
| Human motion | rotation profile, key continuity, acceleration, interruption |
| Combat | target selection, spacing, legal reach, knockback recovery |
| Inventory/shop | state machine, confirmation, retry, rollback |
| Block interaction | visibility, tool choice, progress, timeout |
| Visualization | ownership, expiry, bounds, render-state restoration |

The neural brain remains responsible for goals and tactics. Deterministic bodies remain
responsible for reliable legal execution.
