# GitHub CI and Releases

## CI

Every push and pull request runs two jobs:

1. **Offline contracts and safety checks** compiles common and Forge-facing source for Java 8 against narrow verification stubs and runs 56 assertions covering contracts, safety, bounded state, terrain transforms, mid-range caching, conservative team classification, and the Phase 1 pipeline.
2. **Build Forge 1.8.9 mod** installs Java 8 and Java 17, runs Gradle 8.8 with Architectury Loom, remaps the Forge JAR, validates required Phase 1 classes and metadata, and uploads release-shaped artifacts.

The real Forge dependency resolution/remapping occurs in GitHub because generated packaging environments may not have network access to Minecraft/Forge/Loom artifacts.

## Updating the repository

For the existing repository `5ivesaw/b0t`:

```powershell
git add .
git commit -m "Implement SawBotV1 Phase 1 internal eyes"
git push origin main
```

Wait for both CI jobs to pass before publishing a tag.

## Publishing `0.2.0-alpha.0`

Website method:

1. Open **Actions → Release → Run workflow**.
2. Enter `0.2.0-alpha.0` without the `v` prefix.
3. Keep prerelease enabled.
4. Run the workflow.

Tag method:

```powershell
git tag -a v0.2.0-alpha.0 -m "SawBotV1 Phase 1 internal eyes"
git push origin v0.2.0-alpha.0
```

A suffix such as `-alpha.0`, `-beta.1`, or `-rc.1` is marked as a prerelease when the workflow is triggered by a tag.

## Release assets

- Installable Forge JAR
- Sources JAR
- `SHA256SUMS.txt`
- `PHASE1_REPORT.md`
- `PHASE0_ACCEPTANCE.md`
- This guide

The release workflow uses GitHub's short-lived workflow token and requests only `contents: write`.

## Integrity checks

`tools/verify-built-jar.py` rejects a release when:

- The expected remapped JAR is missing.
- `mcmod.info` is absent, malformed, or has the wrong version/Minecraft version/mod ID.
- The Forge entry class, Observation v0.2 classes, ObservationPipeline, terrain sensor, or entity tracker is missing.
- Java source leaked into the installable JAR.

`tools/package-release.sh` checks the sources JAR and generates SHA-256 hashes for every published asset.

## Local build

Gradle runs on Java 17 and compiles Minecraft code using a Java 8 toolchain:

```powershell
.\gradlew.bat clean ciBuild
.\gradlew.bat runClient
```

If Gradle cannot discover JDK 8, add its absolute path to the user Gradle properties:

```properties
org.gradle.java.installations.paths=C:\Path\To\JDK8
```
