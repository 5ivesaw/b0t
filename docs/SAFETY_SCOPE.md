# Safety and Scope

## Allowed environments

- Single-player training worlds
- LAN sessions
- Private servers owned or controlled by the user
- Purpose-built Bedwars research arenas

## Prohibited capabilities

The project must not implement anti-cheat bypasses, disablers, packet spoofing/cancellation for advantage, reach modification, velocity cancellation, blink, backtrack exploits, impossible placements, silent server-only rotations, fake movement, authentication bypass, or public-server automation.

Packet events may only be observed and timestamped for synchronization, ping estimation, transaction confirmation, and simulator validation.

## Perception boundary

Exact loaded-entity state, including occluded entities, is allowed in controlled research. Every entity independently reports `lineOfSight`, `occluded`, `attackable`, `loaded`, and `trackingConfidence`. Knowing an occluded entity never authorizes attacking through terrain.

## Control boundary

The actuator applies legitimate client controls and rejects malformed or stale commands. Human takeover and emergency release have priority. Disabling SawBot, leaving a world, losing the model connection, or shutting down releases every key.

## Data boundary

Telemetry contains structured numerical/categorical state, action, event, outcome, timing, and identifiers. Screenshots, video frames, OCR products, and armour-colour pixel analysis are forbidden training inputs.
