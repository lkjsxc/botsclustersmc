# 2026-09-26 — wooden-pickaxe learning and saved-model replication

## What changed, and what did not

The live learner began completing full-condition wooden-pickaxe trials after the
previous session. The proposed workbench/placement ablation was therefore deferred:
first verify the new behavior independently rather than change a learning process
that had started improving. No learner, reward, reset, mask, model architecture,
optimizer, actuator or curriculum success criterion was modified in this session.

The implemented capability is native `evaluate --from EVALUATED.zip`: reuse the
exact model from a previously retained evaluation bundle under a new case seed.
It does not read or restore the live checkpoint and never extracts or executes
the archived plugin. The current checkout supplies the actual execution code.
See [the evaluation contract](../EVALUATION.md#re-evaluate-the-same-saved-model).

The measured outcome is **57/64 wooden-pickaxe successes on each of two separate
case suites using the same frozen model**. A later model scored 28/32. This is
learned, primitive-action task performance in bounded Academy rooms with furnished
raw supplies, not a scripted mechanical fixture. It is not a claim of perfect
crafting, a raw-log-to-tool chain, complete vanilla-player survival or cooperation.

## Source and environment

- Baseline main: `eba7e7f2abe718848ec61dd57fc104147a10a5d4`.
- Published implementation: `7d005bf035b5245f04fbe3e1fb79dd6cb249b1eb`.
- Exact locally tested and published tree:
  `3187d2f8c4ba8684c360ee5b652f1745a9da79e4`.
- [PR #13](https://github.com/lkjsxc/botsclustersmc/pull/13) was squash-merged as
  `89da3c294f4e8893fbf87cc4e5f33b76b2ccd458`, with the same tree.
- [PR CI 36206370334](https://github.com/lkjsxc/botsclustersmc/actions/runs/36206370334)
  and [main CI 36206520517](https://github.com/lkjsxc/botsclustersmc/actions/runs/36206520517)
  both completed successfully for Linux/Windows source checks and the browser
  observatory. Optional dispatch-only live/windows-live/paper jobs were skipped,
  not counted as executed passes.

Work used Home Coder workspace `minecraft-agents`, the designated hostname
`cw-c76e701404f844d8876a4d50`. Live source:
`/home/coder/workspace/botsclustersmc-source`; verification worktree:
`/home/coder/workspace/botsclustersmc-crafting-transfer`.
The real tests used pinned Folia 1.21.11 build 14, Java 21, Linux amd64 and the
Ryzen 9 9955HX host. The live JVM reports 16 available processors. The Academy
remained `academy-current`, 1,024 NPCs, 8 GiB heap, port 25565 and worker allocation
9/1/5 for region/inference/learner. NPCs are server-side bodies, not logged-in players.

Both `dist/training.jar` and `dist/botsclustersmc.jar` were byte-identical to the
baseline and to the verification build. The installed live training JAR also
matched. The live checkout was clean and fast-forwarded without restarting the
learner or monitor. No weights, Adam state, world, course history, ports or operator
configuration were reset. Existing backups and failed evaluation records remain.

## Software verification

The complete `./test.sh` suite passed, including 70 new replay-source assertions,
four additional standard-report rejection checks and all existing mathematical,
mechanical, curriculum, crafting-diagnostic, persistence, concurrency and export
tests. The new tests cover exact model round trips, immutable task lists, source
preservation, provenance with matching/different inference builds, required ZIP
entries, duplicate/unknown names, size bounds, corrupt model/plugin/report data,
invalid UTF-8, malformed/missing archives and incompatible command options.

The reader checks internal consistency of an old bundle, not external authenticity
of another person's claimed results. New results still require a complete actual
Minecraft run. Explicitly assisted or reset-intervention reports are rejected as
standard evaluations. Source JAR bytes are checked only as data, never executed.

A new owned, isolated result context at `.build/replay-academy` contained **no
training.bcmc**. The replay command completed 832 real trials there (B below),
proving that it uses the requested saved model rather than a hidden live fallback.
Afterwards, two native-command negative checks passed:

1. Ordinary evaluation without `--from` failed for the missing checkpoint.
2. Replay from a deliberately invalid ZIP failed and produced no export.

Both preserved the previous completed summary and full trial report byte-for-byte;
neither created a fallback training checkpoint. These checks are software evidence,
not additional learned-skill trials.

## A and B: same-model replication

A was a canonical checkpoint evaluation from the live checkout:

```sh
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11,12 --cases 64 \
  --seed 2026092606 \
  --export .build/evidence/crafting-transfer/first-frozen.zip
```

B ran from the verification worktree with the accepted EULA and existing server
cache, using only A's saved model and a separate result context:

```sh
EULA=true ACADEMY=.build/replay-academy \
BCMC_SERVER_CACHE=/home/coder/workspace/botsclustersmc-source/.cache/server \
./evaluate.sh \
  --from /home/coder/workspace/botsclustersmc-source/.build/evidence/crafting-transfer/first-frozen.zip \
  --cases 64 --seed 2026092607 --export .build/replayed-frozen.zip
```

Both used policy **634,197**, with **297,871,508** accepted training samples.
A completed at **09:42:37.347 JST**, B at **09:47:38.032 JST** on September 26.
Each completed all **832** stochastic, full-condition trials with zero evaluator
training samples. The model and inference JAR bytes were verified identical.
The task list, ordering and cases per task were also identical; the requested
case seed differed. No supplied-grid, open-menu or teacher-action intervention
was used. Workstation exams begin closed with raw materials and random facing.

| Full-condition task | A, seed 2026092606 | B, seed 2026092607 |
| --- | ---: | ---: |
| 0 Forward-stop | 64/64 | 64/64 |
| 1 Turn-stop | 64/64 | 64/64 |
| 2 Aim-hold | 64/64 | 64/64 |
| 3 Navigate-stop | 64/64 | 64/64 |
| 4 Step-over | 64/64 | 64/64 |
| 5 Break-log | 64/64 | 64/64 |
| 6 Collect-log | 64/64 | 64/64 |
| 7 Place-block | 64/64 | **63/64** |
| 8 Craft-planks | 64/64 | 64/64 |
| 9 Craft-sticks | 64/64 | 64/64 |
| 10 Craft-workbench | **62/64** | **62/64** |
| 11 Craft-wood-pick | **57/64** | **57/64** |
| 12 Mine-cobblestone | **0/64** | **0/64** |

Every successful wooden-pickaxe trial showed five correct ingredient cells and a
real target output preview, and met the unchanged crafted-and-owned-item success
predicate. Median elapsed time among successes was 71 ticks in each suite.
The seven failures per suite timed out at 3,001 ticks without a target preview:
A's maximum-correct-cell histogram was `{2:1, 3:5, 4:1}`; B's was `{3:5, 4:2}`.
This points to recovery from incomplete assembly as remaining work in these cases;
it does not label all menu exits or ingredient removals as mistakes.

Cobblestone trials broke **zero blocks** in both suites. Some mining contact was
recorded, but neither successful stone breaking nor cobblestone collection was
established. Generic item-pickup counters are not evidence of the task's required
cobblestone outcome. Sustained mining/tool control merits investigation, not an
unsupported claim that a particular optimizer or observation defect caused it.

## Additional checks C and D, not pooled with A/B

**C: retained older model.** The previous session's saved policy **581,011**
(272,564,721 samples) was replayed for task 11 only, 64 cases and seed 2026092606.
It completed at **09:55:43.558 JST** with **0/64** successes. Maximum correct-cell
counts were `{0:47, 1:14, 2:3}`. The model matched its original saved bundle exactly.

Although C uses A's task-reset seed, it is **not a fully paired action-randomness
comparison**: a task-only suite assigns different actor IDs from A's 13-task suite,
and actor ID contributes to the action RNG seed. Its filename
`old-policy-matched-cases.zip` must not be interpreted as proof of fully matched
randomness. C is an additional old-model check, not a controlled causal experiment.
To repeat an actor/action-seed specification, preserve task order and cases per
task as well as the seed; real asynchronous scheduling can still differ.

**D: merged-main canonical path.** After integration, ordinary evaluation without
`--from` was tested again against a new live checkpoint. Policy **640,468**
(300,169,112 samples), seed 2026092608, completed **416** trials at
**09:57:09.522 JST**. Tasks 0–10 each scored **32/32**, wooden pickaxe **28/32**,
and cobblestone **0/32**. This validates the normal checkpoint path after the
replay refactor as well as recording a later learned snapshot.

D is not the same model as A/B and is not pooled with them. Three failed wooden-pick
trials never reached a preview; one reached a target preview once but did not
complete. Thus final output collection is not uniformly reliable in all later
states either. All four failures timed out at 3,001 ticks. Cobblestone again broke
zero blocks. No complete-course or general-survival conclusion follows.

## Evidence retention and audit

All **2,144** actual trials across A/B/C/D were completed and retained, including
all failures. A separate audit checked ZIP integrity, the five intended entries,
exact model/plugin identities, complete unique actor sets, task/seed association,
summary agreement, nonzero elapsed trials, diagnostic denominators, model identity
across A/B, old-model identity for C and absence of replay training state.
It confirmed the same inference JAR bytes across all four runs.

Copies are together in the live source checkout:

```text
.build/evidence/crafting-transfer/
  first-frozen.zip
  replayed-frozen.zip
  old-policy-matched-cases.zip
  main-canonical.zip
  session-audit.json
  first-evaluation.log
  main-canonical.log
  bcmc-replay-tests.log
  bcmc-transfer-replay.log
  bcmc-old-policy-matched.log
```

Original replay bundles remain in the verification worktree's `.build/` directory.
The audit program is `.build/audit-transfer.py` there. The live evaluation panel's
latest result is D, policy 640,468, not a later continuously updated live policy.
No pending evaluator from this session remained at the process check; only the
existing learner and monitor continued. Source and evaluated model bundles are
not automatically installed into a deployment world.

## Live snapshot and next priority

At **2026-09-26 09:59:06.125 JST**, the audit read running policy **642,631**,
**300,974,513** accepted samples, and all **1,024** actors active, ticking and
progressed. Inference failures/rejections, burning bodies and rejected/stale
learner samples were all zero. The status interval reported **2,927.79** accepted
samples/s and **1.354** CPU-core equivalents out of 16 JVM-available processors.
These are interval measurements, not a promised sustained throughput.

The stage population was **449 at wooden-pickaxe stage 11 and 575 at cobblestone
stage 12**, with zero completed-course actors. Progression records their per-actor
frozen curriculum checks; being at stage 12 does not mean they can already mine
cobblestone. The independent full-condition mining scores above remain zero.

The rational next step has changed with the evidence: preserve the now-measured
crafting behavior, test its remaining assembly/collection failures and earlier-skill
retention, and inspect sustained stone-mining behavior as the new frontier gains
training. Do not inject a teacher, relax completion gates or reset the learner to
make the milestone look stronger. The later successes are an observed improvement
after continued training, not causal proof that any single previous code change
was sufficient. Persistent open-world survival and cooperative living remain
separate engineering and learning objectives.
