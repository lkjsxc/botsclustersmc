# Accepted learning contexts: verification and activation

Date: 2026-09-30 (Japan). This is a measurement-only runtime change, not a new
learning experiment or a claim of ordinary pickaxe acquisition.

## Decision and boundary

Entry main is `8967d7690a6eafe1ca9d8018795432aadf51d1fd`. The completed
[station-entry fade](20260930-station-entry-study.md) was independently revalidated
and closed as PR #47, unmerged. Its result-only commit is
`0ee0b736b3cee07ec2c63542102bd905d2f4fea1`. Only its evidence document is carried
forward; its `StationPractice` intervention, controller and experimental models
are not adopted.

The accepted-context change reads pre-action task/menu observations only after an
update has been accepted. It records raw sample and elapsed actor-tick totals in
114 fixed cells. The existing per-task sample status becomes the marginal of the
same immutable snapshot. The snapshot includes its own policy identity and
restored-sample origin to avoid mixing independent live counter reads.

No core policy, distribution, action mask, plugin-world actuator, sensor, reward,
reset, course allocation, gradient formula, Adam update or checkpoint schema is
changed. The new observer uses no world API or gameplay RNG. Its snapshots are
not used to select examples, actions or weights. Detailed field meanings and
limitations are in [Learning contexts](../LEARNING_CONTEXTS.md).

## Engineering checks completed before integration

The focused Java suite first passed 410 checks for all task/menu buckets,
noncanonical values, pre-action versus next-state accounting, exact raw sample
and elapsed-tick totals, preserved input observations and source policy bytes,
snapshot immutability, checked overflow, invalid accounting rejection and explicit
unknown context handling. Tests use a synthetic warm sample-counter origin.

The real asynchronous learner fixtures verify accepted-only publication: a paused
queue rejection and a future-policy stale trajectory are excluded; a deliberately
pathological optimizer makes `UpdateGuard` reject its batch and contributes no
samples or ticks. A valid batch contributes exactly 32 samples and 128 actor ticks.
Repeated synthetic gradients and guarded policy updates are bit-identical before
and after observing the batch. These are numerical tests, not Minecraft skills.

A subsequent concurrent-reader fixture verifies sample arrays, tick arrays, task
marginals and counter identities remain paired during 128 snapshot publications.
Its number of repeated reader checks depends on scheduling. The earlier snapshot
stays unchanged, and the writer finishes without a leaked learner thread.

Both complete local source-suite runs passed, with the second including the
concurrent fixture. Logs are `.build/context-source-tests.log` and
`.build/context-final-source-tests.log` in the dedicated learning-context worktree;
the latter ends `CONTEXT_FINAL_SOURCE_TEST_EXIT 0`. The separate rejected-study
source suite also completed with `CLOSEOUT_SOURCE_TEST_EXIT 0`; its normal and
optimized Python controller regressions each passed 12 methods again. Source
suites compile real-API live fixtures but do not execute Minecraft lifecycle tests.

Artifact comparison against entry main:

| Artifact | SHA-256 |
| --- | --- |
| Unchanged inference JAR, both builds | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |
| Entry training JAR | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |
| Context-observing training JAR | `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f` |

Exactly five training-JAR members differ: `Learner.class`, `TrainingPlugin.class`,
the two new `LearningContexts` classes, and `Learner$MeasuredGradient.class`.
The last differs only in line-number debug metadata; `javap -p -c -s` output is
identical. An initial four-member expectation therefore failed and was investigated,
not silently counted as passing. `javap -v` localized that extra difference to
line 28 becoming line 27. All other members match; inference is byte-identical.

## Activation gate

Integrate only after source CI passes. Then build accepted main, stop only the
owned development training service, preserve its final canonical checkpoint and
resume that complete compatible model/optimizer/course state. Do not install an
experimental station-entry checkpoint or add a state conversion. Keep the current
pinned server and unchanged monitor/evaluator paths. New context counters start
at zero with the retained cumulative sample origin; this is not a learning reset.

Verify the installed runtime identity, restored counters, fresh live learning,
all-actor progress, failure counters and exact context/status marginals. Retain
multiple process-local observations, distinguishing observed exposure from frozen
competence. The completed activation and source identities are recorded below
only after those actions have actually succeeded.

## Completed integration and live activation

Implementation and initial verification commit:
`0486fbd46208aecb9000d3a60c8e00982a71cdc5`, tree
`62e3ba3d3a2a688764b44210e53761630c5b03ec`. GitHub run `36672524303`
passed Ubuntu source, Windows source and observatory checks. Dispatch-only live,
Paper, retention and Windows-live jobs were skipped, not counted as executed.
PR #48 merged as `d2180141c0543a7b8e5386dd18425c19fcf729fb`, with the same
tree; an independent GitHub ref read and clean production fast-forward confirmed
that accepted source before building and activating it. Source commits were
authored as `lkjsxc` and pushed normally.

At 14:19 JST on September 30, only `botsclustersmc-training.service` was stopped
and restarted. The previous supervised process was 121202 (Minecraft child
121289); the new supervised process is 2350978. The monitor remained active and
unchanged. The installed training JAR matched `31e74a15...` above. Server version,
service limits, configurations and Academy paths were unchanged.

The canonical shutdown checkpoint was retained at
`.build/context-activation-20260930/stopped-training.bcmc` before restart. Native
export and `verify-export` passed. It contained 1,182,364 updates and 344,488,915
samples; its SHA-256 is
`a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e`.
The startup journal explicitly reported restored model/optimizer updates
1,182,364, samples 344,488,915 and optimizer step 1,182,364. All 512 actors were
observed with restored-checkpoint startup state and first-issued task 12.
No model/optimizer/course reset or data conversion occurred; the new context
counters began at zero with that cumulative sample origin. NPC bodies remain
ephemeral and are recreated by normal service startup.

Three fresh retained status snapshots covered 20.001 seconds:

| Snapshot | Cumulative trained samples | Context samples since restart | Accepted actor ticks |
| --- | ---: | ---: | ---: |
| First | 344,637,235 | 148,320 | 741,600 |
| Second | 344,658,899 | 169,984 | 849,920 |
| Third | 344,680,563 | 191,648 | 958,240 |

Across the window, 43,328 new samples and 216,640 actor ticks were accepted.
Every snapshot had 512 active and progressing actors; inference failures and
rejections, stale/rejected learner samples and retired actors were zero. The
full 114-cell sample/tick arrays, exact counter-origin binding and all 19 existing
per-task marginals passed the finite read-only activation audit. All unknown-menu
counts were zero. These observations establish live instrumentation and learning
health, not current skill mastery or a performance improvement benchmark.

All first-window accepted samples were task 12. In the third snapshot, its closed
and personal-inventory sample counts were 52,120 and 139,528 respectively; the
other menu buckets were zero. A later separately retained snapshot contained
1,555,148 accepted samples: 1,231,132 for task 12 and positive counts for every
earlier task, including 54,143 for task 11. Its task-12 split was 335,254 closed
and 895,878 personal-inventory samples. The early lack of review exposure was
therefore temporary, not evidence of disabled review. These changing-policy
exposure measurements are separate from a fixed-policy assessment.

The observed course advancement prompted the separately predeclared
[current stopped-policy qualification](20260930-current-policy-qualification.md).
Its 832 ordinary frozen trials found wooden-pickaxe crafting 31/32 on both seeds
and cobblestone mining 0/32 on both, with tasks 0-10 each at least 31/32. The
measured weights predate the observer installation: the new observer cannot be
credited for that learned result. The roadmap now follows this more recent
measured bottleneck rather than assuming the older source's entry deficit remains.

Raw activation receipts, status snapshots and complete stopped state remain in
`.build/context-activation-20260930/`. The qualified current-policy record also
identifies the unpublished research archive that preserves them. Subsequent
result-record edits change documentation only and do not require another restart.
