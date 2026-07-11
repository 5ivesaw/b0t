# Phase 4 Report — Safe Actuator and Local Model Bridge

Candidate version: `0.5.0-alpha.0`

Bridge protocol: `sawbot.bridge/0.1`

Action schema: `sawbot.action/0.1`

Observation schema: `sawbot.observation/0.3`

## Delivered

- Background TCP model bridge bound to loopback by default.
- Versioned handshake with nonce verification and explicit model identity.
- Bounded CRC-protected frames and strict payload limits.
- Latest-wins immutable observation publication.
- Bounded action queue and newest-action consumption.
- Round-trip latency, queue depth, reconnect, drop, and invalid-frame metrics.
- Client-thread-only legitimate-control actuator.
- W/A/S/D, jump, sprint, sneak, hotbar, attack, use/place, drop, inventory toggle.
- Smooth bounded yaw/pitch application across action duration.
- Action sequence, age, finite/range, target, and waypoint validation.
- Single-player/private-host runtime boundary.
- Physical keyboard/mouse takeover.
- F9/F12/disconnect/world-unload complete input release.
- Deterministic Python dummy model and automated actuator demo.
- Ninth compact inspector page for bridge/action state.
- Previous applied action fed into subsequent observations.
- Phase 3 telemetry failure/restart hardening.
- OpenGL state restoration fix for overlay-caused hotbar/held-item tint.

## Safety behavior

The model bridge running is not enough to move the player. SawBot must also be explicitly enabled with F10, the environment guard must allow the world/server, observations must be live, and the action must pass every validator.

F9, F12, physical movement/mouse input, model disconnect, world unload, stale action, or blocked scope immediately releases controlled inputs. Runtime code does not add reach, teleportation, impossible placement, packet advantage, silent rotation, runtime pathfinding, aim correction, or scaffold correction.

## Timing and threading

All socket connect/read/write work runs on `SawBotV1-ModelBridge`, a daemon worker. Minecraft state is read only by the existing client-thread observation pipeline. Client-thread publication is an `ArrayBlockingQueue.offer` operation and never waits for inference.

Default constraints:

- model decision rate: 10 Hz;
- maximum action age after local receipt: 250 ms;
- maximum source-observation lag: 3 snapshots;
- action duration: 1–4 client ticks;
- observation queue: 2;
- action queue: 8;
- reconnect delay: 1 second.

## Verification

Offline verification covers:

- Java 8 source compatibility;
- Action Contract structural and context validation;
- bridge frame round trip, CRC rejection, and payload bound;
- deterministic action decoding;
- real loopback handshake, observation transfer, action return, and non-blocking publication;
- public-server blocking and explicit private-host allowance;
- legitimate movement/camera/hotbar/one-shot actuation;
- duration completion and emergency release;
- observation bridge payload version/magic;
- dummy-model protocol self-test;
- Phase 3 trajectory framing, CRC, validator, replay, and recovery;
- release structure and required JAR classes.

The genuine Forge/Loom remap remains the GitHub Actions build gate.

## Runtime acceptance

Only new Phase 4 behavior needs target-machine testing:

- dummy model reaches `READY`;
- F10 enables only while ready and in allowed scope;
- interactive movement/camera/hotbar/attack/use commands actuate correctly;
- camera deltas are smooth;
- physical input takes control immediately;
- closing the model disables SawBot and releases inputs;
- stale/disconnected model cannot leave a held key;
- MODEL inspector displays latency and queue/counter state;
- telemetry can restart after an error and produce a validator-clean trajectory;
- N/C overlays no longer tint the hotbar or held item;
- no repeated SawBot exception during a focused five-minute actuator test.

## Explicitly not delivered

- learned neural policy;
- Bedwars strategy or task adapter;
- runtime pathfinder, aim helper, scaffold, or handcrafted tactical override;
- public-server automation;
- screenshot, OCR, video, or pixel input;
- packet manipulation for advantage.
