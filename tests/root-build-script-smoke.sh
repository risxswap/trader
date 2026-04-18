#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

help_output="$("$ROOT_DIR/build.sh" help 2>&1)"
echo "$help_output" | grep -q "Usage:"
echo "$help_output" | grep -q "Supported modules:"

if "$ROOT_DIR/build.sh" foo >/tmp/root-build-script-invalid.log 2>&1; then
  echo "Expected invalid command to fail"
  exit 1
fi

grep -q "Unsupported command" /tmp/root-build-script-invalid.log

if "$ROOT_DIR/build.sh" package >/tmp/root-build-script-missing-module.log 2>&1; then
  echo "Expected package without module to fail"
  exit 1
fi

grep -q "Missing module name" /tmp/root-build-script-missing-module.log

if "$ROOT_DIR/build.sh" package foo >/tmp/root-build-script-invalid-module.log 2>&1; then
  echo "Expected unsupported module to fail"
  exit 1
fi

grep -q "Unsupported module" /tmp/root-build-script-invalid-module.log
