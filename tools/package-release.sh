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
  "docs/PHASE6_REPORT.md"
  "docs/HYBRID_ARCHITECTURE.md"
  "docs/NAVIGATION_BODY.md"
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
  sha256sum SawBotV1-* PHASE6_REPORT.md HYBRID_ARCHITECTURE.md NAVIGATION_BODY.md \
    PHASE5_REPORT.md WAYPOINT_MODEL.md PHASE4_RUNTIME_FINDINGS.md PHASE4_REPORT.md \
    MODEL_BRIDGE_PROTOCOL.md PHASE3_RUNTIME_STATUS.md PHASE3_REPORT.md \
    TELEMETRY_FORMAT.md PHASE2_ACCEPTANCE.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md \
    GITHUB_RELEASES.md waypoint_v0.1.json waypoint_eval_v0.1.json \
    waypoint_failures_v0.1.jsonl > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<'NOTES'
# SawBotV1 @VERSION@

Phase 6 hybrid navigation-body candidate for Minecraft Forge 1.8.9.

## Main change

SawBotV1 now separates the learned brain from deterministic specialist bodies. The brain chooses goals and skills; the body performs known mechanics reliably.

This release adds bounded incremental A*, sustained movement ownership, visible responsive turning, step-up jumping, safe support/liquid/hazard checks, stable arrival, stuck recovery, replanning, and a visible route/current node. A G waypoint can be followed directly without the external model, while a connected brain can request the same navigation skill through the existing Action Contract.

It also restores real physical key state when synthetic control ends, adds persistent takeover/emergency feedback, stabilizes offline brain presentation, and simplifies telemetry retry while isolating malformed steps.

The Phase 5 neural waypoint classifier remains published as a historical training/bridge baseline but is not the primary navigator.

## Primary assets

- `SawBotV1-@VERSION@-mc1.8.9.jar`: installable mod.
- `SawBotV1-@VERSION@-sources.jar`: source archive.
- `PHASE6_REPORT.md`: implementation and verification evidence.
- `HYBRID_ARCHITECTURE.md`: brain/body boundary.
- `NAVIGATION_BODY.md`: planner/follower contract.
- `SHA256SUMS.txt`: integrity hashes.
NOTES
sed -i "s/@VERSION@/$VERSION/g" "$DIST/release-notes.md"

printf 'Packaged Phase 6 release assets in %s\n' "$DIST"
ls -lh "$DIST"
