# 2026-09-26 - retention screening and first-lesson coverage

## Decision

Do not deploy the restored-review scheduling candidate. It passed its narrow
four-task screen but failed the prospectively specified fresh-seed confirmation:
wooden pickaxe crafting was **25/32**, below **26/32**. A one-case miss is not
statistical proof that the candidate is harmful, but it is a failed engineering
gate. The threshold was not lowered and a more favorable seed was not substituted.

The accepted software change is read-only `StartupCoverage`: count the first
lesson actually issued to each actor, distinguish fresh initialization from a
restored checkpoint, and separate training from frozen exams. This does not add
review credit or alter lesson selection, rewards, model, Adam, observations,
action masks, primitive actuators or success predicates.

## Source and environment

Public main at the beginning of this continuation was
`c3cc23b63c25492c80ff991098f03a9727192339` (PR #15). It had already reverted the
stone reward/reset extension from PR #14 while retaining harvest diagnostics.
The designated Home Coder workspace was `minecraft-agents`, hostname
`cw-c76e701404f844d8876a4d50`.

The live checkout is `/home/coder/workspace/botsclustersmc-source`, with
`academy-current`, 1,024 server-side NPCs and port 25565. Experiments used Java 21,
pinned Folia 1.21.11 build 14 and the Ryzen 9 9955HX host with 16 effective CPUs.
These bodies are not network-connected vanilla players.

The rejected candidate's predeclared plan and implementation were retained in
local commit `0fff6e75968b2dc7719e0f640417b409f70e942d`, authored at
2026-09-26 02:10:56 UTC, before its isolated training. The original
[gate document](20260926-resume-gate.md) is preserved without rewriting its
thresholds. Its working directory is `/home/coder/workspace/botsclustersmc-resume`.
Startup telemetry is developed separately in
`/home/coder/workspace/botsclustersmc-startup`.

The telemetry's locally tested source commit is
`c607f206f2ce884da2b48ed610775f235d20c0b2`, tree
`2ed96e800da82b12da9aa349f01c21321262b2ec`. Native GitHub object creation reproduced
that entire tree exactly. Publication adds this record and the unchanged gate
as documentation, not different execution code.

## Why a resume experiment was necessary

The complete known-good checkpoint contains policy **662,907**, **310,322,493**
accepted samples and Adam step **662,907**. Its frozen original-code replay
completed both log tasks 64/64 and wooden pickaxes 57/64; cobblestone was 0/64.
The checkpoint, full optimizer and course are retained.

The preceding stone intervention produced a serious retention regression. After
reverting code and restoring the known-good state, early and later evaluations
also showed crafting loss under the original code. Therefore the stone change
is not established as the sole cause. The following are complete observed
four-task reports, all with tasks [5,6,11,12], 64 cases and seed 2026092611:

| Snapshot | Policy | Accepted samples | Break log | Collect log | Wooden pickaxe | Cobblestone |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Earlier live baseline | 650085 | 304260306 | 64 | 64 | 63 | 0 |
| Known-good weights under temporary stone runtime, no learning | 662907 | 310322493 | 64 | 64 | 56 | 0 |
| After stone intervention learning | 664936 | 311261282 | 13 | 7 | 0 | 0 |
| Original-code early resume | 663093 | 310401941 | 64 | 64 | 14 | 0 |
| Known-good original-code frozen replay | 662907 | 310322493 | 64 | 64 | 57 | 0 |
| Original-code later resume | 672026 | 314642356 | 64 | 64 | 3 | 0 |

The same full checkpoint decoded by the production allocator issued **1,024
frontier lessons, zero reviews and zero exams** as its first lessons: 23 actors at
task 11 and 1,001 at task 12. Review debt is transient and starts at zero. A long
first frontier episode can leave earlier tasks absent from initial learner
batches; within-batch balancing cannot supply missing examples.

The candidate gave one actor in five bounded, privately seeded initial review
credit, without consuming the saved lesson RNG. The read-only decode then issued
**819 frontier lessons and 205 reviews**, with first-task counts
`[13,18,20,16,18,31,17,11,17,23,12,28,800,0,0,0,0,0]`.
This establishes scheduling coverage, not a causal explanation of forgetting.

## Isolated candidate results, including rejection

The candidate trained only in its owned, loopback-bound copied Academy with
1,024 actors and a 4 GiB heap. The live learner was not modified by this
continuation. The isolated learner was stopped cleanly before the final screen;
the full final checkpoint remains available, not only inference weights.

| Test | Policy | Accepted samples above the starting checkpoint | Break log | Collect log | Wooden pickaxe | Cobblestone |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Early, 64 cases/task | 663379 | 201512 | 64/64 | 64/64 | 52/64 | 0/64 |
| Final, 64 cases/task | 670675 | 3664945 | 64/64 | 64/64 | 55/64 | 0/64 |
| Fresh confirmation, 32 cases/task | 670675 | 3664945 | 32/32 | 32/32 | **25/32** | 0/32 |

The early snapshot overshot the approximate 80,000..200,000 target range by
1,512 accepted samples; it is reported as measured, not relabeled as within range.
The final snapshot exceeded the minimum one-million-sample budget. These are not
equal-budget paired comparisons with the original-code snapshots.

Final screening completed at 2026-09-26 **11:30:11.001 JST**. Both log thresholds
62/64 and the wooden-pickaxe threshold 52/64 were satisfied. The same frozen
weights were then reused with `--from`, tasks **0..12 in order**, 32 cases and
fresh seed **2026092613**, completing at **11:33:36.238 JST**. Tasks 0..10 each
scored 32/32, but wooden pickaxes failed the required 26/32 threshold.

All **672** final/confirmation trials completed, including every failure, with
zero evaluator training samples. The two bundles contain exactly the same model
and inference JAR. A separate Java check confirmed that the full stopped
`training.bcmc` contains those exact evaluated model bytes, policy 670675,
313987438 samples and Adam step 670675; reading it did not change its bytes.

The earlier 256-case report is retained too. The candidate's source tests passed,
including 363,756 restored-allocation checks, but those tests do not override a
failed real-Minecraft retention gate. The candidate remains stopped and rejected.

## Read-only telemetry accepted instead

`StartupCoverage` is bounded to the existing 10,000-actor maximum and an 18-task
histogram. It records each actor at most once after `Course.issue` returns a
real lesson. A consistent immutable status snapshot exposes the expected,
observed and unobserved population; foundation/frontier/review/exam categories;
all-task and training-only task histograms; and fresh/restored origin.

First issuance is not successful reset, a learned transition, completed episode,
current task distribution or skill certification. Frozen exams are not counted
as training. The diagnostics have no policy, optimizer, random generator or world
reference. They are not included in the inference JAR.

The complete source suite passed with **7,839 new startup counting, validation,
concurrency and noninterference assertions**. Tests compare exact course/RNG bytes
and subsequent real course-issued lessons with and without measurement. Existing
mathematical, mechanical, curriculum, export and evaluation checks also passed.
The inference JAR was byte-identical to the production baseline.

Two separately owned, loopback-bound real servers passed the native startup and
shutdown checks. The copied 1,024-actor checkpoint reported restored=true,
1,024 observed, zero unobserved, 1,024 frontier, zero review/exam/foundation, and
task counts 23 at stage 11 plus 1,001 at stage 12, matching the offline decode.
The fresh 16-actor Academy reported restored=false, 16 observed, zero unobserved,
16 foundation actors and all first tasks at stage zero. Both inference failure
and rejection counters were zero at the captured boundaries.

Both isolated servers shut down normally. The source known-good full checkpoint
was byte-identical before and after. This is real lifecycle/measurement evidence,
not a new learned-skill result. The operator's existing learner and monitor were
not restarted for an instrumentation-only update, so their running process does
not yet expose the new startup fields. Source integration and CI identities are
recorded separately after publication.

## Latest live-policy snapshot, not the rejected candidate

A separate canonical evaluation of the ongoing production learner completed
**416** full-condition trials at **11:38:18.890 JST**, seed **2026092614**.
Policy **684,011**, **320,361,088** accepted samples, zero new evaluator samples:

- Tasks 0,1,3,4,5,6,7,8,9: 32/32 each.
- Aim-hold (2) and craft-workbench (10): 31/32 each.
- Wooden pickaxe (11): 27/32.
- Cobblestone (12): 0/32.

This shows that crafting performance varies across later policies; it does not
erase earlier failures, certify all future weights, or establish that a particular
change caused recovery. The fixed-policy monitor displays this exact snapshot,
not a continuous certification of the live learner.

An independent ZIP audit checked all **1,344** trials across the early, final,
confirmation and latest live bundles: archive integrity, complete unique actor
sets, task counts, success totals, nonzero elapsed trials, zero evaluator learning,
model/plugin byte identities and exact final/confirmation model reuse all passed.

## Retained evidence

In the live checkout, `.build/evidence/` retains:
`harvest-baseline.zip`, `harvest-prerollout.zip`, `harvest-after.zip`,
`harvest-recovery.zip`, `harvest-restored-reference.zip`,
`harvest-original-later.zip`, `harvest-retention-before.zip`,
`retention-live-final.zip`, the pre-rollout full checkpoint, both rollout archives
and the rejected full checkpoint. No failed reports or operator worlds were erased.

In the resume worktree, `.build/` retains `resume-early.zip`, `resume-final.zip`,
`resume-confirm.zip`, `VerifySavedState.java`, and
`resume-trial/academy-current/server/plugins/BotsClustersMC/training.bcmc`.
The live and candidate snapshots have different identities and must not be pooled.

The startup worktree retains `.build/startup_live.py`,
`.build/startup-evidence/`, and the full source-test log
`/tmp/bcmc-startup-tests.log`. The isolated runtime checks are lifecycle/measurement
evidence, not additional skill evaluations.

Persistent open-world survival, the complete log-to-tool chain and cooperative
living remain unestablished. Cobblestone harvesting is still the measured frontier.
