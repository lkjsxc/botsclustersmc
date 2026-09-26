# Shared-workspace training restart — 2026-09-26

## Scope and source

The operator deleted the previous development environment and requested fresh
learning from current main in the shared Coder workspace `tomato-ocelot-73`.
The independently read GitHub main and fresh clone were
`ea45a4c631bffaa5b8d91ddab3284d8fa4458146` (tree
`9d9bdc7515967552f8b9a72d8d295bea41719ace`). No old weights or certificates were
restored. This record and the optional systemd examples do not change core,
inference, optimizer, rewards, curriculum, schemas or the server pin.

Runtime: Ubuntu 24.04.4 LTS, 12 available workspace CPUs, 8 GiB memory, 4 GiB swap;
OpenJDK 21.0.12.1+1-1~24.04.4; systemd 255.4-1ubuntu8.17. Other development work
shares this workspace. The learner uses the pinned Folia 1.21.11 build 14 and
plugin 0.7.3, not a newly selected upstream server version.

## Installed operation

Checkout: `/home/coder/workspace/botsclustersmc`; fresh data: `academy/`.
Configuration: 512 NPCs, heap maximum 4 GiB, region/inference/learner workers
4/1/2, seed 7, port 25565, wildcard game bind, online authentication enabled.
EULA consent was enabled in the local, ignored `.env`. The global sample config
still requires consent and is not silently changed. No public port-forward or
firewall rule was added. These are server-side Villager NPCs, not logged-in players.

`botsclustersmc-training.service` and `botsclustersmc-monitor.service` were
installed as root-owned system units, enabled, and run as unprivileged `coder`.
The committed examples were compared byte-for-byte with the installed units.
Training uses a 5.5 GiB service-group ceiling, a 6-core-equivalent CPU ceiling,
low scheduling priority and bounded restart attempts. Java sees 8 processors.
The read-only monitor has a separate 512 MiB ceiling and listens on loopback
port 8765. The monitor example explicitly names `academy`; a custom ACADEMY
requires updating its metrics path, not only `.env`.

The first service start was 2026-09-26 14:30:21 UTC (23:30:21 JST).
All 512 bodies were active in eight 64-actor islands. Early observations showed
roughly 2,080–2,629 accepted samples/s and all-actor decision coverage, with zero
inference failures/rejections, learner rejections and burning bodies.
Throughput is not constant: at 14:40:20 UTC, 427 actors were in non-training exams
and accepted throughput was 332/s while decisions remained approximately 1,996/s.
Do not interpret exams as a stalled learner or inflate this into a scaling study.

The training service initially used about 1.5–1.9 GB of accounted memory.
Swap was observed, including approximately 342 MB for the training group before
the deliberate restart. This is not a zero-swap or long-duration capacity claim.
No OOM or automatic service restart was observed during these checks.

## Completed validation

A fresh `./test.sh` completed with exit 0, including real API compilation,
numerical/mechanics/curriculum checks, asynchronous learning, source-host export
and evaluation integrity, and inference-JAR separation. Its local log is
`/tmp/botsclustersmc-source-test-20260926.log`. Synthetic learning and scripted
fixture compilation are not learned Minecraft competence.

Local HTTP checks returned 200 for the dashboard and status API; the monitor
returned 404 for `/training.bcmc`. A Coder port-forward URL was obtained.
Authenticated access from the operator's external browser and human game login
were not tested. Control credentials and runtime state were not committed.

At 14:36:30 UTC the training service stopped successfully: Folia completed its
world/chunk/player saves and systemd reported `Result=success`, expected Java
exit status 143. The canonical checkpoint contained policy update **2344** and
**708,108** trained samples. `./export.sh` completed, and
`CheckpointTool verify-export` confirmed exact policy-byte equality with the
canonical training checkpoint. This export is not the earlier evaluated model.

The stopped, complete Academy was retained at
`/home/coder/.local/share/botsclustersmc/pre-restart-academy.tar.gz`
(182,384,252 bytes). This is one private local backup, not off-host redundancy or
an automatic backup schedule.

The service was restarted. At 14:40:20 UTC status explicitly reported
`startup_restored_checkpoint=true`, all 512 startup actors observed, policy 3685,
and 1,023,830 trained samples. At 14:43:25 UTC it reported policy 4884,
1,306,019 samples, all 512 actors active/ticking/progressing, and all 512 courses
at task 1. Inference failure/rejection, learner rejection and burning counts
were zero. This demonstrates saved-state resume and further real learning
updates; historical course promotion does not certify the current shared policy.

## Frozen-policy results, including failures

Both one-shot tests used tasks 0, 1, 2 in that order, 32 cases/task, a separate
loopback-only real Folia server, a 1 GiB evaluation heap and no learning.
The complete summaries report `new_training_samples=0`.

| Snapshot | Policy updates | Trained samples | Case seed | forward-stop | turn-stop | aim-hold |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Initial | 936 | 290,482 | 2026092601 | 30/32 | 0/32 | 0/32 |
| After resume | 5287 | 1,427,356 | 2026092602 | 32/32 | 0/32 | 0/32 |

Initial evaluation completed at 14:34:08 UTC; after-resume evaluation completed
at 14:45:32 UTC. The latter command connection closed while the server continued
its test. It was not relaunched: the completed report, distinct exported ZIP
and absence of remaining evaluation-server processes were checked afterward.

The policies and case seeds differ. These two snapshots are not a controlled
algorithm comparison, same-model seed replication, or statistical proof of
improvement. Task 1 remained unproven by independent evaluation despite the
live course reaching it. Task 2 had not been reached. Tasks 3–17, open-world
survival, cooperation and future policies are not certified.

Both exact tested models and full outcomes remain in untracked local artifacts:
`dist/evaluated-initial.zip` (366,104 bytes) and
`dist/evaluated-after-resume.zip` (366,288 bytes).
The monitor shows the latest completed report; the earlier ZIP preserves the
initial failures. No selected-model promotion or automatic deployment occurred.

Exact policy identities:

- Initial: `353ec2971cc233188f4f0afaa355d5ac60742541ad4f3d97cf3138c7f8a188b9`.
- After resume: `3e449583ff30c96a45a8323a370a8aacf6c1a355a03016eb8c1006d73c3e1eb0`.

Both reports record training JAR
`3ad4de980018e5f98201eafa83c5cd4660ab9294723427bd4501c16b246697f2`
and inference JAR
`1804e928f3e75ba870504379181171f18d1cea9aedb130ebb31da62900857c42`.
Evaluator JAR identities differ; consult the complete retained reports rather
than asserting byte-identical evaluator artifacts.

## Limits and excluded work

An optional change to route the monitor through Host.java was blocked before
installation by the tool safety check. The uninstalled drafts were reverted;
the already working direct Monitor.java service was kept. No alternate route
was used to apply the blocked change.

The normal service stop/save/restart was exercised, but container-wide reboot,
Coder root-filesystem recreation, workspace deletion, forced OOM/crash recovery,
long-duration saturation, Windows live operation and a new inference-server
deployment were not tested here. Enabling a system unit is not proof of recovery
after deleting its OS or persistent home. The initial source suite predates only
the added service examples and documentation; no application source was edited.

There is no autonomous source updater, repeating evaluator or periodic backup
installed by these examples. The learner and read-only monitor remain running.
The next research decision should use retained fixed-policy task coverage and
fresh-seed tests, not update count or historical certificates alone.
