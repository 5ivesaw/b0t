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
java -Dsawbot.fixture="$ROOT/build/phase2-snapshot-fixture.json" -Dsawbot.telemetry.fixture="$ROOT/build/phase3-telemetry-fixture.sbt" -cp "$OUT" dev.fivesaw.sawbot.verification.FoundationContractTest

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
import os
root = Path(os.environ['ROOT_FOR_PY'])
required = [
    root / '.github/workflows/ci.yml',
    root / '.github/workflows/release.yml',
    root / 'tools/package-release.sh',
    root / 'tools/read-version.sh',
    root / 'tools/verify-release-payload.sh',
    root / 'tools/verify-built-jar.py',
    root / 'docs/GITHUB_RELEASES.md',
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
    root / 'sawbot-tools/dummy-model/RUN-DUMMY-MODEL.bat',
    root / 'sawbot-tools/dummy-model/RUN-ACTUATOR-DEMO.bat',
    root / 'tools/test-latest-telemetry.ps1',
    root / 'tools/TEST-LATEST-TELEMETRY.bat',
    root / 'GITHUB_UPLOAD_QUICKSTART.md',
]
missing = [str(path.relative_to(root)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('Missing Phase 4 packaging files: ' + ', '.join(missing))
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
    'PHASE4_REPORT.md', 'MODEL_BRIDGE_PROTOCOL.md', 'PHASE3_RUNTIME_STATUS.md',
    'gh release create', 'contents: write', 'dummy_model.py --self-test'
):
    if token not in ci_text:
        raise SystemExit('CI missing Phase 4 automatic-release token: ' + token)
if "tags:\n      - 'v*'" in release_text:
    raise SystemExit('Manual recovery workflow must not be tag-triggered')
if 'workflow_dispatch:' not in release_text or 'Manual Release Recovery' not in release_text:
    raise SystemExit('Manual release recovery workflow is missing or malformed')
properties = (root / 'gradle.properties').read_text(encoding='utf-8')
if 'loom.platform=forge' not in properties:
    raise SystemExit('gradle.properties does not declare loom.platform=forge')
version = os.environ['SAWBOT_VERIFY_VERSION']
if f'sawbotVersion={version}' not in properties or version != '0.5.0-alpha.0':
    raise SystemExit('Phase 4 version metadata is inconsistent')
package_script = (root / 'tools/package-release.sh').read_text(encoding='utf-8')
for token in ('PHASE4_REPORT.md','MODEL_BRIDGE_PROTOCOL.md','PHASE3_RUNTIME_STATUS.md','SHA256SUMS.txt'):
    if token not in package_script:
        raise SystemExit('Release packager missing ' + token)
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
for token in (
    'dev/fivesaw/sawbot/forge/model/ModelBridge.class',
    'dev/fivesaw/sawbot/forge/model/ModelProtocol.class',
    'dev/fivesaw/sawbot/forge/actuator/SafeActionActuator.class',
    'dev/fivesaw/sawbot/forge/actuator/EnvironmentGuard.class',
    'dev/fivesaw/sawbot/forge/telemetry/TelemetryService.class',
    'dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class',
):
    if token not in verifier:
        raise SystemExit('Release verifier missing Phase 4 class: ' + token)
renderer = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java').read_text(encoding='utf-8')
style = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.java').read_text(encoding='utf-8')
if ('EntityVisualStyle.visibilityRgb(entity)' not in renderer
    or 'EntityVisualStyle.visibilityArgb(entity)' not in renderer
    or 'EntityVisualStyle.SELECTED_ACCENT_RGB' not in renderer):
    raise SystemExit('World renderer does not use immediate visibility style consistently')
if ('LOS_RGB = 0x55FF55' not in style or 'OCCLUDED_RGB = 0xAA55FF' not in style):
    raise SystemExit('Visibility style colours changed without acceptance update')
if ('GL11.glPushAttrib' in renderer or 'GL11.glPopAttrib' in renderer):
    raise SystemExit('Raw GL attribute stack returned; it can desynchronise GlStateManager and tint the HUD')
for token in ('restoreState()', 'FROZEN SNAPSHOT #', '255, 255, 85, 255'):
    if token not in renderer:
        raise SystemExit('World renderer missing state restoration/frozen/yellow-selection token: ' + token)
client = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/client/ClientRuntime.java').read_text(encoding='utf-8')
for token in ('modelBridge.offerObservation', 'modelBridge.pollLatestAction', 'physicalInput.hasTakeoverInput', 'disableAndRelease("model disconnected")'):
    if token not in client:
        raise SystemExit('Client runtime missing Phase 4 safety/integration token: ' + token)
actuator = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/actuator/SafeActionActuator.java').read_text(encoding='utf-8')
for token in ('KeyBinding.setKeyBindState', 'KeyBinding.onTick', 'maximumActionAgeNanos', 'ActionContextValidator.validate', 'InputRelease.releaseAll'):
    if token not in actuator:
        raise SystemExit('Safe actuator missing required mechanism: ' + token)
bridge = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/model/ModelBridge.java').read_text(encoding='utf-8')
for token in ('ArrayBlockingQueue', 'setDaemon(true)', 'socket.setSoTimeout(100)', 'offerObservation', 'pollLatestAction'):
    if token not in bridge:
        raise SystemExit('Model bridge missing bounded/non-blocking mechanism: ' + token)
telemetry = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/telemetry/TelemetryService.java').read_text(encoding='utf-8')
session = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/telemetry/TelemetrySession.java').read_text(encoding='utf-8')
if ('failureLatched' not in telemetry or 'lastFailureMessage' not in telemetry or 'encodingRejectedSteps' not in session):
    raise SystemExit('Phase 3 telemetry restart/error hardening is missing')
contract = (root / 'docs/ACTION_CONTRACT.md').read_text(encoding='utf-8')
protocol = (root / 'docs/MODEL_BRIDGE_PROTOCOL.md').read_text(encoding='utf-8')
if 'sawbot.action/0.1' not in contract or 'local receive timestamp' not in contract:
    raise SystemExit('Action Contract is not synchronized with Phase 4')
if 'sawbot.bridge/0.1' not in protocol or '262,144' not in protocol or 'client thread' not in protocol:
    raise SystemExit('Bridge protocol documentation is incomplete')
print('PASS GitHub repository and Phase 4 packaging check')
PYREPOCHECK


python3 "$ROOT/sawbot-tools/dataset-validator/validate_telemetry.py" "$ROOT/build/phase3-telemetry-fixture.sbt" --json > "$ROOT/build/phase3-telemetry-report.json"
ROOT_FOR_PY="$ROOT" python3 - <<'PYTELEMETRY'
from pathlib import Path
import json, os, shutil, subprocess, sys
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
printf '%s\n' 'PASS offline Phase 4 verification'
