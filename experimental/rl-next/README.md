# botsclustersmc — RL-next components

**Status: experimental Rust components, not an upgrade to the v0.3.1 Minecraft executable.**

The source contains a bounded on-policy collector, elapsed-tick GAE, a conditional categorical policy, guarded PPO update helpers, an adaptive curriculum, policy-bound curriculum checkpointing, and 18 task contracts/evidence validators. It does not yet include their integration into the supplied Azalea/Folia application. Running this package does not make the old bots learn differently. The advanced stage names are contracts, not evidence of playable or mastered Minecraft skills.

## Build and verify

No crate dependencies, Python, GPU, demonstrations, LLM or paid API are required by this package.

```sh
cargo test --release --all-targets
cargo run --release -- benchmark
cargo run --release -- stages
```

`benchmark` trains a categorical policy from sampled rewards in an eight-context/four-action synthetic bandit for five fixed seeds. Its output explicitly says `minecraft_evidence: false`. This is an optimizer correctness check, not a Minecraft benchmark, a comparison against v0.3.1, or a claim that learning is faster.

## Mechanisms

- **Cohorts:** collect from all configured actors under one behaviour policy. Actors stop only between completed episodes, after reaching their quota. Validate sequence, actor, version, generation, capacity and exclusion of exams before accepting data. Do not replace stale on-policy data rejection with uncorrected off-policy replay. Synchronous collection can increase straggler waits; measure effective samples/second and discarded-sample counts in Folia.
- **Time:** use the same explicit elapsed-server-tick discount convention in GAE and potential shaping. A true task terminal has no bootstrap; an administrative truncation bootstraps the final observation but cuts the GAE trace. Never bootstrap from the next reset observation.
- **Conditional actions:** GUI slot affects the executed action only when the chosen GUI operation uses a slot. The joint likelihood and entropy reflect that condition. The entropy gradient includes the activation-probability derivative in the parent head. Normalized per-head entropy prevents a 90-way slot head from mechanically dominating a two-way motor head; its behavioural effect still needs ablation.
- **Update guard:** measure sampled KL over the entire behaviour batch after updating. Retry from a snapshot at a lower learning rate when the update is too large. A snapshot must include model, optimizer and all mutated RNG streams. Errors restore the original state. This engineering guard is **not** the RePPO algorithm.
- **Practice:** 20% prior-skill reviews, half mandatory round-robin and half weighted by learning progress, forgetting and validation weakness. One in five frontier exercises is a full-difficulty probe. Difficulty changes in bounded steps using recent success; exam readiness requires fresh frontier experience and successful full-difficulty probes.
- **Exams:** one frozen policy, full difficulty, separate seed domain, 16 current-skill trials and four trials of every previous skill for every actor. Each actor needs at least 14/16 and 3/4 respectively. One failing actor or forgotten skill blocks promotion. No time-based promotion. Completion is historical; later failed retention checks remain visible. These are curriculum validation exams, not an untouched final research test set.
- **Persistence:** curriculum checkpoints bind to the actual restored policy version/signature. In-flight tasks are not replayed after restart. A partial exam restarts from zero under the same frozen policy with fresh task tokens, not from a cherry-picked subset of successes. The lightweight corruption check is not cryptographic authentication.

## Research basis and limits

Research was investigated on 2026-09-22. This is a selective, problem-driven review, not an assertion that every latest RL paper was covered.

1. Sullivan et al., **Syllabus: Portable Curricula for Reinforcement Learning Agents**, arXiv:2411.11318v2, 2025-08-02. https://arxiv.org/html/2411.11318v2 . The paper presents portable automatic curricula and learning-progress/learnability approaches, and cautions that transfer to complex environments is not automatic. The scheduler here is a custom inspired implementation, not their code or an exact reproduction.
2. Nishimori et al., **Emergence of Exploration in Policy Gradient Reinforcement Learning via Retrying**, arXiv:2606.00151, 2026. https://arxiv.org/abs/2606.00151 . RePPO's retrying objective is a distinct exploration method. It has not been implemented here. Retrying a numerically oversized optimizer update is unrelated to that objective; the similar word must not be used to imply a reproduction.
3. Hafner et al., **Mastering diverse control tasks through world models**, Nature, 2025. https://www.nature.com/articles/s41586-025-08744-2 . DreamerV3 is relevant to long-horizon Minecraft learning but a world-model learner is not a drop-in change to the current small CPU PPO stack. This package is not DreamerV3.
4. Hafner, Yan and Lillicrap, **Training Agents Inside of Scalable World Models**, arXiv:2509.24527, 2025. https://arxiv.org/html/2509.24527v1 . Dreamer4's offline world-model setting and data requirements should not be conflated with reward-only online bots on the user's server. It is not implemented here.
5. Oh et al., **Discovering state-of-the-art reinforcement learning algorithms**, Nature, 2025. https://www.nature.com/articles/s41586-025-09761-x . DiscoRL is algorithm discovery at a very different scale. No DiscoRL reproduction or universal optimality is claimed.

The oldest foundational ideas are deliberately not described as newly invented in 2026. A useful change must survive unit tests, end-to-end integration, multiple learning seeds and a controlled baseline comparison—not merely resemble a recent paper.

## Advanced task contracts

Indices 0–5 retain the existing motor/aim/navigation/step/break-log progression. Indices 6–17 add collect-log, place-block, craft-planks, craft-sticks, craft-workbench, craft-wooden-pickaxe, mine-cobblestone, craft-stone-pickaxe, smelt-iron, supply-chest, a three-block platform and log-to-workbench.

Fixtures may furnish raw materials, a workbench, a furnace or a tool as explicitly listed by `bcmc-rl-check stages`. These are isolated exercises, not claims of unassisted survival. Supply-chest is an individual inventory-transfer exercise, not learned multi-agent cooperation. Later combined exercises do not automatically imply a civilization or open-world policy.

Training-only reverse-curriculum reset states may eventually use partial recipe grids or an already-open menu, but only at reset, with no automatic action selection during an episode. Full-difficulty probes/exams must start without those aids. A real bridge must verify ownership, actual block state, actual acquired output, and session-scoped drops. See `INTEGRATION.md` before deployment.

## Known missing work

The existing app/agent, app/engine, observations, bridge protocol, fixtures, launchers and binaries have not been switched to these components. No live 32-bot Folia test, advanced-skill reachability test, long learning run, throughput ablation, or v0.3.1 checkpoint migration has been established by this component package. Do not overwrite the working installation or claim a new Minecraft runtime release from this artifact.
