# Holdout evidence integrity - 2026-09-29

## Scope and decision

Base main: `a48bf4b6d1e050ebea13be96ca43819ccc1945c8`.

This change repairs the optional developer Python holdout driver. It does not
change the Java evaluator, neural policy, rewards, curriculum, optimizer, resets,
action/observation schema, deployment plugin, or production service configuration.
It is an evaluation correctness change, not demonstrated learning progress.

The preceding protected-frontier pilot remains stopped at initial qualification.
Its selected source and protected model both scored 29/32 on aim-hold on one seed.
No relative-retention continuation was declared or started in this work. Neither
this fix nor the revalidation below changes that pilot's original decision rule.

## Reproduction and implementation

Before editing, `python3 -O -m unittest discover -s tests -p test_holdout.py`
ran the eight existing test methods and reported 146 failed rejection subtests
and ten errors. Most rejection assertions had disappeared under optimization;
some malformed values instead caused incidental Python exceptions. This is not
evidence that a previous published server run actually used optimization.

The driver now uses explicit checks that remain active with `-O`, `-OO` or
`PYTHONOPTIMIZE`. The same protection covers table diagnostics, complete reports,
the unchanged frozen policy, the frozen source checkpoint and the absence of a
generated training checkpoint. A failure marker or nonzero process exit rejects
the run before successful publication.

The report parser reads at most 64 MiB plus one detection byte, requires UTF-8,
rejects duplicate keys at every JSON object level, and rejects non-finite literal
or exponent-overflow numbers. Counts and identities require integers rather than
Python's bool/int equality or float coercion. Success and completion flags require
actual booleans. Diagnostic intervention labels and counts must match the declared
ordinary/assisted condition.

Every unique actor must match its declared task and case index. Its lesson seed
must match `examSeed + task*1000003L + case*104729L`, including Java signed-long
overflow. Swapping actors across tasks or swapping cases within one task cannot
pass merely because aggregate successes still agree. Task summaries must have
the declared order, denominators and trial-derived success counts. Finite angles,
nonnegative integer observations/counters, elapsed ticks and final distance are
validated before table-specific checks.

These are internal-consistency checks, not authentication of an untrusted report.
A valid complete report can still contain zero successful trials. The native Java
operator evaluator remains separate and gains no Python runtime dependency.

## Completed checks

`python3 -m unittest discover -s tests -p test_holdout.py -q` passed all 23 test
methods. The optimization test also launches three real interpreter subprocesses:
one with `-O`, one with `-OO`, and one with `PYTHONOPTIMIZE=2`. Each runs the 22
non-launcher methods. This includes the old diagnostic tests and new type,
coverage, task/case/seed, signed-long, malformed JSON, bounded-read, changed-policy,
training-checkpoint and process-failure regressions. The subprocess repetitions
are not independent gameplay experiments.

A full `./test.sh` completed with exit zero, including Java arithmetic, real-API
compilation, fixture compilation, host tests and inference-artifact separation.
The retained transcript is
`/home/coder/workspace/botsclustersmc-holdout-integrity/.build/holdout-source-suite.log`,
with exit status in the sibling `.exit` file. An earlier tool connection closed
while a source test was running; that unobserved completion was not counted.
The recorded run is a separate completed invocation. `git diff --check` passed.

### Saved real-server evidence revalidation

Under `python3 -O`, the repaired verifier accepted all four preserved baseline
reports from the protected-frontier pilot, covering all 1,536 original trials.
Actor/task/case/seed identities and every task total were rechecked. The actual
frozen policy files also matched their recorded identities. No report or source
policy was changed, and these trials were not rerun.

Each row below is successes out of 32, ordered tasks 0 through 11:

```text
Base,      2026092941: 32 31 29 32 32 32 32 32 32 32 26 0
Base,      2026092942: 32 32 32 32 32 32 32 32 32 32 28 0
Protected, 2026092941: 32 32 29 32 31 32 32 32 32 32 27 0
Protected, 2026092942: 32 31 32 32 32 32 32 32 32 32 28 0
```

The originals remain in the protected-frontier worktree under
`.build/focus-study/evaluation-baseline/`. This audit establishes compatibility
with those actual records, not new skill retention, new trials or new learning.

## Live-test limitation and operating boundary

A requested fresh disposable Folia holdout under `python3 -O` was blocked by the
tool execution safety check before execution. Its intended output directory was
confirmed absent. There is no new live acceptance result, ordinary/diagnostic
comparison, or experimental continuation to report. The blocked operation was
not retried through another execution route.

Both newly built runtime JARs are byte-identical to the production checkout:
training `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc`
(212,945 bytes), inference
`e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0`
(120,533 bytes). The installed Academy `plugins/training.jar` also matches the
training artifact. Therefore no service restart, model installation, checkpoint
reset or gameplay change is needed for this developer-driver-only repair.

A read-only production observation at epoch 1790666770905 reported running,
512 active and progressing NPCs, policy 647243, 186,669,405 trained samples and
2,086.6 accepted samples/second. Inference failure/rejection, learner
rejected/stale samples and retired-agent counters were zero. This is a bounded
health observation, not a new competence certificate or proof of indefinite
health. Learning continued independently; no skill gain is attributed to this fix.

## Next learning question

Retain the separation between input competence, relative retention of an imperfect
input, and ordinary new-skill acquisition. A future protected-learning comparison
still needs its own prospective criteria, larger declared case sets, fixed sample
budgets and complete early/final frozen reports. The integrity repair is a
prerequisite for trustworthy measurement, not a substitute for that comparison.
