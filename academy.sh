#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export BCMC_ROOT="$PWD"
source scripts/env.sh
source scripts/build-state.sh
source scripts/runtime-config.sh
bcmc_validate_runtime
./scripts/preflight.sh
if [[ -f .botsclustersmc-academy-v2 ]]; then exec ./scripts/run.sh "$@"; fi
lab="$BCMC_ROOT/academy-v2"
[[ ! -L $lab ]] || { echo 'Refusing a symlink academy-v2/.' >&2; exit 1; }
[[ $BOTS =~ ^[0-9]+$ ]] && ((10#$BOTS >= 1 && 10#$BOTS <= 64)) || { echo 'BOTS must be 1..64' >&2; exit 1; }
BOTS=$((10#$BOTS)); export BOTS
marker=$(printf 'botsclustersmc-academy-v2\nbots=%s\ncampus=8x16\n' "$BOTS")
if [[ -e $lab ]]; then
  [[ -d $lab && -f $lab/.botsclustersmc-academy-v2 && ! -L $lab/.botsclustersmc-academy-v2 ]] || {
    echo 'Existing academy is not owned by this version. Preserve it and use a fresh source directory.' >&2; exit 1;
  }
  [[ $(cat "$lab/.botsclustersmc-academy-v2") == "$marker" ]] || {
    echo 'Academy population/schema differs; refusing an implicit reset. Use a fresh directory.' >&2; exit 1;
  }
fi
if [[ ${EULA:-false} != true ]] && ! grep -qx 'eula=true' "$lab/server/eula.txt" 2>/dev/null; then
  echo 'Read https://aka.ms/MinecraftEULA, then run EULA=true ./start.sh' >&2; exit 1
fi
command -v flock >/dev/null || { echo 'Install util-linux (flock).' >&2; exit 1; }
# Preserve all pre-existing worlds and models. Claim ONLY a newly created folder.
if [[ ! -e $lab ]]; then mkdir -- "$lab"; printf '%s\n' "$marker" > "$lab/.botsclustersmc-academy-v2"; fi
for name in .runtime state server bin runtime app launcher learning scripts bridge docs tests .env; do
  [[ ! -L $lab/$name ]] || { echo "Refusing linked academy component: $name" >&2; exit 1; }
done
mkdir -p "$lab/.runtime"
exec 8> "$lab/.runtime/run.lock"
flock -n 8 || { echo 'Academy is already running or building.' >&2; exit 1; }
bcmc_acquire_build_lock
if ! bcmc_build_is_current; then ./scripts/build.sh --inherited-lock; fi
bcmc_build_is_current || { echo 'No verified current Rust build.' >&2; exit 1; }
for dir in app launcher learning scripts bridge bin docs tests pins; do
  mkdir -p "$lab/$dir"; cp -a "$dir/." "$lab/$dir/"
done
for file in start.sh stop.sh status.sh console.sh academy.sh VERSION README.md README.ja.md .env.example; do
  cp -a "$file" "$lab/$file"
done
if [[ -d runtime && ! -d $lab/runtime ]]; then cp -a --reflink=auto runtime "$lab/runtime"; fi
# Refresh host helpers when resuming a pre-repair Academy, without replacing its
# pinned server jar, world, configuration, or saved training state.
if [[ -f runtime/host-lib/gson.jar ]]; then
  mkdir -p "$lab/runtime/host-lib"
  [[ ! -L $lab/runtime/host-lib && ! -L $lab/runtime/host-lib/gson.jar ]] || {
    echo 'Refusing linked Academy host library.' >&2; exit 1;
  }
  cp -a runtime/host-lib/gson.jar "$lab/runtime/host-lib/gson.jar"
fi
for file in host-tools.jar host-tools.sha256 host-tools-artifact.sha256; do
  if [[ -f runtime/$file ]]; then
    [[ ! -L $lab/runtime/$file ]] || { echo 'Refusing linked Academy host tool.' >&2; exit 1; }
    cp -a "runtime/$file" "$lab/runtime/$file"
  fi
done
{
  for key in BOTS BOT_PREFIX SEED BIND_ADDRESS SERVER_PORT OFFLINE_ACCESS_ACK JAVA_HEAP_GB FOLIA_THREADS \
      VIEW_DISTANCE SIMULATION_DISTANCE WORLD_DIAMETER MIN_FREE_GB BCMC_MODE RUN_SECONDS \
      JAVA_BIN JAVAC_BIN FOLIA_BUILD ALLOW_EXPERIMENTAL_FOLIA CARGO_BUILD_JOBS BATCH_SAMPLES STARTUP_TIMEOUT_SECONDS BOT_VIEW_DISTANCE SPECTATOR_VIEW_DISTANCE BOT_JOIN_DELAY_MS COHORT_TIMEOUT_SECONDS; do
    if [[ -v $key ]]; then printf '%s=%q\n' "$key" "${!key}"; fi
  done
  printf '%s\n' 'BCMC_CURRICULUM=true' 'ACTION_HZ=5' "ROLLOUT_STEPS=$ROLLOUT_STEPS"
} > "$lab/.env"
printf 'botsclustersmc Academy: %s (%s bots). Status/stop commands work from this source root.\n' "$lab" "$BOTS"
cd -- "$lab"
export BCMC_ROOT="$lab" BCMC_CURRICULUM=true ACTION_HZ=5 ROLLOUT_STEPS="$ROLLOUT_STEPS" BCMC_ACADEMY_LOCK=1
exec ./scripts/run.sh
