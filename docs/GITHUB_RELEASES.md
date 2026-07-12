# GitHub build and release lane

Push the patched repository to `main`:

```powershell
git add -A
git commit -m "Implement Phase 8 real-time bridging specialist"
git push origin main
```

The CI workflow:

1. reads the version from `gradle.properties`
2. compiles Java 8 offline contract tests with warnings as errors
3. verifies navigation, bridging, telemetry, brain transport, actuator, inspector,
   input ownership, and safety contracts
4. runs the real Forge/Loom build
5. verifies the built JAR contents and metadata
6. packages and checksums release assets
7. creates the immutable tag and GitHub release automatically after a successful
   `main` push

Current version:

```properties
sawbotVersion=0.9.0-alpha.0
```

The automatic tag is `v0.9.0-alpha.0`. Published versions are immutable; duplicate
versions fail explicitly.

## Phase 8 primary assets

- `SawBotV1-0.9.0-alpha.0-mc1.8.9.jar`
- `SawBotV1-0.9.0-alpha.0-sources.jar`
- `PHASE8_REPORT.md`
- `BRIDGING_BODY.md`
- `PHASE7_REPORT.md`
- `ADAPTIVE_NAVIGATION.md`
- `HYBRID_ARCHITECTURE.md`
- `NAVIGATION_BODY.md`
- `SHA256SUMS.txt`
- `release-notes.md`

Historical reports, telemetry-format material, and the Phase 5 learned waypoint
baseline remain attached for reproducibility.

`release.yml` is manual recovery only. It must not replace the normal automatic
main-push release.
