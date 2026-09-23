# Source from a top-level command after resolving its own directory.
if [[ ! -f .botsclustersmc-academy-v2 && -f academy-v2/.botsclustersmc-academy-v2 ]]; then
  [[ ! -L academy ]] || { echo 'Refusing linked academy directory.' >&2; exit 1; }
  cd -- academy
fi
export BCMC_ROOT="$PWD"
