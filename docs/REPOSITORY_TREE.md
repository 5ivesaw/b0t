# Repository tree — Phase 3 GitHub-ready candidate

```text
SawBotV1/
├── .github/
│   ├── workflows/{ci.yml,release.yml}
│   ├── ISSUE_TEMPLATE/bug_report.yml
│   ├── dependabot.yml
│   └── release.yml
├── README.md
├── GITHUB_UPLOAD_QUICKSTART.md
├── CHANGELOG.md
├── LICENSE
├── SECURITY.md
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── docs/
│   ├── PROJECT_BRIEF.txt
│   ├── ARCHITECTURE.md
│   ├── PERFORMANCE_BUDGET.md
│   ├── OBSERVATION_CONTRACT.md
│   ├── ACTION_CONTRACT.md
│   ├── TELEMETRY_FORMAT.md
│   ├── PHASE_GATES.md
│   ├── PHASE0_ACCEPTANCE.md
│   ├── PHASE1_ACCEPTANCE.md
│   ├── PHASE2_ACCEPTANCE.md
│   ├── PHASE3_REPORT.md
│   ├── GITHUB_RELEASES.md
│   └── remaining reports, design, audit, and historical evidence
├── sawbot-common/src/main/java/dev/fivesaw/sawbot/common/
│   ├── action/
│   ├── events/
│   ├── observation/
│   ├── telemetry/        # immutable input windows and trajectory steps
│   ├── protocol/
│   └── versioning/
├── sawbot-forge-1.8.9/src/main/
│   ├── java/dev/fivesaw/sawbot/forge/
│   │   ├── client/
│   │   ├── config/
│   │   ├── hud/          # compact text HUD + world debug renderer
│   │   ├── inspection/   # selection, pages, JSON export
│   │   ├── map/
│   │   ├── performance/
│   │   ├── safety/
│   │   ├── sensors/
│   │   ├── telemetry/    # bounded async structured trajectory writer
│   │   └── tracking/
│   └── resources/
├── sawbot-tools/
│   ├── dataset-validator/validate_telemetry.py
│   ├── replay-inspector/inspect_telemetry.py
│   ├── telemetry-inspector/   # later interactive tooling
│   └── benchmark/
├── sawbot-trainer/       # gated until telemetry runtime acceptance
├── sawbot-sim/           # gated placeholder
├── sawbot-arenas/        # gated placeholder
├── prototypes/control-center.html
├── verification-stubs/
├── verification-tests/
└── tools/
    ├── offline-verify.sh
    ├── package-release.sh
    ├── verify-built-jar.py
    ├── verify-release-payload.sh
    ├── TEST-LATEST-TELEMETRY.bat
    ├── test-latest-telemetry.ps1
    └── local bootstrap/preflight scripts
```

Only `sawbot-common` and `sawbot-forge-1.8.9` participate in the Gradle mod build. The Phase 3 validator and replay inspector are Python development tools and are shipped as repository/release documentation assets rather than inside the Minecraft JAR. Neural training, simulator, and arena implementations remain gated.
