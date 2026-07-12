# Hybrid Brain-and-Body Architecture

SawBotV1 now uses a hierarchical hybrid design.

## Brain boundary

Learned systems choose goals, priorities, targets, tactics, risk, and which specialist skill should run. A brain output may request, for example, navigation to waypoint `#1000` with `Skill.NAVIGATION`.

The brain does not need to rediscover deterministic mechanics such as holding W, releasing owned inputs, graph search, collision checks, legal block geometry, or stable camera interpolation.

## Body boundary

Deterministic specialist controllers execute the selected intent:

- local route planning
- route following and sustained movement
- visible camera control
- jump and step-up timing
- collision, hazard, liquid, and unsupported-cell rejection
- stuck detection and replanning
- input ownership and physical-input restoration
- future bridging, inventory, shop, and interaction mechanics

The body never selects Bedwars strategy by itself. It executes a bounded goal supplied by the user, test harness, or learned brain.

## Runtime priority

1. F12 emergency release
2. F9 or physical human takeover
3. environment and GUI guards
4. deterministic specialist controller for an active skill
5. fallback low-level Action Contract actuator
6. idle/manual control

Only the controller that owns a binding may release it. Release restores the real hardware state instead of forcing the human key state to false.

## Navigation body v0.1

The first specialist body is deterministic waypoint navigation:

- bounded incremental A*
- maximum 64 expansions per client tick by default
- hard maximum 4,096 expanded nodes per plan
- 32-block horizontal and 8-block vertical default search bounds
- full support, two-block headroom, liquid, and hazard checks
- one-block step-up and step-down transitions
- diagonal corner-cut prevention
- sustained movement rather than action pulses
- maximum 18 degrees of visible yaw change per tick
- stable arrival hysteresis
- stall detection, alternating recovery strafe, jump, and replanning
- visible route, current node, planner counters, world reads, and recovery counters

All world access remains on the Minecraft client thread. Planning is incremental and explicitly budgeted.
