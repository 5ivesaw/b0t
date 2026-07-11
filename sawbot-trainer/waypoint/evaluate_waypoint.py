#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import math
import random
import statistics
import time
from pathlib import Path

from waypoint_core import (
    ACTION_NAMES,
    FORWARD,
    FORWARD_LEFT,
    FORWARD_RIGHT,
    JUMP_FORWARD,
    STOP,
    TURN_LEFT,
    TURN_RIGHT,
    TinyMlp,
    clamp,
)


class Arena:
    def __init__(self, rng: random.Random) -> None:
        self.safe = {(x, z) for x in range(-13, 14) for z in range(-13, 14)}
        # Edge notches and sparse holes; no hidden correction is applied if a policy falls.
        for _ in range(rng.randint(2, 7)):
            cx, cz = rng.randint(-10, 10), rng.randint(-10, 10)
            for dx in range(rng.randint(1, 2)):
                for dz in range(rng.randint(1, 2)):
                    self.safe.discard((cx + dx, cz + dz))
        self.obstacles = set()
        for _ in range(rng.randint(1, 5)):
            cell = (rng.randint(-9, 9), rng.randint(-9, 9))
            if cell in self.safe:
                self.obstacles.add(cell)

    def is_safe(self, x: float, z: float) -> bool:
        return (math.floor(x), math.floor(z)) in self.safe

    def blocked(self, x: float, z: float) -> bool:
        return (math.floor(x), math.floor(z)) in self.obstacles


def sample_cell(a: Arena, x: float, z: float, heading: float, right: int, forward: int) -> tuple[float, float]:
    fx, fz = math.sin(heading), math.cos(heading)
    rx, rz = math.cos(heading), -math.sin(heading)
    return x + fx * forward + rx * right, z + fz * forward + rz * right


def features(a: Arena, x: float, z: float, heading: float, tx: float, tz: float, collided: bool) -> list[float]:
    dx, dz = tx - x, tz - z
    fx, fz = math.sin(heading), math.cos(heading)
    rx, rz = math.cos(heading), -math.sin(heading)
    right = dx * rx + dz * rz
    forward = dx * fx + dz * fz
    distance = math.hypot(dx, dz)

    def safe_at(r: int, f: int) -> float:
        px, pz = sample_cell(a, x, z, heading, r, f)
        return 1.0 if a.is_safe(px, pz) else 0.0

    feet_x, feet_z = sample_cell(a, x, z, heading, 0, 1)
    obstacle = 1.0 if a.blocked(feet_x, feet_z) else 0.0
    return [
        clamp(right / 12.0, -1.0, 1.0),
        clamp(forward / 12.0, -1.0, 1.0),
        0.0,
        clamp(distance / 20.0, 0.0, 1.0),
        1.0,
        1.0,
        1.0,
        1.0,
        safe_at(-1, 1),
        safe_at(0, 1),
        safe_at(1, 1),
        safe_at(0, 2),
        obstacle,
        0.0,
        0.0,
        1.0,
        1.0 if collided else 0.0,
        0.5,
    ]


def rollout(a: Arena, model: TinyMlp | None, rng: random.Random, max_steps: int = 240) -> dict:
    safe_cells = list(a.safe - a.obstacles)
    start = rng.choice(safe_cells)
    target = rng.choice(safe_cells)
    while math.hypot(target[0] - start[0], target[1] - start[1]) < 7.0:
        target = rng.choice(safe_cells)
    x, z = start[0] + 0.5, start[1] + 0.5
    tx, tz = target[0] + 0.5, target[1] + 0.5

    # Phase 5 evaluates local waypoint following, not global maze solving. Keep a
    # reachable two-cell-wide corridor between held-out start and target while
    # preserving unrelated edge holes. Some episodes retain one jumpable obstacle.
    segments = max(2, int(math.hypot(tx - x, tz - z) * 3.0))
    corridor = []
    for index in range(segments + 1):
        alpha = index / float(segments)
        cx = math.floor(x + (tx - x) * alpha)
        cz = math.floor(z + (tz - z) * alpha)
        for ox, oz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            cell = (cx + ox, cz + oz)
            a.safe.add(cell)
            a.obstacles.discard(cell)
        corridor.append((cx, cz))
    if rng.random() < 0.20 and len(corridor) > 8:
        obstacle = corridor[len(corridor) // 2]
        if obstacle != start and obstacle != target:
            a.obstacles.add(obstacle)

    heading = rng.uniform(-math.pi, math.pi)
    collided = False
    minimum_distance = math.hypot(tx - x, tz - z)

    for step in range(max_steps):
        vector = features(a, x, z, heading, tx, tz, collided)
        if model is None:
            action = rng.randrange(len(ACTION_NAMES))
        else:
            action, _confidence, _probabilities = model.predict(vector)

        if action == TURN_LEFT:
            heading -= math.radians(15.0)
        elif action == TURN_RIGHT:
            heading += math.radians(15.0)

        move = 0.0
        turn = 0.0
        if action == FORWARD:
            move = 0.36
        elif action == FORWARD_LEFT:
            move, turn = 0.34, -math.radians(5.0)
        elif action == FORWARD_RIGHT:
            move, turn = 0.34, math.radians(5.0)
        elif action == JUMP_FORWARD:
            move = 0.68
        heading += turn

        collided = False
        if move > 0.0:
            nx = x + math.sin(heading) * move
            nz = z + math.cos(heading) * move
            if not a.is_safe(nx, nz):
                return {"success": False, "failure": "fall", "steps": step + 1, "minimumDistance": minimum_distance}
            if a.blocked(nx, nz):
                if action == JUMP_FORWARD:
                    landing_x = x + math.sin(heading) * 1.25
                    landing_z = z + math.cos(heading) * 1.25
                    if a.is_safe(landing_x, landing_z) and not a.blocked(landing_x, landing_z):
                        x, z = landing_x, landing_z
                    else:
                        collided = True
                else:
                    collided = True
            else:
                x, z = nx, nz

        distance = math.hypot(tx - x, tz - z)
        minimum_distance = min(minimum_distance, distance)
        if distance <= 1.35:
            return {"success": True, "failure": "", "steps": step + 1, "minimumDistance": minimum_distance}

    return {"success": False, "failure": "timeout", "steps": max_steps, "minimumDistance": minimum_distance}


def benchmark(model: TinyMlp, vector: list[float], iterations: int = 10000) -> dict:
    start = time.perf_counter_ns()
    for _ in range(iterations):
        model.predict(vector)
    elapsed = time.perf_counter_ns() - start
    return {
        "iterations": iterations,
        "averageMicros": elapsed / iterations / 1000.0,
        "totalMillis": elapsed / 1_000_000.0,
    }


def evaluate(model_path: Path, output: Path, failures_path: Path, seed: int, episodes: int) -> dict:
    model = TinyMlp.load(model_path)
    rng = random.Random(seed)
    model_results = []
    random_results = []
    failures = []
    for episode in range(episodes):
        arena_seed = rng.randrange(1 << 30)
        arena = Arena(random.Random(arena_seed))
        model_result = rollout(arena, model, random.Random(arena_seed ^ 0x51A7))
        random_result = rollout(arena, None, random.Random(arena_seed ^ 0xBADA55))
        model_results.append(model_result)
        random_results.append(random_result)
        if not model_result["success"] and len(failures) < 64:
            failures.append({"episode": episode, "arenaSeed": arena_seed, **model_result})

    def summarize(results: list[dict]) -> dict:
        successes = [item for item in results if item["success"]]
        failure_counts = {}
        for item in results:
            if item["failure"]:
                failure_counts[item["failure"]] = failure_counts.get(item["failure"], 0) + 1
        return {
            "episodes": len(results),
            "successes": len(successes),
            "successRate": len(successes) / len(results),
            "meanStepsOnSuccess": statistics.mean([item["steps"] for item in successes]) if successes else None,
            "failureCounts": failure_counts,
            "meanMinimumDistance": statistics.mean(item["minimumDistance"] for item in results),
        }

    vector = features(Arena(random.Random(1)), 0.5, 0.5, 0.0, 8.5, 8.5, False)
    report = {
        "format": "sawbot.waypoint.evaluation/0.1",
        "modelVersion": model.model_version,
        "heldOutSeed": seed,
        "heldOutStartingPositions": True,
        "runtimePathfinder": False,
        "model": summarize(model_results),
        "randomBaseline": summarize(random_results),
        "cpuInference": benchmark(model, vector),
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    with failures_path.open("w", encoding="utf-8", newline="\n") as stream:
        for item in failures:
            stream.write(json.dumps(item, separators=(",", ":")) + "\n")
    return report


def main() -> int:
    parser = argparse.ArgumentParser()
    base = Path(__file__).parent
    parser.add_argument("--model", type=Path, default=base / "checkpoints" / "waypoint_v0.1.json")
    parser.add_argument("--output", type=Path, default=base / "evaluation" / "waypoint_eval_v0.1.json")
    parser.add_argument("--failures", type=Path, default=base / "evaluation" / "waypoint_failures_v0.1.jsonl")
    parser.add_argument("--seed", type=int, default=920260711)
    parser.add_argument("--episodes", type=int, default=800)
    args = parser.parse_args()
    report = evaluate(args.model, args.output, args.failures, args.seed, args.episodes)
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
