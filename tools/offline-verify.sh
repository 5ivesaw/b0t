#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/offline-classes"
VERSION="$(bash "$ROOT/tools/read-version.sh")"
export SAWBOT_VERIFY_VERSION="$VERSION"

rm -rf "$OUT" "$ROOT/build/offline-jar-stage" "$ROOT/dist"
mkdir -p "$OUT"
mapfile -t SOURCES < <(find \
  "$ROOT/verification-stubs/src" \
  "$ROOT/sawbot-common/src/main/java" \
  "$ROOT/sawbot-forge-1.8.9/src/main/java" \
  "$ROOT/verification-tests/src" \
  -name '*.java' -print | sort)
JAVAC=(javac)
if javac --help 2>&1 | grep -q -- '--release'; then
  JAVAC+=(--release 8)
else
  JAVAC+=(-source 1.8 -target 1.8)
fi
"${JAVAC[@]}" -Xlint:all,-options -Werror -d "$OUT" "${SOURCES[@]}"

java \
  -Dsawbot.fixture="$ROOT/build/phase2-snapshot-fixture.json" \
  -Dsawbot.telemetry.fixture="$ROOT/build/phase3-telemetry-fixture.sbt" \
  -Dsawbot.observation.fixture="$ROOT/build/phase5-observation-fixture.bin" \
  -cp "$OUT" dev.fivesaw.sawbot.verification.FoundationContractTest
java -cp "$OUT" dev.fivesaw.sawbot.verification.NavigationBodyContractTest
java -cp "$OUT" dev.fivesaw.sawbot.verification.SegmentedNavigationContractTest
java -cp "$OUT" dev.fivesaw.sawbot.verification.BridgingBodyContractTest

ROOT_FOR_PY="$ROOT" python3 - <<'PY'
from pathlib import Path
import json, os
root = Path(os.environ['ROOT_FOR_PY'])
fixture = root / 'build/phase2-snapshot-fixture.json'
data = json.loads(fixture.read_text(encoding='utf-8'))
assert data['exportFormat'] == 'sawbot.snapshot.debug/0.2'
assert data['schemaVersion'] == 'sawbot.observation/0.3'
assert len(data['localTerrain']['blockStateIds']) == 1521
assert len(data['midRangeMap']['relativeSurfaceY']) == 1089
assert len(data['inventory']['slots']) == 41
assert 'type' in data['entities'][0]
assert 'payloadItemCategory' in data['entities'][0]
print('PASS Phase 2 snapshot JSON parse and bounds check')
PY

ROOT_FOR_PY="$ROOT" python3 - <<'PY'
from pathlib import Path
import json, os, re
root = Path(os.environ['ROOT_FOR_PY'])
version = os.environ['SAWBOT_VERIFY_VERSION']
if version != '1.1.0-alpha.0':
    raise SystemExit(f'Phase 10 version mismatch: {version}')
properties = (root / 'gradle.properties').read_text(encoding='utf-8')
if f'sawbotVersion={version}' not in properties or 'loom.platform=forge' not in properties:
    raise SystemExit('gradle.properties version/platform metadata is inconsistent')
resource = root / 'sawbot-forge-1.8.9/src/main/resources/mcmod.info'
expanded = resource.read_text(encoding='utf-8').replace('${version}', version).replace('${mcversion}', '1.8.9')
metadata = json.loads(expanded)[0]
assert metadata['modid'] == 'sawbotv1'
assert metadata['version'] == version
assert metadata['mcversion'] == '1.8.9'

required = [
    '.github/workflows/ci.yml', '.github/workflows/release.yml',
    'tools/package-release.sh', 'tools/verify-release-payload.sh',
    'tools/verify-built-jar.py', 'docs/PHASE10_REPORT.md',
    'docs/CONTINUOUS_ANYTIME_NAVIGATION.md',
    'docs/PHASE9_REPORT.md', 'docs/SEGMENTED_NAVIGATION_CORE.md',
    'docs/BARITONE_ARCHITECTURE_RESEARCH.md', 'docs/NAVIGATION_BODY.md',
    'docs/HYBRID_ARCHITECTURE.md', 'docs/PHASE8_REPORT.md',
    'docs/BRIDGING_BODY.md', 'docs/GITHUB_RELEASES.md',
    'verification-tests/src/dev/fivesaw/sawbot/verification/SegmentedNavigationContractTest.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/ImmutableNavigationGrid.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/AnytimeMovementSearch.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/SearchDebugEdge.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementAStarPlanner.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementPath.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementPlanResult.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationMovement.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationMovementType.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationProgressWatchdog.java',
    'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/PathSegmentCoordinator.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationPlannerWorker.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationSnapshotCapture.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationMovementExecutor.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationCameraController.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/WorldNavigationGrid.java',
    'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationBodyController.java',
]
missing = [item for item in required if not (root / item).is_file()]
if missing:
    raise SystemExit('Missing Phase 10 repository files: ' + ', '.join(missing))

ci = (root / '.github/workflows/ci.yml').read_text(encoding='utf-8')
manual = (root / '.github/workflows/release.yml').read_text(encoding='utf-8')
for text, label in ((ci, 'CI'), (manual, 'manual release')):
    if '\t' in text:
        raise SystemExit(f'{label} workflow contains tabs')
    for token in ('PHASE10_REPORT.md', 'CONTINUOUS_ANYTIME_NAVIGATION.md',
                  'BARITONE_ARCHITECTURE_RESEARCH.md'):
        if token not in text:
            raise SystemExit(f'{label} workflow missing {token}')
for token in ('SegmentedNavigationContractTest', 'automatic-release:',
              "github.ref == 'refs/heads/main'", 'actions/download-artifact@',
              'tools/verify-release-payload.sh', 'gh release create'):
    if token not in ci:
        raise SystemExit('CI missing token: ' + token)
if 'workflow_dispatch:' not in manual or 'Manual Release Recovery' not in manual:
    raise SystemExit('Manual release recovery workflow is malformed')

planner = (root / 'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/AnytimeMovementSearch.java').read_text(encoding='utf-8')
worker = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationPlannerWorker.java').read_text(encoding='utf-8')
capture = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationSnapshotCapture.java').read_text(encoding='utf-8')
coordinator = (root / 'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/PathSegmentCoordinator.java').read_text(encoding='utf-8')
executor = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationMovementExecutor.java').read_text(encoding='utf-8')
body = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationBodyController.java').read_text(encoding='utf-8')
grid = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/WorldNavigationGrid.java').read_text(encoding='utf-8')
renderer = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java').read_text(encoding='utf-8')
client = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/client/ClientRuntime.java').read_text(encoding='utf-8')

for token in ('PriorityQueue', 'maximumExpandedNodes', 'heuristicWeight',
              'NavigationMovementType.ASCEND', 'NavigationMovementType.DESCEND',
              'BASE_TURN_PENALTY', 'traversalPenalty', 'NodeKey',
              'SearchDebugEdge', 'MIN_PUBLISH_EXPANSIONS', 'requestId'):
    if token not in planner:
        raise SystemExit('Anytime movement search missing: ' + token)
for token in ('ArrayBlockingQueue<PlanRequest>(1)',
              'ArrayBlockingQueue<PlanEnvelope>(6)', 'setDaemon(true)',
              'AnytimeMovementSearch', 'EXPANSIONS_PER_SLICE = 48',
              'latestSubmittedId', 'streamedUpdates', 'debugEdges',
              'superseded', 'shutdown()', 'worker.join'):
    if token not in worker:
        raise SystemExit('Planner worker missing: ' + token)
if 'net.minecraft' in worker:
    raise SystemExit('Planner worker must not import Minecraft classes')
for token in ('LOCAL_CAPTURE', 'FULL_CAPTURE', 'takeLocalRequest',
              'takeFullRequest', 'localRequestId', 'fullRequestId',
              'ImmutableNavigationGrid.Builder', 'maximumCells'):
    if token not in capture:
        raise SystemExit('Snapshot capture missing: ' + token)
for token in ('staged', 'trySplice', 'reconcileNearby', 'rewinds', 'skips',
              'corridorRecoveries'):
    if token not in coordinator:
        raise SystemExit('Path coordinator missing: ' + token)
for token in ('selectAimPoint', 'movement operation timeout', 'restorePhysical',
              'sameHeading', 'isCorridorSafe', 'maximumTurnDegreesPerTick',
              'ascend is not a legal one-block transition'):
    if token not in executor:
        raise SystemExit('Movement executor missing: ' + token)
if 'player.isCollidedHorizontally || recovery' in executor:
    raise SystemExit('Movement executor still performs blind collision jumps')
for token in ('NavigationPlannerWorker', 'NavigationSnapshotCapture',
              'PathSegmentCoordinator', 'buildDirectMicroPath', 'reconcileNearby',
              'rememberRollingGrid', 'FOLLOW+ANYTIME', 'rolling search from live position',
              'streamedPathUpdates', 'maybePlanAhead', 'validateMovementWindow',
              'stuckRecoveries', 'shutdown()'):
    if token not in body:
        raise SystemExit('Navigation body missing: ' + token)
for token in ('MAX_CACHE_ENTRIES', 'BoundedMap', 'refreshStandable',
              'validateMovementWindow', 'probeDirection', 'cacheHits'):
    if token not in grid:
        raise SystemExit('World grid missing: ' + token)
if 'state.inspectorVisible()' not in renderer or 'renderNavigationSearch()' not in renderer:
    raise SystemExit('Route/search rendering must be inspector-only')
if 'startIndex + 96' not in renderer or 'edges.size() - 384' not in renderer:
    raise SystemExit('Route/search rendering bounds are missing')
if 'navigationBody.shutdown()' not in client:
    raise SystemExit('Client runtime does not shut down navigation worker')

bridge = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/bridging/BridgingBodyController.java').read_text(encoding='utf-8')
for forbidden in ('C08PacketPlayerBlockPlacement', 'setPositionAndUpdate', 'teleport'):
    if forbidden in bridge:
        raise SystemExit('Bridging body contains forbidden mechanism: ' + forbidden)
for token in ('findPlacementTarget', 'lineOfSightMatches', 'onPlayerRightClick',
              'placementConfirmationTicks', 'restoreOriginalSlot'):
    if token not in bridge:
        raise SystemExit('Bridging body lost mechanism: ' + token)

architecture = (root / 'docs/HYBRID_ARCHITECTURE.md').read_text(encoding='utf-8').lower()
for token in ('learned brain', 'deterministic', 'specialist', 'never chooses'):
    if token not in architecture:
        raise SystemExit('Hybrid architecture missing boundary: ' + token)
research = (root / 'docs/BARITONE_ARCHITECTURE_RESEARCH.md').read_text(encoding='utf-8')
for token in ('cabaletta/baritone', '054092e44eec61f6ef3818a2b4b7c56df90daf76',
              'clean-room', 'baritone java source file was copied', 'LGPL-3.0'):
    if token.lower() not in research.lower():
        raise SystemExit('Architecture provenance missing: ' + token)

for script in ('tools/package-release.sh', 'tools/verify-release-payload.sh'):
    text = (root / script).read_text(encoding='utf-8')
    for token in ('PHASE10_REPORT.md', 'CONTINUOUS_ANYTIME_NAVIGATION.md',
                  'BARITONE_ARCHITECTURE_RESEARCH.md'):
        if token not in text:
            raise SystemExit(f'{script} missing {token}')
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
for token in ('ImmutableNavigationGrid.class', 'AnytimeMovementSearch.class',
              'SearchDebugEdge.class', 'MovementAStarPlanner.class',
              'PathSegmentCoordinator.class', 'NavigationPlannerWorker.class',
              'NavigationSnapshotCapture.class', 'NavigationMovementExecutor.class'):
    if token not in verifier:
        raise SystemExit('JAR verifier missing ' + token)
print('PASS Phase 10 repository, architecture, performance, and safety checks')
PY

python3 "$ROOT/sawbot-tools/dataset-validator/validate_telemetry.py" \
  "$ROOT/build/phase3-telemetry-fixture.sbt" --json \
  > "$ROOT/build/phase3-telemetry-report.json"
ROOT_FOR_PY="$ROOT" python3 - <<'PY'
from pathlib import Path
import json, os, subprocess, sys
root = Path(os.environ['ROOT_FOR_PY'])
report = json.loads((root / 'build/phase3-telemetry-report.json').read_text(encoding='utf-8'))
assert report['complete'] is True
assert report['telemetry_schema'] == 'sawbot.telemetry/0.1'
assert report['observation_schema'] == 'sawbot.observation/0.3'
assert report['step_count'] >= 1
source = root / 'build/phase3-telemetry-fixture.sbt'
truncated = root / 'build/phase3-telemetry-truncated.sbt.partial'
truncated.write_bytes(source.read_bytes()[:-20])
result = subprocess.run([
    sys.executable, str(root / 'sawbot-tools/dataset-validator/validate_telemetry.py'),
    str(truncated), '--recover', '--json'], capture_output=True, text=True)
if result.returncode != 0:
    raise SystemExit(result.stdout + '\n' + result.stderr)
recovered = Path(json.loads(result.stdout)['recovered_path'])
result = subprocess.run([
    sys.executable, str(root / 'sawbot-tools/dataset-validator/validate_telemetry.py'),
    str(recovered), '--summary-only'], capture_output=True, text=True)
if result.returncode != 0:
    raise SystemExit(result.stdout + '\n' + result.stderr)
print('PASS telemetry framing, CRC, replay data, and recovery')
PY

python3 "$ROOT/sawbot-tools/replay-inspector/inspect_telemetry.py" \
  "$ROOT/build/phase3-telemetry-fixture.sbt" --limit 2 \
  > "$ROOT/build/phase3-replay-summary.txt"
python3 "$ROOT/sawbot-tools/dummy-model/dummy_model.py" --self-test
python3 "$ROOT/sawbot-trainer/waypoint/verify_phase5.py"

# Build a synthetic JAR from the exact lint-clean offline classes, then exercise the
# same JAR verifier, release packager, SHA-256 manifest, and payload verifier used by CI.
STAGE="$ROOT/build/offline-jar-stage"
LIBS="$ROOT/sawbot-forge-1.8.9/build/libs"
rm -rf "$STAGE" "$LIBS"
mkdir -p "$STAGE" "$LIBS"
cp -R "$OUT"/* "$STAGE/"
ROOT_FOR_PY="$ROOT" python3 - <<'PY'
from pathlib import Path
import os
root = Path(os.environ['ROOT_FOR_PY'])
version = os.environ['SAWBOT_VERIFY_VERSION']
text = (root / 'sawbot-forge-1.8.9/src/main/resources/mcmod.info').read_text(encoding='utf-8')
text = text.replace('${version}', version).replace('${mcversion}', '1.8.9')
(root / 'build/offline-jar-stage/mcmod.info').write_text(text, encoding='utf-8')
PY
(
  cd "$STAGE"
  jar cf "$LIBS/SawBotV1-$VERSION-mc1.8.9.jar" .
)
printf 'offline source fixture\n' > "$ROOT/build/OFFLINE_SOURCE.txt"
(
  cd "$ROOT/build"
  jar cf "$LIBS/SawBotV1-$VERSION-sources.jar" OFFLINE_SOURCE.txt
)
python3 "$ROOT/tools/verify-built-jar.py" \
  "$LIBS/SawBotV1-$VERSION-mc1.8.9.jar" --expected-version "$VERSION"
bash "$ROOT/tools/package-release.sh" "$VERSION" >/dev/null
bash "$ROOT/tools/verify-release-payload.sh" "$VERSION" "$ROOT/dist" >/dev/null
printf '%s\n' 'PASS synthetic JAR, Phase 10 release packaging, hashes, and payload verification'
printf '%s\n' 'PASS offline Phase 10 continuous anytime navigation verification'
