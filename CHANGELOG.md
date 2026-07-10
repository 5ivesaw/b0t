# Changelog

## 0.1.0-alpha.0 — Phase 0 foundation

- Locked private-research scope and safety boundary.
- Added Observation Contract v0.1 and Action Contract v0.1.
- Added performance budget, risk register, source audit, telemetry plan, simulator validation plan, and phase gates.
- Added Forge 1.8.9 client entry point, key bindings, safe state controller, emergency key release, minimal HUD, and bounded timing window.
- Added Java 8 offline verifier and contract smoke tests.
- Added standalone control-centre prototype.

## 0.1.0-alpha.0 GitHub packaging update

- Added GitHub Actions CI for offline verification and a real Forge 1.8.9 build.
- Added manual and tag-triggered GitHub Release publishing.
- Added validated installable JAR, sources JAR, checksums, and release documentation assets.
- Replaced the deprecated ForgeGradle 2.1 build lane with Gradle 8.8 and Architectury Loom while preserving Java 8 output and Forge 1.8.9 runtime compatibility.
- Added checksum-verifying local Gradle bootstrap launchers.
- Made `SAWBOT_VERSION` override the repository default so manually selected GitHub Release versions are embedded and validated correctly.
