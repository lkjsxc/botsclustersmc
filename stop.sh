#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
source scripts/control-root.sh
[[ -d .runtime ]] || { echo 'No Academy run has been created.' >&2; exit 1; }
printf 'stop\n' > .runtime/stop
echo 'Stop requested. Wait for the foreground supervisor to finish saving and shutting down Folia.'
