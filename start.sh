#!/usr/bin/env bash
# The one public startup path: never fall back to wilderness training.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ -f .botsclustersmc-academy-v2 ]]; then
  exec ./scripts/run.sh "$@"
fi
exec ./academy.sh "$@"
