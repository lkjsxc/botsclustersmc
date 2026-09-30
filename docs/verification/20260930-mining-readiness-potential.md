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

No gameplay or learning outcome was claimed at declaration.

### Completed execution

Prospective implementation and protocol source:
`06b70bdfa1b1180d8b2d75d6ceef5d48d1ed992c`, tree
`ae1a46b2260a1c917e97f09166de26db44362d07`. It was authored as
`lkjsxc` and pushed before gameplay. PR #50 remained a research PR throughout
execution. GitHub run `36678324392` passed Ubuntu source, Windows source and
observatory checks; dispatch-only live/Paper/retention/Windows-live jobs were
skipped and are not counted as executed. The complete local source suite ended
`MINING_READINESS_SOURCE_EXIT 0`; the 12 controller tests passed in normal and
optimized Python. Production member comparison found exactly
`MiningPractice.class` and `TrainingEnvironment.class` changed in the training
JAR, while the inference JAR was byte-identical to accepted main.

Both initial native exports reproduced the declared complete parent checkpoint
and policy. All two baseline reports completed before either learner started:

```text
parent A: [32,32,31,32,32,32,32,29,32,32,31,31,0]
parent B: [32,32,32,32,32,32,32,31,32,32,32,28,0]
```

The input qualification passed on both seeds. These are the fresh declared seeds,
not scores borrowed from the preceding current-policy qualification.

### Early boundary

Both owned 512-actor learners reached the +250,000 accepted-sample boundary and
stopped cleanly.

| Arm | Additional accepted samples | Stopped samples | Stopped updates | Wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 257,664 | 344,746,579 | 1,183,022 | 146.404 |
| Candidate | 257,280 | 344,746,195 | 1,183,023 | 146.538 |

Stopped early artifacts:

| Artifact | SHA-256 |
| --- | --- |
| Control canonical checkpoint | `49896838e2cef780d8071cd766f7b33c871fa17f05a0035fe943aaa78f8bd244` |
| Control exported policy | `ea92e3b6a915c3db3a4cb439991f2405775233b10c88fe0177fd6dd7985d5cc0` |
| Candidate canonical checkpoint | `7700559c55f1c829247ba4aca8a483d165059d740ef1a667226ff63106a50b60` |
| Candidate exported policy | `7867a763db7968240b969444a5780443183527fc224b5ed04fda973c68a2fd35` |

The complete early frozen matrix was:

```text
control   A: [32,32,32,32,32,32,32,32,32,32,32,31,0]
control   B: [32,32,32,32,32,32,32,32,32,32,32,28,0]
candidate A: [32,32,32,32,32,32,32,32,32,32,32,31,0]
candidate B: [32,32,32,32,32,32,32,32,32,32,32,28,0]
```

Every retention floor passed in both arms. Mining remained 0/32 on both seeds in
both arms. By declaration, retention—not early acquisition—controlled whether the
final segment ran, so both arms continued from their exact stopped early state.

At the last observed early status, accepted task-12 context samples were control
67,921 closed / 183,215 personal-inventory and candidate 69,103 / 181,777. No
other menu bucket was populated. The small context shift is exposure telemetry,
not evidence of successful behavior.

### Final boundary

Both arms continued from their exact early checkpoint and reached the cumulative
+1,000,000 boundary within the declared overshoot.

| Arm | Additional accepted samples | Stopped samples | Stopped updates | Wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 1,011,665 | 345,500,580 | 1,185,181 | 388.210 |
| Candidate | 1,013,868 | 345,502,783 | 1,185,198 | 390.316 |

Stopped final artifacts:

| Artifact | SHA-256 |
| --- | --- |
| Control canonical checkpoint | `84587af4a50ba750f69d1bad67244ec4b4055408f7fad1bd8abf1c5d277773b9` |
| Control exported policy | `9f8f1b33ac640a652bc073471a1c7bc0268551a8b2fa6eadb519524157352306` |
| Candidate canonical checkpoint | `de1335e4029606d68611644f1909a37f32bc7c55ae98bdf4d38e0b1d25b563d9` |
| Candidate exported policy | `3b136e364bff6e7f410da8b3bd06016511fee9745a20d29cc40661803516306c` |

The complete final frozen matrix was:

```text
control   A: [32,32,32,32,32,32,32,32,32,32,31,29,0]
control   B: [32,32,32,32,32,32,32,32,32,32,31,31,0]
candidate A: [32,32,32,32,32,32,32,32,32,32,32,30,0]
candidate B: [32,32,32,32,32,32,32,32,32,32,32,29,0]
```

Every task 0-11 retention gate passed in each arm and seed. Candidate task-12
completion remained **0/32 on both seeds**, exactly like control. It therefore
failed both the absolute 8/32 acquisition requirement and the matched-control
+4/32 requirement. The final outcome is:

```text
stage=final
input_qualified=true
retained=true
acquisition=false
useful_pilot=false
reports=10
frozen_trials=4160
deployment=false
```

This is a complete full-budget rejection, not an early stop or operational
failure. The state potential preserved the measured prior skills under this
single matched run but did not establish ordinary cobblestone acquisition.

### Exposure and behavior interpretation

The final training processes accepted nearly the same amount of task-12 data:
608,848 control versus 607,560 candidate samples. Their task-12 accepted menu
contexts were:

| Arm | Closed | Personal inventory | Other menus |
| --- | ---: | ---: | ---: |
| Control | 161,960 | 446,888 | 0 |
| Candidate | 166,935 | 440,625 | 0 |

The candidate therefore had slightly more accepted closed-menu exposure during
training, but that did not translate to frozen completion. Both final processes
recorded zero task-12 practice successes (control 0/1,010; candidate 0/1,007) and
zero probe successes (control 0/211; candidate 0/212). Review remained active:
task-11 accepted samples were 20,036 control and 21,315 candidate, and task-11
frozen retention passed.

The final frozen mining diagnostics are also unfavorable to the candidate:

| Seed / arm | Pick held observations | Pick target contact | Max pick mining ticks | Effective world-dig selections | Closed / inventory observations |
| --- | ---: | ---: | ---: | ---: | ---: |
| A control | 565 | 0 | 0 | 823 | 5,674 / 13,526 |
| A candidate | 571 | 0 | 0 | 435 | 4,663 / 14,537 |
| B control | 552 | 1 | 4 | 778 | 5,335 / 13,865 |
| B candidate | 676 | 0 | 0 | 441 | 4,648 / 14,552 |

No target block was broken in any final task-12 report. Candidate held-pick
observations increased on seed B, but effective world-dig selections were roughly
halved on both seeds and target contact did not improve. The candidate's frozen
policy also spent more decision observations in the personal inventory than the
matched control, despite its slightly higher closed-menu share in accepted
training samples. These are bounded observations from one training run per arm,
not proof that the shaping term necessarily causes those differences.

The important negative result is stronger: a bounded potential for menu/tool
readiness plus existing target-mining progress was not enough to bootstrap the
missing sustained target interaction. The target-progress component remained
effectively sparse because the policy almost never reached pickaxe contact in the
first place. A next hypothesis should improve **learned continuous spatial control
toward the target** without auto-aim, scripted tool choice, forced menu closure or
answer-supplying masks. Repeating this exact potential longer is not justified by
the completed result.

### Revalidation and retained evidence

A read-only closeout recomputed baseline qualification, both gates and all ten
report bindings/diagnostics; re-ran native `verify-export` on all four stopped
canonical states; replayed every retained process-local status history through
the accepted-sample/context validators; checked exact early-to-final continuation;
and rebuilt an explicit-whitelist archive. The archive CRC and all manifest
member sizes/digests passed.

The unpublished GitHub research draft `mining-readiness-study-20260930` targets
the prospective source and retains `mining-readiness-evidence-20260930.zip`:
**133 members, 7,350,488 bytes**, SHA-256
`8acd75707979fcee7707a6a4e06ad1e5e1c22a2c4af02cbcb8afd8e2a9725d8b`.
A fresh download matched the local archive byte-for-byte. The allowlist includes
the exact parent, both runtimes, all stopped checkpoints/policies, context
histories, training receipts, protocol/controller source and all ten frozen
reports. It excludes Minecraft server binaries, worlds, cache, environment files,
credentials and unrelated data.

Do not deploy the candidate runtime or either candidate checkpoint. PR #50 should
remain a research record and be closed unmerged. The continuously running
development Academy was not modified by this study.
