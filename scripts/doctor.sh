#!/usr/bin/env bash
set -u
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
source scripts/control-root.sh
echo '=== Host (this is not a benchmark) ==='
uname -a
command -v nproc >/dev/null && nproc
free -h 2>/dev/null || true
df -h .
for file in /sys/fs/cgroup/memory.max /sys/fs/cgroup/cpu.max; do
  [[ -f $file ]] && printf '%s: %s\n' "$file" "$(cat "$file")"
done
echo '=== Required build tools ==='
for c in curl git tar sha256sum cc df flock java javac jar; do command -v "$c" || true; done
echo '=== Source/binary integrity (not compiler or server verification) ==='
export BCMC_ROOT="$PWD"
source scripts/build-state.sh
if bcmc_build_is_current; then echo 'Build receipts match current source and binary hashes.'; else echo 'No matching completed build receipt; start.sh will rebuild.'; fi
echo '=== Build identity ===' 
cat bin/build-manifest.txt 2>/dev/null || true
cat runtime/folia.lock 2>/dev/null || true
echo '=== Running marker; stale PID is not proof of a running process ==='
cat .runtime/supervisor.pid 2>/dev/null || true
echo '=== Latest metrics; check the updated timestamp ==='
cat state/status.json 2>/dev/null || true

echo "=== Child RSS (KiB) and process CPU; not cgroup total ==="
for f in .runtime/java.pid .runtime/bots.pid; do
  if [[ -f $f ]]; then
    pid=$(cat "$f")
    [[ $pid =~ ^[0-9]+$ ]] && ps -p "$pid" -o pid,comm,rss,pcpu,nlwp || true
  fi
done
cat .runtime/last-exit.txt 2>/dev/null || true

echo "=== Academy (separate world/state; not a skill certificate) ==="
cat state/academy-status.json 2>/dev/null || true
cat .runtime/lab/bridge.ready .runtime/lab/fatal.txt 2>/dev/null || true
