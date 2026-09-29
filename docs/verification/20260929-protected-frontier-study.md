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

## Outcome: stopped at initial qualification; no continuation performed

Implementation and prospective protocol were committed and published in PR #35
as `61d0aed5cd1e091f72c389c77e16faf6ce071337`, tree
`ea62b234abbc699a8882fdfaa1c04b712031b4ae`, before the real-server evaluations.
The preceding prospective section was not edited after seeing scores.

All four planned baseline reports completed: **1,536 frozen trials**, no missing
trials and **zero new training samples**. The baseline gate failed on seed A
because task 2 (aim-hold) scored **29/32 in both the original and protected
models**, below the declared 30/32 floor. Seed B passed. Consequently neither
experimental training Academy was started. The 250,000- and 1,500,000-sample
segments, their stopped checkpoints and their continuation evaluations do not
exist. No final new-skill result is claimed and no alternative seed was tried.

This is a **failure to qualify the initial state under the declared rule**, not
observed forgetting caused by candidate learning: no candidate learning occurred.
The same source model also failed the absolute floor on the same seed. The JSON
field `retained: false` records the predeclared decision rule, not a causal claim
that the model lost ability. The result does not reject the general protection
mechanism, and it does not validate new-skill learning with this prototype.

### Complete measured scores

Each entry is a success count out of **32**, ordered tasks **0 through 11**.
Seed A is **2026092941**, seed B is **2026092942**. No assistance was used.

```text
Model                Seed A                                  Seed B
Original base        32 31 29 32 32 32 32 32 32 32 26 0      32 32 32 32 32 32 32 32 32 32 28 0
Protected initial    32 32 29 32 31 32 32 32 32 32 27 0      32 31 32 32 32 32 32 32 32 32 28 0
```

Workbench completion was 26/32 and 28/32 in the base, versus 27/32 and 28/32 in
the protected initial model. The workbench-relative rule passed on both seeds.
Ordinary wooden-pickaxe completion was 0/32 for every model/seed. The stop was
specifically the absolute aim-hold floor on seed A, not a workbench retention
failure or a tested failure of the final pickaxe-learning gate.

Initial actor/value functions are exactly equal for identical observations, but
separate real-server runs can produce different trajectories. The full task
score differences (protected minus base) were
`[0,+1,0,0,-1,0,0,0,0,0,+1,0]` on A and
`[0,-1,0,0,0,0,0,0,0,0,0,0]` on B. Do not describe these runs as identical gameplay,
or interpret one-success differences as learned improvement.

### What the implementation checks establish

The full local source suite passed. Dedicated checks executed **10,771,320
assertions** covering exact warm initialization, retained actor/value functions
during 48 synthetic active-copy Adam updates, nonzero active changes, inherited
moment/variance arithmetic, active-network finite differences, all 18 goal routes,
mixed-task scalar/batch equivalence, variable batch sizes and lane order, immutable
anchor sharing, strict envelope rejection, complete checkpoint round trips and
whole-fragment sample filtering. This count includes repeated per-parameter
assertions; it is not ten million independent experiments or gameplay successes.

Both actor and critic output gradients were exercised while the anchor stayed
unchanged. All 48 published snapshots shared the **same anchor object**. Their
49 distinct parameter arrays (48 active snapshots plus the anchor) held
13,493,032 raw float-payload bytes, excluding object/array headers, Adam arrays,
workspaces and game-server memory. This is a structural accounting check, not
measured peak process memory or a scalability benchmark.

A separate cross-classloader check used the actual retained checkpoint and its
original trusted runtime. For each arm it checked **288 original/new forward
functions** spanning all 18 goals and open/closed action masks. Active weights,
Adam first and second moments, optimizer clock and course bytes matched the
original state exactly. The protected arm additionally kept the source policy
as its anchor. Model payloads were **275,499 bytes** for the unprotected control
and **550,883 bytes** for the protected initial policy. No historical certificate
or training progress was invented by the offline construction.

The new policy envelope is deliberately incompatible with the old envelope;
observation/action semantics are unchanged. Neither runtime JAR contains the
offline construction tool. Existing goal/block counterfactual utilities reject
focused inputs rather than silently dropping their protection metadata.

PR CI **36514098201 / attempt 1**, on the exact implementation head, passed
Ubuntu source job **109232442110**, Windows source job **109232442136** and
observatory job **109232441967**. Optional live, retention, Paper and Windows-live
jobs were skipped, not passed. The four real Folia evaluations above were the
separate local study. A local boundary checker also passed **26** prospective
retention-gate tests, including one-seed failure and control deterioration that
must not incorrectly stop an otherwise-qualified protected arm.

### Decision and next evidence requirement

**Do not merge or deploy the one-frontier runtime.** Preserve the prototype,
prospective rule, exact inputs and all baseline trials, and publish this result
on main. The prototype has implemented and numerically tested the protection
boundary; its real-server learning and post-update skill retention remain
**untested** because the initial qualification failed.

The important limitation in this pilot design was an absolute per-task floor
that the selected parent itself did not satisfy on the new test. Previously
favorable 32-case scores were not a guarantee of qualification on fresh cases.
Do not lower this pilot's floor, substitute its favorable seed, or retrospectively
report it as a successful continuation experiment.

A future continuation study must distinguish **input qualification**, **relative
retention during updates**, and **new-skill acquisition** before running it.
An absolute competence requirement needs a prospectively qualified input; a
study of retaining an imperfect input needs an explicitly declared relative
question. Larger, prespecified evaluation sets can reduce sensitivity to one or
two sampled outcomes, but are not a licence to reclassify this stopped pilot.
The fixed old-function boundary remains a viable implementation hypothesis;
warm copying alone has not been shown here to learn pickaxe crafting.

Even exact old-policy output preservation is not a guarantee of success after
world-state distributions or other agents' behavior change. The eventual shared
world still needs ordinary task and cooperative rollout evaluation; parameter
invariance is only one engineering prerequisite.

### Artifacts and operating boundary

Candidate training JAR:
`49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9`.
Candidate inference JAR:
`d7506a46aa63abd69410d0632e6cb0f57d360c7aac2f9d6451161caa0eb01239`.
Original trusted runtime used only by the offline constructor:
`7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc`.

The worktree retains `.build/focus-input/` (source checkpoint, both candidate
checkpoints/policies, recoded base policy and input manifest),
`.build/focus-study/evaluation-baseline/` (four complete reports, individual trials,
worlds, logs, metadata and receipts), `.build/focus-study/declaration.json`,
`baseline-gate.json` and `rejected.json`. The independent auditor verified every
trial denominator/identity, immutable evaluated policies, no generated training
checkpoints, input-file identities and the gate calculations. Its output is
`.build/focus-audited-summary.json`. Runner, state auditor, gate tests and result
auditor remain in `.build/` beside the source-suite transcript.

The previous study's source checkpoint remains unchanged. The production Academy
received no candidate JAR, focused state, reset or service restart. No production
skill change is attributed to these frozen trials. Full raw evidence is retained
locally, not represented as an off-machine archive.

### Mainline reporting boundary

PR #35 was closed **without merging** at result-only head
`f6529e293403394d7ee2e378069d918064d0f0e0`. The experimental runtime remains on
`study/protected-frontier-20260929`; reproducing its constructor or policy format
requires that experimental checkout, not main. This mainline record and the
corresponding `COOPERATIVE_SURVIVAL.md` clarification do not install or expose the
focused-policy implementation. The original prospective protocol and all 48
published task scores were mechanically compared with the committed declaration
and retained reports before publication.
