# Resume coverage experiment - predeclared gate, 2026-09-26

Status: candidate only; no rollout approval is implied by source tests.

## Observation and hypothesis

Canonical checkpoint 662907 / 310322493 samples is intact: a frozen replay under
the original training runtime achieved 64/64 log breaks, 64/64 pickups, 57/64
wooden pickaxes and 0/64 cobblestone. Under the temporary stone intervention the
same original checkpoint gave 64/64, 64/64, 56/64, 0/64 without learning. Actual
server scheduling is not perfectly deterministic even with fixed random seeds.

After resuming the original code, only 79448 accepted training samples later,
a frozen checkpoint achieved 64/64, 64/64, 14/64, 0/64. Thus the earlier failed
stone intervention is not established as the sole cause of retention loss.

A read-only decode of the real 1024-actor checkpoint selected 1024 frontier
lessons, zero reviews and zero exams for its first lessons (23 wooden-pick
actors and 1001 cobblestone actors). Review scheduling debt is not checkpointed.
A long initial frontier episode can leave prior skills absent from updates.

Hypothesis: a bounded review cohort from the first restored lessons can reduce
restart-induced retention loss. This is a scheduling experiment, not proof.

## Candidate and isolation

One actor in five with a nonzero frontier receives a transient initial review
budget of 1..one frontier horizon of actual ticks, uniformly drawn from a private
phase RNG without advancing the persisted lesson RNG. The existing controller
charges four credit units per observed review tick. All other restored actors,
fresh foundation training and promotion/regression resets retain their old
allocation. No reward, success predicate, exam denominator, mask, network,
optimizer, observation, actuator or saved byte layout changes.

Bootstrap credit is not recorded as actual ticks, samples, episodes or success.
This credit is bounded and consumed; ordinary 4:1 observed-tick allocation
continues afterwards. It is not exact restoration of the old scheduling debt.

Only a new loopback-bound, owned Academy copied from the complete known-good
checkpoint may train this candidate. The live Academy, original checkpoint and
failed intervention archives must remain untouched.

## Gate fixed before candidate learning

Use 1024 actors and the full known-good canonical checkpoint 662907 /
310322493 samples, preserving model, Adam and curriculum. Record initial source
commit, sampled model versions and exact accepted-sample budgets.

Early diagnostic: freeze after approximately 80000..200000 accepted samples.
Final gate: at least 1000000 additional accepted samples, not elapsed time.
At both checkpoints evaluate tasks 5,6,11,12, 64 cases each, seed 2026092611.
The final checkpoint must reach at least 62/64 in both log tasks and at least
52/64 wooden pickaxes. No cobblestone ability is claimed without actual success.
Early failure is reported, not silently replaced with a later favorable snapshot.

Before rollout, test the final frozen weights on tasks 0..12, 32 cases each,
fresh seed 2026092613. Require at least 30/32 on each task 0..10 and 26/32 wooden
pickaxes. Also compare the same weights against the earlier broad reference
seed 2026092612 when feasible. Evaluation must add zero training samples.

These are prospective engineering screening thresholds, not a statistical
proof of long-term retention. A single passing run is not a randomized causal
ablation. If the candidate fails, keep diagnostics and the failed evidence but
do not deploy it or relabel failure as progress.
