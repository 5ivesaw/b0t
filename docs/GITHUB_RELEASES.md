# GitHub build and release flow

Normal publication requires one push to `main`:

```powershell
git add -A
git commit -m "Implement Phase 12 human motion and PvP motor body"
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
8. creates tag `v1.3.0-alpha.0` and publishes the GitHub Release

The manual recovery workflow exists only for exceptional publication failures. It is not
part of the normal development path.

## Version source

```properties
sawbotVersion=1.3.0-alpha.0
```

Published versions are immutable. Every new main-branch release must bump the version.

## Phase 12 primary assets

- `SawBotV1-1.3.0-alpha.0-mc1.8.9.jar`
- `SawBotV1-1.3.0-alpha.0-sources.jar`
- `SHA256SUMS.txt`
- `PHASE12_REPORT.md`
- `COMBAT_BODY.md`
- `HUMAN_MOTION_PROFILE.md`
- `PHASE9_REPORT.md`
- `SEGMENTED_NAVIGATION_CORE.md`
- `BARITONE_ARCHITECTURE_RESEARCH.md`
- `NAVIGATION_BODY.md`
- `PHASE8_REPORT.md`
- `BRIDGING_BODY.md`
- retained telemetry/model/evaluation evidence

- `PHASE11_REPORT.md`
- `REFERENCE_BODY_RESEARCH.md`
- `VISUALIZATION_LIFECYCLE.md`
