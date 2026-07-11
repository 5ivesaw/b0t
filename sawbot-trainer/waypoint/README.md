# Phase 5 learned waypoint navigation

This directory contains the first honest learned SawBot behaviour. The runtime policy is a tiny MLP trained from a separate synthetic geometry teacher. The exported model does not call the teacher, A*, a pathfinder, aim assist, or runtime scaffold logic.

## Runtime

1. Run `RUN-WAYPOINT-MODEL.bat`.
2. In a private/local world, aim at a walkable destination block and press `G`.
3. Press `F10` only after the HUD reports the model as READY.
4. `Shift+G` clears the waypoint.

The model receives the versioned internal observation payload, extracts a fixed 18-value feature vector, runs CPU-only inference, and emits legitimate movement/camera controls through `sawbot.action/0.1`.

## Training

`RUN-REBUILD-WAYPOINT-MODEL.bat` regenerates the balanced teacher dataset, trains the 18→32→7 MLP with NumPy, exports a standard JSON checkpoint, and runs held-out rollout evaluation. NumPy is a trainer-only dependency; `waypoint_model.py` uses only Python's standard library.

## Evidence

- `datasets/teacher_waypoint_v0.1.jsonl.gz`: bounded balanced teacher examples.
- `checkpoints/waypoint_v0.1.json`: exported weights and training metrics.
- `evaluation/waypoint_eval_v0.1.json`: held-out rollout and random-baseline metrics.
- `evaluation/waypoint_failures_v0.1.jsonl`: saved failure cases.

This milestone is local waypoint following, not general pathfinding or Bedwars competence.
