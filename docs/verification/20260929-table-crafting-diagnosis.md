# Workbench crafting diagnosis — 2026-09-29

## State of this change

**Checkpoint, not a completed mainline rollout.** PR #31 contains the diagnostic
implementation `6bc5e4087640341b5a05061c5dd018c72ff7207b`, tree
`f4ff0a75b12fc1327b46723b4468d80363839672`, based on main
`3b272d622647225445eeef15e75ae6c0418a6357`. It was committed as lkjsxc and normally
pushed on `study/table-crafting-20260929`. An independent GitHub PR read confirmed
it was open and unmerged. Mergeability is not a successful CI result.

The final-source local suite, final-source two-seed evaluations, and initial
PR CI `36444134388` were launched. They were still unconfirmed at their last
successful observation. A later combined result-aggregation/check-status tool
invocation was blocked by tool safety verification. That request was not retried
or repackaged. This record does not infer the missing outcomes or label them
failures. Inspect the retained outputs and current CI before accepting the change.

No production mainline update, service restart, weight reset, learner change or
policy adoption has been performed for this change.

## Question and scope

The development Academy had 512 active and recently progressing agents, all at
task 10, while the learner continued to update. Moving-policy probe averages
cannot establish what one fixed model can do. The existing detailed crafting
trace covered pickaxes, not the personal-inventory workbench recipe; the generic
visible-recipe and crafted-unit counters did not identify the output material.

Freeze one canonical checkpoint, measure the retained tasks and frontier, then
observe actual assembly transitions without changing any gameplay. Do not hide a
poor confirmation seed or equate an observer change with improved learning.

## Fixed input and completed preliminary measurements

The mainline developer runner froze policy **253067**, trained samples
**70435332**, from the canonical live training checkpoint. Policy SHA-256:
`ea5d92ef03daa5eb15a6e07a5033b16b56b8ac7712367d282a5f4be9c0265f2b`.
Every subsequent invocation in this study names that saved policy, never a newer
live checkpoint. Task order is exactly 0 through 10, 32 cases each. Seeds are
`2026092901` and `2026092902`; standard full-difficulty resets, stochastic neural
actions, no reset intervention, no training and no certificate mutation.

| Completed measurement | Seed | Tasks 0–6 | Place block, task 7 | Planks/sticks, tasks 8–9 | Workbench, task 10 |
| --- | --- | --- | --- | --- | --- |
| Original main evaluator | 2026092901 | Each 32/32 | 32/32 | Each 32/32 | 20/32 |
| Preliminary observer | 2026092901 | Each 32/32 | 32/32 | Each 32/32 | 20/32 |
| Preliminary observer | 2026092902 | Each 32/32 | 30/32 | Each 32/32 | 14/32 |

These are measurements of the same frozen weights, not competing learners.
The 30/32 placement and 14/32 workbench confirmation results must remain visible.
Identical seeds do not guarantee identical server timing or action trajectories.
The observer's synthetic noninterference tests are separate evidence from the
matching seed-one success totals.

The original main run completed 352 trials and reported zero new training samples.
Both preliminary observer runs completed, and retained their complete per-trial
reports. The seed-one trace was additionally checked with the new Python
trace validator. These three completed runs total 1056 trials, not 1056 successes.
They predate the final-source addition of explicit per-material crafted counters;
those counters must not be retroactively claimed to exist in their reports.

## What the preliminary trace actually showed

In the seed-one observer run, 20 workbench trials succeeded and 12 failed.
Eight failures selected the non-target result and recorded stick gains. The
remaining failures included one that reached all four correct cells without
finishing and three whose maximum was three correct cells. This is not solely
an inability to place the fourth plank.

Across all 32 workbench trials, the trace recorded 7899 transitions and 6682
pre-action inventory-visible states. Of those inventory states, 3918 had fewer
than four carried planks and no carried table. There were 24 target-preview
states and 182 other-preview states, with 20 and 8 selected result clicks,
respectively. These are state/request counts, not elapsed-time fractions,
training-sample fractions or universally irrecoverable episodes.

Observed positive stick-stock changes summed to **36**, whereas the generic
original run showed eight failed trials with four crafted units each. This
motivated keeping stock gains separate from actual manufacture: dropping and
recovering an item can raise the positive-gain sum repeatedly. The final source
therefore also emits the exact final `Pocket.crafted` stick/workbench counters.
No claim of 36 manufactured sticks is made.

## Implementation and checks already confirmed

`TableCraftingTrace` reads task-10 observations and reconstructs the four physical
inventory cells, distinguishing stack size, correct cells, wrong cells, surplus,
preview material and actual carried stock. Closed-menu hidden grid cells are not
counted as missing stock. Fill and single-unit-fill probability sums use the full
operation-times-conditional-slot probability and explicit opportunity counts.
The applied frozen likelihood is checked; the observer never samples or changes
an action, weight, mask, random stream, reward, lesson or completion predicate.

Selected result clicks, observed stock gains and exact crafted counters are
separate. Removing partial ingredients and consuming a visible output are not
blindly combined. The observer intentionally excludes cell-change counts during
observed output-stock gains; those filtered counts are not a complete causal
inventory ledger. The native evaluator retains the detailed trace while keeping
its ordinary skill-success summary. The Python runner additionally validates
trace field types, finite sums, array lengths and denominators.

The first complete local source suite passed, including **1421 new Java checks**,
real-API compilation and artifact separation. Seven Python unittest methods
passed, including three new trace-report methods. The final source adds a
specific dropped-item-recovery test and exact crafted-counter output; its final
suite outcome is not asserted here without the retained result being checked.

A dependency-setup attempt using a symlinked cache was refused by the existing
managed-path guard. The test-owned symlink was removed and replaced with an
ordinary cache copy; no guard was disabled. The resulting full preliminary
source suite completed successfully.

Freshly built final-source JARs were byte-for-byte identical to main:

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `training.jar` | 212945 | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |
| `botsclustersmc.jar` | 120533 | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

Both JARs were checked to exclude holdout/TableCrafting classes. `Host.java`
changes only the test list. No core, plugin, training, server pin, service unit,
network exposure or dashboard implementation changes are included.

## Retained evidence and next decision

Production: `/home/coder/workspace/botsclustersmc`. The original freeze is its
`.build/holdout-crafting-20260929-baseline/`, including `source-training.bcmc`,
the saved policy, metadata, runtime, evaluator and complete result.

Owned worktree: `/home/coder/workspace/botsclustersmc-table-crafting`. Its ignored
`.build/` retains `source-first.log` (cache rejection), `source-second.log`
(completed preliminary suite), `source-final.log`, `source-final-result.json`
(when the runner writes it), `table-seed1/`, `table-seed2/`, `final-seed1/`,
`final-seed2/` and the associated invocation logs. Do not overwrite or discard
unfavorable results. Local retention is not permanent off-machine archival.

Final-source invocations retained the same ordered tasks, cases, two seeds and
saved weights; their loopback port is 25585 rather than the preliminary 25584.
The first final-source evaluation overlapped the end of the preliminary second
seed and the local test suite. This is not a throughput benchmark. Their
outcomes remain outside the completed-results table until independently checked.

At the initial live snapshot, 512/512 agents progressed, inference failures and
rejections were zero, and accepted samples were 70428923. The latest successfully
observed production Minecraft PID was still 121289. These observations do not
certify later liveness or current-model skill.

The next learning investigation should distinguish scarce-material consumption
by the wrong recipe, recovery/assembly loops, and failure to collect a completed
table, while retaining the correctly learned stick recipe on task 9. Do not
hide the wrong recipe with a goal-dependent mask or add a recipe macro. Any
reward, termination or allocation experiment needs its own declared budget,
retention thresholds, separate state and fixed-policy confirmation; none was
conducted or accepted here. No improved learning, survival or cooperation is
claimed by this diagnostic-only checkpoint.
