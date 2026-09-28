# Goal-column transfer screen — 2026-09-28

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
