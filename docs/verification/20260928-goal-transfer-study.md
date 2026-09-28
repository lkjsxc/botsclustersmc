# Goal-column transfer screen — 2026-09-28

## Completed decision: reject the zero-training candidate, retain the diagnostic

Implementation and prospective declaration:
`2e333c80d1c712a39adae4922045b97a6c8431c8`, tree
`7caa0649fec567bd6272ec97d6ac4773cac79225`.

The declared first screen failed. Neither arm mined cobblestone in any trial;
the unchanged wooden-pickaxe result also fell below the declared absolute floor.
No confirmation seed or additional candidate training was run. The transformed
weights were not installed in the live Academy. This result rejects this exact
zero-training transfer, not every possible transfer-preserving learner.

Main receives the offline diagnostic, its source tests and this negative result,
NOT a replacement neural policy, learner or runtime behavior. Core, plugin and
training source are unchanged. Both runtime JARs are byte-identical to the base.

### Full-condition, fixed-policy results

Both arms used the source and runtime specified below, seed **2026092861**, and
32 cases for EACH task. Every one of the **832 trials** finished, including all
failures, with **zero new training samples** and no reset intervention.

| Task | Control /32 | Goal-copy candidate /32 |
| --- | ---: | ---: |
| 0 forward-stop | 32 | 32 |
| 1 turn-stop | 32 | 32 |
| 2 aim-hold | 32 | 32 |
| 3 navigate-stop | 32 | 32 |
| 4 step-over | 32 | 32 |
| 5 break-log | 32 | 32 |
| 6 collect-log | 32 | 32 |
| 7 place-block | 32 | 32 |
| 8 craft-planks | 32 | 32 |
| 9 craft-sticks | 32 | 32 |
| 10 craft-workbench | 32 | 32 |
| 11 craft-wood-pick | 19 | 19 |
| 12 mine-cobblestone | 0 | 0 |

Task 11 fails the predeclared >=20 floor in BOTH arms; this is not evidence that
the goal copy damaged crafting. Task 12 independently fails both >=16 and the
+8 relative improvement requirements. No threshold was changed after seeing the
control's 19/32. On tasks 0–11, all 384 paired trial success flags also match.
The trajectories are not asserted identical: ten paired elapsed-tick counts and
26 diagnostic records differ despite the preserved non-recipient functions.

### What changed before completion, and what did not

The following are decision-boundary observations in the 32 stone trials, **not**
per-tick time shares or certified intermediate skills:

| Stone diagnostic | Control | Candidate |
| --- | ---: | ---: |
| Decision-boundary observations | 19,200 | 19,200 |
| Menu-focused selections | 16,495 | 12,765 |
| World-dig selections | 736 | 2,002 |
| Held-pick observations | 649 | 969 |
| Target-pick contact observations | 1 | 79 |
| Trials with any observed target-pick contact | 1 / 32 | 29 / 32 |
| Maximum sampled target-pick progress | 1 tick | 14 ticks |
| Trials with any block broken | 0 / 32 | 0 / 32 |
| Trials with any item collected | 12 / 32 | 29 / 32 |

The actuator needs 40 sustained ticks to break stone with a pick. These sampled
contact diagnostics do not establish an exact maximum over every server tick.
More contact did not produce a broken block or a provenance-correct cobblestone
pickup. The any-item pickup count is especially misleading here: the reset
supplies a pick, and dropped/recollected stock is not newly mined cobblestone.
No any-item count is used as a completion signal.

This is evidence of changed approach/contact behavior under the copied goal,
not successful mining, learned cooperation or post-training skill retention.
The donor's original lesson mask disables menus while the recipient's does not.
The experiment deliberately retains the recipient mask. Different target height,
material, tool requirements, observation history and menu/selection behavior can
all matter; these data do not isolate one of them as the sole cause.

A next representation/learning study should preserve the learned base function
and test stable tool/contact control under the intended deployment affordances.
Blindly increasing population, copying an easier task's action mask, or granting
credit for any pickup does not address this failed completion gate. New training
must be separately declared and must measure earlier-skill retention again.

### Verification and retained evidence

Two complete local `./test.sh` runs passed with Java 21.0.12.1, a 768-MiB source-
test heap cap and two JVM-visible processors. The dedicated suite performed
**449,290 checks**: exact parameter support, 18-goal independent forward oracles,
scalar/batch agreement, policy round trips, immutable inputs and non-overwriting
file boundaries. This is implementation verification, not 449,290 game trials.

PR CI [36391964265](https://github.com/lkjsxc/botsclustersmc/actions/runs/36391964265)
passed Ubuntu and Windows source/API checks plus the synthetic observatory.
Optional broad live, Paper, Windows-live and retention jobs were skipped.
The two actual Folia 1.21.11 build 14 evaluations above were separate workspace
runs. Both exited normally; port 25587 was no longer listening afterward.

The independent pair audit checks ordered task/count/seed identities, all actor
partitions, exact success totals, positive elapsed ticks, policy/runtime/exam-JAR
identities, unchanged input weights, zero evaluation learning and absence of
training checkpoints. It applies the original absolute and relative thresholds.
A premature audit invocation refused the incomplete candidate output; neither
arm was repeated or changed. The final complete audit is retained separately.

Artifact identities:

- Common runtime: `bf75154c58e56c91918aa1e05baaa6cf1465a9afac119da01f59e0d21b45b006`.
- Unchanged inference JAR: `d076ec6e18bc9be7fe6da825d01e01930dd86573ff6be147d52243620948dee5`.
- Parent policy: `726e1a7c99416b6a1802b1b8130da1344902d747f93946ca31f26bd849c11651`.
- Transformed policy: `989003189686c3fbc7265e144693d9f46cb851a253cf6da1ebc57386352a26ef`.
- Control full report: `d220043b1a449c32816581116a61e53437a7ecfb60ff55ee6ec481e6bb673fba`.
- Candidate full report: `8137e9b023f12595fea4cf258e8f419b03b864de0d2e8e518e6feff65c5c4bdd`.

[All paired trial outcomes and stone counters](data/20260928-goal-transfer-trials.json)
are published as compact evidence, including every failed case. Complete original
reports, per-decision aggregate diagnostics, policy copies, runtime JARs, manifests
and server logs remain under `.build/goal-transfer-{control,candidate}-first/`
in the isolated worktree. `.build/goal-transfer-input/` retains the immutable
parent, candidate and transfer manifest. The audit and its machine-readable result
are `.build/compare_goal_transfer.py` and `.build/goal-transfer-comparison.json`;
the final source receipt is `.build/goal-transfer-source-final.log`.

### Live learning remained separate

At **2026-09-28 16:36:31.593 JST**, the unchanged live Academy reported 512 active,
ticking and progressing actors, policy 67843 and 17,043,785 accepted samples.
Its task population was `[0,0,0,3,5,9,209,286,0,0,0,0,0,0,0,0,0,0]`, with zero
inference failures/rejections, retired actors or burning bodies. Supervisor
557629 and server 557694 remained running on the existing port 25566; this
session did not stop, reset, replace or change their settings. These operational
counters and historical course progress are NOT a frozen evaluation of the
current live policy, and the older policy used by this study was not restored
into production.

## Prospective declaration (before either new fixed-policy evaluation)

Base source: `52d335e75c1ca235959e2686d86dfd11ad3cb6a7`.
Workspace: `lkjsxc/tomato-ocelot-73`; isolated worktree
`/home/coder/workspace/botsclustersmc-goal-transfer`.

Question: can the already learned collect-log controller initialize stone mining
without modifying the observation encoder, primitive actions, recipient action
mask, rewards, resets or learner? This is a narrowly scoped transfer screen, not
an independent-expert architecture, goal selector or cooperative-policy claim.

Read the exact saved policy from the completed conditional-menu control study:
policy update 860480, 247715742 training samples, identity
`726e1a7c99416b6a1802b1b8130da1344902d747f93946ca31f26bd849c11651`.
This saved model is read-only evidence, not the changing live Academy. Retain its
bytes once in a new study-owned directory. Do not copy or modify Adam, course
certificates, source checkpoints or operator worlds.

The candidate copies only the 96 first-layer weights for donor task 6
(`collect-log`) into recipient task 12 (`mine-cobblestone`). All other weights
remain unchanged. Source update/sample counters are inherited lineage, NOT new
neural training. The runtime still receives the actual stone goal and the
unchanged stone action mask. In particular, the donor's narrower lesson mask
must NOT be transplanted to suppress menus or supply an answer.

For a one-hot goal, only the recipient's real-valued network function changes.
Pure tests compare every changed parameter against an independent column oracle,
all 18 tasks against independent input substitution using the recipient mask,
scalar/batched inference, strict model round trips and input/output preservation.
This does not guarantee that the new goal works in Minecraft, that invalid
non-one-hot inputs are unaffected, or that later shared-network training retains
skills. The transfer utility stays outside both runtime JARs.

## Fixed comparison and rejection rules

Both arms run exactly the same freshly built base runtime. Control uses retained
source weights; candidate uses the declared column copy. Each runs ordered tasks
0 through 12, 32 full-condition stochastic cases per task, seed **2026092861**,
in sequential disposable loopback-only Folia servers. All 416 trials per arm
must finish with unchanged input weights and zero evaluation training. Preserve
failures and complete trial reports, not just successful traces.

The first screen requires candidate tasks 0–10 >=30/32 and no more than two
successes below control on any task; task 11 >=20/32 and no more than two below
control; task 12 >=16/32 and at least eight more successes than control. The
lower pickaxe floor acknowledges this input model's previously measured 22/32;
this screen is not a new claim of reliable wooden-pickaxe mastery.

Only if the first screen passes, repeat BOTH unchanged saved policies with seed
**2026092862**, the same ordered tasks/counts and the same per-seed thresholds.
Do not choose another donor, edit the initial state, replace a failed trial,
relax a threshold or search seeds to turn this declaration into a pass. A failed
first screen stops this candidate; confirmation is not run. Even two passes only
support bounded zero-training transfer, not a training-retention or deployment
approval. Any later learner change requires a separate declared comparison.

Keep production learning and its observer running unchanged at port 25566.
Use experiment port 25587, no public exposure, at most one study server, a 2-GiB
server heap and two JVM-visible processors. The shared workspace has other
projects; do not stop them or erase their files. Record actual process health,
runtime/source identity, all trial denominators and any unexecuted phases.

## Reproduction

After personally accepting the Minecraft EULA, build from the declared source.
The offline tool requires an existing parent and a NEW output directory:

```sh
java -cp dist/training.jar \
  tests/transfer/org/botsclustersmc/diagnostic/GoalTransfer.java \
  /path/to/saved/policy.bcmc .build/goal-transfer-input 6 12

EULA=true JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2 \
  python3 tests/holdout.py --policy .build/goal-transfer-input/source-policy.bcmc \
  --output .build/goal-transfer-control-first --tasks 0 1 2 3 4 5 6 7 8 9 10 11 12 \
  --cases 32 --seed 2026092861 --port 25587

EULA=true JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2 \
  python3 tests/holdout.py --policy .build/goal-transfer-input/policy.bcmc \
  --output .build/goal-transfer-candidate-first --tasks 0 1 2 3 4 5 6 7 8 9 10 11 12 \
  --cases 32 --seed 2026092861 --port 25587
```

`transfer.json` distinguishes the exact source/candidate and inherited counters.
A successful utility/holdout exit means data integrity, not that the screen
passed. The public record will retain the original declaration and add results.
