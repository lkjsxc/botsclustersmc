# Shared-workspace continuity and independent evaluation — 2026-09-27

## Source, scope and existing training

At 2026-09-26 20:15 UTC (2026-09-27 05:15 JST), both an independent GitHub
branch read and `git fetch` identified main as
`b77697613723542874923f2bd87987ba2b0b7327`. The checkout was clean at
`/home/coder/workspace/botsclustersmc` in `lkjsxc/tomato-ocelot-73`.

Training and its private monitor were already running. This session did not
create a second learner, reset the existing Academy, restore an old-environment
model, or stop the live training service. The fresh origin of this Academy is
recorded in [the initial deployment record](20260926-shared-workspace.md).
The observed automatic restart was at 18:25:19 UTC, following workspace build 6.
Startup restored model/Adam update 92,902 and 22,688,221 accepted samples.
This is observed continuity, not a controlled test of deleting/recreating the
container or its persistent home.

The environment now reports 12 CPUs, 12 GiB memory and 4 GiB swap. Training remains
512 server-side Villager NPCs in eight 64-actor islands, not logged-in players.
Settings remain heap 4 GiB; region/inference/learner workers 4/1/2; seed 7;
game port 25565; online authentication enabled. The locally accepted EULA and
ignored `.env` were not changed. The server remains the repository-pinned Folia
1.21.11 build 14 with plugin 0.7.3 and OpenJDK 21.0.12.1.

This change adds optional evaluation service/timer examples, Linux CI validation
of the units, and operator documentation. No Java application code, observations,
actions, model shape, reward, optimizer, curriculum or checkpoint schema changes.
Repeated builds produced a training JAR byte-identical to the installed live JAR.

## Measured live progress

All times below are UTC on 2026-09-26; add nine hours for the JST date above.

| Snapshot time | Policy updates | Accepted trained samples | Active / progressing NPCs |
| --- | ---: | ---: | ---: |
| 20:16:01.968 | 137,422 | 35,195,694 | 512 / 512 |
| 20:17:16.968 | 137,930 | 35,348,671 | 512 / 512 |
| 20:25:01.968 | 141,087 | 36,294,115 | 512 / 512 |
| 20:28:31.968 | 142,518 | 36,720,144 | 512 / 512 |

The first interval added 152,977 accepted samples in 75 seconds, approximately
2,040 samples/s. This is a short observed interval, not a general scaling result.
All courses were at task 10, `craft-workbench`. An early operator update wrongly
called this the wooden-pickaxe stage; the actual label and correction are explicit
here. Course history is not proof that the current shared policy retains a skill.

Observed inference failures/rejections, learner rejections/stale samples and
burning-body counts were zero. Service `NRestarts=0` refers to the current boot,
not all historical operation. No warning-level training journal entries were
returned for the observed boot. Checkpoint modification times continued advancing.
The training supervisor remained PID 266 and its server PID 1285 throughout the
recorded checks.

## Same-policy fixed-condition tests

Both tests covered tasks 0 through 10 in that order, 32 cases per task, in a
separate loopback-only real Minecraft server with a 1 GiB heap and no learning.
The first command used the canonical checkpoint and retained an evaluated ZIP.
The second used `--from` that ZIP, not the subsequently changing live checkpoint.

Both tested policy update **137,462**, trained samples **35,207,560**, with policy
identity `aee1595723898e22b5964be22519f2a93843efbacd0c4d06a0b8b1704e8377dc`.
Both completed all 352 cases, reported `new_training_samples=0`, and retained
failed trials. The second report records `same_inference_build_as_source=true`.
The evaluator artifact identities differ because the case configuration differs;
this is a same-policy/current-inference-build second-seed test, not an assertion
that the configured evaluator JAR bytes are identical.

| Task | Label | Seed 2026092701 | Seed 2026092702 |
| ---: | --- | ---: | ---: |
| 0 | forward-stop | 32/32 | 32/32 |
| 1 | turn-stop | 32/32 | 31/32 |
| 2 | aim-hold | 31/32 | 31/32 |
| 3 | navigate-stop | 31/32 | 32/32 |
| 4 | step-over | 31/32 | 29/32 |
| 5 | break-log | 32/32 | 32/32 |
| 6 | collect-log | 32/32 | 32/32 |
| 7 | place-block | 30/32 | 31/32 |
| 8 | craft-planks | 32/32 | 32/32 |
| 9 | craft-sticks | 31/32 | 32/32 |
| 10 | craft-workbench | 0/32 | 0/32 |

These are full-condition Academy task results, not open-world generalization,
vanilla-player equivalence, cooperation, all-stage mastery or a guarantee for
later updated policies. Workbench crafting remains unlearned by these tests.
No success thresholds were weakened and no course certificates were fabricated.

Retained private, ignored files relative to the checkout:

- `dist/shared-evaluated-20260927-seed01.zip` — 386,946 bytes.
- `dist/shared-evaluated-20260927-seed02.zip` — 387,327 bytes.

These ZIPs retain the exact tested policy, matching inference artifact and full
trial evidence. They do not contain the optimizer, world or a training backup.
Earlier failed initial/resume evaluations remain in their original ZIPs and in
the initial deployment record; the newer measurements do not erase those failures.
No evaluated model was automatically selected, installed into training or deployed.

## Operational changes and validation

A fresh foreground `./test.sh` completed with exit 0 after adding the units.
The full source log is `.cache/verification-20260927-tests-final.log`.
This includes real API compilation, pure numerical/mechanics/curriculum tests,
asynchronous learning, persistence/export/evaluation integrity and inference-JAR
separation. Synthetic checks are not learned Minecraft skill evidence.

Implementation commit `9fbcd11d237bbbdbb66e3f361700977e40c17fd9` was pushed normally
as `lkjsxc` and independently re-read from GitHub. CI run **36269632710**, attempt 1,
completed with **success** at 20:30:01 UTC. Linux and Windows source checks and the
browser observatory regression passed; the new Linux systemd-unit validation step
also passed. Dispatch-only live/cross-version jobs were not run by this push.
This record's completion update changes documentation only.

`sudo systemd-analyze verify host/systemd/*.service host/systemd/*.timer` and
`git diff --check` passed. The installed root-owned evaluation service and timer
were compared byte-for-byte with the source files. Evaluation runs as `coder`.
Its effective CPU ceiling is two core equivalents, memory ceiling 2.5 GiB,
swap ceiling 256 MiB, Nice 15 and start timeout 15 minutes.
`ProtectSystem=strict`, `ProtectHome=read-only` and the four explicit writable
runtime directories were read back. The container's pre-existing global LXC
drop-in overrides `NoNewPrivileges` to `no`; this session did not change that
platform policy and does not claim the directive in the example is effective
against such overrides.

The timer was enabled at 20:25:56 UTC. Because boot plus five minutes was already
in the past, it immediately started the first automatic evaluation, with
`ActiveState=activating` as expected for an unfinished one-shot service.
The first automatic run completed at **20:28:47 UTC**, with `ExecMainStatus=0`,
`Result=success`, `MainPID=0` and `ActiveState=inactive`. The timer remained active
and scheduled the next activation for **20:58:47 UTC** (05:58:47 JST).
There was no remaining evaluator JVM. The training supervisor and server retained
the same PIDs and continued learning during the complete evaluation.

This automatic run tested newer policy **141,339**, samples **36,368,806**, seed
**6695400432298917380**, all 352 trials and `new_training_samples=0`. In task order
0–10, successes out of 32 were **32, 32, 31, 32, 32, 32, 32, 31, 32, 32, 0**.
Its policy identity is
`aba4570e484c23c92e47f2bcfa9d8a7343aec308b5106e51e8274424beab64cf`.
This is an operational lifecycle check on a different snapshot, not another
same-model seed replication or proof of algorithm improvement. The bounded latest
report is retained; unlike the two manual runs, no exact-model export was requested.

The evaluation group's measured memory peak was 1,659,547,648 bytes and swap peak
was 268,435,456 bytes (256 MiB). Other development was active in the shared
workspace. This is not zero-swap operation or a guarantee against future pressure.
The completed service released its processes rather than keeping this allocation
resident for the next 30 minutes.

The timer schedules the next run approximately 30 minutes after the service
becomes inactive; it does not keep a Minecraft evaluation process alive while
waiting, change the learner or fetch source updates. Each run measures all
reached tasks from a new canonical snapshot and fresh case seed. The existing
Academy evaluation lock also prevents overlapping manual evaluations. The
timer's results replace the bounded latest report, not an unlimited history.
The one-shot API status still contains the evaluator's default `interval_seconds`
value of 600 with `watch=false`; the actual 30-minute schedule belongs to systemd
and was checked with `systemctl list-timers`, not inferred from that API field.

Local HTTP checks returned 200 for `/`, `/api/status` and `/api/evaluation`;
`/training.bcmc`, `/control.properties` and `/api/console` returned 404.
The monitor remains on 127.0.0.1:8765. No public forwarding, firewall or game
authentication settings changed. External browser access and a human Minecraft
login were not tested.

The workspace's installed Coder CLI documents this client-side forwarding form:

```sh
coder port-forward tomato-ocelot-73 --tcp 8765:8765 --tcp 25565:25565
```

Use an authenticated local Coder client. The forwarded dashboard is at
`http://127.0.0.1:8765/`, and the forwarded game endpoint is `127.0.0.1:25565`.
This command documentation is not evidence of a successful external client login.

## Limits and next decision

The measured bottleneck is workbench crafting, not lack of a running learner.
Keep the measurement baseline and use separate owned experiments before changing
learning semantics. Improvements must retain tasks 0–9 in complete frozen
reports as well as improve task 10; the two-seed results here are the comparison
baseline, not evidence for a proposed redesign.

No learner reset, source auto-updater, periodic training backup, forced-OOM test,
container-deletion recovery test, Windows live test or new production inference
deployment was performed. Existing learner and monitor services remain enabled.

A later additional grouped shell read of checkout differences, live counters and
observer-command documentation was blocked by the tool safety check before
execution. That read was not retried or routed through another tool. The completed
lifecycle and last live measurements above were obtained before that blocked
additional check; no new gameplay or service action depended on it.
