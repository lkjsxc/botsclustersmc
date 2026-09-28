# Conditional menu-focus study — 2026-09-28

Baseline: 5196232. Isolated worktree: botsclustersmc-menu-focus.
Production learning, checkpoint, world and service stay untouched.

## Prospective scope and gates

Opening inventory (operation 4) suppresses the six world-control heads in
WorldActions. The candidate marginalizes those unused choices rather than
learning their arbitrary values. It must consistently update canonical action
sampling, likelihood, exact joint entropy and its gradient, exploration-prior
cross entropy/gradient, and old-policy-weighted joint KL. Existing menu clicks
keep their conditional slot distributions. No reward, reset, curriculum,
success predicate, network shape, model schema or optimizer setting changes.

Source gate: independent exhaustive joint enumeration, meaningful finite
differences including the parent/world entropy derivative, empirical sampling,
unreachable-support boundaries, deliberate mutants, full source/API suite.

Behavior gate (declared before running): freeze ONE current production checkpoint
with the existing holdout harness; reuse its exact exported policy in all arms.
Evaluate ordered tasks 0 through 12, 32 full-condition cases each, case seed
2026092851. Baseline and candidate run sequentially, loopback only. No evaluation
learning. Retain every trial and both runtime identities.
On tasks 0-10 require candidate >=30/32 and at most two below baseline; on task
11 require >=24/32 and at most three below baseline. Record task 12 without
interpreting a zero as success. Repeat BOTH arms with the SAME saved policy and
fresh seed 2026092852 only if the first screen passes. Do not pool seeds or replace
a failed screen. A passing fixed-weight screen is not a post-training retention
result. No live activation or reward/learning experiment is authorized by it.

Failed candidates remain stopped and their evidence retained. No hidden action
script, tool selection, goal-conditioned masking, or forced menu closing is added.
The final decision must separate numerical correctness, fixed-policy competence,
and actual learning improvement. Production training remains unchanged unless a
separate prospective learning/rollout gate has completed.

## Implementation notes and interpretation

The old distribution is a valid distribution over attempted inputs, including
latent inputs ignored by the actuator. This candidate is a reparameterization,
not evidence that the old likelihood estimator was mathematically invalid.
For menu probability m and conditional world entropy Hworld, the new entropy is
H(menu) + P(not opening)*Hworld + sum_click P(click)*H(slot|click).
The menu gradient includes the derivative of the world-branch weight. The
exploration prior uses its fixed uniform-menu branch weight, not the learned
menu probability. KL uses the BEFORE policy's branch weight. Unreachable world
heads contribute neither score gradient nor entropy, exploration loss or KL.

At a fixed observation, marginal physical-action probabilities are preserved by
collapsing opening's unused inputs. Whole episodes are NOT bit-identical: the
sampler now draws menu first, and the previous-action observation uses canonical
idle world fields when opening. A requested edit to preserve the earlier random
draw order was blocked by the tool safety check and was not retried or routed
through another tool. The candidate retains its disclosed menu-first sampler.
Case seeds therefore identify reproducible arms, not pathwise-coupled actions.
The changed entropy/prior objective may change subsequent training behavior.

Numerical tests enumerate all 513 distinct tuples of a synthetic mixed-control
fixture, compare probability mass, entropy, KL and prior cross entropy, and
finite-difference the directly enumerated objective. They sample 40,000 tuples,
check canonical opening and unreachable support, and complement the existing
conditional click suite. The combined conditional suite reports 217654 checks.
The complete source/API/artifact-separation suite passed with source_test_exit=0.
Four separately compiled wrong implementations were each rejected by assertions:
unweighted world entropy, an opening world score gradient, missing parent/world
entropy derivative, and unweighted world KL. Compilation failure does not count
as a detected mutant. Run python3 tests/focus_mutations.py to reproduce them.
