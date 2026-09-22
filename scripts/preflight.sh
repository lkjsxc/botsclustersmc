#!/usr/bin/env bash
# Fail before a long Rust build, not after it. Never install system packages as root.
set -Eeuo pipefail
[[ $(uname -s) == Linux ]] || { echo 'Run this server on Linux (x86_64 or aarch64), not directly on macOS/Windows.' >&2; exit 1; }
case $(uname -m) in x86_64|aarch64) ;; *) echo 'Unsupported CPU architecture.' >&2; exit 1;; esac
for cmd in bash curl git cc pkg-config cmake tar unzip sha256sum flock setsid timeout; do
  command -v "$cmd" >/dev/null || {
    echo "Missing prerequisite: $cmd" >&2
    echo 'Ubuntu 24.04+: sudo apt-get install git curl ca-certificates build-essential pkg-config cmake unzip util-linux openjdk-21-jdk' >&2
    exit 1
  }
done
java=${JAVA_BIN:-java}
java_path=$(command -v "$java") || { echo 'Install a Java 21 JDK, or set JAVA_BIN.' >&2; exit 1; }
java_path=$(readlink -f -- "$java_path")
"$java_path" -version 2>&1 | grep -Eq 'version "21([.]|\")' || { echo 'Java 21 is required by the pinned Minecraft 1.21.11 runtime.' >&2; exit 1; }
for cmd in javac jar; do
  [[ -x $(dirname "$java_path")/$cmd ]] || { echo "Missing $cmd: install a JDK, not just a JRE." >&2; exit 1; }
done
