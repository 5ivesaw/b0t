# Risk Register v0.1

| Technical risk | Probability | Impact | Early warning | Mitigation |
|---|---:|---:|---|---|
| Legacy Forge/Loom dependencies disappear or fail repository resolution | Medium | High | Cold GitHub build cannot resolve Loom, MCP, Forge, or Pack200 artifacts | Pin exact plugin/Forge versions, verify Gradle distribution checksum, keep cold-cache CI coverage, record resolved failures, never substitute untrusted binaries |
| Java 17/Java 8 toolchain discovery breaks setup | Medium | High | Gradle starts but compilation reports no matching Java 8 toolchain | CI installs both explicitly; local preflight requires Java 17 on PATH, validates `JAVA8_HOME`, and registers the Java 8 path with Gradle |
| Sensor extraction exceeds 20 TPS budget | Medium | High | p95 extraction >1.5 ms or visible frame spikes | Incremental terrain updates, cached semantics, primitive arrays, per-sensor timing, disabled debug overlays |
| Unsafe worker access to Minecraft objects | Medium | Critical | Concurrent modification, random crashes, inconsistent snapshots | Client-thread snapshot builder; immutable copies; worker APIs accept common-contract values only |
| Observation meaning drifts without version bump | Medium | Critical | Same field name produces different distributions | Contract tests, schema hash, migration notes, explicit semantic version review |
| Synthetic input remains pressed after failure | Medium | Critical | Movement continues after disable/disconnect | Central state controller, zero-action timeout, release-all on every lifecycle boundary, regression test |
| Human actions cannot reliably override synthetic state | Medium | High | Takeover key still leaves held movement/attack | Safety key processed first; immediate release; later raw-input arbitration and UI indicator |
| Dataset logging causes stutter or corruption | Medium | High | frame-time spikes, partial trailing records, growing queue | Bounded queue, async buffered writer, checksummed chunks, recoverable footer, drop counters |
| Simulator teaches incorrect mechanics | High | High | real/sim trajectory divergence exceeds tolerance | Mechanic-by-mechanic differential tests; no simulator curriculum expansion before pass |
| Teacher behaviour leaks into final runtime | Medium | Critical | runtime package references teacher namespace or planner call | Separate module/package, dependency rule, artifact inspection, final-build forbidden-reference test |
| Reward exploit creates impressive but invalid behaviour | Medium | High | reward rises while rendered success falls | fixed evaluation seeds, behaviour review, outcome metrics, exploit-oriented tests |
| CPU inference misses deadlines on i5-6200U | Medium | High | p95 >60 ms at 10 Hz, FPS loss | small model, quantization/ONNX benchmarking, bounded history, lower decision rate, zero-action deadline |
| Entity list ordering destabilizes learning | Medium | Medium | same state yields permuted slots | persistent short-lived IDs, deterministic priority and tie-break ordering |
| Private research build is used on public servers | Low | Critical | non-owned server connection while enabled | environment allowlist/offline default, prominent HUD warning, autonomous enable refused outside approved worlds in later phase |
| Occluded exact state dominates policy and harms restricted vision | Medium | Medium | policy collapses when restricted-vision flag is enabled | mark visibility explicitly, optional masking curriculum, separate evaluation suite |
