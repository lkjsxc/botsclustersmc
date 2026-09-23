#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
exec "${JAVA_BIN:-java}" -Xmx256m host/Host.java evaluate "$@"
