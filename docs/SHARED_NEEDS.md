# Public needs before scripted occupations

The direction is a settlement that keeps functioning, not a large collection of
individual task certificates. A useful intermediate question is: can agents learn
to keep an observable common need met, without a controller assigning professions,
recipes or action sequences?

`tests/needs` implements the **source-only semantics** of that question. It does
not supply an installed Minecraft sensor, learner, new model schema, autonomous
need generator or trained population. Neither public JAR packages these classes.
The [prospective qualification](verification/20261007-shared-needs-plan.md) is
independent of the unfinished physical gates in research PRs #66–#68.

## What every citizen would be allowed to know

An immutable `SharedNeed.Contract` specifies a public target vector for oak planks,
sticks and wooden pickaxes, a fixed episode identity, horizon and cohort size.
Positive targets are externally supplied. They are not inferred from a partner's
private pocket or a hidden planner. The finite bounds are 4,096 items per material,
12,000 room ticks and 512 members. These are software limits, not demonstrated
real-world scaling, an endorsed population size, or a demand-feasibility guarantee.

`SharedNeed.Frame` carries a boundary tick and either known common-bank counts or
an explicit unknown state. The separate 13-float `commons-demand-sidecar-v1` signal
contains demand presence, stock knowledge, remaining horizon, the three requested
counts, three observed counts, three proportional shortfalls, and current coverage.
Counts are normalized by 4,096. Unknown observations clear every stock-derived
feature, not the public request. Empty stock and unavailable information differ.
There is no member ID, occupation, partner inventory, next action or recipe order.

This is NOT an extension secretly spliced into the current 512-float observation.
A future live integration must explicitly revise the model schema and use fresh
compatible experimental inputs; do not relabel an old policy as demand-conditioned.
The demand sidecar alone also does not specify navigation to a chest or workbench.

## One common objective, one elapsed-time ledger

For each requested material, coverage is `min(bank / target, 1)`. The instantaneous
utility `u(t)` is the mean across requested materials, in `[0,1]`. Unrequested stock
and surplus do not improve it. The complete team score is:

```
score = sum(u(t), t = 0 .. H-1) / H
```

This measures **sampled public-stock availability**, not deposit/click counts,
crafting events, individual happiness, productivity or self-sufficient survival.
Ten deposit/withdraw cycles receive no extra event bonus over one deposit with the
same observed coverage-time. Stock held only at the final boundary gets no invented
history. A tool kept in a private pocket is not available in the common bank.
Borrowing a tool can reduce this narrow score even when the tool is useful: real
consumption, personal needs and productive tool use need further objectives and
physical evidence. Do not confuse this fixture with a complete communal economy.

A mean can conceal an entirely unsupplied material. The audit therefore also keeps
per-material coverage-time, fully covered/shortage ticks and the longest confirmed
consecutive shortage, plus ticks when **all** requested materials are covered.
These survive reward-ring eviction. An unknown interval breaks a confirmed shortage
run and prevents complete evidence; it is not quietly counted as a shortage or
as fulfillment. Future acquisition gates need component floors, not only the mean.

For an actor interval `[a,b)`, `NeedWindow.Claim` exposes both:

```
teamReward   = sum(gamma^(t-a) * u(t) / H, t = a .. b-1)
memberReward = teamReward / N
bootstrap    = gamma^(b-a), or 0 at the true H terminal
```

The member shares sum to one team return for complete temporal partitions, not N
copies of the outcome. The common reward and its accounting share are deliberately
separate fields. A future learner must choose and record its loss normalization;
this does not prove population-invariant optimizer behavior. It also does not
establish individual contribution: a passive member shares the team outcome.
The current learner's actual-tick discount can be matched with `gamma=0.997^(1/4)`.

Different actor decision schedules produce the same reconstructed discounted
return when claims are combined with their start-time discounts. Learning quality,
policy lag correction and accepted-sample balance are separate questions. As with
normal RL transitions, a fragment reward is discounted from its own starting tick;
simply adding discounted fragments without those factors is not equivalent.

## Time, ownership and incomplete evidence

The window is a **single-owner** object. A future adapter must sample a loaded,
owned public inventory on its owning scheduler and map actor observations to that
same room clock. `Frame(t)` labels `[t,t+1)`: these are tick-boundary samples, not
proof of every intra-tick event. The fixture does not read Bukkit objects or verify
physical conservation. Draft #68's independent material-accounting work remains
necessary before trusting actual bank stocks; an invented `Stock` is not evidence
that those items existed in Minecraft.

Every next frame must have exactly the next tick and the same contract. There is
no stale-frame interpolation, implicit episode crossing or dynamic renormalization
after member departure. Episode replacements require explicit new contracts and
trajectory boundaries. Memory is bounded by the retained tick capacity plus the
fixed cohort; no unbounded per-decision event log is accumulated.

Each member owns a cursor and sequence. A claim consumes its contiguous reward
interval exactly once. Missing history, unknown stock, future requests and replay
are rejected before changing a cursor. A slow member must explicitly discard an
unavailable prefix via `discardThrough`; the receipt records a discontinuity and
its discarded tick count. The caller must also terminate that actor's trajectory
fragment. The utility does not secretly fabricate a zero-reward transition.

`Audit.outcomeComplete` and `score` concern the complete observed **team outcome**.
Member cursors, claimed ticks and discarded ticks separately report delivery to
actors. A fully measured team outcome does not mean every learner received all
its samples, and a learner's dropped samples do not rewrite physical stock history.
Unknown intervals suppress the final score rather than presenting a partial score
as complete. Only immutable receipts and copied audit lists leave the owner.

## What to implement and establish next

First couple the public request to a deliberately new observation schema and a
real owner-thread bank snapshot, alongside the independent conservation checks.
Keep the same demand semantics for training and frozen evaluation. Do not silently
turn the old "craft a pickaxe" task into an unobserved banking objective. Qualify
real lifecycle, timing/termination and material preservation before learning.

Then predeclare independent fresh learning trajectories, paired shared/split/solo
conditions, full-duration evaluation and per-material floors. Compare a learned
shared-stock objective against a meaningful control, not only an untrained policy.
No reward, learner or rollout integration is adopted by the source-only tests here.

Partner replacement and unfamiliar teammates belong in later frozen population
tests: a fixed pair's success is insufficient for a resilient large settlement.
This evaluation motivation follows Leibo et al., *Scalable Evaluation of Multi-Agent
Reinforcement Learning with Melting Pot* (2021), https://arxiv.org/abs/2107.06857.
It is not a claim that this Java fixture implements Melting Pot or reproduces its
results. After genuine resource sharing, add replenishment without inventory
resets, explicit consumption and repair/replacement of members in persistent worlds.
The proposed long-term organizing signal is public need, not hard-coded jobs.
