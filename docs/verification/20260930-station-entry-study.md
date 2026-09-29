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

No gameplay or learning outcome is claimed at declaration. Append results here
without changing the protocol, thresholds, seeds or rejected intermediate states.
