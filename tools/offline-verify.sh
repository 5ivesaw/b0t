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
    root / 'docs/PHASE2_UI_REVERT.md',
    root / 'docs/PHASE0_ACCEPTANCE.md',
    root / 'docs/PHASE2_REPORT.md',
    root / 'docs/PHASE2_RUNTIME_FEEDBACK.md',
    root / 'docs/PHASE2_RUNTIME_VALIDATION.md',
    root / 'docs/PHASE3_REPORT.md',
    root / 'sawbot-tools/dataset-validator/validate_telemetry.py',
    root / 'sawbot-tools/replay-inspector/inspect_telemetry.py',
    root / 'tools/test-latest-telemetry.ps1',
    root / 'tools/TEST-LATEST-TELEMETRY.bat',
    root / 'docs/PHASE1_ACCEPTANCE.md',
    root / 'GITHUB_UPLOAD_QUICKSTART.md',
]
missing = [str(path.relative_to(root)) for path in required if not path.is_file()]
if missing:
    raise SystemExit('Missing GitHub packaging files: ' + ', '.join(missing))
for workflow in required[:2]:
    text = workflow.read_text(encoding='utf-8')
    if '\t' in text:
        raise SystemExit(f'Tab character found in workflow: {workflow}')
    if ('OWNER' + '/REPOSITORY') in text:
        raise SystemExit(f'Placeholder repository path found in workflow: {workflow}')
ci_text = (root / '.github/workflows/ci.yml').read_text(encoding='utf-8')
release_text = (root / '.github/workflows/release.yml').read_text(encoding='utf-8')
if ('automatic-release:' not in ci_text
    or "github.ref == 'refs/heads/main'" not in ci_text
    or 'needs: [metadata, offline-verification, forge-build]' not in ci_text
    or 'actions/download-artifact@' not in ci_text
    or 'tools/verify-release-payload.sh' not in ci_text
    or 'PHASE2_RUNTIME_VALIDATION.md' not in ci_text
    or 'PHASE3_REPORT.md' not in ci_text
    or 'TELEMETRY_FORMAT.md' not in ci_text
    or 'gh release create' not in ci_text
    or 'contents: write' not in ci_text):
    raise SystemExit('CI does not contain the verified automatic main-branch release gate')
if "tags:\n      - 'v*'" in release_text:
    raise SystemExit('Manual recovery workflow must not be tag-triggered')
if 'workflow_dispatch:' not in release_text or 'Manual Release Recovery' not in release_text:
    raise SystemExit('Manual release recovery workflow is missing or malformed')
properties = (root / 'gradle.properties').read_text(encoding='utf-8')
if 'loom.platform=forge' not in properties:
    raise SystemExit('gradle.properties does not declare loom.platform=forge')
version = os.environ['SAWBOT_VERIFY_VERSION']
if f'sawbotVersion={version}' not in properties:
    raise SystemExit('read-version.sh output does not match gradle.properties')
package_script = (root / 'tools/package-release.sh').read_text(encoding='utf-8')
if ('PHASE2_UI_REVERT.md' not in package_script
    or 'PHASE2_RUNTIME_VALIDATION.md' not in package_script
    or 'PHASE3_REPORT.md' not in package_script
    or 'TELEMETRY_FORMAT.md' not in package_script
    or 'SHA256SUMS.txt' not in package_script):
    raise SystemExit('Release packager does not include runtime evidence and checksums')
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
if ('dev/fivesaw/sawbot/forge/SawBotMod.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/sensors/ObservationPipeline.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/inspection/SnapshotExportService.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/tracking/VisibilitySampler.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/tracking/EntityTypeClassifier.class' not in verifier
    or 'dev/fivesaw/sawbot/common/observation/EntityType.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/telemetry/TelemetryService.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/telemetry/TelemetryBinaryCodec.class' not in verifier
    or 'dev/fivesaw/sawbot/common/telemetry/TrajectoryStep.class' not in verifier):
    raise SystemExit('Release verifier does not check Phase 3 runtime classes')
renderer = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java').read_text(encoding='utf-8')
style = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.java').read_text(encoding='utf-8')
if ('EntityVisualStyle.visibilityRgb(entity)' not in renderer
    or 'EntityVisualStyle.visibilityArgb(entity)' not in renderer
    or 'EntityVisualStyle.SELECTED_ACCENT_RGB' not in renderer
    or 'private static int[] entityColor' in renderer):
    raise SystemExit('World renderer does not use the immediate visibility style consistently')
if ('LOS_RGB = 0x55FF55' not in style or 'OCCLUDED_RGB = 0xAA55FF' not in style):
    raise SystemExit('Visibility style colours changed without updating the acceptance contract')
if ('GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)' not in renderer
    or 'FROZEN SNAPSHOT #' not in renderer
    or '255, 255, 85, 255' not in renderer):
    raise SystemExit('World renderer is missing alpha.6 state isolation, frozen anchor, or yellow selection')
export_service = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/inspection/SnapshotExportService.java').read_text(encoding='utf-8')
if ('SUCCESS_STATUS_LIFETIME_NANOS' not in export_service or 'status = "idle"' not in export_service):
    raise SystemExit('Snapshot export success status is not transient')
contract = (root / 'docs/OBSERVATION_CONTRACT.md').read_text(encoding='utf-8')
if ('sawbot.observation/0.3' not in contract
    or 'sawbot.snapshot.debug/0.2' not in contract
    or 'EntityType' not in contract):
    raise SystemExit('Observation Contract documentation is not synchronized with alpha.6')
telemetry_doc = (root / 'docs/TELEMETRY_FORMAT.md').read_text(encoding='utf-8')
if ('sawbot.telemetry/0.1' not in telemetry_doc or 'little-endian' not in telemetry_doc or 'CRC32' not in telemetry_doc):
    raise SystemExit('Telemetry format documentation is incomplete')
latest_test = (root / 'tools/test-latest-telemetry.ps1').read_text(encoding='utf-8')
if ('sawbot.telemetry/0.1' not in latest_test
    or 'sawbot.observation/0.3' not in latest_test
    or '--recover' not in latest_test
    or 'PrismLauncher' not in latest_test):
    raise SystemExit('Latest-telemetry acceptance helper is incomplete')
print('PASS GitHub repository packaging check')
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

printf '%s\n' 'PASS offline Phase 3 verification'
