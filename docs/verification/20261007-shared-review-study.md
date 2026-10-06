# Shared-policy rehearsal: prospective live retention comparison

Date: 2026-10-07 (Japan). Research only. No automatic runtime/model adoption.

## Decision being tested and prior failure that remains a failure

The [previous admission qualification](20261007-review-admission-results.md)
failed its predeclared finite **individual actor-by-earlier-task** coverage gate:
7/8 candidate cases and 6/8 control cases. Actor 136 missed tasks 1, 2 and 7 in one
half-speed candidate replay despite 20.01315% aggregate late review and five
completed frontier episodes. That result, its source, all raw failures, original
thresholds and PR #60 remain unchanged. This document does **not** declare it
passed or repair individual full-cycle coverage. No extra replay budget or new
duration seed is used to turn it into a pass.

The current implementation has one `Learner` in `TrainingPlugin.initialize` and
one published `Policy` shared by ordinary training actors. `policyFor` returns an
actor-specific frozen snapshot only for its exam. Individual bodies/courses do
not have independent trainable networks. Therefore individual full-cycle coverage
and aggregate data for the shared network are different engineering objectives.
Requiring every actor to touch every earlier task within the same finite window
is not logically necessary to test retention of this one shared policy. It may
be relevant to actor-specific goals, contexts or future individual learners, and
we do not dismiss it as generally irrelevant.

This is a **new, explicitly narrower experiment**: does the unchanged admission
runtime supply sustained, actually accepted earlier-task data to that shared
learner, and does its resulting frozen policy retain ordinary earlier skills
under a reproduced matched-control crafting loss? The known short-screen
feasibility and 20%-range aggregate long behavior justify testing this question,
not claiming the answer. All prior long-run window bursts and individual misses
remain known limitations. No claim of per-actor coverage, universal scheduling
fairness, a deployable scheduler or learned competence follows from exposure alone.

This deliberate endpoint change is declared before any new Minecraft evaluation
or learning. It is not an outcome-dependent amendment of the earlier protocol.
Only the controller, tests and this prospective record are new relative to the
already measured admission runtime. No runtime parameter is tuned on these trials.

## Pinned arms and common starting state

Accepted main at entry: `db89041bde85463329634bc1a1ef39b0f0572763`.
Candidate runtime source is byte-for-byte the training/core/plugin sources from
`3d6cd3ac69f47ffb2d8062a9cbd444f8705052b6` (PR #60). The new research revision
containing this document and controller must be normally pushed and pass exact-
head Ubuntu/Windows/observatory source CI before any live comparison begins.

Control source: `7f18c3b83dd16dede2ff7f2337b3ab84465fe027`, in a clean detached
worktree. This is the prior review-first research runtime, **not ordinary
accepted-main training and not the current moving shared-server policy**.
Both arms retain the previously rejected stronger mining control reward and
task-12 critic-feature detachment. Their only runtime source differences are
`Course.java`, `ReviewEffort.java` and the `TrainingPlugin` interruption hook.

Candidate caps **new** frontier admissions at `max(1, floor(4*n/5))` for enrolled
same-frontier training participants, excluding unstarted, withdrawn, active-exam
and mandatory-exam-waiting actors. Natural inter-episode reset gaps retain
membership. Already running episodes are never interrupted. The applicant still
uses its own least-observed-time earlier-task selector and actual 4:1 effort debt.
No credits, observed ticks, certificates or skills are invented by the quota.
Control has the same review-first resume but no admission cap.

| Pinned object | SHA-256 |
| --- | --- |
| Complete common model / Adam / course | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Initial inference policy | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Candidate training JAR | `d30ef666d97ffa2b5de32729f4171b8bb4c5f81d4c4d431a55feb5f5d13a3430` |
| Control training / every frozen evaluation JAR | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Inference JAR, byte-identical in both arms | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |

The complete parent has 1,182,364 updates and 344,488,915 accepted samples. Both
arms copy that parent into newly owned, separate Academies; neither continues a
failed trained candidate. Verify native complete-state roundtrip/export and
matching first-lesson population before running. Synthetic/native qualification
generates no learner samples. Rebuild artifacts and compare these exact hashes.
All previous failed evidence and the shared Academy remain untouched.

## Fresh ordinary frozen matrix and input qualification

Evaluation seeds are **2026100721 and 2026100722**. They were not used by the
previous comparisons. Each report contains tasks **0 through 12, in that order**,
**32 cases per task**, `reset_intervention=none`: 416 actual trials. Evaluate the
parent on both seeds before either learner starts, using the same control runtime
as every subsequent evaluator. Parent qualifies only if each task 0-10 scores at
least 28/32 and wooden pickaxe (task 11) at least 26/32 on **each** seed.
If qualification fails, retain both full parent reports and run neither learner.
Do not replace seeds or switch parents.

For a qualified parent, after the one learning segment evaluate both stopped
policies on both seeds. The complete comparison is six reports / 2,496 frozen
trial executions. Frozen evaluations never train or change their policy bytes.
Always finish all four stopped-policy reports after an exposure-screen failure;
missing exposure is not a reason to hide a skill result. An operational failure
is separately retained, not promoted as a complete result.

## Finite learning and operational boundaries

Each arm has **512 in-server NPCs**, learning seed 7, one inference worker, one
learner worker, two region threads and a 2 GiB heap. Both start concurrently with
at most two training servers. They bind to localhost on 31681/31682. At most four
frozen evaluators bind to localhost on 31690-31693. Use the existing pinned Folia
server cache, with explicit EULA consent. No third-party or operator world is used.

Stop each learner at **+250,000 actually accepted samples**, allowing at most
**+50,000** for polling/flush overshoot. Record exact stopped native update/sample
counts, not only status estimates, and report any between-arm difference. There
is no second segment, automatic extension, rescue checkpoint, reward change or
sample-budget adjustment, regardless of the result. The controller's wall-clock
and memory limits are operational failure bounds, not competence thresholds.

Weights, optimizer, observation, action semantics, reward, reset mixture, loss,
balancing, terminal predicates and examination criteria must not change between
arms except for the declared admission mechanism. No replayed teacher action,
imitation, pathfinder, auto-equip, automatic menu closing or autocrafting is added.
The study never modifies the shared development runtime or moving policy.

## Actual shared-learner exposure contract

Use `LearningContexts` counts of **accepted pre-action task/menu observations**,
not assigned lessons, completed episodes, all offered data or simulated ticks.
Retain every fresh status snapshot and all 114 task/menu sample and elapsed-tick
cells. Verify origin, shape, integer types, sums and cellwise monotonicity.

Both startup snapshots must show all 512 restored actors' first training lessons
as earlier-task review, with identical complete task populations. Take the first
actual observation at or above each +50k, +100k, +150k, +200k and +250k boundary.
It may overshoot its nominal boundary by at most 25,000 accepted samples. Windows
are **disjoint actual observed intervals**, not overlapping cumulative averages.
A missing/late/duplicated boundary is an exposure failure, never invented timing.

For candidate: each window needs 5%-40% earlier-task samples and at least 50%
frontier samples. Every task 0-12 must have accepted data by the first boundary.
In the observed 100k-to-250k interval, each earlier task 0-11 must have at least
100 accepted samples. Candidate late review share must exceed control by at least
five percentage points. Do not replace these task-specific checks with a total.
These numerical margins are unchanged from the preceding live cohort comparison,
not tuned to this new live run. We record task/menu context breakdowns as well;
we do not pretend that task labels alone guarantee all useful crafting states.

The finite individual full-cycle condition from PR #60 is **not this experiment's
endpoint** and is not asserted to hold. Observed actor liveness and true earned
course state remain software boundaries. This study can at most support a shared-
policy effect under the particular initial state, finite budget and environments.

## Frozen retention, control reproduction and separate acquisition

Let `p` be that seed's parent task score. Candidate must achieve on **both seeds**:

* Tasks 0-10: at least `max(28, p - 3)` successes out of 32 for every task.
* Task 11 (ordinary wooden pickaxe): at least `max(26, p - 4)` out of 32.
* Task 12: no loss exceeding two parent successes (`max(0, p - 2)`).

The matched control must reproduce a task-11 loss of at least **8/32 on each
seed**, while retaining each earlier task 0-10 under the same floors. Without
this reproduction the proposed explanation is unsupported even if candidate
scores look good. Support requires the conjunction of all candidate retention
floors, reproduced control loss and the complete accepted-exposure contrast.
Neither cross-seed averaging nor nominal candidate/control advantage replaces it.

Mining acquisition is a separate criterion: candidate at least 8/32 and at least
four more successes than control on each seed. Held-tool frames, closed menus,
target contact and broken blocks are diagnostic counts, not replacements for
ordinary task completion. All failed and partial mining evidence is retained.

There is one learner trajectory per arm; the two evaluation seeds are not two
independent training replicas. Fixed-policy trials are condition replications,
not a causal identification of every gradient term. The controller always records
`deployment=false`: even a positive pilot requires a separately justified follow-
up and accepted-runtime decision. No skill improvement is claimed before results.
