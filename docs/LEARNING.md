# Integrated learning contract — Academy v2

## What drives the real actors

The normal `app/agent.rs` executes a randomly initialized shared 1420→64→64 MLP,
with eight categorical heads: movement, yaw rate, pitch rate, jump/crouch,
interaction, hotbar, GUI operation and GUI slot. Two 704-value frames, previous
input and actor identity form the input. The bridge supplies state and goals,
never policy actions. There is no pretrained model, pathfinder, teacher action,
LLM, imitation data, task macro or fallback script in the normal executable.

The numeric observations expose local blocks, inventory/cursor, peer state,
authoritative position/orientation, task identity, goal displacement and angle,
remaining finite-horizon time, measured speed, difficulty and confirmed progress.
Player inventory and workbench menus both contain 46 slots; explicit GUI identity
avoids conflating them. Train and exam observations have identical semantics and
contain no exam flag. This is privileged state-based RL, not pixels-only learning.

## One canonical implementation, actually connected

`learning/src/next` owns the shared cohort, curriculum, distribution, numerical
and task contracts. `learning/src/control.rs` coordinates the actual actors.
`app/engine.rs`, `app/agent.rs` and `app/academy.rs` call those modules directly.
Standalone tests re-export the same source; there is no duplicate experimental
implementation that must be manually installed.

A coordinator lock owns the published immutable policy identity, course,
collection generation and actor RNG states. Every issued lesson is bound to run,
actor, generation, serial, task and policy. Actor fragments must have the exact
sequence and contiguous authoritative ticks for that lesson. No receiver channel
silently drops fragments. Capacity is bounded by per-actor quota plus the maximum
remaining episode. A disconnected/stalled actor cannot authorize a subset update.

The default quota is ceil(BATCH_SAMPLES/BOTS), where the default total is
BOTS×ROLLOUT_STEPS: 4096 samples at 64 actors and 64-step fragments. Reaching quota
is not enough: each actor completes its genuine current episode, seals, stops
input and waits before the next reset. The learner only takes a fully sealed
cohort. Each actor's fragments are concatenated in order; GAE never crosses actors.
Earlier finishing actors wait for the slowest. This trades collection consistency
against straggler latency; it is not guaranteed faster than every alternative.

## Probability, elapsed time and update transactions

GUI slot is conditional on left, right or shift click (GUI operation 1–3). For
other operations it is canonical zero and contributes no sampled log probability
or score gradient. Sampling, behavior validation, PPO likelihood, policy gradient,
normalized entropy and frozen inference use the same joint distribution. Entropy
includes the parent's derivative of slot activation probability. Masks retain
mechanical slot legality and task-wide availability, never the correct target,
recipe, tool or movement direction.

The Java bridge measures player age ticks on its owning entity scheduler. An
action transition records the difference between its surrounding authoritative
frames. With four ticks as one nominal decision interval:

```
gamma_dt  = 0.997 ** (elapsed_ticks / 4)
lambda_dt = 0.95  ** (elapsed_ticks / 4)
delta     = reward + gamma_dt * next_value - value
```

Genuine terminals kill bootstrap and GAE carry. Administrative truncation would
bootstrap a final real state but cut the carry; normal training never pretends a
reset is such a state. The task's visible finite horizon is a genuine terminal.
Potential shaping uses the same duration convention and zero terminal potential.
Time regularization scales with observed ticks; turn and input-change costs are
small engineered penalties, not a model of human motion.

PPO uses three epochs, 64-sample minibatches, clip 0.2, Adam base learning rate
0.0003, gradient norm clip 0.5, normalized entropy coefficient 0.002 and squared
critic loss coefficient 0.5. Huber is an available numerical helper, **not enabled
in the live value loss**. Advantages use stable f64 normalization.

After an update, sampled KL is measured on the entire old-policy batch. If it
exceeds 0.03, the exact original model, Adam moments/step and optimizer RNG are
restored and retried at half the learning rate, at most eight attempts. Invalid
numerics fail closed. Only an accepted update increments version once, writes a
complete checkpoint and releases the actors under the coordinator protocol.
This is a PPO engineering guard, not a reproduction of TRPO, RePPO, Dreamer or
DiscoRL. Unit tests do not prove faster real Minecraft learning.

## Adaptive tasks and trustworthy outcomes

All rooms have a 12×12 enclosed interior in a single 16×16 owned chunk, arranged
8×8 for 64 actors. Glass, illumination, reset resources, invulnerability and task
masks are deliberate environment assistance. Minecraft 1.21.11 vanilla input
semantics remain the actuator. The 18 task indices are:

| Index | Task | Furnished initial resources / required outcome |
| --- | --- | --- |
| 0 | Forward-stop | No items; reach and settle |
| 1 | Turn-stop | No items; rotate, reach and settle |
| 2 | Aim-hold | Visible target; hold yaw/pitch |
| 3 | Navigate-stop | Random planar goal; reach and settle |
| 4 | Step-over | One-block obstacle; cross and settle |
| 5 | Break-log | One target log; accepted removal |
| 6 | Collect-log | One log; break and acquire its session-tagged drop |
| 7 | Place-block | Raw planks; occupy a designated cell |
| 8 | Craft-planks | One log item; acquire four crafted planks |
| 9 | Craft-sticks | Two planks; acquire four crafted sticks |
| 10 | Craft-workbench | Four planks; acquire a crafted table |
| 11 | Craft-wood-pick | Furnished workbench, three planks, two sticks |
| 12 | Mine-cobblestone | Furnished wooden pickaxe; mine stone and acquire its drop |
| 13 | Craft-stone-pick | Furnished workbench, three cobblestone, two sticks |
| 14 | Smelt-iron | Furnished furnace, raw iron and coal; extract and acquire iron |
| 15 | Supply-chest | Furnished empty chest and logs; actor-attributed stock increase |
| 16 | Build-platform | Planks; all three designated cells must currently be occupied |
| 17 | Log-to-workbench | One log block, no items; break, collect, craft and acquire table |

Supplying a tool or station is not learning how to obtain it. The chest task is
individual logistics, not cooperation. Completing the final chain does not release
actors into an unimplemented settlement.

Success is based on authoritative context-tagged evidence. A block break needs
uncancelled vanilla destruction and next-tick AIR readback. Drop pickups retain
run/lesson/actor provenance; item merging is disabled in the training world so
old or foreign items cannot borrow a new receipt. Craft statistics must agree
with real acquired inventory/cursor output; crafting result previews are excluded.
Furnace output requires actual extraction attributed to the actor. Chest and
platform goals are read back as current contents, not historical event totals.
Stale tokens, repeated snapshots, reversed counters and impossible stock fail.

Navigation success requires distance ≤0.65, speed ≤0.025 blocks/tick, angular
speed ≤0.15 degrees/tick and ground contact for 20 observed ticks. Aim requires
both angle errors ≤8 degrees with the same angular limit for 20 ticks. Gaps over
8 ticks reset hold. Tasks have explicit finite horizons from 600 to 3000 ticks;
leaving the cell fails. Failed episodes are experience, not mastery evidence.

Difficulty adapts per actor/task using success windows. Initial states get easier
or harder; full training probes use difficulty 1.0 with a separate seed domain
from frozen exams. Approximately every fifth training choice rehearses an older
skill, alternating mandatory round-robin coverage with a learning-progress/
forgetting-weighted draw. This is a custom inspired scheduler, not a paper's exact
reproduction. Some easy training resets partially fill raw recipe ingredients or
open a workstation/container; **full probes and exams never do this**. Supplies
are conserved during preparation and no desired crafted output is furnished.

A frozen exam starts only at a drained cohort boundary after every actor has at
least 40 current-task training episodes, eight full probes and current full-probe
success EMA ≥0.70. Every actor needs 14/16 current successes and 3/4 for every
previous task. Weak individual skills block promotion even if the mean is good.
Exam trajectories are not trained. A final historical course pass is retained
as history, not perpetual proof of mastery. Continued failures are reported.
`BCMC_MODE=eval` uses a frozen checkpoint without changing training state;
`BCMC_MODE=random` is an explicit diagnostic baseline, never a fallback.

## Persistence and restart

`academy-v2/state/training.bcmc` is one fsync/rename checkpoint containing weights,
Adam, optimizer RNG, each actor's sampling RNG and the adaptive curriculum. Policy
version/fingerprint bind its components. Corrupt, oversized or incompatible
checkpoints fail closed. Actor count is exact. v1 weights and split checkpoints
are deliberately not reused after the observation/action/context change.

In-flight lessons cannot be resumed as replayable Minecraft trajectories. Issued
serials remain persisted; a restart has a fresh run and generation. Partial exams
restart at zero with the same frozen policy, not cherry-picked completed scores.
Completed training statistics and random states are restored. World persistence
is still separate: take a complete backup only after orderly shutdown.

Status reports include buffered cohort transitions, actor-local untrained
transitions and unfinished actions. These are not silently included in later
updates. `dropped_rollouts=0` does **not** mean no in-flight work is abandoned at a
requested shutdown. The explicit counts must accompany lifecycle evidence.

## What tests establish

Native tests check math, gradients, state binding, 64-actor barriers and negative
cases. The actual bridge is compiled against libraries extracted from the pinned
Folia jar. Live smoke tests check real clients, actual PPO updates and restart.
Separate scripted fixture tests use production literal input translation and the
same server outcome gate, but never initialize/save a policy or supply training
data. Passing them establishes reachability, not learned competence.

Skill learning, retention and generalization require long-run held-out trials,
multiple seeds and controlled comparisons. See [validation](VALIDATION.md) for
exact tested source revisions, settings, successes and failures.
