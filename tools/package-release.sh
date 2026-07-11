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
[[ -f "$SOURCES_JAR" ]] || { echo "ERROR: sources JAR does not exist: $SOURCES_JAR" >&2; exit 3; }

assets=(
  "docs/PHASE4_REPORT.md"
  "docs/MODEL_BRIDGE_PROTOCOL.md"
  "docs/PHASE3_RUNTIME_STATUS.md"
  "docs/PHASE3_REPORT.md"
  "docs/TELEMETRY_FORMAT.md"
  "docs/PHASE2_ACCEPTANCE.md"
  "docs/PHASE1_ACCEPTANCE.md"
  "docs/PHASE0_ACCEPTANCE.md"
  "docs/GITHUB_RELEASES.md"
)

cp "$FINAL_JAR" "$DIST/"
cp "$SOURCES_JAR" "$DIST/"
for asset in "${assets[@]}"; do cp "$ROOT/$asset" "$DIST/$(basename "$asset")"; done

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE4_REPORT.md MODEL_BRIDGE_PROTOCOL.md PHASE3_RUNTIME_STATUS.md PHASE3_REPORT.md TELEMETRY_FORMAT.md PHASE2_ACCEPTANCE.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md GITHUB_RELEASES.md > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<NOTES
# SawBotV1 $VERSION

Phase 4 safe-actuator and local-model-bridge candidate for Minecraft Forge 1.8.9.

## Scope

This release adds the bounded CRC-protected \`sawbot.bridge/0.1\` loopback protocol, non-blocking model transport, strict action deadlines and reference validation, local/private environment guard, physical takeover, disconnect release, legitimate movement/camera/hotbar/attack/use/drop/inventory actuation, latency instrumentation, and a deterministic dummy model. It also bundles Phase 3 telemetry restart/error hardening and removes the raw OpenGL attribute-stack path associated with hotbar/held-item tint.

It intentionally contains no learned neural policy, Bedwars strategy, handcrafted runtime pathfinder, aim helper, scaffold controller, screenshot/OCR input, packet advantage, or public-server automation.

## Primary assets

- \`SawBotV1-$VERSION-mc1.8.9.jar\`: installable mod.
- \`SawBotV1-$VERSION-sources.jar\`: source archive.
- \`SHA256SUMS.txt\`: integrity hashes.
- \`PHASE4_REPORT.md\`: implementation and focused runtime gate.
- \`MODEL_BRIDGE_PROTOCOL.md\`: protocol, threading, and safety contract.
- \`PHASE3_RUNTIME_STATUS.md\`: telemetry target-machine finding and bundled hardening.
NOTES

printf 'Packaged release assets in %s\n' "$DIST"
ls -lh "$DIST"
