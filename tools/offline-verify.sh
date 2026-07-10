#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/offline-classes"
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
text=resource.read_text(encoding='utf-8').replace('${version}','0.3.0-alpha.0').replace('${mcversion}','1.8.9')
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
    root / 'tools/verify-built-jar.py',
    root / 'docs/GITHUB_RELEASES.md',
    root / 'docs/PHASE0_ACCEPTANCE.md',
    root / 'docs/PHASE2_REPORT.md',
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
properties = (root / 'gradle.properties').read_text(encoding='utf-8')
if 'loom.platform=forge' not in properties:
    raise SystemExit('gradle.properties does not declare loom.platform=forge')
verifier = (root / 'tools/verify-built-jar.py').read_text(encoding='utf-8')
if ('dev/fivesaw/sawbot/forge/SawBotMod.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/sensors/ObservationPipeline.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/inspection/SnapshotExportService.class' not in verifier
    or 'dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class' not in verifier):
    raise SystemExit('Release verifier does not check Phase 2 runtime classes')
print('PASS GitHub repository packaging check')
PYREPOCHECK

printf '%s\n' 'PASS offline Phase 2 verification'
