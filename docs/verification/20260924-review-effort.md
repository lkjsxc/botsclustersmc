# Elapsed-tick review allocation — 2026-09-24

## Delivered source

- Base: `6271796cfa2914a4e30f160823d5b9059520fc24`.
- Implementation: `90acb1d45339fabd2e2481a855b955821c935c93`.
- [Pull request #4](https://github.com/lkjsxc/botsclustersmc/pull/4) was merged normally as `83b206e04ae04b3380a85c0e67f7a18a5ffcec5d`.
- The implementation and final integration both have tree `fa8f3db0862a2dd391fdb41b1373279d80c77736`.

The seven changed files add a training-only `ReviewEffort` ledger, connect it to
actual observation intervals in `Course` and `TrainingPlugin`, add regression
checks through the existing course-test entry point, and explain the contract in
[REVIEW_EFFORT.md](../REVIEW_EFFORT.md) and [LEARNING.md](../LEARNING.md).
Core inference, the deployment plugin, recipes, motor inputs, reward, reset
resources, model schema and exam pass requirements were not changed.

This fixes an exposure-allocation defect, not a newly demonstrated neural skill.
A percentage of review episodes can mean much less review experience when
frontier failures are long and successful reviews are short. The new unit is
observed Minecraft ticks. At a fixed stage, frontier ticks earn one credit and
review ticks spend four; allocation changes only at episode boundaries. Earlier
tasks receive least-served-time review with random tie breaking. Full probes
follow each task's completed training count so their cadence cannot alias with
the frontier/review cycle. Probes still train; frozen exams do not.

## Local software checks

Java 21 compiled and ran the actual allocator and Course classes, not a separate
reimplementation. The final test run reported:

```text
PASS elapsed-tick review checks=4149761
PASS independent course checks=164709
```

The review count is assertion executions, not millions of independent test cases
or Minecraft trials. Fifteen synthetic runs used five frontiers and three
constant/heterogeneous/variable episode-duration patterns, 30,000 episodes each.
Review time ranged from 19.9836% to 20.0021% of observed training time. Exact credit
conservation and one-episode allocation bounds were checked throughout.

The integrated checks cover all 18 synthetic course transitions, per-task probe
cadence, failed frozen exams, forgotten-skill regression, interrupted observed
work, restart semantics, historical certificates, invalid lesson identity,
integer safety and concurrent actor accounting. Synthetic successful outcomes
are supplied only in tests; they are not operator checkpoints or learned passes.

Three shadow builds deliberately restored defects and were all rejected:

| Deliberate defect | Detecting assertion |
| --- | --- |
| Global probe cadence | Per-task probe cadence resists five-episode aliasing |
| Missing effort recording | Frontier is not locked out of full probes |
| Frozen exams enter training budget | Exam cannot buy or consume review time |

All seven uploaded Git blobs were checked against the locally tested files.
The seven-file patch passed a reverse-application check against that source.

## Clean-checkout repository CI

[Workflow run 35923168774](https://github.com/lkjsxc/botsclustersmc/actions/runs/35923168774)
ran the unmodified repository workflow for this PR. Both source jobs succeeded:

| Job | ID | Result |
| --- | --- | --- |
| Ubuntu 24.04 | 107391825907 | Passed |
| Windows | 107391826237 | Passed |

The PR checkout named merge revision
`4dd1477ff79afbaf3b1ddcc24376e68ccb3a6b6f`, combining implementation `90acb1d`
with base `6271796`. The ordinary `./test.sh` / `test.cmd` path compiled against
the pinned official Folia 1.21.11 build 14 API and built the inference and training
JARs. Both downloaded source-test artifacts contain the new review checks, the
existing course checks, core/mechanics/ownership/control tests, guarded updates,
five synthetic bandit seeds, persistence, concurrency, source-launcher export,
operator-evaluation integrity and the final inference-artifact separation pass.

Artifacts are `source-Linux` (10777862779) and `source-Windows` (10778017580).
Their configured retention is finite; the source and this summary remain in Git.
The synthetic bandits are not Minecraft learning results.

The real-server `live`, `windows-live` and `paper` jobs were **skipped** by the
existing pull-request conditions. They require the separate explicit live-test
workflow route. They must not be described as passing for this change.

## Runtime boundary and remaining evidence

No operator Academy restart, canonical checkpoint replacement, world change,
experimental-weight promotion or deployment JAR replacement was performed by
this delivery. Concurrent uncommitted menu-affordance work was left untouched;
its trials are not evidence for this allocator. The GitHub source is integrated,
but on-device deployment and real-server validation of this change remain undone.

The added foundation/frontier/review/exam tick counters are process-local.
Within a process, interrupted observed work retains its effort cost. Stage
changes and process restarts begin a fresh allocation interval. Existing model,
Adam, RNG, course statistics and historical certificates retain the current
checkpoint format; no alternate generation or migration wrapper was introduced.

Before claiming improved retention or faster progress, run isolated training
from copies of the same complete canonical checkpoint, preserve its actor count,
and compare fixed-policy, full-difficulty evaluations on every reached task.
Use both equal accepted-sample and equal wall-time budgets; report reset overhead,
rejected/stale experience and individual task scores. More short reviews mean
more resets. The existing capped task-loss balancer also means 80% frontier time
is not 80% frontier gradient mass. Never replace the operator's live learning
history with a synthetic course or an older trial checkpoint.

No workbench mastery, prevention of catastrophic forgetting, normal-player
survival, open-world generalization or multiplayer cooperation is established by
this software verification.
