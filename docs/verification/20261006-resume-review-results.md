# Resume-first review: completed comparison, candidate rejected

Date: 2026-10-06 (Japan). **Evidence only. No runtime or model adoption.**

This record closes the finite comparison prospectively declared in
[research PR #56](https://github.com/lkjsxc/botsclustersmc/pull/56), at source
`7f18c3b83dd16dede2ff7f2337b3ab84465fe027`. The
[pinned protocol](https://github.com/lkjsxc/botsclustersmc/blob/7f18c3b83dd16dede2ff7f2337b3ab84465fe027/docs/verification/20261004-resume-review.md)
was committed and source CI passed before gameplay. The complete numerical
receipt, report identities, accepted exposure, stopped-state identities and live
health observation are in [the machine-readable result](20261006-resume-review-results.json).

## Decision

Reject the candidate. Scheduling one earlier-task episode at checkpoint resume
successfully changed actual accepted exposure, but did **not** preserve ordinary
wooden-pickaxe crafting. Candidate scores were 18/32 and 19/32, versus the control's
25/32 and 25/32. Both arms failed the declared 26/32 retention floor on both seeds.
Neither acquired ordinary mining: all six parent/post-training reports scored 0/32
on task 12, with zero target pick contact and zero broken blocks.

The control also did **not** reproduce the prespecified loss of at least 8/32 on
each seed: its losses were 4/32 and 2/32. Thus the full review-retention support
criterion failed independently of the candidate's retention failure. This run is
not evidence that review in general is harmful, or that a different review amount
would repair retention. It rejects this particular intervention under this
particular matched training condition. No additional segment was started.

## What was actually compared

Both arms used the previously rejected stronger mining-control reward and task-12
critic-feature detachment. **The control was not unchanged accepted-main training.**
The sole between-arm runtime-source difference was checkpoint-decode allocation:
`Course.java` and `ReviewEffort.java`. Candidate restored each non-foundation actor
with one repayable review credit instead of zero. Fresh starts, promotions,
regressions, long-run 4:1 observed-tick accounting, observations, physical controls,
rewards, gradients, initial model/Adam/course bytes and frozen evaluator were held
as declared. No scripted gameplay, reset assistance or teacher action was added.

Accepted main at entry was `ec2e30115edc14695152c3847385a90314126366`.
The common complete saved parent had 1,182,364 updates and 344,488,915 accepted
samples; it was a preserved qualified checkpoint, **not a snapshot of the then-current
1.4-billion-sample live learner**. Control source was
`ddcf2f6d24bfa0bbb3a14d3665be2e629eeb8602`.

| Input or runtime | SHA-256 |
| --- | --- |
| Common complete parent model/Adam/course | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Common initial policy | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control training and every frozen evaluator | `fef1ef4d73ed320392ab30322afe2233a765e3c4e74e7bb48fc458d3362fd3cd` |
| Candidate training | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Byte-identical inference artifacts | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

Only `Course.class`, `Course$Agent.class` and `ReviewEffort.class` differed between
the training artifacts. Native export and verify-export established the same
original counters and policy from both complete initial states.

## Complete frozen results

There were exactly six complete ordinary reports and **2,496 frozen trial
executions**, with no evaluation learning. Each report used ordered tasks 0-12,
32 cases per task, and `reset_intervention=none`. Both fresh parent reports were
completed and qualified before either learning process started.

Columns ending in 01/02 use seeds 2026100401/2026100402. Every entry is successes
out of 32, not a percentage or a certificate carried over from training.

| Task | Parent 01 | Control 01 | Candidate 01 | Parent 02 | Control 02 | Candidate 02 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 32 | 32 | 32 | 32 | 32 | 32 |
| 1 | 32 | 32 | 32 | 32 | 32 | 32 |
| 2 | 32 | 32 | 32 | 32 | 32 | 32 |
| 3 | 32 | 32 | 32 | 32 | 32 | 32 |
| 4 | 32 | 32 | 32 | 32 | 32 | 32 |
| 5 | 32 | 32 | 32 | 32 | 32 | 32 |
| 6 | 32 | 32 | 32 | 32 | 32 | 32 |
| 7 | 32 | 32 | 32 | 32 | 32 | 31 |
| 8 | 32 | 32 | 32 | 32 | 32 | 32 |
| 9 | 32 | 32 | 32 | 32 | 32 | 32 |
| 10 | 32 | 32 | 31 | 32 | 32 | 32 |
| 11: wooden pickaxe | 29 | 25 | 18 | 27 | 25 | 19 |
| 12: ordinary mining | 0 | 0 | 0 | 0 | 0 | 0 |

All task 0-10 scores retained their per-seed floors of 29/32. Task 11 failed its
26/32 floor in both arms. The candidate's losses from the parent were 11/32 and
8/32. The mining acquisition criterion (candidate at least 8/32 and at least 4/32
above control on each seed) also failed. No task or seed was omitted or averaged
away. Two evaluation seeds are not independent learning replicas: there was one
training trajectory per arm.

## Accepted exposure and finite budgets

Each owned Academy resumed once, used 512 actors and learning seed 7, and stopped
at the declared +250,000 accepted-sample boundary with an allowed +50,000 flush
overshoot. Both processes stopped cleanly; no larger-budget continuation ran.

| Measurement | Control | Candidate |
| --- | ---: | ---: |
| Initial restored actors assigned earlier tasks | 0 | 512 |
| First observed accepted review boundary | None before stop | 343 samples |
| First observed boundary at or above 50,000 | 52,224 | 53,896 |
| Earlier-task accepted samples at that boundary | 0 | 8,968 |
| Frontier task-12 samples at that boundary | 52,224 | 44,928 |
| Earlier-task accepted samples at last pre-stop observation | 0 | 9,384 |
| Total accepted contexts at last pre-stop observation | 254,976 | 256,232 |
| Exact additional samples in stopped checkpoint | 261,792 | 260,104 |
| Exact stopped updates | 1,182,991 | 1,183,156 |
| Preserved fresh status observations | 28 | 28 |

At the candidate's 53,896-sample boundary, accepted samples by task 0-12 were
`[609,713,768,898,1574,1000,1091,458,254,275,546,782,44928]`.
Every earlier task and the frontier had positive accepted data inside the declared
50,000-75,000 window. Control remained frontier-only through its final observed
pre-stop context. Therefore the declared exposure contrast **passed**.

However, review was strongly front-loaded: its observed sample share fell from
8,968/53,896 (16.64%) to 9,384/256,232 (3.66%). Only 416 additional earlier-task
samples appeared between those boundaries, all on wooden-pickaxe task 11. The
final observed task-11 contribution was 1,198 samples (0.47% of accepted contexts).
The long-run 20% observed-tick rule is not a guarantee of 20% accepted examples in
this finite segment. Do not confuse initial assignment, actual actor ticks,
accepted examples, optimizer steps or retained competence.

The exact stopped sample difference was 1,688, within the original flush bounds.
Control made 627 additional optimizer updates and candidate 792. Changed example
lengths, batching, random draws and update paths are consequences of scheduling;
this was not a matched-update-count or matched-trajectory comparison. The 343-sample
observation is the first **observed** review boundary, not an exact first-gradient
timestamp. Context totals are pre-stop measurements, not the later flush totals.

## Mining diagnostics are not mining success

Each task-12 report contained 19,200 observations across 32 trials. Candidate tool
holding rose, but world-dig actions almost disappeared while inventory-open time
increased. These are correlated diagnostics, not an identified causal parameter.

| Seed/arm | Held-pick observations | Inventory-open observations | World-dig selections | Target pick contact | Broken blocks | Generic pickups |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 01 control | 709 | 11,876 | 289 | 0 | 0 | 9 |
| 01 candidate | 3,563 | 18,427 | 11 | 0 | 0 | 0 |
| 02 control | 666 | 11,900 | 282 | 0 | 0 | 24 |
| 02 candidate | 3,853 | 18,400 | 15 | 0 | 0 | 1 |

A held tool, an inventory interaction or a generic pickup does not show completed
cobblestone mining. Successful deployment mechanics also do not establish learned
cooperative survival: the deployment bodies are in-server NPCs, not connected
vanilla players.

## Verification and preserved evidence

Source CI run `37141083142`, attempt 1, passed Ubuntu source, Windows source and
observatory checks before gameplay. Live/retention/Paper/Windows-live workflow jobs
were skipped, not passed. Previously preserved full-source and native-parent
qualification was checked; the 29 controller tests were rerun successfully in
normal and optimized Python. A separate evidence auditor was built without
modifying the pinned research source. Its 22 distinct synthetic-evidence tests
passed in normal and optimized Python, including forged totals/seeds, duplicate
cases, missing trials, context regression, flush overshoot and favorable-but-false
acquisition claims. An initial helper invocation mishandled an expected
`FileExistsError`; the retained unittest harness corrected that fixture-only issue.

The independent auditor recounted all physical trial scores and seed/case identities,
checked frozen artifact and stopped-state bindings, reconstructed actual accepted
exposure, and reproduced every final decision boolean. Its SHA-256 was
`1c57078401c2ba9c4b3cf3f9731a5a539fcb8833d72fcf78feb77038805cdabf`.
The canonical outcome SHA-256 was
`daf51681dbfe6df25fb8620ff2e66fac37d7ae105283cbbe4a860ea01e4c4589`.

Raw evidence remains create-only beneath
`/home/coder/workspace/botsclustersmc-resume-review-20261004/.build/resume-review-study/`.
Both complete stopped model/Adam/course states and the common complete parent are
preserved. Their identities and all six raw-report SHA-256 values are in the JSON
receipt. A verified compact archive contains 163 members, including the qualified
inputs, runtimes, complete stopped states, reports, configurations, logs, status
histories and auditor. Disposable world-region files and generated caches remain
in their original directories rather than in the compact archive.

Archive: `.build/continuation-20261006/evidence.zip`, 9,411,217 bytes,
SHA-256 `356acd4082a08f9396c3413c8ee4464f3f342aa3d43bdd47f97a5ada59d90f01`.
Manifest SHA-256:
`0f4792e62b81fb26478be990a98069dac79d3a442bae0183060e4a2d5334269a`.

## Shared development state and next decision boundary

No experimental runtime or policy was installed on the shared Academy. PID
2351065 remained alive; all 512 actors progressed in the final status observation.
Accepted live samples increased from 1,418,606,807 to 1,419,981,322 during this work.
Inference failures/rejections, learner rejected/stale samples and retired actors
were all zero. The installed training JAR remained
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.
No service restart, state reset or rollback was needed. Both training ports and
all four evaluator ports were released, with no remaining process in an owned
experimental directory. These are live-health observations, not new skill claims.

Do not adopt this one-shot resume offset, continue either rejected state, increase
the reward again, or change seeds/floors to rescue the result. The useful next
question is about **sustained delivered rehearsal across frontier learning**, not
whether an initial lesson was assigned. Before another intervention, distinguish
its actual accepted task/menu exposure and optimizer schedule from earlier review
experiments; keep that one mechanism isolated and declare a new bounded comparison.
The inventory-to-world-dig transition is a separate observed mining bottleneck,
not a reason to claim that retention or acquisition has been repaired. This record
authorizes no additional gameplay run or runtime adoption.
