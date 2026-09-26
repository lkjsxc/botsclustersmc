# 2026-09-26 - normalized hidden-layer cold-start study

## Scope and current decision

The operator explicitly permits training interruption and a fresh start. The
previous production learner was stopped normally and its full checkpoint and
world retained. Two new owned loopback Academies started from seed 7 without
any existing model, optimizer or curriculum. This is a comparison of fresh
learning, not a retention comparison against the earlier mature policy.

The normalization-only candidate is **not yet approved for deployment**. Its
minimum forward-stop screen passed, but the observed turning deficit triggered
an additional, stricter screen. Passing software tests does not override that
real-Minecraft deficit. No stone-harvesting or survival ability is established.

Baseline: main `58ed06a1abdd230751119e958b4546f94b333f08`.
Candidate execution implementation: `a4fc5df` (local Git commit); subsequent
`e1ae48cbbcf834fac55331ebb68edf8235d6c7c9` adds specification/schema tests and
`c82684754a0fe433e79ecb61975941573c4151c2` records the additional prospective gate.
The candidate is retained at `/home/coder/workspace/botsclustersmc-normalized`,
not part of the production implementation at this publication point.

## Intervention

Parameter-free layer normalization precedes each hidden tanh in the shared
512 -> 96 -> 96 policy. Each observation is centered and scaled independently
using its own hidden-unit population variance plus epsilon 1e-5. No running/batch
statistics or learned normalization gain/shift are introduced. The output layer
stays linear and the parameter count remains 68,842. The exact backward pass
includes derivatives through the hidden mean and variance.

The semantic schema changes to `bcmc-citizen-normalized-context`; old weights are
not valid merely because their tensor sizes match. Rewards, primitive actions,
observations, curriculum allocation, initialization arrays, optimizer settings,
full-condition trials and their success predicates are unchanged.

Normalization bounds saturation of hidden tanh outputs. It does not bound the
full network Jacobian away from zero or guarantee plasticity. The normalization
inverse scale can shrink as weight norms grow. Weight projection, weight decay,
recurrent memory, task experts and scripted gameplay are not bundled into this
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
normally. The actual accepted counts include that bounded stopping overshoot.

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

This is an additional prospective engineering screen prompted by the initial
result, not a preplanned statistical superiority test. The original forward-only
screen is preserved rather than retroactively rewritten. Final continuation
results and the operational decision will be appended only after verification.

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

A detached clean checkout at `c826847` rebuilt inference and training JARs that
were byte-identical to the candidate worktree's builds. The clean-checkout source
test log is retained separately. These checks establish software properties,
not learned stone mining, open-world survival or cooperation.

## Evidence retained on the designated workspace

Candidate worktree `.build/normalization-study/` contains all three initial
bundles, complete final training records, the pre-change full checkpoint copy
and the completed initial gate result. The original production Academy remains
at `/home/coder/workspace/botsclustersmc-source/academy-current`.

The additional experiment uses `.build/normalization-followup/`; its driver is
`.build/normalization_followup.py`. The initial driver remains separately at
`.build/normalization_study.py`. The recorded gates are in the candidate's
`docs/verification/20260926-normalization-gate.md` and
`20260926-normalization-followup-gate.md`. Archive validation is implemented in
`.build/audit_normalization.py`; mutation checks and their results are retained
in `.build/normalization_mutations.py` and `.build/normalization-mutations/`.
No failed trial or prior world was erased.
