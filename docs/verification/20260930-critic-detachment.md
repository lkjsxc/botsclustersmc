# Task-12 critic-feature detachment: prospective gradient-ablation diagnosis

Date: 2026-09-30 (Japan). Fixed before any new gameplay. This is an isolated,
finite diagnosis, not a deployable architecture or a claim that detachment fixes
mining. The previous mining-control experiment and its early rejection remain
unchanged. Its stopped failing weights are not reused as an initial model.

## Question and exactly one varied mechanism

The stronger mining control cost lost aim, workbench and pickaxe behavior after
262,144 accepted samples while the matched unchanged-runtime control retained the
declared floors. Both early runs had only task-12 samples and no review ticks.
This does not identify the responsible gradients. Here **both arms receive the
same mining control reward** from research source
`234e0cf5e15b31343f4f1f4b9916ae85c0df180e`. Control uses its unchanged backward
pass. Candidate removes only the current task-12 critic-loss contribution to the
shared hidden layers and goal-input weights.

Candidate backpropagates the unchanged actor derivative through the full shared
network, then accumulates the unchanged value derivative into its existing final
linear value row and bias using the current second-layer activations. Every other
task and unlabelled observation uses the original backward pass exactly. Actor
features and the final value readout still learn; this is not a frozen policy.

Initial actor probabilities, value predictions, model format, entire checkpoint,
Adam moments and course are identical. No forward pass, routing, reset, physical
control, mask, target, terminal predicate, reward, V-trace target, task weight,
exploration coefficient, learning rate or optimizer initialization differs between
arms. Both inference JARs are byte-identical to accepted main. This specifically
does **not** create an independent critic trunk: the critic still reads features
that actor learning can change. Inherited Adam moments still act after the new
critic contribution is removed, and the altered value trajectory can subsequently
change advantages and actor learning. These limitations prevent an exclusive
causal attribution even if the intervention preserves measured behavior.

## Exact source and frozen input

The same complete parent from the current-policy qualification is used in both
new owned Academies, not a newly selected live checkpoint:

| Identity | Value |
| --- | --- |
| Parent updates / samples | 1,182,364 / 344,488,915 |
| Canonical checkpoint SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Parent policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control source | `234e0cf5e15b31343f4f1f4b9916ae85c0df180e` |
| Control and frozen evaluation JAR | `782fb3bcde791745672b5d1433fa3f3efb190737dc594578bbc796079e8233c3` |
| Candidate training JAR | `fef1ef4d73ed320392ab30322afe2233a765e3c4e74e7bb48fc458d3362fd3cd` |
| Both inference JARs | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The new branch changes only `Gradient.class` and adds `CriticFeatures.class` in
its training JAR relative to the control JAR. Native canonical export must match
the prescribed initial policy in each arm before training. The current main
training service and its live model remain untouched.

## Predeclared finite protocol

Evaluate the common parent on fresh seeds **2026093091 and 2026093092**, ordinary
ordered tasks **0-12**, **32 cases per task**, `reset_intervention=none`.
Qualification requires tasks 0-10 at least 28/32 and task 11 at least 26/32 on each
seed. Mining is measured but does not gate the input. Complete both baseline
reports before starting a learner; unqualified input means no training.

Both arms then receive **one +250,000 accepted-sample segment**, at most +50,000
stop/flush overshoot. Each starts from the same full canonical parent with 512
actors and learning seed 7, 2 GiB heap, two visible processors and region threads,
one inference thread and one learner thread, loopback ports 31381/31382. The
original finite runner's 30-minute segment cap, 2 GiB memory floor, source/artifact
pins, controlled shutdown and complete native-verifiable stopped checkpoints
remain in force. No final continuation exists in this diagnosis.

After **both** arms stop, complete all four ordinary frozen reports using the
unchanged control runtime and the same task order/cases/seeds. At most four
loopback evaluators run concurrently. Every result and failure is retained;
all evaluations contribute zero training samples. Maximum complete evidence is
**six reports / 2,496 frozen trials**.

Preservation floors are separately checked for every arm and seed:

- tasks 0-10: `max(28, parent - 3)` /32;
- task 11: `max(26, parent - 4)` /32;
- task 12: `max(0, parent - 2)` /32.

The prespecified diagnostic support criterion is candidate retention of all floors
on both seeds **and** control losses of at least 8/32 on each of tasks 2, 10 and
11, separately on both seeds. This distinguishes failure to reproduce the old
regression from successful prevention of a reproduced regression. Scores and
partial changes are still reported if the conjunction fails. Retention alone is
not mining acquisition. Acquisition is separately described only if candidate
mining reaches at least 8/32 and exceeds control by at least 4/32 on both seeds.
No outcome automatically authorizes deployment, continuation or changed thresholds.
These are engineering screens, not confidence intervals or independent training
replications. A null result cannot prove that critic gradients are irrelevant.

## Engineering qualification

Independent Java tests compare every parameter's derivative against the ordinary
backward pass for all other tasks, and against an actor-only plus final-value-row
oracle for task 12. A finite-difference local surrogate holds critic input features
constant while allowing actor derivatives. Tests verify nonzero actor-feature and
critic-readout updates, cumulative gradients, unchanged observations/derivative
inputs/features/logits, preserved policy bytes and inference-JAR separation.

Controller regressions cover complete matrices, each named reproduction condition,
per-task/seed retention floors, acquisition separation, strict types and budgets,
create-only evidence, no training after failed input qualification and no implicit
CLI start. Existing runner lifecycle/boundary tests continue to run separately.
A complete local suite and source CI must pass before gameplay. Run explicitly:

```sh
EULA=true python3 tests/studies/critic_detachment.py --run
```

The finite wrapper reuses the existing tested mining runner and explicitly binds
both its execution and validation modules to the same source/runtime/seed/budget
configuration. The wrapper has its own create-only evidence root under
`.build/critic-detachment-study/`. It does not rewrite earlier evidence.

## Execution results

No Minecraft outcome is claimed at declaration. Results will be appended without
changing the prospective rules above.
