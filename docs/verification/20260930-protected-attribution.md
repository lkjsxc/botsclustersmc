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
