# 0.4.0 — source-first clone-to-train runtime

- Restore the real Rust actor, CPU PPO learner, Folia bridge, launch scripts,
  tests and operating documentation to the repository, separate from RL-next.
- Build from pinned source on first launch; reuse an already working exact Rust
  toolchain instead of contacting update servers on each compilation.
- Download/verify the missing Gson host dependency instead of requiring a ZIP.
- Track and use the reviewed Folia 1.21.11 build-14 pin on fresh installations.
- Make native prefix/port defaults agree with bcmc / 25565; remove the shell's
  legacy-port rewrite. Preserve checkpoint/environment semantics.
- Fail early for missing Linux/compiler/Java-JDK prerequisites.
- Preserve operator data and report evidence separately from skill claims.

# Changelog

## 0.3.1 — 2026-09-22 — runtime repair

- Repair the invalid14-character default bot prefix; reserve bcmc00..bcmc31, validate operator configuration before creating/starting a world.
- Make port25565 the default effective host/client/probe port through one foreground host supervisor. Retain the original Rust native component unmodified and document unsupported legacy direct launch paths.
- Publish plugin initialization failures synchronously before the asynchronous writer exists; monitor failure during boot, training and shutdown.
- Treat Folia entity task retirement as normal disconnect/shutdown cleanup; discard obsolete sessions without rewards. Guard holding teleport completion against disconnected/replaced players.
- Verify all32 cell receipts, real Minecraft protocol, all32 player decisions and fresh status; preserve no-silent-population-reduction and fail-closed behavior.
- Isolate owned child sessions so terminal Ctrl-C cannot bypass checkpoint-first shutdown; tolerate interrupted probes and repeated cancellation during cleanup.
- Add bounded log writer, disk/cgroup checks, requested-port conflict detection, checkpoint-first stop, late-fatal checking and root console/status routing.
- Refresh host libraries on an existing owned Academy under its lock without replacing the world/checkpoints/pinned server jar.
- Ship recovered pinned runtime dependencies and honest imported-native provenance; normal unchanged startup needs Java21 JDK but no Rust/Cargo compilation.
- Add host/packaging/configuration regression tests and real two-run Folia/PPO checkpoint evidence. See validation report for measured scope, failed diagnostic attempts and unverified skills.
