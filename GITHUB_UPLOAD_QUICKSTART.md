# Update `5ivesaw/b0t` with SawBotV1 Phase 2

Extract the Phase 2 ZIP and copy/overwrite all contents into the root of the existing local `b0t` repository. Keep the hidden `.github` directory.

From PowerShell inside the repository:

```powershell
git status
git add -A
git commit -m "Implement SawBotV1 Phase 2 sensor inspector"
git push origin main
```

Open `https://github.com/5ivesaw/b0t/actions` and wait for both CI jobs:

- **Offline contracts and safety checks**
- **Build Forge 1.8.9 mod**

After both pass, publish the prerelease from GitHub Actions:

1. Open **Actions**.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `0.3.0-alpha.0`.
5. Keep prerelease enabled.
6. Run it.

Or push the release tag after CI passes:

```powershell
git tag -a v0.3.0-alpha.0 -m "SawBotV1 Phase 2 sensor inspector"
git push origin v0.3.0-alpha.0
```

The release should contain:

```text
SawBotV1-0.3.0-alpha.0-mc1.8.9.jar
SawBotV1-0.3.0-alpha.0-sources.jar
SHA256SUMS.txt
PHASE2_REPORT.md
PHASE1_ACCEPTANCE.md
PHASE0_ACCEPTANCE.md
GITHUB_RELEASES.md
```

Install only the `-mc1.8.9.jar` in the Forge 1.8.9 `mods` folder. Remove every older SawBotV1 JAR first.
