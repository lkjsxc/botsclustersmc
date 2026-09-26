# 2026-09-26 - production restart and fixed-policy verification

The normalization-only candidate was rejected and remains stopped. The completed
[comparison and rejection record](20260926-normalization-study.md) preserves all
seven reports and 672 trials; those were fresh-model experiments, not the mature
production policy. The operator's permission to interrupt or restart from scratch
is recorded in AGENTS.md. Permission to reset does not require deploying a worse
candidate or deleting a useful existing model.

## Actual running deployment

Production was restarted from its intact original full checkpoint using the
current unnormalized main implementation in
`/home/coder/workspace/botsclustersmc-source/academy-current`. No normalization
code or experimental weights were installed. Server startup restored policy
705288, 330484383 accepted samples and Adam step 705288 at 13:03:02 JST.
The original world and checkpoint had been preserved throughout both experiments.

Port is 25565, count 1024 server-side NPCs, heap 8 GiB, and automatically selected
region/inference/learner threads 9/1/5. The existing online-mode=true setting was
not changed. The native learner runs in tmux session `bcmc-training-main`; the
private monitor continues on its original localhost:8765 address and data path.
The installed training JAR is byte-identical to the rebuilt current-main JAR.

Three sampled health reports from 13:04:13.516 to 13:04:23.516 JST showed all 1024
actors active, ticking and progressing, with accepted samples increasing from
330730147 to 330768835. Inference failures/rejections, rejected/stale learner
samples and burning bodies were zero in each report. These sampled health checks
are not a guarantee of uninterrupted future operation or of any learned skill.

The StartupCoverage implementation previously merged in PR #16 is now active
in the running process. It recorded all 1024 first-issued lessons, restored=true,
zero unobserved actors, 1024 frontier lessons, and zero initial review/exam/
foundation lessons. The first-task population was 3 at task 11 and 1021 at task 12.
These are curriculum allocations, not certificates for the current policy.

## Frozen evaluations: distinguish the restored and subsequently learned models

The first evaluator started soon after restart and captured the restored checkpoint
before its next persisted update. Therefore its result belongs to the exact
starting model, not to a post-resume learned model. It completed 416 cases with
ordered tasks 0..12, 32 each, seed 2026092625, at 13:06:14.714 JST:

- Policy 705288; accepted samples 330484383; zero evaluator training samples.
- Tasks 0,1,3,4,5,6,7,8: 32/32 each.
- Aim-hold (2) and craft-sticks (9): 31/32 each.
- Craft-workbench (10): 30/32; craft-wood-pick (11): 29/32.
- Mine-cobblestone (12): 0/32.

A second, separately retained evaluation captured policy 707296 after the live
learner had accumulated 331418430 accepted samples: **934047 additional accepted
samples since restoration**. It completed 128 full-condition cases, ordered tasks
[5,6,11,12], 32 each, seed 2026092626, at 13:10:00.112 JST:

| Task | Successes / cases |
| --- | ---: |
| Break log | 32/32 |
| Collect log | 32/32 |
| Craft wooden pickaxe | 27/32 |
| Mine cobblestone | 0/32 |

This second evaluator also added zero training samples. It used a later frozen
policy, not the same weights as the starting-model evaluation. The two seeds
also differ, so the small crafting difference is not an isolated causal estimate
of restart damage or improvement. No continuous skill-retention claim follows.
Cobblestone remains unlearned in these evaluations; open-world survival and
cooperative living are not established.

An independent audit checked all **544** production-evaluation trials, archive
integrity, complete unique actor sets, exact task denominators and success totals,
positive elapsed ticks, zero evaluator learning and the recorded policy/plugin
byte identities. Both production reports used the same inference build, while
the policy identities correctly differed. The 544 production trials must not be
pooled with the 672 fresh-model experimental trials as one skill score.

## Source verification and final operational checks

Main at the operational source check was
`41cf2a1e69b6f539938b6920966c87041886d972`. Its only differences from the previously
validated implementation at `58ed06a` were AGENTS.md and the normalization-study
record. The complete current-main `./test.sh` suite was rerun successfully,
including 7839 startup coverage checks and the existing math, mechanics,
concurrency, export and evaluation tests. The rejected candidate's 75198
normalization checks are NOT claimed as part of production main.

The final scoped process check found only the intended training JVM, PID 3081801,
and no holdout or experimental training JVM. The main worktree was clean and
synchronized. The installed training JAR again matched the current build exactly.
This receipt itself changes documentation only; it does not alter the running
model, rewards or learner. Documentation-only pushes do not run the repository's
path-filtered CI; no new CI pass is invented for them.

## Retained evidence

Under the live checkout's `.build/evidence/`:

- `normalization-production-restart.log` and `normalization-production-health.json`.
- `normalization-production-resume.zip`, containing the exact starting model.
- `normalization-production-postlearning.zip`, containing the later model.
- `normalization_production_audit.py` and `normalization-production-evaluation-audit.json`.
- `normalization-production-source-tests.log` and both evaluator logs.

The rejected candidate retains its early/final checkpoints, all seven experimental
bundles, source/test logs, mutation evidence and verified Git bundle in the
separate normalized worktree. No operator world, failed trial or complete
checkpoint was erased. Ordinary production learning is running again; the
rejected normalization-only experiment is completed, not left training.
