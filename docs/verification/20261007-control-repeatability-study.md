# Prospective fixed-policy control repeatability comparison

Date: 2026-10-07 (Japan). No additional learning or runtime adoption.

## Motivation and scope

The matched review-first controls in the 2026-10-06 cohort-credit comparison and
2026-10-07 shared-review comparison started from the same complete model/Adam/
course and runtime, yet their reported ordinary wooden-pickaxe outcomes differed.
The former scored 21/32 and 19/32 and reproduced large parent-relative loss;
the latter scored 27/32 twice and did not. Their evaluation seeds also differed.
Neither two evaluation seeds nor a configured learning seed make the asynchronous
training trajectories identical. The candidate's sustained accepted rehearsal
was demonstrated, but its retention benefit was not.

Before changing another allocation constant, reward or optimizer, cross the exact
saved policies under a common fresh seed set and repeat each physical evaluation
from a fresh disposable world. This separates observed differences between the
saved states from differences between seed sets and same-policy/same-seed physical
repeat variation. It does not identify which asynchronous training event caused
a parameter change, nor estimate the distribution of all future training runs.
The historical policies were selected because of the earlier discrepancy, not
sampled as randomized new training replicates.

All earlier study decisions and budgets remain unchanged. In particular PR #60's
individual-coverage screen is still failed, and PR #62's rehearsal benefit still
unsupported. This study neither continues their learners nor reclassifies them.
It also does not retry the previously blocked optional per-actor course inspection.

## Immutable policies and common evaluator

Start source from accepted main `2726b81bae1985daf525b920bca3b33255ba6753`.
No `core`, `plugin`, `training`, `host`, `tests/holdout`, `tests/holdout.py`,
`tests/acceptance.py` or workflow change is permitted in the prospective source.
New orchestration, integrity tests and this protocol are the only changes.

| Name | Meaning | Frozen policy SHA-256 |
| --- | --- | --- |
| parent | Common 344,488,915-sample parent | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| control-earlier | Stopped control from PR #58 | `7b9f51d605c89d34cd13c3e3cbd4a36ec281afad62156962e2e6b49b3a974af1` |
| control-later | Stopped control from PR #62 | `d6543d1205b2507c23e15ffe844d4e11cf47656df70840bb185669cd29df433a` |
| review-later | Stopped admission candidate from PR #62 | `57144e26e1af87fa286d7eaf037f928d4fa005e1251bc5155b55e6c87936ea22` |

The published provenance records give earlier-control continuation of 252,940
accepted samples; later-control 254,537; later-review 251,756. These are close
finite budgets, not exactly the same number of updates or examples. Do not claim
a perfectly matched optimizer path. All saved originals remain read-only.

Every physical evaluation uses the same previously pinned control training JAR,
SHA-256 `82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9`,
with the unchanged frozen-exam harness and pinned Folia cache. This is the prior
research evaluation runtime, not a deployment of rejected learning changes.
The shared development runtime and its moving policy are outside the comparison.

## Complete matrix, order and resource boundary

Use exactly four policy identities, fresh evaluation seeds **2026100741** and
**2026100742**, and physical repeats **0 and 1** per policy/seed pair. Each report
contains ordered tasks **0-12**, **32 cases per task**, `reset_intervention=none`:
**16 reports / 6,656 physical trial executions**. Repeats keep the same seed and
case identity; they do not obtain a new seed or reuse a populated world. Each
report starts a separately owned disposable localhost server. Do not substitute
an archived report for any cell of the new matrix.

Run four waves: seed41/repeat0, seed42/repeat0, seed41/repeat1, seed42/repeat1.
All four models appear in each wave. Rotate their localhost slots each wave over
ports **31710-31713**, so every model uses each slot once. At most four evaluators
run concurrently; no training server is started. Require at least 10 GiB available
host/cgroup memory before each wave; insufficient resources stop admission rather
than evict unrelated services. Reuse explicit stored Minecraft
EULA consent. No listener is opened on a public address, no operator world is
modified and no other project is stopped to make room.

The control program allows at most 900 seconds per evaluation process; the
existing harness has its own smaller server cap and clean teardown. A timeout,
failed identity check, missing report or dirty/moving source is an operationally
incomplete matrix, never a favorable subset. Preserve partial evidence and
stop owned processes. No replacement seed, retry selected outcome, extension,
extra training or second matrix is authorized by this protocol.

Before physical execution: normally commit and push this protocol and the runner,
pass offline corruption/interpretation tests and the full source suite, qualify
that exact head with Ubuntu/Windows/observatory CI, and verify all immutable input
hashes. Four optional live workflow jobs may remain skipped; do not call them
passed. The physical matrix is a separately executed experiment.

## Measurements and predeclared interpretation

Record every trial's identity, seed, boolean outcome, elapsed ticks and existing
physical diagnostics. Recount all task successes from raw cases. Keep per-case
outcome flips between repeats even when aggregate scores match. Verify complete
metadata, runtime/exam hashes, policy bytes before/after, and the absence of new
training checkpoint files and accepted training samples.

Primary repeatability measures are per-policy/per-seed **task-11 score delta** and
**number of task-11 case outcomes that flip** between physical repeats. Report
these for every task as well. Engineering screens are score difference at most
**3/32** and at most **4/32** flipped task-11 cases for every policy/seed pair.
These margins mark material observed repeat variation; they are not confidence
intervals, equivalence tests or guarantees of determinism.

A consistent control-state separation requires, on **each seed**, the minimum
score across both repeats of one control to exceed the maximum score across both
repeats of the other by at least **4/32**, in the same direction. Report both
signed envelopes even when the screen fails. A result that is stable for these
selected saved states is not an estimate of future training-seed variance.

For continuity, give all parent-relative floors separately for each seed/repeat:
tasks0-10 `max(28, parent-3)`, task11 `max(26, parent-4)`, task12 `max(0,parent-2)`.
The parent reference qualifies only if every parent report has tasks0-10 >=28/32
and task11 >=26/32. Still complete the entire descriptive frozen matrix if a
parent floor fails: no learning follows it, and the limit must be reported.
Do not average away a failed task, seed or repeat. Report the actual pickaxe
loss of each saved model relative to its paired parent report, without requiring
that a desired control loss occur.

The later review candidate's crafting advantage is only a secondary descriptive
contrast against its own later control; a positive screen requires at least
**4/32** on all four paired seed/repeat scores. It cannot establish a general
rehearsal benefit from one historical candidate trajectory. No deployment follows
any combination of these diagnostic screens.

Mining success and existing menu/held-pick/contact/world-dig/broken-block counters
are secondary descriptive measurements. All reports are ordinary full-condition
trials, not forced inventory closure, tool selection, target facing or teacher
actions. The test bodies are in-server NPCs, not logged-in vanilla players.

## Evidence and completion

Preserve all 16 raw reports, metadata, source identities, start/stop logs, pinned
input copies and per-trial outcome vectors. Independently recount the physical
matrix without importing the controller's analysis. Validate the auditor with
corrupted synthetic fixtures and record any unverified boundary explicitly.
Record shared-server health independently of this frozen competence measurement.
Only tested orchestration/evidence may be considered for main; no reward, learner,
policy, runtime or shared learning progress changes are part of this work.
