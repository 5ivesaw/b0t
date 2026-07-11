# Training Strategy v0.2

Phase 2 inspection passed, so learned mechanics may now enter the runtime only through exported checkpoints and the versioned bridge/action contracts.

## Completed first curriculum

Phase 5 trains waypoint following without falling from a deterministic balanced teacher dataset. The teacher exists only in the offline data-generation script. The live process contains the fixed feature transform and exported MLP, but no teacher calls, runtime pathfinder, hidden steering correction, or Minecraft-world access.

The committed checkpoint is evaluated on held-out randomized starts and compared with a random policy. Failure examples are retained. CI verifies the committed data/checkpoint/evaluation rather than retraining nondeterministically.

## Next curricula

1. Short-gap bridging as a separate skill with explicit placement/contact outcomes.
2. Navigation-plus-bridging composition without a runtime planner.
3. Combat motor policy against varied private-arena opponents.
4. Tactical objective selection at a slower timescale.
5. Bedwars task adapter and population evaluation.

Human structured trajectories remain available for supervised and imitation-learning datasets. Later reinforcement learning must use validated mechanics simulation and differential tests before transfer to the real client.
