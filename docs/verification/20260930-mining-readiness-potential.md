# Mining readiness potential: prospective matched warm continuation

Date: 2026-09-30 (Japan). This protocol is fixed before any new training or
Minecraft evaluation from the candidate. It follows the completed
[target-facing diagnosis](20260930-mining-target-facing.md), which found 0/32
ordinary and 0/32 target-facing mining on both fresh seeds. That result rejected
initial random facing as a sufficient explanation; it did not prove that aim is
irrelevant.

## Question and single learning intervention

Can a bounded state potential for **retaining a usable pickaxe outside menus and
making real progress on the designated target block** acquire ordinary
cobblestone mining while preserving the current model's useful skills?

The candidate changes only task-12 training potential. For a pre-action state:

- add **0.10** when the menu is closed and the selected held item is a wooden or
  stone pickaxe;
- add **0.15 × existing targetMining progress**, where targetMining is nonzero
  only while the runtime's actual mining state names the designated target block.

This is ordinary potential-based shaping through the existing reward expression,
`gamma * nextPotential - oldPotential`. Session potential is initialized after
the reset and supplied inventory are complete, so the supplied starting pickaxe
does not create a free initial reward. Opening a menu, switching away from the
pickaxe, losing target contact and later recovering those states cannot create
unbounded cycle reward: the potential is bounded to 0.25 and telescopes under the
existing discount/terminal handling.

The candidate does **not** change the task-12 success predicate, supplied resource,
target block, spawn, pose, action mask, observations, network, optimizer, V-trace,
action frequency, tool effectiveness, movement, look actions, inventory actions,
dig actuator, pickup provenance, lesson allocation or exam rules. It never chooses
a tool, closes a menu, aims, moves or digs for the policy. Full probes and frozen
evaluations remain unassisted.

The existing task-12 stone block requires sustained ordinary dig contact. A wooden
or stone pickaxe reduces the actuator's stone break duration to 40 ticks; the
existing targetMining diagnostic is normalized by 60 ticks and therefore need
not reach one before a pickaxe break. The shaping follows that existing measured
state and does not redefine physical completion.

## Exact warm source and runtime isolation

Both arms start from the same preserved complete canonical model/Adam/course state
captured before accepted-context observation was activated:

| Identity | Value |
| --- | --- |
| Accepted main/control source | `2e44e2e8e87b3200268d23b979c2b1e3e7800fb8` |
| Initial policy updates | **1,182,364** |
| Initial trained samples | **344,488,915** |
| Canonical checkpoint SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control/evaluation training JAR | `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f` |
| Candidate training JAR | `4a809f7311b71af967b3d5302b064b365c989de38f22ae26e92aa40fe30f75cc` |
| Both inference JARs | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The control is an independent detached checkout at the accepted main source. The
candidate uses a separately owned Academy. Both copy the preserved canonical
checkpoint; neither touches the continuously running development Academy. No
focused-policy conversion, optimizer reset or course rewrite is permitted.

Training uses 512 actors per arm, seed 7, 2 GiB heap, two visible processors, two
region threads, one inference thread and one learner thread. Arms may run
concurrently on loopback-only ports 31281 and 31282. Every segment ends with a
controlled stop and native-verifiable canonical checkpoint. A failed or rejected
candidate is never installed into the development Academy.

All frozen reports, including candidate reports, use the **unchanged control
runtime**. Thus candidate curriculum code cannot leak into evaluation.

## Prospective baseline and frozen matrix

Use fresh frozen-evaluation seeds **2026093051** and **2026093052**, 32 cases for
each ordered ordinary task **0 through 12**. There is no reset intervention.
The common parent is evaluated once on each seed before training: two reports,
**832 frozen trials**.

Input qualification requires, separately on both seeds:

- every task 0-10: at least **28/32**;
- wooden-pickaxe task 11: at least **26/32**;
- task 12 mining is measured but has no minimum input score.

If input qualification fails, no learner starts. Historical course stage,
certificates and the earlier 31/32 task-11 reports are not substituted for this
fresh baseline.

After each learning boundary, evaluate both arms on both seeds: four complete
reports / **1,664 frozen trials** per phase. Every unsuccessful case is retained.
Frozen evaluation contributes zero training samples.

## Accepted-sample budgets and early stop

Both arms begin at 344,488,915 trained samples. Boundaries are cumulative:

- **early: +250,000 accepted samples**;
- **final: +1,000,000 accepted samples**.

At most +50,000 stop/flush overshoot is allowed at each boundary. Offered samples,
wall time, decisions and completed episodes are not substitutes for the accepted
sample counter. Each training segment has a 30-minute operational cap and a 2 GiB
effective available-memory floor.

The retained accepted-context observer is mandatory evidence. Every fresh status
must bind its context origin to the segment's starting trained-sample counter,
contain the complete 19 × 6 task/menu sample and tick arrays, preserve exact
per-task marginals, and show actual task-12 accepted exposure. Context samples and
elapsed actor ticks are recorded separately. These counts describe exposure, not
success.

Both arms must reach and stop at the early boundary before either early result is
used. Complete all four early frozen reports before applying the gate. On each
seed and **in each arm**:

- every ordinary task 0-10 must be at least
  `max(28, parent_successes - 3)` /32;
- wooden-pickaxe task 11 must be at least
  `max(26, parent_successes - 4)` /32;
- task 12 must be at least
  `max(0, parent_successes - 2)` /32.

Any early retention failure in either arm stops **both** final continuations.
Passing retention permits the fixed final budget even if early mining remains
zero. A later recovery cannot erase an early retention veto.

## Useful-pilot screen

A useful final pilot requires all retention gates and, on **both** seeds,
candidate ordinary task-12 completion:

- at least **8/32**, and
- at least **4/32 above the matched control**.

This is an engineering screen, not a confidence interval or proof of mastery.
Two frozen seeds are not independent training replications. Descriptive mining
trace changes—held-pick observations, target contact, maximum target mining ticks,
effective dig selections, broken blocks, pickups and menu occupancy—are retained
even when completion fails, but cannot replace the ordinary completion screen.

The maximum completed evidence is ten reports / **4,160 frozen trials**:
832 baseline + 1,664 early + 1,664 final. An early stop gives six reports /
2,496 trials.

## Health, integrity and noninterference

Fresh training status must show all 512 actors restored and observed, no inference
failure/rejection, no stale/rejected learner samples and no retired actors.
Counters and clocks must be monotone. The exact source, runtime, canonical
checkpoint and policy hashes are pinned throughout. Evidence paths are create-only
and owned by the study worktree.

Offline controller tests cover complete matrices, exact per-seed retention and
acquisition thresholds, accepted-sample bounds, context-array origin/dimensions/
marginals/tick bounds, stale health, monotone counters, create-only evidence,
no-run CLI behavior and EULA gating. They must pass in normal and optimized Python.
The Java potential test covers task scope, finite bounds, monotonic target
progress and a closed-loop telescoping calculation.

No result automatically merges this branch, changes main or deploys a model. A
favorable final pilot would justify a separate adoption decision and activation
verification. A rejection preserves the exact source, checkpoints and reports and
closes the research PR unmerged.

## Execution results

No gameplay or learning outcome is claimed at declaration.
