# Probe policy provenance and matched lesson lifecycle

Date: 2026-09-26. Base main: `d9f9dbfa5943e674b8393561378d1554c9967f9f`.
Designated workspace: `lkjsxc/minecraft-agents`, hostname
`cw-c76e701404f844d8876a4d50`. Development checkout:
`/home/coder/workspace/botsclustersmc-lesson-boundary`.

## Decision

Accept read-only accounting of the behavior policies actually used inside
completed full-difficulty probes, together with paired diagnostic tooling and
regression tests. **Do not adopt the reset-delay experiment.** Do not claim that
the original 130/130 live versus 10/32 frozen wood-pick discrepancy is fully
explained or that cobblestone was learned.

The original production server and its canonical checkpoint remained paused at
policy **802777**, accepted samples **376971442** throughout these experiments.
No original Academy, optimizer or course was copied, reset or migrated. Only
policy bytes from the already evaluated deployment bundle were used in new,
loopback-only diagnostic worlds. The cold independent-expert candidate from the
previous session remains rejected and is absent from this work.

## Fixed-policy, paired-reset diagnostic

The diagnostic uses the ordinary NPC controller, `TrainingEnvironment.reset`
and the ordinary task success checks. There is no optimizer or scripted action
controller. In each arm 32 actors execute tasks **11, 12, 11**: wood pick,
cobblestone, then wood pick again. Every trial keeps the same model. EXAM and
PROBE reset seeds are adjusted for the existing mode-specific XOR salts so the
actual reset RNG inputs match; action RNG seeds are also matched per task/case.
First observations, masks, elapsed ticks and every failed outcome are retained.

Policy bytes are from
`botsclustersmc-source/.build/evidence/20260926-paused-current-evaluation.zip`:
policy 802777 / 376971442 samples. The same inference-only archive is used by the
baseline and the unchanged-controller replication. It is rebuilt from current
source, not executed from the input bundle.

| Run | Seed | EXAM first wood pick | PROBE first wood pick | EXAM repeated wood pick | PROBE repeated wood pick | Cobblestone |
|---|---:|---:|---:|---:|---:|---|
| Original reset | 2026092641 | 11/32 | 11/32 | 9/32 | 11/32 | 0/32 in both arms |
| Extra reset delay | 2026092641 | 11/32 | 11/32 | 9/32 | 9/32 | 0/32 in both arms |
| Original reset, replication | 2026092642 | 8/32 | 8/32 | 8/32 | 9/32 | 0/32 in both arms |

Each run completed all **192** trials: 576 completed trials across the three
runs, not 576 successes. All reported zero training samples. Every first
EXAM/PROBE pair in both original-reset seeds had identical initial 512-element
tensors and 233-element masks, and identical individual success/failure outcomes.
All initial menus were closed and initial crafted-count features were zero.
Mean observed transition duration was five ticks in every arm/phase.

For the repeated wood-pick task, initial differences remained in vertical
velocity (feature 9) and ground contact (feature 11). Thus matching reset/action
random seeds does **not** establish identical server physics across all phases.
The summary prints every differing input index, not only favorable comparisons.
The measured first-trial mode agreement narrows the investigation; it does not
prove universal reset equivalence or isolate all scheduling/history effects.

The extra-delay implementation changed reset completion from the next entity
callback to a two-tick delayed callback. It did not remove those initial-state
differences or improve the first wood-pick result. It was reverted rather than
presented as a fix. Final `core/` and `plugin/` match the original base exactly.

Local experiment identities:

- Baseline diagnostic implementation: `193bcc9`.
- Report validator: `5062057`.
- Rejected reset-delay trial: `c8713a896781f0453588ab77cbbf5c6fdb37a513`.
- Explicit revert: `8012169`.
- Final measurement implementation: `8591b3aa7febc0e3a17ddb7556d0a57a5b90aa26`.
- Live acceptance/replication source: `2abe0a682ede2b80b8c56e9d16ed819665ef57f8`.

These are local commit identities; publication may have different commit
metadata. Whole-tree comparison, rather than claiming identical commit IDs,
is required before integration.

## Applied-policy accounting

`ProbePolicies.Trace` observes `previous.result().policyVersion()` for each
completed action. It does not read the latest learner version as a substitute,
consume random numbers, freeze a probe or alter which policy is used. At probe
completion, successful and failed episodes are partitioned into single-version
and mixed-version groups. Decisions, consecutive version changes, maximum
within-episode version span and the latest episode's min/max are reported.

`LessonOutcomes` publishes the existing totals and their new partition under the
same monitor. A reader cannot observe a total from one moment and a partition
from another. Empty/interrupted attempts and practice/exam episodes do not enter
the completed-probe partition. Traces and counters do not enter the persisted
model, optimizer or course format. The inference-only archive has no new
training dependencies.

The dashboard labels these as **process totals**, not a score of the current
model. Single-version episodes can each use a different version; mixed-version
attempts change policy within the attempt. Missing old measurements are
unavailable, not reconstructed. Invalid/torn/unsafe-integer data fails closed.
Paused/stale snapshots are historical. The separate fixed-policy evaluator
remains the source of measured competence for one model.

## Verification completed locally

The full Java source suite passed. The new **335** checks include exact counts,
nonmonotonic input sequences, zero/absent handling, failed outcomes, independent
snapshots, concurrent writers, atomic total/partition consistency, overflow
rejection without partial mutation, and unchanged action RNG, distributions,
weights and canonical model/Adam/course bytes. Existing course tests were updated
only for the mandatory recorded-usage argument.

The lifecycle report validator passed **12** synthetic tests, including missing
and duplicate trials, invalid task/outcome/shape/nonfinite data, mismatched RNG
identity and zero-duration trials. These synthetic tests are not learned-skill
evidence. The real-API lifecycle fixture is also compiled by the normal suite.

Offline Playwright/Chromium checks passed for the added panel: desktop/mobile
bounds, native and serialized arrays, successful/failed counts, exact freshness
boundary, absent/malformed data, inconsistent partitions and non-certificate
wording. Existing crafting and activation browser checks also passed. Initial
browser execution failed because Playwright was not installed; the pinned
existing CI dependency was installed in a development-only virtual environment,
and the successful rerun is retained separately. No global runtime dependency
was added.

## Real production-plugin smoke test

The actual `TrainingPlugin`, not the lifecycle substitute, was exercised using
`tests/acceptance.py all --count 64 --seconds 40 --inference-count 16
--inference-seconds 10` with explicit EULA consent and a fresh disposable Academy.
It completed fresh learning, clean shutdown, exact model/Adam resume, canonical
export, 18 scripted full-difficulty reachability fixtures, and inference-only
plugin operation with 16 actual NPCs. Pause/resume, goal replacement, ticket
release, respawn and invalid-model fail-closed behavior passed. All test servers
exited; no production restart was required. The scripted fixtures demonstrate
mechanical reachability, not learned skills.

In the first stopped training phase, **all 64 completed probes used multiple
policy versions**; four succeeded. Their 7531 decisions contained 872 changes
between consecutive applied versions, and the largest per-episode version span
was 21. The single-version group was empty. Raw outcome totals and the new
partitions matched at every sampled status. These are task-0 smoke-test results,
not measurements of the old wood-pick probes. They demonstrate actual policy
mixing, not that mixing is the sole cause of the prior discrepancy.

That fresh phase accepted 11791 samples and reported zero rejected/stale learner
samples. The normal shutdown also reported 1306 buffered, untrained samples;
those are not included as trained samples. The exact-resume/export assertions
passed. No claim of a drained zero actor buffer is made for that scratch phase.

## Retained evidence and next boundary

All paths below are relative to the development checkout and are workspace
records, not public download links:

```text
.build/evidence/lifecycle-2026092641-baseline/
.build/evidence/lifecycle-2026092641-settled/
.build/evidence/lifecycle-2026092642-replication/
.build/evidence/probe-policy-live/
.build/evidence/probe-policy-monitor/
```

Each lifecycle folder retains `manifest.json`, `summary.json`, `server.log` and
the full `server/plugins/BotsClustersMC/lifecycle-result.json`. The live smoke
folder retains stopped and resumed snapshots, exact-resume receipt and exported
artifacts. Source/browser/live runner logs remain under `/tmp/bcmc-probe-policy-*`.
No original production credentials or world data are part of the source change.

The original server still runs its previously loaded plugin with decisions and
learning paused. Updating source alone does not install the new collector into
that already running process. Its historical measurements must not be relabeled
as single/mixed-policy data. The new collector is verified in disposable real
servers and becomes active on a subsequent controlled deployment/restart.

The remaining investigation is to measure policy identity during representative
late-task probes and compare frozen checkpoints under aligned conditions, while
preserving transfer and already learned skills. This measurement milestone does
not by itself increase wood-pick competence, acquire cobblestone, or justify
resuming unrestricted training on the original paused checkpoint.
