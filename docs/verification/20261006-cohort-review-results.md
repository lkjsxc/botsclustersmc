# Observed-only cohort credit: complete comparison, scheduler rejected

Date: 2026-10-06 (Japan). **Evidence only. No runtime or model adoption.**

This closes the prospective comparison in
[research PR #58](https://github.com/lkjsxc/botsclustersmc/pull/58), source
`79842719d0ab5f3cddf1775dea5ba72931c029dc`, tree
`e02838a31410371bb798e507bbb766e8d33646cb`. The
[fixed protocol](https://github.com/lkjsxc/botsclustersmc/blob/79842719d0ab5f3cddf1775dea5ba72931c029dc/docs/verification/20261006-cohort-review.md)
was committed, normally pushed and source-CI qualified before gameplay.
The [machine-readable result](20261006-cohort-review-results.json) retains all
scores, raw-report identities, disjoint exposure windows, native stopped-state
identities, post-hoc episode diagnosis and shared-development health.

## Decision

Reject this scheduler under the declared finite protocol. Candidate wooden-pickaxe
scores were **25/32 and 27/32**, compared with control **21/32 and 19/32** and the
common parent **30/32 and 31/32**. Candidate missed the first seed's 26/32 floor;
it exactly met the second seed's 27/32 floor. Both control results failed their
floors. Tasks 0-10 retained their per-seed floors in both arms. Ordinary mining
remained **0/32 in every report**, with zero target pick contact and broken blocks.

Unlike the preceding resume-first comparison, the prescribed control crafting
loss of at least 8/32 on each seed did reproduce (losses 9 and 12). However, the
candidate did **not** deliver the intended sustained accepted-review contrast.
Its late review share was only 0.202%, against control 0.033%, far below the
required five-percentage-point advantage and the per-window review floors.
Consequently the conjunction supporting retained skills through sustained review
failed, independently of the first-seed retention failure.

The nominal +4/+8 crafting advantage is not hidden, but it is not a verified
retention repair or evidence that sustained rehearsal caused an improvement.
There was one learner trajectory per arm; two evaluation seeds are not independent
learning replicas. Both exposure and optimization trajectories changed slightly.
No budget extension, seed substitution, threshold relaxation or runtime adoption
followed the result.

## What changed, and what did not

Both arms started from the same complete saved parent (1,182,364 updates /
344,488,915 accepted samples). Both used the previous rejected stronger mining
reward, task-12 critic-feature detachment and one-credit review-first resume.
**The control was the previous resume-first research runtime, not ordinary
accepted-main training or the current moving live policy.**

The only between-arm runtime-source changes were `Course.java` and
`ReviewEffort.java`: after each actor's identical first issued training lesson,
candidate used the sum of actually observed local credits of current members at
the same frontier to decide whether its next lesson could be review. Each actor's
own least-observed-time earlier-task choice was preserved. No forecast
reservations, fixed reviewer population, early episode interruption, action
teacher, reward, gradient, task balancing, reset, terminal predicate or model/
optimizer initialization change was added between arms. Exams and individual
earned progress were unchanged.

The [older reserved-cohort experiment](20260928-reserved-review-study.md) already
supplied roughly 20% sustained review but failed its final fresh-seed crafting
criterion. It used a different earlier parent, forecast reservations, a cohort
review-task selector and a much longer interrupted budget. This experiment tested
a distinct actual-only credit mechanism; it does not erase that earlier negative
result or establish that sustained rehearsal itself is generally ineffective.

| Pinned identity | SHA-256 or source |
| --- | --- |
| Accepted main at entry | `4bc3ac58b2b2ff365483f5087070fea03bfdb223` |
| Matched control source | `7f18c3b83dd16dede2ff7f2337b3ab84465fe027` |
| Complete common model/Adam/course | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial inference policy | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control training and every frozen evaluator | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Candidate training | `666ef5f6b797a2b41048481fdc871a70f746689bcc6df680b0cf2a3dd657301b` |
| Byte-identical inference artifacts | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The observed seven-class training payload difference was explicitly pinned before
play, including four nested records whose declarations were unchanged but compiled
bytes differed. No independently unverified debug-only equivalence was claimed.
Both actual-parent native checks produced identical first-lesson and post-first-
lesson course hashes even when earlier actors recorded synthetic work before
later actors issued their first lesson. These calls produced no learner samples.

## Complete frozen matrix

Both parent evaluations qualified before either learner started. Each report used
ordinary ordered tasks 0-12, 32 cases each, and `reset_intervention=none`. All six
reports / **2,496 frozen trial executions** completed; no evaluation trained.
Columns 11/12 below denote seeds **2026100611 / 2026100612**, not task IDs.

| Task | Parent 11 | Control 11 | Candidate 11 | Parent 12 | Control 12 | Candidate 12 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 32 | 32 | 32 | 32 | 32 | 32 |
| 1 | 32 | 32 | 32 | 32 | 32 | 31 |
| 2 | 32 | 32 | 32 | 32 | 32 | 32 |
| 3 | 32 | 32 | 32 | 32 | 32 | 32 |
| 4 | 32 | 32 | 32 | 32 | 32 | 32 |
| 5 | 32 | 32 | 32 | 32 | 32 | 32 |
| 6 | 32 | 32 | 32 | 32 | 32 | 32 |
| 7 | 32 | 30 | 31 | 31 | 31 | 32 |
| 8 | 32 | 32 | 32 | 32 | 32 | 32 |
| 9 | 32 | 32 | 32 | 32 | 32 | 32 |
| 10 | 32 | 32 | 32 | 32 | 32 | 32 |
| 11: wooden pickaxe | 30 | 21 | 25 | 31 | 19 | 27 |
| 12: ordinary mining | 0 | 0 | 0 | 0 | 0 | 0 |

All earlier-task scores passed `max(28, parent - 3)` separately. The crafting
floors were `max(26, parent - 4)`: 26/32 and 27/32. Neither cross-seed averaging nor
the candidate's nominal advantage replaces its failed first floor. The separate
mining acquisition criterion (candidate >=8/32 and >=control+4 on both seeds)
failed as well.

## Actual accepted exposure, not assigned labels

Both initial 512-actor distributions matched exactly:
`[42,42,42,37,40,53,53,38,43,38,42,42,0,0,0,0,0,0]`.
Every task 0-12 appeared in accepted data by the first 50,000-sample boundary.
All five observed boundaries were within the declared +25,000 observation
overshoot, so missing observations did not cause the failed exposure screen.

The table reports earlier-task shares in **disjoint actual observed windows**,
not overlapping cumulative means. The candidate requirement was 5%-40% review
and at least 50% frontier in each window.

| Nominal window end | Control observed endpoint | Control review share | Candidate observed endpoint | Candidate review share |
| --- | ---: | ---: | ---: | ---: |
| 50,000 | 56,380 | 16.40% | 56,385 | 16.23% |
| 100,000 | 107,644 | 0.19% | 107,233 | 0.19% |
| 150,000 | 158,860 | 0.09% | 158,647 | 0.23% |
| 200,000 | 200,396 | 0.00% | 200,215 | 0.15% |
| 250,000 | 251,308 | 0.00% | 251,364 | 0.21% |

In the late observed 100k-to-250k interval, control accepted 48 earlier-task
samples out of 143,664; candidate accepted 291 out of 144,131. Candidate counts
by earlier task 0-11 were `[21,14,13,31,42,16,20,48,13,16,22,35]`: **none** reached
the declared 100-sample floor. The candidate's approximately 0.168-percentage-
point late advantage was not the required five points. Both methods remained
front-loaded under this finite budget.

The first observed review boundaries were control 1,176 and candidate 989
accepted samples. These are status-resolution observations, not exact first-
gradient timestamps. The final observed task-11 counts were control 765 and
candidate 746; the nominal crafting-score difference cannot be presented as a
response to more accepted task-11 rehearsal.

| Exact stopped measurement | Control | Candidate |
| --- | ---: | ---: |
| Additional accepted samples | 252,940 | 256,036 |
| Additional optimizer updates | 858 | 872 |
| Total stopped samples | 344,741,855 | 344,744,951 |
| Total stopped updates | 1,183,222 | 1,183,236 |
| Preserved fresh status rows | 27 | 27 |

Both stopped cleanly inside the +250,000 target / +50,000 flush tolerance. Their
3,096-sample difference and different update counts are reported, not treated as
identical optimization paths. Pre-stop context totals and later native stopped
counters remain distinct. No additional segment ran.

## Post-hoc diagnosis: no completed frontier episode

A read-only native comparison of both exact stopped checkpoints with the original
complete parent found **zero newly completed frontier training episodes in both
arms, for all 512 actors**. Frontier probes completed: zero. New exams, passed
exams and regressions: zero. Every actor still had frontier 12. All completed
Course episode deltas were 512 for control and 525 for candidate; with no exams
or completed frontier episodes, the candidate added only 13 completed review
episodes beyond the common initial round.

Task 12 has a 3,000-tick episode horizon, and the scheduler selects a new lesson
only when an actor leaves its current episode. Shared credit alone cannot offer a
new lesson to an actor already inside a long frontier episode. The measured
opportunity shortage explains why a positive credit balance is not sufficient to
supply the intended early-window contrast. It does not identify the gradient term
responsible for forgetting or prove that another scheduler will retain crafting.

The synthetic allocation test used a 5,000-tick early observation and 60,000-tick
long run. Those tests correctly checked accounting and long-run behavior but did
not establish feasibility in this short physical segment. Future software
qualification must explicitly model initial phase alignment, available lesson
completion opportunities and the declared early sample budget. A long-run 20%
accounting identity must not stand in for that short-window check. This diagnostic
was performed after the run, did not change the predeclared gates, and started no
additional learning or gameplay.

## Mining diagnostics

Each task-12 report had 19,200 observations across 32 trials. All reports had zero
target pick contact, zero maximum target pick ticks and zero broken blocks.
World-dig choices and generic pickups are therefore not mining success.

| Seed/arm | Held-pick observations | Inventory-open observations | World-dig choices | Generic pickups |
| --- | ---: | ---: | ---: | ---: |
| 11 control | 908 | 9,695 | 676 | 9 |
| 11 candidate | 546 | 13,047 | 826 | 23 |
| 12 control | 856 | 9,645 | 677 | 19 |
| 12 candidate | 441 | 13,011 | 840 | 19 |

These are frozen diagnostic correlates, not claims that the bots acquired
cooperative survival. The deployment bodies remain in-server NPCs, not connected
vanilla players.

## Verification, archive and shared development

Exact-source CI `37448881355`, attempt 1, passed Ubuntu, Windows and observatory
before gameplay. Four optional live/retention/Paper/Windows-live workflow jobs
were skipped, not passed. The full local source suite was rerun at the committed
head. Cohort unit checks: 111,126; real-parent native checks: 10,270 per arm.
The 21 new and 29 inherited controller tests passed in separate normal/optimized
Python runs. The independent auditor's eight synthetic tests passed in both modes.

The independent auditor recounted every physical trial identity, seed and success,
mining diagnostics, 114-cell task/menu counters, disjoint windows, stopped-state
bindings and final decision. It reproduced the entire controller outcome. Auditor
SHA-256: `f9107391c694eca340509196c601c3f7c24ad81771439b43a2a87c66c7cbb242`.
Outcome SHA-256: `f2887949fdbfe006ade9bff39e458efb4c71e2ce3900ab49bb9a8eb3785f9647`.

Raw evidence remains under
`/home/coder/workspace/botsclustersmc-cohort-review-20261006/.build/cohort-review-study/`;
qualification and post-hoc audit are under `.build/qualification/`. The complete
common parent and both rejected model/Adam/course states remain preserved. A
verified compact archive contains 177 members / 9,454,770 bytes, excluding only
disposable world-region files and generated caches that remain in the original
directories. Archive: `.build/qualification/complete-evidence.zip`, SHA-256
`6c618288a26bc5c8130ee83cc3dff8218bf1ce8e020cfc1fd69ab448e0042569`.
Manifest SHA-256:
`83d1fd61845d5e29cf87ef4215c978f5d80ef582f446d14db9747d2d25cf56d6`.

The shared Academy remained on its original accepted training JAR
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`
and PID 2351065. All 512 actors progressed; live accepted samples rose from
1,432,245,400 to 1,433,787,537 between the recorded health observations. Inference
failures/rejections, learner rejected/stale samples and retired actors were zero.
Both learners and all four evaluators stopped; ports 31681/31682/31690-31693 were
released with no remaining process in an owned experiment directory. No restart,
model replacement or learning reset occurred. This is live-health evidence, not a
new skill evaluation of the current moving shared policy.

The next design question is not another credit constant. It is how to preserve
actual early review opportunities while frontier episodes remain unfinished,
without silently changing gameplay actions or pretending a reservation is observed
time. Any new mechanism needs an explicit short-window feasibility test and a
new isolated, predeclared comparison. Do not extend these rejected states, relax
this result's floors, or claim that sustained rehearsal was tested successfully.
