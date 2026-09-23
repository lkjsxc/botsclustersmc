# Menu-focus delivery and matched neural evaluation

## Source and software acceptance

Implementation commit: `b9527409f58e0e4d8ed4a882bd25cc3038f762f8`.
Mainline integration: `059662f365cc1c4a067bc97956ec107794ca8958` (PR #3).

The development worktree was compared with the published commit before delivery.
After aligning a local lock-variable name and the scratch-directory ignore entry,
`git diff --exit-code` confirmed identical source. The full source suite then
passed again (`/tmp/bcmc-published-source-tests.log`). Linux and Windows CI both
passed in run `35910613477`. The original full-difficulty real-Folia diagnostic
run passed all 18 scripted tasks, including the new adversarial menu checks.
These scripted passes are mechanical reachability evidence, not learned behavior.

The two deployed JARs were also compared byte-for-byte with the evaluated build;
both matched. The inference JAR still contains no training/reset implementation.
The region-ownership review finding on PR #2 was resolved after mainline delivery.

## Fixed-weight mechanics experiment

See [the mechanism report](20260924-menu-focus.md). At policy 183278, with 32
cases per task and seed 924401, the old and new mechanics respectively measured:
planks 30/32 versus 30/32, sticks 16/32 versus 20/32, and workbench 0/32 versus
0/32. Focus and recoverable drops were changed together; this is not an isolated
causal ablation or a statistical-significance claim.

## Additional learning and matched-condition control

An isolated Academy copied the same canonical checkpoint, policy 183278 with
71,333,887 accepted training samples. It ran 1,024 real NPC learners with a 3 GiB
heap and region/inference/learner thread counts 2/1/1. It accepted 801,353 further
samples, then stopped cleanly at policy 185511 / 72,135,240 samples. Its optimizer
and canonical checkpoint were saved, its learner queue drained, and its server
shut down. This candidate did not replace the original Academy.

Both old and candidate weights were independently evaluated with the new input
mechanics, the same seed 924409, 32 cases per task, full-difficulty initial states,
and stochastic neural actions. Neither evaluation trained. The old-weight
control used a separate copy so the earlier mechanics reports were not replaced.

| Task | Before additional learning: 183278 | After: 185511 |
| --- | ---: | ---: |
| Forward and stop | 32/32 | 32/32 |
| Turn and stop | 32/32 | 32/32 |
| Aim and hold | 29/32 | 32/32 |
| Navigate and stop | 32/32 | 32/32 |
| Cross a step | 32/32 | 32/32 |
| Break a log | 32/32 | 32/32 |
| Collect the log | 32/32 | 32/32 |
| Place a block | 31/32 | 32/32 |
| Craft planks | 29/32 | 31/32 |
| Craft sticks | 22/32 | 16/32 |
| Craft a workbench | 1/32 | 1/32 |

The sticks score decreased in this sample. The sample is small and asynchronous
real-server trials are not a bitwise deterministic simulator; the table does not
establish a statistically significant change. It does rule out describing this
candidate as an across-the-board demonstrated improvement. The candidate weights
were NOT promoted. More accepted samples and successful earlier-skill tests do
not establish simultaneous crafting mastery. No reward, success threshold or
exam requirement was relaxed to hide the result.

The four completed neural experiments contain 896 trials in total, including all
failures. Reports remain in the development worktree under `.acceptance/`:
`craft-baseline`, `craft-focused`, `craft-retention-baseline`, and `craft-learning`.
Each has `server/plugins/BotsClustersMC/evaluation.json` and the per-trial
`evaluation-details.json`. The candidate training checkpoint remains stopped.

## Original Academy delivery

The original training server and its evaluator were stopped through their
normal console interfaces. The stopped main Academy checkpoint was saved under
`.cache/upgrades/menu-focus.0IKcNd/` in the original clone, together with its
status and source identity. No live learning history was replaced by the older
experimental branch.

The original clone fast-forwarded to mainline integration 059662f and rebuilt.
Startup restored the exact model and Adam state at update 194777, 76,195,986
samples, optimizer step 194777. A subsequent health observation showed 1,024
active actors, zero pending actors, zero inference failures/rejections, and
update 195080 with 76,318,587 accepted samples. This establishes actual learning
continuity across the update, not just a successful process launch. The source
worktree was clean. The dashboard answered successfully.

The existing independent evaluator service was restarted from the new source
with `--watch --interval 600`; completed scores identify their frozen weights and
must not be presented as certification of a later live policy. Minecraft port
25565 and the existing private dashboard binding were preserved.

## Remaining product boundary

These are bounded privileged-state NPC lessons, not a complete survival player.
The workbench skill remains unreliable, and no open-world goal selection,
hunger/durability model, persistent autonomous life or multi-agent cooperation
was established by these trials. The next learning improvement must demonstrate
crafting reliability while rechecking prior skills, rather than substituting
recipe scripts, inflating actor counts, or treating CPU utilization as mastery.
