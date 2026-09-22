#!/usr/bin/env bash
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export BCMC_ROOT="$PWD"
source scripts/env.sh
source scripts/build-state.sh
bcmc_acquire_build_lock
export RUSTUP_HOME="${RUSTUP_HOME:-$PWD/.build/rustup}"
export CARGO_HOME="${CARGO_HOME:-$PWD/.build/cargo}"
export PATH="$CARGO_HOME/bin:$PATH"
mkdir -p .build logs
for name in learning launcher; do
  source_file="$name/src/lib.rs"
  [[ $name == launcher ]] && source_file=launcher/main.rs
  rustup run "$BCMC_TOOLCHAIN" rustc --edition=2024 --test -O "$source_file" -o ".build/$name-tests"
  ".build/$name-tests" --nocapture | tee "logs/$name-tests.log"
done
