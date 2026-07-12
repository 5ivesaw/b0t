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
  "docs/PHASE11_REPORT.md"
  "docs/REFERENCE_BODY_RESEARCH.md"
  "docs/VISUALIZATION_LIFECYCLE.md"
  "docs/PHASE10_REPORT.md"
  "docs/CONTINUOUS_ANYTIME_NAVIGATION.md"
  "docs/PHASE9_REPORT.md"
  "docs/SEGMENTED_NAVIGATION_CORE.md"
  "docs/BARITONE_ARCHITECTURE_RESEARCH.md"
  "docs/NAVIGATION_BODY.md"
  "docs/PHASE8_REPORT.md"
  "docs/BRIDGING_BODY.md"
  "docs/PHASE7_REPORT.md"
  "docs/ADAPTIVE_NAVIGATION.md"
  "docs/PHASE6_REPORT.md"
  "docs/HYBRID_ARCHITECTURE.md"
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
for asset in "${assets[@]}"; do
  [[ -f "$ROOT/$asset" ]] || { echo "ERROR: release asset missing: $asset" >&2; exit 4; }
  cp "$ROOT/$asset" "$DIST/$(basename "$asset")"
done

(
  cd "$DIST"
  mapfile -t HASH_FILES < <(find . -maxdepth 1 -type f ! -name SHA256SUMS.txt ! -name release-notes.md -printf '%f\n' | sort)
  sha256sum "${HASH_FILES[@]}" > SHA256SUMS.txt
)

cat > "$DIST/release-notes.md" <<'NOTES'
# SawBotV1 @VERSION@

Phase 11 reference-driven-body candidate for Minecraft Forge 1.8.9.

## Main change

Mechanical body work is now reference-driven and license-audited. Bridge placement
evaluates every legal adjacent support and multiple hit vectors, then chooses a visible
normal-reach candidate instead of trusting one exact face center. Debug overlays now
follow body ownership and disappear immediately when the body releases, completes, or
loses its waypoint.

The navigation body remains an operation-based segmented system instead of a rigid
client-thread block list. Minecraft state is captured into bounded immutable snapshots;
one latest-wins worker performs weighted A* over traverse, diagonal, ascent, and descent
operations. The body can begin on a validated micro-route, plan local/full replacement
segments, splice safely, rewind/skip after displacement, project back into a nearby route
corridor, invalidate changed geometry, and replan from the actual player position.

Movement controls are continuous, camera changes remain visible and bounded, route
lookahead is live-validated, and F9/F12/physical takeover retain priority. Search, queues,
snapshots, caches, live validation, and rendering remain bounded for the low-end target.

No external model process is required for deterministic waypoint navigation. The neural
brain remains responsible for selecting goals and tactics, not low-level path mechanics.

## Primary assets

- `SawBotV1-@VERSION@-mc1.8.9.jar`: installable mod.
- `SawBotV1-@VERSION@-sources.jar`: source archive.
- `PHASE11_REPORT.md`: reference-driven body and visualization evidence.
- `REFERENCE_BODY_RESEARCH.md`: reviewed projects and clean-room boundary.
- `VISUALIZATION_LIFECYCLE.md`: overlay ownership, expiry, and render caps.
- `PHASE10_REPORT.md`: continuous anytime navigation implementation evidence.
- `CONTINUOUS_ANYTIME_NAVIGATION.md`: current rolling planner/executor contract.
- `PHASE9_REPORT.md`: previous segmented-core implementation evidence.
- `SEGMENTED_NAVIGATION_CORE.md`: current navigation contract.
- `BARITONE_ARCHITECTURE_RESEARCH.md`: design provenance and clean-room boundary.
- `NAVIGATION_BODY.md`: deterministic body details.
- `PHASE8_REPORT.md` and `BRIDGING_BODY.md`: retained bridging specialist.
- `SHA256SUMS.txt`: integrity hashes.
NOTES
sed -i "s/@VERSION@/$VERSION/g" "$DIST/release-notes.md"

printf 'Packaged Phase 11 release assets in %s\n' "$DIST"
ls -lh "$DIST"
