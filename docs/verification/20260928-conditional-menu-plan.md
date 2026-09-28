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
