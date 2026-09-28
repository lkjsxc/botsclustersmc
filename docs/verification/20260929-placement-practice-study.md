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
