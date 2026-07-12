# Repository tree — Phase 8 bridging specialist candidate

```text
SawBotV1/
├── .github/workflows/{ci.yml,release.yml}
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
│   ├── HYBRID_ARCHITECTURE.md
│   ├── ADAPTIVE_NAVIGATION.md
│   ├── NAVIGATION_BODY.md
│   ├── BRIDGING_BODY.md
│   ├── PERFORMANCE_BUDGET.md
│   ├── OBSERVATION_CONTRACT.md
│   ├── ACTION_CONTRACT.md
│   ├── TELEMETRY_FORMAT.md
│   ├── PHASE_GATES.md
│   ├── PHASE8_REPORT.md
│   ├── PHASE7_REPORT.md
│   └── prior reports, acceptance, release, audit, and historical evidence
├── sawbot-common/src/main/java/dev/fivesaw/sawbot/common/
│   ├── action/
│   ├── bridging/        # bounded corridor and placement-step contract
│   ├── events/
│   ├── navigation/      # cells/path, adaptive cursor, bounded anytime A*
│   ├── observation/
│   ├── telemetry/
│   ├── protocol/
│   └── versioning/
├── sawbot-forge-1.8.9/src/main/
│   ├── java/dev/fivesaw/sawbot/forge/
│   │   ├── actuator/    # validated fallback low-level action execution
│   │   ├── bridging/    # legal placement/confirmation/advance body
│   │   ├── client/      # specialist ownership and runtime priority
│   │   ├── config/
│   │   ├── hud/         # compact HUD + navigation/bridge rendering
│   │   ├── inspection/
│   │   ├── map/
│   │   ├── model/       # bounded loopback brain transport
│   │   ├── navigation/  # live world grid + adaptive navigation body
│   │   ├── performance/
│   │   ├── safety/
│   │   ├── sensors/
│   │   ├── telemetry/
│   │   └── tracking/
│   └── resources/
├── sawbot-tools/
├── sawbot-trainer/waypoint/  # preserved Phase 5 learned baseline
├── sawbot-sim/
├── sawbot-arenas/
├── prototypes/control-center.html
├── verification-stubs/
├── verification-tests/
│   ├── .../NavigationBodyContractTest.java
│   └── .../BridgingBodyContractTest.java
└── tools/
    ├── offline-verify.sh
    ├── package-release.sh
    ├── verify-built-jar.py
    ├── verify-release-payload.sh
    └── local telemetry/bootstrap/preflight scripts
```

Only `sawbot-common` and `sawbot-forge-1.8.9` participate in the mod build. World
access and mechanical specialist execution remain on the Minecraft client thread.
Search, corridor size, placement attempts, confirmation waits, route windows, caches,
and per-tick turn rates have explicit bounds.
