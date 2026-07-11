#!/usr/bin/env python3
"""Print a compact human-readable timeline from a SawBotV1 trajectory."""

from __future__ import annotations

import argparse
import importlib.util
import pathlib
import sys


def load_validator(root: pathlib.Path):
    path = root / "sawbot-tools" / "dataset-validator" / "validate_telemetry.py"
    spec = importlib.util.spec_from_file_location("sawbot_validate_telemetry", path)
    if spec is None or spec.loader is None:
        raise RuntimeError("cannot load telemetry validator")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=pathlib.Path)
    parser.add_argument("--limit", type=int, default=40)
    args = parser.parse_args()

    root = pathlib.Path(__file__).resolve().parents[2]
    validator = load_validator(root)
    report, _, _ = validator.validate(args.path, include_steps=True)
    print(f"SawBot trajectory: {args.path}")
    print(f"{report.telemetry_schema} / {report.observation_schema}")
    print(f"world={report.world_identifier} task={report.task_adapter}")
    print(f"steps={report.step_count} complete={report.complete} error={report.error or '-'}")
    print("ordinal  obs->outcome  ticks  inputs/drop  mouse dx/dy  keyBits  slot  gui  events  incomplete")
    for step in report.steps[: max(0, args.limit)]:
        print(
            f"{step.ordinal:7d}  {step.observation_sequence}->{step.outcome_sequence}  "
            f"{step.observation_tick}->{step.outcome_tick}  "
            f"{step.input_samples}/{step.dropped_input_samples}  "
            f"{step.mouse_delta_x:+d}/{step.mouse_delta_y:+d}  "
            f"0x{step.key_or:03x}  {step.first_selected_slot:4d}  {step.gui_samples:3d}  "
            f"{step.outcome_events:6d}  {step.incomplete_outcome}"
        )
    return 0 if report.complete else 2


if __name__ == "__main__":
    raise SystemExit(main())
