# 2026-09-26 - normalized hidden-layer cold-start study

## Decision: rejected, not deployed

The operator explicitly permits training interruption and a fresh start. The
previous production learner was stopped normally and its full checkpoint and
world retained. Two new owned loopback Academies started from seed 7 without
any existing model, optimizer or curriculum. This is a comparison of fresh
learning, not a retention comparison against the earlier mature policy.

The normalization-only candidate is **rejected for deployment**. Its minimum
forward-stop pilot passed, but the observed turning deficit triggered a stricter
prospective follow-up. After approximately six million accepted samples per arm,
the candidate scored 28/32 forward-stop and 0/32 turn-stop on BOTH final seeds;
the baseline scored 32/32 on both tasks and both seeds. All four follow-up reports
completed. Software correctness and reduced saturation do not override this
real-Minecraft failure. No stone-harvesting or survival improvement is established.

Production was restarted on the existing unnormalized main implementation and
its intact mature checkpoint. The candidate code and weights are not installed.
This study publishes operator authorization and experimental evidence to main,
not the rejected architecture or its incompatible model schema.

Baseline: main `58ed06a1abdd230751119e958b4546f94b333f08`.
Candidate execution implementation: `a4fc5df` (local Git commit); subsequent
`e1ae48cbbcf834fac55331ebb68edf8235d6c7c9` adds specification/schema tests and
`c82684754a0fe433e79ecb61975941573c4151c2` records the additional prospective gate.
The rejected candidate remains at `/home/coder/workspace/botsclustersmc-normalized`,
with both its experimental training processes stopped.

## Intervention

Parameter-free layer normalization precedes each hidden tanh in the shared
512 -> 96 -> 96 policy. Each observation is centered and scaled independently
using its own hidden-unit population variance plus epsilon 1e-5. No running/batch
statistics or learned normalization gain/shift are introduced. The output layer
stays linear and the parameter count remains 68,842. The exact backward pass
includes derivatives through the hidden mean and variance.

The candidate semantic schema is `bcmc-citizen-normalized-context`; old weights
are not valid merely because their tensor sizes match. Production remains
`bcmc-citizen-egocentric-context`. Rewards, primitive actions, observations,
curriculum allocation, initialization arrays, optimizer settings, full-condition
trials and their success predicates were unchanged between the experimental arms.

Normalization bounds saturation of hidden tanh outputs. It does not bound the
full network Jacobian away from zero or guarantee plasticity. The normalization
inverse scale can shrink as weight norms grow. Weight projection, weight decay,
recurrent memory, task experts and scripted gameplay were not bundled into this
candidate. See Ba et al., [Layer Normalization](https://arxiv.org/abs/1607.06450),
and Lyle et al., [Disentangling the Causes of Plasticity Loss](https://arxiv.org/abs/2402.18762)
and [Normalization and effective learning rates](https://arxiv.org/abs/2407.01800).
This is not a replication of their benchmark results or their combined methods.

## Initial real-Minecraft pilot: all completed results

Workspace: `minecraft-agents`, hostname `cw-c76e701404f844d8876a4d50`.
Both arms used 1,024 server-side NPCs, a 3 GiB Java heap, seed 7, and
region/inference/learner threads 3/1/3 on the same designated host. The server
was pinned Folia 1.21.11 build 14. These NPCs are not vanilla network players.
Each arm stopped after status reached 3,000,000 accepted samples, then drained
normally. The actual accepted counts include the stopping overshoot.

| Arm / evaluation seed | Policy | Accepted samples | Forward-stop | Turn-stop | Aim-hold |
| --- | ---: | ---: | ---: | ---: | ---: |
| Unnormalized, 2026092621 | 6675 | 3016086 | 32/32 | 24/32 | 0/32 |
| Normalized, 2026092621 | 6635 | 3016048 | 32/32 | 0/32 | 0/32 |
| Same normalized weights, 2026092622 | 6635 | 3016048 | 32/32 | 0/32 | 0/32 |

The first arm took 845.246 seconds and the second 846.073 seconds from their
launcher starts through normal shutdown. This shared-host measurement is not a
hardware-independent throughput benchmark. All three 96-case reports completed
with zero evaluator learning. A separate archive audit checked all 288 trials,
complete actor sets, task denominators, success totals, model/JAR identities and
exact same-model reuse for the second normalized seed.

The normalized learner received **1,354,195** accepted turn-stop samples, compared
with **1,055,854** for the baseline. Its corresponding training outcomes were
15 successes / 11,001 trials, versus 7,529 / 13,402 for the baseline. Thus merely
not having started the turning task does not explain the observed deficit.
These adaptive training trials are not interchangeable with the frozen exams.

At the final learner batch, normalized second-layer mean local tanh slopes were
about 0.566 for forward-stop and 0.588 for turn-stop. The baseline values were
about 0.561 and 0.629. These are different sampled batches, not paired observations,
and local slopes exclude the normalization Jacobian. Healthy-looking activation
numbers alone did not establish turning competence.

## Additional gate fixed before continuation

The local follow-up gate was committed as `c826847` before resumed learning.
Resume both arms from their own complete stopped checkpoints. Add at least
3,000,000 accepted samples to each, retaining the early and final checkpoints.
Keep all runtime and learner settings unchanged; record actual final overshoot.
Each continuation has a 3,600-second execution bound.

Evaluate each final model on ordered tasks [0,1,2], 32 cases each, seed
2026092623. Re-evaluate each exact final model with `--from`, same tasks and
case counts, seed 2026092624. Require four complete reports and zero evaluator
learning. On **both seeds separately**, the candidate must score at least 30/32
forward-stop and 26/32 turn-stop, and must be no more than two successes below
the baseline on either required task. Aim-hold is a recorded frontier, not a
required learned skill at this budget. Do not pool seeds to hide failure.

This was an additional prospective engineering screen prompted by the initial
result, not a preplanned statistical superiority test. The original forward-only
screen and its passed result are preserved, not retroactively rewritten. The
additional screen failed; no threshold was lowered or seed substituted.

## Completed follow-up: all four reports

| Arm / evaluation seed | Policy | Accepted samples | Forward-stop | Turn-stop | Aim-hold |
| --- | ---: | ---: | ---: | ---: | ---: |
| Unnormalized, 2026092623 | 13694 | 6020842 | 32/32 | 32/32 | 0/32 |
| Same unnormalized weights, 2026092624 | 13694 | 6020842 | 32/32 | 32/32 | 0/32 |
| Normalized, 2026092623 | 13050 | 6024210 | 28/32 | 0/32 | 0/32 |
| Same normalized weights, 2026092624 | 13050 | 6024210 | 28/32 | 0/32 | 0/32 |

Baseline additional accepted samples: 3,004,756, completed in 901.742 seconds.
Candidate additional accepted samples: 3,008,162, completed in 762.531 seconds.
Both stopped normally, under the same initial configuration. Different episode
lengths and adaptive curriculum positions affect throughput; the faster candidate
was not the more competent policy. The budgets are close, not exactly identical.

The baseline evaluated at 12:59:29.171 and 13:00:12.003 JST on 2026-09-26. The
candidate evaluated at 13:00:55.452 and 13:01:39.050 JST. Both full checkpoints
are retained. Each seed within an arm used exactly the same frozen policy and
inference JAR; the evaluator added zero training samples. All 384 follow-up
trials completed, including every failure. The combined archive audit covered
**672 complete trials** across all seven initial and follow-up reports.

During the additional training, baseline turn-stop had 54,275 successes in
54,726 trials with 1,810,376 accepted turn samples; the candidate had 22 successes
in 19,474 trials with 2,412,732 accepted turn samples. The candidate's last measured
turn batch had second-layer saturation about 0.0101 and local tanh slope about
0.612. Reduced saturation again was not sufficient for learned turning.

The baseline's final stage population was 16 at task 1 and 1,008 at task 2;
the candidate remained entirely at task 1. These are curriculum positions, not
successful aim-hold evaluations. Both arms still scored 0/32 on aim-hold.

## Software and artifact checks completed

The full candidate source suite passed. The new independent double oracle,
finite-difference layer/network Jacobians, gradient accumulation, zero-variance
and extreme-input cases, scalar/batch bit identity and neighbor-lane isolation
produced 75,198 assertions. Eleven schema-boundary checks covered valid-checksum
incompatible models/checkpoints and exact current-format roundtrips.

Three separately compiled incorrect variants were rejected: omitting the
mean/variance derivative, incorrect batch centering, and a changed epsilon.
The tested source and candidate JAR remained unchanged. A separate real-data
probe rejected both the retained pre-change full checkpoint and an earlier
actual evaluated policy specifically for schema mismatch, not checksum damage;
the original full checkpoint remained byte-identical.

A detached clean checkout at `c826847` passed the full source suite and rebuilt
inference and training JARs byte-identical to the candidate worktree's builds.
These checks establish software properties, not learned stone mining, open-world
survival or cooperation. The rejected source and its tests were not merged into
main; docs-only main updates did not trigger the path-filtered validation workflow.

## Restored production operation

The live checkout was fast-forwarded cleanly to main
`9a2d26e77f0428b1d2d683cf6892f4df2e528f39`, whose executable source is unchanged
from `58ed06a`. Before restart, the original full checkpoint was compared
byte-for-byte with `prechange-training.bcmc` and was unchanged. The latest main
was built and the ordinary launcher restarted in tmux session `bcmc-training-main`.

At 13:03:02 JST the server restored policy **705288**, **330484383** accepted
samples and Adam step **705288**. It used the original `academy-current` world,
port **25565**, 1,024 NPCs, the existing 8 GiB heap and automatic thread allocation
9/1/5. Existing online authentication remained enabled. The localhost monitor
remained on its original Academy path; no rejected experimental policy was copied
into production and no original world was erased.

A three-snapshot health check at 13:04:13.516 through 13:04:23.516 JST confirmed
1,024 active/ticking/progressing actors. Accepted samples rose from 330730147 to
330768835 and policy updates from 705832 to 705918. Inference failures/rejections,
learner rejected/stale samples and burning bodies were zero in all three samples.
The installed training JAR was byte-identical to the newly built main JAR.

The earlier `StartupCoverage` implementation is now active in the actual running
production process: restored=true, 1,024 first-issued lessons, zero unobserved,
1,024 frontier and zero review/exam/foundation. Initial task counts were three
at task 11 and 1,021 at task 12. This diagnoses actual initial allocation; it is
not proof of retained crafting or learned cobblestone. A frozen model evaluation
is separately identified by its policy version and must not be inferred from
these running-process counters.

## Evidence retained on the designated workspace

Candidate worktree `.build/normalization-study/` contains all three initial
bundles, complete final training records, the pre-change full checkpoint copy
and the completed initial gate result. `.build/normalization-followup/` contains
all four final bundles, early/final full checkpoints, sample budgets and the
failed additional gate result. Its driver is `.build/normalization_followup.py`;
the initial driver remains `.build/normalization_study.py`.

The recorded gates remain in the candidate's
`docs/verification/20260926-normalization-gate.md` and
`20260926-normalization-followup-gate.md`. Archive validation is implemented in
`.build/audit_normalization.py`, with output retained in
`.build/normalization-artifact-audit.log`. Mutation checks remain in
`.build/normalization_mutations.py` and `.build/normalization-mutations/`.

`.build/normalization-rejected.bundle` was created and verified: it preserves
the three candidate commits through `c826847`, with existing public commit
`58ed06a` as its prerequisite. This is an archive of a rejected experiment,
not a pending production rollout. Candidate and baseline experimental learners
are stopped; only the deliberately restarted original main learner operates.

The live checkout's `.build/evidence/` retains
`normalization-production-restart.log`, `normalization_production_health.py`, and
`normalization-production-health.json`. The original production Academy remains
at `/home/coder/workspace/botsclustersmc-source/academy-current`.
No failed trial, complete checkpoint or prior world was erased.
