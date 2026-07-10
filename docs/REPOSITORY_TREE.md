# Repository tree — Phase 0 GitHub-ready foundation

```text
SawBotV1/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   └── release.yml
│   ├── ISSUE_TEMPLATE/bug_report.yml
│   ├── dependabot.yml
│   └── release.yml
├── .gitattributes
├── .gitignore
├── README.md
├── GITHUB_UPLOAD_QUICKSTART.md
├── SECURITY.md
├── LICENSE
├── CHANGELOG.md
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
├── docs/
│   ├── VISION.md
│   ├── ARCHITECTURE.md
│   ├── SAFETY_SCOPE.md
│   ├── PERFORMANCE_BUDGET.md
│   ├── OBSERVATION_CONTRACT.md
│   ├── ACTION_CONTRACT.md
│   ├── TELEMETRY_FORMAT.md
│   ├── TRAINING_STRATEGY.md
│   ├── SIMULATOR_VALIDATION.md
│   ├── SOURCE_AUDIT.md
│   ├── RISK_REGISTER.md
│   ├── PHASE_GATES.md
│   ├── GITHUB_RELEASES.md
│   ├── PHASE0_REPORT.md
│   ├── PROJECT_BRIEF.txt
│   ├── PHASE0_FILE_MANIFEST.txt
│   └── REPOSITORY_TREE.md
├── sawbot-common/
│   ├── build.gradle
│   └── src/main/java/dev/fivesaw/sawbot/common/
│       ├── action/
│       ├── observation/
│       └── versioning/
├── sawbot-forge-1.8.9/
│   ├── build.gradle
│   └── src/main/
│       ├── java/dev/fivesaw/sawbot/forge/
│       │   ├── client/
│       │   ├── config/
│       │   ├── hud/
│       │   ├── performance/
│       │   └── safety/
│       └── resources/
├── sawbot-trainer/
├── sawbot-sim/
├── sawbot-arenas/
├── sawbot-tools/
├── prototypes/control-center.html
├── verification-stubs/
├── verification-tests/
└── tools/
    ├── bootstrap-toolchain.ps1
    ├── gradle-bootstrap.ps1
    ├── preflight.ps1
    ├── offline-verify.sh
    ├── package-release.sh
    └── verify-built-jar.py
```

The requested future module boundaries remain present. Only `sawbot-common` and `sawbot-forge-1.8.9` participate in the Phase 0 Gradle build; trainer, simulator, arena, and inspection-tool directories remain documented placeholders until their gated phases begin.
