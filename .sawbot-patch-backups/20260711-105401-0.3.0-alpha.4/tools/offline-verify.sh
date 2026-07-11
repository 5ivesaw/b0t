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
java -Dsawbot.fixture="$ROOT/build/phase2-snapshot-fixture.json" -cp "$OUT" dev.fivesaw.sawbot.verification.FoundationContractTest

ROOT_FOR_PY="$ROOT" python3 - <<'PYJSONFIXTURE'
from pathlib import Path
import json, os
root=Path(os.environ['ROOT_FOR_PY'])
fixture=root/'build/phase2-snapshot-fixture.json'
data=json.loads(fixture.read_text(encoding='utf-8'))
assert data['exportFormat']=='sawbot.snapshot.debug/0.1'
assert len(data['localTerrain']['blockStateIds'])==1521
assert len(data['midRangeMap']['relativeSurfaceY'])==1089
assert len(data['inventory']['slots'])==41
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
    root / 'docs/INTERFACE_DESIGN_SYSTEM.md',
    root / 'docs/PHASE0_ACCEPTANCE.md',
    root / 'docs/PHASE2_REPORT.md',
    root / 'docs/PHASE2_RUNTIME_FEEDBACK.md',
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
if ('INTERFACE_DESIGN_SYSTEM.md' not in package_script
    or 'SHA256SUMS.txt' not in package_script):
    raise SystemExit('Release packager does not include the design charter and checksums')
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
if ('dev/fivesaw/sawbot/forge/SawBotMod.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/sensors/ObservationPipeline.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/inspection/SnapshotExportService.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/tracking/VisibilitySampler.class' not in verifier):
    raise SystemExit('Release verifier does not check Phase 2 runtime classes')
renderer = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.java').read_text(encoding='utf-8')
style = (root / 'sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.java').read_text(encoding='utf-8')
if ('EntityVisualStyle.visibilityRgb(entity)' not in renderer
    or 'EntityVisualStyle.visibilityArgb(entity)' not in renderer
    or 'EntityVisualStyle.SELECTED_ACCENT_RGB' not in renderer
    or 'private static int[] entityColor' in renderer):
    raise SystemExit('World renderer does not use the immediate visibility style consistently')
if ('LOS_RGB = 0x55FF55' not in style or 'OCCLUDED_RGB = 0xAA55FF' not in style):
    raise SystemExit('Visibility style colours changed without updating the acceptance contract')
print('PASS GitHub repository packaging check')
PYREPOCHECK

printf '%s\n' 'PASS offline Phase 2 verification'
