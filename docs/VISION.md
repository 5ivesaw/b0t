# Vision

SawBotV1 is a reusable neural-agent platform whose behaviour is observable and attributable. The visible Minecraft 1.8.9 client is the initial body. Sensors produce bounded structured state from internal game objects; a learned policy selects objectives, skills, targets, and motor actions; a safe actuator applies legitimate client controls; telemetry connects decisions to outcomes.

The platform must always answer: what the agent knew, what it remembered, what it intended, which controls it emitted, how old the state was, whether the action succeeded, and whether behaviour came from a human, teacher, or neural checkpoint.

## Seven-day MVP

The aggressive target is one known private 1v1 arena with a complete but imperfect pipeline: Forge mod, inspected sensors, structured telemetry, model bridge, learned waypoint navigation, basic distilled bridging, basic neural combat, tactical objective switching, and measured full-match attempts. Scope is reduced before validation is skipped.

## Explicit non-goals for the MVP

- Public-server operation
- Anti-cheat bypass
- Human-level universal Bedwars
- Multiple maps or four-team play
- Image-based perception
- Cloud or CUDA requirement
- Final-runtime pathfinding, aim assist, scaffold, or scripted tactical play
