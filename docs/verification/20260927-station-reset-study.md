# Station reset study — 2026-09-27

## Plan fixed before the reset-intervention trials

This is a test-only diagnostic, not a learner intervention or a claim of improved
wooden-pickaxe skill. Source baseline is
`e45b8de2869be75005939a885de5c2d120c4b646`. Work uses the separate worktree
`/home/coder/workspace/botsclustersmc-station-study` in shared Home Coder workspace
`lkjsxc/tomato-ocelot-73`. The running Academy remains in the main checkout.

The native ordinary evaluator completed 384 full-condition trials, tasks 0–11 in
that order, 32 cases per task, seed 2026092703. Its saved policy is update 482209,
137210877 accepted samples, identity
`99e32f96a7017035fe4e9dc2e37b911b8ed90e817a308f28531bd081d83db3c6`.
The exact model, matching inference JAR and every trial are retained in
`.build/station-baseline.zip`. New evaluator training samples were zero.
Success counts in task order were 32, 32, 31, 32, 32, 32, 32, 32, 32, 32, 29, 0.
The three workbench failures and one aiming failure are retained, not discarded.

The next planned matrix uses ONLY that saved policy, never a changing canonical
checkpoint. Tasks are `[11]` in every matrix run; cases per task are 32. Case
seeds are 2026092704 and 2026092705. Each seed runs all three conditions below:

1. `none`: unchanged full reset, closed menu and raw supplies.
2. `workbench-open`: initial workbench open, unchanged raw stock and empty cursor.
3. `pickaxe-grid`: initial workbench open, its five ingredient units arranged at
   reset, empty cursor. The output is only a preview: no crafted or owned pickaxe
   is supplied. The fixed policy must select the actual output action.

The matrix has 192 trials, including all failures. Its accepted-training-sample
budget is **zero**. No adaptive stopping, best-seed selection or certificate
mutation is permitted. Each assisted condition must run the exact intervention
once per actor before any policy decision, mark `diagnostic_only=true`, identify
`reset_intervention` and report all 32 applied resets. Missing trials, mismatched
identities, changed weights, training state creation or malformed diagnostic
labels reject the run as software evidence. An operational failure must be
retained and explained separately rather than silently removed.

The recipe outcome, horizon, random pose, policy inputs, action masks and neural
actions after reset remain unchanged. No policy or action RNG is consumed by the
reset helper. Within the matrix, task order, case count and actor IDs are held
fixed, so reset seeds and initial action RNG specifications correspond. Real
asynchronous scheduling can still differ; these are not identical trajectories.
The 12-task native baseline assigns different actor IDs to task 11 and is NOT a
paired-randomness control for the task-only matrix.

Run at most two disposable loopback-only test servers together, each with a
2 GiB maximum heap and two JVM-visible processors, while leaving the production
learner and monitor running. No production weights, optimizer, course, rewards,
training resets or model architecture are changed. The full-condition results
remain the only competence measurements; assisted scores cannot replace them.
The standard native validator, export and replay paths reject assisted reports.

The study is descriptive failure localization. Even a large condition difference
would not, by itself, identify a particular neural architecture, reward scale or
optimizer as the cause. Any later training change needs its own separately owned
experiment and earlier-skill retention checks.
