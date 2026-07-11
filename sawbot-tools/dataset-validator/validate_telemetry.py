#!/usr/bin/env python3
"""Validate and optionally recover SawBotV1 structured trajectory files."""

from __future__ import annotations

import argparse
import json
import pathlib
import struct
import sys
import zlib
from dataclasses import dataclass, asdict

FILE_MAGIC = b"SBTEL001"
FILE_VERSION = 1
ENDIAN_MARKER = 0x01020304
RECORD_MAGIC = 0x31524253
RECORD_STEP = 1
RECORD_FOOTER = 2
RECORD_HEADER = struct.Struct("<IBBHqqIII")
STEP_SUMMARY = struct.Struct("<4sHBBqqqqHiHHHiiBB")
FOOTER_FIXED = struct.Struct("<4sHHqqI")


class ValidationError(RuntimeError):
    pass


@dataclass
class StepSummary:
    ordinal: int
    observation_sequence: int
    observation_tick: int
    outcome_sequence: int
    outcome_tick: int
    input_samples: int
    dropped_input_samples: int
    outcome_events: int
    key_or: int
    mouse_delta_x: int
    mouse_delta_y: int
    first_selected_slot: int
    gui_samples: int
    incomplete_outcome: bool


@dataclass
class ValidationReport:
    path: str
    telemetry_schema: str
    observation_schema: str
    world_identifier: str
    task_adapter: str
    complete: bool
    truncated: bool
    step_count: int
    footer_step_count: int | None
    dropped_steps: int | None
    rolling_crc32: int
    file_size: int
    valid_prefix_bytes: int
    error: str | None
    steps: list[StepSummary]


def read_exact(data: bytes, offset: int, size: int) -> tuple[bytes, int]:
    end = offset + size
    if end > len(data):
        raise EOFError(f"needed {size} bytes at offset {offset}")
    return data[offset:end], end


def read_u16_string(data: bytes, offset: int) -> tuple[str, int]:
    raw, offset = read_exact(data, offset, 2)
    length = struct.unpack("<H", raw)[0]
    raw, offset = read_exact(data, offset, length)
    try:
        return raw.decode("utf-8"), offset
    except UnicodeDecodeError as exc:
        raise ValidationError(f"invalid UTF-8 at offset {offset - length}") from exc


def parse_header(data: bytes) -> tuple[dict[str, object], int]:
    minimum = 8 + 4 * 3 + 8 * 3
    if len(data) < minimum:
        raise ValidationError("file is too short for the telemetry header")
    offset = 0
    magic, offset = read_exact(data, offset, 8)
    if magic != FILE_MAGIC:
        raise ValidationError(f"bad file magic: {magic!r}")
    version, endian, flags = struct.unpack_from("<III", data, offset)
    offset += 12
    if version != FILE_VERSION:
        raise ValidationError(f"unsupported file version {version}")
    if endian != ENDIAN_MARKER:
        raise ValidationError(f"unexpected endian marker 0x{endian:08x}")
    created_ms, episode_msb, episode_lsb = struct.unpack_from("<qqq", data, offset)
    offset += 24
    telemetry_schema, offset = read_u16_string(data, offset)
    observation_schema, offset = read_u16_string(data, offset)
    world_identifier, offset = read_u16_string(data, offset)
    task_adapter, offset = read_u16_string(data, offset)
    return {
        "version": version,
        "flags": flags,
        "created_epoch_millis": created_ms,
        "episode_msb": episode_msb,
        "episode_lsb": episode_lsb,
        "telemetry_schema": telemetry_schema,
        "observation_schema": observation_schema,
        "world_identifier": world_identifier,
        "task_adapter": task_adapter,
    }, offset


def parse_step(payload: bytes, ordinal: int) -> StepSummary:
    if len(payload) < STEP_SUMMARY.size:
        raise ValidationError(f"step {ordinal} payload is too short")
    fields = STEP_SUMMARY.unpack_from(payload, 0)
    (
        magic,
        payload_version,
        _action_source,
        incomplete,
        observation_sequence,
        observation_tick,
        outcome_sequence,
        outcome_tick,
        input_samples,
        dropped_input_samples,
        outcome_events,
        _reserved,
        key_or,
        mouse_x,
        mouse_y,
        first_slot,
        gui_samples,
    ) = fields
    if magic != b"STP1" or payload_version != 1:
        raise ValidationError(f"step {ordinal} has an unknown payload header")
    if first_slot > 8:
        raise ValidationError(f"step {ordinal} selected slot is invalid")
    return StepSummary(
        ordinal=ordinal,
        observation_sequence=observation_sequence,
        observation_tick=observation_tick,
        outcome_sequence=outcome_sequence,
        outcome_tick=outcome_tick,
        input_samples=input_samples,
        dropped_input_samples=dropped_input_samples,
        outcome_events=outcome_events,
        key_or=key_or,
        mouse_delta_x=mouse_x,
        mouse_delta_y=mouse_y,
        first_selected_slot=first_slot,
        gui_samples=gui_samples,
        incomplete_outcome=bool(incomplete),
    )


def parse_footer(payload: bytes) -> tuple[int, int, int, str]:
    if len(payload) < FOOTER_FIXED.size + 2:
        raise ValidationError("footer payload is too short")
    magic, version, _reserved, steps, dropped, rolling = FOOTER_FIXED.unpack_from(payload, 0)
    if magic != b"FTR1" or version != 1:
        raise ValidationError("footer payload is malformed")
    reason, end = read_u16_string(payload, FOOTER_FIXED.size)
    if end != len(payload):
        raise ValidationError("footer has trailing bytes")
    return steps, dropped, rolling, reason


def validate(path: pathlib.Path, include_steps: bool = True) -> tuple[ValidationReport, bytes, list[bytes]]:
    data = path.read_bytes()
    header, offset = parse_header(data)
    header_end = offset
    valid_records: list[bytes] = []
    steps: list[StepSummary] = []
    running_crc = 0
    footer_step_count: int | None = None
    dropped_steps: int | None = None
    complete = False
    truncated = False
    error: str | None = None

    while offset < len(data):
        record_start = offset
        if len(data) - offset < RECORD_HEADER.size:
            truncated = True
            error = f"truncated record header at offset {offset}"
            break
        (
            magic,
            record_type,
            flags,
            _reserved,
            ordinal,
            observation_sequence,
            uncompressed_length,
            compressed_length,
            stored_crc,
        ) = RECORD_HEADER.unpack_from(data, offset)
        offset += RECORD_HEADER.size
        if magic != RECORD_MAGIC:
            error = f"bad record magic at offset {record_start}"
            break
        if flags != 1:
            error = f"unsupported record flags {flags} at ordinal {ordinal}"
            break
        if uncompressed_length < 0 or compressed_length < 0 or uncompressed_length > 2_000_000:
            error = f"invalid lengths at ordinal {ordinal}"
            break
        if offset + compressed_length > len(data):
            truncated = True
            error = f"truncated payload at ordinal {ordinal}"
            break
        compressed = data[offset:offset + compressed_length]
        offset += compressed_length
        try:
            payload = zlib.decompress(compressed)
        except zlib.error as exc:
            error = f"DEFLATE failure at ordinal {ordinal}: {exc}"
            break
        if len(payload) != uncompressed_length:
            error = f"length mismatch at ordinal {ordinal}"
            break
        actual_crc = zlib.crc32(payload) & 0xFFFFFFFF
        if actual_crc != stored_crc:
            error = f"CRC mismatch at ordinal {ordinal}"
            break

        valid_records.append(data[record_start:offset])
        if record_type == RECORD_STEP:
            summary = parse_step(payload, ordinal)
            if summary.observation_sequence != observation_sequence:
                raise ValidationError(f"record/payload sequence mismatch at ordinal {ordinal}")
            steps.append(summary)
            running_crc = zlib.crc32(payload, running_crc) & 0xFFFFFFFF
        elif record_type == RECORD_FOOTER:
            footer_step_count, dropped_steps, footer_crc, _reason = parse_footer(payload)
            if footer_step_count != len(steps):
                raise ValidationError("footer step count does not match parsed steps")
            if footer_crc != running_crc:
                raise ValidationError("footer rolling CRC does not match step payloads")
            complete = True
            if offset != len(data):
                raise ValidationError("bytes exist after the footer")
            break
        else:
            error = f"unknown record type {record_type} at ordinal {ordinal}"
            break

    if offset == len(data) and not complete and error is None:
        truncated = True
        error = "file ended without a footer"

    report = ValidationReport(
        path=str(path),
        telemetry_schema=str(header["telemetry_schema"]),
        observation_schema=str(header["observation_schema"]),
        world_identifier=str(header["world_identifier"]),
        task_adapter=str(header["task_adapter"]),
        complete=complete,
        truncated=truncated,
        step_count=len(steps),
        footer_step_count=footer_step_count,
        dropped_steps=dropped_steps,
        rolling_crc32=running_crc,
        file_size=len(data),
        valid_prefix_bytes=header_end + sum(len(record) for record in valid_records),
        error=error,
        steps=steps if include_steps else [],
    )
    return report, data[:header_end], valid_records


def encode_footer_record(ordinal: int, step_count: int, rolling_crc: int, reason: str) -> bytes:
    reason_bytes = reason.encode("utf-8")
    payload = FOOTER_FIXED.pack(b"FTR1", 1, 0, step_count, 0, rolling_crc)
    payload += struct.pack("<H", len(reason_bytes)) + reason_bytes
    compressed = zlib.compress(payload, 1)
    crc = zlib.crc32(payload) & 0xFFFFFFFF
    header = RECORD_HEADER.pack(
        RECORD_MAGIC,
        RECORD_FOOTER,
        1,
        0,
        ordinal,
        -1,
        len(payload),
        len(compressed),
        crc,
    )
    return header + compressed


def recover(path: pathlib.Path, report: ValidationReport, header: bytes, records: list[bytes]) -> pathlib.Path:
    step_records = records[: report.step_count]
    target = path.with_suffix("") if path.suffix == ".partial" else path
    target = target.with_name(target.name + ".recovered.sbt")
    footer = encode_footer_record(report.step_count, report.step_count, report.rolling_crc32,
                                  "recovered_after_interruption")
    target.write_bytes(header + b"".join(step_records) + footer)
    return target


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=pathlib.Path)
    parser.add_argument("--json", action="store_true", dest="as_json")
    parser.add_argument("--recover", action="store_true")
    parser.add_argument("--summary-only", action="store_true")
    args = parser.parse_args()

    try:
        report, header, records = validate(args.path, include_steps=not args.summary_only)
        recovered: pathlib.Path | None = None
        if args.recover:
            if report.complete:
                raise ValidationError("complete files do not need recovery")
            if report.step_count == 0:
                raise ValidationError("no complete step records are available to recover")
            recovered = recover(args.path, report, header, records)
            recovered_report, _, _ = validate(recovered, include_steps=False)
            if not recovered_report.complete:
                raise ValidationError("recovered file did not validate as complete")

        if args.as_json:
            payload = asdict(report)
            payload["steps"] = [asdict(step) for step in report.steps]
            if recovered is not None:
                payload["recovered_path"] = str(recovered)
            print(json.dumps(payload, indent=2))
        else:
            state = "COMPLETE" if report.complete else "INCOMPLETE"
            print(f"{state}: {args.path}")
            print(f"schema={report.telemetry_schema} observation={report.observation_schema}")
            print(f"steps={report.step_count} size={report.file_size} valid_prefix={report.valid_prefix_bytes}")
            if report.error:
                print(f"issue={report.error}")
            if recovered is not None:
                print(f"recovered={recovered}")

        return 0 if report.complete or recovered is not None else 2
    except (OSError, EOFError, ValidationError) as exc:
        print(f"INVALID: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
