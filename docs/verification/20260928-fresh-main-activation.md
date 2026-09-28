# Fresh main activation — 2026-09-28

## Operator request and deployed source

The operator explicitly requested applying the merged changes to the learning
server and permitted discarding all existing learned progress. This record
supersedes the earlier statement that PR #24 was source-only and not yet active.

Deployed product source: `68710af3da8cda7df6e29ef2476543f42067cc90`.
The later commit containing this record changes documentation only.

Workspace: `lkjsxc/tomato-ocelot-73`.
Checkout: `/home/coder/workspace/botsclustersmc`.
Active Academy: `academy`.
Training service: `botsclustersmc-training.service`.

The new `academy/server/plugins/training.jar` was byte-compared with the
fresh main build and the previously tested shared-inventory build; both match.
It differs from the old installed runtime. Installed runtime identity:
`78745059d95dd0697c20f883d4b80c3e42bba0d7d2dc4e0b813c3881d978c227`.

This activates the shared-resource fixes, not the rejected conditional-menu,
stone-reward or expert-policy candidates. No new learning algorithm, reward,
curriculum, model schema or completion-rule change was introduced in this rollout.

## Reset boundary

The evaluation timer, evaluator and monitor were stopped before the training
service. The training supervisor requested normal Minecraft shutdown; the journal
records saved worlds and successful service deactivation at 13:42:31 JST.
The old training process and port listeners were gone before the Academy moved.

The entire old Academy was moved, without merging any of its contents into the
new one, to `.build/academy-retired-20260928T044231Z`. This preserves prior
experiment evidence and worlds; the bytes were **not erased**. Neither current
configuration nor the services refer to this retired directory.

A fresh Academy was created at the usual `academy` path. It started with a newly
initialized random policy, fresh Adam state, new world, zero learned samples,
zero updates and no inherited curriculum certificates. Previous metric history
and evaluation results were also left in the retired Academy.

Before stopping, a snapshot recorded policy 927013 / 267618662 trained samples.
These counters were **not carried forward**. The user-authorized restart resets
learned progress; it is not an exact-state continuation or a retention result.

## Startup and operation

The unchanged deployment configuration remains:
512 actors, seed 7, 4-GiB heap, region/inference/learner threads 4/1/2, eight
JVM-visible processors, port 25565, bind 0.0.0.0, online authentication enabled.
The monitor remains loopback-only at 127.0.0.1:8765.

The training service started at **13:43:40 JST** with supervisor PID 518034 and
Minecraft PID 518105. At **13:43:56.187 JST**, the initial snapshot recorded
zero updates and samples, `startup_restored_checkpoint=false`, all course stages
at zero and all historical certificates zero; 448 actors had spawned so far.

At **13:44:36.186 JST**, all 512 actors were active, ticking and progressing.
All 512 first-issued lessons were task 0; the snapshot still explicitly reported
no restored checkpoint. The policy had advanced to 235 updates / 78238 samples.

At **13:47:01.187 JST**, a fresh HTTP monitor snapshot recorded:

| Metric | Value |
| --- | ---: |
| Active / ticking / progressing actors | 512 / 512 / 512 |
| Policy updates | 1209 |
| Accepted trained samples | 373110 |
| Sample rate for that status interval | 1956.20 samples/s |
| Current course task | 0 |
| Inference failures / rejections | 0 / 0 |
| Learner rejected / stale samples | 0 / 0 |
| Burning / retired actors | 0 / 0 |

The new canonical checkpoint was saved again at 13:46:50 JST, and the independent
evaluation below successfully read the new complete checkpoint. The training and
monitor services were running without automatic restarts since this activation.
The initial monitor read returned no evaluation result, rather than displaying
the old policy's results.

The training service, monitor and evaluation timer are enabled. The existing
timer is restored with its original `OnUnitInactiveSec=30min` cadence; the
separate evaluation service is inactive after successful completion, which is
normal for this oneshot service. This is an observed state, not an uptime promise.

## Verification

A fresh complete main `./test.sh` finished successfully before activation,
including the new shared-resource regression, existing learning/math checks,
real-API compilation, checkpoint/export checks and inference-artifact separation.
The tests used a bounded 768-MiB JVM heap and two visible processors. No source
changes were needed.

The existing evaluator was also explicitly run once against the **new** Academy.
Its complete 32-case task-0 report used frozen policy **528**, trained samples
**168558**, seed **2947735218767677922**, on Folia 1.21.11 build 14.
It recorded **16/32 forward-stop successes** and **zero new evaluation training
samples**. Its runtime identity matches the installed new JAR.
The service completed with exit status 0 and `Result=success`.

This early fixed-policy result verifies the actual evaluation path and measures
that particular early model. It does not certify a later changing policy,
demonstrate retained old skills, resolve stone mining, or establish cooperation
or open-world survival.

## Retained evidence

Under the main checkout's `.build/`:

- `20260928-main-reset-before-status.txt`
- `20260928-main-reset-source-tests.log`
- `20260928-main-reset-first-status.txt`
- `20260928-main-reset-active-status.json`
- `20260928-main-reset-empty-evaluation.json`
- `20260928-main-reset-verified-status.json`
- `20260928-main-reset-evaluation.json`

The new Academy retains its own canonical checkpoint, runtime log, fresh metric
history and full evaluation evidence. Prior failed experiments and other
projects in the shared workspace were not deleted or restarted.
