#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
java=${JAVA_BIN:-java}
java_path=$(readlink -f -- "$(command -v "$java")")
javac="$(dirname "$java_path")/javac"; jar="$(dirname "$java_path")/jar"
[[ -x $javac && -x $jar ]] || { echo 'A Java 21 JDK (java, javac, jar) is required.' >&2; exit 1; }
"$javac" -version 2>&1 | grep -Eq '^javac 21([.]|$)' || { echo 'A Java 21 JDK is required.' >&2; exit 1; }
./scripts/fetch-host-lib.sh
hash=$({ sha256sum scripts/HostTools.java scripts/build-host-tools.sh runtime/host-lib/gson.jar; } | sha256sum | awk '{print $1}')
if [[ -f runtime/host-tools.jar && -f runtime/host-tools.sha256 && -f runtime/host-tools-artifact.sha256 ]] &&
   [[ $(cat runtime/host-tools.sha256) == "$hash" ]] &&
   [[ $(sha256sum runtime/host-tools.jar) == "$(cat runtime/host-tools-artifact.sha256)" ]]; then exit 0; fi
mkdir -p .build runtime
stage=$(mktemp -d .build/host-tools.XXXXXX)
trap 'rm -rf -- "$stage"' EXIT
"$javac" -J-Xmx256m --release 21 -encoding UTF-8 -proc:none -cp runtime/host-lib/gson.jar -d "$stage" scripts/HostTools.java
"$jar" --create --file "$stage/host-tools.jar" -C "$stage" HostTools.class
mv -f "$stage/host-tools.jar" runtime/host-tools.jar
sha256sum runtime/host-tools.jar > runtime/host-tools-artifact.sha256
printf '%s\n' "$hash" > runtime/host-tools.sha256
