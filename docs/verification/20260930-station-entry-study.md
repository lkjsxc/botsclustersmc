# Station-entry warm continuation: prospective learning test

Date: 2026-09-30 (Japan). This protocol and implementation are committed before
any new training or evaluation below. The earlier
[protected attribution and current-model entry ladder](20260930-protected-attribution.md)
are complete, separately retained experiments; their conditions are not changed.

## Question and single intervention

The current-main frozen policy ordinarily crafted a wooden pickaxe in 1/32 and
0/32 cases on a paired starting-state ladder, but in 30/32 and 31/32 with only
the initial workbench interface open and all raw ingredients still unarranged.
Do not infer that the current model lacks the recipe from a much older candidate.
Test a learned bridge to the useful open-station behavior rather than automatically
opening stations during ordinary gameplay or adding more output projections.

For station PRACTICE at difficulty d between zero and one, the candidate starts
with the station closed with probability d, instead of always open. A separate
lesson-seeded reset draw selects that state. At d=0 it stays open; full-difficulty
practice, probes and exams stay closed exactly as before. The choice is monotone
for a fixed lesson seed. It consumes no ingredient/cursor RNG: every retained
open start preserves the original material placement and RNG continuation exactly.
Closed starts preserve the original raw stock and empty cursor. No later action
is selected by this reset choice. The assistance label describes the actual start.

This applies through the common station helper to wooden/stone pickaxes, furnace
and chest practice. This learning test measures the current wooden-pickaxe frontier;
it does not establish a learning benefit on later station tasks. Other tasks'
menu selection and full-condition evaluation are unchanged. No reward, goal,
terminal success, action mask, primitive action, observation, network, optimizer,
course serialization, review allocation or inference-plugin code is changed.
The existing pose assistance still follows the lesson's difficulty.

Current difficulty derives from an exponential average of **all non-exam task
outcomes**, including probes, not solely assisted practice success. Probes already
start closed and generate learning samples. Therefore this is a graded-transfer
hypothesis, not the false claim that the old learner sees no closed-state data.
Long failed closed episodes can dominate ticks even with a small episode fraction;
retain and report actual task samples and frontier/review/exam effort.

## Exact warm inputs and isolation

Both arms begin with the exact complete canonical checkpoint, including model,
Adam state and course. No focused-policy conversion or optimizer reset is applied.
The approved development Academy remains running and untouched.

| Identity | Value |
| --- | --- |
| Control source, runtime-identical to accepted main | `63fcde679bd3188716193787aa11098eccd3df0e` |
| Accepted diagnostic main merge | `8967d7690a6eafe1ca9d8018795432aadf51d1fd` |
| Initial trained samples / policy updates | 297,530,150 / 1,019,070 |
| Canonical checkpoint SHA-256 | `294684242df33f8547406fe561a91be2b1fb0eff7c682e96dbb3038ce32280a1` |
| Initial policy SHA-256 | `2c9297f28855c081ed0516933f01fdc6406f57b0409794cecbaffebc3cf34720` |
| Control and all evaluation runtime SHA-256 | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |
| Candidate training runtime SHA-256 | `007b9ee76eb30b81f64e9fbae2584f3c178a7eab72608697d0bcd44777a7099e` |
| Both inference-plugin SHA-256 values | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The two inference JARs are byte-identical. Comparing every training-JAR member
finds exactly one changed member: `StationPractice.class`. Native checkpoint
export and byte-exact canonical/export verification qualify both arms before
starting. Source and runtime identities are checked throughout the run. The
candidate never substitutes its curriculum into the evaluator: **every frozen
report runs the unchanged control runtime** and unchanged holdout source.

Use distinct owned Academies under the study worktree, 512 actors each, seed 7,
2 GiB Java heap, two region threads, one inference thread, one learner thread,
two visible CPU processors and loopback-only ports 31081/31082. Both training
arms may run concurrently. The original checkpoint is copied, never overwritten.
Each segment must end with a controlled stop and a native-verifiable canonical
checkpoint, including failed/early-stop states. No checkpoint is selected after
looking for favorable late performance.

## Prospective evaluation and stopping rule

Use seeds **2026093011 and 2026093012**, 32 cases per task. Evaluate ordered tasks
**[0,1,2,3,4,5,6,7,8,9,10,11]** in the ordinary `none` condition. Separately evaluate
ordered task **[11]** with the existing `workbench-open` reset intervention and
raw ingredients. Keep assisted completion separate from ordinary competence.
The common parent is evaluated once per seed/condition before either arm trains:
four baseline reports, 832 trial executions. Do not borrow baseline scores from
another seed or a different ordered task list.

Input qualification: every ordinary task 0-10 and the open-workbench raw-assembly
condition must have at least 28/32 successes on **both** seeds. Ordinary task 11
is measured, not required to pass. If qualification fails, stop without training.

Training boundaries are cumulative **+500,000 accepted samples** (early) and
**+2,000,000 accepted samples** (final), measured from the same 297,530,150 parent
counter in each arm. Allow at most +50,000 stop/flush overshoot at either boundary.
This budget counts all accepted tasks, not offered samples, decisions, episodes
or wall time. Keep per-task sample totals and observed tick effort. Both arms
must reach the early boundary and stop before evaluating either early result.
The later segment must continue the exact preserved early checkpoint.

At each boundary, finish all eight reports (two arms, two seeds, two conditions)
before making the retained-skill decision. On each seed and in **each arm**:

- Each ordinary task 0-10 must be at least `max(28, parent_successes - 3)` /32.
- Ordinary task 11 must be at least `max(0, parent_successes - 2)` /32.
- Open-workbench raw assembly must be at least `max(26, parent_successes - 4)` /32.

Failure in either arm at the early boundary prevents **both** final continuations;
a control regression is not silently treated as proof of a candidate-specific
cause. A candidate regression also cannot be hidden by later recovery. The early
check is a retention screen, not a claim that 500,000 samples are sufficient to
learn entry. Passing retention permits the fixed final budget even without early
ordinary improvement.

A useful final pilot requires all retained-skill screens plus, on **both** seeds,
candidate ordinary task-11 completion **at least 8/32 and at least 4/32 higher
than the matched control**. Assisted gains alone cannot satisfy this condition.
This is an engineering pilot screen, not a confidence interval, ordinary mastery,
independent training replications, natural-terrain transfer or cooperative survival.
Only a completed favorable pilot is eligible for an adoption decision; the runner
never installs a model or merges a branch automatically.

Operational bounds: 1,800 seconds per training segment, 1,200 seconds per evaluation,
2 GiB effective available-memory floor, at most two training servers or four
loopback evaluators at once. Fresh process health must show no inference failures,
rejected/stale learner samples or retired actors; all 512 initial actor lessons
must be observed with restored-checkpoint status. Preserve partial failure evidence
and stop owned processes on failure. An operational failure is not a completed
learning verdict. Maximum frozen evidence if the final boundary is reached is
20 reports / 4,160 trial executions; early stop gives 12 / 2,496.

## Pre-execution engineering validation

The new station-entry reset test fails against the original runtime because closed
start frequency does not increase. With the candidate it passes **1,777,212**
checks covering all station types, monotone seed-level fading, empirical frequency,
all non-station/full-condition boundaries, exact retained-open state/RNG equality,
closed raw-state conservation, no completed output and correct assistance labels.
The original complete ingredient-reset distribution remains independently tested;
actual selected start composition is covered by the new test rather than falsely
assuming every sub-full lesson still starts open.

Both complete local source suites passed. Twelve offline controller tests pass in
normal and optimized Python, covering complete ordered report matrices, per-seed
and per-arm retention/gain thresholds, assistance separation, strict types,
accepted-sample caps, health freshness, monotone counters and no-start CLI/EULA
behavior. These tests enter CI but do not count as learned Minecraft success.

Run explicitly, only after committing the source and declaration:

```sh
EULA=true python3 tests/studies/station_entry.py --run
```

The workspace-specific controller pins its owned paths and source artifacts. It
is a finite research procedure, not an unattended deployment service. Exact input
and each stopped model/optimizer/course state, all raw reports, metadata, receipts,
health histories and decision results remain in `.build/station-entry-study/`.

## Execution results

At declaration no gameplay or learning outcome was claimed. The results below
were recorded afterward without changing the protocol, thresholds or seeds.

### Source qualification and completed baseline

Actual implementation and prospective declaration:
`0a3dbf192b5b16e2042ceeb84d52149293b2c41e`, tree
`75e9a37fb5f9f357a12a993e9b795dc1317661ba`, authored as `lkjsxc` and pushed
normally before execution. Research PR #47 is not an accepted runtime change.
GitHub run `36647078174` passed Ubuntu source, Windows source and observatory
checks. Dispatch-only live, Paper, retention and Windows-live jobs were skipped,
not counted as executed tests. The 12 controller tests also passed again in normal
and optimized Python during closeout. Both initial native exports reproduced the
same prescribed policy from the same complete model/optimizer/course checkpoint.

All four baseline reports completed before either learner started. Seed A is
`2026093011`; seed B is `2026093012`. Ordinary vectors are ordered tasks 0-11,
each score out of 32. Assisted rows are complete wooden-pickaxe production with
only the initial workbench menu opened and no arranged ingredients.

```text
parent ordinary A: [32,32,32,32,32,32,32,32,32,32,32,0]
parent ordinary B: [32,32,31,32,32,32,32,32,32,32,32,1]
parent open A/B:    32/32, 31/32
```

Both seeds passed the declared input qualification. These are the study's new
baseline seeds and ordered task lists, not scores borrowed from the earlier
entry ladder. All evaluations used the unchanged control runtime and contributed
zero training samples.

### Matched early learning segments

Both separately owned 512-actor learners reached the early accepted-sample
boundary and stopped cleanly before the early frozen matrix. The original
checkpoint was not overwritten, and the running development Academy was untouched.

| Arm | Additional accepted samples | Stopped total samples | Stopped updates | Segment wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 505,094 | 298,035,244 | 1,020,776 | 270.756 |
| Candidate | 505,713 | 298,035,863 | 1,020,693 | 270.705 |

Both totals satisfy +500,000 through +550,000. Wall time is descriptive, not a
throughput benchmark; similar accepted-sample totals do not imply identical
optimizer trajectories, episode counts or observed context coverage.

| Stopped artifact | SHA-256 |
| --- | --- |
| Control canonical checkpoint | `3c060a45f445469fd99a81f2b6329bc4441c291d09fff43587d0aabbd0e60547` |
| Control exported policy | `2742fdbfc9ea904e96ce34a3e4cb1110cc9daa0d11d4198d5d1ab23a3801c9f1` |
| Candidate canonical checkpoint | `dfc73e4fc66e76e21f116db0af2580662a762902f03282156212f499c9f81bfb` |
| Candidate exported policy | `0625c1104f9e76653b358a41120e9fd3c5250d750d2248e277ca78c19ce1ec68` |

Native `verify-export` passed again for both stopped states at closeout. Each
process retained 52 status observations; their clocks, total counters and task
counters were monotone. Both recorded all 512 first-issued lessons at task 11
with restored-checkpoint status, and ended with all 512 actors progressing.
Inference failures/rejections, rejected/stale learner samples and retired actors
were zero in the retained histories.

The last observed task-11 accepted counts were control 412,777 and candidate
418,905. Total accepted counts in those same observations were 503,154 and
504,401. The final stop/flush added 1,940 and 1,312 samples respectively; do not
mix the earlier per-task snapshot with the later exact stopped denominator.
Observed frontier/review ticks were control 2,102,795 / 452,545 and candidate
2,135,190 / 427,695; no exam ticks were recorded.

At the last observation, task-11 practice completion was control 2,416 successes
among 2,925 finished episodes and candidate 251 among 763. Completed probes were
53/524 and 32/335. These are changing-policy, completed-episode counters; they are
not frozen competence estimates, counts of all started episodes, or direct
measurements of accepted closed/open-menu samples. The fewer completed practice
episodes under the candidate must not be hidden by reporting only total samples.

### Complete early matrix and rejection

All eight early reports completed, preserving both arms, both seeds and both
ordinary/assisted conditions.

```text
control ordinary A:   [32,32,32,32,32,32,32,32,32,32,30,2]
control ordinary B:   [32,32,32,32,32,32,32,32,32,32,32,3]
candidate ordinary A: [32,32,32,32,32,32,32,32,32,32,32,0]
candidate ordinary B: [32,32,32,32,32,32,32,32,32,32,32,1]
```

| Complete task-11 condition | Parent A | Control A | Candidate A | Parent B | Control B | Candidate B |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Ordinary, initially closed | 0/32 | 2/32 | 0/32 | 1/32 | 3/32 | 1/32 |
| Initially open, raw assembly | 32/32 | 32/32 | 25/32 | 31/32 | 31/32 | 23/32 |

Both arms retained every ordinary task 0-10 under its declared floor. The
candidate's raw-assembly scores failed the separate 28/32 and 27/32 floors.
The control retained raw assembly on both seeds. Ordinary candidate completion
did not improve over the parent and did not pass the acquisition screen.
Retention failure, not the absence of immediate ordinary gain alone, triggered
the predeclared early stop. Neither +2,000,000 continuation ran.

The outcome is `stage=early`, `input_qualified=true`, `retained=false`,
`acquisition=false`, `useful_pilot=false`, `deployment=false`, with exactly
12 reports and 2,496 frozen trial executions. There is no operational-failure
record and no final-phase directory. This is a completed early rejection, not a
full-budget result, an incomplete matrix, or evidence of general impossibility.

Do not deploy this reset fade or either experimental model. The measured deficit
still concerns transferring useful open-workbench behavior to ordinary entry;
simply fading the initial opening with the existing difficulty was not sufficient
under this protocol and damaged previously useful assembly. This comparison does
not identify which gradients, optimizer moments or observation subsets caused the
loss, nor establish a learning benefit or failure on furnace/chest/stone practice.
Two evaluation seeds are not independent training replicates.

Before another acquisition change, distinguish accepted pre-action contexts and
elapsed actor ticks rather than relying on completed episodes or aggregate task
samples. Read-only context accounting must not select actions, alter rewards,
rebalance losses or reinterpret old saved counters. It cannot retrospectively
supply the missing closed/open split for this completed experiment.

### Preserved and revalidated evidence

Raw evidence remains under
`/home/coder/workspace/botsclustersmc-station-entry/.build/station-entry-study/`
in `lkjsxc/tomato-ocelot-73`. A fresh read-only closeout audit revalidated all
12 original result/metadata/receipt/policy bindings, native stopped exports,
per-arm accepted-sample budgets, retained histories, the complete matrix and the
independently recomputed per-cell floors. No gameplay was rerun or added to the
2,496 denominator during that audit.

The unpublished GitHub research draft `station-entry-study-20260930` points to
source `0a3dbf1` and retains `station-entry-evidence-20260930.zip`: 80 members,
4,540,258 bytes, SHA-256
`f33e0f250b94c4938c14ec32d4e292fa90225853393a5625dce1b036399be374`.
A fresh download matched the local archive; ZIP integrity and every manifest
member's size and digest passed. The archive includes the parent, both stopped
canonical states, runtime JARs, original protocol/controller, all frozen reports
and receipts, histories and validation logs. It excludes Minecraft server
binaries, worlds, cache, credentials and unrelated projects. It is not a released
plugin, an accepted checkpoint or a skill certificate. Failed evidence remains
available even though the experimental runtime is not merged.
