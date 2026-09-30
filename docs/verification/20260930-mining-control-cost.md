# Mining spatial-control cost: prospective matched warm continuation

Date: 2026-09-30 (Japan). This protocol is fixed before any Minecraft training or
frozen gameplay result from the candidate. It follows two completed negative
results on the same qualified parent family:

- a one-time favorable target-facing reset did not improve task-12 completion;
- a bounded potential on closed-menu held-pickaxe readiness plus already-existing
  target-mining progress retained earlier skills but left mining at 0/32 on both
  seeds after a full matched +1M-sample comparison.

Those studies show that initial pose and a progress signal that activates mainly
after contact are insufficient. They do not establish that learned alignment,
reach or movement control is irrelevant.

## Question and single intervention

Can a **nonpositive continuous control cost** teach the policy to maintain the
spatial prerequisites for real target contact, while preserving the current
model's useful skills?

Only task 12 gains one extra training reward term. Before the target block is
broken, for each transition of `ticks` elapsed actor ticks, define:

```text
time = ticks / 4
alignment =
    0.025 * min(1, |yaw_error| / 90)
  + 0.025 * min(1, |pitch_error| / 45)

reach = 0.015 * min(1, max(0, horizontal_distance - 3) / 8)

lined_up = max(0, 1 - (|yaw_error| + |pitch_error|) / 30)
motion =
    0.004 * min(1, horizontal_speed / 0.18)
          * lined_up
          * min(1, 3 / (horizontal_distance + 0.01))

unfinished = 0.040 * (1 - designated_target_mining_progress)

control_reward = -(unfinished + alignment + reach + motion) * time
```

All inputs already exist in the ordinary training observation/runtime state.
`designated_target_mining_progress` is nonzero only when the runtime's actual
mining state names the declared target block; selecting dig in empty air does not
reduce the cost. Once the target block has actually been broken, this control cost
becomes exactly zero so it does not penalize post-break pickup/recovery.

The term is always nonpositive and bounded by 0.109 per four actor ticks before
the target breaks. It is not potential shaping and cannot create reward by
cycling through a favorable state. Lower cost requires observable alignment,
appropriate distance, reduced motion while lined up, or actual target-mining
progress.

## What does not change

The candidate does **not** alter:

- task-12 reset distribution, supplied wooden pickaxe, target block or spawn;
- actions, action masks, action frequency, inventory semantics or tool effects;
- movement, look or dig actuators;
- observations or target coordinates;
- network architecture, optimizer, V-trace, entropy term or task balancing;
- terminal success: task 12 still requires provenance-correct cobblestone pickup
  retained in the pocket;
- review allocation, probes, exams or deployment inference code.

There is no auto-aim, pathfinder, forced inventory closure, forced tool selection,
teacher action, scripted dig, or answer-supplying mask. Full probes and frozen
evaluation remain unchanged and unassisted.

The formula is intentionally analogous to the existing log-harvest control cost,
but it is implemented in a separate task-12-only class so the accepted
`HarvestPractice` reset behavior and log tasks are unchanged.

## Exact parent and runtime isolation

Both arms begin from the preserved complete canonical model/Adam/course state
captured at 1,182,364 policy updates and 344,488,915 trained samples.

| Identity | Value |
| --- | --- |
| Accepted main / control source | `38bcd960eee29c2401eb13538d24a6acb817f42e` |
| Canonical checkpoint SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control/evaluation training JAR | `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f` |
| Candidate training JAR | `782fb3bcde791745672b5d1433fa3f3efb190737dc594578bbc796079e8233c3` |
| Both inference JARs | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The candidate training JAR differs from control only in
`MiningControl.class`, `TrainingEnvironment.class`, and
`TrainingPlugin.class`. Inference is byte-identical.

The control uses an independent detached accepted-main checkout. Each arm owns a
separate Academy copied from the preserved checkpoint. The live development
Academy remains untouched. Training uses 512 actors, learning seed 7, 2 GiB heap,
two visible processors, two region threads, one inference thread and one learner
thread per arm on loopback ports 31381/31382.

All frozen reports—including candidate checkpoints—are evaluated using the
unchanged control runtime.

## Fresh baseline and complete matrix

Use frozen evaluation seeds **2026093071** and **2026093072**. Each report contains
32 cases for every ordered ordinary task **0 through 12**, with
`reset_intervention=none`.

Before learning, the common parent is evaluated once per seed: two reports /
**832 trials**. Input qualification requires independently on each seed:

- tasks 0-10: at least 28/32 each;
- wooden-pickaxe task 11: at least 26/32;
- task 12 is measured but has no minimum.

If the baseline fails, no learner starts.

At each learning boundary, both arms are evaluated on both seeds: four reports /
**1,664 frozen trials**. Evaluation contributes zero training samples. Every
failure is retained.

## Accepted-sample budgets and retention veto

Both arms start at 344,488,915 trained samples. Cumulative boundaries:

- early: **+250,000 accepted samples**;
- final: **+1,000,000 accepted samples**.

At most +50,000 stop/flush overshoot is allowed. Each segment has a 30-minute cap
and a 2 GiB available-memory floor. Both arms reach and stop at a boundary before
any report from that boundary is used.

Every fresh learner status must show all 512 actors, restored-checkpoint startup,
zero inference failures/rejections, zero stale/rejected learner samples and zero
retired actors. Accepted-context arrays must bind to that process's exact starting
sample counter, retain the complete 19 × 6 task/menu sample/tick layout and match
the existing task marginals. Actual task-12 accepted context exposure is required.

At early and final boundaries, for each seed and each arm:

- task 0-10 score >= `max(28, parent - 3)`;
- task 11 score >= `max(26, parent - 4)`;
- task 12 score >= `max(0, parent - 2)`.

Any early retention failure in either arm stops **both** final continuations.
Passing retention permits the fixed final budget even if early mining remains
zero.

## Useful final pilot

A useful final pilot requires all retention screens and candidate task-12
completion, separately on both seeds:

- at least **8/32**, and
- at least **4/32 above matched control**.

The engineering thresholds are not confidence intervals. The two frozen seeds are
not independent training replications. Held-pick observations, target contact,
maximum pick mining ticks, effective world-dig selections, broken blocks, pickups
and menu-state occupancy are descriptive secondary measurements and cannot replace
the ordinary completion screen.

Maximum complete evidence: **10 reports / 4,160 frozen trials**. Early stop:
6 reports / 2,496 trials.

## Pre-execution verification

The Java control-cost test covers task isolation, finite input validation,
symmetry, nonpositivity, bounded scale, elapsed-tick scaling, post-break zero cost
and 10,000 randomized states. It is numerical evidence only.

The finite Python study controller checks complete condition matrices, exact
per-seed retention/acquisition thresholds, accepted-sample/overshoot bounds,
process freshness, context-origin/dimension/marginal/tick validation, monotone
counters, create-only files, EULA gating and no-run CLI behavior. It must pass in
normal and optimized Python. The full local source suite and GitHub Ubuntu,
Windows and observatory source checks must pass before gameplay.

No result automatically merges the candidate, changes main or installs a model.
A favorable full-budget pilot still requires a separate adoption decision and
live activation check. A rejected candidate is preserved and closed unmerged.

## Execution results

No outcome was claimed at declaration. The following completed observations were
appended later without altering the protocol, source, thresholds or trial matrix.

### Qualified inputs, execution and complete early rejection

Actual source `234e0cf5e15b31343f4f1f4b9916ae85c0df180e`, tree
`5321c61508566a5e96cc081969d68a233ba2c03e`, was authored as `lkjsxc` and
pushed before gameplay. CI run `36681275504` passed Ubuntu, Windows and
observatory checks; dispatch-only live/Paper/retention/Windows-live jobs were
skipped. The 12 controller methods passed again in normal and optimized Python
at closeout. Both initial native exports matched the prescribed complete parent.

All six reports / 2,496 frozen trials completed. A is seed `2026093071`, B is
`2026093072`. Vectors are ordered tasks 0-12, every score out of 32.

```text
parent    A: [32,32,32,32,32,32,32,32,32,32,32,30,0]
parent    B: [32,32,32,32,32,32,32,32,32,32,32,29,0]
control   A: [32,32,32,32,32,32,32,32,32,32,32,30,0]
control   B: [32,32,32,32,32,32,32,32,32,32,32,27,0]
candidate A: [32,32, 7,32,32,29,32,28,32,32, 9, 0,0]
candidate B: [32,32, 8,31,30,30,31,31,32,32,11, 0,0]
```

Both arms accepted exactly **262,144** new samples and stopped at 344,751,059.
Control stopped at update 1,183,005; candidate at 1,183,019. Native verification
confirmed the saved policy exactly matches each complete stopped checkpoint.
Candidate retention failed for task 2, task 10 and task 11 on both seeds, and
task 7 on seed A. The fixed floors were 29/32 for tasks 0-10 and 26/32 for task
11. The control retained all declared floors. Mining acquisition was zero in
both arms on both seeds. Neither +1,000,000 final continuation was run.

| Stopped artifact | SHA-256 |
| --- | --- |
| Control canonical checkpoint | `df2e968906f480f29cfa654ee20369706bfc415216db857f5ba2ab164a745f18` |
| Control policy | `4dcfdf66e27db615841df7627f5c2f6f44ba933a706becd7cecce48b767b6b45` |
| Candidate canonical checkpoint | `6550ce1576e219953c4995373dc8fb8edf343b9cdc84c828763961899f5add56` |
| Candidate policy | `b0ca13b62a1d34047ad3b650069862057292d71113be7f78f2d8692fb8ab94ab` |

Each arm retained 28 process-local health observations. All 512 actors were
observed at warm startup and progressing at the final observation; recorded
inference failures/rejections, stale/rejected learner samples and retirements
were zero. The clock, task counters and all context cells were monotone.
Every accepted sample in the last snapshots belonged to task 12. Neither process
had yet accumulated review ticks. This short early transition budget therefore
covered no earlier-task review; that is not proof that review is permanently
disabled. The matched control experienced the same absence without the candidate's
large retention loss.

Last-observed control accepted counts were 71,540 closed and 190,348 inventory,
total 261,888; candidate 44,074 closed and 217,878 inventory, total 261,952.
Stop/flush added 256 and 192 samples respectively. Do not combine those earlier
context denominators with the final stopped totals. Segment wall times were
157.134 and 157.177 seconds, descriptive only, not a throughput benchmark.

### Mining observations and interpretation

Each ordinary mining report contained 19,200 decision-boundary observations.
The candidate's final frozen reports had more closed-menu observations than
control (9,188/9,080 versus 5,044/5,222), but fewer effective world-dig selections
(175/143 versus 605/652). Held-pick observations were 730/613 versus 499/544.
No arm recorded target pickaxe contact or a broken block at the early boundary.
Changing occupancy or holding a tool more often is not completed mining.

The candidate is rejected and must not be deployed. This is a completed early
retention veto, not a full-budget result or an operational failure. The results
show that this exact stronger control-cost intervention damages existing behavior
before useful mining is acquired. They do not isolate actor versus critic gradients,
optimizer moments, reward scale or representation changes as the cause. A large
shared-trunk update is a hypothesis to test, not a conclusion from occupancy.
Further extension of this rejected state is prohibited by the declared stop.

### Preserved evidence and independent closeout

A read-only closeout reran all original report validators and bindings, recomputed
every per-seed/task retention floor, verified native stopped exports and all
context/time-series identities. No new gameplay contributed to the 2,496 trials.
The original evidence remains in the dedicated mining-control worktree under
`.build/mining-control-study/`.

The unpublished research draft `mining-control-study-20260930` targets the original
source. `mining-control-evidence-20260930.zip` contains 64 members, 5,283,829 bytes,
SHA-256 `9ae8aaef62e090b38e0c256378c970dbb3ae925c1a36644e48a2bf4c2247d5cf`.
ZIP integrity and every manifest entry passed; a fresh GitHub download matched
exactly. The explicit allowlist retains complete initial/stopped canonical states,
runtimes, seed-specific evaluators, reports, receipts, histories and source logs;
it excludes server binaries, worlds, caches, credentials and unrelated data.
PR #51 is closed unmerged; the branch and failed evidence remain available.
