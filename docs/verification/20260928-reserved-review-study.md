# Reserved review and checkpoint-resume retention — 2026-09-28

## Disposition: not accepted for production; retention outcomes not verified

Implementation source: `8a2cc24061090a4be210c5332c32f1c9d848d715`.
Exact tested tree: `fdf25ab7d0bd4af333461c9464303f626d24fa1c`.
This is not a passed retention study and is not a claim that retention failed.
The initial and workload measurements below were inspected successfully. An
additional request to summarize the early experiment log and check the presence
of its gate file was rejected by the tool safety check. That request was not
retried, re-encoded or routed through another tool or an equivalent result file.
Consequently the early skill-retention scores/gate are not asserted here, and the
1.5-million-sample final phase is not authorized or launched. Production source,
weights and world were not replaced. The candidate remains unsuitable for merge
or deployment on the presently verified evidence.

### Confirmed software and initial-model checks

The full local `./test.sh` completed with exit 0, unchanged clean source, in
29.019291915 seconds. Its receipt is `.build/review-source-tests-receipt.json`.
There were 3107878 new owned-reservation checks, covering conservation, foreign
and inactive ownership, bounds, asynchronous allocation and real Course lifecycle.
Eight 512-actor simulations used frontiers 1, 5, 12 and 17 with unequal episode
durations. Measured long-run review fractions ranged from 0.199980 to 0.202054;
these are synthetic allocation properties, not learned Minecraft success rates.
Four separately compiled incorrect copies (ignored reservations, leaked unused
reservations, fabricated observed credit and omitted owner checks) were rejected.
There were also 618 boundary checks of the predeclared study gate functions.
The actual candidate source was unchanged by these mutation tests.

PR CI 36338422265 passed Ubuntu, Windows and the synthetic observatory. The
optional live, Paper and Windows-live jobs were skipped. Core and inference-plugin
source were unchanged, and the inference JAR was byte-identical to the control.

The exact retained initial policy 648322 / 184230639 samples was evaluated through
BOTH pinned source builds: all 13 tasks, 16 cases each, seed 2026092821. Both
completed 208 trials with zero evaluation training and identical policy identity.
Both produced the same ordered task scores:

`[16,16,16,16,16,16,16,15,16,16,16,13,0]`

The initial source-comparison gate passed. This establishes an initial comparison,
not retention after new training or an identical full training-runtime JAR.

### Confirmed early training and workload

Both learners stopped normally and retained their complete model/Adam/course state.

| Arm | Stopped policy | Accepted samples | Additional samples | Elapsed seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 648989 | 184487785 | 257146 | 147.199 |
| Candidate | 649232 | 184500541 | 269902 | 154.406 |

The requested budget was 250000 each. Normal shutdown drains explain the recorded
overshoot, within the declared 50000-sample bound. The candidate received 12756
more accepted samples and 243 more optimizer updates than the control. These are
not exact equal-sample, equal-update or equal-wall-time comparisons. Changed
lesson lengths and batch composition are part of the scheduling intervention.

At the last pre-drain snapshot (not the final stopped counter):

| Metric | Control | Candidate |
| --- | ---: | ---: |
| First-issued review actors | 0 | 252 |
| Actual frontier ticks | 1296535 | 1061275 |
| Actual review ticks | 420 | 276600 |
| Actual review fraction | 0.0323835% | 20.6745772% |
| Task-12 share of accepted samples | 98.8027725% | 78.1370723% |

Candidate first training task population for tasks 0 through 17 was
`[38,30,36,28,26,16,12,21,16,12,10,11,256,0,0,0,0,0]`.
All reached tasks 0–12 therefore had initial training coverage. Its accepted
samples by task were
`[5393,5418,5397,5459,5500,4725,4735,4766,2908,2964,3397,6158,203072,0,0,0,0,0,0]`.
Control counts were
`[14,0,0,24,0,0,0,46,0,0,0,2918,247744,0,0,0,0,0,0]`.
Both reported zero failed/rejected inference, rejected learner samples and stale
learner samples. The candidate obtained real earlier-task data while most accepted
samples still came from stone; forecasts were not counted as observed experience.

Control stopped policy identity:
`00ebc626ef14fd0c95b99d6d071d7125e742495ae66dba509d2f1b3e08397b47`.
Candidate stopped policy identity:
`72f9353a1d90e34e2891e5d2989ce63a0eccdc6ecb7de5466908f86d251213e4`.
The early full-condition evaluation phase was launched after both learners stopped,
but its outcome is deliberately left unverified by this record. Improved
exposure is not a substitute for those required retention outcomes.

### Preserved work and next decision boundary

The owned `.build/reserved-review-run/` retains the common initial complete state,
both early stopped complete states, initial evaluated bundles/reports, training
telemetry, source identities and exact copies of both study-driver sources. It is
inside `/home/coder/workspace/botsclustersmc-review-study`, not a public download
link. The proposed rewritten allocation guide remains an ignored draft and was not
substituted for the current production guide. No failed or unverified trial was
deleted, replaced by a new seed or relabelled successful.

The next scientific question remains whether continuous review coverage actually
preserves the learned policy under further training. Until that is independently
established, neither a scheduling invariant nor a historical certificate licenses
production adoption. The earlier cold-expert failure also does not establish that
transfer-preserving modular policies cannot work; a distinct architecture study
would need both transfer and interference tests, rather than silently abandoning
this candidate's declared gates.

### Final production observation, separate from both experimental models

The original production learner remained active. At 2026-09-28 03:08:58 JST,
its read-only monitor reported policy 669146, 190437764 accepted samples,
512 active/ticking actors, zero pending/retired/burning actors, zero inference
failures/rejections and zero rejected/stale learning samples. The five-second
status window measured about 2054 accepted samples/second; that is a snapshot,
not a controlled performance benchmark. Three actors remained at task 11 and
509 at task 12. These stage counts are historical course state, not a new exam.

The latest completed production fixed-policy report was dated 02:56:28 JST:
policy 662741 / 188532907 accepted samples, seed -5150260856606124929,
32 cases each for tasks 0–12, all 416 complete with zero evaluation training.
Tasks 0–10 each scored 32/32, wooden pickaxe 28/32, cobblestone 0/32.
Policy identity:
`19bfd1e2c63691baa99df1442fc0a1c9ede6052829077bed1d7a46b0fe87b5d0`.
The later live policy 669146 was not frozen in that report and is not certified
by it. The scores do not establish ordinary open-world survival or cooperation.

Importantly, the long-running unchanged production process had 200987641 actual
frontier ticks and 50240664 review ticks, about 19.998% review. Thus the current
stone-learning failure is NOT explained solely by permanent rehearsal starvation.
The measured startup gap and the separate failure to learn cobblestone must remain
distinct problems. Better startup allocation alone may be insufficient.

The production training unit remained at MainPID 141387 / NRestarts 2, and its
monitor at MainPID 261 / NRestarts 0. Both were active. The two training restarts
predated this session; claiming zero lifetime restarts would be incorrect.
The study controller had exited. A final Java-process inventory contained only
the original production launcher, original production server and monitor; no
experimental server was left running. Approximately 10675 MiB was available.
No production checkpoint, world or earlier experiment was deleted or overwritten.

## Original prospective protocol, fixed before experimental training

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
