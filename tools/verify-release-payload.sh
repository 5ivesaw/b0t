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
  "PHASE4_REPORT.md"
  "MODEL_BRIDGE_PROTOCOL.md"
  "PHASE3_RUNTIME_STATUS.md"
  "PHASE3_REPORT.md"
  "TELEMETRY_FORMAT.md"
  "PHASE2_ACCEPTANCE.md"
  "PHASE1_ACCEPTANCE.md"
  "PHASE0_ACCEPTANCE.md"
  "GITHUB_RELEASES.md"
)
for file in "${required[@]}"; do [[ -f "$DIST/$file" ]] || { echo "ERROR: release payload is missing $DIST/$file" >&2; exit 2; }; done
python3 "$ROOT/tools/verify-built-jar.py" "$DIST/SawBotV1-$VERSION-mc1.8.9.jar" --expected-version "$VERSION"
(cd "$DIST" && sha256sum -c SHA256SUMS.txt)
printf 'PASS verified release payload for %s\n' "$VERSION"
