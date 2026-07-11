# Local Model Bridge Protocol

Protocol identifier: `sawbot.bridge/0.1`

Status: Phase 4 runtime candidate

## Purpose

The bridge connects the visible Forge client to a local model process without making the Minecraft client thread wait for inference or socket I/O. The current dummy model is deterministic test infrastructure, not the final neural policy.

Default endpoint: `127.0.0.1:25189`.

## Transport and framing

- TCP, loopback by default.
- Network byte order for frame headers and action payloads.
- Maximum payload: 262,144 bytes.
- Frame header: magic `SBM1`, protocol version, frame type, reserved flags, payload length, CRC32.
- Unknown, corrupt, oversized, or version-mismatched frames are rejected.
- Observation payloads use the bounded little-endian `OBS1` encoding shared with telemetry internals.

Frame types:

| Type | Direction | Meaning |
|---|---|---|
| `HELLO` | client → model | protocol/schema/version negotiation plus random session nonce |
| `HELLO_ACK` | model → client | nonce echo, model identity, capabilities |
| `OBSERVATION` | client → model | immutable Observation Contract snapshot |
| `ACTION` | model → client | Action Contract v0.1 command |
| `PING/PONG` | both | liveness/latency support |
| `ERROR` | both | terminal protocol error |
| `GOODBYE` | both | clean disconnect |

## Threading and bounds

The client thread only performs bounded queue operations:

- observation queue: 2 entries, latest wins;
- action queue: 8 entries, newest action is consumed;
- sent-observation timestamp cache: 32 entries;
- socket connect timeout: configurable, default 500 ms, worker thread only;
- socket read timeout: 100 ms, worker thread only;
- reconnect delay: configurable, default 1,000 ms.

The worker is daemonized and never calls Minecraft world APIs. It receives immutable snapshots produced on the client thread.

## Handshake

The client sends:

- bridge identifier;
- mod version;
- observation schema identifier;
- action schema identifier;
- random 64-bit nonce;
- decision-rate expectation.

The model must echo the nonce and provide a non-empty model version. SawBot does not enter `READY` until this succeeds.

## Action acceptance

An action is accepted only when all of the following are true:

- the bridge is ready;
- SawBot is explicitly enabled;
- observations are live, not frozen;
- the current environment is single-player or an exact configured private host;
- Action Contract ranges and finite-value checks pass;
- source observation sequence is within the configured lag bound;
- local receive-time age is within the deadline;
- selected entity/landmark references exist in the latest snapshot;
- no GUI or physical-human-takeover condition blocks application.

Disconnect, stale action, invalid context, world unload, F9, F12, or physical human input releases every synthetic control.

## Dummy model

Run one of:

```text
sawbot-tools\dummy-model\RUN-DUMMY-MODEL.bat
sawbot-tools\dummy-model\RUN-ACTUATOR-DEMO.bat
```

The interactive model accepts: `idle`, `forward`, `back`, `left`, `right`, `jump`, `sprint`, `sneak`, `yaw <deg>`, `pitch <deg>`, `attack`, `use`, `drop`, `inventory`, `slot <1-9>`, `demo`, `status`, and `quit`.

The dummy model exists only to prove transport, deadlines, control actuation, disconnect behavior, and instrumentation before a learned policy is connected.
