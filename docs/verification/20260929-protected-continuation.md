# Protected continuation of an imperfect input - 2026-09-29

## Prospective protocol

This is a **new relative-retention experiment**, not a revision, rescue or
completion of the stopped [absolute-qualification pilot](20260929-protected-frontier-study.md).
That pilot's failed initial gate remains unchanged. Here the question is whether
continued task-11 learning preserves the measured behavior of one specified,
imperfect source policy, and whether the new skill improves. No absolute mastery
qualification is required or claimed.

Use the unchanged one-frontier implementation from
`61d0aed5cd1e091f72c389c77e16faf6ce071337`, inherited through result-only
`f6529e293403394d7ee2e378069d918064d0f0e0`. This experiment adds a tracked runner,
checkpoint auditor and decision-rule tests. It does not change the network,
learning arithmetic, rewards, curriculum, resets, action meanings or runtime
artifacts of that prototype. The implementation is experimental and is not the
mainline runtime. Do not install it in the shared live Academy.

### Fixed input and comparison

Retain exactly the earlier source checkpoint: policy **332305**, **92,574,669**
accepted samples; checkpoint identity
`495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855`.
The previously constructed control and protected initial checkpoints retain its
active weights, Adam moments/clock, course and random-generator state. Re-audit
those facts against the saved original runtime before evaluation. Do not select
a newer or more favorable checkpoint after seeing results.

Both arms learn only task 11 (wooden pickaxe); whole non-task-11 fragments are
counted as scope-skipped, not accepted training. The **control is frontier-only
finetuning**, not mainline mixed-task training. Its active copy answers all goals.
The protected arm uses the identical warm active copy for task 11 and a shared
immutable source policy for every other observed goal. Actor and critic updates
cannot modify that anchor. Each decision executes one network. Neither arm
receives teacher actions, additional gameplay masks, assisted evaluation resets
or supplied crafted outputs. Routing is the existing observable task ID.

This design changes the scientific question and evaluation precision, not the
stopped pilot's rule. The input is knowingly imperfect. Positive results would
support bounded relative retention and new-skill feasibility for this input,
not an absolute skill certificate, general lifelong learning or cooperation.

### Samples, cases and stopping

Before any new evaluation or training, commit and publish this protocol and the
runner. Evaluate ordered tasks **0 through 11**, **128 cases per task**, on fresh
layout/sampling seeds **2026092961** and **2026092962**. These are not two
independent training replications. All tests use full difficulty, ordinary resets,
stochastic frozen policies and zero training samples.

First evaluate source and protected initial policy on both complete suites.
The initial unprotected copy is checked exactly by the forward-function auditor,
rather than duplicating identical initial-function tests on another server.

Then run the control followed by the protected arm to **250,000 additional
accepted task-11 samples**, stopping and preserving each complete checkpoint.
Allow at most **50,000** overshoot at each boundary. Evaluate both stopped arms
on both complete suites. Apply the protected relative-retention rule immediately;
a failure ends this study, preserving all early adverse results.

Only if that rule passes, resume each exact stopped state to **1,500,000 total
additional accepted task-11 samples** and repeat both complete suites. No extra
training, checkpoint substitution, alternate seed or changed rule rescues a
failed gate. Control deterioration is reported but does not itself stop the
protected arm. Preserve all 12 reports and **18,432 trials** if all phases finish;
otherwise report the actual completed subset and the reason.

### Separate integrity, retention and growth rules

Every checkpoint must retain the declared route and exact protected anchor
weights/counters. Initial active weights, moments, optimizer clock, course and
forward functions must equal the original. Continued active weights must
actually change, with increasing accepted sample counts. No non-target samples
may enter the accepted per-task counts. These are integrity requirements, not
evidence of learned competence.

At baseline, early and final boundaries, the protected score on **each task and
each seed** may be at most **8 successes out of 128** below the same-seed source
score. This 6.25-percentage-point descriptive screen is **not a formal statistical
noninferiority test**, nor does passing it imply mastery. Exact old-function
preservation and ordinary rollout scores are reported separately; asynchronous
server trajectories need not match even with identical functions.

The final new-skill gate additionally requires protected task 11 to reach
**16/128 on each seed**, and to be no more than **8/128 below the final control**
on each seed. This retains the earlier pilot's 12.5% feasibility floor; it is not
mastery. If both arms remain at zero, report protection and failure to acquire
the new skill separately, not a successful learning system.

### Operational and publication boundaries

Worktree: `/home/coder/workspace/botsclustersmc-protected-continuation`.
The runner is `tests/studies/protected_continuation.py`; run its companion
`test_relative_screen.py` first. `FocusAudit.java` compares complete state and
explicit arm identity. The input directory is `.build/focus-input/`, including
the original trusted runtime as `source-runtime.jar`. Evidence is written
exclusively into a new `.build/continuation-study/`; existing output is never
overwritten. The prior pilot's evidence is not modified.

Use one experimental learner at a time: 512 actors, 2 GiB maximum heap, two
Folia region threads, one inference worker and one learner worker, training seed
7. Use loopback ports 30851/30852 and the existing pinned server cache. At most
two disposable evaluators run concurrently on ports 30860-30863. Maintain a
2 GiB available-memory floor. Each training segment has a 30-minute operational
cap; an operational failure is not a negative learning result and is not silently
rerun. Record runtime identities, samples, coverage, scope skips and elapsed time.

Candidate runtime identities must remain the previously validated values:
training `49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9`,
inference `d7506a46aa63abd69410d0632e6cb0f57d360c7aac2f9d6451161caa0eb01239`.
Original trusted runtime:
`7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc`.

Keep the live mainline server running independently. No result automatically
authorizes merging or deploying this fixed-frontier runtime: it has no general
next-task learning path. Publish negative as well as positive results, including
all task scores and failures. Full local files are not an off-machine archive.
