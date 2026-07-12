# Baritone Architecture Research Notes

Reference repository: `cabaletta/baritone`

Reference revision inspected during Phase 9 research:
`054092e44eec61f6ef3818a2b4b7c56df90daf76`

## Use in SawBotV1

Phase 9 is a clean-room implementation of general pathing-system architecture. No
Baritone Java source file was copied into SawBotV1. The reference implementation targets
newer Minecraft mappings and APIs, whereas SawBotV1 targets Forge 1.8.9/MCP.

The architectural lessons adopted are:

- paths are sequences of movement operations, not only block coordinates
- movement operations expose valid positions and completion/failure state
- current and future path plans are separate
- planning can run ahead of execution
- future plans must be discarded or spliced when position changes
- execution reconciles against actual player position
- route geometry and costs are revalidated as the world changes
- movements have timeouts and safe cancellation boundaries
- input ownership is explicit and centrally released

SawBot-specific extensions include immutable client-thread snapshots, a single bounded
latest-wins worker, low-end hardware budgets, private-environment guards, neural
brain/deterministic body separation, compact engineering diagnostics, and specialist
handoff to legal bridging.

## Licensing

Baritone is distributed under LGPL-3.0. Because SawBotV1 does not include copied or
linked Baritone implementation code in Phase 9, Baritone is not bundled as a library or
derivative source component. This document preserves provenance for the design research.
Any future direct source reuse must add the applicable license, notices, corresponding
source, and relinking/modification obligations before release.
