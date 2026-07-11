# Phase 4 Runtime Findings Bundled into Phase 5

The target-machine Phase 4 session proved that the bridge handshake and outgoing dummy-model actions worked, but exposed an input-ownership defect: the disabled/waiting actuator could repeatedly release key bindings that belonged to the human. That interrupted walking, jumping, and flying even while SawBot was disabled.

Phase 5 corrects this by tracking synthetic continuous-input ownership. Disabled/waiting ticks perform no key mutation unless an active synthetic command previously acquired those controls. Physical input monitoring is armed at enable time after stale mouse deltas are drained. F9 and F12 publish distinct compact takeover/emergency notices. The bridge HUD uses a stable `OFFLINE` label across reconnect attempts instead of alternating `CONNECTING` and `DISCONNECTED` every retry.

The telemetry writer also no longer interrupts an active NIO write during normal close. Its bounded queue poll wakes naturally, avoiding `ClosedByInterruptException` while preserving bounded shutdown.
