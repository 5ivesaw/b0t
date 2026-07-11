"""Shared Phase 5 waypoint teacher, feature contract, and tiny MLP runtime."""
from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

MODEL_FORMAT = "sawbot.waypoint.mlp/0.1"
MODEL_VERSION = "SawBotV1-Waypoint-0.1"
USER_WAYPOINT_ID = 1000

FEATURE_NAMES = [
    "target_right_norm",
    "target_forward_norm",
    "target_up_norm",
    "target_distance_norm",
    "support_left_safe",
    "support_center_safe",
    "support_right_safe",
    "void_clearance_norm",
    "front_left_safe",
    "front_center_safe",
    "front_right_safe",
    "landing_two_ahead_safe",
    "obstacle_feet",
    "obstacle_head",
    "hazard_front",
    "on_ground",
    "horizontal_collision",
    "velocity_forward_norm",
]

ACTION_NAMES = [
    "STOP",
    "TURN_LEFT",
    "TURN_RIGHT",
    "FORWARD",
    "FORWARD_LEFT",
    "FORWARD_RIGHT",
    "JUMP_FORWARD",
]

STOP, TURN_LEFT, TURN_RIGHT, FORWARD, FORWARD_LEFT, FORWARD_RIGHT, JUMP_FORWARD = range(7)


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def safe_score(distance: float) -> float:
    """1 means support is directly under the sample; 0 means no nearby support."""
    return 1.0 - clamp(float(distance) / 4.0, 0.0, 1.0)


def teacher_action(features: Sequence[float]) -> int:
    if len(features) != len(FEATURE_NAMES):
        raise ValueError("feature length")
    (
        target_right,
        target_forward,
        _target_up,
        target_distance,
        _support_left,
        support_center,
        _support_right,
        _void_clearance,
        front_left,
        front_center,
        front_right,
        landing_two,
        obstacle_feet,
        obstacle_head,
        hazard_front,
        on_ground,
        horizontal_collision,
        _velocity_forward,
    ) = features

    if target_distance <= 0.065:
        return STOP

    angle = math.atan2(target_right, max(0.025, target_forward))
    target_behind = target_forward < -0.03

    # Never hide an edge failure with runtime correction: the teacher simply labels
    # a turn/stop action and the trained model must learn that decision boundary.
    unsafe_ahead = front_center < 0.45 or hazard_front > 0.5
    if unsafe_ahead:
        if front_left > front_right + 0.15:
            return TURN_LEFT
        if front_right > front_left + 0.15:
            return TURN_RIGHT
        if abs(angle) > 0.12:
            return TURN_RIGHT if angle > 0.0 else TURN_LEFT
        return STOP

    blocked = obstacle_feet > 0.5 or horizontal_collision > 0.5
    head_blocked = obstacle_head > 0.5
    if blocked:
        if on_ground > 0.5 and not head_blocked and landing_two > 0.55:
            return JUMP_FORWARD
        if front_left > front_right + 0.08:
            return TURN_LEFT
        if front_right > front_left + 0.08:
            return TURN_RIGHT
        return TURN_RIGHT if angle >= 0.0 else TURN_LEFT

    if support_center < 0.25:
        return STOP
    if target_behind or angle < -0.58:
        return TURN_LEFT
    if angle > 0.58:
        return TURN_RIGHT
    if angle < -0.12:
        return FORWARD_LEFT
    if angle > 0.12:
        return FORWARD_RIGHT
    return FORWARD


def action_to_motor(action_index: int) -> dict[str, float | int]:
    if action_index == STOP:
        return {"forward": 0.0, "strafe": 0.0, "yaw": 0.0, "jump": 0.0, "sprint": 0.0, "duration": 2}
    if action_index == TURN_LEFT:
        return {"forward": 0.0, "strafe": 0.0, "yaw": -13.0, "jump": 0.0, "sprint": 0.0, "duration": 3}
    if action_index == TURN_RIGHT:
        return {"forward": 0.0, "strafe": 0.0, "yaw": 13.0, "jump": 0.0, "sprint": 0.0, "duration": 3}
    if action_index == FORWARD:
        return {"forward": 1.0, "strafe": 0.0, "yaw": 0.0, "jump": 0.0, "sprint": 1.0, "duration": 2}
    if action_index == FORWARD_LEFT:
        return {"forward": 1.0, "strafe": -0.20, "yaw": -4.0, "jump": 0.0, "sprint": 0.0, "duration": 2}
    if action_index == FORWARD_RIGHT:
        return {"forward": 1.0, "strafe": 0.20, "yaw": 4.0, "jump": 0.0, "sprint": 0.0, "duration": 2}
    if action_index == JUMP_FORWARD:
        return {"forward": 1.0, "strafe": 0.0, "yaw": 0.0, "jump": 1.0, "sprint": 0.0, "duration": 2}
    raise ValueError("action index")


@dataclass(frozen=True)
class TinyMlp:
    feature_names: tuple[str, ...]
    action_names: tuple[str, ...]
    mean: tuple[float, ...]
    scale: tuple[float, ...]
    weight1: tuple[tuple[float, ...], ...]
    bias1: tuple[float, ...]
    weight2: tuple[tuple[float, ...], ...]
    bias2: tuple[float, ...]
    model_version: str

    @classmethod
    def load(cls, path: Path) -> "TinyMlp":
        payload = json.loads(path.read_text(encoding="utf-8"))
        if payload.get("format") != MODEL_FORMAT:
            raise ValueError("unsupported waypoint model format")
        if payload.get("featureNames") != FEATURE_NAMES:
            raise ValueError("waypoint feature contract mismatch")
        if payload.get("actionNames") != ACTION_NAMES:
            raise ValueError("waypoint action contract mismatch")
        return cls(
            feature_names=tuple(payload["featureNames"]),
            action_names=tuple(payload["actionNames"]),
            mean=tuple(float(v) for v in payload["normalization"]["mean"]),
            scale=tuple(float(v) for v in payload["normalization"]["scale"]),
            weight1=tuple(tuple(float(v) for v in row) for row in payload["weights"]["w1"]),
            bias1=tuple(float(v) for v in payload["weights"]["b1"]),
            weight2=tuple(tuple(float(v) for v in row) for row in payload["weights"]["w2"]),
            bias2=tuple(float(v) for v in payload["weights"]["b2"]),
            model_version=str(payload["modelVersion"]),
        )

    def predict_logits(self, features: Sequence[float]) -> list[float]:
        if len(features) != len(self.feature_names):
            raise ValueError("feature length")
        normalized = [
            (float(value) - self.mean[index]) / self.scale[index]
            for index, value in enumerate(features)
        ]
        hidden: list[float] = []
        for row, bias in zip(self.weight1, self.bias1):
            total = bias
            for weight, value in zip(row, normalized):
                total += weight * value
            hidden.append(math.tanh(total))
        logits: list[float] = []
        for row, bias in zip(self.weight2, self.bias2):
            total = bias
            for weight, value in zip(row, hidden):
                total += weight * value
            logits.append(total)
        return logits

    def predict(self, features: Sequence[float]) -> tuple[int, float, list[float]]:
        logits = self.predict_logits(features)
        maximum = max(logits)
        exponents = [math.exp(clamp(value - maximum, -40.0, 0.0)) for value in logits]
        total = sum(exponents)
        probabilities = [value / total for value in exponents]
        index = max(range(len(probabilities)), key=probabilities.__getitem__)
        return index, probabilities[index], probabilities
