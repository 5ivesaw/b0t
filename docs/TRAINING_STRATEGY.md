# Training Strategy v0.3

SawBotV1 uses a hierarchical hybrid curriculum.

## Learned brain

Training focuses on decisions whose correct answer depends on context rather than fixed mechanics:

- objective and target selection
- attack, defend, collect, buy, bridge, retreat, and regroup decisions
- risk and threat evaluation
- combat intent, spacing, and aim target
- choosing a specialist controller and its parameters

## Deterministic bodies

Known mechanics remain inspectable and testable code:

- local and long-range pathfinding
- movement/camera execution
- collision, void, and hazard safety
- jump and recovery timing
- bridging geometry and legal placement
- inventory and shop interaction
- deterministic action ownership and release

The brain may tune parameters, but bodies enforce geometry, timing, safety, and legitimate controls.

## Phase 5 disposition

The Phase 5 18→32→7 waypoint classifier remains a preserved research baseline. Its held-out synthetic evaluation demonstrated the bridge and training pipeline, while target-machine behavior demonstrated that reactive low-level classification is not an adequate navigation architecture.

## Next curricula

1. Neural tactical selector that emits navigation goals.
2. Deterministic bridging body plus learned bridge-style/risk selection.
3. Combat body with learned target, movement intent, aim point, and aggression.
4. Inventory/shop body with learned purchase strategy.
5. Bedwars objective brain and private-arena evaluation.
