#!/usr/bin/env bash
# Host lifecycle only. Gameplay, PPO, rewards and curriculum stay in the pinned Rust client.
# Rust owns preparation/status/audits. SERVER_PORT is shared by preparation,
# the actual Folia process and the Azalea clients.
# Keep asynchronous children out of shell-managed job groups; setsid owns them.
set -Eeuo pipefail
set +m
umask 077
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export BCMC_ROOT="$PWD" BCMC_CURRICULUM=true
source scripts/env.sh
source scripts/runtime-config.sh
bcmc_validate_runtime
[[ -f .botsclustersmc-academy-v1 ]] || { echo 'Missing Academy ownership marker.' >&2; exit 1; }
mkdir -p .runtime logs state server
failure=''; server_pid=''; bots_pid=''; loggers=(); cleaned=false; stop_requested=false
log() { printf '[botsclustersmc] %s\n' "$*" >&2; }
die() { [[ $stop_requested == false ]] || exit 0; failure=$*; log "ERROR: $failure"; exit 1; }
atomic() { local dest=$1; cat > "$dest.new"; mv -f -- "$dest.new" "$dest"; }
free_disk() { local available; available=$(df -Pk "$BCMC_ROOT" | awk 'END {print $4}'); [[ $available =~ ^[0-9]+$ ]] && ((available >= MIN_FREE_GB*1024*1024)); }
wait_child() {
  local pid=$1 limit=$2 label=$3 began=$SECONDS result=0
  while kill -0 "$pid" 2>/dev/null; do
    if ((SECONDS-began >= limit)); then
      log "$label failed to stop within ${limit}s; terminating this owned child. Preserve state for diagnosis."
      kill -KILL "$pid" 2>/dev/null || true; result=1; break
    fi
    sleep 0.25
  done
  wait "$pid" || result=1
  return "$result"
}
cleanup() {
  local rc=$1
  [[ $cleaned == false ]] || return
  cleaned=true; trap - EXIT; trap '' INT TERM; set +e
  if [[ -n $server_pid || -n $bots_pid ]]; then
    log 'Stopping learning, saving checkpoints, then stopping Folia...'
    printf 'stop\n' > .runtime/stop
    [[ -z $bots_pid ]] || wait_child "$bots_pid" 90 learner || rc=1
    if [[ -n $server_pid ]]; then
      if kill -0 "$server_pid" 2>/dev/null; then printf 'stop\n' >&7; fi
      wait_child "$server_pid" 180 Folia || rc=1
    fi
    { exec 7>&-; } 2>/dev/null
    for pid in "${loggers[@]}"; do wait_child "$pid" 10 logger || rc=1; done
  fi
  if [[ -n ${run:-} && -s .runtime/lab/fatal.txt ]] && grep -q "^$run " .runtime/lab/fatal.txt; then
    rc=1; failure=$(cat .runtime/lab/fatal.txt); log "Late environment failure: $failure"
  fi
  rm -f .runtime/supervisor.pid .runtime/java.pid .runtime/bots.pid .runtime/console.fifo .runtime/*.pipe
  if ((rc == 0)); then
    printf 'clean shutdown\n' | atomic .runtime/last-exit.txt
    log 'Clean shutdown: model, curriculum and world saved.'
  else
    printf 'abnormal: %s\n' "${failure:-child or supervisor failed; inspect logs}" | atomic .runtime/last-exit.txt
  fi
  exit "$rc"
}
# An operator signal may interrupt a foreground probe/sleep. Normalize only that
# cancellation; child failures and actual fatal receipts are still checked below.
trap 'rc=$?; if [[ $stop_requested == true && -z $failure ]]; then rc=0; fi; cleanup "$rc"' EXIT
trap 'stop_requested=true; printf "stop\n" > .runtime/stop' INT TERM
command -v setsid >/dev/null || die 'Install util-linux (setsid and flock).'
free_disk || die "At least $MIN_FREE_GB GiB free disk is required. No data will be deleted."
# A configured Java heap plus modest native headroom must fit the actual cgroup.
if [[ -r /sys/fs/cgroup/memory.max ]]; then
  limit=$(cat /sys/fs/cgroup/memory.max)
  if [[ $limit =~ ^[0-9]+$ ]] && ((limit < (JAVA_HEAP_GB+1)*1024*1024*1024)); then
    die "JAVA_HEAP_GB=$JAVA_HEAP_GB does not fit this cgroup plus 1 GiB native headroom. Reduce the heap explicitly, or increase the allocation."
  fi
fi
./scripts/build-host-tools.sh || die 'Host tools could not be built with Java 21 JDK.'
java=${JAVA_BIN:-java}
host() { "$java" -Xms16m -Xmx64m -XX:ActiveProcessorCount=2 -cp runtime/host-tools.jar:runtime/host-lib/gson.jar HostTools "$@"; }
host available "$BIND_ADDRESS" "$SERVER_PORT" || die "TCP $BIND_ADDRESS:$SERVER_PORT is unavailable. Stop the other server; no existing process is killed."
# Reuse only dependency caches, never an unrelated world or checkpoint.
for name in libraries versions cache; do
  if [[ -d runtime/server-cache/$name && ! -e server/$name ]]; then
    cp -a --reflink=auto "runtime/server-cache/$name" "server/$name"
  fi
done
./bin/botsclustersmc-run prepare || die 'Pinned runtime / bridge preparation failed.'
[[ $stop_requested == false ]] || exit 0
[[ $(grep -c "^server-port=$SERVER_PORT$" server/server.properties) == 1 ]] || die 'Server port was not written exactly once.'
run="$(date +%s%N)-$$"
export BCMC_RUN_ID="$run"
mkdir -p .runtime/lab
rm -f .runtime/stop .runtime/server-ready.json .runtime/population.json state/status.json
rm -f .runtime/lab/{request,frame}-*.txt .runtime/lab/{bridge.ready,campus.ready,fatal.txt}
printf '{"run_id":"%s","started":%s,"bots":%s,"port":%s}\n' "$run" "$(date +%s)" "$BOTS" "$SERVER_PORT" | atomic .runtime/run.json
printf '%s\n' "$$" | atomic .runtime/supervisor.pid
printf 'running: no clean shutdown recorded yet\n' | atomic .runtime/last-exit.txt
local_host=$BIND_ADDRESS; [[ $local_host != 0.0.0.0 ]] || local_host=127.0.0.1
export BOT_SERVER="$local_host:$SERVER_PORT"
rm -f .runtime/console.fifo
mkfifo -m 600 .runtime/console.fifo
exec 7<> .runtime/console.fifo
start_logs() {
  local label=$1 channel
  for channel in out err; do
    rm -f ".runtime/$label-$channel.pipe"
    mkfifo -m 600 ".runtime/$label-$channel.pipe"
    # New sessions isolate terminal Ctrl-C from these children. Only the owned
    # supervisor requests checkpoint-first shutdown; loggers must survive it.
    setsid "$java" -Xms16m -Xmx64m -XX:ActiveProcessorCount=2 \
      -cp runtime/host-tools.jar:runtime/host-lib/gson.jar HostTools log "logs/$label.$channel.log" \
      < ".runtime/$label-$channel.pipe" 7>&- &
    loggers+=("$!")
  done
}
start_logs folia
log "Starting Folia 1.21.11 at $BIND_ADDRESS:$SERVER_PORT, $BOTS bots, Java heap ${JAVA_HEAP_GB}GiB."
(
  cd server
  exec setsid "$java" -Xms1G "-Xmx${JAVA_HEAP_GB}G" -XX:+UseG1GC -XX:ActiveProcessorCount=10 \
    -XX:ParallelGCThreads=2 -XX:ConcGCThreads=1 -XX:+ExitOnOutOfMemoryError \
    -XX:MaxDirectMemorySize=512M -XX:MaxMetaspaceSize=768M -Dio.netty.eventLoopThreads=2 \
    -Dterminal.jline=false -Dterminal.ansi=false -jar "$BCMC_ROOT/runtime/folia.jar" --nogui
) <&7 > .runtime/folia-out.pipe 2> .runtime/folia-err.pipe &
server_pid=$!; printf '%s\n' "$server_pid" | atomic .runtime/java.pid
began=$SECONDS; server_ready=false
while :; do
  [[ ! -f .runtime/stop ]] || exit 0
  kill -0 "$server_pid" 2>/dev/null || die 'Folia exited during startup; inspect logs/folia.*.log.'
  code=0; host campus "$BCMC_ROOT" "$run" "$BOTS" > /dev/null 2> .runtime/campus-error.txt || code=$?
  ((code != 1)) || die "$(cat .runtime/campus-error.txt)"
  if [[ $server_ready == false ]] && host probe "$local_host" "$SERVER_PORT" > .runtime/probe.json.new 2> .runtime/probe-error.txt; then
    mv .runtime/probe.json.new .runtime/probe.json
    printf '{"run_id":"%s","protocol":774,"port":%s}\n' "$run" "$SERVER_PORT" | atomic .runtime/server-ready.json
    server_ready=true
  fi
  if [[ $server_ready == true ]] && ((code == 0)); then break; fi
  ((SECONDS-began < STARTUP_TIMEOUT_SECONDS)) || die "Startup did not become ready in ${STARTUP_TIMEOUT_SECONDS}s; no bots started."
  sleep 1
done
printf 'worldborder set %s\n' "$WORLD_DIAMETER" >&7
log "All $BOTS enclosed training cells verified. Starting the Rust learner on $BOT_SERVER."
start_logs bots
RUN_SECONDS=0 setsid ./bin/botsclustersmc-bots </dev/null 7>&- > .runtime/bots-out.pipe 2> .runtime/bots-err.pipe &
bots_pid=$!; printf '%s\n' "$bots_pid" | atomic .runtime/bots.pid
began=$SECONDS; checked=$((SECONDS-5)); has_acted=false
while [[ ! -f .runtime/stop ]]; do
  kill -0 "$server_pid" 2>/dev/null || die 'Folia exited unexpectedly.'
  kill -0 "$bots_pid" 2>/dev/null || die 'Rust learner exited unexpectedly; inspect logs/bots.err.log.'
  for pid in "${loggers[@]}"; do kill -0 "$pid" 2>/dev/null || die 'A bounded log writer exited unexpectedly.'; done
  if ((RUN_SECONDS > 0 && SECONDS-began >= RUN_SECONDS)); then break; fi
  if [[ -f .runtime/lab/fatal.txt ]]; then die "$(cat .runtime/lab/fatal.txt)"; fi
  if ((SECONDS-checked >= 5)); then
    code=0; host status "$BCMC_ROOT" "$run" "$BOTS" "$BOT_PREFIX" "$has_acted" > .runtime/population.json.new 2> .runtime/status-error.txt || code=$?
    ((code != 1)) || die "$(cat .runtime/status-error.txt)"
    if [[ -s .runtime/population.json.new ]]; then mv .runtime/population.json.new .runtime/population.json; fi
    if ((code == 0)) && [[ $has_acted == false ]]; then
      has_acted=true; log "All $BOTS bots are online, spawned and making policy decisions."
    fi
    if [[ $has_acted == false ]] && ((SECONDS-began > 360)); then die 'Not all requested bots spawned and acted within 360 seconds.'; fi
    if [[ ! -f state/status.json ]] && ((SECONDS-began > 120)); then die 'No learner status was published.'; fi
    free_disk || die 'Free disk guard activated; preserving world and learning data.'
    java_rss=$(awk '/^VmRSS:/ {print $2}' "/proc/$server_pid/status" 2>/dev/null || true)
    bots_rss=$(awk '/^VmRSS:/ {print $2}' "/proc/$bots_pid/status" 2>/dev/null || true)
    printf '{"run_id":"%s","updated":%s,"java_rss_kib":%s,"bots_rss_kib":%s}\n' "$run" "$(date +%s)" "${java_rss:-0}" "${bots_rss:-0}" | atomic .runtime/resources.json
    # Retain 14 compressed Folia logs; never delete worlds, live logs or checkpoints.
    mapfile -t old_logs < <(find server/logs -maxdepth 1 -type f -name '*.log.gz' -printf '%T@ %p\n' 2>/dev/null | sort -n | cut -d' ' -f2-)
    if ((${#old_logs[@]} > 14)); then for ((i=0;i<${#old_logs[@]}-14;i++)); do rm -f -- "${old_logs[i]}"; done; fi
    checked=$SECONDS
  fi
  sleep 0.5
done
