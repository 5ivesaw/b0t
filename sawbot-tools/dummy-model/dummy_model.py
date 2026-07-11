#!/usr/bin/env python3
"""Standard-library Phase 4 model-bridge server and deterministic actuator test model."""

from __future__ import annotations

import argparse
import dataclasses
import queue
import socket
import struct
import threading
import time
import zlib
from typing import Optional

PROTOCOL = "sawbot.bridge/0.1"
MODEL_VERSION = "dummy-model/0.1"
MAGIC = 0x53424D31
VERSION = 1
MAX_PAYLOAD = 262144
TYPE_HELLO = 1
TYPE_HELLO_ACK = 2
TYPE_OBSERVATION = 3
TYPE_ACTION = 4
TYPE_PING = 5
TYPE_PONG = 6
TYPE_ERROR = 7
TYPE_GOODBYE = 8
OBS_MAGIC = 0x3153424F
FRAME_HEADER = struct.Struct(">IBBHII")
ACTION_STRUCT = struct.Struct(">q11fbBiifBBB")
OBS_PREFIX = struct.Struct("<IHHqq")


class ProtocolError(RuntimeError):
    pass


def recv_exact(connection: socket.socket, length: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < length:
        chunk = connection.recv(length - len(chunks))
        if not chunk:
            raise EOFError("client disconnected")
        chunks.extend(chunk)
    return bytes(chunks)


def read_frame(connection: socket.socket) -> tuple[int, bytes]:
    header = recv_exact(connection, FRAME_HEADER.size)
    magic, version, frame_type, _flags, length, expected_crc = FRAME_HEADER.unpack(header)
    if magic != MAGIC:
        raise ProtocolError("frame magic mismatch")
    if version != VERSION:
        raise ProtocolError(f"unsupported frame version {version}")
    if length > MAX_PAYLOAD:
        raise ProtocolError(f"payload too large: {length}")
    payload = recv_exact(connection, length)
    if zlib.crc32(payload) & 0xFFFFFFFF != expected_crc:
        raise ProtocolError("payload CRC mismatch")
    return frame_type, payload


def write_frame(connection: socket.socket, frame_type: int, payload: bytes) -> None:
    if len(payload) > MAX_PAYLOAD:
        raise ProtocolError("outbound payload too large")
    header = FRAME_HEADER.pack(
        MAGIC,
        VERSION,
        frame_type,
        0,
        len(payload),
        zlib.crc32(payload) & 0xFFFFFFFF,
    )
    connection.sendall(header + payload)


def read_u16_text(payload: bytes, offset: int, maximum: int) -> tuple[str, int]:
    if offset + 2 > len(payload):
        raise ProtocolError("truncated string length")
    length = struct.unpack_from(">H", payload, offset)[0]
    offset += 2
    if length > maximum or offset + length > len(payload):
        raise ProtocolError("invalid string length")
    return payload[offset : offset + length].decode("utf-8"), offset + length


def write_u16_text(value: str, maximum: int) -> bytes:
    encoded = value.encode("utf-8")
    if len(encoded) > maximum:
        raise ProtocolError("string too long")
    return struct.pack(">H", len(encoded)) + encoded


@dataclasses.dataclass
class Action:
    forward: float = 0.0
    strafe: float = 0.0
    yaw: float = 0.0
    pitch: float = 0.0
    jump: float = 0.0
    sprint: float = 0.0
    sneak: float = 0.0
    attack: float = 0.0
    use: float = 0.0
    drop: float = 0.0
    inventory: float = 0.0
    hotbar: int = -1
    skill: int = 0
    target: int = -1
    waypoint: int = -1
    confidence: float = 1.0
    duration: int = 1
    objective: int = 0
    abort: int = 0

    def encode(self, observation_sequence: int) -> bytes:
        return ACTION_STRUCT.pack(
            observation_sequence,
            self.forward,
            self.strafe,
            self.yaw,
            self.pitch,
            self.jump,
            self.sprint,
            self.sneak,
            self.attack,
            self.use,
            self.drop,
            self.inventory,
            self.hotbar,
            self.skill,
            self.target,
            self.waypoint,
            self.confidence,
            self.duration,
            self.objective,
            self.abort,
        )

    def summary(self) -> str:
        bits = "".join(
            token
            for token, value in (
                ("J", self.jump),
                ("S", self.sprint),
                ("N", self.sneak),
                ("A", self.attack),
                ("U", self.use),
                ("D", self.drop),
                ("I", self.inventory),
            )
            if value >= 0.5
        )
        return (
            f"f={self.forward:+.1f} s={self.strafe:+.1f} "
            f"yaw={self.yaw:+.1f} pitch={self.pitch:+.1f} "
            f"buttons={bits or '-'} slot={self.hotbar + 1 if self.hotbar >= 0 else '-'}"
        )


class CommandController:
    def __init__(self, mode: str) -> None:
        self.mode = mode
        self.action = Action()
        self.pulse: Optional[Action] = None
        self.demo_start: Optional[int] = 0 if mode == "demo" else None
        self.lock = threading.Lock()
        self.stop_requested = False

    def action_for(self, observation_count: int) -> Action:
        with self.lock:
            if self.pulse is not None:
                action = self.pulse
                self.pulse = None
                return action
            if self.demo_start is not None:
                return self.demo_action(observation_count - self.demo_start)
            return dataclasses.replace(self.action)

    @staticmethod
    def demo_action(index: int) -> Action:
        stages = [
            Action(),
            Action(forward=1.0, duration=2, skill=1),
            Action(forward=-1.0, duration=2, skill=1),
            Action(strafe=-1.0, duration=2, skill=1),
            Action(strafe=1.0, duration=2, skill=1),
            Action(jump=1.0),
            Action(forward=1.0, sprint=1.0, duration=2, skill=1),
            Action(sneak=1.0, duration=2),
            Action(yaw=12.0, duration=4),
            Action(yaw=-12.0, duration=4),
            Action(pitch=8.0, duration=4),
            Action(pitch=-8.0, duration=4),
            Action(hotbar=0),
            Action(hotbar=1),
            Action(hotbar=2),
            Action(attack=1.0),
            Action(use=1.0),
            Action(drop=1.0),
            Action(inventory=1.0),
            Action(),
        ]
        stage = min(len(stages) - 1, max(0, index // 8))
        return stages[stage]

    def apply_command(self, raw: str, observation_count: int) -> str:
        parts = raw.strip().lower().split()
        if not parts:
            return ""
        command = parts[0]
        with self.lock:
            if command in ("quit", "exit"):
                self.stop_requested = True
                return "server will stop"
            if command == "help":
                return (
                    "idle | forward | back | left | right | jump | sprint | sneak | "
                    "yaw <deg> | pitch <deg> | attack | use | drop | inventory | "
                    "slot <1-9> | demo | status | quit"
                )
            if command == "idle":
                self.action = Action()
                self.demo_start = None
                return "persistent action: idle"
            if command == "forward":
                self.action = Action(forward=1.0, duration=2, skill=1)
                self.demo_start = None
            elif command == "back":
                self.action = Action(forward=-1.0, duration=2, skill=1)
                self.demo_start = None
            elif command == "left":
                self.action = Action(strafe=-1.0, duration=2, skill=1)
                self.demo_start = None
            elif command == "right":
                self.action = Action(strafe=1.0, duration=2, skill=1)
                self.demo_start = None
            elif command == "sprint":
                self.action = Action(forward=1.0, sprint=1.0, duration=2, skill=1)
                self.demo_start = None
            elif command == "sneak":
                self.action = Action(sneak=1.0, duration=2)
                self.demo_start = None
            elif command == "jump":
                self.pulse = Action(jump=1.0)
            elif command == "attack":
                self.pulse = Action(attack=1.0)
            elif command == "use":
                self.pulse = Action(use=1.0)
            elif command == "drop":
                self.pulse = Action(drop=1.0)
            elif command == "inventory":
                self.pulse = Action(inventory=1.0)
            elif command == "yaw" and len(parts) == 2:
                self.pulse = Action(yaw=max(-45.0, min(45.0, float(parts[1]))), duration=4)
            elif command == "pitch" and len(parts) == 2:
                self.pulse = Action(pitch=max(-30.0, min(30.0, float(parts[1]))), duration=4)
            elif command == "slot" and len(parts) == 2:
                slot = int(parts[1])
                if slot < 1 or slot > 9:
                    return "slot must be 1..9"
                self.pulse = Action(hotbar=slot - 1)
            elif command == "demo":
                self.demo_start = observation_count
                self.action = Action()
                return "one-shot actuator demo started"
            elif command == "status":
                return "current: " + self.action.summary()
            else:
                return "unknown command; type help"
            if self.pulse is not None:
                return "next action: " + self.pulse.summary()
            return "persistent action: " + self.action.summary()


def console_worker(controller: CommandController, command_queue: queue.Queue[str]) -> None:
    while not controller.stop_requested:
        try:
            line = input("model> ")
        except (EOFError, KeyboardInterrupt):
            controller.stop_requested = True
            return
        command_queue.put(line)


def decode_hello(payload: bytes) -> tuple[str, str, str, str, int, int]:
    offset = 0
    protocol, offset = read_u16_text(payload, offset, 48)
    mod_version, offset = read_u16_text(payload, offset, 64)
    observation_schema, offset = read_u16_text(payload, offset, 48)
    action_schema, offset = read_u16_text(payload, offset, 48)
    if offset + 10 != len(payload):
        raise ProtocolError("invalid hello size")
    nonce, decision_rate = struct.unpack_from(">qH", payload, offset)
    return protocol, mod_version, observation_schema, action_schema, nonce, decision_rate


def encode_hello_ack(nonce: int) -> bytes:
    return (
        write_u16_text(PROTOCOL, 48)
        + write_u16_text(MODEL_VERSION, 64)
        + struct.pack(">qI", nonce, 0)
    )


def decode_observation_prefix(payload: bytes) -> tuple[int, int]:
    if len(payload) < OBS_PREFIX.size:
        raise ProtocolError("truncated observation")
    magic, version, _reserved, sequence, client_tick = OBS_PREFIX.unpack_from(payload)
    if magic != OBS_MAGIC or version != 1:
        raise ProtocolError("unsupported observation payload")
    return sequence, client_tick


def serve(host: str, port: int, mode: str) -> int:
    controller = CommandController(mode)
    commands: queue.Queue[str] = queue.Queue()
    if mode == "interactive":
        threading.Thread(
            target=console_worker,
            args=(controller, commands),
            name="SawBot-Dummy-Console",
            daemon=True,
        ).start()

    print("SawBotV1 Phase 4 dummy model")
    print(f"Protocol: {PROTOCOL}")
    print(f"Listening: {host}:{port}")
    print(f"Mode: {mode}")
    if mode == "interactive":
        print("Type 'help' for actuator commands. Start with 'idle'.")

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((host, port))
        server.listen(1)
        server.settimeout(0.5)
        observation_count = 0
        while not controller.stop_requested:
            while True:
                try:
                    command = commands.get_nowait()
                except queue.Empty:
                    break
                try:
                    print(controller.apply_command(command, observation_count))
                except (ValueError, TypeError) as exc:
                    print(f"invalid command: {exc}")

            try:
                connection, address = server.accept()
            except socket.timeout:
                continue
            print(f"Client connected: {address[0]}:{address[1]}")
            with connection:
                connection.settimeout(5.0)
                try:
                    frame_type, payload = read_frame(connection)
                    if frame_type != TYPE_HELLO:
                        raise ProtocolError("first frame was not HELLO")
                    protocol, mod_version, observation_schema, action_schema, nonce, decision_rate = decode_hello(payload)
                    if protocol != PROTOCOL:
                        raise ProtocolError(f"unsupported protocol {protocol}")
                    print(
                        f"Handshake: mod={mod_version} obs={observation_schema} "
                        f"action={action_schema} rate={decision_rate}Hz"
                    )
                    write_frame(connection, TYPE_HELLO_ACK, encode_hello_ack(nonce))
                    connection.settimeout(1.0)
                    last_report = time.monotonic()
                    while not controller.stop_requested:
                        while True:
                            try:
                                command = commands.get_nowait()
                            except queue.Empty:
                                break
                            try:
                                print(controller.apply_command(command, observation_count))
                            except (ValueError, TypeError) as exc:
                                print(f"invalid command: {exc}")
                        try:
                            frame_type, payload = read_frame(connection)
                        except socket.timeout:
                            continue
                        if frame_type == TYPE_OBSERVATION:
                            sequence, client_tick = decode_observation_prefix(payload)
                            observation_count += 1
                            action = controller.action_for(observation_count)
                            write_frame(connection, TYPE_ACTION, action.encode(sequence))
                            if time.monotonic() - last_report >= 1.0:
                                print(
                                    f"obs #{sequence} tick {client_tick} -> {action.summary()}"
                                )
                                last_report = time.monotonic()
                        elif frame_type == TYPE_PING:
                            write_frame(connection, TYPE_PONG, payload)
                        elif frame_type == TYPE_GOODBYE:
                            break
                        else:
                            raise ProtocolError(f"unexpected frame type {frame_type}")
                except (EOFError, ConnectionError, OSError, ProtocolError) as exc:
                    print(f"Client disconnected: {exc}")
    return 0



def self_test() -> int:
    left, right = socket.socketpair()
    try:
        payload = b"phase4-frame"
        write_frame(left, TYPE_PING, payload)
        frame_type, decoded = read_frame(right)
        assert frame_type == TYPE_PING and decoded == payload
    finally:
        left.close()
        right.close()

    hello = (
        write_u16_text(PROTOCOL, 48)
        + write_u16_text("0.6.0-alpha.0", 64)
        + write_u16_text("sawbot.observation/0.3", 48)
        + write_u16_text("sawbot.action/0.1", 48)
        + struct.pack(">qH", 123456789, 10)
    )
    decoded = decode_hello(hello)
    assert decoded == (
        PROTOCOL,
        "0.6.0-alpha.0",
        "sawbot.observation/0.3",
        "sawbot.action/0.1",
        123456789,
        10,
    )

    observation = OBS_PREFIX.pack(OBS_MAGIC, 1, 0, 77, 900)
    assert decode_observation_prefix(observation) == (77, 900)

    action = Action(
        forward=1.0,
        strafe=-0.5,
        yaw=12.0,
        pitch=-4.0,
        jump=1.0,
        sprint=1.0,
        attack=1.0,
        hotbar=2,
        target=7,
        waypoint=3,
        duration=4,
    )
    packed = action.encode(77)
    unpacked = ACTION_STRUCT.unpack(packed)
    assert unpacked[0] == 77
    assert unpacked[1] == 1.0 and unpacked[2] == -0.5
    assert unpacked[12] == 2 and unpacked[14] == 7 and unpacked[15] == 3
    assert unpacked[17] == 4
    assert len(packed) == ACTION_STRUCT.size

    print("PASS dummy model protocol self-test")
    return 0

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25189)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument(
        "--mode",
        choices=("interactive", "idle", "demo"),
        default="interactive",
    )
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    try:
        return serve(args.host, args.port, args.mode)
    except KeyboardInterrupt:
        print("\nDummy model stopped.")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
