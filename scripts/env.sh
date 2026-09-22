# Source only operator-owned Bash config. Defaults < .env < exported variables.
_bcmc_root=$BCMC_ROOT
_bcmc_environment_snapshot=$(export -p)
set -a
source "$_bcmc_root/.env.example"
if [[ -f "$_bcmc_root/.env" ]]; then source "$_bcmc_root/.env"; fi
set +a
eval "$_bcmc_environment_snapshot"
export BCMC_ROOT="$_bcmc_root"
unset _bcmc_environment_snapshot _bcmc_root
