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
  "docs/PHASE12_REPORT.md"
  "docs/COMBAT_BODY.md"
  "docs/HUMAN_MOTION_PROFILE.md"
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

Phase 12 human-motion and explicit-target PvP motor candidate for Minecraft Forge 1.8.9.

## Main change

SawBot now has its first bounded local combat motor. The learned brain or explicit private
test harness selects one tracked player; the body never scans for or substitutes an
opponent. It turns the visible camera with bounded rate and acceleration, maintains local
approach/spacing/retreat movement, guards unsupported directions, and attacks only with
current line of sight, normal range, alignment, hurt-time recovery, and cooldown.

Combat yields entirely when intent ends or the selected target is invalid. F9, F12,
physical takeover, disable, freeze, GUI/world loss, skill switch, disconnect, and runtime
errors restore all owned inputs. Navigation and bridging remain retained deterministic
bodies below combat in the specialist priority chain.

Phase 12 also stabilizes the asynchronous support-break navigation contract by giving the
planner worker a deterministic scheduling window in the synthetic tight-loop test while
preserving the original invalidation, replan, and successful-reroute assertions.

No external model process is required for deterministic body testing. The neural brain
remains responsible for target, objective, tactics, risk, and specialist selection.

## Primary assets

- `SawBotV1-@VERSION@-mc1.8.9.jar`: installable mod.
- `SawBotV1-@VERSION@-sources.jar`: source archive.
- `PHASE12_REPORT.md`: implementation and verification evidence.
- `COMBAT_BODY.md`: explicit-target motor contract.
- `HUMAN_MOTION_PROFILE.md`: visible bounded motion profile.
- `REFERENCE_BODY_RESEARCH.md`: pinned reference and clean-room decisions.
- retained Phase 11 navigation, bridging, visualization, telemetry, and model evidence.
- `SHA256SUMS.txt`: integrity hashes.
NOTES
sed -i "s/@VERSION@/$VERSION/g" "$DIST/release-notes.md"

printf 'Packaged Phase 12 release assets in %s\n' "$DIST"
ls -lh "$DIST"
