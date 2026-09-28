# Workbench crafting diagnosis — 2026-09-29

## State of this change

**Accepted diagnostic-only change.** PR #31 merged as
`92d51486809a38557d42c2d60998e5e2123c9ece` after final-source results and CI
were checked. Its implementation is `6bc5e4087640341b5a05061c5dd018c72ff7207b`, tree
`f4ff0a75b12fc1327b46723b4468d80363839672`, based on main
`3b272d622647225445eeef15e75ae6c0418a6357`. It was committed as lkjsxc and normally
pushed on `study/table-crafting-20260929`. The merged tree is
`dd39e6a9f3e3900897aacc115b331854c2f859f1`, identical to PR head
`f5e587b7d80c19d2889ee749237cccaace1b5bd6`. GitHub's PR state and main ref
were independently re-read after merging; the production checkout was then
fast-forwarded and rebuilt.

At the earlier checkpoint, a result-aggregation call was blocked by tool safety
verification, so final results and CI were left unconfirmed rather than inferred.
On continuation the ordinary tools successfully read the retained suite/logs,
validated both complete final reports, and confirmed final-head CI
`36444554292`: Ubuntu, Windows and observatory succeeded. Optional live/retention
CI jobs were skipped and are not counted as gameplay evidence. An additional
optional query for probability breakdowns and class-byte comparisons was blocked;
no result from that query is claimed or required for the recorded checks.

There is no learner, reward, curriculum, gameplay, schema or policy change.
Fresh mainline training/inference JARs match the validated candidate byte-for-byte;
the installed training JAR also matches. Training and monitor processes do not
need a restart for identical implementations. The disposable evaluation service
is executed from integrated main to activate the changed diagnostic source;
its activation result is recorded below. No learning data was reset.

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

## Verified final-source measurements

The two final-source runs also completed, with the same saved policy, ordered
tasks 0–10, 32 cases per task, the two declared seeds and no reset assistance.
Both logs report zero new training samples, complete trials and unchanged weights.
A separate verification on continuation rechecked policy/runtime/evaluator
identities, every actor's task and seed, aggregate success counts, positive trial
durations, unchanged policy, absence of a training checkpoint and all task-10
trace denominators. No new model was selected for this verification.

| Task | Seed 2026092901 | Seed 2026092902 |
| --- | ---: | ---: |
| 0 forward-stop | 32/32 | 32/32 |
| 1 turn-stop | 32/32 | 32/32 |
| 2 aim-hold | 32/32 | 32/32 |
| 3 navigate-stop | 31/32 | 32/32 |
| 4 step-over | 32/32 | 32/32 |
| 5 break-log | 32/32 | 32/32 |
| 6 collect-log | 32/32 | 32/32 |
| 7 place-block | 32/32 | 30/32 |
| 8 craft-planks | 32/32 | 32/32 |
| 9 craft-sticks | 32/32 | 31/32 |
| 10 craft-workbench | 20/32 | 14/32 |

The 31/32 navigation and stick results are not replaced by the more favorable
preliminary runs. Workbench totals coincide with the preliminary measurements,
but trajectories and some other-task outcomes differ. This is not an exact
live-game replay or a before/after learning comparison. The five complete suites
now total **1760 trials**, of which **704** use the final diagnostic source.

Final `result.json` identities:

- Seed 2026092901: `5f8423b63e3edd6854fa98f2b38b532814a735ee834a2023813de020b6eefb95`.
- Seed 2026092902: `81a00ecc7da84a47d074ee15c4fb411d6e01212c7efb04ff2c84724bd0e7d5e4`.

The exact crafted counters confirm four sticks were manufactured in **8 of 12**
failed workbench trials on the first seed and **13 of 18** on the second.
That is 21 of the 30 failures across these two measured suites, not an estimated
failure proportion for arbitrary future policies. None of these 21 trials
manufactured a workbench. Of the other nine failures, three reached four correct
grid cells at some point but never manufactured a table; six reached at most
three correct cells. Reaching four cells does not establish how long a valid
collection opportunity remained. Missing carried stock may still be in the world.

The exact per-material counters, rather than sums of positive stock changes,
support these manufacture statements. The three failure groups motivate separate
learning hypotheses; the observer does not identify their optimizer/reward cause.

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
specific dropped-item-recovery test and exact crafted-counter output. Its full
local suite completed successfully on implementation `6bc5e40` in 69.128 seconds,
including **1425** new Java measurement checks. The seven Python tests were run
again successfully on continuation. Merged-main CI **36445748144** also
completed successfully for Ubuntu, Windows and observatory; opt-in live/retention
jobs were skipped. Neither unit-test counts nor CI success establish learned skill.

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

## Integrated activation and uninterrupted learning

The production checkout was fast-forwarded to merged main `92d5148`, with no
tracked differences from the accepted PR tree, and `./build.sh` completed.
Both fresh artifacts retained the identities above; the installed
`academy/server/plugins/training.jar` was also byte-identical. The learning
runtime, monitor source/page, network settings and service configurations did
not change. No training/monitor restart or checkpoint reset was necessary.

At **2026-09-29 00:43:55 JST** (September 28 15:43:55 UTC), the existing
`botsclustersmc-evaluation.service` was started from integrated main. Its
invocation was `dd3135c27290434994baf06f2b64dbe2`, main PID 293202. It exited
normally at **00:46:46 JST**, status 0, `Result=success`. Its documented 1-GiB
heap and two-visible-processor service setting were unchanged.

This activation run froze a different, later policy: **263490**, samples
**73542651**, seed **-2205686911000250895**. All 352 trials completed with zero
new training samples. Aim-hold scored 30/32, workbench 21/32, and each other
selected task 32/32. This random-seed service smoke test is not a sixth arm of
the pinned study and is not evidence of improvement caused by this observer.
The report timestamp is `1790610403690`; the evaluator JAR identity is
`23b7b0e2f20dce710cfaa4b1b07b5a908b8242555a711ea11f8f2701c10c95bf`.

The native detailed report was checked for the expected policy/sample identity,
all actors/tasks/seeds, aggregate success counts and the new `table_crafting`
trace on all 32 workbench trials. Those traces passed the Python denominator
validator, and all three exact manufactured/carried counters were present as
nonnegative integers. The live `/api/evaluation` returned HTTP 200, the same new
policy/report timestamp and a completed evaluator state. This checks the actual
service path, not only a developer fixture or an updated checkout.

Across fresh status snapshots **00:43:55.905–00:47:20.905 JST**:

| Measurement | Before evaluation activation | After completed evaluation |
| --- | ---: | ---: |
| Active / recently progressing agents | 512 / 512 | 512 / 512 |
| Accepted training samples | 73590716 | 74004543 |
| Policy updates | 263653 | 265035 |
| Decision transitions | 24097836 | 24513679 |
| Inference failures / rejections | 0 / 0 | 0 / 0 |
| Frontier task | 10 | 10 |

The second snapshot came through the live status HTTP endpoint and passed the
fifteen-second freshness bound; pending actors were zero. Supervisor PID 121202,
Minecraft PID 121289 and monitor PID 207536, with their original start times,
were unchanged. The evaluation timer remained active. This verifies continued
learning activity, not current-policy mastery or full survival competence.

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
seed and the local test suite. This is not a throughput benchmark. The final
outcomes were subsequently independently checked and appear in their own table
above; preliminary observations were not rewritten.

At the original study's initial snapshot, 512/512 agents progressed, inference
failures and rejections were zero, and accepted samples were 70428923. This is
historical context; the separate activation section records the later live checks.
Native service reports under `academy/server/plugins/BotsClustersMC/` rotate with
subsequent evaluations, unlike the five named study directories. The native
activation identities and results above retain what was observed, not a claim
that the rotating metrics will always contain that same report.

The next learning investigation should distinguish scarce-material consumption
by the wrong recipe, recovery/assembly loops, and failure to collect a completed
table, while retaining the correctly learned stick recipe on task 9. Do not
hide the wrong recipe with a goal-dependent mask or add a recipe macro. Any
reward, termination or allocation experiment needs its own declared budget,
retention thresholds, separate state and fixed-policy confirmation; none was
conducted or accepted here. No improved learning, survival or cooperation is
claimed by this diagnostic-only change.
