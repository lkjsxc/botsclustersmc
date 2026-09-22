# Integration and acceptance contract

This document describes **remaining work**, not changes already applied to the supplied v0.3.1 runtime. Do not label the component artifact v0.4.0 or overwrite working binaries until the following integration is implemented and tested.

## Existing application boundaries

The inspected distribution contains `learning/src/ppo.rs`, `learning/src/curriculum.rs`, `app/engine.rs`, `app/agent.rs`, `app/academy.rs`, `app/sensor.rs`, `app/act.rs` and a Java Folia bridge. Preserve literal vanilla client actions, no target-dependent action selection, no pathfinder, no crafting macro, and no default fallback from learned actions to a script.

1. **Collector wiring.** Replace timed/size-only publication while other actors still collect the old version with an explicit behaviour cohort. Give each actor a stable index and monotonically increasing fragment sequence. Freeze the behaviour model across fragments; seal only after quota AND a genuine episode terminal. Stop before requesting the next lesson. Bound the channel and per-actor episode capacity; queue overflow/disconnection is an observable failure, not silent data loss. Publish a committed model and round under one coordination protocol. A stalled actor must not authorize a partial-cohort update. Log all deliberate cancellation/discard counts.
2. **Transition semantics.** Record actual elapsed server ticks from the two authoritative observations surrounding the action. Store the final observation and separate `Continuing`, `Truncated` and `Terminated`. GAE, potential shaping and stated episode deadlines must share the same duration convention. Concatenate fragments per actor, preserving episode boundaries; never compute a trace across actors. Include remaining task time in the observation for finite-horizon deadlines.
3. **Conditional policy.** Preserve the base mechanical slot mask. Use the same conditional GUI distribution in sampling, behaviour log-probability validation, updated-policy likelihood, policy gradient, entropy and exam inference. Merely replacing the ignored slot with zero while leaving old log-probabilities unchanged is incorrect. Merely dropping a sampled child entropy while ignoring its parent's activation derivative is also incorrect.
4. **Optimizer transaction.** Snapshot model, Adam moments/step count and all update RNG streams. Evaluate final sampled KL on the whole old-policy batch. No publish/checkpoint/phase side effects inside retried update closures. Report accepted final KL, attempts, clipping, gradient norm, explained variance, actual environment samples and time spent waiting/collecting/training. Huber loss, normalized entropy and changed minibatches need ablations; they are not proven faster by unit tests.
5. **Curriculum.** Bind issued lessons and recorded outcomes to run, actor, generation, serial and policy. Seed full-difficulty training probes separately from full-difficulty frozen exams. Only enter exams at a drained boundary. Every actor must pass the current and all prior skills. Preserve adaptive statistics with the model; do not import old weights into changed observation/head schemas by pretending compatibility.

## Folia task fixtures and trustworthy outcomes

For every additional task, implement region-owned geometry changes and entity-owned player/inventory changes. Keep every room in its intended region/chunk; use region scheduler callbacks, player entity schedulers and `teleportAsync`. File I/O belongs off the region tick. Confirm a reset is ready only after actual teleport/inventory/geometry completion.

- Give each item drop its originating run/actor/lesson token. Reject or remove old-session and cross-room items; do not let a previous episode donate free progress.
- Count actual target block removal after readback, not a predicted/cancelled block-break callback. Mining tasks must require valid tools where vanilla rules do.
- Count real inventory/cursor acquisition, excluding crafting result previews. Raw materials or reset aids are not task completion. A craft statistic must agree with acquired output.
- For placement, verify the intended cells currently contain the intended blocks. Historical placement counts cannot pass after the blocks were removed. Reset all bounded stray placements too.
- For smelting, require real actor-attributed extraction and acquired iron. Furnishing a furnace is not learning to craft one; label that limitation.
- For supply, verify the correct container's real contents and the actor's actual transfer. This individual chest task is not multi-agent cooperation.
- Training-only partially filled recipe grids/open menus are **initial states**, not actions. Full probes and exams must use empty grids and no menu-opening assistance. Do not silently auto-fill a recipe after the episode starts.
- Ensure each task has reachable goal geometry, supplies, a sufficient vanilla time budget, a bounded reset, valid input actions, and a verifiable completion condition before exposing it in the normal curriculum.

## Compatibility and operation

Retain `SERVER_PORT=25565`, `BOTS=32`, `BOT_PREFIX=bcmc`, `BIND_ADDRESS=0.0.0.0`, explicit offline-access acknowledgment and explicit Minecraft EULA acceptance. Keep the fixed Java/Minecraft/Azalea protocol combination until a migration is actually verified. Preserve the original worlds and checkpoints. A new observation/bridge schema requires a new owned state directory and a clear failure for incompatible state, not a silent reset or unsafe reuse.

Never bundle operator `.env`, worlds, credentials, private checkpoints, toolchains or dependency caches into the source artifact. Do not ship an old binary under a new source receipt. A native artifact must be built from the shipped source and tested through its public entrypoints.

## Acceptance evidence still required

- Compile/test the complete Rust application, launcher and Java bridge against their exact pinned dependencies.
- Start the public entrypoint with 32 actual clients against Folia, observe finite transitions, at least one real PPO update, strict version/cohort accounting, and no bridge/reset/scheduler errors.
- Graceful stop and exact model/optimizer/RNG resume; fresh lesson tokens; a partial frozen exam restarts consistently.
- Exercise every new fixture and prove success through real vanilla actions in a separate, explicitly diagnostic test path. Scripted diagnostic actions must never enter learner checkpoints or be presented as learned policy behaviour.
- Verify failure/timeout/reset cases, stale frames, old drops, cancelled actions, inventory previews, wrong placements, server restart and incomplete cohorts.
- Compare old/new learning on the same task distribution and multiple seeds. Report elapsed time, real environment samples, discarded transitions, per-agent challenge success and retention. A synthetic bandit is not a substitute.
- Distinguish **implemented**, **reachable**, **learned**, **retained**, and **generalized**. A larger stage enum proves none of the latter four.
