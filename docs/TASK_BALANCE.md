# Bounded task balance in the actor-learner

## Episodes are not equal amounts of experience

The former curriculum selected review by episode, while gradients consumed
transitions. A short successful motor review contributes fewer transitions than
a long unsuccessful resource trial. For illustration, 20 ten-decision review
episodes mixed with 80 three-hundred-decision trials would put only 0.83% of the
transitions in review, despite a 20% review-episode rate. This arithmetic example
is not a measured population statistic.

The current [review scheduler](REVIEW_EFFORT.md) budgets actual non-exam decision ticks,
addressing the supply imbalance separately from the within-batch loss objective.
It does not promise that exactly 20% of accepted samples belong to past tasks.

The learner now records actual accepted samples by task for the current process.
It also exposes the counts and loss weights in the latest batch. Neither repeated
loss weighting nor an exam is counted as new training experience. A synthetic
unlabelled-observation bucket is the final array entry; real sensor frames carry
one explicit task identity and should not contribute to that bucket.

## One allocation shared by every gradient worker

For each nonempty real batch, present tasks initially receive equal total loss
mass. A rare task's per-transition weight is capped at eight. The mass that cannot
be assigned under that cap is redistributed among the uncapped tasks. Absent
tasks receive no mass; no experience is fabricated or replayed to fill them in.
The sum of `task_count * task_weight` equals the actual number of transitions.

For a batch containing 500 transitions of one task and 12 of another, the rare
task gets weight 8, or 96 units of loss mass. The common task gets the remaining
416 units, or weight 0.832. The batch still records exactly 512 trained samples.
A batch with only one task has weight one and is unchanged.

The allocation is computed once for the complete batch, before splitting work.
Every gradient worker uses that same immutable allocation. Actor, critic and
regularization gradients receive the same state weight; V-trace targets, true
terminals, behavior probabilities and elapsed-tick discounts are unchanged.
Value-loss, entropy and importance summaries consequently describe the weighted
batch objective. The existing global norm clip and measured policy-change guard
still apply before publication. The bounded policy-change sample includes at
least one observation of every task present in the batch, so an upweighted rare
task is not silently skipped between evenly spaced samples. This remains a guard
on sampled states, not a bound on every possible observation.

Numerical tests verify the cap and conserved mass, defensive snapshots, identical
serial versus split-worker gradients under one allocation, and exact accepted
sample accounting after asynchronous learner shutdown. This is a change to the
multi-task loss objective, not a strictly on-policy claim or a mastery guarantee.

## What this does not solve

Weighting cannot restore tasks for which no examples arrive. It does not add
memory, demonstrate actions, alter curriculum promotion, relax exam conditions,
normalize every task's reward scale, or prove lifelong retention. Frozen-policy
trials remain necessary: the resource experiment learned log harvesting while
losing earlier aiming and stepping performance before task balancing was added.
Those regressions are retained rather than hidden behind historical certificates.

The general problem of competing task contributions is discussed in Hessel et al.,
*Multi-task Deep Reinforcement Learning with PopArt* (2019):
https://arxiv.org/abs/1809.04474
This implementation is not PopArt: it does not perform PopArt's value-target and
output-preserving normalization. Its capped within-batch loss allocation is the
specific mechanism described above, with separate empirical verification.
