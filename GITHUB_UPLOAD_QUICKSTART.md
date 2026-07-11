# Update `5ivesaw/b0t` with the SawBotV1 Phase 2 visual patch

This update is distributed as a **patch-only ZIP**. Run `apply-patch.ps1` from the extracted patch folder, or copy only the included repository-path files over the existing local `b0t` repository. Read `DELETE_THESE.txt` before committing.

From PowerShell inside the repository:

```powershell
git status
git add -A
git commit -m "Make Phase 2 LOS OCC colours update immediately"
git push origin main
```

Open `https://github.com/5ivesaw/b0t/actions` and wait for both CI jobs:

- **Offline contracts and safety checks**
- **Build Forge 1.8.9 mod**

After both pass, publish the prerelease from GitHub Actions:

1. Open **Actions**.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `0.3.0-alpha.2`.
5. Keep prerelease enabled.
6. Run it.

Or push the release tag after CI passes:

```powershell
git tag -a v0.3.0-alpha.2 -m "SawBotV1 Phase 2 visibility colour fix"
git push origin v0.3.0-alpha.2
```

The release should contain:

```text
SawBotV1-0.3.0-alpha.2-mc1.8.9.jar
SawBotV1-0.3.0-alpha.2-sources.jar
SHA256SUMS.txt
PHASE2_REPORT.md
PHASE1_ACCEPTANCE.md
PHASE0_ACCEPTANCE.md
GITHUB_RELEASES.md
```

Install only the `-mc1.8.9.jar` in the Forge 1.8.9 `mods` folder. Remove every older SawBotV1 JAR first.
