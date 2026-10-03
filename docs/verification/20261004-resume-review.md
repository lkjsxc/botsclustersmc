# Resume-review phase: prospective retained-skill comparison

Date: 2026-10-04 (Japan). Fixed before gameplay. This is a finite research
comparison, not an accepted curriculum change or a deployable learning solution.

## Question and one changed mechanism

The completed [actor attribution](20261002-actor-drift.md) found output sensitivity
in the preserved critic-detachment pair. Neither component-only replacement was a
verified repair. The preceding detached candidate also accepted only task-12
examples before its early boundary. This study tests **initial earlier-task
exposure**, not another reward, representation, output, or gradient intervention.

Both new arms start from the same complete qualified parent. Both use the previous
**detached-critic runtime with the stronger mining-control reward**. The control
is therefore a reproduction of the preceding rejected candidate's learning
condition, **not unchanged mainline training**. Candidate differs only on course
checkpoint decoding: transient per-actor review credit starts at **one instead of
zero**. Each restored actor whose frontier exceeds zero starts with an earlier
task selected by the original least-observed-ticks rule and persisted random
state. This does not choose a gameplay action or supply an answer.

The single credit unit is an explicit scheduling phase offset, **not an observed
tick, optimizer sample, earned certificate or completed episode**. Actual review
work is debited at the unchanged four-frontier-ticks-per-review-tick rate. After
any positive amount of initial review, that actor owes frontier work. The exact
interval identity is `credit = 1 + frontier_ticks - 4 * review_ticks`; whole-episode
overshoot remains bounded as before. The asymptotic observed review share remains
20%, not a permanent review-weight increase. Foundation actors remain on task 0.
Fresh starts, promotions and regressions still begin with zero review credit.
Mandatory frozen exams keep their ordering and do not consume training credit.
As before, effort debt is process-local; this protocol has exactly one learning
resume per arm and does not repeatedly restart to obtain new offsets.

No reward, observation, actuator, reset distribution, teacher, loss weighting,
V-trace target, actor/critic forward function, gradient path, Adam state, model
schema, certificate or terminal predicate differs between arms. Initial model,
optimizer and persisted course bytes are identical. Scheduling subsequently
changes accepted examples, random draws, visited states and optimization; these
are consequences, not held-identical trajectories. A positive result would not
isolate a gradient term or prove a general explanation of forgetting.

## Pinned input and implementations

| Identity | Value |
| --- | --- |
| Accepted main at entry | `ec2e30115edc14695152c3847385a90314126366` |
| Matched control source | `ddcf2f6d24bfa0bbb3a14d3665be2e629eeb8602` |
| Parent updates / samples | 1,182,364 / 344,488,915 |
| Complete parent model/Adam/course SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control and all frozen-evaluation runtime SHA-256 | `fef1ef4d73ed320392ab30322afe2233a765e3c4e74e7bb48fc458d3362fd3cd` |
| Candidate training runtime SHA-256 | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Both inference JARs SHA-256 | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

Relative to the control, only `Course.java` and `ReviewEffort.java` differ in runtime
source; only `Course.class`, `Course$Agent.class` and `ReviewEffort.class` differ in
the training artifact. No experimental artifact is installed on the shared Academy.
Native export and verify-export must reproduce the original policy and counters
from the complete canonical parent in both arms before new learning.

## Complete finite protocol

First evaluate the common parent on fresh seeds **2026100401 and 2026100402**,
ordinary ordered tasks **0-12**, **32 trials per task**, `reset_intervention=none`.
Input qualification requires every task 0-10 at least 28/32 and task 11 at least
26/32 separately on each seed. Complete both reports before any learner starts.
A failed qualification stops learning without changing seeds, floors or source.

Each arm then receives one **+250,000 accepted-sample** segment with at most
**+50,000** stop/flush overshoot. Both use 512 actors, learning seed 7, 2 GiB heap,
two visible processors and region threads, one inference thread and one learner
thread. Two owned, loopback-only Academies use ports 31681/31682. The inherited
30-minute operational cap, memory floor, immutable input/runtime checks, native
stopped-checkpoint verification and failure cleanup remain mandatory. There is
**no larger-budget continuation** in this protocol.

After both arms stop, complete every ordinary post-training task/seed report using
the identical control evaluator. At most four loopback evaluators, ports
31690-31693, run together. The maximum complete comparison is **six reports /
2,496 frozen trial executions**, all without new learning. Retain failures and
partial results; never silently replace a physical run or evaluate only favorable
tasks. Exposure failure does not suppress the full frozen comparison.

### Accepted exposure, not assigned lessons

Retain every fresh process-local status observation, first-complete startup,
last-observed status, exact stopped checkpoint and native counters. Verify the
original context base, all task/menu marginals and monotonic sample/tick cells.
Keep final pre-stop context counts distinct from later stop/flush totals.

Candidate startup must show all 512 restored actors initially assigned earlier
tasks. In the first status with **at least 50,000 accepted context samples**, and
no more than 75,000, every earlier task **0-11** and frontier **12** must have
positive actually accepted samples. Control must initially assign all 512 actors
to task 12 and have zero accepted earlier-task samples through its last observed
pre-stop status. This is a declared exposure contrast, not a claim that mainline
never reviews. Record the first *observed* positive review sample boundary without
calling it the exact first-gradient time; status cadence limits timing resolution.
A legitimate failed coverage screen is reported as such, not repaired by changing
the sample window after seeing results.

### Retention, diagnostic support and acquisition

Per-seed preservation floors are `max(28, parent - 3)` for tasks 0-10,
`max(26, parent - 4)` for task 11 and `max(0, parent - 2)` for task 12.
No averaging across seeds or tasks can replace a failed floor.

The prespecified review-retention support criterion is the conjunction of:
candidate retaining **all** floors on both seeds; control retaining tasks 0-10
while losing at least **8/32** ordinary wooden-pickaxe completions on each seed;
and the accepted exposure contrast above. Report partial patterns even when this
conjunction fails. A control without the declared reproduced loss does not prove
that the candidate prevented that loss.

Mining acquisition is separately described only when candidate ordinary mining
is at least **8/32** and exceeds control by at least **4/32** on each seed. Report
actual broken blocks, held-tool observations, target contact, world-dig inputs
and generic pickups separately. Retaining crafting is not acquiring mining.
These are engineering screens, not confidence intervals. There is one training
trajectory per arm; two evaluation seeds are not independent learning replicas.
No result automatically authorizes deployment or another training segment.

## Engineering and evidence boundary

A failing behavioral test must reproduce frontier-only restored startup before
the change. New Java checks cover the repayable offset, unchanged long-run actual
tick share, all frontiers, exact checkpoint serialization, persisted statistics,
certificates, RNG reproducibility, interrupted review, foundation behavior,
promotion/regression reset and mandatory exams. Existing math, mechanics, lifecycle
and artifact-separation checks remain. Controller tests cover strict complete
matrices, per-task/per-seed floors, real accepted coverage, status timing bounds,
missing contexts, invalid population, counter regressions, exact finite budgets,
create-only evidence, symlink rejection and no implicit gameplay start.

The local full suite, normal and optimized controller tests, actual-parent native
checks and source CI must pass before gameplay. Commit and push the prospective
source and protocol before the first evaluation. Run only from the research
worktree with separately owned Academies:

```sh
EULA=true python3 tests/studies/resume_review.py --run
```

Evidence is create-only under `.build/resume-review-study/`. The common complete
parent, both complete stopped states, all raw reports/configurations/receipts,
learning histories, source/runtime identities and failures are preserved. The
shared development service, its 512 actors, current policy and other projects
remain untouched. Any accepted-main result record must distinguish code changes,
controlled learning outcomes and live health; documentation-only updates with
unchanged runtime inputs require no service restart.
