#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
source scripts/control-root.sh
[[ -x bin/botsclustersmc-run ]] || { echo 'No native runtime. Inspect bin/ and the build manifest.' >&2; exit 1; }
if [[ ${1:-} != --json ]]; then
  running=false
  if [[ -s .runtime/supervisor.pid ]]; then
    pid=$(cat .runtime/supervisor.pid)
    if [[ $pid =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then running=true; fi
  fi
  if [[ $running == true ]]; then echo 'Supervisor: running'; else echo 'Supervisor: stopped (any metrics below describe the LAST run, not live bots).'; fi
  cat .runtime/last-exit.txt .runtime/population.json 2>/dev/null || true
fi
[[ -s state/status.json ]] || { echo 'No learner status yet. During startup, inspect logs/folia.out.log and .runtime/last-exit.txt.' >&2; exit 2; }
exec ./bin/botsclustersmc-run status "$@"
