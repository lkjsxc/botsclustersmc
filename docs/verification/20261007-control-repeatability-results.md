# Common-seed physical repeats: saved control states remain measurably different

Date: 2026-10-07 (Japan). **All 16 ordinary reports / 6,656 physical frozen trials
completed. No additional learning, policy replacement or runtime adoption.**

Research [PR #64](https://github.com/lkjsxc/botsclustersmc/pull/64) used prospective
source `fbb5162336917586c96ad7718695e43616237629`, tree
`a619476765328830b9e4a2721103f5bcb66a42c8`. Its
[fixed protocol](https://github.com/lkjsxc/botsclustersmc/blob/fbb5162336917586c96ad7718695e43616237629/docs/verification/20261007-control-repeatability-study.md)
and both independent analysis implementations were normally committed and pushed
before gameplay. Exact-source CI qualified that head first. The
[machine-readable evidence](20261007-control-repeatability-results.json) contains
the full primary decision, all per-task scores and report identities, repeat
outcome-flip vectors, post-hoc record comparisons, audit and archive identities.

## Decision

The previously reported discrepancy between the two saved controls is **not
explained solely by using different evaluation seed sets**. On common fresh seeds,
the later control outperformed the earlier control by **5/32 and 12/32** ordinary
wooden-pickaxe cases. These differences survived both fresh physical repeats;
the minimum later-control score exceeded the maximum earlier-control score by
those same margins on each seed, above the predeclared four-case screen.

All four models on both seeds had **zero task-11 outcome flips** between physical
repeats: 256 paired case outcomes agreed, not merely their aggregate counts.
This supports narrow repeatability of the measured crafting endpoint here, not
bit-identical physics or a universal determinism guarantee. Other task outcomes
and recorded physical diagnostics did vary, as reported below.

The later review candidate still has **no verified general retention benefit**.
On seed41 it scored 22/32 in both repeats, below its 26/32 floor and four below
its matched later control. On seed42 it scored 30/32 twice, one above its control
and above its 27/32 floor. It neither passes all fresh retention floors nor the
predeclared four-case advantage on all paired reports. All 16 mining completion
scores and all broken-block counts were zero. No model or research runtime is
adopted, and no additional learning segment or evaluation matrix followed.

This does not rewrite PR #62: that earlier six-report matrix passed its original
retention floors but did not reproduce the required control loss. Those original
observations and its unsupported benefit remain unchanged. This new case bank
adds a fresh candidate retention failure; it does not retroactively change the
old trials or make PR #60's earlier individual-coverage screen pass.

## Complete crafting endpoint

Each cell is successes out of 32 ordinary cases. R0/R1 are fresh physical runs
with the **same** seed, policy bytes and case identities, not different training
replicates. Every report also included all tasks 0-12 in order.

| Saved model | Seed41 R0 | Seed41 R1 | Seed42 R0 | Seed42 R1 |
| --- | ---: | ---: | ---: | ---: |
| Common parent | 30 | 30 | 31 | 31 |
| Earlier control (PR #58) | 21 | 21 | 17 | 17 |
| Later control (PR #62) | 26 | 26 | 29 | 29 |
| Later review candidate (PR #62) | 22 | 22 | 30 | 30 |

Here seed41/42 mean **2026100741 / 2026100742**. All four parent reports qualified.
Tasks 0-10 retained their per-report floors in every saved model. The earlier
control failed only task 11, with parent-relative losses nine and fourteen on the
two seeds; the later control passed all task floors, with losses four and two.
The review candidate lost eight cases on seed41 and one on seed42. Averaging
these seeds would conceal its two failed seed41 repeats and is not the decision.

The matched task 11 floors were `max(26,parent-4)`: 26/32 on seed41, 27/32 on seed42.
Earlier tasks used `max(28,parent-3)`. The earlier control's task 7 score 29/32 on
seed42/R0 exactly met its 29/32 floor; boundary passes are not exact preservation.

## Full ordered per-task score matrix

Vectors below are tasks **0 through 12**, each with denominator **32**. No task,
seed, model or repeat was omitted. The raw per-case outcomes and all physical
diagnostics are preserved in the verified archive, not replaced by these sums.

| Model | Seed | Repeat | Scores for tasks 0-12 |
| --- | ---: | ---: | --- |
| Common parent | 2026100741 | 0 | `32,32,32,32,32,32,32,32,32,32,32,30,0` |
| Common parent | 2026100741 | 1 | `32,32,32,32,32,32,32,31,32,32,32,30,0` |
| Common parent | 2026100742 | 0 | `32,32,32,32,32,32,32,32,32,32,32,31,0` |
| Common parent | 2026100742 | 1 | `32,32,32,32,32,32,32,32,32,32,32,31,0` |
| Earlier control (PR #58) | 2026100741 | 0 | `32,32,32,32,32,32,32,32,32,32,32,21,0` |
| Earlier control (PR #58) | 2026100741 | 1 | `32,32,32,32,32,32,32,31,32,32,32,21,0` |
| Earlier control (PR #58) | 2026100742 | 0 | `32,32,32,32,32,32,32,29,32,32,31,17,0` |
| Earlier control (PR #58) | 2026100742 | 1 | `32,32,32,32,32,32,32,30,32,32,31,17,0` |
| Later control (PR #62) | 2026100741 | 0 | `32,32,32,32,31,32,32,32,32,32,31,26,0` |
| Later control (PR #62) | 2026100741 | 1 | `32,32,32,32,31,32,32,32,32,32,32,26,0` |
| Later control (PR #62) | 2026100742 | 0 | `32,32,32,32,32,32,32,32,32,32,31,29,0` |
| Later control (PR #62) | 2026100742 | 1 | `32,32,32,32,32,32,32,32,32,32,31,29,0` |
| Later review candidate (PR #62) | 2026100741 | 0 | `32,32,31,32,32,32,32,32,32,32,32,22,0` |
| Later review candidate (PR #62) | 2026100741 | 1 | `32,32,31,32,32,32,32,32,32,32,32,22,0` |
| Later review candidate (PR #62) | 2026100742 | 0 | `32,32,31,32,32,32,32,32,32,32,31,30,0` |
| Later review candidate (PR #62) | 2026100742 | 1 | `32,32,31,32,32,32,32,32,32,32,31,30,0` |

## What repeatability does and does not show

The predeclared task 11 score-delta margin was at most three cases and the outcome-
flip margin at most four per policy/seed pair. Every pair had actual score delta
zero and zero flipped task 11 cases. Across **all 3,328 paired trials** of all
13 tasks, four binary outcomes changed: task 7 for the parent/seed41 and earlier
control on each seed, and task 10 for later-control/seed41. There was no missing
trial or changed trial seed behind those differences.

A post-hoc read-only comparison of the already completed raw reports found that
only **217/256** paired task 11 records were exactly equal across all recorded
fields. Thirty-nine differed in at least one recorded field despite identical
success/failure outcomes. For example, final distances, diagnostic aggregates and
occasionally elapsed ticks differed. These are comparisons of rounded serialized
fields, not complete hidden physical trajectories. No additional game or test
matrix was run for this secondary description, and no primary threshold changed.

A paired-case description of the two controls gives more detail than net scores.
On seed41, seven cases succeeded only for the later control in both repeats, two
only for the earlier control, nineteen for both and four for neither. On seed42,
twelve succeeded only for the later control, none only for the earlier, seventeen
for both and three for neither. No case was mixed across repeats in these
partitions. This is a property of these fixed case banks and selected saved
states, not an estimate of all future training runs.

The experiment identifies **saved-policy differences under shared evaluation
conditions**. It does not isolate which optimization event produced them. The
two controls had the same starting model/Adam/course and configured research
runtime, but different actual asynchronous trajectories and stopped budgets:
252,940 versus 254,537 added accepted samples, a 1,597-sample difference, and 858
versus 861 optimizer updates. Sampling order, actual context sequences, batching,
optimizer evolution and the small budget difference are not separated by these
frozen trials. Do not claim that scheduling latency alone caused the discrepancy
or that two selected controls estimate a population training variance.

## Immutable scope and physical execution

The common parent had 344,488,915 accepted samples. Saved policy identities were:

| Model | Policy SHA-256 |
| --- | --- |
| Parent | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Earlier control | `7b9f51d605c89d34cd13c3e3cbd4a36ec281afad62156962e2e6b49b3a974af1` |
| Later control | `d6543d1205b2507c23e15ffe844d4e11cf47656df70840bb185669cd29df433a` |
| Later review candidate | `57144e26e1af87fa286d7eaf037f928d4fa005e1251bc5155b55e6c87936ea22` |

Every evaluator used training JAR
`82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9`
and unchanged mainline frozen-exam sources. This was the historical matched
research evaluator, not installation of rejected learning changes. Both control
trajectories used the prior stronger mining reward, task 12 critic-feature
detachment and review-first resume; the later candidate added review admission.
These are not ordinary-mainline learning trajectories or the moving shared policy.

The prospective source changed only orchestration, integrity tests and protocol.
Core, plugin, training, host, frozen-exam implementation, acceptance harness and
workflows were unchanged. Each physical report used a new disposable localhost
server with ordinary `reset_intervention=none`, 416 in-server NPC trial bodies,
the existing pinned Folia cache and explicit stored EULA consent. They were not
connected vanilla players or autonomous survival trials. Ordinary reset supplies
and externally specified goals remain part of the established task definitions.

All four policies appeared in each of four waves. Ports 31710-31713 rotated so
each model used each slot exactly once. At most four evaluators ran concurrently,
with two advertised processors per JVM and the ten-GiB free-memory admission
check. The complete matrix finished normally, without operational timeout,
substituted seed, reused world, selected rerun or budget extension. All four
ports were checked free afterward. Input policy hashes matched before and after;
evaluated policy copies were unchanged and no training checkpoint was created.

## Mining remains an inventory-to-world tool-use problem

Each mining report had 19,200 observations and zero ordinary completion. The
following ranges are across the four reports for each saved model, not independent
learned replicas. Target contacts and broken blocks are sums across those reports.

| Model | Inventory-open share | Held-pick observations per report | World-dig choices per report | Total target-pick contacts | Broken blocks |
| --- | ---: | ---: | ---: | ---: | ---: |
| Common parent | 72.891%-73.380% | 543-635 | 763-801 | 1 | 0 |
| Earlier control (PR #58) | 49.776%-50.062% | 842-885 | 707-800 | 0 | 0 |
| Later control (PR #62) | 18.411%-19.714% | 316-374 | 865-984 | 0 | 0 |
| Later review candidate (PR #62) | 96.297%-96.479% | 924-1120 | 21-25 | 0 | 0 |

One parent trial on seed42/R1 had a fleeting target-pick contact observation;
all other reports had zero contact. It did not break a block or complete mining.
The later review candidate still spent more than 96% of mining observations in
its personal inventory, with only 21-25 effective world-dig choices per report.
The later control spent much less time there but still failed to mine. These
are frozen behavioral correlates, not proof that review causes inventory loops,
that menu closure alone solves mining, or authorization for scripted equipment,
GUI closing, pathfinding or teacher actions.

## Verification, preserved evidence and shared development

The controller's eighteen corruption/interpretation tests and the independently
implemented auditor's twelve corruption tests passed in both ordinary and
optimized Python. Full source suites passed before and at the committed head.
Exact prospective source CI **37521997768** passed Ubuntu, Windows and observatory
before gameplay; retention, live, Paper and Windows-live jobs were skipped,
not passed. This separate physical experiment is not mislabeled as those CI jobs.

The auditor imports neither the controller nor the frozen harness. It independently
recounted all physical identities and case seeds, booleans, per-task scores,
primary repeated-case flips, mining/menu observations, artifact bindings, complete
matrix and the entire decision. All **381,437** checks passed, with
byte-identical real-audit output under ordinary and optimized Python. The existing
frozen harness additionally validates the high-dimensional crafting probability
traces; that validation is not independently duplicated by this auditor.
Auditor SHA-256: `26bbc63d635d53259fc264dc634e97fdf598a5eda7224b0edc6eec7d8493a18c`.
Controller outcome SHA-256: `f5cccc9cfacdc25d31b1d235865a4c139e6553869325d6a93d591867fa96c9ae`.

Two supplementary inspection/progress-readback tool calls were blocked and not
retried; their boundaries are recorded separately. Neither was a required
physical evaluation or the primary auditor. No result is inferred from those
unavailable readbacks, and no execution restriction was modified. The required
immutable preflight, all sixteen physical reports and both full audits completed.

Raw evidence remains under
`/home/coder/workspace/botsclustersmc-control-repeatability-20261007/.build/control-repeatability-study/`
and `.build/qualification/`. The verified **210-member /
13,571,718-byte** archive contains all 16 reports, their metadata, actual
runtime/exam artifacts, logs, frozen inputs, source, audit and secondary comparison:
`complete-evidence.zip`, SHA-256
`42cf45041a749b4e921e4b525d010f9117b0f274633e2b126dd207ee3e1d637d`.
Manifest SHA-256:
`d1b762a9f2f4da475926fdef620a61cd7de1cf98cdbf0f88dfd2e3415bf3fd23`.
All members were reread and checked for size, SHA-256 and ZIP integrity. Disposable
world-region files and generated caches remain outside that compact selection in
their original owned directories; no operator world or failed evidence was deleted.

The shared development server kept accepted JAR
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.
Its separate accepted-sample counter rose from **1,501,696,425** to
**1,503,450,642** during the recorded health interval. All 512 shared
NPCs remained active and progressing, with zero observed inference failures/
rejections, learner rejected/stale samples and retired actors. No shared restart,
model replacement or learning reset occurred. These ongoing shared samples are
not samples from this frozen experiment, and operational health is not skill proof.

## Next engineering decision

Do not keep tuning a review percentage to obtain a desired failing control.
Use this result to require actual independent **learning trajectories**, shared
frozen case banks and explicit per-task retention/acquisition endpoints when
comparing learning changes. More evaluation seeds alone do not replicate learning.
A single-trajectory study may remain exploratory, but must not be promoted as a
general retention repair because one selected control happened to forget.

The next task-directed hypothesis should concern learned tool selection, inventory
exit and sustained world contact under ordinary accepted-mainline learning, with
separate controlled diagnostics where needed. Rejected stress rewards and critic
ablations remain useful historical evidence, not the default production baseline
for every future experiment. Keep the demonstrated review-data delivery, failed
individual review-cycle guarantee, new seed-sensitive candidate retention loss
and stable saved-control differences as separate facts. No action macro, automatic
equipment or unmeasured runtime adoption follows this diagnostic result.
