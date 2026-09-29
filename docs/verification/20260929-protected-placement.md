# Protected storage-to-placement comparison - 2026-09-29

## Prospective protocol

This is a new experiment, not a revision of PR #33's failed early-retention
screen or PR #36's failed acquisition result. The earlier placement mixture was
never tested through its final acquisition phase because both unprotected
continuations lost workbench skill. The later protected continuation preserved
old behavior but did not acquire ordinary pickaxe crafting. We now hold the
tested protection boundary constant in BOTH arms and change reset exposure only.

### Fixed input and intervention

Use the preserved source policy 332305 / 92,574,669 accepted samples, original
checkpoint `495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855`.
Both arms start from the exact same previously constructed
`protected-training.bcmc` in the retained protected-continuation worktree.
Re-audit active parameters, Adam moments/clock, course/RNG state and initial
forward functions against the original trusted runtime before evaluating.
Do not select a more favorable checkpoint or resume either earlier study's final
model. The input is knowingly imperfect; this is not an absolute mastery exam.

Both arms reuse the unchanged experimental protected implementation from
`e1f8e6567116c02e0af18e64a466f7e0289e5a14`: task 11 learns on a warm copy;
all other observable goals use the same immutable source network. Actor and
critic gradients cannot change the source. Only task-11 samples are accepted.
This is fixed-frontier research code, not a general deployable learning system.

Control uses the unchanged protected-continuation runtime. Candidate changes
only InitialCrafting.prepare, using the reset intervention first studied in
`57cbcfa3dfdbc48c408110419b1f0513859d07cf`: at nonzero, sub-full pickaxe
practice difficulty, half of starts leave exactly one uniformly selected recipe
cell empty, its remaining raw ingredient in the original storage slot, and an
empty cursor. The other half retains the original mixture. Difficulty zero,
ordinary full resets and all other tasks remain unchanged. No completed item
or subsequent action is supplied. This is declared reset assistance, not
unassisted training or teacher-action imitation.

Before execution, compare runtime archive entries: only
`org/botsclustersmc/training/InitialCrafting.class` may differ between arms,
and inference JAR bytes must match. Rewards, observations, action meanings,
legal masks, model arithmetic, optimizer, task allocation and horizons stay fixed.
Ordinary and assisted evaluations use the SAME unchanged control runtime plus
current mainline holdout validation. No experimental runtime goes into the live
Academy.

### Fixed evaluation and sample plan

Publish implementation, runner, tests and this protocol before any new game
evaluation or training. Fresh evaluation seeds are **2026092981** and
**2026092982**. These are two evaluation conditions of one training continuation,
not two independent training replications.

At baseline, evaluate the original source and protected initial model on ordered
tasks **0 through 11**, **64 cases per task**, under ordinary full-difficulty
resets. Also evaluate the original source on each of the five single-missing-cell
task-11 diagnostics, **32 cases per cell per seed**. Positions, in fixed order:
top-left, top-center, top-right, upper handle, lower handle. Assisted reports
stay separately labelled and cannot count as ordinary skill or certification.

Train control followed by candidate to **250,000 additional accepted task-11
samples**, allowing at most **50,000** overshoot. Stop cleanly and preserve each
complete checkpoint. Repeat both complete ordinary suites. If EITHER arm fails
the relative-retention screen, stop before further training and retain all
adverse results.

Only if the early screen passes, resume the exact two stopped checkpoints to
**1,500,000 total additional accepted task-11 samples** per arm, with the same
overshoot limit. Repeat both complete ordinary suites and all five single-cell
diagnostics for both final models. Preserve all results, even if acquisition
fails. No adaptive budget extension, selected checkpoint or substitute seed.

A complete run has **12 ordinary reports / 9,216 trials**, plus **30 explicitly
assisted reports / 960 trials**. No incomplete subset is described as complete.
All evaluations freeze weights and accept zero training samples.

### Separate decision rules

Relative retention: at baseline, early and final, every tested protected model
must score no more than **4/64** below the same-seed source on EACH task.
Apply this to both continued arms, not only candidate. This 6.25 percentage-point
descriptive engineering screen is not a statistical noninferiority guarantee.
Check exact source-function preservation separately from asynchronous rollouts.

Ordinary acquisition gain: final candidate task 11 must achieve at least
**8/64 on each seed**, AND exceed final control by at least **4/64 on each
seed**. A retained model with zero ordinary completions still fails acquisition.

Intermediate placement gain: on each seed, candidate must complete at least
**4/32 in EVERY missing position** and at least **32 more completions out of
160** than control. Report all five counts and baseline counts. Passing this
assisted screen without ordinary acquisition demonstrates an intermediate
behavior only, not a learned complete recipe.

These rules are fixed before outcomes. No result alone authorizes adopting the
fixed-task architecture. Mainline may receive evidence; productionization still
requires a reusable multi-task learning path, ordinary new-skill acquisition,
retention and bounded resource costs. Prior failed studies remain unchanged.

### Execution and evidence boundaries

Candidate worktree: `/home/coder/workspace/botsclustersmc-protected-placement`.
Runner: `tests/studies/protected_placement.py`; unit tests:
`tests/studies/test_protected_placement.py`. Evidence is exclusively in a new
`.build/protected-placement-study/`. Existing evidence is never overwritten.
The reference source/runtime and input files are read from the preserved
`botsclustersmc-protected-continuation` worktree. Verify its source remains
`e1f8e6567116c02e0af18e64a466f7e0289e5a14` and its runtime identity remains
`49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9`.

Use one experimental learner at a time, 512 actors, 2 GiB heap, two region
threads, one inference worker, one learner worker, training seed 7, loopback
ports 30951/30952. At most two evaluators run concurrently on 30960-30963.
Keep the pinned Minecraft server and a 2 GiB available-memory floor.
Training segments have a 30-minute operational cap. Operational failure is
reported separately, not silently rerun as a new scientific attempt.

Capture complete checkpoints, first-issued coverage, training-status histories,
sample counts, scope-skipped samples, artifact identities, all reports and
per-trial diagnostics. Python optimization must not disable acceptance checks.
The live mainline server remains independent and is not reset or replaced.
Raw evidence is local retention, not an off-machine public archive.

## Disclosed operational stop and recovery declaration

The original execution at `ca75ffcc03348083e282406a73d0e14964c47444` completed
all fourteen baseline reports: 3,072 ordinary trials and 320 explicitly assisted
trials. It then stopped during the first control startup because the new Python
runner incorrectly required eighteen learned-task counters. The unchanged Java
`TaskBalance` publishes eighteen named tasks **plus an unlabelled bucket**, for
nineteen counters. This is a runner validation defect, not a policy failure.
The native saved checkpoint audit reports 332307 updates / 92,574,673 accepted
samples: **four** additional task-11 samples, with the source anchor unchanged.
Candidate training never started; no early or final comparison took place.
The owned control server shut down, and no experiment process remained.

The complete original attempt remains in `.build/protected-placement-study/`.
Its declaration identity is
`fd392dab3dac602f1d6de9a4f64c29f715f7fce022b00380970d0da662fd1ae5`,
failure record is
`50e9c982735c6a838ab6ddf09048e8d91ff100c333ea415d3eb0a4dfe93a2049`,
and stopped checkpoint is
`e3422c34e7a27e2844668d755bada28043394611ba736c5e89bd9f38453fff79`.
None is overwritten, deleted, or reclassified as a scientific retention failure.

The baseline source's five assisted counts, ordered as above and out of 32,
were **17,20,18,0,1** on seed 2026092981 and **15,17,15,0,1** on seed
2026092982. Both source ordinary pickaxe results were 0/64. Both protected-initial
ordinary suites passed the originally declared relative-retention screen.
These are measured initial conditions, not candidate improvements.

Before any further game execution, publish this recovery declaration and the
runner-only repair. The repair accepts exactly nineteen finite nonnegative
integer counters and still rejects **any** accepted sample outside task 11,
including the unlabelled bucket. Tests cover the actual shape, every prohibited
bucket, missing/extra entries, malformed values, booleans, floats and overflow.
Both runtime JARs remain byte-identical to the originally declared pair.

Recovery uses a new owned `.build/protected-placement-recovery/` directory.
It validates **all fourteen existing baseline reports** against the same input,
metadata, runtime, per-trial coverage and receipts, then references them without
rerunning them or counting them twice. A baseline-reuse manifest retains every
original report identity. There is no selection of a favorable baseline subset.

Both learning arms start again from the **original exact protected input**, not
from the four-sample operational checkpoint. Those four samples are separately
accounted operational work, excluded from both comparison arms. The training
seed, evaluation seeds, task order, sample targets, overshoot, stop rules,
ordinary acquisition gate and assisted placement gate above are unchanged.
If the run completes, its evidence consists of the fourteen reused baseline
reports plus twenty-eight new reports, not fifty-six independent reports.
This explicitly disclosed recovery repairs execution only; it does not erase the
failure, revise a scientific threshold, or authorize a deployment.

## Measured ordinary outcome: retention passed, acquisition failed

Interim closeout: all twelve ordinary reports are complete. The final five-cell
assisted matrix is still running under the unchanged declared protocol. This
interim record does not claim that all 42 reports have completed.

The recovery used published implementation `e86f64a33e51b6d6b72d90cbaed3b65edb3d72ee`,
tree `10d3ad50f1f039417cc2e439d78e62f349a3b53f`. The prospective protocol and
recovery declaration above are retained byte-for-byte. Both arms completed the
predeclared training budget and all twelve ordinary frozen evaluations. Every
protected model passed the task-by-task relative-retention screen at baseline,
early and final boundaries. Ordinary wooden-pickaxe completion remained **0/64
on both seeds in both final arms**. This fails the 8/64 floor and the required
4/64 improvement over control. No reset-mixture adoption or deployment follows.

### Complete ordinary scores

Each vector is ordered tasks **0 through 11**, every entry out of **64**. Seed A
is **2026092981**; seed B is **2026092982**. All conditions are ordinary,
full-difficulty, stochastic frozen-policy evaluations with zero training samples.

```text
Boundary  Model       Seed  Scores (tasks 0..11)
Initial   Source      A     64 64 63 64 64 64 64 64 64 63 62 0
Initial   Source      B     64 64 63 64 64 64 64 64 64 64 58 0
Initial   Protected   A     64 64 63 64 64 64 64 64 64 63 62 0
Initial   Protected   B     64 64 63 64 63 64 64 64 64 64 58 0
Early     Control     A     64 64 63 64 64 64 64 64 64 63 62 0
Early     Control     B     64 64 63 64 63 64 64 64 64 64 58 0
Early     Candidate   A     64 64 63 64 64 64 64 64 64 63 62 0
Early     Candidate   B     64 63 63 64 63 64 64 64 64 64 58 0
Final     Control     A     64 64 63 64 64 64 64 64 64 62 62 0
Final     Control     B     64 64 63 64 64 64 64 64 64 64 58 0
Final     Candidate   A     64 64 63 64 64 64 64 64 64 62 62 0
Final     Candidate   B     64 63 63 64 64 64 64 64 64 64 58 0
```

Final workbench scores were **62/64 and 58/64** in both arms, equal to the
same-seed source counts. Every retained task was within the declared margin;
no average concealed a failed task. The final candidate differs from source by
minus one success on task 9 for A and on task 1 for B, with the other counts
unchanged. Exact anchor-function preservation does not guarantee identical
asynchronous server trajectories. The two evaluation seeds remain conditions
of one training continuation, not independent training replications.

### What the ordinary traces locate, and what they do not

This is a post-hoc description of already recorded trajectories, not a new
acceptance rule or a causal attribution. The final candidate observed an open
workbench in **13/64 cases on each seed**. Its maximum-correct-cell histograms,
ordered zero through five, were **[59,4,1,0,0,0]** and **[62,2,0,0,0]**.
The final control histograms were **[60,4,0,0,0,0]** and **[48,15,1,0,0,0]**,
with open workbenches in 17/64 and 26/64 cases. No final ordinary trial reached
three correct cells or an observed target preview. This is not a completed
recipe that was merely left uncollected.

For the upper-handle cell, the candidate's joint fill probability, averaged over
observed pre-action states with the matching ingredient on the cursor and that
cell empty, was approximately **0.00000773** and **0.0000303**. There were only
63 and 107 such states. These correlated, policy-dependent states are not
independent trials or a randomized comparison. Open/preview counts are
pre-action observations; cell maxima also include post-action states and do not
exclude other incorrectly occupied cells. None is a success certificate.

The final training segment's completed one-missing-cell practice counters rose
from **517 in control to 1,352 in candidate**, with 213 and 500 reported
completions respectively. These are mixed-policy, changing-difficulty practice
outcomes, not frozen skill measurements. The counters establish exposure, not
an improvement in ordinary execution. Completed four- and five-missing-cell
practice counters were zero in both final segments; full-condition probes were
still run (622 control, 606 candidate, all unsuccessful). Probes are learning
rollouts in this implementation; only EXAM rollouts are excluded from training.
Thus these observations do not support a claim that the learner received no
full-condition samples, or that practice success equalled retained competence.
