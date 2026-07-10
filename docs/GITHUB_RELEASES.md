# GitHub CI and Releases

## What the repository now does

Every push and pull request runs two independent checks:

1. **Offline verification** compiles the contracts and Forge-facing source against narrow API stubs, then executes the Phase 0 safety tests.
2. **Forge build** uses Gradle 8.8 with Architectury Loom, runs Gradle on Java 17, compiles the mod for Java 8, remaps the Forge JAR, validates its contents, and uploads it as a workflow artifact.

The build lane was modernized because legacy ForgeGradle 2.1 and Gradle 2.x are no longer a dependable foundation for present-day hosted CI. This changes build tooling only; it does not change SawBot's runtime contracts, safety scope, or Minecraft 1.8.9 target.

## Uploading the repository

1. Extract the ZIP.
2. Create an empty GitHub repository.
3. Upload **the contents inside the `SawBotV1` folder**, not the outer ZIP folder.
4. Commit to the default branch.
5. Open the repository's **Actions** tab and allow the first CI run to finish.

No repository secret or personal access token is required. GitHub supplies the release workflow with a short-lived `GITHUB_TOKEN`. The workflow requests only `contents: write` while publishing a release.

## Creating a release from the GitHub website

1. Open **Actions**.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter a version without `v`, such as `0.1.0-alpha.0`.
5. Leave **Publish as a prerelease** enabled for Phase 0.
6. Run the workflow.

The workflow builds and verifies the project, creates tag `v<version>`, publishes the GitHub Release, and attaches:

- Installable Forge JAR
- Sources JAR
- SHA-256 checksums
- Phase 0 report
- GitHub build/release guide

## Creating a release with Git

Pushing a version tag also starts the release workflow:

```bash
git tag v0.1.0-alpha.0
git push origin v0.1.0-alpha.0
```

Versions containing a suffix such as `-alpha.0`, `-beta.1`, or `-rc.1` are automatically marked as prereleases when triggered by a tag.

## Workflow files

- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/release.yml`
- `.github/dependabot.yml`

## Release integrity

`tools/verify-built-jar.py` rejects a release when:

- The expected remapped Forge JAR is missing.
- `mcmod.info` is absent or malformed.
- The version inside `mcmod.info` does not match the release version.
- Core Forge or common-contract classes are missing.
- Java source files leaked into the installable JAR.

`tools/package-release.sh` then creates `SHA256SUMS.txt` for the released assets.

## Local build

The checked-in `gradlew` and `gradlew.bat` are checksum-verifying bootstrap launchers. They download Gradle 8.8 into the user's Gradle cache because this generated archive cannot safely embed an unverified wrapper JAR.

Gradle itself runs on Java 17. Compilation targets a Java 8 toolchain.

```powershell
.\gradlew.bat clean ciBuild
```

```bash
./gradlew clean ciBuild
```

For local development, install JDK 17 and JDK 8. If Gradle does not discover JDK 8 automatically, set:

```properties
org.gradle.java.installations.paths=/absolute/path/to/jdk8
```

inside the user's `~/.gradle/gradle.properties` file.
