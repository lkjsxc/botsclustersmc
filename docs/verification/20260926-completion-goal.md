# 2026-09-26 — completion-preserving workstation practice

## Scope and source identity

This change removes an unobserved opening-only terminal goal from workstation
practice. It does **not** establish learned wooden-pickaxe completion or autonomous
survival/cooperation. Mechanical correctness, runtime health and learned ability
are reported separately below.

- Baseline: `f68fbec3eaefd03c27bc15d2520ac3063dcfaebb`.
- Tested implementation: `4f6557912866dc46eb4ce14ef52e45b55d215b92`.
- Exact tested tree: `5efc00f6b4dbd0f285d6a108e34fb394507a8940`.
- [PR #11](https://github.com/lkjsxc/botsclustersmc/pull/11) was squash-merged as
  `15c83b4b68a0c490395c8a1f0fb325445589d7e0`; the merged tree is identical.
- [PR CI 36195543650](https://github.com/lkjsxc/botsclustersmc/actions/runs/36195543650)
  and [main CI 36195734621](https://github.com/lkjsxc/botsclustersmc/actions/runs/36195734621)
  passed Linux/Windows source checks and the synthetic browser observatory tests.
  The optional workflow-dispatch live jobs were skipped, not counted as passes.

The local real-server checks used the pinned Folia 1.21.11 build 14, Java 21,
Linux amd64 on an AMD Ryzen 9 9955HX host. The operator JVM reports 16 available
processors; this is distinct from the host's 32 logical processors.

## Defect and correction

The old `StationPractice.acquisition` chose an opening-only goal using the lesson
seed. `TrainingEnvironment.success` returned `stationOpen(npc)` for that phase,
before testing the actual crafted/extracted/supplied item outcome. In other
lessons the same station task required completion. The phase was not supplied to
the actor or critic as an observation. Excluding opening wins from the completion
EMA did not remove the different terminal flag/reward target.

This is a goal-contract problem found during investigation of failed crafting;
it is not evidence that this was the sole cause of the learning plateau.

The correction keeps one task-completion outcome in every station lesson.
Assisted practice below difficulty 1 begins at the open target station and retains
the existing raw-cell/cursor preparation. Full probes, difficulty-1 practice and
frozen exams retain their closed menu, raw stock, world/pose random stream and
original item requirements. Full probes still train the complete opening and
operation sequence; frozen exams remain excluded from learning.

No teacher action, recipe macro, pathfinder, observation input, inference rule or
optimizer change was introduced. The terminal reward **formula** and bounded
potential shaping are unchanged; opening no longer triggers successful termination
or its success reward. The inference JAR was byte-identical to the baseline.
Every new task-completion outcome updates the difficulty EMA. Historical weights,
Adam state, RNG/course history and existing EMA/counters are retained. Old totals
may still contain historical opening-only wins; they were not relabeled or erased.

The HUD now says `practice: complete at open station`. The observatory reports
`station_success: task-completion` and
`station_curriculum: completion-preserving-resets`, without opening-only success
counters. Missing or invalid goal diagnostics display unavailable.

## Software verification

`./test.sh` passed all source checks, including 181,037 composed crafting assertions,
menu/station mechanics, curriculum readiness, persistence, concurrency, mathematical
learning tests and launcher/export checks. These counts are assertions, not that
many independently learned Minecraft skills.

The new live `StationChecks` keeps the physical station state and frame fixed
while changing three lesson kinds, four difficulties and 64 seeds. Across the four
station tasks, 3,072 cases verify that opening alone is not success and that the
potential does not depend on the unobserved lesson metadata. Wrong-menu/target
checks also pass, and fixture mutations are restored before scripted completion.

**Negative control:** compile the new live regression against the old training JAR
and run it in the isolated real-Folia fixture. It fails as expected:

```text
Station goal alias: opening alone completed
Lesson[serial=1, task=CRAFT_WOOD_PICK, difficulty=0.2, seed=4, kind=PRACTICE]
```

**Corrected control:** the new training JAR passes those station cases and all
18 full scripted task fixtures. Scripts here are explicit mechanical test drivers,
not actors' learned actions or injected training experience.

The isolated lifecycle also passed:

```sh
EULA=true python3 tests/acceptance.py all \
  --count 128 --seconds 15 \
  --inference-count 64 --inference-seconds 15 \
  --output .build/completion-goal-validation/lifecycle
```

This covers source startup, 128 training actors, normal stop, exact checkpoint
resume, export and a 64-actor inference deployment, including existing fail-closed
and goal-replacement checks. The fresh-run resume receipt expected update 39 and
12,197 samples and restored exactly `[39, 12197, 39]`, including Adam step 39.
The source/negative/positive/lifecycle driver completed successfully in 231.24 s.
This is a bounded lifecycle check, not a long-duration throughput benchmark.

Local receipts are retained under the verification worktree's
`.build/completion-goal-validation/`: `source-tests.log`, `baseline.json`,
`candidate.json`, `lifecycle/`, and `result.json`. CI separately exercised
Linux/Windows source builds and desktop/mobile, malformed-data and stale-data
browser behavior. No local browser pass is claimed.

## Operator deployment and preservation

The live Academy was stopped normally; its supervisor exited and its learner
reported stopped before backup or source replacement. The complete Academy,
operator configuration, old training JAR and a complete source Git bundle were
saved in the sibling private directory
`botsclustersmc-backup-20260925T221509Z-completion-goal`.
The compressed archive integrity check and `git bundle verify` both passed.

The live checkout was clean and fast-forwarded to the merged commit. The server
was rebuilt and restarted with the same 1,024 actors, 8 GiB heap, port 25565 and
region/inference/learner worker allocation 9/1/5. No hot reload or random reset was
used. The startup log reports the exact stopped checkpoint:

```text
Restored exact optimizer/model:
updates=560712, samples=262971606, optimizer-step=560712
```

The read-only monitor was restarted with its existing loopback binding on port
8765. Its served page contains the new completion-goal text. No new public endpoint
or management privilege was added.

## Paired-seed frozen-policy measurements

Two complete, stochastic, full-condition evaluations used seed `2026092604`,
32 cases for each task 0–11, and the same inference build. Both report zero new
training samples inside the evaluator. Failed trials are retained.

The first evaluator copied the **resume checkpoint**, before the new process had
saved additional training. It is a baseline, not a post-change learned result.
The second copied an actual later checkpoint after 694,810 accepted samples and
1,534 updates under the corrected curriculum.

| Snapshot | Policy update | Accepted samples | Result timestamp, JST |
| --- | ---: | ---: | --- |
| Resume baseline | 560,712 | 262,971,606 | 2026-09-26 07:18:20 |
| Early follow-up | 562,246 | 263,666,416 | 2026-09-26 07:21:33 |

| Full-condition task | Baseline | Early follow-up |
| --- | ---: | ---: |
| 0 Forward-stop | 32/32 | 32/32 |
| 1 Turn-stop | 32/32 | 32/32 |
| 2 Aim-hold | 32/32 | 32/32 |
| 3 Navigate-stop | 32/32 | 32/32 |
| 4 Step-over | 32/32 | 32/32 |
| 5 Break-log | 32/32 | 32/32 |
| 6 Collect-log | 32/32 | 32/32 |
| 7 Place-block | 31/32 | 32/32 |
| 8 Craft-planks | 32/32 | 32/32 |
| 9 Craft-sticks | 32/32 | 32/32 |
| 10 Craft-workbench | 32/32 | 32/32 |
| 11 Craft-wood-pick | **0/32** | **0/32** |

No learned wooden-pickaxe improvement was observed in this early follow-up.
The one-case placement difference is not a causal improvement claim. These are
paired-seed observational tests of a short training interval, not replicated
controlled training experiments. The successful basic tasks are measured in the
bounded Academy rooms, not an unrestricted survival world.

Both exact evaluated models, matching inference JARs and every one of their 384
trials are retained in separate bundles in the live source checkout:

```text
.build/evidence/completion-goal/evaluated-policy.zip
.build/evidence/completion-goal/evaluated-policy-after-training.zip
.build/evidence/completion-goal/comparison.json
```

The bundle audit checked ZIP integrity, all 384 retained trials per snapshot,
policy-file/report identity, distinct policy weights and identical inference JARs.
The operator-facing latest evaluation panel now refers to policy 562,246, not to
whatever newer policy the live learner has subsequently produced.

To repeat the native evaluation for a new saved checkpoint, choose a new output
path; do not overwrite an earlier tested bundle:

```sh
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 32 \
  --seed 2026092604 --export .build/new-evaluated-policy.zip
```

## Live health and next scientific question

At **2026-09-26 07:21:46 JST**, the comparison receipt independently read:

- Running policy 564,002; 264,500,000 accepted samples, or 1,528,394 additional
  samples since the preserved resume checkpoint.
- 1,024 active, ticking and progressed actors; 0 inference failures/rejections,
  0 burning bodies, and 0 rejected/stale learner samples.
- 3,713.55 accepted samples/s over that status interval; 1.126 CPU cores used out
  of 16 JVM-available processors. This is a snapshot, not a promised sustained rate.
- All 1,024 actors still at wooden-pickaxe stage 11; 0 completed-course actors.

Very early process-local practice ratios were deliberately not used as improvement
evidence: short successful attempts finish before long failed attempts time out.
Frozen completed trials and stopped/resumed identity checks are separate from
these in-flight training statistics.

The next priority is to identify which ingredient-placement states and action/value
estimates prevent full recipe completion, using frozen policy diagnostics and
controlled comparisons that also re-test earlier skills. A larger model, new memory
architecture or longer run should not be called a solution merely because sample
counts rise. Persistent open-world survival and cooperative settlement remain
separate engineering/learning work; this correction neither implements nor
certifies them.
