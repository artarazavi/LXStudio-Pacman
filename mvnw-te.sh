#!/usr/bin/env bash

set -euo pipefail

if [[ "${OSTYPE:-}" != darwin* ]]; then
  echo "mvnw-te.sh is intended for macOS. Please run Maven with Java 21." >&2
  exec mvn "$@"
fi

if ! command -v /usr/libexec/java_home >/dev/null 2>&1; then
  echo "Could not find /usr/libexec/java_home. Please install Temurin 21 and set JAVA_HOME manually." >&2
  exit 1
fi

JAVA21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"

if [[ -z "${JAVA21_HOME}" ]]; then
  echo "Temurin/OpenJDK 21 was not found. Install it first, for example with: brew install temurin@21" >&2
  exit 1
fi

export JAVA_HOME="${JAVA21_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
M2_REPO="${HOME}/.m2/repository"

if [[ ! -f "${M2_REPO}/org/jogamp/gluegen/gluegen-rt-main/2.4.0-rc-20230123/gluegen-rt-main-2.4.0-rc-20230123.pom" ]] \
  || [[ ! -f "${M2_REPO}/org/jogamp/jogl/jogl-all-main/2.4.0-rc-20230123/jogl-all-main-2.4.0-rc-20230123.pom" ]]; then
  "${SCRIPT_DIR}/install-jogamp-archive.sh"
fi

exec mvn "$@"
