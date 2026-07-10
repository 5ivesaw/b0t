#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
GRADLE_VERSION="8.8"
GRADLE_SHA256="a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612"
BOOTSTRAP_ROOT="${GRADLE_USER_HOME:-$HOME/.gradle}/sawbot-bootstrap"
GRADLE_HOME="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION"
ZIP="$BOOTSTRAP_ROOT/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  mkdir -p "$BOOTSTRAP_ROOT"
  if [[ ! -f "$ZIP" ]]; then
    if command -v curl >/dev/null 2>&1; then
      curl --fail --location --retry 3 "$URL" --output "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget --tries=3 --output-document="$ZIP" "$URL"
    else
      echo "ERROR: curl or wget is required to bootstrap Gradle $GRADLE_VERSION." >&2
      exit 1
    fi
  fi

  if command -v sha256sum >/dev/null 2>&1; then
    echo "$GRADLE_SHA256  $ZIP" | sha256sum --check --status || {
      rm -f "$ZIP"
      echo "ERROR: Gradle distribution checksum mismatch." >&2
      exit 1
    }
  elif command -v shasum >/dev/null 2>&1; then
    [[ "$(shasum -a 256 "$ZIP" | awk '{print $1}')" == "$GRADLE_SHA256" ]] || {
      rm -f "$ZIP"
      echo "ERROR: Gradle distribution checksum mismatch." >&2
      exit 1
    }
  else
    echo "ERROR: sha256sum or shasum is required." >&2
    exit 1
  fi

  command -v unzip >/dev/null 2>&1 || {
    echo "ERROR: unzip is required to bootstrap Gradle." >&2
    exit 1
  }
  rm -rf "$GRADLE_HOME"
  unzip -q "$ZIP" -d "$BOOTSTRAP_ROOT"
fi

exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
