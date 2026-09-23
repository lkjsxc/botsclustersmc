#!/usr/bin/env bash
# Real two-run Folia acceptance. No fake server, synthetic transitions or tasks.
set -Eeuo pipefail
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export BCMC_ROOT="$PWD"
source scripts/env.sh
source scripts/build-state.sh
[[ ${EULA:-false} == true ]] || {
  echo 'Read the Minecraft EULA, then run: EULA=true ./smoke.sh' >&2; exit 1;
}
academy_mode=true
hz=5;verifier=verify-academy
bots=${SMOKE_BOTS:-2}
[[ $bots =~ ^([1-9]|[12][0-9]|3[0-2])$ ]] || { echo 'SMOKE_BOTS must be 1..64' >&2; exit 1; }
first=${SMOKE_SECONDS:-300}
second=${SMOKE_RESTART_SECONDS:-180}
for seconds in "$first" "$second"; do
  [[ $seconds =~ ^[0-9]+$ ]] && (( seconds >= 120 && seconds <= 3600 )) || {
    echo 'SMOKE_SECONDS / SMOKE_RESTART_SECONDS must be 120..3600.' >&2; exit 1;
  }
done
bcmc_acquire_build_lock
if ! bcmc_build_is_current; then ./scripts/build.sh --inherited-lock; fi
bcmc_build_is_current || { echo 'No current build; no Minecraft test was attempted.' >&2; exit 1; }
mkdir -p experiments
lab=$(mktemp -d "$BCMC_ROOT/experiments/smoke-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX")
# No original server/, state/, .runtime/, or .env is copied. This creates a new
# research world and weights, and cannot overwrite the operator's colony.
for name in app launcher scripts bridge bin docs tests pins; do cp -a "$name" "$lab/"; done
mkdir -p "$lab/learning"
cp -a learning/src learning/Cargo.toml "$lab/learning/"
for name in start.sh stop.sh status.sh console.sh academy.sh VERSION README.md README.ja.md .env.example; do cp -a "$name" "$lab/"; done
if [[ -d runtime ]]; then cp -a --reflink=auto runtime "$lab/"; fi
mkdir -p "$lab/evidence"
printf 'botsclustersmc-academy-v2\nbots=%s\ncampus=8x16\n' "$bots" > "$lab/.botsclustersmc-academy-v2"
echo "Isolated real-Minecraft test directory: $lab"
trap 'printf "stop\n" > "$lab/.runtime/stop" 2>/dev/null || true; exit 130' INT TERM
trap 'echo "Preserved test world, state and evidence: $lab"' EXIT
run_round() {
  local seconds=$1 name=$2
  (
    cd -- "$lab"
    # Experiment settings, not capacity or learning-time claims.
    EULA=true BCMC_MODE=train BOTS="$bots" BCMC_CURRICULUM="$academy_mode" ACTION_HZ="$hz" ROLLOUT_STEPS=32 BATCH_SAMPLES=64 \
      JAVA_HEAP_GB="${SMOKE_HEAP_GB:-3}" FOLIA_THREADS=2 RUN_SECONDS="$seconds" \
      BIND_ADDRESS=127.0.0.1 OFFLINE_ACCESS_ACK=false \
      ./start.sh
  ) 2>&1 | tee "$lab/evidence/$name-supervisor.log"
}
run_round "$first" round1
BCMC_ROOT="$lab" "$lab/bin/botsclustersmc-run" "$verifier" | tee "$lab/evidence/round1-verification.txt"
cp "$lab/state/status.json" "$lab/evidence/round1-status.json"
cp "$lab/.runtime/run.json" "$lab/evidence/round1-run.json"
cp "$lab/.runtime/server-ready.json" "$lab/evidence/round1-server-ready.json"
run_round "$second" round2
BCMC_ROOT="$lab" "$lab/bin/botsclustersmc-run" "$verifier" "$lab/evidence/round1-status.json" | tee "$lab/evidence/round2-verification.txt"
cp "$lab/state/status.json" "$lab/evidence/round2-status.json"
cp "$lab/.runtime/run.json" "$lab/evidence/round2-run.json"
cp "$lab/.runtime/server-ready.json" "$lab/evidence/round2-server-ready.json"
printf '%s\n' 'Real two-run lifecycle acceptance passed. No cooperative living or learned-skill claim.'
