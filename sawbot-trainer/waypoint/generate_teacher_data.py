#!/usr/bin/env python3
from __future__ import annotations

import argparse
import gzip
import json
import math
import random
from pathlib import Path

from waypoint_core import ACTION_NAMES, FEATURE_NAMES, teacher_action


def sample_features(rng: random.Random) -> list[float]:
    angle = rng.uniform(-math.pi, math.pi)
    distance = rng.uniform(0.2, 20.0)
    target_right = math.sin(angle) * min(1.0, distance / 12.0)
    target_forward = math.cos(angle) * min(1.0, distance / 12.0)
    target_up = rng.uniform(-0.35, 0.35)
    target_distance = min(1.0, distance / 20.0)

    support_left = rng.betavariate(5.0, 1.3)
    support_center = rng.betavariate(6.0, 1.2)
    support_right = rng.betavariate(5.0, 1.3)
    void_clearance = rng.random()

    front_left = 1.0 if rng.random() > 0.12 else 0.0
    front_center = 1.0 if rng.random() > 0.14 else 0.0
    front_right = 1.0 if rng.random() > 0.12 else 0.0
    landing_two = 1.0 if rng.random() > 0.10 else 0.0
    obstacle_feet = 1.0 if rng.random() < 0.14 else 0.0
    obstacle_head = 1.0 if obstacle_feet and rng.random() < 0.25 else 0.0
    hazard_front = 1.0 if rng.random() < 0.035 else 0.0
    on_ground = 1.0 if rng.random() > 0.06 else 0.0
    horizontal_collision = 1.0 if obstacle_feet and rng.random() < 0.7 else 0.0
    velocity_forward = rng.uniform(-0.35, 1.0)

    if hazard_front:
        front_center = 0.0
    if not front_center and rng.random() < 0.6:
        landing_two = 0.0

    return [
        target_right,
        target_forward,
        target_up,
        target_distance,
        support_left,
        support_center,
        support_right,
        void_clearance,
        front_left,
        front_center,
        front_right,
        landing_two,
        obstacle_feet,
        obstacle_head,
        hazard_front,
        on_ground,
        horizontal_collision,
        velocity_forward,
    ]


def generate(output: Path, total: int, seed: int) -> dict:
    rng = random.Random(seed)
    per_class = max(1, total // len(ACTION_NAMES))
    counts = [0] * len(ACTION_NAMES)
    rows: list[tuple[list[float], int]] = []
    attempts = 0
    while len(rows) < per_class * len(ACTION_NAMES):
        attempts += 1
        features = sample_features(rng)
        label = teacher_action(features)
        if counts[label] >= per_class:
            continue
        counts[label] += 1
        rows.append((features, label))
        if attempts > total * 200:
            raise RuntimeError("could not balance teacher dataset")
    rng.shuffle(rows)

    output.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(output, "wt", encoding="utf-8", newline="\n") as stream:
        header = {
            "format": "sawbot.waypoint.teacher/0.1",
            "seed": seed,
            "featureNames": FEATURE_NAMES,
            "actionNames": ACTION_NAMES,
            "rows": len(rows),
            "classCounts": counts,
        }
        stream.write(json.dumps({"header": header}, separators=(",", ":")) + "\n")
        for features, label in rows:
            stream.write(json.dumps({"x": features, "y": label}, separators=(",", ":")) + "\n")
    return header


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path(__file__).parent / "datasets" / "teacher_waypoint_v0.1.jsonl.gz")
    parser.add_argument("--rows", type=int, default=28000)
    parser.add_argument("--seed", type=int, default=5102026)
    args = parser.parse_args()
    header = generate(args.output, args.rows, args.seed)
    print(json.dumps(header, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
