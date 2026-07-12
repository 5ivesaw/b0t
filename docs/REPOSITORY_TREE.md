# Repository tree — Phase 9 segmented navigation candidate

```text
.github/workflows/ci.yml
.github/workflows/release.yml
README.md
CHANGELOG.md
gradle.properties
build.gradle
docs/PHASE9_REPORT.md
docs/SEGMENTED_NAVIGATION_CORE.md
docs/BARITONE_ARCHITECTURE_RESEARCH.md
docs/NAVIGATION_BODY.md
docs/HYBRID_ARCHITECTURE.md
docs/BRIDGING_BODY.md
docs/PHASE_GATES.md
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/ImmutableNavigationGrid.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementAStarPlanner.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementPath.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/MovementPlanResult.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationMovement.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationMovementType.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/NavigationProgressWatchdog.java
sawbot-common/src/main/java/dev/fivesaw/sawbot/common/navigation/PathSegmentCoordinator.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationBodyController.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationCameraController.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationMovementExecutor.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationPlannerWorker.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/NavigationSnapshotCapture.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/navigation/WorldNavigationGrid.java
sawbot-forge-1.8.9/src/main/java/dev/fivesaw/sawbot/forge/bridging/BridgingBodyController.java
sawbot-trainer/waypoint/RUN-WAYPOINT-MODEL.bat
sawbot-tools/dummy-model/RUN-DUMMY-MODEL.bat
verification-tests/src/dev/fivesaw/sawbot/verification/SegmentedNavigationContractTest.java
verification-tests/src/dev/fivesaw/sawbot/verification/NavigationBodyContractTest.java
verification-tests/src/dev/fivesaw/sawbot/verification/BridgingBodyContractTest.java
tools/offline-verify.sh
tools/package-release.sh
tools/verify-built-jar.py
tools/verify-release-payload.sh
```

Only the primary Phase 9 and retained runtime/release files are listed here.
