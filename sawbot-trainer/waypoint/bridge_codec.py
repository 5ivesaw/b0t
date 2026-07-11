"""sawbot.bridge/0.1 framing and the observation subset used by Phase 5."""
from __future__ import annotations

import dataclasses
import socket
import struct
import zlib
from typing import BinaryIO

PROTOCOL = "sawbot.bridge/0.1"
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
OBSERVATION_MAGIC = 0x3153424F
OBSERVATION_VERSION = 1
LOCAL_TERRAIN_CELLS = 13 * 9 * 13
MID_RANGE_COLUMNS = 33 * 33
FLAG_SOLID = 1 << 0
FLAG_HAZARD = 1 << 5
FLAG_SAFE_SUPPORT = 1 << 9
USER_WAYPOINT_ID = 1000

FRAME_HEADER = struct.Struct(">IBBHII")
ACTION_STRUCT = struct.Struct(">q11fbBii fBBB".replace(" ", ""))


class ProtocolError(RuntimeError):
    pass


class Reader:
    def __init__(self, data: bytes) -> None:
        self.data = data
        self.offset = 0

    def take(self, size: int) -> bytes:
        end = self.offset + size
        if size < 0 or end > len(self.data):
            raise ProtocolError("truncated observation")
        value = self.data[self.offset:end]
        self.offset = end
        return value

    def unpack(self, fmt: str):
        structure = struct.Struct("<" + fmt)
        return structure.unpack(self.take(structure.size))

    def u8(self) -> int: return self.unpack("B")[0]
    def i8(self) -> int: return self.unpack("b")[0]
    def u16(self) -> int: return self.unpack("H")[0]
    def i16(self) -> int: return self.unpack("h")[0]
    def i32(self) -> int: return self.unpack("i")[0]
    def i64(self) -> int: return self.unpack("q")[0]
    def f32(self) -> float: return self.unpack("f")[0]
    def f64(self) -> float: return self.unpack("d")[0]
    def boolean(self) -> bool: return self.u8() != 0

    def utf8(self) -> str:
        length = self.u16()
        return self.take(length).decode("utf-8")

    def skip(self, size: int) -> None:
        self.take(size)


@dataclasses.dataclass(frozen=True)
class Hello:
    mod_version: str
    observation_schema: str
    action_schema: str
    nonce: int
    decision_rate: int


@dataclasses.dataclass(frozen=True)
class ObservationView:
    sequence: int
    client_tick: int
    absolute_x: float
    absolute_y: float
    absolute_z: float
    velocity_forward: float
    on_ground: bool
    horizontal_collision: bool
    support_left: float
    support_center: float
    support_right: float
    distance_to_void: float
    terrain_flags: tuple[int, ...]
    terrain_collision: bytes
    landmarks: tuple[dict, ...]

    @staticmethod
    def terrain_index(right: int, up: int, forward: int) -> int:
        if not (-6 <= right <= 6 and -4 <= up <= 4 and -6 <= forward <= 6):
            raise IndexError("terrain offset")
        return ((up + 4) * 13 + (forward + 6)) * 13 + (right + 6)

    def flags_at(self, right: int, up: int, forward: int) -> int:
        return self.terrain_flags[self.terrain_index(right, up, forward)]

    def collision_at(self, right: int, up: int, forward: int) -> int:
        return self.terrain_collision[self.terrain_index(right, up, forward)]

    def user_waypoint(self) -> dict | None:
        for landmark in self.landmarks:
            if landmark["id"] == USER_WAYPOINT_ID:
                return landmark
        return None


def recv_exact(sock: socket.socket, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        chunk = sock.recv(size - len(chunks))
        if not chunk:
            raise EOFError("connection closed")
        chunks.extend(chunk)
    return bytes(chunks)


def read_frame(sock: socket.socket) -> tuple[int, bytes]:
    header = recv_exact(sock, FRAME_HEADER.size)
    magic, version, frame_type, _reserved, length, expected_crc = FRAME_HEADER.unpack(header)
    if magic != MAGIC or version != VERSION:
        raise ProtocolError("bridge header mismatch")
    if length < 0 or length > MAX_PAYLOAD:
        raise ProtocolError("bridge payload length")
    payload = recv_exact(sock, length)
    if zlib.crc32(payload) & 0xFFFFFFFF != expected_crc:
        raise ProtocolError("bridge CRC mismatch")
    return frame_type, payload


def write_frame(sock: socket.socket, frame_type: int, payload: bytes) -> None:
    if len(payload) > MAX_PAYLOAD:
        raise ProtocolError("payload too large")
    header = FRAME_HEADER.pack(
        MAGIC,
        VERSION,
        frame_type,
        0,
        len(payload),
        zlib.crc32(payload) & 0xFFFFFFFF,
    )
    sock.sendall(header + payload)


def read_be_text(payload: bytes, offset: int, maximum: int) -> tuple[str, int]:
    if offset + 2 > len(payload):
        raise ProtocolError("truncated string")
    length = struct.unpack_from(">H", payload, offset)[0]
    offset += 2
    if length > maximum or offset + length > len(payload):
        raise ProtocolError("invalid string")
    return payload[offset:offset + length].decode("utf-8"), offset + length


def write_be_text(value: str, maximum: int) -> bytes:
    encoded = value.encode("utf-8")
    if len(encoded) > maximum:
        raise ProtocolError("string too long")
    return struct.pack(">H", len(encoded)) + encoded


def decode_hello(payload: bytes) -> Hello:
    offset = 0
    protocol, offset = read_be_text(payload, offset, 48)
    mod_version, offset = read_be_text(payload, offset, 64)
    observation_schema, offset = read_be_text(payload, offset, 48)
    action_schema, offset = read_be_text(payload, offset, 48)
    if protocol != PROTOCOL or offset + 10 != len(payload):
        raise ProtocolError("hello mismatch")
    nonce, decision_rate = struct.unpack_from(">qH", payload, offset)
    return Hello(mod_version, observation_schema, action_schema, nonce, decision_rate)


def encode_hello_ack(nonce: int, model_version: str) -> bytes:
    return write_be_text(PROTOCOL, 48) + write_be_text(model_version, 64) + struct.pack(">qI", nonce, 0)


def encode_action(
    observation_sequence: int,
    *,
    forward: float = 0.0,
    strafe: float = 0.0,
    yaw: float = 0.0,
    pitch: float = 0.0,
    jump: float = 0.0,
    sprint: float = 0.0,
    sneak: float = 0.0,
    attack: float = 0.0,
    use: float = 0.0,
    drop: float = 0.0,
    inventory: float = 0.0,
    hotbar: int = -1,
    skill: int = 1,
    target: int = -1,
    waypoint: int = USER_WAYPOINT_ID,
    confidence: float = 1.0,
    duration: int = 2,
    objective: int = 13,
    abort: int = 0,
) -> bytes:
    return ACTION_STRUCT.pack(
        observation_sequence,
        forward,
        strafe,
        yaw,
        pitch,
        jump,
        sprint,
        sneak,
        attack,
        use,
        drop,
        inventory,
        hotbar,
        skill,
        target,
        waypoint,
        confidence,
        duration,
        objective,
        abort,
    )


def decode_observation(payload: bytes) -> ObservationView:
    reader = Reader(payload)
    magic = reader.i32() & 0xFFFFFFFF
    version = reader.u16()
    reader.u16()
    sequence = reader.i64()
    client_tick = reader.i64()
    if magic != OBSERVATION_MAGIC or version != OBSERVATION_VERSION:
        raise ProtocolError("observation payload mismatch")

    reader.utf8()  # observation schema
    reader.i64()   # monotonic timestamp
    reader.i64(); reader.i64()  # UUID
    reader.utf8()  # world
    reader.utf8()  # task adapter
    reader.i64()   # validity flags

    # Self state.
    reader.skip(4 * 4)  # health, absorption, hunger, armour
    absolute_x = reader.f64()
    absolute_y = reader.f64()
    absolute_z = reader.f64()
    reader.f32(); reader.f32()
    velocity_forward = reader.f32()
    reader.skip(3 * 4)  # acceleration
    reader.skip(3 * 4)  # yaw, pitch, fall
    self_bits = reader.u16()
    reader.i32(); reader.i32()
    reader.i8()
    support_left = reader.f32()
    support_center = reader.f32()
    support_right = reader.f32()
    distance_to_void = reader.f32()
    reader.u16()

    # Local terrain arrays.
    reader.skip(3 * 4)
    reader.u8(); reader.u16()
    reader.skip(LOCAL_TERRAIN_CELLS * 2)  # state IDs
    reader.skip(LOCAL_TERRAIN_CELLS)      # categories
    terrain_flags = tuple(reader.u16() for _ in range(LOCAL_TERRAIN_CELLS))
    terrain_collision = reader.take(LOCAL_TERRAIN_CELLS)

    # Mid-range map.
    reader.skip(3 * 4)
    reader.u8(); reader.u8()
    reader.skip(MID_RANGE_COLUMNS * 2 * 3)

    # Entities.
    entity_count = reader.u16()
    reader.u16()
    reader.skip(entity_count * 84)

    # Inventory.
    reader.i8()
    reader.utf8()
    reader.skip(5 * 4)
    slot_count = reader.u8()
    reader.skip(slot_count * 14)

    # Landmarks.
    landmark_count = reader.u16()
    landmarks: list[dict] = []
    for _ in range(landmark_count):
        landmark_id = reader.i32()
        landmark_type = reader.u8()
        team = reader.u8()
        right = reader.f32()
        up = reader.f32()
        forward = reader.f32()
        distance = reader.f32()
        estimated_cost = reader.f32()
        danger = reader.f32()
        value = reader.f32()
        confidence = reader.f32()
        reachable = reader.boolean()
        landmarks.append({
            "id": landmark_id,
            "type": landmark_type,
            "team": team,
            "right": right,
            "up": up,
            "forward": forward,
            "distance": distance,
            "estimated_cost": estimated_cost,
            "danger": danger,
            "value": value,
            "confidence": confidence,
            "reachable": reachable,
        })

    return ObservationView(
        sequence=sequence,
        client_tick=client_tick,
        absolute_x=absolute_x,
        absolute_y=absolute_y,
        absolute_z=absolute_z,
        velocity_forward=velocity_forward,
        on_ground=(self_bits & (1 << 0)) != 0,
        horizontal_collision=(self_bits & (1 << 1)) != 0,
        support_left=support_left,
        support_center=support_center,
        support_right=support_right,
        distance_to_void=distance_to_void,
        terrain_flags=terrain_flags,
        terrain_collision=terrain_collision,
        landmarks=tuple(landmarks),
    )
