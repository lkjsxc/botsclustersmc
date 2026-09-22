#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
source scripts/control-root.sh
command=${*:-}
[[ -n $command && ${#command} -le 1024 && $command != *$'\n'* && $command != *$'\r'* ]] || {
  echo 'Usage: ./console.sh "one Minecraft console command" (up to 1024 characters).' >&2; exit 1;
}
[[ -p .runtime/console.fifo && -s .runtime/supervisor.pid && -s .runtime/java.pid ]] || { echo 'No running Academy console.' >&2; exit 1; }
pid=$(cat .runtime/supervisor.pid)
[[ $pid =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null || { echo 'The supervisor is not running.' >&2; exit 1; }
timeout 5 bash -c 'printf "%s\n" "$1" > "$2"' _ "$command" "$PWD/.runtime/console.fifo"
printf 'Command submitted. Response: %s/logs/folia.out.log\n' "$PWD"
