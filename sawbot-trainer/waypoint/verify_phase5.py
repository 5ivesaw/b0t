#!/usr/bin/env python3
"""Pure-standard-library verification for the Phase 5 learned waypoint policy."""
from __future__ import annotations

import gzip
import json
import math
import statistics
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

from bridge_codec import ACTION_STRUCT, USER_WAYPOINT_ID, decode_observation, encode_action
from live_features import features_from_observation
from waypoint_core import ACTION_NAMES, FEATURE_NAMES, TinyMlp, action_to_motor


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("FAIL Phase 5 verification: " + message)


def main() -> int:
    checkpoint_path = HERE / "checkpoints" / "waypoint_v0.1.json"
    dataset_path = HERE / "datasets" / "teacher_waypoint_v0.1.jsonl.gz"
    evaluation_path = HERE / "evaluation" / "waypoint_eval_v0.1.json"
    failures_path = HERE / "evaluation" / "waypoint_failures_v0.1.jsonl"

    model = TinyMlp.load(checkpoint_path)
    require(model.model_version == "SawBotV1-Waypoint-0.1", "model version")
    require(list(model.feature_names) == FEATURE_NAMES, "feature ordering")
    require(list(model.action_names) == ACTION_NAMES, "action ordering")
    require(len(model.weight1) == 32, "hidden width is not 32")
    require(all(len(row) == len(FEATURE_NAMES) for row in model.weight1), "w1 shape")
    require(len(model.weight2) == len(ACTION_NAMES), "w2 output shape")
    require(all(len(row) == 32 for row in model.weight2), "w2 hidden shape")

    with gzip.open(dataset_path, "rt", encoding="utf-8") as stream:
        header_record = json.loads(next(stream))
        header = header_record.get("header", {})
        require(header.get("format") == "sawbot.waypoint.teacher/0.1", "teacher format")
        require(header.get("featureNames") == FEATURE_NAMES, "dataset features")
        require(header.get("actionNames") == ACTION_NAMES, "dataset actions")
        require(header.get("rows") == 28000, "dataset row declaration")
        require(header.get("classCounts") == [4000] * len(ACTION_NAMES), "dataset balance")
        counted = 0
        counts = [0] * len(ACTION_NAMES)
        sample_features = None
        for line in stream:
            record = json.loads(line)
            x = record.get("x")
            y = record.get("y")
            require(isinstance(x, list) and len(x) == len(FEATURE_NAMES), "dataset feature row")
            require(isinstance(y, int) and 0 <= y < len(ACTION_NAMES), "dataset label")
            require(all(math.isfinite(float(value)) for value in x), "dataset non-finite value")
            counts[y] += 1
            counted += 1
            if sample_features is None:
                sample_features = [float(value) for value in x]
        require(counted == 28000, "dataset row count")
        require(counts == [4000] * len(ACTION_NAMES), "observed class balance")

    evaluation = json.loads(evaluation_path.read_text(encoding="utf-8"))
    require(evaluation.get("format") == "sawbot.waypoint.evaluation/0.1", "evaluation format")
    require(evaluation.get("heldOutStartingPositions") is True, "held-out starts")
    require(evaluation.get("runtimePathfinder") is False, "runtime pathfinder must be false")
    model_rate = float(evaluation["model"]["successRate"])
    random_rate = float(evaluation["randomBaseline"]["successRate"])
    require(model_rate >= 0.80, "held-out success below 80%")
    require(model_rate >= random_rate * 8.0, "model does not clearly beat random baseline")
    require(int(evaluation["model"]["episodes"]) >= 500, "too few held-out episodes")
    require(float(evaluation["cpuInference"]["averageMicros"]) < 5000.0, "inference budget")
    require(failures_path.is_file() and failures_path.stat().st_size > 0, "failure examples missing")

    assert sample_features is not None
    timings = []
    for _ in range(2000):
        started = time.perf_counter_ns()
        action_index, confidence, probabilities = model.predict(sample_features)
        timings.append(time.perf_counter_ns() - started)
    require(0 <= action_index < len(ACTION_NAMES), "predicted action index")
    require(0.0 <= confidence <= 1.0, "confidence bounds")
    require(abs(sum(probabilities) - 1.0) < 1e-6, "probability normalization")
    require(all(0.0 <= value <= 1.0 for value in probabilities), "probability bounds")
    motor = action_to_motor(action_index)
    require(1 <= int(motor["duration"]) <= 20, "motor duration")
    require(-1.0 <= float(motor["forward"]) <= 1.0, "motor forward")
    require(-1.0 <= float(motor["strafe"]) <= 1.0, "motor strafe")

    observation_fixture = HERE.parents[1] / "build" / "phase5-observation-fixture.bin"
    if observation_fixture.is_file():
        decoded = decode_observation(observation_fixture.read_bytes())
        require(decoded.sequence == 7 and decoded.client_tick == 20, "bridge observation identity")
        user_waypoint = decoded.user_waypoint()
        require(user_waypoint is not None, "user waypoint missing from decoded observation")
        require(user_waypoint["id"] == USER_WAYPOINT_ID, "decoded waypoint ID")
        live_features = features_from_observation(decoded)
        require(live_features is not None and len(live_features) == len(FEATURE_NAMES), "live feature extraction")
        require(all(math.isfinite(float(value)) for value in live_features), "live feature finiteness")
        live_action, live_confidence, live_probabilities = model.predict(live_features)
        require(0 <= live_action < len(ACTION_NAMES), "live fixture prediction")
        require(abs(sum(live_probabilities) - 1.0) < 1e-6, "live fixture probability normalization")

    payload = encode_action(
        123,
        forward=float(motor["forward"]),
        strafe=float(motor["strafe"]),
        yaw=float(motor["yaw"]),
        jump=float(motor["jump"]),
        sprint=float(motor["sprint"]),
        confidence=float(confidence),
        duration=int(motor["duration"]),
    )
    require(len(payload) == ACTION_STRUCT.size, "bridge action payload size")
    unpacked = ACTION_STRUCT.unpack(payload)
    require(unpacked[0] == 123, "bridge observation sequence")
    require(unpacked[15] == USER_WAYPOINT_ID, "bridge waypoint reference")
    require(all(math.isfinite(float(value)) for value in unpacked[1:12]), "bridge non-finite action")

    average_micros = statistics.fmean(timings) / 1000.0
    require(average_micros < 5000.0, "live Python inference exceeded 5 ms")
    print(
        "PASS Phase 5 learned waypoint verification "
        f"(dataset={counted}, held_out={model_rate:.4f}, random={random_rate:.4f}, "
        f"runtime={average_micros:.2f} us)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
