# GitHub CI and Automatic Releases

## Normal process

A push to `main` is now the complete release operation:

```powershell
git add -A
git commit -m "Describe the SawBotV1 update"
git push origin main
```

No manual tag command and no second workflow launch are required.

## Pipeline

`CI and automatic release` runs these jobs:

1. **Resolve repository version** reads and validates `sawbotVersion` from `gradle.properties`.
2. **Offline contracts and safety checks** compiles Java 8-compatible source against the verification APIs, runs the regression suite, validates exported JSON, and audits repository structure.
3. **Build Forge 1.8.9 mod** installs Java 8 and Java 17, builds and remaps the real mod, validates its metadata/classes, and packages a release-shaped artifact.
4. **Publish release automatically** runs only for successful pushes to `main`. It downloads the exact artifact produced by the tested build, verifies checksums again, creates the version tag, and publishes the GitHub Release.

Pull requests and non-main branches run verification/builds but do not publish releases.

## Version rule

The source of truth is:

```properties
sawbotVersion=0.3.0-alpha.3
```

The automatic release is tagged:

```text
v0.3.0-alpha.3
```

Published versions are immutable. Reusing a version causes an explicit failure. This prevents a release tag from referring to different code at different times.

## Authentication and recursion safety

The release job uses the repository's short-lived `GITHUB_TOKEN` and requests only `contents: write`. The tag and release are created by the same tested workflow. GitHub does not start a second workflow from ordinary events created with `GITHUB_TOKEN`, so the automatically created tag does not produce a release loop.

If repository policy blocks write access, enable **Read and write permissions** under **Settings → Actions → General → Workflow permissions**.

## Manual recovery

`Manual Release Recovery` is retained for exceptional cases such as a release that failed after a successful build. It can use the repository version or an explicitly supplied version. It refuses to overwrite an existing release.

## Release assets

- Installable Forge JAR
- Sources JAR
- `SHA256SUMS.txt`
- `PHASE2_REPORT.md`
- `PHASE2_RUNTIME_FEEDBACK.md`
- `PHASE1_ACCEPTANCE.md`
- `PHASE0_ACCEPTANCE.md`
- `GITHUB_RELEASES.md`
- `INTERFACE_DESIGN_SYSTEM.md`

## Integrity checks

`tools/verify-built-jar.py` rejects a release when required metadata/classes are missing, the embedded version is wrong, or Java source leaks into the installable JAR.

`tools/package-release.sh` creates the release payload and hashes.

`tools/verify-release-payload.sh` checks every required file, validates the installable JAR, and verifies `SHA256SUMS.txt` after artifact transfer.
