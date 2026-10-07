# Closed communal supply: complete learning comparison, gate failed

## Decision

Do not adopt this synthetic policy or claim that public demand plus a common
consumption reward has established reliable complementary supply. All three
independent learned trajectories failed the predeclared two-member and eight-
member gates on both evaluation seeds. No Minecraft runtime, model, reward,
Academy or service was changed or deployed in this work.

The useful deliverable is an executable closed-stock learning experiment and an
independent complete-trace checker, not another implied cooperative-skill pass.
Keep PR #72 draft as a research implementation and retained negative result.

## Immutable sources and study

Accepted starting source: `4734fa31413d40f742e3412ec5f5b91e377351fc`.
Prospective plan: `25a7d1b` (committed before implementation/training).
Frozen implementation: `4fc06837e29c2d1e3c02c10dcd5314ba2d297dbe`.
Tree: `796ed517ae7dfe94d3664cf75a2c1b0cf8c0a3a4`.
Worktree: `/home/coder/workspace/botsclustersmc-commons-learning-20261008`.
Branch: `study/commons-learning-20261008`.

The [prospective declaration](20261008-communal-supply-plan.md) fixed three learning
seeds, visible/hidden-demand arms, 600 update attempts, 256 offered transitions
per batch and all evaluation cases before any learning. All six arms completed:
921,600 total offered transitions, with 153,600 per arm. Checkpoints at 0, 100,
300 and 600 were evaluated at populations two and eight with two fresh fixed
scenario seeds and 256 cases each. All **24,576 frozen synthetic trials** completed.
No early failure was hidden, no seed was replaced and no budget was extended.
Exact accepted counts and optimizer identities are in the accompanying JSON.

## What actually ran

The environment uses the repository's real core `Pocket`, `Stack`, shift-click
and conditional action distribution. It is not a Bukkit chest or a Minecraft
server. Each member owns one private plank or stick, with exactly half the members
holding each material. Ownership and private slot placement are shuffled. The
single shared slot can hold only one material type, making an unwanted deposit
capable of obstructing a useful transfer.

A synthetic public consumer requests all available units of one material, then
all units of the other, in a balanced unpredictable order. It irreversibly consumes
a complete requested batch and returns one common reward to every member. There
is no intrinsic reward for deposits, withdrawals or the identity of the provider.
No material is injected after reset. Private stocks, common stock and actual
consumed units must conserve both materials after every serialized operation.

All actors choose from the same pre-action state. A separately keyed random order
then applies one literal no-click or shift-click operation for each actor. Mechanical
masks describe the current pocket/store only and do not encode the demand. The
actual Pocket operation rechecks stale feasibility. Actor observations contain
their own pockets, public store, public timing/population and, only in the visible
arm, the current demand. No actor ID, partner-private pocket, assigned occupation,
recipe controller or action labels are supplied to the learned policy.

The network, `Gradient`, `VTrace`, `UpdateGuard` and `Adam` code are unchanged.
This experiment collects synchronous fixed-policy batches; it does not exercise
the asynchronous Learner queue or establish joint-policy off-policy correction.
Every actor has a separate contiguous four-transition trajectory, a shared outcome
reward and true terminal only at the fixed final horizon. The synthetic input
identity is `synthetic-communal-supply-memory-only`; its dimensions are not a claim
of Minecraft observation compatibility. Models stay in memory, with no checkpoint
import or deployment-policy export.

## Complete final primary results

A primary success requires both material requests to finish within the first two
joint steps. Each cell contains two independently keyed evaluation sets, A and B,
with 256 cases each. A is seed 2026100881; B is 2026100882.

| Independent learning seed | Visible demand, 2 members A / B | Hidden demand, 2 members A / B | Visible demand, 8 members A / B |
| --- | --- | --- | --- |
| 2026100801 | 116/256 / 121/256 | 113/256 / 117/256 | 63/256 / 60/256 |
| 2026100802 | 117/256 / 126/256 | 117/256 / 123/256 | 77/256 / 67/256 |
| 2026100803 | 116/256 / 123/256 | 114/256 / 119/256 | 69/256 / 64/256 |

Visible two-member rates were 45.31%-49.22%, versus hidden 44.14%-48.05%.
The paired visible advantage ranged from 0 to 1.5625 percentage points, not the
required 20 points. All visible two-member results missed the required 90%.
Eight-member rates were 23.44%-30.08%, missing the required 80%. The declared
compound gate failed for every learning trajectory and both evaluation seeds.

The machine-readable report retains every earlier checkpoint and the separate
four-step completion, first-step service, wrong-material deposits, per-material
consumed/common/remaining stock totals. Detailed per-member transfers and exact
selected actions/application orders remain in the raw trial records. Do not
substitute a more favorable secondary endpoint for the declared primary endpoint.

This rejects the tested implementation/budget as a reliable demand-responsive
supply mechanism. It does not prove that shared rewards never work, that every
future policy architecture will fail, or that credit assignment alone caused the
failure. An optional post-hoc conditional-donation diagnostic was not completed;
no causal explanation is inferred from an unavailable diagnostic.

## Verification, not inferred execution

The frozen source passed the complete local Java/API/artifact-separation suite
with exit zero. The new native mechanics suite made 1,351,423 checks, including
262,144 exhaustively enumerated three-step two-member primitive histories,
permutation/private-view boundaries, stale withdrawals and circulation without
consumption reward. Additional software populations of 8 and 64 are not real-world
scaling results. Python report/lifecycle tests passed **57/57 normally and 57/57
under -O**, including nested optimized-interpreter corruption checks.

The independent Python checker does not call Java or Pocket. It reconstructs
integer slot transfers, keyed initial ownership, serial action order, pre-action
mechanical support, useful consumption and all material balances for every trial.
It rejects incomplete/duplicate case and update matrices, wrong scenario identity,
forged totals and changing frozen-policy identities. Both normal and -O full
replays exited zero and generated byte-identical result JSON. A runner completion
marker alone would not pass these checks. Recorded policy hashes bind native
metadata across a checkpoint; without exported weights, the checker does not
independently reproduce the neural action probabilities or authenticate arbitrary
fabricated files. It independently checks the supplied complete histories.

The initial source compilation failed because wildcard imports made `Policy`
ambiguous; the import was corrected before learning. An initial negative evidence
test tried to change a permutation entry to a value it already had. That invalid
negative fixture was corrected; the validator was not weakened. Both failures
and the subsequently successful fixed-source tests remain retained.

Both public artifacts rebuild byte-identically to the accepted parent and exclude
all synthetic study classes:

- Inference SHA256: `1e9c88cd12587f9a6944acc90169e819bd17db493e1be389178e3db4ee9cef36`.
- Training SHA256: `aae1fb27130b8c520dfc89f0de1cff7eab3c18febd95c043bfdef989e700f826`.

Persistent evidence is under this worktree's `.build/qualification/`:
`communal-supply/trials.tsv`, `communal-supply/updates.tsv`, the completion/identity
records, original study output, complete ordinary/optimized independent reports
and source-test logs. The committed JSON retains their relevant hashes and all
aggregate metrics. Raw evidence was not overwritten or pruned. Fresh exact-head
remote CI is tracked separately from local source and learning outcomes.

## Execution and deployment boundary

The initial combined request to inspect shared main/service/Academy state was
blocked by execution screening. It was not retried, split into smaller retries
or retrieved through another tool. This study instead used explicitly identified
immutable source files and a new source worktree. Existing Academy files, saved
models and service status were not obtained or modified for this study.

Later, an optional progress-log/first-step conditional-donation diagnostic was
also blocked. That diagnostic was not performed, retried or recovered elsewhere.
The previously started finite learning run and its already-declared complete
independent replay returned exit zero through their original execution session.
That completion did not retrieve the blocked tail/conditional-donation analysis.

No deployment, restart, runtime reset or claim of fresh shared-server health is
made. The previously verified deployment is historical context, not a substitute
for current live verification. PR #72 remains a separate research draft; its
synthetic policies are not suitable deployment files even if a later experiment
passes this small environment's learning gate.

## What this changes in the development decision

Public needs remain a sensible interface to investigate, but supplying those
inputs and a common reward is now measured as insufficient in this exact small
experiment. Increasing NPC count or simply announcing communal rewards would
not address the observed failure. The next hypothesis should isolate how actions
are credited and how the network conditions on public demand, while retaining
this common-reward baseline and independent learned trajectories. That is a new
prospective study, not permission to extend or relabel the completed budget.

The existing stock-only needs sidecar, physical commons exams, durable NPC state,
real-world public-store observations, useful continuous replenishment and learned
Minecraft cooperation remain separate work. This synthetic experiment does not
waive their gates or adopt any failed mining/rehearsal candidate.
