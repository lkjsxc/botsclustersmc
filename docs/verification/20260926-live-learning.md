# Live learning and wooden-pickaxe diagnosis — 2026-09-26 JST

## Runtime and change boundary

The authorized Home Coder workspace was `minecraft-agents`, hostname
`cw-c76e701404f844d8876a4d50`. The active checkout was
`/home/coder/workspace/botsclustersmc-source`, running the fresh
`academy-current` created in the preceding session.

The production source was `24e848ec91956806d7440435250e97ae83ec7d86`
(PR #9's conditional menu decoder plus PR #10's test-clock correction).
GitHub main and the clean local checkout were checked independently.
The freshly built `dist/training.jar` and installed Academy `training.jar`
were byte-equal. Settings remained 1,024 NPCs, 16 effective CPUs, an 8-GiB
maximum Java heap and Minecraft port 25565.

This continuation did **not** restart production, reset learned weights,
discard the new Academy, change its rewards/curriculum/pass criteria, or
install an experimental runtime. The isolated diagnostic below is not the
production runtime. The committed change is an evidence record, not a new
learning implementation or a claim that the remaining bottleneck was fixed.

## Operational health versus learning progress

The read-only monitor returned 720 five-second snapshots from
**05:27:55.238 through 06:27:50.238 JST**. Across these sampled statuses:

| Measurement | Observed result |
| --- | --- |
| Runtime state | All `running` |
| Active / ticking NPCs | Minimum 1,024 / 1,024 |
| Additional trained samples | 14,583,451 |
| Additional model updates | 30,323 |
| Inference failures / rejections added | 0 / 0 |
| Learner rejections / stale samples added | 0 / 0 |
| Inference / learner queue sampled maximum | 0 / 0 |
| Accepted training samples/s | Median 4,055.14; minimum 3,778.77 |
| JVM CPU-core equivalents | Median 1.148 |
| Sampled Java heap | Maximum 1,783 MiB |
| Task-11 full probes in the interval | **0 successes / 6,908 trials** |

All 1,024 actors were at stage 11 throughout this interval. The live course
record showed historical certificates through task 10, but those were not
accepted as proof of the current policy's retained skills. A later status
at **06:39:05.238 JST** reported policy **542621**, **254,294,163** trained
samples, all 1,024 actors ticking and progressing, and zero cumulative
inference failures/rejections, learner rejections/stale samples and burning
bodies. These counters are runtime evidence, not survival-skill scores.

Queue, population, CPU and heap values are sampled observations, not a
claim that every intermediate instant was traced. Healthy execution and
continuing weight updates do not establish continuing skill improvement:
task 11 was visibly stalled.

## Completed canonical frozen evaluations

Both commands ran against the active Academy's atomically published canonical
checkpoint, in separate loopback-only disposable Minecraft servers. The live
learner continued. Each evaluation used one frozen model, the normal
full-difficulty reset and ordinary neural primitive actions, and verified
**zero new training samples**.

```sh
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 32 \
  --seed 2026092601 --export dist/check-20260926-all.zip

./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 64 \
  --seed 2026092602 --export dist/check-20260926-confirm.zip
```

| Evaluation | Policy updates | Trained samples | Completed, JST |
| --- | ---: | ---: | --- |
| A: 32 cases/task | 536242 | 251,241,011 | 06:29:28.414 |
| B: 64 cases/task | 539044 | 252,581,685 | 06:35:10.600 |

| Task | A: passed/cases | B: passed/cases |
| --- | ---: | ---: |
| 0 Forward-stop | 32/32 | 64/64 |
| 1 Turn-stop | 32/32 | 64/64 |
| 2 Aim-hold | 32/32 | 64/64 |
| 3 Navigate-stop | 32/32 | **63/64** |
| 4 Step-over | 32/32 | 64/64 |
| 5 Break-log | 32/32 | 64/64 |
| 6 Collect-log | 32/32 | 64/64 |
| 7 Place-block | 32/32 | 64/64 |
| 8 Craft-planks | 32/32 | 64/64 |
| 9 Craft-sticks | 32/32 | 64/64 |
| 10 Craft-workbench | 32/32 | 64/64 |
| 11 Craft-wood-pick | **0/32** | **0/64** |

A and B have different snapshots and different seeds. They are not pooled
into a single-policy success estimate or presented as a controlled comparison
of old and new algorithms. The one navigation failure is retained.

These are separate, goal-directed Academy tasks, with the documented task
supplies and bounded mechanics. Passing them is not a test of an autonomous
raw-log-to-tool chain, an unknown open world, or cooperation. Tasks 12–17 were
not tested here. Bodies remain server-side NPCs, not logged-in players.

## Where the first wood-pick exam failed

A read-only analysis validated evaluation A's complete 384-trial report and
selected all 32 task-11 trials, not only a successful-looking subset:

- All 32 trials reached an open workbench at least once.
- All 32 also entered the ordinary inventory screen.
- Out of 19,200 recorded decision observations, 1,133 were in the workbench,
  12,984 in the ordinary inventory and 5,083 with no menu open.
- Only two trials showed any recipe preview / any crafted output; neither
  completed the requested wooden pickaxe.
- No trial placed five item units simultaneously in a crafting grid:
  the recorded per-trial maximum ranged from one to four.

Thus this snapshot could reach the table, but repeatedly left its crafting
screen and failed to assemble the requested recipe. This is not proof that
every close/open operation is intrinsically wrong, nor that a particular
optimizer defect or hidden-layer saturation caused the behavior.
`TrialTrace` counters aggregate observations and the first 24 menu operations;
they are not a complete state-by-state replay. Grid-unit count is not the
number of correctly placed recipe cells.

## Paired reset diagnostic: does opening the table for it solve the failure?

A canonical checkpoint was exported once to an isolated diagnostic worktree.
Both arms used those **same policy bytes**, model **540572**,
**253,313,661** trained samples, seed **2026092603**, task 11 and 32 cases.
After completion, both on-disk policy copies still exactly matched the original.

The standard arm used the unchanged production runtime and `tests/holdout.py`.
The assisted arm used an isolated build with only this reset intervention:

```java
Pocket.Menu initial=StationPractice.initialMenu(lesson,pocketRng);
// Diagnostic-only EXAM reset; no action or recipe is selected.
if(lesson.kind()==Course.Kind.EXAM&&lesson.task()==Task.CRAFT_WOOD_PICK)
    initial=Pocket.Menu.WORKBENCH;
```

The existing reset then assigned the target container and opened the menu.
Both arms still began with the ordinary three planks and two sticks, **no
prefilled grid and no supplied output**. Difficulty, random initial pose,
horizon and success predicate were unchanged. After reset, the policy remained
free to close the menu or make any normally available primitive action.
No optimizer or teacher acted in either arm.

The inference-only JARs were byte-identical. The diagnostic runtime differed
in the reset code; the diagnostic Python driver also marked metadata and its
published result as `diagnostic_only` with `reset_intervention`.
That build was never copied into the active Academy.

| Arm | Passed/cases | Workbench observations / all observations |
| --- | ---: | ---: |
| Standard closed start | **0/32** | 1,279 / 19,200 |
| Assisted open-table start | **0/32** | 1,051 / 19,200 |

Both completed at approximately **06:38:31 JST** with unchanged weights and no
training. The standard arm's maximum grid-unit histogram was
`{1:5, 2:21, 3:5, 4:1}`; the assisted arm's was
`{1:2, 2:21, 3:6, 4:2, 5:1}`. Reaching five units once did not make a correct
wooden-pickaxe recipe or a successful trial.

For this model and paired case suite, removing the initial opening requirement
was insufficient. This narrows the problem beyond initial table acquisition;
it does not establish the exact learning cause or prove all possible
assistance ineffective. Assisted results are **not** counted as skill
certificates or mixed into the canonical evaluation panel.

The diagnostic worktree is detached at the measured production commit:
`/home/coder/workspace/botsclustersmc-diagnostics-20260926`.
Its only source edits were the reset intervention and explicit diagnostic
labels (two files; nine insertions, one deletion). Raw reports and unchanged
policy copies are under `.build/closed-standard`, `.build/open-assisted` and
`.build/frozen`. The ordinary `holdout.py` integrity checks completed for both
arms; this is not a claim that a full source-test/CI run was performed on the
diagnostic-only fork.

## Retained artifacts and next experimental boundary

The canonical, exact-model deployment bundles are retained in the active
checkout's `dist/check-20260926-all.zip` and
`dist/check-20260926-confirm.zip`. The active Academy's normal evaluation
summary is B; diagnostic-only results did not replace it.

No model/world cleanup was needed for this continuation. At the final process
check, the paired test sessions had exited, their loopback ports 25586/25587
were no longer listening, and `bcmc-training` / `bcmc-monitor` remained alive.
The production ports were 25565 and loopback-only 8765.

The next intervention should address reliable material selection, placement
and staying in the appropriate menu, while preserving the earlier skills.
Compare a justified change with retained fixed-model cases; do not substitute
more sample counts, easier promotion thresholds, another reset, or initial
table-opening assistance for evidence of improved assembly. The present
results do not isolate observation representation, shared-policy interference,
exploration or optimization as the root cause.

## 運用メモ

本稼働は `academy-current` のまま継続しています。新しい初期化や本番プラグインの
差し替えは行っていません。独立試験では作業台製作までの技能を再確認できましたが、
木のツルハシ製作は未習得です。学習処理が稼働していることと、技能が伸び続けて
いることは分けて扱ってください。

`botsclustersmc-diagnostics-20260926` は補助あり対照試験を含む実験用です。
そこの `dist/training.jar` を本番へコピーしないでください。再起動や運用コマンドは
`/home/coder/workspace/botsclustersmc-source` 側を使ってください。
