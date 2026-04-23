#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

MVNW="$ROOT_DIR/mvnw"
if [[ ! -f "$MVNW" && -f "$ROOT_DIR/../mvnw" ]]; then
  MVNW="$ROOT_DIR/../mvnw"
fi
if [[ ! -f "$MVNW" ]]; then
  echo "Cannot find Maven Wrapper: $MVNW"
  exit 1
fi
chmod +x "$MVNW"

ARTIFACT_ID="$("$MVNW" -q -DforceStdout help:evaluate -Dexpression=project.artifactId)"

"$MVNW" -f "$ROOT_DIR/../pom.xml" -pl trader-executor -am -DskipTests package

JAR_PATH="$(ls -1 target/*.jar | grep -v 'original-' | head -n 1 || true)"
if [[ -z "${JAR_PATH}" ]]; then
  echo "Cannot find packaged jar under target/"
  exit 1
fi
if ! jar tf "$JAR_PATH" >/dev/null 2>&1; then
  echo "Packaged jar is invalid: ${JAR_PATH}"
  exit 1
fi

APPLICATION_YML="$ROOT_DIR/src/main/resources/application.yml"
LOGBACK_XML="$ROOT_DIR/src/main/resources/logback-spring.xml"
DOCKER_COMPOSE="$ROOT_DIR/docker-compose.yml"
CONFIG_PROPERTIE="$ROOT_DIR/src/main/resources/config.properties"
for required_file in "$APPLICATION_YML" "$LOGBACK_XML" "$DOCKER_COMPOSE"; do
  if [[ ! -f "$required_file" ]]; then
    echo "Required file not found: ${required_file}"
    exit 1
  fi
done

DIST_DIR="$ROOT_DIR/target"
PKG_DIR="${DIST_DIR}/${ARTIFACT_ID}"
rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR"
mkdir -p "${PKG_DIR}/config"

cp -f "$JAR_PATH" "${PKG_DIR}/trader-executor.jar"
cp -f "$ROOT_DIR/docker-compose.yml" "${PKG_DIR}/docker-compose.yml"
cp -f "$APPLICATION_YML" "${PKG_DIR}/config/application.yml"
cp -f "$LOGBACK_XML" "${PKG_DIR}/config/logback-spring.xml"
cp -f "$CONFIG_PROPERTIE" "${PKG_DIR}/config/config.properties"
if [[ -d "$ROOT_DIR/bin" ]]; then
  cp -R "$ROOT_DIR/bin" "${PKG_DIR}/bin"
  chmod +x "${PKG_DIR}/bin/"*.sh 2>/dev/null || true
fi
if ! jar tf "${PKG_DIR}/trader-executor.jar" >/dev/null 2>&1; then
  echo "Copied jar is invalid: ${PKG_DIR}/trader-executor.jar"
  exit 1
fi

TAR_PATH="${DIST_DIR}/${ARTIFACT_ID}.tar.gz"
tar -czf "$TAR_PATH" -C "$DIST_DIR" "${ARTIFACT_ID}"

echo "Jar: ${PKG_DIR}/trader-executor.jar"
ls -lh "${PKG_DIR}/trader-executor.jar"
if command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "${PKG_DIR}/trader-executor.jar"
elif command -v sha256sum >/dev/null 2>&1; then
  sha256sum "${PKG_DIR}/trader-executor.jar"
fi

echo "Packaged: ${TAR_PATH}"
