# GitHub build and release flow

Normal publication requires one push to `main`:

```powershell
git add -A
git commit -m "Implement Phase 9 segmented navigation core"
git push origin main
```

The CI workflow then:

1. resolves `sawbotVersion` from `gradle.properties`
2. compiles every Java source and runs all offline contracts
3. builds/remaps the Forge 1.8.9 JAR
4. validates JAR metadata and required classes
5. packages documentation/model evidence and SHA-256 hashes
6. downloads and revalidates the exact tested artifact
7. rejects a reused tag/version
8. creates tag `v1.0.0-alpha.0` and publishes the GitHub Release

The manual recovery workflow exists only for exceptional publication failures. It is not
part of the normal development path.

## Version source

```properties
sawbotVersion=1.0.0-alpha.0
```

Published versions are immutable. Every new main-branch release must bump the version.

## Phase 9 primary assets

- `SawBotV1-1.0.0-alpha.0-mc1.8.9.jar`
- `SawBotV1-1.0.0-alpha.0-sources.jar`
- `SHA256SUMS.txt`
- `PHASE9_REPORT.md`
- `SEGMENTED_NAVIGATION_CORE.md`
- `BARITONE_ARCHITECTURE_RESEARCH.md`
- `NAVIGATION_BODY.md`
- `PHASE8_REPORT.md`
- `BRIDGING_BODY.md`
- retained telemetry/model/evaluation evidence
