# GitHub CI and Releases

## CI

Every push and pull request runs two jobs:

1. **Offline contracts and safety checks** compiles common and Forge-facing source for Java 8 against narrow verification stubs, runs 508 assertions, generates a full debug snapshot, parses the JSON, and verifies exact terrain/map/inventory bounds.
2. **Build Forge 1.8.9 mod** installs Java 8 and Java 17, runs Gradle 8.8 with Architectury Loom, remaps the Forge JAR, validates required Phase 2 classes and metadata, and uploads release-shaped artifacts.

The real Forge dependency resolution/remapping occurs on GitHub because the artifact-generation environment may not have network access to Minecraft/Forge/Loom repositories.

## Updating `5ivesaw/b0t`

```powershell
git add -A
git commit -m "Implement SawBotV1 Phase 2 sensor inspector"
git push origin main
```

Wait for both CI jobs to pass before publishing a tag.

## Publishing `0.3.0-alpha.0`

Website method:

1. Open **Actions → Release → Run workflow**.
2. Enter `0.3.0-alpha.0` without the `v` prefix.
3. Keep prerelease enabled.
4. Run the workflow.

Tag method:

```powershell
git tag -a v0.3.0-alpha.0 -m "SawBotV1 Phase 2 sensor inspector"
git push origin v0.3.0-alpha.0
```

## Release assets

- Installable Forge JAR
- Sources JAR
- `SHA256SUMS.txt`
- `PHASE2_REPORT.md`
- `PHASE1_ACCEPTANCE.md`
- `PHASE0_ACCEPTANCE.md`
- This guide

The workflow uses GitHub's short-lived workflow token and requests only `contents: write`.

## Integrity checks

`tools/verify-built-jar.py` rejects a release when:

- The expected remapped JAR is missing.
- `mcmod.info` is absent, malformed, or has the wrong version, Minecraft version, or mod ID.
- The Forge entry class, observation classes, pipeline, terrain/entity sensors, difference calculator, inspector controller, snapshot exporter, or world renderer is missing.
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
