# botsclustersmc engineering contract

The product is a mostly-Rust, pure-RL Minecraft/Folia experiment for a 16 CPU,
12 GiB RAM, 120 GB Linux allocation. Communicate with the operator in Japanese;
use English for source and technical records. Execute authorized development,
not merely plans. Preserve operator worlds, private configuration and evidence.

Read README.ja.md, docs/LEARNING.md and docs/VALIDATION.md before changes.
The real runtime uses `learning/src/next` through `control`, `bundle`, PPO and
`app/`. `experimental/rl-next` is a standalone re-export, not another implementation.
The live task environments number 18; implementation is not proof of mastery.

## Learning integrity

The neural policy chooses every normal gameplay input. No pathfinder, auto-aim,
smart tool choice, recipe/build/harvest macro, demonstration, imitation/LLM,
fabricated reward or scripted failure fallback. Task-wide masks may limit input
availability; state-dependent mechanical masks may reject nonexistent slots.
Neither may reveal a correct action. Privileged state/goal information, rewards,
actuator constraints and reset assistance must be disclosed.

`tests/live-client.rs` is a separately compiled, explicitly scripted diagnostic.
It is never linked into the normal actor executable, initializes no neural policy,
and must not write training checkpoints or demonstrations. Its success establishes
reachability, not learning. Never train on its inputs or results.

Keep behavior policy and lesson identity immutable during each cohort. All actors
must reach quota AND a real terminal before release. A missing actor, bad sequence,
stale context or queue/capacity error is not permission for a partial update.
Use the same conditional GUI distribution in sampling, likelihood, gradient,
entropy and frozen exams. Tick-aware GAE and shaping share the duration convention.
Model, Adam and optimizer RNG belong to the same retried KL transaction.
No model publication, phase change or checkpoint side effect inside retry closures.

Rust owns policy, rewards, curriculum, frozen evaluation and PPO. Java host tools
own lifecycle only; the Java bridge owns bounded environment resets and telemetry.
Use owning entity schedulers for players/items, region schedulers for blocks and
teleportAsync. No unsupported cross-region mutation, synchronous world loading,
or Bukkit stubs as compatibility evidence. File I/O stays off tick callbacks
except bounded fatal emergency reporting, which must never choose gameplay.

Frozen exams never train. Every actor must pass current and EACH previous skill;
average scores cannot bypass individual failures. No elapsed-time promotion or
release into an unimplemented settlement. Full probes/exams have empty recipe
grids and no menu-opening assistance. Training-only initial assistance conserves
raw ingredients. Target removal, crafted acquisition, drops, extraction, deposits
and current placements require authoritative session-scoped evidence.

## Delivery and state

Keep the tested pins: Minecraft/Folia 1.21.11 build14, protocol774, Java21,
Azalea f8ddefa70cc53e6385785fb56e7a688a389cf0ab and nightly-2026-02-04.
A source receipt is not a build test. Compile and test the actual native app
before publishing a native build; do not relabel an old executable as new.
Compile the Java bridge against the exact server's extracted API libraries.

The public entrypoints remain start/status/console/stop shell scripts. The owned
v2 runtime lives in `academy-v2/`; old `academy/` data is never auto-migrated or
removed. BOTS defaults to64, port25565 and prefixbcmc are consistent across layers.
Human view defaults to12 chunks, actor view to3 and simulation to3. Observation
controls must remain read-only with respect to actors/rewards/course progress.

Retain explicit EULA consent, offline-network warnings, port checks, run/build
exclusion, bounded queues/logs and disk guard. Reject unowned or symlinked training
components. A changed observation/action meaning requires a new context/schema
or a tested explicit migration. Never silently reuse same-sized weights.
The atomic `training.bcmc` bundle binds policy, optimizer, RNG and course. It is
NOT an atomic world+model transaction. Restore a complete stopped backup on
failure, never implicitly initialize over damaged state. Report unfinished work.

## Evidence

Distinguish source checks, native tests, actual API compilation, live lifecycle,
scripted reachability, learned skill, retention and generalization. None substitutes
for the next. Preserve failed attempts and explain repairs. Source artifacts and
commits exclude .env, credentials, operator logs/worlds/weights, binaries,
toolchains and dependency caches. CI evidence may contain whitelisted logs/status
from disposable tests, never operator data. Update Japanese instructions,
VERSION/CHANGELOG and dated verification records. Do not claim learning speed,
capacity, human likeness or indefinite uptime without corresponding measurements.
