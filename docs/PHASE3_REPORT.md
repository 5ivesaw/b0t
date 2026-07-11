# Phase 3 Report — Structured Telemetry

Candidate version: `0.4.0-alpha.0`

Telemetry schema: `sawbot.telemetry/0.1`
Observation schema: `sawbot.observation/0.3`
Action schema: `sawbot.action/0.1`

## Completed

- Bounded asynchronous trajectory writer.
- Little-endian binary format with explicit file and payload versions.
- Independent per-record DEFLATE compression.
- Per-record CRC32 and clean-footer rolling CRC32.
- `.partial` interrupted-write behavior and validator recovery.
- Exact client-tick human key-state capture.
- Raw Minecraft `MouseHelper.deltaX/deltaY` capture on client-tick END.
- Selected hotbar slot and GUI-open capture.
- Causal observation/action/outcome alignment.
- Bounded recent outcome-event association.
- Telemetry status, queue size, step count, and drop count in the compact HUD.
- Dataset validator and replay inspector command-line tools.
- One-click Windows latest-trajectory acceptance test with safe temporary corruption/recovery checks.
- K default key for start/stop telemetry; fully rebindable.
- Runtime shutdown handling and bounded queue overflow behavior.
- Phase 2 small fixes bundled into this substantive release:
  - selected-block outline renders only while F7 inspector is open;
  - explanatory prose removed from the in-game HUD;
  - OpenGL colour cache reset after labels to prevent held-item/hotbar tinting.

## Runtime contract

Press K to start. A session begins only after a valid observation exists. The first observation becomes the baseline. Each newer observation finalizes the previous observation's input window and sends an immutable step to the writer queue.

Press K again to stop. A bounded final incomplete step is written when pending human input exists. The writer drains its queue, appends a footer, flushes, and renames `.sbt.partial` to `.sbt` asynchronously.

Manual takeover and emergency release do not stop human telemetry. Disconnect/world unload stops and finalizes the session because the world contract changed.

## Output location

```text
.minecraft/sawbotv1/telemetry/
```

## Performance design

- Minecraft world objects are never read by the writer thread.
- The client thread gives the worker immutable snapshots and immutable input windows only.
- Disk writes are buffered at 64 KiB.
- Queue offer is non-blocking.
- Compression runs on the telemetry worker.
- Default queue memory is bounded to 64 pending steps.
- Default compression level is 1.

## Offline evidence

The offline suite verifies:

- telemetry schema identity;
- input-window immutability and bounds;
- exact key-bit and raw mouse-delta persistence;
- deterministic binary encoding;
- complete `.sbt` creation;
- record CRC validation;
- footer rolling CRC validation;
- deliberate footer truncation detection;
- valid-prefix recovery into a separate file;
- replay-inspector parsing;
- Phase 0–2 contracts and safety behavior.

## Known limitations

- The Phase 3 writer captures human actions only. Model and teacher action sources are reserved for later phases.
- No reward value or task-specific terminal reason is generated yet beyond session termination metadata.
- The binary validator currently validates framing, summaries, checksums, bounds, and recovery; a future migration tool will decode every observation field into training tensors.
- Mouse deltas are Minecraft's raw per-tick `MouseHelper` deltas, not operating-system screen recordings or rendered motion.
- No neural training begins in this phase.

## Runtime acceptance checklist

- [ ] K starts telemetry and HUD shows `recording`.
- [ ] Queue remains bounded and usually near zero.
- [ ] The one-click report shows nonzero key and mouse activity, multiple tested hotbar slots, and GUI-open samples.
- [ ] K stops telemetry and status progresses through `finalizing` to `saved`/idle.
- [ ] A `.sbt` file appears and no `.partial` remains after a clean stop.
- [ ] `tools/TEST-LATEST-TELEMETRY.bat` reports the latest file `COMPLETE` with CRC checks passing.
- [ ] Replay inspector prints observation, tick, input, mouse, key-bit, and event summaries.
- [ ] A deliberately interrupted copy is reported incomplete and recoverable.
- [ ] Telemetry does not cause severe stutter.
- [ ] Queue overflow, if forced, is visible rather than blocking Minecraft.
- [ ] No screenshots, images, or video files are produced.
- [ ] N no longer tints the hotbar/held item.
- [ ] N alone does not show the selected-block outline; F7 does.
- [ ] Inspector text contains compact data, not tutorial paragraphs.
