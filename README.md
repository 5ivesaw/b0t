# SawBotV1

SawBotV1 is a client-side Minecraft Java Edition 1.8.9 Forge research mod for a visible, inspectable neural agent. Minecraft is the agent's body. The mod reads structured internal game state, never screen pixels, and actuates the same legitimate client controls available to a player.

## Current gate

This repository is stopped at **Phase 0 — Forge Foundation**. It contains contracts, engineering records, a safe client lifecycle, key bindings, a minimal HUD, bounded timing instrumentation, an offline verifier, a standalone control-centre prototype, and GitHub CI/release automation.

It contains no Bedwars logic, neural training, pathfinder, aim assist, scaffold controller, packet advantage, screenshot pipeline, or public-server automation.

The complete locked project brief is preserved verbatim in [`docs/PROJECT_BRIEF.txt`](docs/PROJECT_BRIEF.txt).

## GitHub build and automatic releases

Every push and pull request runs:

- Offline contract and safety verification
- A real remapped Minecraft Forge 1.8.9 build
- Release-JAR structure and version validation
- Workflow artifact upload

A release can be published entirely from GitHub:

1. Open **Actions**.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `0.1.0-alpha.0` or another version without the `v` prefix.
5. Run it.

The workflow creates the tag and GitHub Release, then attaches the installable JAR, sources, checksums, and reports. See [`GITHUB_UPLOAD_QUICKSTART.md`](GITHUB_UPLOAD_QUICKSTART.md) and [`docs/GITHUB_RELEASES.md`](docs/GITHUB_RELEASES.md).

## Toolchain

- Gradle 8.8
- Architectury Loom `0.10.0.5`
- Architectury Pack200 `0.1.3`
- Java 17 for the Gradle process
- Java 8 toolchain for compiled Minecraft code
- Minecraft Forge `1.8.9-11.15.1.2318-1.8.9`
- MCP mappings `stable_22`

The modern build lane replaces the deprecated ForgeGradle 2.1 setup while retaining Minecraft 1.8.9 and Java 8 output.

## Local build

Install JDK 17 and JDK 8, then run:

```powershell
.\gradlew.bat clean ciBuild
```

or:

```bash
./gradlew clean ciBuild
```

The launchers download Gradle 8.8 and verify its official SHA-256 checksum. The final mod is written to:

```text
sawbot-forge-1.8.9/build/libs/SawBotV1-<version>-mc1.8.9.jar
```

## Offline source verification

```bash
bash tools/offline-verify.sh
```

This does not replace a real Forge build or client launch. It checks Java 8 source compatibility, contracts, state transitions, action validation, input release, metadata, and bounded timing logic.

## Phase 0 keys

- `F8`: enable/disable SawBot
- `F9`: immediate manual takeover
- `F12`: emergency release all inputs
- `F7`: toggle inspector placeholder
- `F6`: freeze/unfreeze foundation state
- `F5`: toggle telemetry intent placeholder

No action-driving model exists in Phase 0.

## Safety scope

Autonomous testing is restricted to single-player worlds, LAN tests, owned private servers, and purpose-built local arenas. The project does not permit anti-cheat bypasses, public-server automation, authentication bypasses, packet manipulation for advantage, altered reach, impossible placement, or silent server-only controls.
