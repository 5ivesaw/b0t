# Repository tree — Phase 2 GitHub-ready candidate

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
│   ├── PHASE_GATES.md
│   ├── PHASE0_ACCEPTANCE.md
│   ├── PHASE1_ACCEPTANCE.md
│   ├── PHASE1_REPORT.md
│   ├── PHASE2_REPORT.md
│   ├── GITHUB_RELEASES.md
│   ├── PHASE2_FILE_MANIFEST.txt
│   └── remaining design/audit documents
├── sawbot-common/src/main/java/dev/fivesaw/sawbot/common/
│   ├── action/
│   ├── events/
│   ├── observation/       # includes ObservationDiff + calculator
│   ├── protocol/
│   └── versioning/
├── sawbot-forge-1.8.9/src/main/
│   ├── java/dev/fivesaw/sawbot/forge/
│   │   ├── client/
│   │   ├── config/
│   │   ├── hud/           # text HUD + world debug renderer
│   │   ├── inspection/    # selection, pages, JSON export
│   │   ├── map/
│   │   ├── performance/
│   │   ├── safety/
│   │   ├── sensors/
│   │   └── tracking/
│   └── resources/
├── sawbot-trainer/        # gated placeholder
├── sawbot-sim/            # gated placeholder
├── sawbot-arenas/         # gated placeholder
├── sawbot-tools/          # gated placeholder
├── prototypes/control-center.html
├── verification-stubs/
├── verification-tests/
└── tools/
    ├── offline-verify.sh
    ├── package-release.sh
    ├── verify-built-jar.py
    └── local bootstrap/preflight scripts
```

Only `sawbot-common` and `sawbot-forge-1.8.9` participate in the Gradle build. Trainer, simulator, arena, and high-volume telemetry implementations remain gated.
