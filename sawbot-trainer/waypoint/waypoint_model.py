#!/usr/bin/env python3
from __future__ import annotations

import argparse
import socket
import time
from pathlib import Path

from bridge_codec import (
    TYPE_ACTION,
    TYPE_GOODBYE,
    TYPE_HELLO,
    TYPE_HELLO_ACK,
    TYPE_OBSERVATION,
    TYPE_PING,
    TYPE_PONG,
    decode_hello,
    decode_observation,
    encode_action,
    encode_hello_ack,
    read_frame,
    write_frame,
)
from live_features import features_from_observation
from waypoint_core import ACTION_NAMES, TinyMlp, action_to_motor


def serve(host: str, port: int, model_path: Path) -> int:
    model = TinyMlp.load(model_path)
    print("SawBotV1 Phase 5 learned waypoint model")
    print(f"Model: {model.model_version}")
    print(f"Listening: {host}:{port}")
    print("Set a target in Minecraft with G, start the model, then enable with F10.")

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((host, port))
        server.listen(1)
        while True:
            connection, address = server.accept()
            print(f"Client connected: {address[0]}:{address[1]}")
            with connection:
                connection.settimeout(6.0)
                try:
                    frame_type, payload = read_frame(connection)
                    if frame_type != TYPE_HELLO:
                        raise RuntimeError("expected HELLO")
                    hello = decode_hello(payload)
                    write_frame(connection, TYPE_HELLO_ACK, encode_hello_ack(hello.nonce, model.model_version))
                    print(
                        f"Handshake: mod={hello.mod_version} obs={hello.observation_schema} "
                        f"action={hello.action_schema} rate={hello.decision_rate}Hz"
                    )
                    connection.settimeout(2.0)
                    last_status = ""
                    while True:
                        frame_type, payload = read_frame(connection)
                        if frame_type == TYPE_PING:
                            write_frame(connection, TYPE_PONG, payload)
                            continue
                        if frame_type == TYPE_GOODBYE:
                            break
                        if frame_type != TYPE_OBSERVATION:
                            continue
                        observation = decode_observation(payload)
                        features = features_from_observation(observation)
                        if features is None:
                            status = "waiting for G waypoint"
                            action_payload = encode_action(
                                observation.sequence,
                                forward=0.0,
                                strafe=0.0,
                                yaw=0.0,
                                confidence=1.0,
                                duration=2,
                                waypoint=-1,
                                skill=0,
                                objective=0,
                            )
                        else:
                            action_index, confidence, probabilities = model.predict(features)
                            motor = action_to_motor(action_index)
                            status = (
                                f"#{observation.sequence} {ACTION_NAMES[action_index]} "
                                f"p={confidence:.3f} dist={observation.user_waypoint()['distance']:.2f}m"
                            )
                            action_payload = encode_action(
                                observation.sequence,
                                forward=float(motor["forward"]),
                                strafe=float(motor["strafe"]),
                                yaw=float(motor["yaw"]),
                                jump=float(motor["jump"]),
                                sprint=float(motor["sprint"]),
                                confidence=float(confidence),
                                duration=int(motor["duration"]),
                            )
                        write_frame(connection, TYPE_ACTION, action_payload)
                        if status != last_status and (features is None or observation.sequence % 10 == 0):
                            print(status)
                            last_status = status
                except (EOFError, ConnectionError, OSError, RuntimeError) as exc:
                    print(f"Client disconnected: {exc}")
                    time.sleep(0.25)


def main() -> int:
    parser = argparse.ArgumentParser()
    base = Path(__file__).parent
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25189)
    parser.add_argument("--model", type=Path, default=base / "checkpoints" / "waypoint_v0.1.json")
    args = parser.parse_args()
    return serve(args.host, args.port, args.model)


if __name__ == "__main__":
    raise SystemExit(main())
