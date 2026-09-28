# 2026-09-28 — Latest development runtime activated, not just synchronized

## Operator mandate and activated source

The operator explicitly requires the running development server to stay current
with accepted development, even if all previous learning data is lost. Commit
`61bc6c03272b7c016d8cabdc98ca5246b2ae3a46` records this completion rule in
`AGENTS.md` and the Japanese service guide. Preservation of existing learning is
not a reason to leave the old JVM running or to ask for repeat authorization.
This is not an unattended Git polling/deployment service.

Workspace: `lkjsxc/tomato-ocelot-73`.
Checkout: `/home/coder/workspace/botsclustersmc`.
Activated source: `61bc6c03272b7c016d8cabdc98ca5246b2ae3a46`.
Source tree: `cee6c7525a5204ef8201c92d6c0f0ae1194ba990`.
Runtime and tests are identical to main `dfcb5e1`, including the PR #28
implementation `85be01d`; the mandate commit changes documentation only.

The full `./test.sh` suite ran on this checkout with a 768-MiB Java heap cap and
two effective processors and exited zero. Its final output records all tests
completed and inference artifact separation. The rebuilt training JAR matched
the exact PR #28 JAR previously tested on Folia and Paper. After startup, the
installed `academy/server/plugins/training.jar` matched that rebuilt artifact.

The previous installed JAR was 208,401 bytes; the new one is 212,836 bytes.
This was an actual artifact replacement and process restart, not only a Git
update. Artifact identities are in the accompanying data receipt.

## Controlled stop and the failed first start

The evaluation timer and service were stopped before building. The old training
supervisor PID 272 and Minecraft PID 927 stopped normally at 20:56:08 JST. The
last canonical checkpoint contained 171,818 policy updates, 46,072,578 accepted
samples and Adam step 171,818. Untrained actor buffers are not part of that exact
saved optimizer/model; the shutdown log reported 7,530 buffered-untrained samples.

At 20:56:09 JST the first new supervisor built successfully, then failed with
`ERROR: Address already in use` before installing the new plugin or starting
Minecraft. Its existing systemd restart policy retried once at 20:56:40 JST.
The retry started Minecraft and remapped the updated plugin. No port setting was
changed and no unrelated process was killed. The existing configuration and the
old startup logs already used port 25566, not 25565.

The initial activation verifier rejected the attempt because the restart count
was one rather than zero. That failed receipt remains intact. A separate verifier
checked the successful retry without another restart and retained the initial
failure in its result. The bind failure's exact cause was not narrowed to a
specific competing process or socket state; its disappearance is not a code fix.

## Verified current service and learning

The active supervisor is PID 88139; its Minecraft child is PID 88443. The monitor
was also restarted, from PID 268 to PID 84532, and returns fresh status over its
existing loopback-only port 8765. Minecraft listens on the unchanged port 25566.
The supervisor's invocation identity, PID and restart counter remained unchanged
through the separate 35-second post-retry observation. The counter remains one;
it is not reported as zero.

| Post-retry observation | First | Last |
| --- | ---: | ---: |
| Time, JST | 20:58:08 | 20:58:43 |
| Active / progressed actors | 512 / 512 | 512 / 512 |
| Pending actors | 0 | 0 |
| Minimum decisions in latest status interval | 20 | 20 |
| Accepted learning samples | 46,220,928 | 46,294,496 |
| Policy updates | 172,321 | 172,548 |
| Decision transitions this process | 157,782 | 229,462 |
| Inference failures / rejections / retired actors | 0 / 0 / 0 | 0 / 0 / 0 |

Accepted samples increased by 73,568 and policy updates by 227 across those 35
seconds. The latest status and dashboard both reported `running`. The exact
restore log matches the stopped checkpoint's update, sample and optimizer-step
numbers. `startup_restored_checkpoint=true`; no learning reset was needed because
this update retained the checkpoint format. The permission to reset is not an
instruction to discard compatible state unnecessarily.

The existing periodic evaluation timer was restored after successful verification.
No unrelated project or world was deleted or reconfigured. Network exposure,
learner settings, rewards, curriculum, model schema and runtime source were not
changed in this activation session.

## Evidence and limits

The compact receipt is `data/20260928-latest-development-activation.json`.
The checkout retains `.build/latest-activation-source-20260928.log` and its zero
exit receipt, the rejected `.build/latest-activation-20260928.json`, the full
successful `.build/latest-activation-recovery-20260928.json`, and the corresponding
driver/service logs. The one-shot activation and recovery verifier scripts are
retained there too; they are not runtime dependencies or autonomous updaters.

This establishes latest-runtime activation and continued learning, not skill
retention, improved mining, or learned cooperation. No frozen-policy retention
experiment was run for this operation. The final evidence commit changes only
this record, its data and a service-guide link, so it does not require yet another
runtime restart.
