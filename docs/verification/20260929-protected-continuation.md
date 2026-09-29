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

## Outcome: retention observed, new-skill gate failed

The complete experiment finished on 2026-09-29. Its prospective source and
protocol were published as `e1f8e6567116c02e0af18e64a466f7e0289e5a14` before
training. The section above is retained byte-for-byte. This closeout re-audited
existing saved evidence; it did not rerun Minecraft or add training samples.

All **12 reports / 18,432 ordinary frozen-policy trials** completed. Protected
retention passed at initial, early and final boundaries on both declared seeds.
The final protected workbench scores were **119/128 and 117/128**, matching the
same-seed source counts; the unprotected control scored **37/128 and 39/128**.
The control's earlier 7/128 and 8/128 workbench failures are retained below, not
hidden by its somewhat better final results.

Every model/seed/boundary scored **0/128 on ordinary wooden-pickaxe completion**.
Consequently both final acquisition gates failed the required 16/128 floor.
This is **observed retention with failed acquisition**, not a successful learning
system, a passed mastery test, an operational failure, or evidence that additional
identical training will solve the next skill. Do not merge or deploy PR #36's
fixed-frontier runtime. Keep its source and complete states as experimental evidence.

### Complete task scores

Each vector is ordered tasks **0 through 11**, every entry out of **128**.
Seed A is **2026092961**; seed B is **2026092962**.

```text
Boundary  Model       Seed  Scores (tasks 0..11)
Initial   Source      A     128 128 126 128 128 128 128 128 128 128 119 0
Initial   Source      B     128 128 128 128 128 128 128 128 128 128 117 0
Initial   Protected   A     128 128 126 128 128 128 128 128 128 128 119 0
Initial   Protected   B     128 128 128 127 128 128 128 128 128 128 118 0
Early     Control     A     128 124 124 126 128 128 128 126 119 128   7 0
Early     Control     B     128 126 128 127 128 128 128 128 118 127   8 0
Early     Protected   A     128 127 126 128 128 128 128 128 128 128 119 0
Early     Protected   B     128 127 128 128 128 128 128 127 128 128 117 0
Final     Control     A     114  79 120  86  92 128 128 123 128 128  37 0
Final     Control     B     109  69 123  96  98 128 128 120 128 128  39 0
Final     Protected   A     128 128 126 128 128 128 128 128 128 128 119 0
Final     Protected   B     128 128 128 127 128 128 128 127 128 127 117 0
```

The protected final minus source differences were all zero on A and minus one
on tasks 3, 7 and 9 on B, with all other differences zero. The independent
calculation checked every task at every boundary; no average substituted for a
failed task/seed. Identical functions need not produce identical asynchronous
server trajectories. These two evaluation seeds are not independent training
replications, and the descriptive eight-success margin is not a statistical
noninferiority guarantee.

### Training and state integrity

Additional accepted samples are relative to the original **92,574,669**. Segment
durations are measured runner durations, not a compute-normalized benchmark.

| Arm | Boundary | Additional accepted task-11 samples | Saved total samples | Saved updates | Segment seconds |
| --- | --- | ---: | ---: | ---: | ---: |
| Control | Early | 254,721 | 92,829,390 | 333,078 | 151.107 |
| Protected | Early | 263,208 | 92,837,877 | 333,170 | 155.239 |
| Control | Final | 1,503,741 | 94,078,410 | 337,968 | 820.532 |
| Protected | Final | 1,505,326 | 94,079,995 | 337,827 | 786.462 |

All four saved states met their declared sample boundary and 50,000 overshoot
limit. Each final segment resumed the exact corresponding early checkpoint,
including optimizer and course state. Initial active parameters, Adam state,
course and forward functions matched the original source. Continued active
parameters changed; the protected source weights and counters did not. Both
actor and critic use that fixed old-function boundary for retained goals.

All 375 captured training-status records had zero non-task-11 accepted counts
and reported no inference failure/rejection, learner rejection/stale samples or
retired actors. The four segments captured 28, 28, 163 and 156 records respectively
when ordered control-early, protected-early, control-final, protected-final.
Each started with 512 task-11 actors. Scope-skipped non-target fragments are not
accepted samples or queue failures. Last observed task counts can precede the
stopped checkpoint's final flush; the table reports the saved counts rather than
rounding or substituting the last status poll.

Both arms used the same inherited partial-grid training resets. Neither the
curriculum/reset code nor the reset-sampling rule changed between this prototype
and its mainline base. This comparison does not independently validate those resets;
ordinary evaluations contained no assisted reset. The control is task-11-only
finetuning, not unchanged-mainline mixed-task continuation.

### Post-hoc failure localization, not a new decision rule

The read-only trace summary inspected all **1,536 task-11 trials** already in the
12 reports. No report had a pre-action target pickaxe preview. In the final
protected reports, 62/128 and 68/128 cases had an observed open workbench. Their
maximum-correct-cell histograms, ordered zero through five correct cells, were
`[114,12,2,0,0,0]` and `[108,18,2,0,0,0]`. Only two cases per seed reached two
correct cells; none reached three. This places the observed failure before
output collection, at entering and assembling the recipe, rather than showing
an acquired recipe that merely was not collected.

The final protected conditional probability of filling the empty upper-handle
cell when the cursor held its matching ingredient averaged about **0.0000634**
and **0.0000710** in the observed states. These probabilities are descriptive,
state-weighted within-trial diagnostics, not independent samples, randomized
causal interventions, or a new acceptance threshold. Correct-cell maxima include
post-action states and do not rule out extra wrong cells; preview/open counts
refer only to pre-action observations. Neither metric certifies completion.

The next useful learning study should keep the now-tested retention boundary
while testing why primitive material-selection and slot-placement do not become
reliable. It must distinguish learning a placement action from retaining it,
and success on assisted starts from composing an ordinary complete recipe.
An observation/action representation or exploration change must first preserve
old behavior, declare its full-condition acquisition budget and compare matched
controls. More actors, a longer identical run, a recipe mask or a teacher macro
would not answer this result's remaining question.

### Closeout validation and publication boundary

The independent read-only auditor rechecked all report/trial identities,
denominators, immutable evaluated policies, declared runtime identity, exact
resumes, all four complete stopped checkpoints and the original input files.
It recalculated retention and acquisition decisions without new game trials.
The current mainline strict JSON/trial validator also accepted all 12 reports.

During this closeout, a separate developer-validator gap was reproduced:
`verify_report` accepted missing/empty pickaxe diagnostics and even a trace with
`max_correct_cells: 9000`. The regression first failed seven expected checks.
The fix requires every pickaxe trace field for tasks 11 and 13, rejects traces
on other tasks, validates all 32 mask bins and five cell denominators, cursor
unions, finite probability sums and pre/post-action accounting. It never changes
success labels or supplies information to the policy. Six added unittest methods
bring `test_holdout.py` to **29 methods**, including real `-O`, `-OO` and
`PYTHONOPTIMIZE=2` subprocess checks.

The strengthened validator accepted all **18,432** continuation trials and a
separate **16-report / 1,216-trial** retained pickaxe diagnostic set: 768 ordinary
trials and 448 explicitly assisted trials, including 75 cases with observed
pickaxe previews and 67 assisted completions. These are revalidations of existing
records, not newly run evaluations or new ordinary successes. All original raw
reports remain unchanged.

The implementation, protocol, runner and runtime remain on experimental commit
`e1f8e6567116c02e0af18e64a466f7e0289e5a14`. Mainline receives this result and the
developer evidence-validation fix, **not** the focused model, training code,
experimental weights, optimizer, curriculum, or assistance. The live Academy
was not reset or replaced. Full raw evidence remains local, not a durable
public/off-machine archive.

### Reproduction and retained evidence

At `/home/coder/workspace/botsclustersmc-protected-continuation/`:

- `.build/continuation-study/`: declaration, all phase reports/trials, complete
  input/stopped states, training status records, gates and completion record.
- `.build/focus-input/`: original input, trusted source runtime and both initial
  policies/checkpoints.
- `.build/independent-audit.json` and `.build/independent-trace-summary.json`:
  the completed read-only closeout audit and post-hoc diagnostic summary.
- `.build/continuation_audit.py` and `.build/continuation_trace_summary.py`:
  the local auditors. The prospective runner and Java checkpoint auditor are
  tracked in `tests/studies/` on the experimental source, not mainline.

The four stopped checkpoint identities, in the order used in the sample table:

```text
control early    2763b2f7f780162ac1f1dd73d4fb8703b5c36e6a3ce0ad9a445cf949f9cfe5a2
protected early  bb5bbd75e0946cb18a79ffa368a71602ebc30ba264b24997bd43abb20e7d2680
control final    3d528866fac5aa97f479213bd0797d1f98e988b001e3413dfd1c0c95d15f0e87
protected final  7b823c07820522c2f76792b3d829a96a9d805f2bb2679bbecb44fca5ef58c30b
```

The immutable JSON result identities, ordered exactly as the complete score table:

```text
9c8a21182c085b793da5a21e52e3d7943c30b0de5b3f95c323465afcba22836b
ae2569841be3847bd59fea269370e4736c404cbc06090909a7e4c7bceec00192
24af5ee24a49567ac4907cfde7993350cb56005600ff93edcd409cee7f48735c
b556872515b603c7beb071e652f4d9a30705730db61c2a4423fac8bb94a5656a
90cf83e2c8c353c023f700c88eefd669e8b03a90e27b6d8d407cf1b2067a3e34
dd285774ca67974021acaba80caaf135a3c9eef44fa46d7e96e6f990021d796e
c3557656000fa8f0dc9747d4c0142c32b120949a13e8f6359abc908f0308d25c
c0f9e35476e271a1c28ca1691350072b02d8c6cd092686a4f28f68363045baff
7cdce67278c4c74cbff17ace090fd3ca5891201420d5963ba3bd0e4e38fe9b76
e63a99206b310477a0e545b83ea9eccd7cc6a9aa89b2ec40b130698b06efa2c1
789f368beecba74595b87b792837381c1f35e3ed32fa5dadbfb46039b8da6fdc
02d68884158938b27c6b0d16fccf7f633be971a8ed6b3d6ec3135b4a0afd6e6f
```
