# Changelog

## 0.5.0 — 2026-09-23 — integrated 64-actor Academy

- Connect the canonical RL-next mechanisms to the real Minecraft actor/learner:
  strict episode-boundary cohorts, policy/lesson leases, adaptive probes and
  rehearsals, per-actor frozen exams, timed GAE and potential shaping.
- Use the same conditional GUI distribution for sampling, old/new likelihood,
  policy/entropy gradients and exams. Validate final whole-batch sampled KL and
  retry weights, Adam and optimizer RNG as one transaction.
- Integrate 18 task environments and authoritative outcome gates through real
  Folia geometry, drops, inventory/cursor, crafting statistics, furnace extraction,
  chest contents and current platform occupancy. This is not learned mastery.
- Increase defaults and supported population to64, with 64 owned training cells
  and 16 additional connection slots for humans. Keep port25565 and prefixbcmc.
- Add observer-only 12-chunk viewing, configurable to16; keep actor view and
  simulation at3. Add a two-page actor menu, room switching, whole-campus overview,
  12-second tour, per-actor action bar and shared training status.
- Separate personal inventory and workbench GUI identity despite equal slot counts.
  Preserve individual episode drop provenance by disabling training-world merges.
- Introduce a new Academy-v2 observation/context schema and one atomic checkpoint
  binding model, optimizer, all sampling RNG states and adaptive course progress.
  Preserve the old academy/ directory; never silently import incompatible weights.
- Correct control-root routing, population guards, pinned API names and tests.
  Record real 64-client update/save/resume evidence and retained failure reports.
- Add separately built scripted fixture/observer diagnostics. These never initialize
  a model, write demonstrations, or enter the normal learned actor executable.
- Replace the obsolete component-only ZIP workflow with tests of canonical source.
  Source-first clone/build/start remains the public delivery path.

## 0.4.0 — source-first clone-to-train runtime

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
