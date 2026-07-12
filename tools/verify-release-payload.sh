#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:?usage: tools/verify-release-payload.sh VERSION [DIST_DIR]}"
DIST="${2:-$ROOT/dist}"
required=(
  "SawBotV1-$VERSION-mc1.8.9.jar"
  "SawBotV1-$VERSION-sources.jar"
  "SHA256SUMS.txt"
  "release-notes.md"
  "PHASE8_REPORT.md"
  "BRIDGING_BODY.md"
  "PHASE7_REPORT.md"
  "ADAPTIVE_NAVIGATION.md"
  "PHASE6_REPORT.md"
  "HYBRID_ARCHITECTURE.md"
  "NAVIGATION_BODY.md"
  "PHASE5_REPORT.md"
  "WAYPOINT_MODEL.md"
  "PHASE4_RUNTIME_FINDINGS.md"
  "PHASE4_REPORT.md"
  "MODEL_BRIDGE_PROTOCOL.md"
  "PHASE3_RUNTIME_STATUS.md"
  "PHASE3_REPORT.md"
  "TELEMETRY_FORMAT.md"
  "PHASE2_ACCEPTANCE.md"
  "PHASE1_ACCEPTANCE.md"
  "PHASE0_ACCEPTANCE.md"
  "GITHUB_RELEASES.md"
  "waypoint_v0.1.json"
  "waypoint_eval_v0.1.json"
  "waypoint_failures_v0.1.jsonl"
)
for file in "${required[@]}"; do
  [[ -f "$DIST/$file" ]] || { echo "ERROR: release payload is missing $DIST/$file" >&2; exit 2; }
done
python3 "$ROOT/tools/verify-built-jar.py" "$DIST/SawBotV1-$VERSION-mc1.8.9.jar" --expected-version "$VERSION"
python3 "$ROOT/sawbot-trainer/waypoint/verify_phase5.py"
(cd "$DIST" && sha256sum -c SHA256SUMS.txt)
printf 'PASS verified Phase 8 release payload for %s\n' "$VERSION"
