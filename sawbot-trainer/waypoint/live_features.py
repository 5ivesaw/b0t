"""Map a live ObservationView into the fixed waypoint feature vector."""
from __future__ import annotations

from bridge_codec import FLAG_HAZARD, FLAG_SAFE_SUPPORT, FLAG_SOLID, ObservationView
from waypoint_core import clamp, safe_score


def has_flag(observation: ObservationView, right: int, up: int, forward: int, flag: int) -> float:
    return 1.0 if (observation.flags_at(right, up, forward) & flag) != 0 else 0.0


def features_from_observation(observation: ObservationView) -> list[float] | None:
    waypoint = observation.user_waypoint()
    if waypoint is None or not waypoint["reachable"]:
        return None

    target_right = clamp(float(waypoint["right"]) / 12.0, -1.0, 1.0)
    target_forward = clamp(float(waypoint["forward"]) / 12.0, -1.0, 1.0)
    target_up = clamp(float(waypoint["up"]) / 6.0, -1.0, 1.0)
    target_distance = clamp(float(waypoint["distance"]) / 20.0, 0.0, 1.0)

    front_left = has_flag(observation, -1, -1, 1, FLAG_SAFE_SUPPORT)
    front_center = has_flag(observation, 0, -1, 1, FLAG_SAFE_SUPPORT)
    front_right = has_flag(observation, 1, -1, 1, FLAG_SAFE_SUPPORT)
    landing_two = has_flag(observation, 0, -1, 2, FLAG_SAFE_SUPPORT)
    obstacle_feet = has_flag(observation, 0, 0, 1, FLAG_SOLID)
    obstacle_head = max(
        has_flag(observation, 0, 1, 1, FLAG_SOLID),
        1.0 if observation.collision_at(0, 1, 1) > 0 else 0.0,
    )
    hazard_front = max(
        has_flag(observation, 0, -1, 1, FLAG_HAZARD),
        has_flag(observation, 0, 0, 1, FLAG_HAZARD),
    )

    return [
        target_right,
        target_forward,
        target_up,
        target_distance,
        safe_score(observation.support_left),
        safe_score(observation.support_center),
        safe_score(observation.support_right),
        clamp(float(observation.distance_to_void) / 16.0, 0.0, 1.0),
        front_left,
        front_center,
        front_right,
        landing_two,
        obstacle_feet,
        obstacle_head,
        hazard_front,
        1.0 if observation.on_ground else 0.0,
        1.0 if observation.horizontal_collision else 0.0,
        clamp(float(observation.velocity_forward) / 0.6, -1.0, 1.0),
    ]
