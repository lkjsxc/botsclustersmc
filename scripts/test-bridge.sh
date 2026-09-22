#!/usr/bin/env bash
# Execute the real pure planner/protocol, then PARSE plugin syntax without stubs.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
stage=$(mktemp -d)
trap 'rm -rf -- "$stage"' EXIT
javac --release 21 -d "$stage" bridge/src/org/botsclustersmc/lab/Protocol.java \
  bridge/src/org/botsclustersmc/lab/CampusPlan.java bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java \
  bridge/ProtocolTest.java bridge/CampusPlanTest.java bridge/EnvironmentConfigTest.java tests/JavaSyntaxCheck.java
java -cp "$stage" EnvironmentConfigTest
java -cp "$stage" ProtocolTest
java -cp "$stage" CampusPlanTest
java -cp "$stage" JavaSyntaxCheck bridge/src/org/botsclustersmc/lab/*.java
