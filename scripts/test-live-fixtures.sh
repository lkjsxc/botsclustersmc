#!/usr/bin/env bash
# Independent scripted reachability/observer checks. NEVER produces training data.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export BCMC_ROOT="$PWD"
source scripts/env.sh
source scripts/build-state.sh
[[ ${EULA:-false} == true ]] || { echo 'Read the Minecraft EULA, then explicitly set EULA=true for this isolated test.' >&2; exit 1; }
bcmc_acquire_build_lock
bcmc_build_is_current || { echo 'Build the real application first with scripts/build.sh.' >&2; exit 1; }
export RUSTUP_HOME="${RUSTUP_HOME:-$PWD/.build/rustup}" CARGO_HOME="${CARGO_HOME:-$PWD/.build/cargo}"
export PATH="$CARGO_HOME/bin:$PATH" CARGO_BUILD_JOBS=2 CARGO_PROFILE_RELEASE_DEBUG=0 CARGO_PROFILE_RELEASE_LTO=false CARGO_PROFILE_RELEASE_CODEGEN_UNITS=8 CARGO_INCREMENTAL=0
repo="$PWD/.build/azalea"
[[ $(git -C "$repo" rev-parse HEAD) == "$BCMC_AZALEA_REV" ]]
mkdir -p "$repo/azalea/examples/bcmc_fixture_check"
cp tests/live-client.rs "$repo/azalea/examples/bcmc_fixture_check/main.rs"
(cd "$repo"; rustup run "$BCMC_TOOLCHAIN" cargo build --locked --release -p azalea --example bcmc_fixture_check --no-default-features --features packet-event)
rustup run "$BCMC_TOOLCHAIN" rustc --edition=2024 -O tests/prepare-live.rs -o .build/prepare-live
python3 tests/live-fixtures.py "$repo/target/release/examples/bcmc_fixture_check" "$PWD/.build/prepare-live"
