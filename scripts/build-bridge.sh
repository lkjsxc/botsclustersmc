#!/usr/bin/env bash
# Compiles against the exact libraries extracted from the pinned Folia jar.
# No Maven snapshot resolution, no downloaded plugin binaries, no stub APIs.
set -Eeuo pipefail
: "${BCMC_ROOT:?}" "${BCMC_JAVA:?}" "${BCMC_FOLIA_JAR:?}"
cd -- "$BCMC_ROOT"
[[ ${BCMC_CURRICULUM:-false} == true && -f .botsclustersmc-academy-v1 ]] || {
  echo 'The environment bridge may only be installed in an isolated academy.' >&2; exit 1;
}
if [[ $BCMC_JAVA == */* ]]; then java_path=$(readlink -f -- "$BCMC_JAVA"); else java_path=$(readlink -f -- "$(command -v "$BCMC_JAVA")"); fi
javac=${JAVAC_BIN:-"$(dirname "$java_path")/javac"}
jar_tool="$(dirname "$java_path")/jar"
[[ -x $javac && -x $jar_tool ]] || {
  echo 'Academy needs a Java 21 JDK (javac and jar), not only a JRE.' >&2
  echo 'Install openjdk-21-jdk, or set JAVA_BIN to a JDK21 bin/java.' >&2; exit 1;
}
"$javac" -version 2>&1 | grep -Eq '^javac 21([.]|$)' || { echo 'javac must be version 21.' >&2; exit 1; }
mkdir -p .build logs runtime server/plugins
fingerprint=$({ sha256sum -- "$BCMC_FOLIA_JAR"; find bridge -type f -print0 | sort -z | xargs -0 sha256sum; sha256sum scripts/build-bridge.sh; } | sha256sum | awk '{print $1}')
if [[ -f runtime/bridge-source.sha256 && -f runtime/bridge-artifact.sha256 && -f server/plugins/BotsClustersMCLab.jar ]] \
  && [[ $(cat runtime/bridge-source.sha256) == "$fingerprint" ]] \
  && [[ $(sha256sum server/plugins/BotsClustersMCLab.jar) == "$(cat runtime/bridge-artifact.sha256)" ]]; then exit 0; fi
# patchonly exits before starting the server; it extracts/checks the server's own
# dependencies. The outer supervisor has already required EULA and stopped runs.
(cd server; timeout 900 "$java_path" -Xmx1G -Dpaperclip.patchonly=true -jar "$BCMC_FOLIA_JAR")
mapfile -d '' -t jars < <(find "$BCMC_ROOT/server/libraries" "$BCMC_ROOT/server/versions" -type f -name '*.jar' -print0 | sort -z)
(( ${#jars[@]} > 0 )) || { echo 'Folia API libraries were not extracted.' >&2; exit 1; }
classpath=$(IFS=:; printf '%s' "${jars[*]}")
stage=$(mktemp -d "$BCMC_ROOT/.build/bridge.XXXXXX")
trap 'rm -rf -- "$stage"' EXIT
mkdir -p "$stage/classes"
"$javac" -J-Xmx512m --release 21 -encoding UTF-8 -proc:none -cp "$classpath" \
  -d "$stage/classes" bridge/src/org/botsclustersmc/lab/*.java bridge/ProtocolTest.java bridge/CampusPlanTest.java bridge/EnvironmentConfigTest.java
"$java_path" -cp "$stage/classes" EnvironmentConfigTest | tee logs/bridge-environment-tests.log
"$java_path" -cp "$stage/classes" ProtocolTest | tee logs/bridge-protocol-tests.log
"$java_path" -cp "$stage/classes" CampusPlanTest | tee logs/bridge-campus-tests.log
"$jar_tool" --create --file "$stage/BotsClustersMCLab.jar" -C "$stage/classes" org -C bridge plugin.yml
mv -f "$stage/BotsClustersMCLab.jar" server/plugins/BotsClustersMCLab.jar
sha256sum server/plugins/BotsClustersMCLab.jar > runtime/bridge-artifact.sha256
printf '%s\n' "$fingerprint" > runtime/bridge-source.sha256
printf '%s\n' 'Environment bridge compiled; real Folia curriculum acceptance is a separate gate.'
