#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
exec "${JAVA_BIN:-java}" host/Host.java stop "$@"
