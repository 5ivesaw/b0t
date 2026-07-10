# Phase Report — Phase -1, Phase 0, and GitHub release lane

Date: 2026-07-10

## Workspace preservation

The starting archive contained the previously generated SawBotV1 Phase -1 and Phase 0 foundation. Runtime contracts, safety behavior, Java packages, documentation, and the control-centre prototype were preserved. The legacy build lane was replaced because the repository now needs to build reproducibly on GitHub-hosted runners.

## Completed

- Preserved the visible-client, internal-state-only, private-research scope.
- Preserved Observation Contract v0.1 and Action Contract v0.1.
- Preserved the complete user-approved project brief verbatim as `docs/PROJECT_BRIEF.txt` so GitHub remains self-contained.
- Preserved the Forge lifecycle, key bindings, safe state controller, complete input release, minimal HUD, configuration, and bounded performance timing.
- Preserved the offline Java 8 contract/safety verifier and eight foundation checks.
- Replaced ForgeGradle 2.1/Gradle 2.x configuration with Gradle 8.8, Architectury Loom `0.10.0.5`, and the Architectury Pack200 adapter.
- Kept Java 8 as the Minecraft bytecode/toolchain target while using Java 17 for the Gradle process.
- Added GitHub Actions CI for offline verification and a real remapped Forge 1.8.9 build.
- Added tag-triggered and manual GitHub Release publishing.
- Added installable-JAR validation, mandatory source-JAR validation, SHA-256 asset hashes, workflow artifacts, and release notes.
- Added a GitHub upload quick-start, Dependabot configuration, issue template, security policy, and release changelog configuration.
- Made `mcmod.info` the authoritative release-version source through Forge metadata, preventing a hardcoded annotation version from disagreeing with a workflow-selected release version.
- Corrected the JAR verifier to check the actual mod entry class: `dev/fivesaw/sawbot/forge/SawBotMod.class`.
- Added `loom.platform=forge`, required by the selected legacy Forge Loom lane.
- Corrected release-version precedence so `SAWBOT_VERSION` from GitHub Actions overrides the default in `gradle.properties`; this prevents non-default releases from being packaged with stale metadata.

## GitHub workflows

### `.github/workflows/ci.yml`

Runs on pushes, pull requests, and manual dispatch. It performs:

1. Offline contract and safety verification.
2. Shell/Python syntax validation.
3. Java 8 and Java 17 toolchain setup.
4. Gradle 8.8 Forge build.
5. Remapped JAR validation.
6. Phase 0 artifact packaging and upload.

### `.github/workflows/release.yml`

Runs from a `v*` tag or manually from the Actions tab. It:

1. Validates the requested version.
2. Runs offline verification.
3. Builds the remapped Forge JAR.
4. Checks JAR contents and metadata.
5. Generates checksums and release notes.
6. Uploads a workflow artifact.
7. Creates a GitHub tag and Release when manually dispatched, or publishes against the pushed tag.
8. Attaches the installable JAR, source JAR, checksums, and reports.

The release job receives only `contents: write` permission. No repository secret or personal access token is required for the normal workflow.

## Files created or materially changed for GitHub packaging

- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/release.yml`
- `.github/dependabot.yml`
- `.github/ISSUE_TEMPLATE/bug_report.yml`
- `.gitattributes`
- `.gitignore`
- `GITHUB_UPLOAD_QUICKSTART.md`
- `README.md`
- `SECURITY.md`
- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`
- `sawbot-common/build.gradle`
- `sawbot-forge-1.8.9/build.gradle`
- `sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/SawBotMod.java`
- `tools/gradle-bootstrap.ps1`
- `tools/bootstrap-toolchain.ps1`
- `tools/preflight.ps1`
- `tools/offline-verify.sh`
- `tools/package-release.sh`
- `tools/verify-built-jar.py`
- `docs/ARCHITECTURE.md`
- `docs/GITHUB_RELEASES.md`
- `docs/SOURCE_AUDIT.md`
- `docs/PHASE0_REPORT.md`
- `CHANGELOG.md`

The complete repository manifest is stored in `docs/PHASE0_FILE_MANIFEST.txt`.

## Verification performed in the packaging environment

- PASS — Java 8-targeted compilation against narrow Forge/Minecraft verification stubs.
- PASS — `FoundationContractTest` eight checks.
- PASS — expanded `mcmod.info` JSON parsing.
- PASS — repository packaging-file checks.
- PASS — shell syntax for `gradlew`, `offline-verify.sh`, and `package-release.sh`.
- PASS — Python bytecode compilation for `verify-built-jar.py`.
- PASS — GitHub workflow YAML parsing.
- PASS — standalone control-centre HTML parsing.
- PASS — standalone control-centre JavaScript syntax.
- PASS — synthetic valid-JAR acceptance by `verify-built-jar.py`.
- PASS — malformed/missing-entry synthetic JAR rejection by `verify-built-jar.py`.
- NOT EXECUTED HERE — online dependency resolution, real Loom remapping, and development-client launch.

## Why the real Forge build remains a GitHub acceptance gate

This packaging environment cannot directly resolve the required Gradle, Minecraft, MCP, Forge, and Loom binary dependencies. Therefore it would be dishonest to claim that the final remapped mod was built here. The repository is configured so the first GitHub Actions run performs that real online build and either produces the artifact or exposes the exact dependency/build error.

Phase 1 must not begin merely because the ZIP exists. Phase 0 runtime acceptance still requires a successful GitHub build followed by a local `runClient` test.

## GitHub acceptance checklist

- [ ] Extract the ZIP and publish its contents as the repository root.
- [ ] Confirm `.github/workflows/ci.yml` exists on GitHub.
- [ ] Confirm the first **CI** workflow starts automatically.
- [ ] Confirm `Offline contracts and safety checks` passes.
- [ ] Confirm `Build Forge 1.8.9 mod` passes.
- [ ] Download the `SawBotV1-Phase0` workflow artifact.
- [ ] Confirm it contains the installable JAR, source JAR, checksums, and report.
- [ ] Run the **Release** workflow with version `0.1.0-alpha.0`.
- [ ] Confirm GitHub creates tag `v0.1.0-alpha.0` and a prerelease with all assets.

## Runtime acceptance checklist

- [ ] Install JDK 17 and JDK 8 locally.
- [ ] Run `gradlew.bat clean ciBuild`.
- [ ] Run `gradlew.bat runClient`.
- [ ] Confirm the title screen works and a local world can be joined.
- [ ] Confirm the compact SawBotV1 HUD appears.
- [ ] Confirm F8 toggles enabled/disabled state.
- [ ] Hold movement/attack, press F9, and confirm immediate takeover/release.
- [ ] Hold movement/attack, press F12, and confirm emergency release.
- [ ] Idle for five minutes and inspect `logs/latest.log` for repeated errors.
- [ ] Record normal FPS and HUD timing measurements on the target HP EliteBook.

## Expected result

The GitHub workflow produces `SawBotV1-0.1.0-alpha.0-mc1.8.9.jar`. Installing it in a Forge 1.8.9 client should show the compact Phase 0 HUD while leaving SawBot disabled by default. Phase 0 has no autonomous actuator or neural model, so it must not move the player.

## Send back on failure

- The failed GitHub Actions step and its complete error section.
- Relevant `run/logs/latest.log` lines for runtime failures.
- Exact Gradle error text.
- Screenshot only for HUD/UI defects.
- Measured FPS and displayed handler timing.
- Reproduction steps and which safety key/state failed.
