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
  "docs/PHASE7_REPORT.md"
  "docs/ADAPTIVE_NAVIGATION.md"
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
  sha256sum SawBotV1-* PHASE7_REPORT.md ADAPTIVE_NAVIGATION.md PHASE6_REPORT.md \
    HYBRID_ARCHITECTURE.md NAVIGATION_BODY.md PHASE5_REPORT.md WAYPOINT_MODEL.md \
    PHASE4_RUNTIME_FINDINGS.md PHASE4_REPORT.md MODEL_BRIDGE_PROTOCOL.md \
    PHASE3_RUNTIME_STATUS.md PHASE3_REPORT.md TELEMETRY_FORMAT.md \
    PHASE2_ACCEPTANCE.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md \
    GITHUB_RELEASES.md waypoint_v0.1.json waypoint_eval_v0.1.json \
    waypoint_failures_v0.1.jsonl > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<'NOTES'
# SawBotV1 @VERSION@

Phase 7 real-time adaptive-navigation candidate for Minecraft Forge 1.8.9.

## Main change

The navigation specialist now treats a route as a continuously revised safe corridor rather than a mandatory sequence of block centres.

This release adds bounded anytime A* frontier routes, current-position rolling replanning, active-route hot swapping, bounded path re-anchoring after displacement, safe lookahead smoothing, live block/support invalidation, several 20 Hz local steering candidates, faster bounded visible camera response, and detailed adaptive diagnostics.

Routine replanning no longer releases movement. Manual takeover, emergency stop, disable, freeze, GUI pause, and world unload remain authoritative and cause the next enable to plan from the player's actual current position.

The learned brain remains responsible for goals, tactics, targets, and specialist selection. Deterministic bodies execute known mechanics.

## Primary assets

- `SawBotV1-@VERSION@-mc1.8.9.jar`: installable mod.
- `SawBotV1-@VERSION@-sources.jar`: source archive.
- `PHASE7_REPORT.md`: implementation and verification evidence.
- `ADAPTIVE_NAVIGATION.md`: real-time planner/follower contract.
- `HYBRID_ARCHITECTURE.md`: brain/body boundary.
- `NAVIGATION_BODY.md`: current navigation-body summary.
- `SHA256SUMS.txt`: integrity hashes.
NOTES
sed -i "s/@VERSION@/$VERSION/g" "$DIST/release-notes.md"

printf 'Packaged Phase 7 release assets in %s\n' "$DIST"
ls -lh "$DIST"
