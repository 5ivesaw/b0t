#!/usr/bin/env python3
from __future__ import annotations

import argparse
import gzip
import json
import time
from pathlib import Path

import numpy as np

from waypoint_core import ACTION_NAMES, FEATURE_NAMES, MODEL_FORMAT, MODEL_VERSION


def load_dataset(path: Path) -> tuple[np.ndarray, np.ndarray, dict]:
    rows = []
    labels = []
    header = None
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        for index, line in enumerate(stream):
            payload = json.loads(line)
            if index == 0:
                header = payload["header"]
                continue
            rows.append(payload["x"])
            labels.append(payload["y"])
    if header is None or header["featureNames"] != FEATURE_NAMES or header["actionNames"] != ACTION_NAMES:
        raise RuntimeError("dataset contract mismatch")
    return np.asarray(rows, dtype=np.float32), np.asarray(labels, dtype=np.int64), header


def softmax(logits: np.ndarray) -> np.ndarray:
    shifted = logits - logits.max(axis=1, keepdims=True)
    exp = np.exp(shifted)
    return exp / exp.sum(axis=1, keepdims=True)


def accuracy(x: np.ndarray, y: np.ndarray, w1, b1, w2, b2) -> float:
    hidden = np.tanh(x @ w1.T + b1)
    logits = hidden @ w2.T + b2
    return float((logits.argmax(axis=1) == y).mean())


def main() -> int:
    parser = argparse.ArgumentParser()
    base = Path(__file__).parent
    parser.add_argument("--dataset", type=Path, default=base / "datasets" / "teacher_waypoint_v0.1.jsonl.gz")
    parser.add_argument("--output", type=Path, default=base / "checkpoints" / "waypoint_v0.1.json")
    parser.add_argument("--seed", type=int, default=5102026)
    parser.add_argument("--hidden", type=int, default=32)
    parser.add_argument("--epochs", type=int, default=90)
    parser.add_argument("--batch", type=int, default=256)
    parser.add_argument("--learning-rate", type=float, default=0.012)
    args = parser.parse_args()

    np.random.seed(args.seed)
    x, y, header = load_dataset(args.dataset)
    order = np.random.permutation(len(x))
    split = int(len(x) * 0.85)
    train_ids, validation_ids = order[:split], order[split:]
    mean = x[train_ids].mean(axis=0)
    scale = x[train_ids].std(axis=0)
    scale = np.where(scale < 1e-4, 1.0, scale)
    xn = (x - mean) / scale

    w1 = np.random.normal(0.0, 0.16, (args.hidden, len(FEATURE_NAMES))).astype(np.float32)
    b1 = np.zeros(args.hidden, dtype=np.float32)
    w2 = np.random.normal(0.0, 0.16, (len(ACTION_NAMES), args.hidden)).astype(np.float32)
    b2 = np.zeros(len(ACTION_NAMES), dtype=np.float32)

    # Adam state.
    params = [w1, b1, w2, b2]
    m = [np.zeros_like(value) for value in params]
    v = [np.zeros_like(value) for value in params]
    beta1, beta2, eps = 0.9, 0.999, 1e-8
    step = 0
    start = time.perf_counter()

    for epoch in range(args.epochs):
        shuffled = np.random.permutation(train_ids)
        for offset in range(0, len(shuffled), args.batch):
            ids = shuffled[offset:offset + args.batch]
            xb = xn[ids]
            yb = y[ids]
            hidden = np.tanh(xb @ w1.T + b1)
            logits = hidden @ w2.T + b2
            probs = softmax(logits)
            probs[np.arange(len(ids)), yb] -= 1.0
            probs /= len(ids)

            grad_w2 = probs.T @ hidden
            grad_b2 = probs.sum(axis=0)
            grad_hidden = (probs @ w2) * (1.0 - hidden * hidden)
            grad_w1 = grad_hidden.T @ xb
            grad_b1 = grad_hidden.sum(axis=0)
            grads = [grad_w1, grad_b1, grad_w2, grad_b2]

            step += 1
            for index, (parameter, gradient) in enumerate(zip(params, grads)):
                m[index] = beta1 * m[index] + (1.0 - beta1) * gradient
                v[index] = beta2 * v[index] + (1.0 - beta2) * (gradient * gradient)
                m_hat = m[index] / (1.0 - beta1 ** step)
                v_hat = v[index] / (1.0 - beta2 ** step)
                parameter -= args.learning_rate * m_hat / (np.sqrt(v_hat) + eps)

        if epoch in (0, 9, 29, 59, args.epochs - 1):
            print(
                f"epoch {epoch + 1:3d}/{args.epochs} "
                f"train={accuracy(xn[train_ids], y[train_ids], w1, b1, w2, b2):.4f} "
                f"validation={accuracy(xn[validation_ids], y[validation_ids], w1, b1, w2, b2):.4f}"
            )

    elapsed = time.perf_counter() - start
    train_accuracy = accuracy(xn[train_ids], y[train_ids], w1, b1, w2, b2)
    validation_accuracy = accuracy(xn[validation_ids], y[validation_ids], w1, b1, w2, b2)
    checkpoint = {
        "format": MODEL_FORMAT,
        "modelVersion": MODEL_VERSION,
        "featureNames": FEATURE_NAMES,
        "actionNames": ACTION_NAMES,
        "hiddenSize": args.hidden,
        "normalization": {"mean": mean.tolist(), "scale": scale.tolist()},
        "weights": {
            "w1": w1.tolist(),
            "b1": b1.tolist(),
            "w2": w2.tolist(),
            "b2": b2.tolist(),
        },
        "training": {
            "seed": args.seed,
            "datasetRows": int(len(x)),
            "trainRows": int(len(train_ids)),
            "validationRows": int(len(validation_ids)),
            "epochs": args.epochs,
            "batch": args.batch,
            "learningRate": args.learning_rate,
            "trainAccuracy": train_accuracy,
            "validationAccuracy": validation_accuracy,
            "trainingSeconds": elapsed,
            "datasetHeader": header,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(checkpoint, indent=2), encoding="utf-8")
    print(json.dumps(checkpoint["training"], indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
