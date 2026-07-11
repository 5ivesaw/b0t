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
[[ -f "$SOURCES_JAR" ]] || {
  echo "ERROR: sources JAR does not exist: $SOURCES_JAR" >&2
  exit 3
}

cp "$FINAL_JAR" "$DIST/"
cp "$SOURCES_JAR" "$DIST/"
cp "$ROOT/docs/PHASE2_REPORT.md" "$DIST/PHASE2_REPORT.md"
cp "$ROOT/docs/PHASE2_RUNTIME_FEEDBACK.md" "$DIST/PHASE2_RUNTIME_FEEDBACK.md"
cp "$ROOT/docs/PHASE1_ACCEPTANCE.md" "$DIST/PHASE1_ACCEPTANCE.md"
cp "$ROOT/docs/PHASE0_ACCEPTANCE.md" "$DIST/PHASE0_ACCEPTANCE.md"
cp "$ROOT/docs/GITHUB_RELEASES.md" "$DIST/GITHUB_RELEASES.md"
cp "$ROOT/docs/INTERFACE_DESIGN_SYSTEM.md" "$DIST/INTERFACE_DESIGN_SYSTEM.md"
cp "$ROOT/docs/PHASE2_UI_REFRESH.md" "$DIST/PHASE2_UI_REFRESH.md"

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE2_REPORT.md PHASE2_RUNTIME_FEEDBACK.md PHASE2_UI_REFRESH.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md GITHUB_RELEASES.md INTERFACE_DESIGN_SYSTEM.md > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<NOTES
# SawBotV1 $VERSION

Phase 2 premium inspector candidate for Minecraft Forge 1.8.9.

## Release assets

- \`SawBotV1-$VERSION-mc1.8.9.jar\`: installable Forge mod.
- \`SawBotV1-$VERSION-sources.jar\`: Java source archive.
- \`SHA256SUMS.txt\`: integrity hashes.
- \`PHASE2_REPORT.md\`: implementation, validation, limitations, and runtime checklist.
- \`PHASE1_ACCEPTANCE.md\`: recorded Phase 1 runtime acceptance evidence.
- \`PHASE0_ACCEPTANCE.md\`: recorded Phase 0 runtime acceptance evidence.
- \`INTERFACE_DESIGN_SYSTEM.md\`: locked visual and interaction-quality standard.
- \`PHASE2_UI_REFRESH.md\`: premium HUD implementation details and real-client checklist.

## Scope

This release retains the accepted Phase 2 inspector, immediate LOS/OCC visual-state correction, and verified single-push release lane. It replaces the original debug-text wall with a compact status island and a polished eight-page inspector workspace, adds backed world-label chips, real keybinding labels, reduced-motion support, centralized visual tokens, and independent HUD render timing. Runtime sensor semantics remain unchanged.

It intentionally contains no neural model, autonomous actuator loop, Bedwars policy, screen capture, OCR, packet advantage, reach modification, aim assist, scaffold controller, or public-server automation.
NOTES

printf 'Packaged release assets in %s\n' "$DIST"
ls -lh "$DIST"
