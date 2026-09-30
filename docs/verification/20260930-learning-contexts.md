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
