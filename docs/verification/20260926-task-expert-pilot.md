# Task-expert pilot: rejected, with a separate launcher fix

Date: 2026-09-26. This is a completed negative experiment, not a new learning
architecture accepted into production. All measurements below came from the
operator-designated `minecraft-agents` workspace.

## Decisions

**Reject the cold independent-expert replacement.** Its numerical implementation
passed the source suite, but it failed the predeclared Minecraft task-1 gate in
two fixed-policy tests. PR #17 is closed without merging. Keep its source,
predeclared plan and failed trials as evidence, not as a pending production change.

**Accept only the independent Academy data guard.** PR #18 was merged as
`46cfa1ccb04070ffbcc8643011c9da1a597c564d` after Ubuntu, Windows and browser CI
passed. It changes launcher ownership checks, tests and documentation, not policy,
optimizer, reward, curriculum, model schema or game mechanics.

This document does not certify a later live policy or open-world survival.
Production was separately restarted in place and then paused for a retained-skill
investigation. Its later fixed-policy results and final operating state belong in
a separate continuation record, rather than being attributed to this pilot.

## Original-policy baseline

Source: `fa3cebe816a8d90b56e838d60031547c90c49fb8`.
The unchanged shared policy was frozen at update **789705**, with **370738571**
accepted training samples. Seed **2026092631**, 32 full-condition cases for each
of tasks 0 through 12: 416 completed trials and zero new training samples.

| Tasks | Passed / cases per task |
|---|---:|
| 0, 1, 3, 4, 5, 6, 7, 8, 9, 10 | 32 / 32 each |
| 2: aim-hold | 31 / 32 |
| 11: craft-wood-pick | 16 / 32 |
| 12: mine-cobblestone | 0 / 32 |

The stone trials contained 19200 decision-boundary observations. Of these,
16717 were menu-focused selections, 474 observed a held pick, and **zero** observed
target-pick contact. There were 647 world-dig selections. These are sampled
observations, not tick-exact time shares or proof that task interference is the
sole cause. The original learned model was not acquiring cobblestone reliably.

Production was normally stopped and saved at policy/Adam **800351**, with
**375827378** accepted samples. The original `academy-current` was retained in
place. No replacement checkpoint, reset, schema migration or imported certificate
was used in production.

## Candidate and numerical checks

The implementation contains 18 independent MLPs routed by the already observed
one-hot task goal. Every cold expert starts with the same seeded initial weights.
Actors still choose the same primitive controls; no teacher, pathfinder,
autocrafting fallback, reward change or easier full-condition exam was introduced.

Each update normalizes and clips the mean gradient separately for each sampled
expert. Adam first/second moments and bias-correction clocks are expert-local.
An absent expert remains bit-identical even when its optimizer has nonzero
residual momentum. Batched inference groups actors by expert, executes one MLP
per actor, and restores the original lane order. It does not execute all 18 MLPs
for every actor.

The candidate has a strict, different model/training schema, persisted local
optimizer clocks, and no legacy loader. Its policy payload is 4956744 bytes,
compared with the baseline's 275486 bytes. An initial source-suite failure exposed
an old 4 MiB evaluated-bundle model-entry bound; it was corrected to the existing
8 MiB schema model bound before the passing test run.

The full local source suite passed, including **1803 dedicated expert checks**:
independent scalar/batch oracles, meaningful finite differences for all experts,
inactive gradient support, repeated weight/moment/clock invariance, and strict
checkpoint round trips and malformed-state rejection. Existing learning,
mechanics, asynchronous, export and evaluation checks also passed. This proves
neither learned Minecraft skills nor the opt-in broad deployment acceptance suite.
Real-API diagnostic fixtures were compiled; the actual learning trial below is
separate evidence.

Exact tested source identity:

- Local experiment commit: `3661aef32bdad26dc6294dde5410fd17c88d3034`.
- Published experiment commit: `38c12a68f3d4041aae361f2d77ede715ef2dfc42`.
- Identical complete tree: `318d9c0c580f44f6497ffa0b2ef4ae5d0f21fe11`.

A fresh public Git fetch and whole-tree diff confirmed the published/local match.
The different commit metadata is not presented as an identical commit ID.
The predeclared plan is retained in that published commit at
`docs/verification/20260926-expert-pilot-plan.md`.

## Predeclared real-Folia study

The pilot created an entirely new `academy-experts-fresh`: 1024 NPCs, seed 7,
8 GiB maximum heap, 9/1/5 region/inference/learner threads, loopback port 25631,
and online authentication. Only the existing pinned public server cache was
reused. Weights, optimizer, clocks and course records all started fresh. No actor
was granted a previous-task certificate or jumped directly to a later stage.

The declaration specified stops at the first sampled boundaries at or after
one million and five million accepted samples, plus a 40-minute elapsed cap.
Actual saved counts, including overshoot, are reported below. Each boundary used
32 full-condition cases per task from zero through the highest reached task,
including the unfinished frontier. The first seed was 2026092632; the later seed
was 2026092633; replication used 2026092634.

Acceptance required **both tasks 0 and 1 to reach at least 28/32 in both later
tests**. This was a cold-start feasibility gate, not proof of retaining the old
shared model's tasks 0 through 11. It was not relaxed after observing the result.

| Frozen update | Accepted samples | Evaluation seed | Forward-stop | Turn-stop |
|---:|---:|---:|---:|---:|
| 2168 | 1032171 | 2026092632 | 31/32 | Not reached; not tested |
| 10828 | 5025343 | 2026092633 | 32/32 | 0/32 |
| Same 10828 | Same 5025343 | 2026092634 | 32/32 | 0/32 |

Task 1, `turn-stop`, requires navigating and stopping after a changed initial
facing; it is not merely rotating toward a target. During phase two its training
successes were **1/22247**, and its full probes were **0/5105**. All 1024 actors
advanced normally from stage zero to stage one, but none advanced beyond it.
At the saved endpoint, expert clocks 0 and 1 were 10737 and 7183; all remaining
16 expert clocks were zero.

Both phases ended normally and saved their exact checkpoints. They reported zero
queue rejections, stale learner samples and inference failures. In phase two,
214 sampled statuses had a median positive throughput of approximately
4004.77 samples/second. Sampled used heap reached **7747 MiB**. This is not a
post-GC retained-memory measurement or a controlled efficiency comparison.
Per-actor frozen exams retain an entire Policy object; retaining full expert
banks is a plausible memory contributor, not a measured causal attribution.

All **160** pilot evaluation cases completed with zero training samples, including
all failures. The replication used `--from` the earlier evaluated bundle, and
identical model and inference-JAR bytes were confirmed. ZIP CRCs, summary/full
trial consistency, unique actor/task accounting, positive elapsed ticks,
denominators and model/plugin identities were independently checked.

## What this rejects, and what remains plausible

The result rejects this particular cold independent-expert replacement under its
declared budget and gate. It does not establish that every modular policy fails,
that loss of cross-task transfer is the only cause, or that task 1 could never
learn under a different budget. Extending this failed trial until it passes would
not convert it into a pass of the original declaration.

The next design must address **both transfer and interference**, not just isolate
weights. A transfer-preserving initialization or representation and a memory-
efficient frozen-policy representation are candidates for a separately declared
study. They are not implemented or accepted by this record. Full-condition,
fixed-policy retention tests must remain distinct from historical course
certificates, assisted training successes and live changing-policy probe counts.

## Independent Academy data guard

The custom pilot directory exposed a separate problem: a root rule for `academy/`
does not exclude arbitrary names selected with `ACADEMY`. Before starting a
server, `ownAcademy()` now creates or validates an internal `.gitignore` containing
exactly `*` and LF. Directories are excluded too, preventing descendant negations
from bringing runtime data into ordinary staging. Creation uses `CREATE_NEW` and
`NOFOLLOW_LINKS`; custom conflicting files are rejected, never overwritten.

The change adds 29 filesystem/Git checks using only synthetic temporary data.
The original launcher fails the new regression fixture; the corrected launcher
passes it. Git staging, nested negations, ownership, existing-data preservation,
symlinks, malformed ignore paths and complete fixture cleanup are exercised.
This does not protect already tracked data, force-added files or external copies,
and is not access control or a backup. See `../ACADEMY_DATA.md`.

Final source tree: `278e1c03e97b9bbb8958100b93a55aa5624e61c7`.
Local commit `5a25507c89b0fc67c882c9bdca703ef0fcc7c0ca` and published PR head
`7b6e60f9276c7f31abb4effe627a1a61cb8fbd7b` had that identical tree.
Local full tests passed. PR CI **36228487509 / attempt 1** completed successfully:
Ubuntu source job 108366962558, Windows source job 108366962240, browser job
108366962181. Optional broad live deployment jobs were skipped, not passed.

The first PR run **36228210223** is retained: Ubuntu and browser passed, while
Windows failed deleting a DOS read-only Git object in the synthetic fixture.
The fix clears that attribute only inside the test-owned temporary repository,
uses try-with-resources to preserve original failures, and asserts cleanup.
Runtime deletion permissions and `Host.deleteTree` were not weakened.

## Safety boundary and retained evidence

A requested full copy of the original Academy was blocked by the tool safety
check. It was not retried through another command, encoding or tool. The pilot
plan changed to entirely new data instead. No copied backup or imported original
model is claimed. The original production Academy stayed in place throughout.

The following evidence remains in the designated workspace; it is not embedded
in the source repository and these paths are not public download links:

- `botsclustersmc-source/.build/evidence/20260926-mining-continuity-baseline.zip`.
- `botsclustersmc-skill-isolation/.build/evidence/expert-pilot-1000000.zip`.
- `botsclustersmc-skill-isolation/.build/evidence/expert-pilot-5000000.zip`.
- `botsclustersmc-skill-isolation/.build/evidence/expert-pilot-replication.zip`.
- The same pilot directory contains both phase manifests, the bounded controller,
  full logs, and `expert-pilot-audit.json` with the complete audit identities.

The late and replication policy digest is
`ce1364ffd1e11f0417928a2a02dcc1441356f235dc15bf01fb31d86a0b9562d5`.
Their inference JAR digest is
`7bf1707a974b16d211b779a5789dc0bdb7375e915f9471f2097ba02de69e2e9d`.
The pilot remains stopped. Rejected experiment code is absent from main.
