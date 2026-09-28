# Conditional menu-focus study — 2026-09-28

## Decision: do not adopt this candidate

PR [23](https://github.com/lkjsxc/botsclustersmc/pull/23) is a resolved negative
screen, not an approved runtime change. The first prospective fixed-weight gate
failed. Do not run its conditional confirmation phase or install its learner.
The original production learner continues uninterrupted. No production
checkpoint, world or runtime was replaced by this study.

Declaration commit: `b9ef803`. Candidate implementation:
`8c10962d588ead38c56137671d8950eb78f2bb42`, tree
`6360fe2e0432e350cf52647c2196f211b795867f`. Baseline source: `5196232`.
The complete declaration and candidate tests remain in the PR's source history.

## Question and boundaries

When opening inventory, the actuator ignores the six simultaneous world-control
heads. The existing distribution still scores those attempted inputs. That is a
valid latent-action distribution, not an invalid likelihood estimator.

The candidate represents opening as a single canonical input instead, with:
`P(menu) * P(world controls | not opening) * P(slot | click)`.
Likelihood, entropy and its full parent/child derivative, the fixed exploration
prior and old-policy-weighted KL were updated together. No recipe, tool selection,
forced menu closing, reward, reset, curriculum, outcome predicate, neural shape
or optimizer setting was changed.

This is NOT just a behavior-neutral refactor. Although physical-action marginals
match at one fixed observation, the candidate draws the menu head first and puts
canonical idle world controls in the previous-action observation when opening.
Its subsequent observations, random stream and entropy/prior objective therefore
differ. A requested edit to preserve the old random-draw order was blocked by the
tool safety check; it was not retried or routed through another tool.

## Completed tests

The full local `./test.sh` source/API/artifact-separation suite completed with
`source_test_exit=0`. The conditional distribution suite reports 217654 checks,
including independent enumeration of 513 distinct mixed-control tuples, directly
enumerated finite-difference objectives, 40000 sampled tuples, canonical opening,
and unreachable-support boundaries.

Four separately compiled deliberately incorrect implementations were each
rejected by assertions: unweighted world entropy, a ghost opening score gradient,
a missing parent/world entropy derivative, and unweighted world KL. An unchanged
copy passed. Compilation failures did not count as detected mutants.

PR CI [36368460714](https://github.com/lkjsxc/botsclustersmc/actions/runs/36368460714)
passed Ubuntu source/API, Windows source/API and synthetic observatory checks.
Optional retention, broad live, Paper and Windows-live jobs were skipped, not
passed. The real fixed-policy runs below were separate workspace experiments.

## Real fixed-policy comparison

One canonical checkpoint was copied once by the existing holdout harness.
Both arms used its exact exported weights: policy update **860480**, accepted
training samples **247715742**. Ordered tasks 0–12, 32 full-condition stochastic
cases each, seed **2026092851**, separate sequential loopback-only Folia servers.
Each arm completed every one of its 416 trials with zero evaluation learning.

| Task | Baseline /32 | Candidate /32 |
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
| 9 craft-sticks | 32 | 31 |
| 10 craft-workbench | 31 | 32 |
| 11 craft-wood-pick | 22 | 20 |
| 12 mine-cobblestone | 0 | 0 |

The declaration required candidate tasks 0–10 >=30/32 and no more than two below
baseline; task 11 >=24/32 and no more than three below baseline. Task 11 fails
the absolute floor, not the relative margin. The baseline itself scored below
that absolute floor. This does NOT establish that the candidate statistically
damaged crafting; it establishes that the declared adoption screen did not pass.

No candidate training was performed. These data neither measure post-training
retention nor reject every future use of conditional controls. The second seed
2026092852 was conditional on a first-screen pass and was not run. No failed
trial, threshold or seed was replaced. Neither arm demonstrated stone mining,
let alone cooperative survival.

## Reproducible identities and retained evidence

Common policy:
`726e1a7c99416b6a1802b1b8130da1344902d747f93946ca31f26bd849c11651`.

Baseline runtime:
`3ad4de980018e5f98201eafa83c5cd4660ab9294723427bd4501c16b246697f2`.

Candidate runtime:
`f01f2971331d3832e0f6f79342e8b1d9065085eab3ce73939748bb1867600a08`.

Full report identities, baseline then candidate:
`503aa19e159bfb7ea65ab7ad3bf815d5277cfb91c03acc44873eb5fdafe11b4d`;
`ebcea43975888ba72f8b970269e43a4b0d2e7d8e05b8344bcbcdab14d88bfd89`.

Workspace: `lkjsxc/tomato-ocelot-73`.
Evidence worktree: `/home/coder/workspace/botsclustersmc-menu-focus`.
Under its `.build/`, `focus-control-first/` and `focus-candidate-first/` retain
all trials, input artifacts, metadata and logs. `focus-first-comparison.json`
records the unchanged gate, generated by `compare_focus.py`, which independently
checks trial partitions, success totals, policy/runtime identities and absence
of evaluation training state. An initial verifier spelling error was corrected;
it did not alter either report. `conditional-full-tests.log` and
`focus-mutants.log` retain source and mutation results.

Both disposable exam processes exited normally. No candidate learner or extra
monitor was started. Production services and the periodic evaluation timer were
not stopped or restarted. Main receives this evidence and the cooperative-survival
direction, not the rejected executable implementation.

## Production observation after publication

At 2026-09-28 11:15:08.356 JST, a fresh read-only status snapshot reported
512 active, ticking and progressing actors, policy 866968 and 249667099 accepted
samples, at 2006.81 samples/s for that status interval. Inference failure/rejection,
learner rejection/staleness, burning and retired counts were all zero. Supervisor
PID 141387 and server PID 141440 were unchanged and had run for over 15 hours.
This is an operational snapshot, not a skill-evaluation result or uptime promise.

The first evidence/direction publication is main `3810377ac338d3109aaa075e8f20f056611558c6`.
A whole-source comparison with `5196232` confirmed no changes under core, plugin,
training, host or tests. The production build's training JAR still matched the
baseline runtime identity above. Only documentation is integrated from this study.

A fresh complete main `./test.sh` run after the first publication also passed,
including source/API compilation, native report/replay validation and inference
artifact separation, with `source_test_exit=0`. Its receipt is
`/home/coder/workspace/botsclustersmc/.build/20260928-main-after-focus.log`.
This is separate from the candidate's passing suite; the two implementations
must not be conflated.
