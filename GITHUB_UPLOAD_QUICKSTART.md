# Update `5ivesaw/b0t` and publish automatically

SawBotV1 updates are delivered as **patch-only ZIPs**. The permanent Downloads automation can apply them without extraction, commit them, push `main`, and clean old SawBot patch downloads.

Repository:

```text
C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready
```

For this repository state, the expected automatic release is:

```text
v0.4.0-alpha.0
```

## Automatic local update

1. Download `SawBotV1-0.4.0-alpha.0-PATCH.zip` into Downloads.
2. The installed SawBotV1 Downloads Automation validates and applies it.
3. It commits using `AUTO_UPDATE.json` and pushes `main`.
4. GitHub Actions verifies, builds, tags, and publishes the release.
5. Downloading the validated release JAR automatically replaces the older SawBot JAR in the configured PrismLauncher instance.

Do not create or push a tag manually.

## Manual fallback

Extract the patch ZIP and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\apply-patch.ps1 -RepositoryPath "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
cd "C:\Users\fivesaw\Desktop\SawBotV1-GitHub-Ready"
git add -A
git commit -m "Implement Phase 3 structured telemetry"
git push origin main
```

## GitHub pipeline

The `CI and automatic release` workflow:

1. reads `sawbotVersion` from `gradle.properties`;
2. runs offline contract, sensor, safety, and telemetry verification;
3. builds the remapped Forge 1.8.9 JAR;
4. validates the exact JAR and transferred release payload;
5. creates tag `v<sawbotVersion>`;
6. publishes the GitHub Release and all required assets.

Published versions are immutable. A reused version fails rather than overwriting a release.
