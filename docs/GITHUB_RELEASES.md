# GitHub build and release lane

Push the patched repository to `main`:

```powershell
git add -A
git commit -m "Implement Phase 7 real-time adaptive navigation"
git push origin main
```

The CI workflow:

1. reads the version from `gradle.properties`
2. compiles Java 8 offline contract tests with warnings as errors
3. verifies adaptive navigation, telemetry, bridge, actuator, inspector, and safety contracts
4. runs the real Forge/Loom build
5. verifies the built JAR contents and metadata
6. packages and checksums release assets
7. creates the immutable tag and GitHub release automatically on a successful `main` push

Current version:

```properties
sawbotVersion=0.8.0-alpha.0
```

The automatic tag is `v0.8.0-alpha.0`. Published versions are immutable; duplicate versions fail explicitly.

## Phase 7 primary assets

- `SawBotV1-0.8.0-alpha.0-mc1.8.9.jar`
- `SawBotV1-0.8.0-alpha.0-sources.jar`
- `PHASE7_REPORT.md`
- `ADAPTIVE_NAVIGATION.md`
- `HYBRID_ARCHITECTURE.md`
- `NAVIGATION_BODY.md`
- `SHA256SUMS.txt`
- `release-notes.md`

Historical reports, telemetry format material, and the Phase 5 learned waypoint baseline remain attached for reproducibility.

`release.yml` is manual recovery only. It must not be used as a substitute for the normal automatic main-push release.
