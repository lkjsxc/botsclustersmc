# Standalone checks for the integrated RL implementation

The canonical modules now live in `learning/src/next`. This crate re-exports those
exact sources through `src/lib.rs`; there is no separate implementation to install.
The actual Minecraft actors call the integrated native coordinator, timed PPO,
conditional distribution, adaptive curriculum and task-evidence gates.

```sh
cargo +nightly-2026-02-04 test --locked --all-targets
cargo +nightly-2026-02-04 run --locked --release -- benchmark
cargo +nightly-2026-02-04 run --locked --release -- stages
```

Run from this directory in a complete repository checkout. The relative canonical
source path intentionally requires the repository; copying only this folder is
not a standalone source package. No component-only runtime ZIP is published.

The benchmark is synthetic. Its result is not Minecraft learning-speed, human-like
behavior, reachability or cooperative-living evidence. See the root
[learning contract](../../docs/LEARNING.md) and [validation](../../docs/VALIDATION.md).
Historical component findings do not substitute for application acceptance.
