# Pickaxe storage-to-placement practice study — 2026-09-29

## Candidate and prospective decision rule

Control source is `192ec5f66cf16d40906265465f53958237186e04`. The candidate
changes only `InitialCrafting.prepare` for wooden/stone pickaxe practice with
0 < difficulty < 1. Half of these resets leave exactly one uniformly shuffled
recipe cell missing, with the remaining raw unit in its original storage slot
and an empty cursor. The other half retains the original frontier/earlier-start
mixture, including ready-output collection and cursor assistance. Difficulty zero
is unchanged. Full-difficulty resets, probes/exams, rewards, optimizer, task
allocation, horizons, observations, masks, motor actions and inference are unchanged.
No policy actions are prescribed; all gameplay remains neural-policy controlled.

This tests a narrow hypothesis suggested by the preceding five-cell diagnosis:
more storage-to-placement exposure may improve completion from every missing
position without damaging retained skills. It does not assert that the existing
curriculum is defective or that assisted completion is natural skill mastery.

The following plan is declared before copying the current production checkpoint
or running any candidate training. Freeze one canonical `training.bcmc` once,
including policy, optimizer, course, actor RNG and earned certificates. Both arms
start from exactly those bytes in separate owned Academies; never train the
production Academy with the candidate. Keep the input, all intermediate/stopped
states, failures and trial reports. Source commits and artifact identities are
recorded in the local manifest before training.

- Use 512 actors, training seed 7, 2 GiB Java heaps, two region threads, one
  inference thread and one learner thread per arm. Bind experiment ports only to
  loopback. Keep the existing pinned server. Abort below a 2 GiB available-memory
  floor. Run at most two training arms and four evaluation servers concurrently.
- Evaluate the frozen parent on ordered full-condition tasks 0 through 11, 32
  cases each, seeds **2026092911** and **2026092912**. These are new evaluation
  seeds, not a claim of independent training replications.
- Stop each arm after **250,000 additional accepted samples** (at most 50,000
  overshoot), preserve its complete checkpoint, and repeat both complete frozen
  full-condition suites. Inspect first-issued task coverage and retain both early
  results. If either arm fails the retention rule below, reject the candidate and
  do not continue the planned final phase. Do not hide an adverse early model.
- Only after passing that gate, resume the exact stopped states to a total of
  **1,500,000 additional accepted samples** per arm (same 50,000 overshoot bound).
  Repeat both full-condition suites and all five single-missing-cell diagnostics,
  task 11 only, 32 cases per position, on both seeds for both final models.
- Retention requires every task 0–9 to score at least 30/32, task 10 no more than
  three successes below the same-seed parent, and task 11 no more than two below
  the parent. Apply this to each arm, each seed, early and final; never average
  away a weak retained task.
- Candidate adoption additionally requires, **on each seed separately**, at least
  32 more single-cell completions than control out of 160, at least 4/32 in
  **every** missing position, task 10 no more than three below control, and task 11
  no more than two below control. Report all positions. The five positions are
  top-left, top-center, top-right, upper handle and lower handle.

These are prospective engineering decision thresholds, not statistical
significance guarantees. Success would establish only a useful intermediate
practice intervention on this one resumed model. Ordinary pickaxe skill still
requires ordinary, unassisted results. No certificate, export gate or success
predicate is relaxed. A failed candidate is not deployed; preserve its evidence
and publish the negative outcome instead.

All evaluation uses the same unchanged control runtime and unchanged holdout
implementation, with fixed policy files and zero new training samples. The only
candidate runtime change is practice-only reset sampling; it is not exercised by
ordinary frozen EXAM resets. Every assisted report retains its diagnostic label
and remains ineligible for native evaluation/export certification.

## Engineering checks and evidence layout

Candidate worktree: `/home/coder/workspace/botsclustersmc-pickaxe-practice`.
Control worktree: `/home/coder/workspace/botsclustersmc-pickaxe-control`.
The local bounded runner is candidate `.build/placement_study.py`; evidence root
is `.build/placement-study/`. This is local retention, not permanent off-machine
archival. Both the runner and the common frozen-policy evaluator preserve each
report, identities and completion-integrity logs.

Before the candidate change, the new composed test failed on the reserved
storage-to-placement assertion. An initial focused compilation omitted two
existing test dependencies; adding them exposed that expected assertion failure.
After the change, the focused composed suite passed 212,561 checks. In each
2,048-seed nonzero-difficulty matrix, the reserved arm covered the five positions
190, 214, 207, 216 and 196 times. Tests cover both wood and stone, conservation,
empty cursor, original storage remainder, no supplied output, deterministic
replay, retained earlier/frontier coverage, ordinary probes/exams, completion
accounting and unchanged checkpoint round trips. This is mechanical evidence,
not a learned-gameplay result.

An initial background test invocation failed before running tests because the
remote background wrapper tried to execute shell builtin `cd` directly. The
corrected invocation explicitly runs `bash -lc`; this operational failure is not
a product regression or a passing test. Full source-suite and measured outcomes
are recorded below after execution; no learned improvement is claimed here yet.

## Measured outcome: rejected at the early retention gate

The tested implementation/declaration is
`57cbcfa3dfdbc48c408110419b1f0513859d07cf`, tree
`e43812307d1c3015a712c4a73f18b7febb1dd138`, in PR #33. Subsequent edits to
this record report results; they do not change the tested implementation or the
prospective rule above.

The canonical input exported policy **332305**, trained samples **92,574,669**.
Its checkpoint identity is
`495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855`;
its frozen policy identity is
`894a84745625919548620e0a47698d38153234e9b73d7a0a9acb14a0aac4acb1`.
Both arms restored these exact input bytes. Their first-issued coverage records
observed all 512 actors on frontier task 11, with zero initial review actors and
zero unobserved actors. This is a measured startup allocation, not a proof that
startup allocation caused the later retention loss.

| Model | Policy updates | Accepted samples | Additional samples |
| --- | ---: | ---: | ---: |
| Frozen parent | 332305 | 92,574,669 | 0 |
| Stopped control | 333159 | 92,827,654 | 252,985 |
| Stopped candidate | 333144 | 92,828,423 | 253,754 |

Both stopped checkpoints remain within the declared early budget/overshoot.
The control and candidate do not have identical optimizer-step counts; they are
sample-budget-matched asynchronous continuations, not identical trajectories.
Both experimental servers shut down normally and their stopped checkpoints were
exported through the unchanged native checkpoint tool.

Each entry below is a success count **out of 32**, in the declared order of
full-condition tasks **0 through 11**. The last two entries are workbench and
wooden-pickaxe crafting. No reset assistance was used in these evaluations.

| Model | Seed | Ordered task scores |
| --- | ---: | --- |
| Parent | 2026092911 | 32, 32, 31, 32, 32, 32, 32, 32, 32, 32, **31, 0** |
| Parent | 2026092912 | 32, 32, 31, 32, 32, 32, 32, 32, 32, 32, **31, 0** |
| Control | 2026092911 | 32, 31, 32, 32, 30, 32, 32, 32, 32, 32, **27, 0** |
| Control | 2026092912 | 32, 30, 32, 32, 32, 32, 32, 32, 32, 32, **23, 0** |
| Candidate | 2026092911 | 32, 31, 32, 31, 32, 32, 32, 31, 32, 32, **22, 0** |
| Candidate | 2026092912 | 32, 32, 32, 31, 30, 32, 32, 32, 32, 32, **29, 0** |

All six reports completed: **2,304 frozen-policy trials**, unchanged evaluated
weights and zero new training samples. Tasks 0–9 meet the 30/32 floor in every
report. The workbench retention floor was 28/32; both control seeds and the first
candidate seed fail it. The second candidate seed passes, but a favorable seed
cannot erase an adverse one. All ordinary wooden-pickaxe evaluations remain
0/32.

**Decision: do not adopt or deploy the candidate.** The planned 1,500,000-sample
phase and all final five-cell comparisons were **not run**. Therefore this study
does not establish whether the proposed reset mixture improves single-cell
placement. The local continuation runner now also enforces the already-declared
early gate before creating the later phase; invoking the gate on these results
correctly rejects the three failing arm/seed combinations.

Both continuations lost measured workbench reliability, with neither candidate
seed pair uniformly dominating control. Do not attribute all loss to the reset
change, do not conclude that restart alone caused it, and do not present these
two evaluation seeds as independent training replications. They are a negative
engineering screen on one resumed model. The immediate lesson is to investigate
retention during next-skill continuation before increasing placement exposure or
spending the larger training budget. Preserve the earlier learned function and
measure startup, early and later retained skills separately in a new study.

## Completed checks, artifact boundaries and retained evidence

The full local source suite completed with `SOURCE_SUITE_EXIT=0`. The focused
composed test passed 212,561 checks. An additional read-only cross-JAR comparison
found **56,832 identical non-target reset/RNG states**, 106 unchanged archive
entries, and exactly one changed training entry: `InitialCrafting.class`.
Candidate and control inference JAR bytes were identical.

PR CI **36466144136**, for the exact implementation head, passed Ubuntu source,
Windows source and observatory browser jobs. Opt-in live/retention/Paper jobs
were skipped by that workflow; they must not be described as CI passes. The
real-server evidence here is the separately executed local continuation/evaluation
study, not those skipped jobs.

The common evaluation/control training JAR identity was
`7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc`.
The candidate training JAR was
`27a637ab6153f48e0cc66469089c2896af55d94d5f510eb8fff320bea06d9b5b`.
The shared unchanged inference JAR was
`e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0`.

Local evidence paths, relative to the candidate worktree:

- `.build/placement-study/`: immutable input, native-export identity receipts,
  per-arm first-active/last-observed snapshots, status histories, stopped full
  checkpoints, frozen policies and completed early-training receipts.
- `.build/placement-evaluation-baseline/` and
  `.build/placement-evaluation-early/`: all six complete reports, per-trial
  diagnostics, metadata, common-runtime copies and evaluation logs. The
  `completed.json` in each directory enumerates every declared model/seed pair.
- `.build/placement_study.py`, `.build/ResetBoundary.java` and
  `.build/placement-source-suite.log`: local runner, cross-JAR checker and the
  successfully completed source-suite transcript. The runner is a local study
  utility, not a new production feature or a generally supported CLI.

The first baseline attempt was refused before any server/trials because its
output directory was below the input policy directory. Only the output location
was corrected to the sibling paths above; input, runtime, task order, counts and
seeds stayed unchanged, and the refused attempt's logs remain retained. A later
foreground source-test call lost its tool connection; no second successful suite
is claimed from that interrupted response. One ad-hoc status aggregation call
was blocked and did not execute.

A proposed external archive builder was also blocked by tool safety checks before
its implementation was written or executed. It was not retried through another
route. **No public checkpoint/report archive or study release was created.**
The raw evidence remains local, not durably archived off-machine; only the source,
prospective protocol and this measured record are committed to GitHub. Do not
remove these worktrees/evidence directories under the assumption that the raw
study data has already been published.

The production Academy was not used by either continuation, apart from the one
read-only input snapshot. It received no candidate runtime or experimental
checkpoint. Only this negative evidence and the corresponding development-order
clarification are proposed for main; the candidate implementation remains rejected.
