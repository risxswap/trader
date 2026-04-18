#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

SUPPORTED_MODULES=(
  "trader-admin"
  "trader-base"
  "trader-collector"
  "trader-executor"
  "trader-statistic"
)

is_supported_module() {
  local target="$1"
  local module
  for module in "${SUPPORTED_MODULES[@]}"; do
    if [[ "$module" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

require_mvnw() {
  if [[ ! -f "$ROOT_DIR/mvnw" ]]; then
    echo "Cannot find Maven Wrapper: $ROOT_DIR/mvnw" >&2
    exit 1
  fi
  chmod +x "$ROOT_DIR/mvnw"
}

print_help() {
  cat <<'EOF'
Usage:
  ./build.sh package <module>
  ./build.sh full-install
  ./build.sh help

Supported modules:
  trader-admin
  trader-base
  trader-collector
  trader-executor
  trader-statistic
EOF
}

command="${1:-}"

case "$command" in
  help|"")
    print_help
    [[ -n "$command" ]] || exit 1
    exit 0
    ;;
  package|full-install)
    ;;
  "")
    ;;
  *)
    echo "Unsupported command: $command" >&2
    print_help
    exit 1
    ;;
esac

if [[ "$command" == "package" ]]; then
  module="${2:-}"
  if [[ -z "$module" ]]; then
    echo "Missing module name for package command" >&2
    print_help
    exit 1
  fi

  if ! is_supported_module "$module"; then
    echo "Unsupported module: $module" >&2
    print_help
    exit 1
  fi

  module_dir="$ROOT_DIR/$module"
  if [[ ! -d "$module_dir" ]]; then
    echo "Module directory not found: $module_dir" >&2
    exit 1
  fi

  package_script="$module_dir/package.sh"
  if [[ -f "$package_script" ]]; then
    chmod +x "$package_script"
    exec "$package_script"
  fi

  require_mvnw
  exec "$ROOT_DIR/mvnw" -pl "$module" -am clean package -DskipTests
fi

if [[ "$command" == "full-install" ]]; then
  require_mvnw
  exec "$ROOT_DIR/mvnw" clean install -DskipTests
fi

echo "Unsupported command: $command" >&2
print_help
exit 1
