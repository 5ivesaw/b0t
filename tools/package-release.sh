#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:?usage: tools/package-release.sh VERSION}"
DIST="$ROOT/dist"
LIBS="$ROOT/sawbot-forge-1.8.9/build/libs"
FINAL_JAR="$LIBS/SawBotV1-$VERSION-mc1.8.9.jar"
SOURCES_JAR="$LIBS/SawBotV1-$VERSION-sources.jar"

rm -rf "$DIST"
mkdir -p "$DIST"

python3 "$ROOT/tools/verify-built-jar.py" "$FINAL_JAR" --expected-version "$VERSION"
python3 "$ROOT/sawbot-trainer/waypoint/verify_phase5.py"
[[ -f "$SOURCES_JAR" ]] || { echo "ERROR: sources JAR does not exist: $SOURCES_JAR" >&2; exit 3; }

assets=(
  "docs/PHASE5_REPORT.md"
  "docs/WAYPOINT_MODEL.md"
  "docs/PHASE4_RUNTIME_FINDINGS.md"
  "docs/PHASE4_REPORT.md"
  "docs/MODEL_BRIDGE_PROTOCOL.md"
  "docs/PHASE3_RUNTIME_STATUS.md"
  "docs/PHASE3_REPORT.md"
  "docs/TELEMETRY_FORMAT.md"
  "docs/PHASE2_ACCEPTANCE.md"
  "docs/PHASE1_ACCEPTANCE.md"
  "docs/PHASE0_ACCEPTANCE.md"
  "docs/GITHUB_RELEASES.md"
  "sawbot-trainer/waypoint/checkpoints/waypoint_v0.1.json"
  "sawbot-trainer/waypoint/evaluation/waypoint_eval_v0.1.json"
  "sawbot-trainer/waypoint/evaluation/waypoint_failures_v0.1.jsonl"
)

cp "$FINAL_JAR" "$DIST/"
cp "$SOURCES_JAR" "$DIST/"
for asset in "${assets[@]}"; do cp "$ROOT/$asset" "$DIST/$(basename "$asset")"; done

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE5_REPORT.md WAYPOINT_MODEL.md PHASE4_RUNTIME_FINDINGS.md     PHASE4_REPORT.md MODEL_BRIDGE_PROTOCOL.md PHASE3_RUNTIME_STATUS.md PHASE3_REPORT.md     TELEMETRY_FORMAT.md PHASE2_ACCEPTANCE.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md     GITHUB_RELEASES.md waypoint_v0.1.json waypoint_eval_v0.1.json     waypoint_failures_v0.1.jsonl > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<'NOTES'
# SawBotV1 @VERSION@

Phase 5 first-learned-behaviour candidate for Minecraft Forge 1.8.9.

## Scope

This release adds user waypoint #1000, a deterministic balanced mechanics dataset, an exported 18→32→7 MLP, held-out rollout evaluation, failure examples, and a pure-standard-library local inference process over the existing bounded bridge/action contracts. The checked-in evaluation records 87.25% success over 800 held-out starts versus 3.625% for the random baseline.

It also bundles the Phase 4 runtime corrections: disabled/waiting actuator ticks no longer clear human movement keys, bridge status no longer flickers on every reconnect attempt, F9/F12 receive distinct notices, and normal telemetry close no longer interrupts an active NIO write.

There is no runtime teacher, pathfinder, hidden steering correction, Bedwars strategy, aim helper, scaffold controller, screenshot/OCR input, packet advantage, or public-server automation.

## Primary assets

- `SawBotV1-@VERSION@-mc1.8.9.jar`: installable mod.
- `SawBotV1-@VERSION@-sources.jar`: source archive.
- `waypoint_v0.1.json`: learned checkpoint.
- `waypoint_eval_v0.1.json`: held-out evaluation and random baseline.
- `waypoint_failures_v0.1.jsonl`: retained failure examples.
- `PHASE5_REPORT.md`: architecture, data, model, and gate evidence.
- `SHA256SUMS.txt`: integrity hashes for every published asset.
NOTES
sed -i "s/@VERSION@/$VERSION/g" "$DIST/release-notes.md"

printf 'Packaged release assets in %s
' "$DIST"
ls -lh "$DIST"
