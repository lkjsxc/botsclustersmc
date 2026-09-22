#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
source scripts/control-root.sh
command -v tar >/dev/null
mkdir -p diagnostics
stamp=$(date -u +%Y%m%dT%H%M%SZ)
dir="diagnostics/$stamp"
mkdir -p "$dir"
./scripts/doctor.sh > "$dir/doctor.txt" 2>&1
for f in logs/build.log logs/core-tests.log logs/launcher-tests.log logs/adapter-tests.log logs/learning.csv \
  logs/bridge-protocol-tests.log logs/academy-episodes.csv logs/folia.out.log logs/folia.err.log logs/bots.out.log logs/bots.err.log server/logs/latest.log; do
  [[ -f $f ]] && tail -n 200 "$f" > "$dir/${f//\//_}" || true
done
for f in state/status.json state/academy-status.json state/environment.txt; do
  [[ -f $f ]] && cp "$f" "$dir/" || true
done
for f in bin/build-manifest.txt bin/source.sha256 bin/artifacts.sha256 .runtime/run.json .runtime/server-ready.json .runtime/last-exit.txt .runtime/lab/bridge.ready .runtime/lab/campus.ready .runtime/lab/fatal.txt .runtime/resources.json; do
  [[ -f $f ]] && cp -- "$f" "$dir/${f//\//_}" || true
done
# No .env, Minecraft world, account tokens, or policy weights are included.
# Logs can contain names, addresses and paths. Review before sharing.
tar -czf "diagnostics-$stamp.tar.gz" -C diagnostics "$stamp"
printf 'Created %s; review names/addresses in the logs before sharing.\n' "diagnostics-$stamp.tar.gz"
