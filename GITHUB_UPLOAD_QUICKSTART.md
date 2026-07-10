# Update `5ivesaw/b0t` with SawBotV1 Phase 1

Extract the Phase 1 ZIP and copy/overwrite its contents into the root of your existing local `b0t` repository. Keep the hidden `.github` directory.

From PowerShell inside that repository:

```powershell
git status
git add .
git commit -m "Implement SawBotV1 Phase 1 internal eyes"
git push origin main
```

Open `https://github.com/5ivesaw/b0t/actions` and wait for both CI jobs:

- **Offline contracts and safety checks**
- **Build Forge 1.8.9 mod**

After both pass, publish the Phase 1 prerelease from the website:

1. Open **Actions**.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `0.2.0-alpha.0`.
5. Keep **Publish as a prerelease** enabled.
6. Run it.

Or create the release with a tag after CI passes:

```powershell
git tag -a v0.2.0-alpha.0 -m "SawBotV1 Phase 1 internal eyes"
git push origin v0.2.0-alpha.0
```

The release should contain:

```text
SawBotV1-0.2.0-alpha.0-mc1.8.9.jar
SawBotV1-0.2.0-alpha.0-sources.jar
SHA256SUMS.txt
PHASE1_REPORT.md
PHASE0_ACCEPTANCE.md
GITHUB_RELEASES.md
```

Install only the `-mc1.8.9.jar` file in the Forge 1.8.9 `mods` folder. Remove the old `0.1.0-alpha.0` JAR first so two SawBot versions do not load together.
