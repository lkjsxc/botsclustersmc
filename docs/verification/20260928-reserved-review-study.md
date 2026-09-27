# Reserved review and checkpoint-resume retention — 2026-09-28

## Final disposition: reject this candidate at the original confirmation gate

The previously unread early result was a genuine pass, and the continuation is
now complete. The candidate improved measured retention relative to the unchanged
control, but it did NOT meet the original fresh-seed wooden-pickaxe threshold.
Do not merge or deploy the reservation allocator on this study's evidence, and do
not extend this failed candidate until a favorable seed appears. Retain the source,
all states, failed cases and the two resource-aborted execution records.

The isolated CI continuation `36343677879` completed successfully on controller
source `b16fdc57799889b1a4575ecb6bb97dcfee4f23d0`. Both final models reached the same
originally declared additional-sample target within the 50000-sample overshoot bound.
Control: policy **652272**, **185740543** total samples, **1509904** additional.
Candidate: policy **652957**, **185740052** total samples, **1509413** additional.
The difference is 491 accepted samples, not an exactly equal sample/update budget.
Each model was tested on seed 2026092822 and the SAME model replayed on seed
2026092823. Every task had 32 full-condition cases; no evaluator learned samples.

| Task IDs | Control, seed 2026092822 | Candidate, seed 2026092822 | Control, seed 2026092823 | Candidate, seed 2026092823 |
| --- | ---: | ---: | ---: | ---: |
| 0,1,3,4,5,6,7,8,9 | 32 each | 32 each | 32 each | 32 each |
| 2 aim-hold | 31 | 32 | 27 | 32 |
| 10 craft-workbench | 32 | 32 | 31 | 32 |
| 11 craft-wood-pick | 5 | 24 | 1 | **21** |
| 12 mine-cobblestone | 0 | 0 | 0 | 0 |

The candidate required at least **24/32 on task 11 on EACH final seed**. Its first
final result passed exactly at 24; confirmation scored 21 and failed. Pooling the
two seeds or comparing only against the worse control cannot change that result.
The candidate's tasks 0–10 passed their 30/32 minimum on both seeds. Cobblestone
remained unlearned and was never an acceptance requirement for this retention study.

### Independent completed-data audit

The native `ReplaySource` validator checked all four CI ZIPs, including complete
trial identities, counts, failed cases, model/plugin identities and zero evaluation
training. An independent Java auditor compared transferred full checkpoint bytes,
validated the final complete checkpoints, checked the original sample budget,
confirmed identical-model replay, reconstructed workload from the raw interrupted
and CI status histories, and recomputed the declared gates without trusting a
stored boolean. It reported `audit_passed=true`, `study_gate_passed=false`, with
only `confirmation retention task 11` failing. The CI execution's success means
that the experiment completed, not that its scientific acceptance gate passed.

This final audit covered **1664 CI trials**. The previously revalidated four
initial/early bundles add 832 trials, for **2496 completed trials** in this study.
The original two shared-host resource aborts remain separate non-completed runs;
no missing final evaluation was invented for them.

Candidate measured review fraction over the final-phase interrupted+CI observations
was **20.2301934%**, with **78.9157507%** of accepted samples on task 12. In the CI
segment alone these were **20.2532024%** and **78.8349923%**. Each earlier task had
more than 100 accepted samples in both scopes; workload requirements passed.
These are last observed pre-drain counters, not invented exact post-drain totals.
The result therefore cannot be described as a failure to provide any old-task data,
nor as retained skills obtained by stopping frontier learning. Coverage improved
but the required hard-skill reliability was not retained on the confirmation seed.

Runtime source pins remain baseline `bb0dcfd1` and candidate `8a2cc240`. The CI
runner recorded Temurin 21.0.12.1+1 and 16373452 KiB total RAM. The normal pinned
Folia server remained 1.21.11 build 14. The native audit confirmed inference JAR
bytes identical to the pre-study production build. The extra restores, fresh
worlds, host/JDK changes and unequal optimizer updates limit causal interpretation;
this is not an uninterrupted replication or a multi-training-seed superiority claim.

### Delivered tooling, not the rejected allocator

Retain the opt-in portable `tests/retention_ci.py` controller, its six input-boundary
unit tests and the explicitly gated CI job separately from allocator adoption.
They enabled a completed, checkpoint-only comparison without modifying the shared
workspace's other project or relaxing the memory floor. No world or credentials
were transferred. The input asset remains an unpublished draft; the workflow is
not a deployment mechanism, and rerunning it does not turn a failed gate into a pass.

The full native audit is retained at
`/home/coder/workspace/botsclustersmc-review-study/.build/reserved-review-run/ci-results/native-audit.json`.
That directory also retains both downloaded CI evidence artifacts, exact stopped
checkpoints, raw final/confirmation ZIPs and status histories. The draft input is
`retention-input-20260928`, asset `retention-input.zip`; it must stay unpublished.
The production learner is not restarted or given either experimental model.

## Continuation: the original early gate is now independently verified

During the next operator-requested continuation, the retained early results could
be read. Before any resumed training, the existing native `ReplaySource` validator
rechecked all four initial/early evaluated bundles: 832 complete trials, original
ordered tasks and seed 2026092821, zero evaluation training, matching model bytes,
matching retained summaries, and exact complete early checkpoint copies. The
independent `.build/VerifyReviewResume.java` auditor recomputed the original early
skill and workload gates and confirmed the saved `early-gate.json` pass.

| Task | Unchanged control / 16 | Reservation candidate / 16 |
| --- | ---: | ---: |
| 0–9 | 16 each | 16 each |
| 10 craft-workbench | 2 | 16 |
| 11 craft-wood-pick | 1 | 11 |
| 12 mine-cobblestone | 0 | 0 |

This resolves the earlier *unverified* status; it does not discard or replace an
earlier failed gate. The separate stone-reward candidate in PR 20 remains rejected.
PR 21 was reopened for the original prospective final phase: resume each exact
early state up to 1500000 total additional accepted samples, then test 32 cases
per task on seed 2026092822 and confirm each SAME model on seed 2026092823.
Thresholds, source implementation `8a2cc24`, driver bytes and seeds are unchanged.
No candidate model is installed in production by this continuation.

A separate integration checkout added a shared-frontier concurrency regression:
64 earned-stage-2 actors, eight concurrent workers, an independent status reader,
and 32000 actual simulated lesson lifecycles with normal finishes and cancellations.
The allocator must release every outstanding reservation, count actual ticks once,
and never manufacture frontier progress for a peer. The dedicated suite now has
3115052 checks. Its full source suite at `27281f9af04e1f454c9d81fdd5d57ffa566319fe`
passed with stable input, exit 0, in 31.394224530 seconds. This test-only extension
does not change the pinned implementation used by the real-Minecraft experiment.
Both training and inference JARs byte-match the original candidate build.

The historical closeout below describes the earlier information state, not the
final outcome of the continued study.

## Resource interruption and prospective serial recovery

The simultaneous final-phase execution aborted when measured available memory
fell below the original 1 GiB floor. Its last printed 330-second snapshot had
896791 additional accepted samples in the control and 905909 in the candidate.
The controller requested orderly stops of both experimental learners. A subsequent
process inventory contained only the original production launcher/server and
monitor; production PIDs and restart counts were unchanged. Concurrent work in the
shared workspace was not killed, reconfigured or used as a source of model data.
The original `review-final.log` and `final-status.jsonl` are preserved as an aborted
execution, not relabelled as a successful uninterrupted final phase. No final
skill evaluation was reached by that aborted driver.

Before further training, archive each complete stopped checkpoint under new
`serial-aborted-*` names and validate it with the normal checkpoint exporter.
Continue FROM THOSE exact states, one experimental learner at a time, retaining
all earlier states. Keep source `8a2cc24`, seed 7, 512 actors, 2 GiB heap, two
JVM-visible CPUs and region/inference/learner threads 2/1/1. Do not lower the 1 GiB
memory floor. The existing 2400-second training bound applies to each serial arm.
Keep the ORIGINAL total additional-sample target 1500000 and the 50000 maximum
overshoot; the interrupted samples count toward it, not as a free extra budget.

Use new `serial-control` / `serial-candidate` training labels and
`serial-final-*` / `serial-confirmation-*` evaluated bundle names. Do not overwrite
the aborted log, early results or original checkpoints. The extra restore and
serial execution are a declared operational change, not an identical-scheduling
replication. No final skill score was observed before this recovery decision.

Final tasks, order, 32-case denominators, seeds 2026092822/2026092823, and ALL
original final retention thresholds remain unchanged. Reconstruct final-phase
workload by summing the last recorded actual counters from the interrupted and
serial segments; state the unobserved shutdown-drain limitation. Require the same
10–35% actual review, at least 50% task-12 accepted samples and at least 100 accepted
samples of each earlier task. Also require those workload bounds in the serial
candidate segment itself, so recovery cannot hide a new starvation interval.
Recompute gates independently with the native bundle/trial validator before any
adoption. A further resource interruption is not permission to weaken a limit,
replace a seed or publish the candidate as passed. Production remains unchanged.

## Second resource interruption and isolated CI continuation protocol

The serial control also hit the unchanged 1 GiB memory floor on the shared host.
No final evaluation was reached. Both original logs and all stopped states remain
retained, and neither resource interruption is a failed skill trial or a passed
uninterrupted experiment. Further work will not kill another project or lower the
memory floor. Instead, an explicitly dispatched GitHub-hosted runner per arm will
resume the exact stopped model/Adam/course bytes without a copied world or private
configuration. Source pins, 512 actors, seed 7, heap 2 GiB, threads 2/1/1, active CPUs
2, total additional-sample target 1500000, maximum overshoot 50000, final tasks,
32-case denominators and seeds 2026092822/2026092823 remain unchanged.

The exact transferred states are control policy 651323 / 185295282 samples and
candidate policy 651475 / 185174549 samples. The difference reflects the extra
partial serial control run, not a fresh equal-state restart. The CI budgets end
at the same originally declared total sample target, including EVERY prior accepted
sample. Extra restores, a new disposable world, different runner hardware/JDK and
changed execution order are explicit limitations on causal interpretation.
No final skill score was observed before declaring this CI recovery.

The bounded input ZIP has exactly `manifest.json`, `control.bcmc`, `candidate.bcmc`.
Its SHA-256 is `a9e4dc21e676a3af5a69e1d2ff6ff401814ecf026fd4490eb54fe08c3c447693`.
It contains model/Adam/course bytes and observed prefix workload numbers only:
no executable JAR, world, `.env`, control credentials or other project files.
It is retained as an unpublished draft-release asset, not a normal/latest release.
CI accepts only this pinned archive and source pair and never extracts an archive
path or executes code supplied by it. Native checkpoint validation precedes training.
Every output artifact is an allowlisted evidence directory outside the Academy;
checkpoint evidence contains no world or console credentials. Artifacts have the
repository's normal Actions access and 14-day retention, not an encrypted archive.
The draft input must not be published as a product release.

The optional job is dispatched only with both `eula_consent` and `retention_study`.
It does not run on ordinary pushes or PRs, and it cannot deploy to production.
Its token is supplied only to the fixed draft-asset download step; Minecraft and
the study controller receive no GH_TOKEN/GITHUB_TOKEN. Retain the actual runtime
and JDK identities, complete final checkpoints, raw final/confirmation trial ZIPs,
status history and failures. Stop each learner normally before evaluating.

Final acceptance retains ALL original skill and workload thresholds. Combine the
last observed pre-drain counters from interrupted segments with the CI segment for
final-phase workload, without inventing drain samples. Additionally require the
same workload bounds within the candidate CI segment itself. Audit both complete
final reports and both same-model fresh-seed confirmations independently. A CI
transport/runtime failure is not permission to weaken an experiment gate or claim
learned success. Production is unchanged until all required checks establish a
candidate suitable for a separately controlled rollout.

## Earlier disposition: not accepted; retention outcomes were unverified

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

### Mainline closeout validation

The reports were integrated into main without either experimental implementation.
A fresh `./test.sh` on main `6ffd447f41df913bdb715063ce7027f23bdaea88` completed the
entire source suite and inference-artifact separation checks successfully. The
following, separate `gh run list` in the same shell exceeded the tool call's total
timeout; this was not a failed or timed-out source test. A later independent
GitHub read confirmed that Markdown/docs-only pushes are excluded by the existing
workflow's `paths-ignore`; no new main CI run is claimed for these report commits.
The two candidate PR source/browser CI runs are identified above and in the stone
study, and both PRs are closed unmerged (PR 20 rejected; PR 21 withheld unverified).

A whole-directory diff confirmed unchanged `core`, `plugin`, `training`, `host`,
`tests` and `tools` relative to starting main `bb0dcfd1`. The rebuilt main training
JAR was byte-compared with the original production plugin and matched; the main
inference JAR matched the untouched control build. Production service PIDs and
restart counts remained unchanged. The final follow-up to this tested main only
adds this closeout record, not code, tests or model data.

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
