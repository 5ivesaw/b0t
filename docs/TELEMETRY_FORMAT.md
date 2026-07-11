# SawBotV1 Structured Telemetry Format v0.1

Identifier: `sawbot.telemetry/0.1`

Status: Phase 3 runtime candidate

## Scope

This format stores structured Minecraft state and control telemetry only. It never stores screenshots, rendered frames, video, OCR output, textures, or pixel-derived features.

Each trajectory step causally associates:

- one immutable observation snapshot;
- exact client-tick human key states captured after that observation;
- raw `MouseHelper.deltaX` and `MouseHelper.deltaY` values;
- selected hotbar slot;
- whether a GUI was open;
- the next observation sequence/tick boundary;
- bounded outcome events that occurred after the source observation;
- action source (`HUMAN`, later `MODEL` or `TEACHER`);
- incomplete-outcome status for the final partial window.

## File extension

- Cleanly closed session: `.sbt`
- Interrupted or currently open session: `.sbt.partial`
- Validator-recovered session: `.recovered.sbt`

## Endianness and framing

All numeric values are little-endian.

The file begins with:

- 8-byte magic: `SBTEL001`
- unsigned 32-bit file version: `1`
- unsigned 32-bit endian marker: `0x01020304`
- unsigned 32-bit flags
- signed 64-bit creation epoch milliseconds
- 128-bit episode UUID
- bounded UTF-8 telemetry schema identifier
- bounded UTF-8 observation schema identifier
- bounded UTF-8 world identifier
- bounded UTF-8 task-adapter identifier

Each record contains:

- 32-bit record magic: `SBR1`
- record type (`STEP` or `FOOTER`)
- flags
- record ordinal
- source observation sequence
- uncompressed payload length
- compressed payload length
- CRC32 of the uncompressed payload
- DEFLATE-compressed payload

## Compression

Every payload is independently compressed with DEFLATE. The default runtime level is `1` to minimize CPU load on the i5-6200U. Independent records allow bounded memory usage and recovery after an interrupted write.

## Checksums

Two checksum layers are used:

1. Every record stores a CRC32 of its uncompressed payload.
2. The clean footer stores a rolling CRC32 across all uncompressed step payloads in order.

A clean file is valid only when every record CRC, step count, and footer rolling CRC agree.

## Recoverability

The writer first creates `.sbt.partial`. It appends complete independently framed records and writes a footer only during a clean close. After flushing the footer, it atomically renames the file to `.sbt` when supported.

`validate_telemetry.py --recover` scans until the first incomplete or corrupt record, retains only the valid prefix, and writes a new footer to a separate `.recovered.sbt` file. It never modifies the source file.

## Boundedness

- Writer queue: configurable, default 64 trajectory steps, hard range 8–256.
- Human input samples per observation: configurable, default 32, hard maximum 64.
- Entities: maximum 32 per observation.
- Landmarks: maximum 64 per observation.
- Events: maximum 64 per observation and outcome window.
- Terrain tensor: exactly 1,521 cells.
- Mid-range map: exactly 1,089 columns.
- Inventory: exactly 41 slots.

When an input window exceeds its bound, the oldest samples are dropped and the exact dropped count is persisted. When the writer queue is full, new steps are rejected and counted; the Minecraft client thread never blocks on disk I/O.

## Causal alignment

For observation `O_n`, the associated input window contains client-tick samples captured after `O_n` and before `O_(n+1)`. The step records `O_(n+1)` as its outcome boundary. This avoids naively pairing a human action only with the state captured at the same instant.

The final session step may be marked `incompleteOutcome=true` when recording stops before a newer observation is available.

## Tools

Validate:

```bash
python sawbot-tools/dataset-validator/validate_telemetry.py trajectory.sbt
```

Recover an interrupted file:

```bash
python sawbot-tools/dataset-validator/validate_telemetry.py trajectory.sbt.partial --recover
```

Inspect a compact timeline:

```bash
python sawbot-tools/replay-inspector/inspect_telemetry.py trajectory.sbt --limit 40
```

## Migration policy

Persistent readers must reject unknown major versions. Minor additions require either reserved fields or an explicit schema bump. Field meaning must never change silently. Observation and action schema identifiers are stored in the file so incompatible datasets can be rejected before training.
