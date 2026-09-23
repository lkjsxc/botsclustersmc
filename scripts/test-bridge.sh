#!/usr/bin/env bash
# Pure environment/UI contracts are not a replacement for actual Folia API compilation.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
stage=$(mktemp -d)
trap 'rm -rf -- "$stage"' EXIT
javac --release 21 -d "$stage" \
  bridge/src/org/botsclustersmc/lab/Protocol.java \
  bridge/src/org/botsclustersmc/lab/CampusPlan.java \
  bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java \
  bridge/src/org/botsclustersmc/lab/TaskFixtures.java \
  bridge/src/org/botsclustersmc/lab/ObserverState.java \
  bridge/ProtocolTest.java bridge/CampusPlanTest.java bridge/EnvironmentConfigTest.java \
  bridge/TaskFixturesTest.java bridge/ObserverStateTest.java tests/JavaSyntaxCheck.java
for suite in EnvironmentConfigTest ProtocolTest CampusPlanTest TaskFixturesTest ObserverStateTest; do java -cp "$stage" "$suite"; done
java -cp "$stage" JavaSyntaxCheck bridge/src/org/botsclustersmc/lab/*.java
