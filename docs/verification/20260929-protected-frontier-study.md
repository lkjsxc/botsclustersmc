# Protected frontier study - 2026-09-29

## Prospective protocol (before Minecraft evaluation or continuation)

Base source: `aa9e6c72521e880f2e1d392067d1cc275e9c05a4`. This is a bounded
one-frontier architecture pilot, not a finished lifelong learner. No experiment
checkpoint is installed in the live development Academy. Preserve old evidence.

The preceding [parameter attribution](20260929-retention-attribution.md) localised
retention sensitivity to the shared trunk in one model pair. This pilot tests
whether explicitly protecting that old function permits continued frontier
learning without changing earlier-task behavior. It does not infer that changing
the representation alone must solve pickaxe crafting.

### Candidate and matched control

Both arms use the same current candidate implementation and the same exact
parent model, Adam moments/clock, course bytes and RNG states. The input is the
previous placement study's `input-training.bcmc`, policy 332305, samples 92,574,669,
checkpoint identity `495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855`.
Do not select a different checkpoint after seeing results.

Both arms update only on **task 11 (wooden pickaxe)** trajectories. Non-target
trajectories are excluded as whole episode fragments and counted separately,
not mislabeled as accepted learning samples or queue failures. The existing
course/reset/reward/action/observation/optimizer arithmetic is otherwise unchanged.
The control is **frontier-only finetuning**, not unchanged-mainline mixed-task
training. This distinction is necessary when interpreting the comparison.

The protected arm retains one immutable original policy shared by all snapshots
and uses it for every externally observed goal except task 11. A warm full-network
copy handles task 11 and receives gradients. Its initial actor and value function
is exactly the original function, including for task 11. The control uses the
same warm active copy and task-11 gradients but applies that changing function
to every goal. No teacher decisions, plan, pathfinder, recipe macro or extra
motor mask is introduced. Routing uses the already visible task ID, not hidden
success information. The retained anchor is an explicit model component, not a
fallback chosen after observing a failure.

Each decision executes one MLP, not both. Mixed batches pack lanes by selected
network and restore their original order. Immutable old weights are shared by
reference across learner snapshots; no 18-network bank is copied per update.
Both actor and critic learning operate only on the active copy, preventing the
critic loss from mutating the anchor through its trunk. This prototype supports
one fixed frontier only; it does not auto-promote, consolidate, add tasks, or
select its own goals. A successful pilot would motivate a generalisation, not
silently certify lifelong learning or complete cooperative survival.

A strict new `BCMC-FOCUSED-POLICY` envelope stores the route, active parameters and
optional anchor/counters. The runtime does not decode the previous envelope.
The offline `FocusExperiment` uses the explicitly trusted previous runtime in
an isolated classloader to read its own checkpoint, then constructs both new
states without changing moments or course bytes. It is absent from both runtime
JARs. Observation schema and action meanings are unchanged. Nested anchors,
invalid/missing/multiple goals and cross-goal training fragments are rejected.

This pilot is related in motivation, but is **not an implementation or replication**
of Rusu et al., *Progressive Neural Networks*, arXiv:1606.04671. That work retains
prior features through lateral connections. Here transfer is warm copying and
protection is explicit routing to the preserved source; there are no lateral
connections or dynamic progressive columns. See https://arxiv.org/abs/1606.04671.

### Fixed budgets and evaluations

Use two separately owned disposable Academies with 512 actors each, training
seed 7, 2 GiB maximum heaps, two Folia region threads and one inference/learner
thread per arm. Run the training arms sequentially to avoid competing builds in
the one candidate checkout; maximum one experimental learner at a time. The live
512-actor server remains running. Use loopback-only experiment ports, the existing
pinned server cache, and a 2 GiB available-memory floor. Each training segment has
a 30-minute operational cap. An operational failure is reported, not replaced by
an easier run. Never overwrite failed/partial outputs.

Before training, evaluate both the preserved base and protected initial policy
on ordered tasks **0 through 11**, **32 cases per task**, seeds **2026092941** and
**2026092942**. These are new layout/sampling seeds, not independent training
replications. All exams are full difficulty, ordinary resets, frozen stochastic
policy and zero new training samples. The unprotected initial actor is bit-equal
to base by the numerical oracle; it need not repeat the same baseline twice.

Stop each arm at the first observed boundary at or after **250,000 additional
accepted task-11 samples**, preserving the full stopped checkpoint. Permit at most
**50,000 samples of overshoot**. Evaluate both stopped arms on both complete
suites. If the protected arm fails its retention rule, stop and reject the pilot;
never conceal an early adverse result. Control retention loss is an experimental
outcome, not a reason to stop the protected arm or omit later control evidence.

Only after that early gate, resume each exact stopped state to **1,500,000 total
additional accepted task-11 samples** (same overshoot limit), then repeat both
full suites. At most four disposable evaluation servers run concurrently. Save
all reports/trials, start and stopped states, runtime/model identities, task
coverage, scope-skipped counts, accepted task counts and measured elapsed time.
Check that no non-target task enters the accepted sample counts.

### Prespecified decision rules

The protected arm must preserve the anchor weights byte-for-byte in initial,
early, final and restored checkpoints. Full-condition retention at each evaluated
boundary requires tasks 0-9 **at least 30/32 each**, and task 10 **no more than three
successes below the same-seed base**. Task 11 may not fall more than two below
same-seed base. Apply separately to each seed and boundary, including initial
protected evaluation; do not average away a failure.

A useful new-skill pilot additionally requires final protected task 11 **at least
4/32 on each seed**, and no more than two successes below final control. This low
feasibility floor is not mastery, certification, or sufficient to deploy the
one-frontier prototype. If both arms stay at zero, classify the result as
protection verified but new-skill gate failed, not a successful learning system.
No longer training, alternate seed, assisted reset or relaxed threshold rescues
the declared result. A successful score cannot hide a source/integrity failure.

No original course certificate is newly earned by construction. Any observed
promotion still uses the existing frozen exams; this pilot does not start learning
the promoted task. Continued multi-task growth is outside this fixed-frontier
experiment. No prototype is merged into production merely for preserving old
behavior by freezing it. Production integration needs a general usable learning
path and separate deployment checks; negative pilots remain closed experimental
branches with preserved source and evidence.

### Engineering prerequisites

Run all source tests and dedicated checks for exact initial functions over all
18 goals, unchanged prior functions through actor/critic Adam updates, nonzero
active gradients and finite differences, mixed scalar/batch equivalence, immutable
snapshot sharing, full checkpoint round trips, malformed envelope/route rejection,
whole-fragment sample accounting and offline/runtime separation. Recheck on the
real input that both active arrays, Adam states and course bytes are unchanged
by construction and the candidate's anchor equals the original policy.

Worktree: `/home/coder/workspace/botsclustersmc-protected-frontier` in
`lkjsxc/tomato-ocelot-73`. Full evidence will be retained in its ignored `.build/`
directory. This is local retention, not a public or durable off-machine archive.
