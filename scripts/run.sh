#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export BCMC_ROOT="$PWD"
source "$BCMC_ROOT/scripts/env.sh"
source "$BCMC_ROOT/scripts/build-state.sh"
source "$BCMC_ROOT/scripts/runtime-config.sh"
bcmc_validate_runtime
[[ -f .botsclustersmc-academy-v2 ]] || { echo "Dedicated Academy ownership marker missing; use ./start.sh from the source root." >&2; exit 1; }
export BCMC_CURRICULUM=true
mkdir -p .runtime logs
command -v flock >/dev/null || { echo 'Install util-linux (flock) first.' >&2; exit 1; }
if [[ ${BCMC_ACADEMY_LOCK:-0} == 1 ]]; then
  [[ $(readlink "/proc/$$/fd/8" 2>/dev/null) == "$BCMC_ROOT/.runtime/run.lock" ]] || { echo 'Invalid inherited academy lock.' >&2; exit 1; }
  exec 9>&8
else
  exec 9> .runtime/run.lock
fi
flock -n 9 || { echo 'This botsclustersmc directory is already running or building.' >&2; exit 1; }
if [[ ${EULA:-false} != true ]] && ! grep -qx 'eula=true' server/eula.txt 2>/dev/null; then
  printf '%s\n' 'Minecraft EULA: https://aka.ms/MinecraftEULA' \
    'Read it, then run: EULA=true ./start.sh' >&2
  exit 1
fi
if ! bcmc_build_is_current; then
  echo 'Source/binary receipt missing or stale; building the current source.' >&2
  ./scripts/build.sh --inherited-lock 2>&1 | tee logs/build.log
fi
bcmc_build_is_current || { echo 'Build receipt did not verify; refusing to launch.' >&2; exit 1; }
# The supervisor remains in the foreground. Ctrl-C or ./stop.sh saves and stops.
exec ./scripts/supervise.sh
