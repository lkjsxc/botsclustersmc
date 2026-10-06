# Review admission: short opportunity delivered, full qualification failed

Date: 2026-10-07 (Japan). **Offline scheduling evidence only. No new Minecraft
learning, frozen gameplay evaluation, model replacement or runtime adoption.**

Research [PR #60](https://github.com/lkjsxc/botsclustersmc/pull/60) used source
`3d6cd3ac69f47ffb2d8062a9cbd444f8705052b6`, tree
`4f20933b281ad5393e0b2029769ca9ff8c3d0f9e`. The
[fixed qualification protocol](https://github.com/lkjsxc/botsclustersmc/blob/3d6cd3ac69f47ffb2d8062a9cbd444f8705052b6/docs/verification/20261007-review-admission-qualification.md)
was committed and normally pushed before the new long replays. The older short
results were already known and explicitly labelled retrospective in that record.
The [machine-readable result](20261007-review-admission-results.json) preserves
all case outcomes, exact missing actor/task identities, traces, input identities,
independent-audit boundaries, source CI and live shared-Academy health.

## Decision

**Not qualified: do not start the proposed live-learning comparison or adopt this
runtime.** Candidate passed all eight short scheduling-opportunity cases, but
only seven of eight predeclared long cases. The matched control passed six of
eight long cases. Every long case completed its entire fixed budget; neither an
operational timeout nor an early termination caused a missing case.

The failed candidate case had a healthy aggregate late review share of
**20.01315%**, yet actor **136** had no observations of earlier tasks **1, 2 and 7**
by the 3,000,000-decision boundary. This violated the declared actor-by-task
coverage requirement. No extra budget, replacement seed, relaxed threshold or
new live learning followed the result. The research source and failure evidence
remain available; no experimental runtime belongs on main.

This is not a measured forgetting result, a proof of permanent starvation, or a
refutation of review admission in general. In every candidate scenario all actors
completed frontier work; even the failing actor completed five frontier episodes.
The actor-by-task screen is deliberately stronger than aggregate shared-policy
coverage. Its relevance to learned retention must itself remain explicit, not be
silently equated with a skill test. A future different coverage contract needs a
new prospective record, not a reinterpretation that makes this run pass.

## Implementation and lifecycle repair

The study limits each new frontier admission to
`max(1, floor(same-frontier enrolled training participants * 4 / 5))`.
At a fully enrolled population of 512 this permits 409 frontier episodes and
leaves 103 opportunities for review. A rejected frontier applicant selects its
own least-observed-time earlier task. Existing episodes are never interrupted;
therefore population shrinkage can temporarily leave occupancy above the new
bound. The bound is on admission, not every snapshot. Singletons are exempt from
a fractional reviewer requirement.

Membership begins at first training issuance and survives natural reset gaps.
Explicit abandonment or interrupted-gap withdrawal removes it. Decoded/unstarted
actors, active exams and peers waiting for mandatory exams cannot contribute
reviewer capacity. Neither membership nor the quota is observed time or earned
credit, and neither is serialized as a certificate or invented learning history.

The saved candidate initially excluded active exams but not mandatory-exam
waiters. A new regression test first demonstrated that an eligible peer counted
as available even though `issue` could only return null for that peer. Excluding
`eligible(peer)` repaired that mismatch without starting, waiving or altering its
exam. The failing test log is preserved. The corrected lifecycle/state-machine
test passed **93,530** checks, alongside **293,975** admission checks and the full
local source suite for the runtime/lifecycle changes. Native replay fixtures were
also compiled separately against both exact runtime JARs.

The unchanged per-actor 4:1 review-time account still accumulates actual debt.
This guard keeps opportunities available at a group level; it does not add a
per-actor deadline for completing an entire earlier-task review cycle.

## Matched identities and scope

The control is the previously rejected review-first research runtime, source
`7f18c3b83dd16dede2ff7f2337b3ab84465fe027`, **not ordinary accepted-main training**.
Both arms retain the stronger mining reward and task-12 critic-feature detachment.
Only `Course.java`, `ReviewEffort.java` and the interruption hook in
`TrainingPlugin.java` differ between their runtime sources. Core inference,
actuators, resets, rewards, gradient rules and model/optimizer initialization are
unchanged between the arms.

| Artifact | SHA-256 |
| --- | --- |
| Complete common parent / Adam / course | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Parent inference policy | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Control training JAR | `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9` |
| Candidate training JAR | `d30ef666d97ffa2b5de32729f4171b8bb4c5f81d4c4d431a55feb5f5d13a3430` |
| Byte-identical inference JAR in both arms | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |
| Historical duration CSV | `38268911c0a258c77fb5a17203b8434e3346923c5847202c3eed800ddcda8bcc` |

Both native parent checks passed 10,773 assertions and reproduced the same decoded
course hash and initial training distribution:
`[42,42,42,37,40,53,53,38,43,38,42,42,0,0,0,0,0,0]`.
They verified complete parent roundtrip, exported policy bytes, certificates and
zero invented observed effort. The older helper calls review-first semantics
`candidate` in its internal label, so both initial-check JSON rows have that label;
the enclosing filenames/runtime identities identify the actual comparison arms.

Exactly seven training class payloads differ: `Course`, `Course$Agent`,
`Course$Metrics`, `Course$Progress`, `Course$StageMetrics`, `ReviewEffort` and
`TrainingPlugin`. The first artifact preflight incorrectly expected an eighth
changed payload, `Course$Effort`, which was actually byte-identical. It stopped
before running any screen. Its expected set was tightened to the measured seven;
no additional class, runtime change or relaxed behavioral gate was permitted.
The original preflight failure and correction record are retained.

## Complete short and long matrices

The duration bank contains historical tasks 0-11, 32 cases per task, separately
for seeds 2026100611 and 2026100612. These are not fresh evaluation seeds. Four
scheduling cases use ascending order, descending order, half-speed even-numbered
actors, and synthetic one-decision reviews. The synthetic case intentionally
repeats across the two banks; these eight cases are not independent learner
replications.

Each short case used 250,000 modeled decisions. Candidate passed **8/8**;
control passed **0/8**, the declared negative-control contrast. Native checks were
254,290 and 22,106 respectively. The candidate preserved short review opportunity
while long frontier episodes were still unfinished. Assignment and simulated
observed-time counters are not accepted neural-training samples.

Each long case used exactly 3,000,000 modeled decisions, each with five actor-ticks.
Frontier episodes lasted 600 decisions. The late interval was the last 2,000,000
decisions. Every case needed 10%-40% late review, at least one frontier completion
per actor and at least one observation per actor/earlier-task pair. No synthetic
exam, promotion, regression reset or forced stage was used.

| Duration seed | Scheduling case | Control late review | Candidate late review | Control / candidate minimum actor-task count | Control / candidate pass |
| --- | --- | ---: | ---: | ---: | --- |
| 2026100611 | Ascending | 19.19830% | 21.70170% | 25 / 29 | yes / yes |
| 2026100611 | Descending | 19.19810% | 21.55930% | 25 / 28 | yes / yes |
| 2026100611 | Half speed | 21.10685% | 20.39610% | 0 / 15 | no / yes |
| 2026100611 | One-decision review | 19.41440% | 20.86000% | 88 / 88 | yes / yes |
| 2026100612 | Ascending | 19.30580% | 21.19095% | 14 / 30 | yes / yes |
| 2026100612 | Descending | 19.30510% | 21.28955% | 14 / 34 | yes / yes |
| 2026100612 | Half speed | 20.93050% | 20.01315% | 0 / 0 | no / no |
| 2026100612 | One-decision review | 19.41440% | 20.86000% | 88 / 88 | yes / yes |

The long native checks totaled 1,334,993 for control and 1,693,317 for candidate.
Both programs returned their declared failed-qualification exit after emitting
all eight complete cases. The primary qualification comprised 4 million short
plus 48 million long modeled decisions. No Minecraft learner was run.

Review can arrive in bursts after synchronized episode completions. Candidate
50k-window review reached 70.632%, and control reached 100% in the one-decision
stressor. The prospective protocol reported these extrema but did not apply the
short-window bounds to every long-run window. Do not conceal these bursts behind
a late average, or retroactively introduce an undeclared long-window gate.

## Post-hoc trace: aggregate capacity is not an individual coverage deadline

An instrumented, test-only copy replayed the same 48 million long decisions,
without changing the runtime, duration fixtures, random choices or budget. Every
primary JSON outcome, including all failures, matched exactly before and after
instrumentation. This was a diagnostic replay, not another learning replica or
an extension of the failed states.

The control missed earlier-task cells in nine half-speed actors for the first
bank and five for the second. Candidate missed only actor 136 in the second bank;
its tasks 1, 2 and 7 each had zero observations. That actor's final earlier-task
counts were `[14,0,0,16,55,25,14,0,10,7,13,600]`, with 3,152 frontier decisions and
five completed frontier episodes.

Actor 136 entered a 600-decision task-11 review with local credit 2,461. Its actual
3,000 review ticks reduced credit to -9,539. The group admission limit then caused
127 further decisions on other earlier tasks, reducing local credit to -12,079.
Once frontier admissions became available, four completed frontier episodes
raised credit only to -79. It entered another frontier episode before the fixed
observation boundary, while three previously unseen earlier tasks remained.

The selector still chose a least-observed-time task whenever it was offered a
review. The coverage miss arose from when review was offered relative to long
local debt repayment and actor speed, not from secretly changing earlier-task
selection or interrupting an episode. This diagnoses the modeled finite-window
mechanism; it does not prove permanent exclusion or predict frozen crafting
scores. Aggregate accepted-context coverage, actor-level opportunities and actual
skill retention must remain three separate measurements in the next design.

## Audit, source CI, archive and unchanged shared development

An independent parser reproduced every short-window numerator and decision, long
case identity, final actor/task marginal, completion minimum and missing-cell
trace. Its 127,864 checks passed. Ten mutation tests passed in both ordinary and
optimized Python execution, and both audit modes produced byte-identical results.
The long late-share values remain native report values: no separate late-boundary
actor matrix was emitted, so the auditor does not claim to independently derive
those numerators. This limitation is explicit in the machine-readable result.

Exact source CI run `37500883802`, attempt 1, passed Ubuntu, Windows and observatory.
The optional retention, live, Windows-live and Paper jobs were **skipped**, not
passed. Source tests and scheduling replays do not certify gameplay competence.

Raw evidence, including the red lifecycle test, original preflight rejection,
all failed replays, original and instrumented source, audit and both complete
runtime artifacts, remains in the research worktree's `.build/qualification/`.
The verified archive contains the full common parent and policy, 69 members and
1,777,684 bytes:
`complete-qualification-evidence-20261007.zip`, SHA-256
`c3513ac958e9e364da438df8201c5167c6d488f16e81fe7079e7c62c0cdce656`.
Manifest SHA-256:
`2feb9f508d33d7a4ce45487d0039938032a3589c7bb85d639bda0bdc19417b11`.
All archived entries were reread and checked against the size/hash manifest.

The shared Academy kept training JAR
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.
Its two recorded health snapshots had 1,482,661,197 then 1,482,955,659 accepted
samples. All 512 actors were active and progressing; inference failures/rejections,
learner rejected/stale samples and retired actors were zero. There was no shared
restart, installed-model replacement or learning-progress reset. This is operating
health, not a new frozen skill assessment of its moving policy.

The next question is a principled coverage contract under unequal actor speed and
long review episodes, not simply a different admission percentage or a longer run.
Preserve actual-time accounting, non-preemption and earned state while explicitly
separating the shared learner's context coverage from any required individual
review-cycle bound. Any changed mechanism or contract requires new qualification
and then a separately predeclared isolated learning comparison.
