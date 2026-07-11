#!/usr/bin/env python3
"""Validate the releasable SawBotV1 Forge JAR before publishing it."""
from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path

REQUIRED_ENTRIES = {
    "mcmod.info",
    "dev/fivesaw/sawbot/forge/SawBotMod.class",
    "dev/fivesaw/sawbot/common/action/ActionCommand.class",
    "dev/fivesaw/sawbot/common/observation/ObservationSnapshot.class",
    "dev/fivesaw/sawbot/common/observation/EntityType.class",
    "dev/fivesaw/sawbot/common/observation/LocalTerrainSnapshot.class",
    "dev/fivesaw/sawbot/forge/sensors/ObservationPipeline.class",
    "dev/fivesaw/sawbot/forge/sensors/LocalTerrainSensor.class",
    "dev/fivesaw/sawbot/forge/tracking/EntityTrackerSensor.class",
    "dev/fivesaw/sawbot/forge/tracking/EntityTypeClassifier.class",
    "dev/fivesaw/sawbot/forge/tracking/VisibilitySampler.class",
    "dev/fivesaw/sawbot/common/observation/ObservationDiffCalculator.class",
    "dev/fivesaw/sawbot/forge/inspection/InspectorController.class",
    "dev/fivesaw/sawbot/forge/inspection/SnapshotExportService.class",
    "dev/fivesaw/sawbot/forge/hud/WorldDebugRenderer.class",
    "dev/fivesaw/sawbot/forge/hud/EntityVisualStyle.class",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", type=Path)
    parser.add_argument("--expected-version", required=True)
    args = parser.parse_args()

    jar = args.jar.resolve()
    if not jar.is_file():
        print(f"ERROR: release JAR does not exist: {jar}", file=sys.stderr)
        return 2

    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        missing = sorted(REQUIRED_ENTRIES - names)
        if missing:
            print("ERROR: release JAR is missing required entries:", file=sys.stderr)
            for name in missing:
                print(f"  - {name}", file=sys.stderr)
            return 3

        if any(name.endswith(".java") for name in names):
            print("ERROR: release JAR unexpectedly contains Java source files.", file=sys.stderr)
            return 4

        metadata = archive.read("mcmod.info").decode("utf-8")
        parsed = json.loads(metadata)
        if not isinstance(parsed, list) or not parsed:
            print("ERROR: mcmod.info has an invalid root structure.", file=sys.stderr)
            return 5
        metadata = parsed[0]
        if metadata.get("modid") != "sawbotv1":
            print("ERROR: mcmod.info modid is not 'sawbotv1'.", file=sys.stderr)
            return 6
        if metadata.get("mcversion") != "1.8.9":
            print("ERROR: mcmod.info Minecraft version is not '1.8.9'.", file=sys.stderr)
            return 7
        version = metadata.get("version")
        if version != args.expected_version:
            print(
                f"ERROR: mcmod.info version is {version!r}; expected {args.expected_version!r}.",
                file=sys.stderr,
            )
            return 8

    print(f"PASS release JAR verification: {jar.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
