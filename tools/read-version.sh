#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPERTIES="$ROOT/gradle.properties"

[[ -f "$PROPERTIES" ]] || {
  echo "ERROR: missing gradle.properties" >&2
  exit 2
}

VERSION="$(sed -n 's/^sawbotVersion=//p' "$PROPERTIES" | tail -n 1 | tr -d '\r' | xargs)"

if [[ -z "$VERSION" ]]; then
  echo "ERROR: sawbotVersion is not defined in gradle.properties" >&2
  exit 3
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "ERROR: invalid sawbotVersion: $VERSION" >&2
  exit 4
fi

printf '%s\n' "$VERSION"
