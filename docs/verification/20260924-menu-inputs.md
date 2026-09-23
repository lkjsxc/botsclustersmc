# Mechanical menu inputs, evaluated policy artifacts and live delivery

## Exact source and concurrent mainline integration

PR #5 implementation: `3ef82e78268d8d4c2d105d05c5f2be3f34d40d81`.
Mainline merge: `6a05579d1aa318808de247533e955fd04cff65b4`.
Tested tree: `e9a6c8c54a6e7c05d6c4661b08d7bc2cb1b96084`.

During this work, PR #4 independently added the least-served, elapsed-tick review
allocator to main. The final integration preserves that `ReviewEffort`
implementation, tests and documentation. The parallel experimental `ReviewBudget`
implementation was removed, not retained as an alternate generation. Its measured
results below are explicitly historical and do not certify the final allocator.
The developer worktree at local commit `700020f` and the published integration
had identical complete Git trees. Both deployed JARs were also compared
byte-for-byte with the tested integrated build and matched.

The new production changes are the goal-independent mechanical menu filter and
one-shot evaluated-policy export. See [Menu inputs](../MENU_INPUTS.md),
[Review effort](../REVIEW_EFFORT.md), and [Evaluation](../EVALUATION.md).
The filter does not select the correct recipe, ingredient or destination. The
slot mask is a union across click operations, so some operation/slot combinations
can still have no effect. No reward, full-difficulty success predicate or frozen
exam threshold was relaxed.

## Software and lifecycle acceptance

The full integrated source suite passed locally and on both Ubuntu and Windows
in GitHub Actions run `35924187275`. This includes 1,462,522 menu assertion
executions, 4,149,761 mainline review-effort assertions, 19 evaluated-artifact
integrity checks, and the existing numerical, persistence, course, concurrency,
canonical export and evaluation validation tests. Assertion counts are not counts
of independent Minecraft trials. CI here establishes source/JDK acceptance on
the two operating systems, not new real-Minecraft Windows acceptance.

All 18 real-Folia scripted reachability fixtures passed, including actual sensor
checks for the new menu filter, with zero learner and inference samples. These
scripted inputs are not neural skill evidence and are excluded from public JARs.

An experimental retained ZIP and the final live retained ZIP each separately
passed actual inference-only deployment acceptance with 64 NPCs. Only the included
inference JAR and policy were installed. Pause/resume, rapid goal replacement,
chunk ticket release, respawn and corrupt-model refusal were checked. The host
server remained running when the deliberately corrupted test model was refused.
These tests touched disposable servers, not the live Academy's checkpoint.

## Fixed-weight mechanical comparison

The starting canonical snapshot was policy 234045, with 93,937,480 accepted
training samples. Real Folia 1.21.11 build 14 evaluated 32 full-difficulty cases
per task with seed 924603, stochastic actions and no learning during evaluation.
The policy bytes were identical before and after the mechanical mask change.

| Task | Previous mask | Mechanical mask |
| --- | ---: | ---: |
| Forward-stop | 32/32 | 32/32 |
| Turn-stop | 32/32 | 32/32 |
| Aim-hold | 23/32 | 23/32 |
| Navigate-stop | 32/32 | 32/32 |
| Step-over | 32/32 | 32/32 |
| Break-log | 32/32 | 32/32 |
| Collect-log | 32/32 | 32/32 |
| Place-block | 32/32 | 32/32 |
| Craft-planks | 31/32 | 32/32 |
| Craft-sticks | 28/32 | 32/32 |
| Craft-workbench | 0/32 | 1/32 |

This is a small mechanics experiment, not new learning or a statistical-
significance claim. It does not establish reliable workbench creation.

## Three independent additional-learning runs

All three started from the same policy 234045 canonical checkpoint and Adam
state. Each used 1,024 actual NPCs, a 3 GiB heap, and region/inference/learner thread
counts 2/1/1 on the same Linux allocation with 16 effective CPUs and 12 GiB memory.
The original Academy continued independently. Runs targeted two million added
accepted samples, then stopped normally; whole batches and status polling account
for the bounded overshoot. Each final learner queue was empty and its canonical
checkpoint was saved. None of these experimental weights replaced the live
Academy's existing learning history.

| Candidate | Final policy | Final accepted samples | Added samples |
| --- | ---: | ---: | ---: |
| Mechanical mask, old episode-probability review | 238105 | 95,955,756 | 2,018,276 |
| Mechanical mask, superseded random-prior-task tick budget | 238119 | 95,967,154 | 2,029,674 |
| Mechanical mask, final mainline least-served ReviewEffort | 238112 | 95,963,333 | 2,025,853 |

Matched-case evaluations used seed 924603, 32 cases per task and fixed weights:

| Task | Old episode review | Historical random tick review | Final least-served review |
| --- | ---: | ---: | ---: |
| Forward-stop | 32/32 | 32/32 | 32/32 |
| Turn-stop | 31/32 | 32/32 | 31/32 |
| Aim-hold | 24/32 | 28/32 | 27/32 |
| Navigate-stop | 32/32 | 32/32 | 32/32 |
| Step-over | 32/32 | 32/32 | 32/32 |
| Break-log | 32/32 | 32/32 | 32/32 |
| Collect-log | 32/32 | 32/32 | 32/32 |
| Place-block | 32/32 | 31/32 | 31/32 |
| Craft-planks | 32/32 | 32/32 | 32/32 |
| Craft-sticks | 32/32 | 32/32 | 32/32 |
| Craft-workbench | 5/32 | 4/32 | 2/32 |

The final allocator did not dominate every measured skill. Scheduling and probe
cadence differ, sample counts are close but not identical, and real asynchronous
servers are not bitwise deterministic simulators. These results are not a pure
single-variable causal ablation, a statistically established ranking or proof
of lifelong retention. The final source uses the existing mainline allocator
rather than maintaining competing implementations or choosing a headline score.

## Fresh-case evaluation of the integrated candidate

Policy 238112 was separately tested on 64 cases per task with new seed 924641.
Its weights were byte-identical to the preceding 32-case integrated evaluation.
Forward-stop, turn-stop, navigation, step-over, log breaking, log collection,
plank crafting and stick crafting each scored 64/64. Aim-hold scored 57/64,
placement 62/64, and workbench crafting only 1/64. All 704 trials were retained.
This weak workbench result is not hidden by the later live-model result.

## Preserve and update the original Academy

The running Academy was stopped through its normal console command and the
supervisor's exit was checked. Its stopped canonical checkpoint, status, original
training JAR, last evaluation and source identity were saved in the original
checkout under `.cache/upgrades/menu-inputs.SiSH0u/`. The world was not deleted
or replaced with an experimental world. The source fast-forwarded normally to
mainline merge 6a05579 and rebuilt.

Startup restored the original model and Adam state at policy 255270,
104,184,605 accepted samples, optimizer step 255270. Later status showed policy
262254, 107,506,690 samples, all 1,024 actors active, no pending actors, and zero
inference failures, inference rejections or burning actors. The allocator reported
`observed-ticks`, with 13,473,032 frontier ticks and 3,204,032 review ticks in that
process. Whole episodes and initialization mean the instantaneous share need not
be exactly 20%. The private dashboard responded successfully.

The native evaluation watcher was restarted from the updated source with
`--watch --interval 600`. A later heartbeat showed it in the normal waiting
state after a completed test. This is a server-side service, not a promise of
ongoing manual supervision. Minecraft port 25565 and the existing private
monitor binding at `100.94.221.97:8765` were preserved.

## Retained live policy: exact deployable artifact

While the original Academy continued learning, a one-shot evaluation froze its
policy 258790, with 105,844,631 accepted samples. It used 64 full-difficulty cases
per task, stochastic actions and seed -6086744076461530990. No learning occurred
inside the evaluator. The result was:

| Task | Successes / cases |
| --- | ---: |
| Forward-stop | 64/64 |
| Turn-stop | 61/64 |
| Aim-hold | 62/64 |
| Navigate-stop | 64/64 |
| Step-over | 64/64 |
| Break-log | 64/64 |
| Collect-log | 64/64 |
| Place-block | 62/64 |
| Craft-planks | 64/64 |
| Craft-sticks | 64/64 |
| Craft-workbench | 45/64 |

This model had substantially more accumulated training than the isolated
candidates. Its result is not a controlled improvement ratio over their scores,
and it does not certify the live weights updated after this snapshot.

The native `--export` command retained the actual tested weights, the inference
JAR from the same immutable build, the summary, all 704 trials and deployment
notes in `/home/coder/workspace/botsclustersmc/dist/evaluated.zip` on the operator
device. Its size was 397,689 bytes. The five expected entries, ZIP integrity,
exact policy/JAR identities and summary agreement with every trial were checked.
The subsequently extracted pair passed the separate real 64-NPC inference-only
lifecycle test described above. The ZIP is not an optimizer/world backup and is
not automatically installed into another world. Existing output paths are never
overwritten. See the evaluation guide for filesystem and deployment constraints.

## Evidence locations and remaining limits

Seven independent neural evaluations contain 3,168 trials in total, including
all failures. In the development worktree, the root is
`.acceptance/affordances-20260924060429/`. Reports are in `baseline`, `focused`,
`learning`, `balanced-learning`, `integrated-learning` and `integrated-holdout`,
each under `server/plugins/BotsClustersMC/`. The live export's unchanged reports
are also in `live-retained-deploy/` and inside the ZIP. `verified-summary.json`
was generated by checking all trial identities, seeds, denominators and successes,
plus the identity and contents of all five retained ZIPs.

`integrated-source-tests.log`, `fixtures.log`, `bundle-inference.log` and
`live-bundle-inference.log` retain software acceptance. The three learning-result
and learning-snapshots files retain actual run counters and clean shutdowns.
All experimental training processes were stopped; the original Academy's learner,
monitor and native evaluator remain the operational services.

There is still no demonstrated autonomous survival settlement, learned choice of
high-level goals, persistent individual life, hunger/durability model or multi-
agent cooperation. These are privileged-state, bounded NPC tasks with supplied
resources and goals. The bodies are server-side Villagers, not logged-in players.
Even the retained live model failed 19 of 64 workbench cases and some motor cases.
Combining independent task successes into a complete log-to-workbench life cycle
or cooperative open-world behavior remains a separate, unproven claim.
