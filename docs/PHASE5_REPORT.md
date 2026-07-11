# Phase 5 Report — First Learned Behaviour

Candidate version: `0.6.0-alpha.0`  
Learned model: `SawBotV1-Waypoint-0.1`  
Runtime bridge: `sawbot.bridge/0.1`

## Gate objective

Phase 5 adds the first exported learned policy: navigate toward a user-selected semantic waypoint while avoiding unsupported or hazardous forward cells. The runtime model receives only the existing structured Observation Contract and emits the existing Action Contract. There is no screenshot input, runtime teacher, pathfinder, aim helper, scaffold logic, or hidden corrective controller.

## User waypoint

`G` sets waypoint `#1000` one block above the block under the crosshair. `Shift+G` clears it. The waypoint is published as a normal bounded landmark with type `STAGING_AREA`, confidence `1.0`, and egocentric right/up/forward coordinates. It therefore passes through the same observation, telemetry, bridge, and action-reference validation paths as future task landmarks.

## Dataset and teacher

The checked-in compressed dataset uses format `sawbot.waypoint.teacher/0.1`:

- 28,000 rows
- 18 ordered input features
- 7 discrete motor actions
- 4,000 rows per action
- deterministic generation seed
- explicit support, void, obstacle, hazard, target, collision, and motion features

The teacher is used only to create labels. It is not imported by the live model process and is not present in the Forge runtime JAR.

## Model

The exported checkpoint uses format `sawbot.waypoint.mlp/0.1`:

- 18 inputs
- one 32-unit `tanh` hidden layer
- 7 action logits
- stored normalization statistics
- pure-standard-library live inference
- bounded action duration, camera delta, movement axes, and confidence

The seven learned outputs are `STOP`, `TURN_LEFT`, `TURN_RIGHT`, `FORWARD`, `FORWARD_LEFT`, `FORWARD_RIGHT`, and `JUMP_FORWARD`.

## Evaluation

The checked-in held-out evaluation contains 800 unseen starting states:

- learned-policy success: **698/800 = 87.25%**
- random-policy success: **29/800 = 3.625%**
- learned-policy timeout failures: 102
- runtime pathfinder: `false`
- measured pure-Python inference: approximately 33.7 microseconds per decision in the build environment

Failure examples are retained in `evaluation/waypoint_failures_v0.1.jsonl` rather than being hidden.

## Runtime architecture

The live model is a separate local process. It decodes immutable observations, searches only the bounded landmark list for waypoint `#1000`, derives the fixed 18-feature vector, runs the MLP, and sends an ordinary `ActionCommand`. The mod still owns all environment, deadline, stale-sequence, reference, human-takeover, disconnect, and emergency-release checks.

The actuator/input-ownership correction bundled in this candidate prevents a disabled or waiting actuator from repeatedly clearing human movement keys. Continuous synthetic keys are released only when the actuator actually owns them.

## Reproducibility

Training can be rebuilt with `RUN-REBUILD-WAYPOINT-MODEL.bat`. CI does not retrain; it verifies the committed dataset, checkpoint, evaluation, bridge payload, and inference contract using `verify_phase5.py`. This prevents nondeterministic release builds while keeping the training path reproducible.

## Remaining boundary

This is a waypoint-navigation milestone, not Bedwars competence. It does not select strategic objectives, identify beds/generators, bridge gaps, fight opponents, purchase items, or plan routes. Those remain later task adapters and learned skill phases.
