# Training Strategy v0.1

Training remains blocked until the Phase 2 sensor-inspector gate passes.

The planned sequence is teacher-generated mechanics examples plus structured human trajectories, supervised skill models, focused scenario randomization and snapshot branching, validated mechanics simulation, then reinforcement-learning fine-tuning and opponent-population evaluation. Teachers are separate build/runtime dependencies and cannot appear in exported inference artifacts.

Initial learned milestone is waypoint following without falling. Bridging and combat follow separate curricula. Tactical decisions run at a slower timescale than motor actions. Evaluation uses fixed held-out states/opponents and compares random policy, teacher, current checkpoint, and previous checkpoint.
