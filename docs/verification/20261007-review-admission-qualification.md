# Review admission: source and scheduling qualification

Date: 2026-10-07 (Japan). Research only; no runtime or model adoption.

## Question and fixed intervention

Keep an actual opportunity to review while frontier episodes are unfinished.
At each new frontier lesson, count enrolled training participants at that same
frontier and admit at most `max(1, floor(participants * 4 / 5))` frontier episodes.
An actor over that bound selects its own least-observed-time earlier task.
Do not interrupt any current episode. Shrinking populations can therefore be
above the bound until their next admission opportunity; this is not an invariant
on all snapshots. A singleton is explicitly exempt from a fractional reviewer.

Enrollment begins at the first training issue and survives natural inter-episode
reset gaps. Abandonment or explicit interrupted-gap withdrawal removes it.
Unstarted/decoded actors, mandatory-exam waiters and active exams cannot supply
review capacity. Occupancy is transient, not a checkpoint field, earned credit,
observed time or a certificate. Actual review still incurs the existing local
4:1 effort debt; the admission guard does not fabricate effort.

Both the candidate and its matched control retain review-first resume, the
previous rejected stronger mining reward and task-12 critic-feature detachment.
The control source is `7f18c3b83dd16dede2ff7f2337b3ab84465fe027`, **not ordinary
accepted-main training**. The intended between-arm runtime source boundary is
`Course.java`, `ReviewEffort.java` and the interruption call in `TrainingPlugin.java`.
Core inference, actuators, resets, rewards, gradients, model and optimizer must
be identical between those arms. No unmerged candidate may enter the shared
Academy. Accepted main at entry is `ed013b62de3bd871c1d4a38e9a364491dc7dcf54`.

## Known short-screen history, not a prospective result

The 2026-10-06 work already observed candidate 8/8 short cases at 250,000 modeled
decisions. This is scheduling evidence only, not newly accepted learner data or
skill retention. Its initial current-lesson-only membership lost reviewers in
normal reset gaps; the saved candidate fixed that before this continuation.

The 2026-10-07 lifecycle regression first failed on the saved candidate: a peer
waiting for its mandatory exam inflated reviewer capacity although `issue`
could not give it training. The candidate now excludes such peers without
starting, waiving or changing their exams. The failing log is retained under
`.build/qualification/lifecycle-red-20261007.log`. Additional deterministic
state-machine checks cover idle gaps, sparse enrollment, explicit withdrawal,
restart, non-preemption and exact observed-time accounting.

## Long-replay screen fixed before its first execution

`ReviewAdmissionReplayTest` uses the exact complete common parent
`a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e`
and policy `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc`.
The historical duration CSV has SHA-256
`38268911c0a258c77fb5a17203b8434e3346923c5847202c3eed800ddcda8bcc`:
tasks 0-11, 32 cases each, separately for seeds 2026100611 and 2026100612.
These historical seeds are not fresh evaluation seeds and no new game is run.

Run four scheduling cases per duration seed: ascending order, descending order,
one half-speed actor cohort, and one-decision synthetic review. The last case is
an explicitly synthetic lifecycle stressor, not historical gameplay. Each case
ends at 3,000,000 modeled decisions, with 5 actor-ticks each and a 600-decision
frontier horizon. Evaluate the late interval after the first 1,000,000 decisions.
Do not infer an exact real-server acceptance rate from this replay.

For **both** candidate and matched control, each of the eight cases must finish
with 10%-40% late review, at least one completed frontier episode per actor and
at least one observed decision for every actor/earlier-task pair. Exact tick
accounting and unchanged model/Adam/source-checkpoint bytes are required.
No synthetic exam, forced frontier, promotion or regression reset is permitted;
an unexpected stage change or exam readiness fails this fixed-parent screen.
Record all 50k-window extrema, but do not impose the short-screen per-window
bounds on long synchronized completion waves. These broad feasibility margins
are engineering screens, not confidence intervals or fairness proofs.

Separately rerun the original short screen in both actual compiled runtimes:
candidate must pass 8/8 and matched control must pass 0/8. Preserve all failed
cases; do not extend budgets or relax screens after their outcomes. Checkpoint
identity, initial lessons, runtime source/artifact boundaries, full local source
suite and source CI must also qualify before any new live-learning comparison.

No live-learning protocol is authorized by this qualification record alone.
Any new comparison must declare fresh frozen evaluation seeds, complete tasks,
case counts, accepted-sample bounds and rejection thresholds in a normally pushed,
source-CI-qualified revision before the learners start. Until then there is no
new learning result, deployment, reset of shared progress or retention claim.
