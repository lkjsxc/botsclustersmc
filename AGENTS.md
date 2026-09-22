# botsclustersmc engineering contract

The product is a mostly-Rust, pure-RL Minecraft/Folia experiment for a 16 CPU,
12 GiB RAM, 120 GB Linux allocation. Communication with the operator is Japanese.
Do not replace implementation with plans. Preserve operator worlds and evidence.

Read README.ja.md, docs/LEARNING.md and docs/VALIDATION.md before changes. The current
delivery is a source-first runnable repository. The root application is separate
from experimental/rl-next, whose 18 task contracts are not live Minecraft stages.
Never invent successful compiler/server/learning/skill runs.

## Learning integrity

Neural policy chooses every gameplay action. No pathfinding, auto-aim, tool choice,
recipe/build/harvest macro, imitation/LLM/demonstration, fake rewards or fallback
controller. Explicit diagnostic random mode is not training. Stage masks are
fixed task-wide input availability, never target-dependent answers. Goal observations,
reward engineering, actuator constraints and environment resets must be disclosed.

Rust owns curriculum/rewards/PPO/evaluation. Java host tools own lifecycle validation/logging only; they never choose gameplay.
The tiny Java bridge owns only real-Folia
region-safe geometry/resets and entity-safe telemetry. Use entity schedulers for
players, region schedulers for blocks, teleportAsync for teleport. No cross-region
raw mutations or unsupported world loading. No Bukkit stubs as compatibility proof.

Hold policies frozen during exams; do not enqueue exam trajectories. Rehearse and
examine every earlier task. Keep per-agent gates: average success is insufficient.
No elapsed-time promotion or auto-release into an unimplemented settlement.
Data from resets, stale tokens or client block predictions cannot become successes.

## Delivery and persistence

Keep version/protocol pins until a verified migration: Folia/Minecraft 1.21.11,
protocol774, Java21, Azalea f8ddefa70cc53e6385785fb56e7a688a389cf0ab and
nightly-2026-02-04. Never claim an imported binary was locally compiled. A source receipt alone is not a build test.
Use component-specific receipts. Run build tests before publishing a genuinely new native build. Bridge is
compiled against the exact server's extracted API libraries, not floating Maven.

`start.sh` always dispatches to the Academy. `academy.sh` must not import or overwrite original server/state. Reject unowned and
linked training components. Keep run/build exclusion, port check, offline-network
warning, explicit EULA consent, bounded logs/queues and disk guard. State schema
or input/action meaning changes require a new context marker or explicit tested
migration, not same-sized silent weight reuse. Pair course checkpoint with policy
version/fingerprint. Do not restore partial checkpoints over remaining evidence.

## Evidence

Execute feasible tests, distinguish native tests, independent reference tests,
source-contract checks, live lifecycle and actual skill benchmarks. No one level
substitutes for another. Keep failed attempts with explanations. Source ZIPs exclude
secrets, .env, worlds, weights, binaries, toolchains and caches. Private runtime repair
bundles may include byte-identified binaries and whitelisted supplied server dependency
caches, with clear provenance and notices. Never include fabricated receipts.
Update Japanese instructions, VERSION/CHANGELOG, validation status and actual test records.
Never call a first pass human likeness, survival, cooperation or a capacity guarantee.

Public runtime entrypoints are start/status/console/stop shell scripts. Port25565
and prefixbcmc are consistent defaults in .env, Rust and Java. Direct native
run/probe/console paths are not operator entrypoints. Normal Folia entity retirement is not a learning success
or a fatal error; do not mutate world/entity state from retired callbacks.
