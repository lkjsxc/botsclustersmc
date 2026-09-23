# Runtime-only configuration, shared by the source entry and Academy entry.
# The native Rust client supports prefixes of 1..13 ASCII characters.
bcmc_validate_runtime() {
  local name value part
  local -a octets
  [[ ${BOT_PREFIX:-} =~ ^[a-zA-Z0-9_]{1,13}$ ]] || {
    echo 'BOT_PREFIX must be 1..13 ASCII letters, digits or underscores. Use bcmc (bcmc00..bcmc63); botsclustersmc is too long for the pinned client.' >&2; return 1;
  }
  for name in BOTS SERVER_PORT JAVA_HEAP_GB FOLIA_THREADS MIN_FREE_GB RUN_SECONDS STARTUP_TIMEOUT_SECONDS BOT_VIEW_DISTANCE SPECTATOR_VIEW_DISTANCE BOT_JOIN_DELAY_MS COHORT_TIMEOUT_SECONDS; do
    value=${!name:-}
    [[ $value =~ ^[0-9]{1,9}$ ]] || { echo "$name must be an unsigned integer." >&2; return 1; }
    printf -v "$name" '%d' "$((10#$value))"; export "$name"
  done
  (( BOTS >= 1 && BOTS <= 64 )) || { echo 'BOTS must be 1..64.' >&2; return 1; }
  (( SERVER_PORT >= 1 && SERVER_PORT <= 65535 )) || { echo 'SERVER_PORT must be 1..65535.' >&2; return 1; }
  (( JAVA_HEAP_GB >= 2 && JAVA_HEAP_GB <= 8 )) || { echo 'JAVA_HEAP_GB must be 2..8.' >&2; return 1; }
  (( FOLIA_THREADS >= 1 && FOLIA_THREADS <= 10 )) || { echo 'FOLIA_THREADS must be 1..10.' >&2; return 1; }
  (( MIN_FREE_GB >= 2 && MIN_FREE_GB <= 100 )) || { echo 'MIN_FREE_GB must be 2..100.' >&2; return 1; }
  (( RUN_SECONDS <= 864000 )) || { echo 'RUN_SECONDS must be 0..864000.' >&2; return 1; }
  (( STARTUP_TIMEOUT_SECONDS >= 30 && STARTUP_TIMEOUT_SECONDS <= 1800 )) || { echo 'STARTUP_TIMEOUT_SECONDS must be 30..1800.' >&2; return 1; }
  (( BOT_VIEW_DISTANCE >= 3 && BOT_VIEW_DISTANCE <= 6 )) || { echo 'BOT_VIEW_DISTANCE must be 3..6.' >&2; return 1; }
  (( SPECTATOR_VIEW_DISTANCE >= 3 && SPECTATOR_VIEW_DISTANCE <= 16 )) || { echo 'SPECTATOR_VIEW_DISTANCE must be 3..16.' >&2; return 1; }
  (( BOT_JOIN_DELAY_MS >= 100 && BOT_JOIN_DELAY_MS <= 3000 )) || { echo 'BOT_JOIN_DELAY_MS must be 100..3000.' >&2; return 1; }
  (( COHORT_TIMEOUT_SECONDS >= 180 && COHORT_TIMEOUT_SECONDS <= 3600 )) || { echo 'COHORT_TIMEOUT_SECONDS must be 180..3600.' >&2; return 1; }
  [[ ${BIND_ADDRESS:-} =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || { echo 'BIND_ADDRESS must be an IPv4 address.' >&2; return 1; }
  IFS=. read -r -a octets <<< "$BIND_ADDRESS"
  for part in "${octets[@]}"; do ((10#$part <= 255)) || { echo 'Invalid IPv4 address.' >&2; return 1; }; done
  [[ $BIND_ADDRESS == 127.* || ${OFFLINE_ACCESS_ACK:-false} == true ]] || {
    echo "Offline mode has NO account authentication. Restrict TCP $SERVER_PORT before setting OFFLINE_ACCESS_ACK=true." >&2; return 1;
  }
  [[ ${BCMC_MODE:-} =~ ^(train|eval|random)$ ]] || { echo 'BCMC_MODE must be train, eval or random.' >&2; return 1; }
}
