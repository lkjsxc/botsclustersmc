# Station reset study — 2026-09-27

## Plan fixed before the reset-intervention trials

This is a test-only diagnostic, not a learner intervention or a claim of improved
wooden-pickaxe skill. Source baseline is
`e45b8de2869be75005939a885de5c2d120c4b646`. Work uses the separate worktree
`/home/coder/workspace/botsclustersmc-station-study` in shared Home Coder workspace
`lkjsxc/tomato-ocelot-73`. The running Academy remains in the main checkout.

The native ordinary evaluator completed 384 full-condition trials, tasks 0–11 in
that order, 32 cases per task, seed 2026092703. Its saved policy is update 482209,
137210877 accepted samples, identity
`99e32f96a7017035fe4e9dc2e37b911b8ed90e817a308f28531bd081d83db3c6`.
The exact model, matching inference JAR and every trial are retained in
`.build/station-baseline.zip`. New evaluator training samples were zero.
Success counts in task order were 32, 32, 31, 32, 32, 32, 32, 32, 32, 32, 29, 0.
The three workbench failures and one aiming failure are retained, not discarded.

The next planned matrix uses ONLY that saved policy, never a changing canonical
checkpoint. Tasks are `[11]` in every matrix run; cases per task are 32. Case
seeds are 2026092704 and 2026092705. Each seed runs all three conditions below:

1. `none`: unchanged full reset, closed menu and raw supplies.
2. `workbench-open`: initial workbench open, unchanged raw stock and empty cursor.
3. `pickaxe-grid`: initial workbench open, its five ingredient units arranged at
   reset, empty cursor. The output is only a preview: no crafted or owned pickaxe
   is supplied. The fixed policy must select the actual output action.

The matrix has 192 trials, including all failures. Its accepted-training-sample
budget is **zero**. No adaptive stopping, best-seed selection or certificate
mutation is permitted. Each assisted condition must run the exact intervention
once per actor before any policy decision, mark `diagnostic_only=true`, identify
`reset_intervention` and report all 32 applied resets. Missing trials, mismatched
identities, changed weights, training state creation or malformed diagnostic
labels reject the run as software evidence. An operational failure must be
retained and explained separately rather than silently removed.

The recipe outcome, horizon, random pose, policy inputs, action masks and neural
actions after reset remain unchanged. No policy or action RNG is consumed by the
reset helper. Within the matrix, task order, case count and actor IDs are held
fixed, so reset seeds and initial action RNG specifications correspond. Real
asynchronous scheduling can still differ; these are not identical trajectories.
The 12-task native baseline assigns different actor IDs to task 11 and is NOT a
paired-randomness control for the task-only matrix.

Run at most two disposable loopback-only test servers together, each with a
2 GiB maximum heap and two JVM-visible processors, while leaving the production
learner and monitor running. No production weights, optimizer, course, rewards,
training resets or model architecture are changed. The full-condition results
remain the only competence measurements; assisted scores cannot replace them.
The standard native validator, export and replay paths reject assisted reports.

The study is descriptive failure localization. Even a large condition difference
would not, by itself, identify a particular neural architecture, reward scale or
optimizer as the cause. Any later training change needs its own separately owned
experiment and earlier-skill retention checks.

## Completed results

The native baseline completed at 19:25:31 JST on September 27. All six planned
matrix suites then completed, with the same saved weights and runtime:

| Initial condition | Seed 2026092704 | Seed 2026092705 | Interpretation |
| --- | ---: | ---: | --- |
| Ordinary full reset | 0/32 | 0/32 | Full-condition competence remains unestablished |
| Workbench initially open, raw stock | 2/32 | 0/32 | Opening assistance alone does not resolve the main failure |
| Workbench initially open, supplied ingredient grid | 32/32 | 32/32 | Collection works from this assisted state; NOT assembly mastery |

The ordinary controls ended at 19:29:48 and 19:29:56 JST; open-workbench runs at
19:33:44 and 19:33:54; supplied-grid runs at 19:33:58 and 19:34:08. Each suite
reported zero new training samples. The baseline plus matrix contains **576
completed real Minecraft trials**. All failures are retained. The 64 supplied-grid
successes are explicitly diagnostic and must not be pooled with the full-condition
success counts or shown as a learned-pickaxe certificate.

In the ordinary task-only controls, only 11/32 and 9/32 trials reached an observed
workbench state. The maximum-correct-cell histograms were `{0:27,1:5}` and
`{0:28,1:4}`: no control ever assembled two correct cells or reached a target
preview. All 64 timed out at 3001 ticks.

Opening the workbench initially exposed every trial to that menu, but most still
failed. Maximum-correct-cell histograms were `{0:12,1:13,2:3,3:2,5:2}` and
`{0:24,1:5,3:3}`. Only the two successful trials reached a target preview; their
median completion time was 51 ticks. The 62 failures timed out at 3001 ticks.
There were 13 and 5 observed partial-workbench exits respectively. These are
measurements, not proof that every exit is intrinsically an invalid action.

With the supplied grid, all 64 trials completed using actual sampled neural
controls, with a median of 6 ticks in each seed suite. All trials observed the
five-cell recipe and target preview; no chosen close action was recorded.
Reset validation established zero initially crafted/owned pickaxes. The ordinary
unchanged completion predicate, not preview visibility, determined success.

The evidence localizes an important gap to the **raw-stock-to-assembled-recipe
path**, not merely approaching/opening the workstation or clicking a visible
finished output. Entry remains unreliable too. Opening alone is not sufficient;
this does not establish that entry never matters. Nor does it establish the cause
inside the network or optimizer, or that the same behavior holds for every pose,
other material, future policy or stone-pickaxe task.

## Implementation and software verification

The plan was committed before the matrix as `11bdae8`. The diagnostic implementation
was normally pushed to main as
`63706657189271a46db221740f474d984d663459`, tested source tree
`1c4adabb0f0f863bd9b81f918ae2da7c7c9da791`.
It adds test-only `ResetIntervention`, owner-thread pre-decision application in
`FrozenPolicyExam`, explicit developer CLI options, diagnostic report/metadata
labels, invariants, regression tests and usage documentation. It does not add an
operator-facing assisted option to native `evaluate`.

Two full local `./test.sh` runs passed during implementation. The final run included
259 new reset assertions, all existing mathematical/mechanical/curriculum/
persistence/concurrency tests and 48 native report-integrity assertions (six new
rejections of these assistance labels). Four Python unittest methods with subtests
cover CLI restrictions, all report modes, missing/malformed assistance labels and
coverage, and rejection of assisted reports as ordinary controls.

[CI 36312854274](https://github.com/lkjsxc/botsclustersmc/actions/runs/36312854274)
completed its Linux source, Windows source and browser-observatory jobs successfully
for that exact implementation. Dispatch-only live, Windows-live and Paper-matrix
jobs were skipped, not counted as executed passes. The 576 Minecraft trials above
were performed independently in the shared Linux workspace against pinned Folia
1.21.11 build 14. Stone-pickaxe reset invariants were tested synthetically; this
study did not measure a learned stone-pickaxe policy.

Both locally built production JARs were byte-identical to the existing artifacts.
Training runtime identity remained
`3ad4de980018e5f98201eafa83c5cd4660ab9294723427bd4501c16b246697f2`;
inference identity remained
`1804e928f3e75ba870504379181171f18d1cea9aedb130ebb31da62900857c42`.
The evaluator JAR changes, intentionally; the policy and production runtime do not.
The native full-condition path and exact evaluated export were exercised by the
384-case baseline, and assisted reports remain excluded by the native validator.

## Operational exceptions and retained evidence

The first two shell batches completed their ordinary controls, then ended after
preparing the open-workbench condition. Those two preparation directories have
metadata and compiled files but no server log or trial report; no continuing
exam process was present. The interruption's cause was not established. Both
directories were preserved. The unchanged open-workbench case specifications were
restarted into separately named `-launch2` directories, with shell output redirected
to persistent logs. These are operational restarts before recorded gameplay, not
selection of a better-performing seed or deletion of failed policy trials.

Evidence remains in the study worktree, not in the live Academy:

```text
.build/station-baseline.zip
.build/station-baseline.log
.build/station-policy/policy.bcmc
.build/station-trials/2026092704-none/
.build/station-trials/2026092705-none/
.build/station-trials/2026092704-workbench-open/          # preserved incomplete preparation
.build/station-trials/2026092705-workbench-open/          # preserved incomplete preparation
.build/station-trials/2026092704-workbench-open-launch2/
.build/station-trials/2026092705-workbench-open-launch2/
.build/station-trials/2026092704-pickaxe-grid/
.build/station-trials/2026092705-pickaxe-grid/
.build/station-matrix-2026092704.log
.build/station-matrix-2026092705.log
.build/station-source-tests.log
.build/audit-station-study.py
.build/station-study-audit.json
```

The independent audit verifies the baseline ZIP, model/runtime identities across
all suites, 576 complete trial records with unique actor IDs within each suite,
task/seed association, summary agreement, nonzero elapsed time, diagnostic denominators and intervention
coverage, unchanged policy files, absence of training checkpoints and retention of
the two incomplete preparations. It also records exact histograms and probability
sums. These files are local evidence and would be lost if the workspace is deleted;
the committed source and this record alone do not contain the saved model bytes.

## Live continuity and next decision

At 19:34:21.968 JST, the untouched live learner reported policy 486960,
138628910 accepted samples, 512 active/ticking/progressing agents and zero
inference failures/rejections, retired or burning bodies, and rejected/stale
learner samples. All 512 agents were at stage 11; none had completed the course.
The five-second interval measured 2038.81 accepted samples/s and 0.582 CPU-core
equivalents out of eight JVM-visible processors. These are interval measurements,
not a sustained-performance claim. They are not results for the frozen policy
482209 tested above.

The main checkout was fast-forwarded without restarting training or the monitor.
Their service process IDs remained 266 and 261 with zero service restarts. No
weights, Adam state, course, world, ports, identity/authentication settings or
existing periodic evaluation timer were reset. The two diagnostic servers used at
most two JVM-visible processors and a 2 GiB heap each, and all matrix runs ended.
The standard observatory result is independent of these assisted local reports.

The next justified learning target is raw assembly and recovery from incomplete
recipes, while retaining earlier skills and the now-measured collection behavior.
A future learning experiment should separate the number/pattern of missing cells,
measure full raw-start transfer on held-out seeds, and compare against continued
unchanged training from the same canonical state before replacing the learner.
Forbidding menu exits, adding recipe-aware gameplay actions or relaxing success
gates would hide the problem rather than demonstrate learned control. This session
deliberately makes **no claim of improved full-condition wooden-pickaxe skill** or
open-world cooperative survival.
