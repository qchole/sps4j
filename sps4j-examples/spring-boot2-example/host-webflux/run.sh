#!/bin/bash

VERSION="$(mvn help:evaluate -Dexpression=revision -q -DforceStdout)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/dist"
PROJECT_NAME="$(basename ${SCRIPT_DIR})"

java -cp "${DIST_DIR}/${PROJECT_NAME}-${VERSION}.jar:${DIST_DIR}/lib/*:${DIST_DIR}" io.github.sps4j.example.webflux.host.Main "$@"