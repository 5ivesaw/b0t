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
cp "$ROOT/docs/PHASE0_REPORT.md" "$DIST/PHASE0_REPORT.md"
cp "$ROOT/docs/GITHUB_RELEASES.md" "$DIST/GITHUB_RELEASES.md"

(
  cd "$DIST"
  sha256sum SawBotV1-* PHASE0_REPORT.md GITHUB_RELEASES.md > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<NOTES
# SawBotV1 $VERSION

Phase 0 Forge foundation for Minecraft 1.8.9.

## Release assets

- \`SawBotV1-$VERSION-mc1.8.9.jar\`: installable Forge mod.
- \`SawBotV1-$VERSION-sources.jar\`: Java source archive.
- \`SHA256SUMS.txt\`: integrity hashes.
- \`PHASE0_REPORT.md\`: exact implementation and verification report.

## Scope

This release contains the safe lifecycle, key bindings, compact HUD, versioned observation/action contracts, performance instrumentation, and project documentation. It intentionally contains no neural model, Bedwars automation, packet advantage, screenshot pipeline, aim assist, scaffold controller, or public-server automation.
NOTES

printf 'Packaged release assets in %s\n' "$DIST"
ls -lh "$DIST"
