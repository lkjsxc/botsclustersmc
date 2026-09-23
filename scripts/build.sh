#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export BCMC_ROOT="$PWD"
source "$BCMC_ROOT/scripts/env.sh"
source "$BCMC_ROOT/scripts/build-state.sh"
AZALEA_REV=$BCMC_AZALEA_REV
TOOLCHAIN=$BCMC_TOOLCHAIN
[[ $(uname -s) == Linux ]] || { echo 'Linux is required.' >&2; exit 1; }
case $(uname -m) in x86_64|aarch64) ;; *) echo 'Supported CPUs: x86_64, aarch64' >&2; exit 1;; esac
for cmd in curl git tar sha256sum cc df awk find sort readlink flock; do
  command -v "$cmd" >/dev/null || {
    echo "Missing prerequisite: $cmd" >&2
    echo 'Ubuntu/Debian: sudo apt-get install ca-certificates curl git build-essential pkg-config cmake unzip util-linux' >&2
    echo 'Arch: sudo pacman -S --needed base-devel curl git cmake unzip util-linux' >&2
    exit 1
  }
done
bcmc_acquire_build_lock "${1:-}"
# Keep room for dependency compilation; the runtime separately reserves 10 GiB.
_available_kib=$(LC_ALL=C df -Pk "$BCMC_ROOT" | awk 'END {print $4}')
[[ $_available_kib =~ ^[0-9]+$ ]] && (( _available_kib >= 20 * 1024 * 1024 )) || {
  echo 'At least 20 GiB free disk is required before the initial Rust build.' >&2
  exit 1
}
unset _available_kib
# Build while Folia is stopped. Cap parallel rustc jobs; no full-size LTO.
export CARGO_BUILD_JOBS="${CARGO_BUILD_JOBS:-2}"
[[ $CARGO_BUILD_JOBS =~ ^[1-4]$ ]] || { echo 'CARGO_BUILD_JOBS must be 1..4' >&2; exit 1; }
export RUSTUP_HOME="${RUSTUP_HOME:-$BCMC_ROOT/.build/rustup}"
export CARGO_HOME="${CARGO_HOME:-$BCMC_ROOT/.build/cargo}"
export PATH="$CARGO_HOME/bin:$PATH"
export CARGO_PROFILE_RELEASE_DEBUG=0
export CARGO_PROFILE_RELEASE_LTO=false
export CARGO_PROFILE_RELEASE_CODEGEN_UNITS=8
export CARGO_NET_GIT_FETCH_WITH_CLI=true
export CARGO_INCREMENTAL=0
mkdir -p .build bin logs
stage=$(mktemp -d "$BCMC_ROOT/.build/install.XXXXXX")
trap 'rm -rf -- "$stage"' EXIT
source_before=$(bcmc_source_fingerprint)
if ! command -v rustup >/dev/null; then
  curl --fail --location --proto '=https' --tlsv1.2 --retry 3 \
    https://sh.rustup.rs -o .build/rustup-init.sh
  # rustup's official HTTPS installer is the initial toolchain trust root.
  sh .build/rustup-init.sh -y --no-modify-path --profile minimal --default-toolchain none
fi
if ! rustup run "$TOOLCHAIN" rustc --version >/dev/null 2>&1; then
  rustup toolchain install "$TOOLCHAIN" --profile minimal
fi
rustup run "$TOOLCHAIN" rustc --version
rustup run "$TOOLCHAIN" rustc --edition=2024 --test -O learning/src/lib.rs -o .build/learning-tests
.build/learning-tests --nocapture | tee logs/core-tests.log
rustup run "$TOOLCHAIN" rustc --edition=2024 --test -O launcher/main.rs -o .build/launcher-tests
.build/launcher-tests --nocapture | tee logs/launcher-tests.log
rustup run "$TOOLCHAIN" rustc --edition=2024 -O launcher/main.rs -o "$stage/botsclustersmc-run"

repo="$BCMC_ROOT/.build/azalea"
if [[ ! -d $repo/.git ]]; then
  mkdir -p "$repo"
  git -C "$repo" init
  git -C "$repo" remote add origin https://github.com/azalea-rs/azalea.git
fi
if ! git -C "$repo" cat-file -e "$AZALEA_REV^{commit}" 2>/dev/null; then
  git -C "$repo" fetch --depth=1 origin "$AZALEA_REV"
fi
# This is a disposable vendor checkout, never the user's repository.
git -C "$repo" checkout --detach "$AZALEA_REV"
[[ $(git -C "$repo" rev-parse HEAD) == "$AZALEA_REV" ]]
git -C "$repo" diff --exit-code -- Cargo.lock Cargo.toml azalea/Cargo.toml
# Restore only this declared file in the disposable vendor checkout, then
# apply the repository-owned protocol correction to the exact pinned revision.
# The operator's checkout and data are never reset by this step.
git -C "$repo" restore --source="$AZALEA_REV" --worktree -- azalea-client/src/plugins/packet/game/mod.rs azalea-client/src/plugins/interact/pick.rs
git -C "$repo" apply --check "$BCMC_ROOT/pins/azalea-client.patch"
git -C "$repo" apply "$BCMC_ROOT/pins/azalea-client.patch"
# The example reuses the upstream workspace's exact Cargo.lock and dependency set.
example="$repo/azalea/examples/botsclustersmc"
mkdir -p "$example/core"
cp app/*.rs "$example/"
cp -a learning/src/. "$example/core/"
(
  cd "$repo"
  rustup run "$TOOLCHAIN" cargo test --locked --release -p azalea \
    --example botsclustersmc --no-default-features --features packet-event \
    -- --test-threads=1 2>&1 | tee "$BCMC_ROOT/logs/adapter-tests.log"
  rustup run "$TOOLCHAIN" cargo build --locked --release -p azalea \
    --example botsclustersmc --no-default-features --features packet-event
)
cp "$repo/target/release/examples/botsclustersmc" "$stage/botsclustersmc-bots"
chmod 755 "$stage/botsclustersmc-run" "$stage/botsclustersmc-bots"
[[ $(bcmc_source_fingerprint) == "$source_before" ]] || {
  echo 'Source changed during compilation. No new binaries were installed.' >&2; exit 1;
}
# Publish under the installation lock; the source receipt is the LAST write.
# A crash between moves leaves no passing source+artifact receipt.
mv -f "$stage/botsclustersmc-run" bin/botsclustersmc-run
mv -f "$stage/botsclustersmc-bots" bin/botsclustersmc-bots
sha256sum bin/botsclustersmc-run bin/botsclustersmc-bots > "$stage/artifacts.sha256"
{
  printf 'botsclustersmc source %s\n' "$(cat VERSION)"
  echo "Azalea=$AZALEA_REV"
  echo "AzaleaPatch=pins/azalea-client.patch"
  echo 'Minecraft=1.21.11'
  echo "Rust=$TOOLCHAIN"
  echo "Source=$source_before"
  rustup run "$TOOLCHAIN" rustc --version --verbose
  sha256sum "$repo/Cargo.lock"
  cat "$stage/artifacts.sha256"
} > "$stage/build-manifest.txt"
printf '%s\n' "$source_before" > "$stage/source.sha256"
mv -f "$stage/artifacts.sha256" bin/artifacts.sha256
mv -f "$stage/build-manifest.txt" bin/build-manifest.txt
printf '{"native_origin":"locally compiled by scripts/build.sh","source":"%s","toolchain":"%s"}\n' "$source_before" "$TOOLCHAIN" > bin/provenance.json
printf '%s/%s\n' "$(uname -s)" "$(uname -m)" > bin/target.txt
mv -f "$stage/source.sha256" bin/source.sha256
bcmc_build_is_current || { echo 'Installed build failed its integrity check.' >&2; exit 1; }
printf '%s\n' 'Build and local Rust unit tests completed.' \
  'Minecraft/Folia smoke testing is a separate gate: see docs/VALIDATION.md.'
