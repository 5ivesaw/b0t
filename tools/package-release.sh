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
cp "$ROOT/docs/PHASE2_RUNTIME_VALIDATION.md" "$DIST/PHASE2_RUNTIME_VALIDATION.md"
cp "$ROOT/docs/PHASE1_ACCEPTANCE.md" "$DIST/PHASE1_ACCEPTANCE.md"
cp "$ROOT/docs/PHASE0_ACCEPTANCE.md" "$DIST/PHASE0_ACCEPTANCE.md"
cp "$ROOT/docs/GITHUB_RELEASES.md" "$DIST/GITHUB_RELEASES.md"
cp "$ROOT/docs/PHASE2_UI_REVERT.md" "$DIST/PHASE2_UI_REVERT.md"

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE2_REPORT.md PHASE2_RUNTIME_FEEDBACK.md PHASE2_RUNTIME_VALIDATION.md PHASE1_ACCEPTANCE.md PHASE0_ACCEPTANCE.md GITHUB_RELEASES.md PHASE2_UI_REVERT.md > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<NOTES
# SawBotV1 $VERSION

Phase 2 sensor-inspector candidate for Minecraft Forge 1.8.9.

## Release assets

- \`SawBotV1-$VERSION-mc1.8.9.jar\`: installable Forge mod.
- \`SawBotV1-$VERSION-sources.jar\`: Java source archive.
- \`SHA256SUMS.txt\`: integrity hashes.
- \`PHASE2_REPORT.md\`: implementation, validation, limitations, and runtime checklist.
- \`PHASE2_RUNTIME_VALIDATION.md\`: target-machine evidence, findings, and alpha.6 corrections.
- \`PHASE1_ACCEPTANCE.md\`: recorded Phase 1 runtime acceptance evidence.
- \`PHASE0_ACCEPTANCE.md\`: recorded Phase 0 runtime acceptance evidence.
- \`PHASE2_UI_REVERT.md\`: target-machine rationale and exact scope of the text-HUD restoration.

## Scope

This release hardens the accepted compact Phase 2 inspector after a long target-machine session. It makes frozen world-anchored snapshot semantics explicit, adds bounded specific entity types and dropped-item payload categories through Observation Contract v0.3, forces immediate refresh on unfreeze, makes block selection consistently yellow, expires transient success notices, and restores complete OpenGL state after debug rendering. The single-push GitHub release lane remains unchanged.

It intentionally contains no neural model, autonomous actuator loop, Bedwars policy, screen capture, OCR, packet advantage, reach modification, aim assist, scaffold controller, or public-server automation.
NOTES

printf 'Packaged release assets in %s\n' "$DIST"
ls -lh "$DIST"
