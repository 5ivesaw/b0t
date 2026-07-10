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
cp "$ROOT/docs/PHASE1_REPORT.md" "$DIST/PHASE1_REPORT.md"
cp "$ROOT/docs/PHASE0_ACCEPTANCE.md" "$DIST/PHASE0_ACCEPTANCE.md"
cp "$ROOT/docs/GITHUB_RELEASES.md" "$DIST/GITHUB_RELEASES.md"

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE1_REPORT.md PHASE0_ACCEPTANCE.md GITHUB_RELEASES.md > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<NOTES
# SawBotV1 $VERSION

Phase 1 internal-eyes candidate for Minecraft Forge 1.8.9.

## Release assets

- \`SawBotV1-$VERSION-mc1.8.9.jar\`: installable Forge mod.
- \`SawBotV1-$VERSION-sources.jar\`: Java source archive.
- \`SHA256SUMS.txt\`: integrity hashes.
- \`PHASE1_REPORT.md\`: implementation, validation, limitations, and runtime checklist.
- \`PHASE0_ACCEPTANCE.md\`: recorded Phase 0 runtime acceptance evidence.

## Scope

This release adds exact client-internal self state, a bounded 13x9x13 egocentric terrain tensor, an incrementally cached 33x33 mid-range map, loaded-entity tracking with explicit line-of-sight/occlusion/attackability, inventory encoding, bounded events, landmarks, server timing, immutable ObservationSnapshot v0.2, and an in-game inspector.

It intentionally contains no neural model, autonomous actuator loop, Bedwars policy, screen capture, OCR, packet advantage, reach modification, aim assist, scaffold controller, or public-server automation.
NOTES

printf 'Packaged release assets in %s\n' "$DIST"
ls -lh "$DIST"
