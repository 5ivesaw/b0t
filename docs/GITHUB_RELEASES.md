# GitHub CI and Automatic Releases

## Normal process

A push to `main` is the complete release operation:

```powershell
git add -A
git commit -m "Implement Phase 6 hybrid navigation body"
git push origin main
```

No manual tag command or second workflow launch is required.

## Pipeline

`CI and automatic release` performs:

1. Resolve and validate `sawbotVersion` from `gradle.properties`.
2. Compile Java 8-compatible source against the offline verification APIs.
3. Run foundation, telemetry, bridge, actuator, safety, and deterministic navigation-body checks.
4. Build/remap the real Forge 1.8.9 mod with Java 8 toolchain output.
5. Verify the JAR metadata and required classes.
6. Package and checksum the exact tested artifact.
7. On a successful `main` push, create the immutable version tag and GitHub Release.

Pull requests and non-main branches verify/build but do not publish.

## Version rule

```properties
sawbotVersion=0.7.0-alpha.0
```

The automatic tag is `v0.7.0-alpha.0`. Published versions are immutable; duplicate versions fail explicitly.

## Authentication and recursion safety

The release job uses the repository's short-lived `GITHUB_TOKEN` with only `contents: write`. The tested workflow creates the tag/release, and the generated tag does not start a release loop.

If repository policy blocks publication, enable **Read and write permissions** under **Settings → Actions → General → Workflow permissions**.

## Manual recovery

`Manual Release Recovery` is retained only for exceptional failures after a successful source update. It refuses to overwrite an existing release.

## Phase 6 primary assets

- Installable Forge JAR
- Sources JAR
- `SHA256SUMS.txt`
- `PHASE6_REPORT.md`
- `HYBRID_ARCHITECTURE.md`
- `NAVIGATION_BODY.md`
- Phase 5 historical checkpoint/evaluation assets
- Phase 4 bridge/actuator reports
- Phase 3 telemetry reports and format
- Phase 0–2 acceptance evidence
- `GITHUB_RELEASES.md`

`tools/verify-built-jar.py` validates metadata and required classes. `tools/package-release.sh` builds the payload and hashes. `tools/verify-release-payload.sh` rechecks every required file, JAR, and checksum after artifact transfer.
