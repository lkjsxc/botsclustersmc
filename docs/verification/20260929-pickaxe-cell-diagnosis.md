# Pickaxe single-cell reset diagnosis — 2026-09-29

## Scope and prospective measurements

This is diagnostic tooling only. It does not change core, plugin or training
sources, rewards, optimizer, curriculum, skill completion, model schema, masks,
network exposure or production service settings. An assisted diagnostic is not a
standard skill evaluation and must never be accepted as a certificate.

The initial live check on main `3248d917989a15d70bd4895c4eada4f52bea2e9c`
found 512 active/progressing actors: 497 at task 11 and 15 at task 10. Historical
actor certificates are not current frozen-policy competence. Instead of changing
workbench rewards based on the previous frontier, freeze the current canonical
checkpoint and diagnose the newly reached wooden-pickaxe frontier.

The initial, unchanged-main measurement froze policy **300067**, trained samples
**82958930**. Ordered tasks 0 through 11, 32 cases each, seed **2026092903**:
task 4 scored 31/32, task 10 scored 25/32, task 11 scored 0/32, and each other task
scored 32/32. Opening the workbench at reset also scored 0/32 on task 11.
A full-grid reset diagnostic was started separately. These initial measurements
were inspected before selecting the five-cell diagnostic below; they are not a
pre-registered learning comparison.

Before running any of the five new conditions, declare:

- Keep that exact saved policy; no additional training samples or weight selection.
- Task 11, 32 cases for **each** of the five conditions, first seed **2026092903**,
  confirmation seed **2026092904**. Report every condition, including failures.
- Also measure the same frozen model on ordered full-condition tasks 0–11, 32
  cases each on the confirmation seed, and both existing open/full-grid
  diagnostics on that seed. Thus the planned complete study has 1216 trials:
  768 standard-condition trials and 448 explicitly assisted diagnostic trials.
- No learned improvement or mastery acceptance threshold is being relaxed:
  this change is acceptable only as non-certifying, reset-only measurement
  tooling with mechanical conservation, label/scope rejection, complete reports,
  frozen-policy identity and unchanged runtime artifacts checked.

The five missing cells are top-left (slot 36), top-center (37), top-right (38),
upper handle (40), and lower handle (43). Each start supplies the same three
planks and two sticks as ordinary task 11: four raw units are moved into the grid,
and the single missing unit remains in its original storage slot. The cursor is
empty. No crafted output, extra materials, extra observation fields or gameplay action
hints are supplied. Task 13 supports the same diagnostics with cobblestone, but its learned
performance is not part of this study. Unit tests cover both materials.

The existing evaluator applies assistance once on the owning entity thread,
after reset completion and before the first policy decision. Its ordinary task
success predicate, horizon, stochastic policy and observation/actuator remain
unchanged. Every assisted report carries its exact reset label, diagnostic-only
flag and full intervention coverage. The ordinary native evaluation/export path
must continue to reject any assisted report.

Production source and saved inputs:
`/home/coder/workspace/botsclustersmc/.build/pickaxe-frontier-20260929-baseline/`.
The canonical snapshot is `source-training.bcmc`; the policy is
`server/plugins/BotsClustersMC/policy.bcmc`. Preserve these and all adverse results.
The new owned worktree is `/home/coder/workspace/botsclustersmc-pickaxe-cells`.
Its `.build/` holds test logs and the additional measurements. Local retention is
not a claim of permanent off-machine archival.

## Validation and artifact boundaries

Reset implementation and prospective declaration were committed before the new
five-cell runs as `ae615175650e4c2b31650a857434d716b844994f`, tree
`350e13b357a29f468783d8cf8ed9c35bc805b45c`. A companion test/documentation commit,
`1f2173092bc0e4bd165adeba3ffb5397470d7452`, tree
`64faeeb9f596f4200f1c4922259ce658e3ae20d2`, adds all five labels to the native
report-rejection tests. It does not change the evaluator implementation or any
runtime source. The five-cell implementation and frozen inputs are constant
throughout the measurements.

The controlled full source suite passed on the first implementation and again
on the final companion source. The final run ended with `SOURCE_SUITE_EXIT=0`.
It included 1284 reset checks, 63 native evaluation-integrity checks (15 additional
new-label rejections), export/replay checks and inference-artifact separation.
Eight Python test methods passed, including all labels, mismatched labels,
non-pickaxe tasks, moving-checkpoint rejection and assisted-as-standard rejection.
The Java checks cover both wooden and stone ingredients, all five cells, selected
hotbar preservation, empty cursor, exact four-unit layout, storage remainder,
no supplied output, ordinary subsequent completion and malformed/repeated reset
rejection. Live scripted fixtures were compiled by this suite, not executed; the
separate frozen-policy runs below are the actual Minecraft evidence.

Adverse execution is retained, not counted as a passing gate: an initial tool call
was canceled by a one-second deadline; the first logged full run then failed with
`NoClassDefFoundError: TrainingState` at `ExportTest`. No product change was made
to conceal it. The two controlled full-suite runs passed; a specific root cause
for the first failure was not established. Logs are
`.build/pickaxe-cells-source.log`, `pickaxe-cells-source-repeat.log`, and
`pickaxe-cells-final-source.log` in the study worktree.

`CheckpointTool verify-export` independently confirmed that the saved policy is
exactly the policy in the retained canonical checkpoint. Rebuilt training and
inference artifacts are byte-identical to the production checkout; neither
contains a holdout/reset-diagnostic class:

| Identity | SHA-256 |
| --- | --- |
| Canonical snapshot | `cab092242122d7decb0e1066f8173a299fd580f6998c4c4cfcad163fa34e9885` |
| Frozen policy | `b79069d63941eb6a91f00313dd7a26c660458bc2a2ccb7e8ff1a72bde870555c` |
| Training runtime | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |
| Inference runtime | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

These identify local inputs; a digest is not independent proof of a claimed
measurement or a substitute for its completed trial data.

## Results

All **1216 declared trials** completed: **768 standard-condition** and **448
assisted diagnostic** trials, across 16 reports. Every report was revalidated
against its ordered task/case/seed specification, the exact same frozen policy,
zero new training samples, immutable runtime, actual evaluator artifact,
loopback metadata and the runner's completed-integrity log. All single-cell
traces observed at least four correct cells. Additional arithmetic checks passed
for mask partitions, cell counts, finite probability sums and their corresponding
opportunity denominators. These checks do not select favorable scores.

The ordinary full-condition results were:

| Task IDs | Seed 2026092903 | Seed 2026092904 |
| --- | --- | --- |
| 0–3 and 5–9, each task | 32/32 | 32/32 |
| 4, step-over | 31/32 | 32/32 |
| 10, craft-workbench | 25/32 | 26/32 |
| 11, craft-wood-pick | 0/32 | 0/32 |

Wooden-pickaxe completion under each declared initial condition:

| Initial condition | Seed 2026092903 | Seed 2026092904 |
| --- | --- | --- |
| `none` (ordinary, closed menu and raw stock) | 0/32 | 0/32 |
| `workbench-open` (raw stock) | 0/32 | 0/32 |
| `pickaxe-grid` (all five cells supplied) | 29/32 | 27/32 |
| `pickaxe-missing-top-left` | 1/32 | 0/32 |
| `pickaxe-missing-top-center` | 0/32 | 0/32 |
| `pickaxe-missing-top-right` | 4/32 | 4/32 |
| `pickaxe-missing-handle-upper` | 0/32 | 0/32 |
| `pickaxe-missing-handle-lower` | 1/32 | 1/32 |

The five single-cell conditions total **11/320** completions. Exactly those 11
trials reached five correct cells and a target preview; the other **309/320** did
not. The top-right condition accounts for 8/64 completions, versus 3/256 across
the other positions. This is descriptive position imbalance, not a significance
claim or a reason to omit the weak positions. Supplying the whole grid produced
56/64 completions, but supplies the assembly work and is not a learned-assembly
certificate. The two ordinary task-11 suites produced no target previews.

The evidence locates a weak transition between an almost completed recipe with
the remaining unit in storage and the completed recipe. Opening the menu alone
is insufficient. It does **not** isolate pickup versus placement versus
preserving previously correct cells; it also does not prove a representation,
reward, curriculum or optimizer defect. A useful next learning comparison should
measure all five single-cell positions separately rather than let successful
ready-output collection stand in for ingredient placement. That comparison has
not been performed here; no learner intervention or learned improvement is claimed.

The standard suites use tasks 0–11 in order, while every assisted suite uses only
task 11. Actor IDs also affect policy sampling, so these are not paired-action
counterfactuals. Even matching seed and task lists permit asynchronous server
variation. All results concern this one frozen policy in supplied/reset Academy
rooms, not natural-terrain survival, stone-pickaxe skill, cooperation, or the
continually changing production model. Historical per-actor certificates remain
separate from these current-model measurements.

## Evidence locations and report identities

The original main-source suites are under production `.build/` as
`pickaxe-frontier-20260929-baseline`, `pickaxe-frontier-20260929-open`, and
`pickaxe-frontier-20260929-grid`. The confirmation standard suite is study
`.build/full-condition-2026092904`. New missing-cell directories are
`.build/pickaxe-missing-<cell>-<seed>`; confirmation open/grid directories are
`.build/workbench-open-2026092904` and `.build/pickaxe-grid-2026092904`.
Each directory retains `result.json`, metadata, frozen policy, runtime, evaluator,
server log and every adverse trial. The sibling `.log` records runner integrity.
The study worktree also retains `.build/verify-pickaxe-study.py` and its generated
`.build/pickaxe-study-summary.json` with all 16 report paths and aggregates.

SHA-256 identities below refer to the complete corresponding `result.json`, not
just its task-11 row. They identify retained files, not external authenticity.

| Condition | Seed 2026092903 report | Seed 2026092904 report |
| --- | --- | --- |
| `none` | `2ed84adf0103168f8386e8592a6e61f55e08e5d97dbe5ada0a4785bc59d6bd4b` | `74b7f38a2ff1055459b42b56506ccd8ca62ffd269ce080b6f185195b10526d41` |
| `workbench-open` | `c0945b56a8e7800df241a01ff32867cb79ff122df01736ef49235033de84e772` | `e584ea50a0ef75bffc1e1d9e696c42f8bb80afbfef6686a68ca04cf26c960855` |
| `pickaxe-grid` | `a025f21d55afcdcb4462ecc430592d78bd28672c7675e9e7f0cc00b500542946` | `567ff8e1630b4b9dfd9fc28894e21d736dce04b58e9dfa369a71a4d755701855` |
| Missing top-left | `05c5998637fb926d52bcaf0bb91479425e823b58f922f08b8794990f548563f7` | `920162404ce69f35974da65ce7a1032501dac2ccdf3cdc0738986ca6c0f6e048` |
| Missing top-center | `cf11c7e38fa1fe62d363300de21f01757b50b3d65f1b019fd7e58b6b81c56ca4` | `048d84f2dff15aa32ef88fb1e5822dc6566e306c6a79966e450182b12a22e612` |
| Missing top-right | `7504b502d98159263942bb46c8311767db2c30927f418938a25f97509275c205` | `b0767011c06ac89a30176d351f36f9682edc4f6810f0819f187a9a15331c937b` |
| Missing handle-upper | `203e045cd64abe55fed9d2681a4591a2ec520d2c0b2c810688f154f92cd8a0e1` | `885927e0a6154587a48a6e3e71b09cbefc9637228abc7f8b0cf143b297ff7c4e` |
| Missing handle-lower | `352f7a4885c89fe6a9a345f718b697509f045a653cf930246ff33b03207f8a71` | `9d63a961c8eae151c6cec571cea00661a4fa0409a847836ccb86cc0b9a3c4540` |

## Production and integration boundary

Source CI run **36458029308** for `1f2173092bc0e4bd165adeba3ffb5397470d7452`
passed Linux, Windows and observatory jobs. Optional live/retention/Paper jobs
were skipped, not counted as passes. Real frozen-policy evidence is the separate
1216-trial matrix above. PR #32 contains only developer diagnostics, tests and
documentation. No production configuration, reward, curriculum, model format or
learning-source changes are included.

The installed `academy/server/plugins/training.jar`, production build and study
build were independently byte-compared equal. No production restart or weight
replacement is needed for this test-only change, and none was performed during
the study. A 15-second live verification at epochs 1790616535905–1790616550906
found all 512 actors active and progressing, inference failures 0, and accepted
samples advancing from 84733844 to 84763734 (approximately 1993 samples/second).
At that capture, 507 actors were at task 11 and five at task 10; those are lesson
positions, not frozen-model certifications. The live checkpoint continued to
advance independently of the unmodified saved evaluation policy.
