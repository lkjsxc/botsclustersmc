# Accepted learning-context coverage

This is process-local measurement of the data that actually entered an accepted
learner update. It is not a new reward, curriculum, loss weight, policy input,
replay buffer or competence score. The ordinary inference artifact is unchanged.

The [rejected station-entry fade](verification/20260930-station-entry-study.md)
showed why total transitions and completed episodes are insufficient descriptions
of training exposure. The candidate and control accepted similar sample totals,
but finished very different numbers of practice episodes. The old evidence did
not contain an accepted closed/open-menu split. These new counters cannot
retroactively fill that gap or identify which gradients caused the regression.

## What is counted

After `UpdateGuard` accepts a complete batch, each of its transitions contributes
one sample and its recorded elapsed actor ticks. Classification reads only the
**pre-action** observation: the existing task one-hot and `observation[42]`, where
`Sensors` encodes the menu ordinal divided by four. The next/bootstrap observation
is not counted. A transition that changes menus is attributed to its starting
menu, not fractionally split across the intervening ticks.

Queue/paused rejections, stale trajectories and guard-rejected updates contribute
nothing. Normal non-exam practice and full probes may contribute; frozen exams
never enter the learner. The measurement does not distinguish practice from probe,
assisted from unassisted starts, or completed from unfinished episodes. It counts
accepted samples, not offered transitions or every action the server executed.

Counts are unweighted. `TaskBalance` weights still determine the existing learning
loss, but are not applied to this exposure denominator. Elapsed ticks are summed
per actor transition; concurrent actors contribute separately. They are not unique
server ticks, wall-clock duration, complete episodes, successful crafts, visits
to a new state, or the number of independently trained policies.

## Status fields and interpretation

`./status.sh` / `status.cmd` and the raw status endpoint expose two fixed-length
arrays under `learner_context_samples_by_task_and_menu` and
`learner_context_ticks_by_task_and_menu`. They are JSON-array strings, following
the existing per-task status convention. Each has **114** elements, in task-major
order: 19 task buckets times six menu buckets. The cell index is `task * 6 + menu`.

| Menu index | Meaning | Exact observation value |
| --- | --- | --- |
| 0 | Closed | 0 |
| 1 | Personal inventory | 0.25 |
| 2 | Workbench | 0.5 |
| 3 | Furnace | 0.75 |
| 4 | Chest | 1 |
| 5 | Unknown / noncanonical | Not one of the five values above |

Task indices 0-17 are the existing task enum; task bucket 18 is unlabelled or
ambiguous, following `TaskBalance.task`. No approximately matching menu value is
rounded into a known bucket. Unknown contexts remain visible instead of silently
disappearing. The exported menu order, bucket counts and layout identify the
schema explicitly rather than requiring a consumer to assume it.

For wooden-pickaxe task 11, cells 66 through 71 describe closed, inventory,
workbench, furnace, chest and unknown pre-action states. A high closed proportion
can describe substantial closed-state exposure but not successful station entry.
A high workbench proportion may reflect slow actions or failures rather than good
assembly. Zero samples mean no accepted observation, not demonstrated inability.
Compare window deltas only within the same process and context-counter origin.

One immutable snapshot supplies both arrays, their totals, its policy update
identity, the context-counter origin and the existing
`learned_task_samples_this_process` marginal. These invariants hold within it:

```text
sum(context samples) = learner_context_samples
learner_context_samples + learner_context_base_trained_samples
    = learner_context_trained_samples
sum(context ticks) = learner_context_ticks
sum(the six menus for task t) = learned_task_samples_this_process[t]
```

`learner_context_policy_updates` identifies the accepted update represented by
that snapshot. Other top-level live policy fields may be read just before or after
another asynchronous publication; use the context fields' own identity rather
than requiring every independently read live metric to be simultaneous.

The scope is `accepted-pre-action-observations-this-process`. Counters start at
zero each time the learner starts, while `learner_context_base_trained_samples`
records the restored policy's cumulative sample count. Restarting measurement
therefore does not mean the learned model/optimizer/course was reset. These
counters are not checkpoint state, and no model schema or restore conversion is
introduced. Record process start and origin when retaining time-series evidence.

## Resource and noninterference boundary

`LearningContexts` is training-only pure Java. One learner thread publishes bounded
immutable snapshots; readers receive defensive copies of arrays. It retains only
fixed-size totals, not observations, actor identities, trajectories or additional
network snapshots. No Bukkit API, new tick-thread work, disk write, action RNG,
extra inference request or tensor-gradient operation is introduced. Status is
serialized through the existing background status writer.

Accounting validates a single accepted update and exact sample-counter advance
before publishing the next snapshot. Checked arithmetic prevents silently wrapped
counter identities. Tests cover every task/menu cell, noncanonical observations,
pre-action versus bootstrap states, elapsed-tick denominators, frozen snapshots,
rejected/stale/guard-rejected batches, and unchanged gradients and guarded policy
updates. Ordinary inference JAR separation remains independently checked.

The useful next learning result must still improve ordinary task completion while
retaining raw assembly and earlier skills under a predeclared matched comparison.
This instrumentation improves the description of exposure; it is not itself a
claim of better learned behavior or autonomous cooperative survival.
