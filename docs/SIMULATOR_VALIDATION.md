# Simulator Validation v0.1

The simulator will implement only mechanics needed by active curricula: movement, collision, jumping, sneaking, falling/void, placement, reach, damage, knockback, and latency variation.

No mechanic is trusted because it looks plausible. For identical initial state and action sequence, real Minecraft telemetry and simulator traces are compared per tick for position, velocity, grounded state, jump apex, landing tick, collisions, placement, reach, hit timing, damage, and knockback. Tolerances are declared per mechanic. Every mismatch is saved as a regression fixture. Simulator-generated training is blocked for any mechanic whose differential suite fails.
