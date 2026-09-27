# Reserved review and checkpoint-resume retention — 2026-09-28

## Prospective protocol, fixed before experimental training

This is a separate hypothesis from rejected PR #20. That stone-reward candidate
failed its early retention gate and must not continue. The present candidate is
based on unchanged main `bb0dcfd1d1c5e94f8276e92367187e064145e1a9`, without any stone
reward, reset, policy, optimizer, model-shape or inference change.

The unchanged resumed control in the first study lost workbench 15/16 to 3/16 and
wooden-pickaxe 14/16 to 0/16 on matched case seed 2026092811. Initial policy 648322
had 184230639 accepted samples and six actors at task 11, 506 at task 12. Its
complete retained training checkpoint, including Adam and actual course history,
is the common starting point for this study; no live weights will be replaced.
Initial policy SHA-256:
`7b3cd742be2edcc429dd17aa46946428460e04363004e620dc625b31770ad6a3`.

## Intervention and boundaries

The 18 frontier cohorts each share an effort ledger because their actors train a
shared network. At issue time, reserve that task's existing maximum episode horizon.
Select using actual effort plus separately tracked unconsumed reservations, and
balance earlier tasks using actual plus reserved review ticks. Real elapsed ticks
replace the corresponding forecast; normal completion or abandonment releases the
unused remainder. Reservations never become training samples, actual telemetry,
rewards, completed episodes or skill certificates. All state is bounded by the
existing 10000-actor limit. One actor never waits for a cohort or evaluation barrier.

Compared with the old policy, the scheduling of training lessons and their RNG
consumption changes. Peer review debt is intentionally shared only within a
frontier; earned progress, exams, demotion and curriculum difficulty remain per
actor. Practice observations, primitive actions, loss weights, replay, rewards,
network, Adam, frozen evaluation conditions and complete checkpoint byte format
remain unchanged. Full-condition evaluation uses no training allocation.
The inference JAR must be byte-identical. This is not a claim of exact transient
scheduler continuation: like before, actual effort history is not serialized.
Empty restored pools now reserve coverage as work is assigned instead of sending
all actors to long frontier episodes before any rehearsal can occur.

## Isolated, pinned comparison

Use a fresh control Academy and a fresh candidate Academy, each initialized from
the exact retained `initial-training.bcmc` from the first study, not from its failed
trained arms. Pin clean source commits and preserve the driver, source identities,
initial state, all intermediate/final states, logs and complete trial reports.
Use 512 actors, seed 7, 2-GiB heap, region/inference/learner threads 2/1/1,
`-XX:ActiveProcessorCount=2`, and separate loopback-only server ports per arm.
Leave production training, its monitor, its evaluator timer and other projects
unchanged. At most two study servers run concurrently; evaluate stopped arms
sequentially. Abort on I/O errors, unhealthy status, stale telemetry or less than
1 GiB measured available memory. Each training phase has a 2400-second bound.

Before training, evaluate the exact initial model through BOTH source builds on
ordered tasks `[0,1,2,3,4,5,6,7,8,9,10,11,12]`, 16 cases each, seed 2026092821.
Require identical policy identities and task scores; retain both complete reports.
Every evaluation must complete all cases with zero new training samples.

Train both arms for 250000 additional accepted samples, then stop normally and
retain complete states. Record actual drain overshoot, never claim exact equal
sample counts; reject a study execution with overshoot exceeding 50000 samples.
Evaluate all 13 tasks, 16 cases each, seed 2026092821. Early candidate gates:
all tasks 0–10 at least 14/16; task 11 at least 10/16; no more than two successes
below control on tasks 0–10 or three on task 11. Require at least a six-success
advantage over control on workbench OR wooden-pickaxe. Task 12 is recorded, not
required to improve. Also require every task 0–12 in initial candidate training
coverage, at least 5% actual review ticks and at least 50% of accepted samples on
task 12 at the last pre-drain snapshot. The unchanged control is expected to be a
regression comparator: unlike the rejected stone study, its own low retention is
not a reason to erase the candidate comparison or demand its recovery.

Any failed candidate gate forbids continuation or production adoption. If all early
gates pass, resume each arm from its own exact stopped state up to 1500000 total
additional accepted samples. Stop and evaluate all 13 tasks, 32 cases each, seed
2026092822. Re-evaluate each SAME exported model with `--from` on fresh seed
2026092823, 32 cases per task. On EACH final seed independently require candidate
tasks 0–10 at least 30/32 and no more than two below control; task 11 at least 24/32
and no more than three below control. Task 12 remains reported without a success
threshold: this study concerns retention on resume, not learning a new mining skill.
Require final actual review tick fraction between 10% and 35%, at least half of
accepted samples on task 12, and at least 100 accepted samples from every task
0–11 in the final phase. These workload gates prevent a nominal retention result
obtained merely by stopping frontier learning. Preserve all early failures and
all seeds; do not change thresholds, pool seeds or continue a failed early gate.

These are prospective engineering acceptance criteria on one training seed, not
multi-seed statistical superiority or general-survival evidence. A passing isolated
study is still not permission to replace a production checkpoint silently. Any
later production rollout needs a complete pre-rollout snapshot, an explicit source
and state identity, and a fresh early full-condition retention check.
