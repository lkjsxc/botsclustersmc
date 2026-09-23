# Review allocated by observed training time

## Why episode percentages were misleading

An episode is not a fixed amount of experience. With 3,000 ticks per current-task
failure and 20 ticks per successful review, a 20% review-episode probability gives
only `0.2 * 20 / (0.8 * 3000 + 0.2 * 20)`, or about 0.166% of observed ticks to
review. This is an arithmetic example, not a measured Minecraft success rate.
The capped [task-loss balancer](TASK_BALANCE.md) cannot give weight to examples
that do not arrive. Curriculum exposure and within-batch loss allocation are
separate mechanisms, both of which need to be visible.

## The allocation rule

Each actor has a small, training-only `ReviewEffort` ledger. Above stage zero,
every observed tick on the current task adds one credit; every observed tick
reviewing an earlier task spends four credits. At the next episode boundary,
positive credit selects review; otherwise the actor practices its current task.
Review chooses the earlier task with the least accumulated review time in this
allocation interval, breaking ties with the actor's existing random generator.
No task beyond the actor's current stage can be selected.

For a fixed stage and no restart, the exact ledger identity is
`credit = current_task_ticks - 4 * review_ticks`. Whole episodes may overshoot
zero. If current episodes cost at most F observed ticks and review episodes at
most R, boundary credit lies between -4R and F; therefore review's fraction of
observed training ticks approaches 1/5 as exposure grows. Within review, choosing
a least-served task limits the difference in task exposure to one maximum review
episode. These are allocation properties, not a learned-policy guarantee.

The unit is the elapsed Minecraft ticks of a completed observation interval,
exactly the interval already recorded for the trajectory's discount. It is not
wall-clock time, a nominal four-tick substitute, completed-episode count, CPU
usage, accepted learner samples, or gradient mass. The ledger includes both
successful and unsuccessful work, and observed fragments of subsequently
interrupted episodes. It never creates or replays a transition.

Stage zero has no previous task, so its work is tracked separately. Frozen exam
intervals are also counted separately and never buy or consume training credit.
Promotion and regression reset the actor's allocation interval. Ordinary pause
and abandonment retain observed effort. A process restart begins a new interval:
debt and per-task review-time tallies are deliberately transient, like in-flight
world work, while the existing canonical model, Adam, RNG, course statistics and
earned certificates remain unchanged. No migration wrapper or second checkpoint
is introduced. Restart is not bitwise continuation of a live world trajectory.

## Probe cadence must not alias with scheduling

A frontier episode followed by four short reviews is a legitimate allocation
cycle. A single global "every fifth episode" probe counter can then always hit
the same task, starving another task of full-condition probes. Probe positions
are now per task: completed training counts 0, 5, 10, and so on select a probe.
An abandoned trial does not advance the completed count. Probes still train;
frozen individual exams are different and remain excluded from learning.

Readiness thresholds, the 16 current-task and four-per-previous-task exam cases,
14/16 and 3/4 passing requirements, resets, primitive actions, rewards and neural
observations are unchanged. The allocator chooses which existing lesson to reset
into; it never chooses a movement, target block, recipe, ingredient or menu click.

## Status and limits

The training status exposes `review_allocation: "observed-ticks"` and four
nonnegative process-local counters: `foundation_ticks_this_process`,
`frontier_ticks_this_process`, `review_ticks_this_process`, and
`exam_ticks_this_process`. Foundation means stage zero, where review is impossible.
For a settled cohort above stage zero, divide review ticks by frontier plus review
ticks to inspect exposure. New processes, changing stages and unfinished long
trials need not show exactly 20%. The accepted-sample and loss-weight metrics
remain separate and must not be relabelled as time allocation.

Short reviews require more resets and may lower samples per wall-clock second.
Earlier-task exposure also changes the input to the existing equal-mass,
cap-eight batch balancer: 80% current-task time does **not** mean 80% of its loss.
Queue rejection, staleness, irregular decision intervals and frozen exams further
separate actual learner sample proportions from the exposure budget. None of
these effects should be hidden by replacing sample counts with tick counts.

Before attributing retention or faster learning to this change, compare copied
canonical checkpoints under equal accepted-sample and separately equal wall-time
budgets, using fixed-policy full-difficulty evaluations on all reached tasks.
Report individual task results, resets, throughput and failures, not only a mean.
Do not replace the operator's live learning history with a synthetic course or
an older experimental checkpoint. This mechanism is not open-world survival,
cooperation, a new learned skill, or proof against catastrophic forgetting.

## Reproducible software checks

`./test.sh` (or `test.cmd`) runs `ReviewEffortTest` through the existing course
test entry point. It checks unequal and variable episode durations at five
frontiers, exact credit conservation, balanced review time, long integer
arithmetic, per-task probe cadence, invalid lesson identities, interrupted work,
restart semantics, failed exams, regression and concurrent actor accounting.
The allocation simulation deliberately contains no neural policy or Minecraft
world. Its passing results establish the stated software properties only.
