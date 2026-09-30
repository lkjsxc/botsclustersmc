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

No outcome was claimed at declaration. The complete results below were appended
after execution without changing the source, budgets, seeds or interpretation
criteria.

### Qualified source and initial model

Implementation and prospective protocol source:
`ec3329dcff45d5a71701fcdad02693def06741c8`, tree
`83faecbc8abf88ddd778aac57f403910d89b0505`. The commit was authored as
`lkjsxc` and pushed before gameplay. CI run `36717584734` passed source checks
on Ubuntu and Windows and the observatory job. Dispatch-only live/Paper/retention/
Windows-live jobs were skipped. The new Java derivative test passed 1,463,158
checks, including finite differences and exact parameter support; the complete
local suite ended `CRITIC_SOURCE_TEST_EXIT 0`. Ten wrapper controller methods
passed in normal and optimized Python, as did the inherited twelve runner tests.
The explicit gameplay entrypoint reran its ten tests before constructing the study.

Both initial native exports matched the same prescribed canonical checkpoint and
policy. Every evaluator used the control JAR, including candidate evaluation;
all non-configuration evaluator JAR payloads were byte-identical. Each complete
seed-specific configuration was verified against the requested task list and seed.
The actual executed policy, runtime, metadata and report counter identities passed
an independent read-only closeout audit.

### Complete ordinary matrix

A is seed `2026093091`; B is `2026093092`. All scores below are out of 32, with
vectors ordered by ordinary tasks 0-12. There is no extra reset intervention.

```text
parent    A: [32,32,32,32,32,32,32,32,32,32,32,27,0]
parent    B: [32,32,32,32,32,32,32,31,32,32,32,31,0]
control   A: [32,32, 6,32,31,19,19,27,32,32,10, 0,0]
control   B: [32,32,11,31,29,21,24,29,32,32, 8, 0,0]
candidate A: [32,32,31,32,32,32,32,31,32,32,32,12,0]
candidate B: [32,32,32,32,32,32,32,32,32,32,32,11,0]
```

The common parent passed the new input screen on both seeds. Its task-11 scores
27/32 and 31/32 are not replaced with more favorable historical scores. The
preservation floors for that task are therefore 26/32 and 27/32 respectively.
All six reports and **2,496 frozen trial executions** completed. Every evaluation
contributed zero new training samples.

The new control is **the stronger mining-control reward without detachment**,
not unchanged production training. It reproduced losses of at least 8/32 on all
three prespecified tasks (2, 10 and 11) on both seeds. Candidate tasks 0-10 all
retained their floors, including aim hold 31/32 and 32/32 and workbench 32/32 on
both seeds. This partial preservation relative to the matched control is reported.
It does not repair candidate task-11 losses: wooden pickaxe fell to 12/32 and
11/32, below both floors. Mining remained 0/32 in both arms on both seeds.

The exact computed interpretation is:

```text
candidate_retained=false
control_retained=false
control_regression_reproduced=true
detachment_preserves_under_reproduced_loss=false
acquisition=false
deployment=false
```

This is a **completed single-budget diagnosis**, not an interrupted experiment
or a +1M acquisition study. The inherited runner calls its post-training directory
`evaluation-early`, but there is no later segment in this protocol. Neither arm
was extended beyond its declared boundary. A favorable subset of tasks is not
reported as passing the conjunction of all retention requirements.

### Exact learning budgets and retained state

| Arm | Additional accepted samples | Stopped total samples | Stopped updates | Segment wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 252,928 | 344,741,843 | 1,183,007 | 146.450 |
| Candidate | 253,408 | 344,742,323 | 1,183,011 | 146.551 |

Both counts satisfy the declared +250,000 through +300,000 range. They are not
identical, and similar wall times are not a throughput or deterministic-trajectory
claim. Native `verify-export` passed again on both stopped canonical states.

| Stopped artifact | SHA-256 |
| --- | --- |
| Control canonical checkpoint | `539269e3c24b53a5f3a1d82ba81094146e5c7e0711c26628bd9a5cbc7f7d59fe` |
| Control policy | `4d3605714c7105e64d42c6955c746c742677206c8f95a82f10b0ad19c709f612` |
| Candidate canonical checkpoint | `ea196e219c8fd0d1ddcf3156df6811dfb57832cd94cbcdbb367266c11066fb40` |
| Candidate policy | `bd8fc04cc2e1e5b770cffecd6e833b3c26020d1af41c40c3652fc3cf10cff9b9` |

Each arm retained 27 fresh process-local health observations. Initial coverage
observed all 512 actors with restored-checkpoint status; the final observations
had 512 active and progressing actors. Recorded inference failures/rejections,
stale/rejected learner samples and retirements were zero. Total counters, task
marginals and each context sample/tick cell were monotone and correctly bound to
the original 344,488,915-sample process origin.

In both final observations all accepted samples were task 12 and review ticks
were zero. Control counts were 48,932 closed plus 202,204 inventory = 251,136;
candidate 69,994 closed plus 181,238 inventory = 251,232. The stop/flush added
1,792 and 2,176 samples respectively. Those earlier menu counts must not be
combined with the later stopped totals as though sampled simultaneously.
Actor ticks in those menu bins were five times their sample counts. No inference
about completed-episode mastery follows from these exposure measurements.

### Mining behavior and bounded interpretation

Each mining report had 19,200 decision-boundary observations. The two post-training
arms spent similar amounts of time in closed-menu observations: control
9,469/9,496 and candidate 9,247/9,345. Nevertheless, effective world-dig selections
were control 1,516/1,507 and candidate **34/30**. Candidate held-pick observations
were 511/739, versus control 700/651. No arm recorded a target-pickaxe contact,
positive target-pickaxe mining ticks or a broken block. Generic item pickups are
not credited as cobblestone acquisition.

The critic-feature intervention substantially changed the observed retention
pattern relative to a reproduced failing control, but it did **not** preserve all
useful behavior or acquire mining. Do not deploy either arm. Do not call this a
proof that all forgetting is critic-driven, that actor learning is harmless, or
that an independent critic network will necessarily solve the problem. This
single intervention also changes later value estimates, advantages, optimization
and visited states; inherited Adam moments remain. Two evaluation seeds are not
independent training replicates. The missing early review exposure is common to
both arms and does not prove a permanent curriculum failure.

A next useful diagnosis can separate the remaining task-11 actor-feature drift
from actor-output drift using the preserved ordinary parent and stopped candidate.
Any parameter hybrids must remain explicitly counterfactual, with unchanged
physical controls and complete frozen retention/completion matrices. Such a
diagnosis still would not identify the responsible gradient terms. A subsequent
learning proposal must preserve the now-demonstrated earlier skills and ordinary
wooden-pickaxe crafting while producing real cobblestone pickup; neither stronger
costs nor this partial detachment result alone meets that acceptance condition.

### Evidence retention

Raw evidence stays in
`/home/coder/workspace/botsclustersmc-critic-detachment/.build/critic-detachment-study/`.
The read-only closeout verified every original result/metadata/receipt binding,
full interpretation, accepted-sample boundary, context history and native stopped
export. It added no new gameplay or trials and left source inputs unchanged.
The gameplay log ends `CRITIC_DIAGNOSIS_EXIT 0`; no experiment workers remain.

The unpublished research draft `critic-detachment-study-20260930` targets the
original source `ec3329d`. Its `critic-detachment-evidence-20260930.zip` has
66 members, 5,289,443 bytes, SHA-256
`b979a2ea0ca419e518a0655fa6949b0626488fa9e1010c011dadf7c08c45d368`.
ZIP integrity and each manifest entry's size/digest passed, and a fresh GitHub
download matched the local archive. The explicit allowlist preserves the common
parent, complete stopped model/Adam/course states, runtime and evaluator JARs,
all reports and histories, original prospective protocol and controller source.
It excludes Minecraft server binaries, worlds, cache, credentials and unrelated
data. This is not a published plugin or accepted learning checkpoint. PR #52
is closed unmerged; the source branch and both outcomes remain preserved.

### Mainline boundary and development health

The result-only research commit is
`ddcf2f6d24bfa0bbb3a14d3665be2e629eeb8602`; the earlier cost closeout is
`880e40e` on its own preserved branch. Mainline receives this record, the complete
cost result and the updated decision roadmap only. It does not receive
`CriticFeatures`, `MiningControl`, the experimental gradient path, controller,
models or learning configuration. Reproducing the research command requires the
recorded research source and its separately owned inputs, not an ordinary main
checkout.

During the diagnosis, the independent mainline health audit retained a fresh
snapshot at epoch 1790773173723: 400,539,820 cumulative samples, 1,368,917 updates,
512 active/progressing actors and course task 12. All recorded inference failures/
rejections, stale/rejected learner samples and retirements were zero. Both
training and monitor services were active, and the installed training JAR still
matched accepted SHA-256
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.
The supervised process remained 2350978 with context origin 344,488,915 samples.
No production restart or learning-state reset occurred. These are live health and
exposure checks, not a new frozen skill qualification of the moving live policy.
The raw status and identity are retained in the documentation worktree under
`.build/development-health-1790773173723/`.
