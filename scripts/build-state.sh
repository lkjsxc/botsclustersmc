# Sourced from project entrypoints. Source/binary receipts are local integrity
# checks, not publisher signatures. Runtime bundles disclose imported binaries.
BCMC_AZALEA_REV=f8ddefa70cc53e6385785fb56e7a688a389cf0ab
BCMC_TOOLCHAIN=nightly-2026-02-04

# Native component fingerprint, not a claim that this host compiled it.
# Bridge, shell and documentation changes do not invalidate unchanged Rust code.
bcmc_source_fingerprint() (
  set -o pipefail
  cd -- "$BCMC_ROOT"
  {
    printf '%s\n' "Azalea=$BCMC_AZALEA_REV" "Rust=$BCMC_TOOLCHAIN" \
      'release: debug=0 lto=false codegen-units=8; launcher rustc -O'
    {
      printf '%s\0' learning/Cargo.toml pins/azalea-client.patch
      find app launcher learning/src -type f -name '*.rs' -print0
    } | LC_ALL=C sort -zu | while IFS= read -r -d '' file; do
      sha256sum -- "$file" || exit
    done
  } | sha256sum | awk '{print $1}'
)

bcmc_build_is_current() (
  cd -- "$BCMC_ROOT"
  [[ -x bin/botsclustersmc-run && -x bin/botsclustersmc-bots && -s bin/source.sha256 && -s bin/artifacts.sha256 ]] || exit 1
  [[ -f bin/target.txt && $(cat bin/target.txt) == "$(uname -s)/$(uname -m)" ]] || exit 1
  local expected
  expected=$(bcmc_source_fingerprint) || exit
  [[ $(cat bin/source.sha256) == "$expected" ]] || exit 1
  # Compare fixed paths, not arbitrary paths read from the receipt.
  [[ $(sha256sum bin/botsclustersmc-run bin/botsclustersmc-bots) == "$(cat bin/artifacts.sha256)" ]]
)

bcmc_acquire_build_lock() {
  mkdir -p "$BCMC_ROOT/.runtime"
  if [[ ${1:-} == --inherited-lock ]]; then
    [[ $(readlink "/proc/$$/fd/9" 2>/dev/null) == "$BCMC_ROOT/.runtime/run.lock" ]] || {
      echo 'Missing inherited installation lock.' >&2; return 1;
    }
  else
    exec 9> "$BCMC_ROOT/.runtime/run.lock"
  fi
  flock -n 9 || { echo 'This botsclustersmc directory is already running or building.' >&2; return 1; }
}
