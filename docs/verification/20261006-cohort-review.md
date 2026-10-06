# Observed-only cohort credit: prospective retention comparison

Date: 2026-10-06 (Japan). Fixed before any gameplay in this study.
Research only: this protocol does not authorize runtime/model adoption or an
unbounded learning continuation.

## Question and relationship to the failed studies

The completed [resume-first comparison](20261006-resume-review-results.md)
delivered initial earlier-task experience but did not retain ordinary wooden
pickaxe crafting. Review represented 16.64% of accepted contexts at the first
53,896-sample observation, but only 3.66% at the last 256,232-sample observation.
Giving every restored actor one initial review credit is not sustained rehearsal.

The [older reserved-cohort study](20260928-reserved-review-study.md) already
supplied about 20% continued review and still failed its fresh-seed crafting
criterion. It used a different, much older complete parent, forecast reservations
of full episode horizons, cohort-level least-served-task selection, and a larger
interrupted continuation. Do not erase that negative result or describe sustained
coverage itself as a proven retention solution.

This new test does not copy that reservation mechanism or continue any rejected
trained state. It isolates **sharing actual observed credit after identical first
lessons** under the latest matched resume-first learning condition. It preserves
each actor's original least-observed-time earlier-task selection and uses no
predicted effort, reservations, fixed reviewers or interrupted gameplay episodes.
The useful question is whether this observed-only sharing delivers continuing
rehearsal and preserves the whole measured skill set in a finite comparison.
Neither outcome is assumed from arithmetic or previous partial results.

## The single changed mechanism

Both arms use the previous rejected stronger mining-control reward, task-12
critic-feature detachment and one-credit resume phase. The **control is the
previous resume-first research runtime, not ordinary accepted-main training**.

Candidate keeps each actor's local observed-time ledger and per-task review totals.
For each of the 18 frontiers, it additionally maintains the sum of current members'
local credits. After an actor's first issued training lesson in a new allocation
interval, positive cohort credit permits that actor's next lesson to be review;
nonpositive credit selects its own frontier. Which earlier task is selected still
uses that actor's least-observed review time and original RNG.

For fixed membership above stage zero, the exact identity is
`cohort_credit = initial_member_offsets + frontier_ticks - 4 * review_ticks`.
An in-flight frontier actor can therefore fund a peer's next review before its own
long episode ends. Only actually observed intervals buy or spend credit. Whole
concurrent episodes can overshoot the sign boundary; this is **not** a guaranteed
20% finite-window sample allocation. There is no cohort barrier or action choice.

Every actor's first issued training lesson after restore or stage change uses its
original local credit, independent of peers' scheduling speed. Both restored arms
therefore start all 512 actors on the same earlier-task distribution. Fresh starts
and promotion/regression still start that actor on its frontier. Abandonment keeps
actually observed work; an issuance without observations creates no time credit.
An actor changing frontier withdraws its exact local contribution from the old
pool and contributes zero to the new one. Earned progress is never shared.

Foundation work buys no review. Exams preserve their original order, frozen model,
readiness and pass thresholds; their ticks remain separate. Cohort sums and first-
lesson flags are transient, as are the existing effort ledgers. The canonical
model/Adam/course format, stored statistics, certificates and RNG are unchanged.

No reward, gradient path, task balancing, observation, action, reset distribution,
terminal condition, model architecture, optimizer initialization or inference
artifact differs between arms. Changed subsequent experiences, RNG draws,
fragment lengths, batching and optimizer update counts are consequences of the
scheduling intervention, not held-identical trajectories.

## Pinned inputs and artifact boundary

| Identity | Value |
| --- | --- |
| Accepted main at entry | `4bc3ac58b2b2ff365483f5087070fea03bfdb223` |
| Matched resume-first control source | `7f18c3b83dd16dede2ff7f2337b3ab84465fe027` |
| Parent updates / accepted samples | 1,182,364 / 344,488,915 |
| Complete parent model/Adam/course SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control training and every frozen evaluator SHA-256 | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Candidate training SHA-256 | `666ef5f6b797a2b41048481fdc871a70f746689bcc6df680b0cf2a3dd657301b` |
| Byte-identical inference JARs SHA-256 | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

Only `Course.java` and `ReviewEffort.java` differ in runtime source relative to
control. The seven differing training payloads are `Course.class`,
`Course$Agent.class`, `Course$Effort.class`, `Course$Metrics.class`,
`Course$Progress.class`, `Course$StageMetrics.class`, and `ReviewEffort.class`.
The four record declarations are unchanged in source, but their compiled bytes
also differ after editing the enclosing file; this record does not claim those
four payloads are byte-identical or independently verified as debug-only changes.
The artifact guard pins this complete observed class set and both full JAR hashes.
The common parent is a preserved qualified
checkpoint, not a snapshot of the current moving development policy.

Native checks of the real parent must reproduce exact model/Adam bytes and export
counters in both arms. They must also reproduce identical first-lesson and
post-first-lesson course hashes under interleaved simulated observations. The
read-only qualification observed the same first task population in both arms:
`[42,42,42,37,40,53,53,38,43,38,42,42,0,0,0,0,0,0]`.
Those synthetic Course calls are not gameplay or additional learner samples.

## Complete finite comparison

First evaluate the common parent on fresh seeds **2026100611 and 2026100612**,
ordinary ordered tasks **0-12**, **32 cases each**, `reset_intervention=none`.
Each complete report must score at least 28/32 on every task 0-10 and 26/32 on
task 11. Both reports finish before any learner starts. Failed input qualification
stops the study before training; no seed, floor or source substitution is allowed.

Each arm resumes the same complete original parent exactly once and receives one
**+250,000 accepted-sample** segment with at most **+50,000** stop/flush overshoot.
No larger segment is authorized. Each owned, loopback-only Academy uses 512 actors,
learning seed 7, a 2 GiB heap, two JVM-visible CPUs, region/inference/learner threads
2/1/1 and ports 31681/31682. Available memory must remain at least 2 GiB. The
inherited 30-minute operational cap, clean stop, native stopped-state verification
and failure cleanup remain mandatory. Other projects and shared learning are not
stopped to make room.

After both learners stop, evaluate each frozen final model on both declared seeds
using the identical control evaluator and ordinary tasks 0-12. Up to four owned
loopback evaluators use ports 31690-31693. Maximum complete comparison: **six
reports / 2,496 frozen trial executions**, with no evaluation learning. Complete
all post-training reports even when the accepted-exposure screen fails. Preserve
operational failures and partial results without calling them complete reports.

## Prespecified accepted-exposure screen

First-complete startup must show 512 restored review actors in both arms, with
identical initial training-task populations and zero initial frontier actors.
Retain every fresh status row, first-complete startup, final observed status,
complete stopped checkpoint and native counters. Validate the original context
base, task/menu marginals, all 114 sample/tick cells and monotonicity.

Use the first observed context total at or above each boundary
**50,000 / 100,000 / 150,000 / 200,000 / 250,000**. Each must be no more than
25,000 samples beyond its boundary. Windows run from zero to the first endpoint,
then from one actual observed endpoint to the next; do not interpolate exact
cutoffs or mix overlapping cumulative averages with disjoint windows.

For the candidate, each of those five disjoint windows must contain **5%-40%
actually accepted earlier-task samples** and **at least 50% frontier samples**.
The first endpoint must include positive accepted samples from every task 0-12.
In the late interval from the first observed 100,000 boundary to the first observed
250,000 boundary, each earlier task 0-11 must contribute at least 100 samples.
Candidate late review share must exceed control by at least five percentage
points. Both initial distributions must match. These requirements define the
sustained-exposure contrast; initial assignments alone do not satisfy it.

A missing, late or multiply crossed observation boundary fails the timing/exposure
screen, without inventing samples and without suppressing the frozen comparison.
Malformed or regressing evidence is an operational failure, not a valid low score.
The first observed positive review boundary is status-resolution evidence, never
an exact first-gradient timestamp. Final pre-stop context counts are distinct
from the later exact stopped checkpoint totals.

## Retention support and separate acquisition

Per-seed task floors are `max(28, parent - 3)` for tasks 0-10,
`max(26, parent - 4)` for task 11, and `max(0, parent - 2)` for task 12.
Every floor must pass separately; averages cannot rescue a failed task or seed.

The prespecified retention-support criterion requires all candidate floors on
both seeds, the sustained-exposure contrast above, and reproduced control wooden-
pickaxe loss of at least 8/32 on **each** seed with tasks 0-10 retained. A control
without that reproduced loss cannot show that the candidate prevented it.
Report partial patterns even when the conjunction fails.

Mining acquisition is separate: candidate ordinary task-12 success must be at
least 8/32 and at least 4/32 above control on each seed. Report actual broken
blocks, held-tool observations, target pick contact, world-dig selections and
generic pickups separately. Tool holding is not completed cobblestone acquisition.

These are engineering screens, not confidence intervals. There is one training
trajectory per arm; two evaluation seeds are not independent learning replicas.
No outcome automatically licenses deployment, a second segment or an unplanned
search over rewards, seeds, review amounts or additional architectures.

## Software qualification and evidence ownership

Before gameplay: reproduce the missing peer-credit behavior against the unchanged
control; pass the full source suite, dedicated cohort lifecycle/concurrency tests,
ordinary and optimized controller tests, real-parent native checks, exact artifact
boundary, and exact-source CI. Commit and normally push this declaration and all
implementation/test sources before the first frozen parent evaluation.

The research worktree is
`/home/coder/workspace/botsclustersmc-cohort-review-20261006`.
The existing resume-first worktree provides the immutable matched control runtime.
The only explicit gameplay entry is:

```sh
EULA=true python3 tests/studies/cohort_review.py --run
```

Evidence is create-only under `.build/cohort-review-study/`; software qualification
is retained under `.build/qualification/`. Original parent, both complete stopped
states, every raw report, runtime/source identity, status history and failure are
preserved. The shared Academy and its current 512 actors remain untouched by this
research branch. A later accepted-main record must distinguish software properties,
measured learning results and live health. Documentation-only integration with
identical runtime inputs does not require a restart.
