# Protected active-policy attribution and a current mainline snapshot

Date: 2026-09-30 (Japan). The diagnostic protocol below is declared before its
new Minecraft executions. The current-main snapshot section records an independent
observation already completed before declaration; it is not a matched learning arm.

## Question

The prior [matched click-slot experiment](20260930-click-slots-comparison.md)
lost useful partial placement within task 11 while protecting other task IDs.
Use its exact initial and stopped candidate to distinguish sensitivity to the
active representation from sensitivity to the final action projections. Do not
continue training, change the rejected protocol, or substitute a newer model.

## Qualified artifact boundary

`PolicyBlocks.compose` still rejects focused policies. The separate
`composeProtected`/`runProtected` entry points require the same protected active
task, identical immutable anchor weights (including signed zero), identical anchor
counters, and nondecreasing active counters. Composition retains the actual base
anchor and routing. Validation happens before any output directory is reserved.
All 16 block mixtures and both exact inputs are retained with a create-only
manifest. Every mixture inherits base counters; none is newly trained.

Independent tensor-index checks cover every parameter and all 16 masks, exact
serialization, source immutability, mixed active/frozen scalar and batch routing,
critic-to-actor independence at inference, malformed pairs and file boundaries.
The diagnostic remains absent from both runtime JARs. There is no production
model, reward, observation, control, training or reset implementation change.

The experiment runner pins the two canonical checkpoints, their policy exports
and the runtime. Native `verify-export` binds each input to its checkpoint. It
checks strict metadata, complete condition coverage and immutable policy bytes,
and retains unsuccessful trials and operational failures rather than promoting
partial results. CLI help and invocation without `--run` start nothing. The
explicit entry point runs its offline regressions before creating study state.

| Input | SHA-256 |
| --- | --- |
| Initial policy | `03d06a4a5d0b81abc687158ceb1e9325b151621b6712ae85e694452a3a9fe96e` |
| Stopped policy | `6e42a9bd398cea4ac147359e315d1ea6ce9b60036e3bb45ee8a0dcda1b7b5576` |
| Initial checkpoint | `36e668ddc1fae904a95a19c5c7e9fcedd8de190f65d827c2ef239f4f6596024f` |
| Stopped checkpoint | `ed4ba59ca19d32d932f7a6f5b927cbfa96db37aa2acb782e6aeca9c6a2df50f6` |
| Candidate training runtime | `fd35fa6408d5b56bc7ffd4b0822281732bae8ba1a367067232ca4378667f6741` |

## Prospective frozen comparison

The representation consists of the first-layer goal columns plus all remaining
first and second hidden-layer parameters (GOAL and TRUNK, bits 1 and 2). ACTOR
(bit 4) is every final action projection. All gameplay comparisons retain the
initial final value projection (CRITIC bit 8 unset). A critic-only output swap
cannot change the action probabilities in this implementation; tests check that
independently. This does not exclude critic-to-representation learning effects.

| Mask | Active representation | Action projection | Protected anchor |
| --- | --- | --- | --- |
| 0 | Initial | Initial | Exact initial anchor |
| 3 | Stopped | Initial | Exact initial anchor |
| 4 | Initial | Stopped | Exact initial anchor |
| 7 | Stopped | Stopped | Exact initial anchor |

Mask 0 must equal the exact initial policy bytes. Mask 7 has the stopped actor
function, not the stopped critic or training counters. All four are diagnostics,
including the endpoints. There is no optimizer or new training in any evaluator.

Use seeds 2026093002 and 2026093003, each with 32 cases per task. The ordinary
condition uses ordered tasks [10, 11]. Separately evaluate each one-missing-cell
condition: top left, top center, top right, upper handle and lower handle, each
on task 11 only. Assistance applies once at reset, never selects a later action,
and never pools with ordinary completion. No condition is dropped after looking
at its scores, including the previously zero handle conditions.

The full matrix is 48 reports and 1,792 trial executions. Up to four isolated
loopback evaluators run concurrently, ports 25592-25595, at most two visible CPU
processors per JVM. The live Academy stays running and unchanged. Each evaluator
has a 1,200-second operational cap; effective available memory must remain at
least 2 GiB when a job starts. Partial operational results are not a complete
sensitivity conclusion. The runner must finish the full matrix before classifying.

For each assisted cell, report all four representation contrasts (stopped minus
initial representation, at each actor background and seed) and all four actor
contrasts. Call a factor consistently degrading for that cell only when every
one of its four contrasts is at most -8 successes out of 32. This fixed screen
is not a confidence interval. A failed consistency screen does not prove absence
of an effect; report interactions and raw counts without pooling cells.

These counterfactuals test a parameter replacement, not gradient causality, which
examples caused forgetting, independent training replicates, generalization to
natural terrain, a deployable architecture, or a learned cooperative settlement.
A favorable hybrid remains diagnostic, not a newly trained replacement.

## Independent current-main snapshot, before attribution

The live main checkout was clean at `11dd8f51a9821ee025f335e41a0f7ef95e328597`.
A complete canonical checkpoint was copied without stopping the live learner,
then independently evaluated with its own mainline runtime. This is separate from
the older rejected candidate above and does not change that experiment's outcome.

The frozen model has 1,019,070 updates and 297,530,150 trained samples. Ordered
tasks 0-11 were evaluated with 32 cases each, first seed 2026093001, then the exact
same policy on confirmation seed 2026093004. Both 384-trial reports completed with
zero new learning and unchanged evaluated weights.

| Tasks | Seed 2026093001 | Seed 2026093004 |
| --- | ---: | ---: |
| Each task 0-10 | 32/32 | 32/32 |
| Task 11, ordinary wooden pickaxe | 1/32 | 2/32 |

This establishes rare ordinary completion in this snapshot, not stable acquisition
or a controlled benefit from a particular change. The second seed was chosen
before its run but after the first result was seen. Neither is an independent
training replicate. Full difficulty still uses the documented Academy rooms,
supplied targets/resources and invulnerability; it is not unassisted survival.
No reset-only intervention, teacher action, scripted recipe or output item was
supplied for these ordinary trials. Success uses the existing task predicate.

| Mainline artifact | SHA-256 |
| --- | --- |
| Canonical checkpoint | `294684242df33f8547406fe561a91be2b1fb0eff7c682e96dbb3038ce32280a1` |
| Frozen policy | `2c9297f28855c081ed0516933f01fdc6406f57b0409794cecbaffebc3cf34720` |
| Mainline training runtime | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |

Evidence is in `/home/coder/workspace/botsclustersmc/.build/` under
`frontier-snapshot-20260930` and `frontier-confirm-20260930`, with their logs.
The original copied canonical checkpoint, per-trial diagnostics, evaluator JARs,
policy, runtime and metadata are retained. Historical certificates and the live
changing-policy success counters are not the basis of the frozen result above.

## Secondary trace inspection of the current-main snapshot

This analysis was selected after the two complete snapshot reports, not declared
as a primary endpoint. It reads existing traces and performs no new simulation.

| Task 11 trace count, each out of 32 | Seed 2026093001 | Seed 2026093004 |
| --- | ---: | ---: |
| No observed workbench state | 24 | 26 |
| At least one workbench state | 8 | 6 |
| Target preview observed | 1 | 2 |
| Left the workbench with a partial recipe | 5 | 3 |
| Ordinary success | 1 | 2 |

All three successful trials observed all five correct cells and a target preview;
they finished after 326, 231 and 251 ticks. This is not evidence that unsuccessful
trials would succeed merely by opening the workbench. In the non-visiting subsets,
menu observation counts were [3146 closed, 11254 personal inventory] and [3447,
12153]; none was a workbench state. Of those subsets, 22/24 and 21/26 came within
three units of the supplied target at least once. Distance alone does not establish
correct aim, line of sight or a valid interaction. Do not reinterpret this
post-selected subset as a controlled opening intervention.

The current `StationPractice.initialMenu` gives station tasks an open station in
all sub-full-difficulty PRACTICE starts. Full probes/exams begin closed; probes
still supply training data, so this is not a claim that closed states are absent
from learning. It motivates the following independent, prospectively specified
comparison on the same current-main snapshot, not a change to the old candidate
attribution or its stopping rule.

## Additional current-main starting-state ladder, declared before execution

Use the same policy `2c9297f...` and main runtime `7e3df712...` above, not the old
click-slot candidate. Freeze tasks to **[11]**, 32 cases per condition, with new
seeds **2026093005** and **2026093006**. For each seed run every condition in the
ordered ladder `none`, `workbench-open`, `pickaxe-grid`. Keeping the task list
identical matters: the earlier tasks 0-11 reports use different actor IDs and must
not be substituted for this ladder's `none` condition.

The six reports total 192 trial executions. `none` starts closed with raw stock.
`workbench-open` changes only the initial menu. `pickaxe-grid` opens the menu and
arranges the five existing raw units, but supplies no completed item or cursor
assistance. These are existing diagnostic reset implementations, not new gameplay
code. No later move or click is supplied; closing remains possible. The last two
conditions remain explicitly assisted and ineligible for native certification.

Run only after all four attribution workers have ended. Use up to three loopback
evaluators at once on ports 25596-25598, two visible CPU processors per JVM, the
same prepared server cache, and a 1,200-second cap per evaluator. Preserve every
result, exact policy/runtime and assistance metadata. All six must complete with
unchanged policy bytes and zero training before interpreting the ladder.

On **each seed separately**, an increase of at least 8 successes/32 from `none` to
`workbench-open` is the entry-sensitivity screen. An increase of at least 8/32 from
`workbench-open` to `pickaxe-grid`, with the latter at least 24/32, is the
assembly-versus-collection screen. Neither is a confidence interval or a learning
benefit. A missed screen does not erase the observed entry failures or prove
absence of an effect. Report every raw count and both contrasts without selecting
a favorable seed. This is one fixed model, not independent training replications.
The planned result informs which start-state bridge to investigate; it does not
justify automatic station opening, recipe completion, a deployment substitution
or a claim of survival/cooperation.

## Execution results

No attribution gameplay result is claimed in its original declaration. The
starting-state ladder above was added after the first attribution seed completed,
but before any ladder trial. Append complete results and source identities below
without changing either protocol.

### Completed protected attribution

Implementation and original declaration: `d649e32be8862cd161988c2b5b432db339187050`,
tree `e0330b527a437c20b3dfa10cfdfe5394fa4aef6b`, research PR #45. All 48 reports
completed, totaling 1,792 trials, with unchanged evaluated policies and zero new
training samples. The complete predeclared matrix, including every zero-result
handle condition, finished normally; no later training was performed.

Each entry below is **seed 2026093002 / seed 2026093003**, each count out of 32.

| Condition | Mask 0: initial both | Mask 3: stopped representation | Mask 4: stopped actor | Mask 7: stopped both |
| --- | ---: | ---: | ---: | ---: |
| Ordinary task 10 | 24 / 31 | 24 / 31 | 24 / 31 | 24 / 31 |
| Ordinary task 11 | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Missing top left | 15 / 14 | 3 / 2 | 16 / 14 | 2 / 3 |
| Missing top center | 20 / 19 | 1 / 2 | 20 / 17 | 0 / 2 |
| Missing top right | 19 / 18 | 9 / 8 | 20 / 16 | 11 / 9 |
| Missing upper handle | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |
| Missing lower handle | 0 / 0 | 0 / 0 | 0 / 0 | 0 / 0 |

The top-left representation contrasts are [-12, -12, -14, -11] and the top-center
contrasts are [-19, -17, -20, -15]. Both meet the fixed all-background/all-seed
-8/32 screen. Top-right contrasts [-10, -10, -9, -7] are all adverse, but **do not
meet** that screen; its threshold was not changed to manufacture a passing result.
Actor contrasts for the three head positions range from -2 to +2; none meets the
consistent-degradation screen. The zero-baseline handle cells provide no evidence
of retained competence and are not pooled into the head-cell comparison.

For this old stopped candidate, replacing the representation is sufficient to
reproduce the large left/center loss under both actor projections. Retaining the
initial representation while using the stopped actor retains the measured head
placements. This localizes parameter sensitivity; it does not identify which
loss term, gradient, training example or optimizer state caused the change. It
also does not imply that every architecture with conditioned slot outputs fails.
All four mixtures still fail ordinary pickaxe completion. Keep this branch and
its hybrids as diagnostics; **do not merge or deploy the rejected runtime**.

### Completed current-main starting-state ladder

Its separate pre-execution declaration is
`2d54da8ef8d3de3ff13ca04f7eded38ad48cf755`, tree
`a6d6d0d2c28f3f1076fb1f2f43c9ff9132a28b4f`. The actual gameplay and verification
source remains main `11dd8f51a9821ee025f335e41a0f7ef95e328597`; that declaration
changes documentation only. The six reports completed after attribution ended,
with identical ordered tasks [11], 32 cases per condition, fixed policy 1,019,070
and 297,530,150 trained samples, unchanged policy bytes and zero new learning.

| Starting state | Seed 2026093005 | Seed 2026093006 |
| --- | ---: | ---: |
| Ordinary closed menu, raw ingredients | 1/32 | 0/32 |
| Workbench open, same raw ingredients | 30/32 | 31/32 |
| Workbench open, five raw units arranged | 31/32 | 32/32 |

The entry contrasts are **+29/32 and +31/32**, both exceeding the predeclared
+8/32 screen. The further assembly-bypass contrasts are only +1/32 and +1/32,
so the assembly-versus-collection screen is not met. The current model can
usually assemble and collect this recipe when it starts with the appropriate
interface open. Its ordinary entry/starting-state transfer is the immediate
bottleneck in this controlled setting. This is a result for the newer current-main
model, not a contradiction of the older candidate's missing-handle failures.

The open-menu condition is assistance, not ordinary mastery. This does not prove
that closing an incorrect personal inventory alone fixes the failure; aiming,
interaction choice and context switching remain possible contributors. It is not
a controlled learning improvement, natural-terrain competence or cooperation.

### Development decision

Prioritize a **learned station-entry bridge** from the current, qualified warm
checkpoint before adding more actor heads or repeating the old placement-only
curriculum. A concrete candidate is to fade initial open-station assistance as
practice progresses, while keeping the original complete-task predicate, raw
resource conservation and unchanged full-condition probes/exams. It must not
open a station automatically during ordinary gameplay or substitute an opening
subgoal for actually crafting the tool.

The next learning comparison must retain the current open-menu raw-assembly
function as well as tasks 0-10. Compare against an unchanged continuation of the
same canonical checkpoint. Bound accepted samples and report observed effort:
long failed closed-menu episodes can dominate ticks even when their episode
fraction is small. Neither the old attribution nor the new reset ladder proves
that a particular training mixture, value-gradient change or architecture will
improve ordinary completion. Those require their own predeclared learning test.

### Engineering checks and retained evidence

Both complete local Java/source suites passed: the research runtime and the
unchanged mainline runtime. The research block-counterfactual suite passed
8,792,924 checks. Its 12 offline controller tests passed under normal and optimized
Python. GitHub run **36641789762** passed Ubuntu source, Windows source and
observatory jobs on the implementation/declaration commit. The additional local
starting-state controller passed six offline completeness/type/no-start tests in
both Python modes and revalidated all six real reports through the original
mainline verifier. These test counts are not learned-gameplay achievements.

The combined work completed **56 reports / 2,752 frozen trial executions**:
1,792 old-candidate attribution trials, 768 current-main snapshot trials, and 192
current-main starting-state trials. Assisted conditions remain separate from
ordinary exams. The live Academy continued with 512 agents; no experimental
runtime or policy replaced it. Mainline runtime identity remains `7e3df712...`.

A whitelisted evidence archive has been stored as an **unpublished GitHub draft**,
`protected-attribution-20260930`, targeting the research source. Asset
**599494083**, `protected-attribution-evidence-20260930.zip`, is 14,017,696 bytes
with SHA-256 `0f092e8539e1b0eec8c189e3d56f22bfa17d081c4cbec0e5d87b160900152fb3`.
GitHub reports the same asset digest, and a fresh authenticated download matched
the local file byte-for-byte. The archive's 215 members passed CRC and per-member
size/digest verification before upload.

It retains both old candidate checkpoints and policies, all 16 mixtures, the
new current-main checkpoint/policy, the two repository-owned runtimes, all 56
complete raw reports and their metadata, receipts, original declarations,
verification logs and the actual local controllers. There are no Minecraft
server binaries, worlds, caches, credentials or unrelated project files. The
archive and draft are **not a deployable release** and certify no new skill.

Local originals remain in the main checkout's `.build/frontier-snapshot-20260930`,
`frontier-confirm-20260930` and `frontier-entry-20260930`, plus the research
worktree's `.build/protected-attribution`. Original canonical inputs, stopped
states and failed trials were not overwritten. The local archive is in the
research `.build/`; its separately downloaded comparison copy is in
`.build/retained-roundtrip/`.

For individual replay, use `tests/holdout.py` from the recorded matching source
and the matching archived policy/runtime. For example, from the unchanged
mainline source and an explicitly prepared local server cache:

```sh
EULA=true JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2 \
  python3 tests/holdout.py \
  --policy /absolute/evidence/inputs/main-policy.bcmc \
  --runtime /absolute/evidence/runtime/main-training.jar \
  --cache /absolute/prepared/server-cache \
  --output /absolute/new-disposable-exam \
  --tasks 11 --cases 32 --seed 2026093005 --port 25596 \
  --reset-intervention workbench-open
```

Preserve the original ordered tasks, seed, cases and reset label for every replay;
do not silently substitute the continually changing live model. The archived
local orchestration scripts document this execution and use explicit workspace
paths; they are not portable, unattended launch defaults.
