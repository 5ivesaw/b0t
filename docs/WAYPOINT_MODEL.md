# Learned Waypoint Model

## Controls

- `G`: set user waypoint `#1000` above the crosshair-selected block.
- `Shift+G`: clear the user waypoint.
- `F10`: enable the actuator after the learned model reaches `READY`.
- `F9`: immediate manual takeover.
- `F12`: emergency release.

## Start the model

Run `sawbot-trainer\waypoint\RUN-WAYPOINT-MODEL.bat`. The model listens on `127.0.0.1:25189`, uses checkpoint `checkpoints/waypoint_v0.1.json`, and prints its selected action, probability, and target distance periodically.

## Feature contract

The model consumes exactly 18 ordered values: normalized target right/forward/up/distance, three foot-support scores, void clearance, three front safety probes, two-block landing safety, feet/head obstacle flags, front hazard, on-ground, horizontal collision, and normalized forward velocity.

The live process does not inspect Minecraft objects and does not derive a path. It uses only the serialized observation supplied by the client bridge.

## Training files

- `generate_teacher_data.py`: deterministic balanced mechanics examples.
- `train_waypoint.py`: NumPy training and checkpoint export.
- `evaluate_waypoint.py`: held-out rollout and random-baseline comparison.
- `verify_phase5.py`: pure-standard-library release verification.
- `waypoint_model.py`: live model process.

## Safety

The checkpoint cannot bypass client safety. Every output is validated against age, observation sequence, selected waypoint existence, environment scope, GUI state, physical takeover, F9/F12, and disconnect status before any legitimate input is applied.
