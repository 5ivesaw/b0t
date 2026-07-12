#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/offline-classes"
VERSION="$(bash "$ROOT/tools/read-version.sh")"
export SAWBOT_VERIFY_VERSION="$VERSION"
rm -rf "$OUT"
mkdir -p "$OUT"
mapfile -t SOURCES < <(find "$ROOT/verification-stubs/src" "$ROOT/sawbot-common/src/main/java" "$ROOT/sawbot-forge-1.8.9/src/main/java" "$ROOT/verification-tests/src" -name '*.java' -print | sort)
JAVAC=(javac)
if javac --help 2>&1 | grep -q -- '--release'; then JAVAC+=(--release 8); else JAVAC+=(-source 1.8 -target 1.8); fi
"${JAVAC[@]}" -Xlint:all,-options -Werror -d "$OUT" "${SOURCES[@]}"
java -Dsawbot.fixture="$ROOT/build/phase2-snapshot-fixture.json" -Dsawbot.telemetry.fixture="$ROOT/build/phase3-telemetry-fixture.sbt" -Dsawbot.observation.fixture="$ROOT/build/phase5-observation-fixture.bin" -cp "$OUT" dev.fivesaw.sawbot.verification.FoundationContractTest
java -cp "$OUT" dev.fivesaw.sawbot.verification.NavigationBodyContractTest
java -cp "$OUT" dev.fivesaw.sawbot.verification.BridgingBodyContractTest

ROOT_FOR_PY="$ROOT" python3 - <<'PYJSONFIXTURE'
from pathlib import Path
import json, os
root=Path(os.environ['ROOT_FOR_PY'])
fixture=root/'build/phase2-snapshot-fixture.json'
data=json.loads(fixture.read_text(encoding='utf-8'))
assert data['exportFormat']=='sawbot.snapshot.debug/0.2'
assert len(data['localTerrain']['blockStateIds'])==1521
assert len(data['midRangeMap']['relativeSurfaceY'])==1089
assert len(data['inventory']['slots'])==41
assert data['schemaVersion']=='sawbot.observation/0.3'
assert 'type' in data['entities'][0]
assert 'payloadItemCategory' in data['entities'][0]
print('PASS Phase 2 snapshot JSON parse and bounds check')
PYJSONFIXTURE

ROOT_FOR_PY="$ROOT" python3 - <<'PYVALIDATEJSON'
from pathlib import Path
import json, os
root=Path(os.environ['ROOT_FOR_PY'])
resource=root/'sawbot-forge-1.8.9/src/main/resources/mcmod.info'
version=os.environ['SAWBOT_VERIFY_VERSION']
text=resource.read_text(encoding='utf-8').replace('${version}',version).replace('${mcversion}','1.8.9')
json.loads(text)
print('PASS mcmod.info JSON expansion check')
PYVALIDATEJSON

ROOT_FOR_PY="$ROOT" python3 - <<'PYREPOCHECK'
from pathlib import Path
import json, os
root = Path(os.environ['ROOT_FOR_PY'])
required = [
    root / '.github/workflows/ci.yml',
    root / '.github/workflows/release.yml',
    root / 'tools/package-release.sh',
    root / 'tools/read-version.sh',
    root / 'tools/verify-release-payload.sh',
    root / 'tools/verify-built-jar.py',
    root / 'docs/GITHUB_RELEASES.md',
    root / 'docs/PHASE8_REPORT.md',
    root / 'docs/BRIDGING_BODY.md',
    root / 'docs/PHASE7_REPORT.md',
    root / 'docs/ADAPTIVE_NAVIGATION.md',
    root / 'docs/PHASE6_REPORT.md',
    root / 'docs/HYBRID_ARCHITECTURE.md',
    root / 'docs/NAVIGATION_BODY.md',
    root / 'docs/PHASE5_REPORT.md',
    root / 'docs/WAYPOINT_MODEL.md',
    root / 'docs/PHASE4_RUNTIME_FINDINGS.md',
    root / 'docs/PHASE4_REPORT.md',
    root / 'docs/MODEL_BRIDGE_PROTOCOL.md',
    root / 'docs/PHASE3_RUNTIME_STATUS.md',
    root / 'docs/PHASE3_REPORT.md',
    root / 'docs/TELEMETRY_FORMAT.md',
    root / 'docs/PHASE2_ACCEPTANCE.md',
    root / 'docs/PHASE1_ACCEPTANCE.md',
    root / 'docs/PHASE0_ACCEPTANCE.md',
    root / 'sawbot-tools/dataset-validator/validate_telemetry.py',
    root / 'sawbot-tools/replay-inspector/inspect_telemetry.py',
    root / 'sawbot-tools/dummy-model/dummy_model.py',
    root / 'sawbot-trainer/waypoint/verify_phase5.py',
    root / 'sawbot-trainer/waypoint/waypoint_model.py',
    root / 'sawbot-trainer/waypoint/checkpoints/waypoint_v0.1.json',
    root / 'sawbot-trainer/waypoint/datasets/teacher_waypoint_v0.1.jsonl.gz',
    root / 'sawbot-trainer/waypoint/evaluation/waypoint_eval_v0.1.json',
    root / 'sawbot-trainer/waypoint/evaluation/waypoint_failures_v0.1.jsonl',
    root / 'verification-tests/src/dev/fivesaw/sawbot/verification/NavigationBodyContractTest.java',
    root / 'verification-tests/src/dev/fivesaw/sawbot/verification/BridgingBodyContractTest.java',
    root / 'tools/test-latest-telemetry.ps1',
    root / 'tools/TEST-LATEST-TELEMETRY.bat',
    root / 'GITHUB_UPLOAD_QUICKSTART.md',
]
missing = [str(path.relative_to(root)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('Missing Phase 8 repository files: ' + ', '.join(missing))
for workflow in required[:2]:
    text = workflow.read_text(encoding='utf-8')
    if '\t' in text:
        raise SystemExit(f'Tab character found in workflow: {workflow}')
    if ('OWNER' + '/REPOSITORY') in text:
        raise SystemExit(f'Placeholder repository path found in workflow: {workflow}')
ci_text = (root / '.github/workflows/ci.yml').read_text(encoding='utf-8')
release_text = (root / '.github/workflows/release.yml').read_text(encoding='utf-8')
for token in (
    'automatic-release:', "github.ref == 'refs/heads/main'",
    'needs: [metadata, offline-verification, forge-build]',
    'actions/download-artifact@', 'tools/verify-release-payload.sh',
    'PHASE8_REPORT.md', 'BRIDGING_BODY.md', 'PHASE7_REPORT.md',
    'ADAPTIVE_NAVIGATION.md', 'PHASE6_REPORT.md',
    'HYBRID_ARCHITECTURE.md', 'NAVIGATION_BODY.md',
    'PHASE5_REPORT.md', 'WAYPOINT_MODEL.md', 'waypoint_v0.1.json',
    'waypoint_eval_v0.1.json', 'verify_phase5.py',
    'NavigationBodyContractTest', 'BridgingBodyContractTest',
    'gh release create', 'contents: write',
    'dummy_model.py --self-test'
):
    if token not in ci_text:
        raise SystemExit('CI missing Phase 8 automatic-release token: ' + token)
if "tags:\n      - 'v*'" in release_text:
    raise SystemExit('Manual recovery workflow must not be tag-triggered')
if 'workflow_dispatch:' not in release_text or 'Manual Release Recovery' not in release_text:
    raise SystemExit('Manual release recovery workflow is missing or malformed')
for token in ('PHASE8_REPORT.md', 'BRIDGING_BODY.md', 'PHASE7_REPORT.md',
              'ADAPTIVE_NAVIGATION.md', 'PHASE6_REPORT.md',
              'HYBRID_ARCHITECTURE.md', 'NAVIGATION_BODY.md',
              'PHASE5_REPORT.md', 'WAYPOINT_MODEL.md', 'waypoint_v0.1.json',
              'waypoint_eval_v0.1.json'):
    if token not in release_text:
        raise SystemExit('Manual release workflow missing Phase 8 asset: ' + token)
properties = (root / 'gradle.properties').read_text(encoding='utf-8')
if 'loom.platform=forge' not in properties:
    raise SystemExit('gradle.properties does not declare loom.platform=forge')
version = os.environ['SAWBOT_VERIFY_VERSION']
if f'sawbotVersion={version}' not in properties or version != '0.9.0-alpha.0':
    raise SystemExit('Phase 8 version metadata is inconsistent')
package_script = (root / 'tools/package-release.sh').read_text(encoding='utf-8')
for token in ('PHASE8_REPORT.md','BRIDGING_BODY.md','PHASE7_REPORT.md',
              'ADAPTIVE_NAVIGATION.md','PHASE6_REPORT.md',
              'HYBRID_ARCHITECTURE.md','NAVIGATION_BODY.md',
              'PHASE5_REPORT.md','WAYPOINT_MODEL.md','waypoint_v0.1.json',
              'waypoint_eval_v0.1.json','SHA256SUMS.txt'):
    if token not in package_script:
        raise SystemExit('Release packager missing ' + token)
payload_verifier = (root / 'tools/verify-release-payload.sh').read_text(encoding='utf-8')
for token in ('PHASE8_REPORT.md','BRIDGING_BODY.md','PHASE7_REPORT.md',
              'ADAPTIVE_NAVIGATION.md','PHASE6_REPORT.md',
              'HYBRID_ARCHITECTURE.md','NAVIGATION_BODY.md',
              'PASS verified Phase 8 release payload'):
    if token not in payload_verifier:
        raise SystemExit('Release payload verifier missing ' + token)
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
for token in (
    'dev/fivesaw/sawbot/common/navigation/IncrementalAStarPlanner.class',
    'dev/fivesaw/sawbot/common/navigation/AdaptivePathCursor.class',
    'dev/fivesaw/sawbot/common/navigation/NavigationPath.class',
    'dev/fivesaw/sawbot/forge/navigation/WorldNavigationGrid.class',
    'dev/fivesaw/sawbot/forge/navigation/NavigationBodyController.class',
    'dev/fivesaw/sawbot/forge/model/ModelBridge.class',
    'dev/fivesaw/sawbot/forge/actuator/SafeActionActuator.class',
    'dev/fivesaw/sawbot/forge/map/NavigationWaypointController.class',
    'dev/fivesaw/sawbot/forge/telemetry/TelemetryService.class',
    'dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class',
    'dev/fivesaw/sawbot/common/bridging/BridgeCorridorPlanner.class',
    'dev/fivesaw/sawbot/common/bridging/BridgePlacementStep.class',
    'dev/fivesaw/sawbot/forge/bridging/BridgingBodyController.class',
):
    if token not in verifier:
        raise SystemExit('Release verifier missing Phase 8 class: ' + token)
renderer = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java').read_text(encoding='utf-8')
style = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.java').read_text(encoding='utf-8')
if ('EntityVisualStyle.visibilityRgb(entity)' not in renderer
    or 'EntityVisualStyle.visibilityArgb(entity)' not in renderer
    or 'EntityVisualStyle.SELECTED_ACCENT_RGB' not in renderer):
    raise SystemExit('World renderer does not use immediate visibility style consistently')
if ('LOS_RGB = 0x55FF55' not in style or 'OCCLUDED_RGB = 0xAA55FF' not in style):
    raise SystemExit('Visibility style colours changed without acceptance update')
if ('GL11.glPushAttrib' in renderer or 'GL11.glPopAttrib' in renderer):
    raise SystemExit('Raw GL attribute stack returned; it can desynchronise GlStateManager')
for token in ('restoreState()', 'FROZEN SNAPSHOT #', '255, 255, 85, 255',
              'USER_WAYPOINT_ID', 'navigationBody.pathCells()', 'renderNavigationPath',
              'bridgingBody.planSteps()', 'renderBridgePlan', 'drawLine'):
    if token not in renderer:
        raise SystemExit('World renderer missing state/route token: ' + token)
client = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/client/ClientRuntime.java').read_text(encoding='utf-8')
for token in ('modelBridge.offerObservation', 'modelBridge.pollLatestAction',
              'navigationBody.observeBrainAction', 'navigationBody.tick',
              'navigation body priority', 'physicalInput.hasTakeoverInput',
              'physicalInput.arm()', 'NavigationWaypointController', 'setFromCrosshair',
              'NAV ENABLED: deterministic body', 'navigationBody.release("emergency stop")',
              'config.navigationLookaheadNodes()', 'config.navigationReactiveProbeDistance()',
              'bridgingBody.observeBrainAction', 'bridgingBody.shouldOwnBridge',
              'bridgingBody.tick', 'toggleBridgeIntent', 'bridging body priority',
              'bridgingBody.release("emergency stop")'):
    if token not in client:
        raise SystemExit('Client runtime missing Phase 8 integration token: ' + token)
navigation = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationBodyController.java').read_text(encoding='utf-8')
planner = (root / 'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/IncrementalAStarPlanner.java').read_text(encoding='utf-8')
grid = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/WorldNavigationGrid.java').read_text(encoding='utf-8')
cursor = (root / 'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/AdaptivePathCursor.java').read_text(encoding='utf-8')
for token in ('planner.step(expansionsPerTick)', 'applyMovement', 'maximumTurnDegreesPerTick',
              'stuckRecoveries', 'startPlan', 'arrivalTicks', 'restorePhysical',
              'reanchorToCurrentPosition', 'maybeStartRollingReplan', 'chooseSteering',
              'FOLLOW+REPLAN', 'validatePathWindow', 'bestEffortPath'):
    if token not in navigation:
        raise SystemExit('Adaptive navigation body missing deterministic mechanism: ' + token)
for token in ('PriorityQueue', 'maximumExpandedNodes', 'horizontalRadius',
              'verticalRadius', 'diagonalClear', 'NavigationPlanState.SEARCHING',
              'bestEffortPath', 'bestFrontier', 'traversalPenalty', 'TURN_PENALTY'):
    if token not in planner:
        raise SystemExit('Anytime planner missing bounded mechanism: ' + token)
for token in ('MAX_CACHE_ENTRIES', 'isStandable', 'FLAG_LIQUID', 'FLAG_HAZARD',
              'FLAG_SAFE_SUPPORT', 'nearestStandable', 'refreshStandable',
              'validatePathWindow', 'isCorridorSafe', 'probeDirection', 'traversalPenalty'):
    if token not in grid:
        raise SystemExit('Live world navigation grid missing mechanism: ' + token)
for token in ('project', 'farthestSafeLookahead', 'nearestHorizontalDistanceSquared',
              'maximumBacktrackNodes', 'maximumForwardNodes'):
    if token not in cursor:
        raise SystemExit('Adaptive path cursor missing corridor mechanism: ' + token)
if 'new Thread' in navigation or 'new Thread' in grid:
    raise SystemExit('Navigation body/grid must remain client-thread-only')
bridge_body = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/bridging/BridgingBodyController.java').read_text(encoding='utf-8')
bridge_planner = (root / 'sawbot-common/src/main/java/dev/fivesaw/sawbot/common/bridging/BridgeCorridorPlanner.java').read_text(encoding='utf-8')
bridge_doc = (root / 'docs/BRIDGING_BODY.md').read_text(encoding='utf-8')
for token in ('shouldOwnBridge', 'findPlacementTarget', 'lineOfSightMatches',
              'onPlayerRightClick', 'placementConfirmationTicks',
              'maximumPlacementAttempts', 'restoreOriginalSlot',
              'InputRelease.restorePhysical', 'pendingPlacementSupport',
              'moveOntoSupportedCell', 'OUT_OF_BLOCKS', 'AIM_BLOCKED'):
    if token not in bridge_body:
        raise SystemExit('Bridging body missing legal bounded mechanism: ' + token)
for token in ('maximumSteps', 'BridgePlacementStep', 'BridgeDirection',
              'Math.abs(targetX - x)', 'Math.abs(targetZ - z)', 'steps.size() < maximumSteps'):
    if token not in bridge_planner:
        raise SystemExit('Bridge corridor planner missing bounded mechanism: ' + token)
for forbidden in ('sendQueue', 'addToSendQueue', 'C08PacketPlayerBlockPlacement',
                  'setPositionAndUpdate', 'teleport'):
    if forbidden in bridge_body:
        raise SystemExit('Bridging body contains forbidden mechanism: ' + forbidden)
for token in ('normal client reach', 'ray-trace', 'world-state placement confirmation',
              'public-server automation', 'Only one controller owns'):
    if token.lower() not in bridge_doc.lower():
        raise SystemExit('Bridging body documentation missing boundary: ' + token)
actuator = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/actuator/SafeActionActuator.java').read_text(encoding='utf-8')
input_release = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/safety/InputRelease.java').read_text(encoding='utf-8')
for token in ('KeyBinding.setKeyBindState', 'KeyBinding.onTick', 'maximumActionAgeNanos',
              'ActionContextValidator.validate', 'ownsContinuousInputs', 'releaseIfOwned'):
    if token not in actuator:
        raise SystemExit('Safe actuator missing required mechanism: ' + token)
for token in ('Keyboard.isKeyDown', 'Mouse.isButtonDown', 'restorePhysical'):
    if token not in input_release:
        raise SystemExit('Physical input restoration is incomplete: ' + token)
if 'InputRelease.releaseAll(minecraft);' not in actuator:
    raise SystemExit('Safe actuator lost complete emergency/block release path')
bridge = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/model/ModelBridge.java').read_text(encoding='utf-8')
for token in ('ArrayBlockingQueue', 'setDaemon(true)', 'socket.setSoTimeout(100)',
              'offerObservation', 'pollLatestAction', 'displayState()', 'return "OFFLINE"'):
    if token not in bridge:
        raise SystemExit('Model bridge missing bounded/stable mechanism: ' + token)
telemetry = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/telemetry/TelemetryService.java').read_text(encoding='utf-8')
session = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/telemetry/TelemetrySession.java').read_text(encoding='utf-8')
if ('failureLatched' not in telemetry or 'prepareRetry()' not in telemetry
    or 'encodingRejectedSteps' not in session or 'capture remains active' not in session):
    raise SystemExit('Preserved telemetry retry/error isolation is missing')
if 'worker.interrupt();' in session:
    raise SystemExit('Normal telemetry close must not interrupt an active NIO writer')
waypoint = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/map/NavigationWaypointController.java').read_text(encoding='utf-8')
landmarks = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/map/LandmarkSensor.java').read_text(encoding='utf-8')
if ('USER_WAYPOINT_ID = 1000' not in waypoint or 'setWorldTarget' not in waypoint
    or 'LandmarkType.STAGING_AREA' not in waypoint or 'navigationWaypoint.capture' not in landmarks):
    raise SystemExit('Semantic waypoint observation/controller is incomplete')
model = (root / 'sawbot-trainer/waypoint/waypoint_model.py').read_text(encoding='utf-8')
for forbidden in ('net.minecraft', 'pathfind', 'teacher_action('):
    if forbidden in model:
        raise SystemExit('Historical live waypoint model contains forbidden runtime dependency: ' + forbidden)
if 'TinyMlp' not in model or 'features_from_observation' not in model:
    raise SystemExit('Historical Phase 5 learned baseline is incomplete')
evaluation = json.loads((root / 'sawbot-trainer/waypoint/evaluation/waypoint_eval_v0.1.json').read_text(encoding='utf-8'))
if evaluation.get('runtimePathfinder') is not False:
    raise SystemExit('Historical evaluation incorrectly claims a runtime pathfinder')
if evaluation['model']['successRate'] < 0.8 or evaluation['model']['successRate'] <= evaluation['randomBaseline']['successRate']:
    raise SystemExit('Historical waypoint evaluation does not meet its preserved gate')
contract = (root / 'docs/ACTION_CONTRACT.md').read_text(encoding='utf-8')
protocol = (root / 'docs/MODEL_BRIDGE_PROTOCOL.md').read_text(encoding='utf-8')
architecture = (root / 'docs/HYBRID_ARCHITECTURE.md').read_text(encoding='utf-8')
if 'sawbot.action/0.1' not in contract or 'local receive timestamp' not in contract:
    raise SystemExit('Action Contract is not synchronized')
if 'sawbot.bridge/0.1' not in protocol or '262,144' not in protocol or 'client thread' not in protocol:
    raise SystemExit('Bridge protocol documentation is incomplete')
adaptive = (root / 'docs/ADAPTIVE_NAVIGATION.md').read_text(encoding='utf-8')
for token in ('learned brain', 'deterministic', 'specialist', 'never chooses'):
    if token.lower() not in architecture.lower():
        raise SystemExit('Hybrid architecture report missing boundary: ' + token)
for token in ('anytime', 'rolling replanning', 'route corridor', 're-anchor',
              '20 Hz reactive steering', 'live world invalidation'):
    if token.lower() not in adaptive.lower():
        raise SystemExit('Adaptive navigation report missing mechanism: ' + token)
print('PASS GitHub repository and Phase 8 navigation/bridging packaging check')
PYREPOCHECK

python3 "$ROOT/sawbot-tools/dataset-validator/validate_telemetry.py" "$ROOT/build/phase3-telemetry-fixture.sbt" --json > "$ROOT/build/phase3-telemetry-report.json"
ROOT_FOR_PY="$ROOT" python3 - <<'PYTELEMETRY'
from pathlib import Path
import json, os, subprocess, sys
root=Path(os.environ['ROOT_FOR_PY'])
report=json.loads((root/'build/phase3-telemetry-report.json').read_text(encoding='utf-8'))
assert report['complete'] is True
assert report['telemetry_schema']=='sawbot.telemetry/0.1'
assert report['observation_schema']=='sawbot.observation/0.3'
assert report['step_count']>=1
step=report['steps'][0]
assert step['input_samples']==1
assert step['mouse_delta_x']==6 and step['mouse_delta_y']==-4
assert step['key_or'] & 1
source=root/'build/phase3-telemetry-fixture.sbt'
truncated=root/'build/phase3-telemetry-truncated.sbt.partial'
data=source.read_bytes()
truncated.write_bytes(data[:-20])
result=subprocess.run([sys.executable,str(root/'sawbot-tools/dataset-validator/validate_telemetry.py'),str(truncated),'--recover','--json'],capture_output=True,text=True)
if result.returncode!=0:
    raise SystemExit(result.stdout+'\n'+result.stderr)
recovery=json.loads(result.stdout)
recovered=Path(recovery['recovered_path'])
result2=subprocess.run([sys.executable,str(root/'sawbot-tools/dataset-validator/validate_telemetry.py'),str(recovered),'--summary-only'],capture_output=True,text=True)
if result2.returncode!=0:
    raise SystemExit(result2.stdout+'\n'+result2.stderr)
print('PASS Phase 3 telemetry framing, input alignment, CRC, and recovery')
PYTELEMETRY

python3 "$ROOT/sawbot-tools/replay-inspector/inspect_telemetry.py" "$ROOT/build/phase3-telemetry-fixture.sbt" --limit 2 > "$ROOT/build/phase3-replay-summary.txt"
python3 "$ROOT/sawbot-tools/dummy-model/dummy_model.py" --self-test
python3 "$ROOT/sawbot-trainer/waypoint/verify_phase5.py"
printf '%s\n' 'PASS offline Phase 8 navigation and bridging verification'
