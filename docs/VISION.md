# Vision

SawBotV1 is a reusable hybrid-agent platform whose behaviour is observable and attributable. The visible Minecraft 1.8.9 client is the initial body. Sensors produce bounded structured state from internal game objects; learned systems choose objectives, tactics, priorities, targets, risk, and specialist skills; deterministic specialist bodies execute known mechanics through legitimate client controls; telemetry connects intent, execution, and outcome.

The platform must always answer: what the agent knew, what it remembered, what it intended, which specialist owned execution, which controls were applied, how old the state was, whether the action succeeded, and whether the intent came from a human, teacher, or neural checkpoint.

## Hierarchical operating model

```text
learned strategic brain
        ↓ bounded intent
specialist task controller
        ↓ mechanical request
navigation / movement / camera / combat / bridging / inventory bodies
        ↓ legitimate input
Minecraft client
```

A specialist may solve geometry, timing, collision, route following, and input ownership. It may not silently replace the selected objective, invent Bedwars strategy, or choose a different tactical target. This preserves neural decision-making without forcing a model to relearn deterministic mechanics such as holding a key, checking support, or following a collision-safe corridor.

## Near-term MVP

The aggressive target is one known private 1v1 arena with a complete but imperfect pipeline: Forge mod, inspected sensors, structured telemetry, model bridge, adaptive navigation body, bridging body, combat body, inventory/shop body, learned tactical objective switching, and measured full-match attempts. Scope is reduced before validation is skipped.

## Explicit non-goals for the MVP

- Public-server operation
- Anti-cheat bypass
- Human-level universal Bedwars
- Multiple maps or four-team play
- Image-based perception
- Cloud or CUDA requirement
- Hidden strategic correction inside deterministic bodies
- Teleportation, altered reach, impossible placement, silent rotation, or packet advantage
