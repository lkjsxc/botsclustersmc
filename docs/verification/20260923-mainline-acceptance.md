# Mainline acceptance — 2026-09-23

## Accepted source and independent CI

GitHub Actions run [35847346541](https://github.com/lkjsxc/botsclustersmc/actions/runs/35847346541)
completed all seven jobs successfully against runtime source
`4cfc06d9c43c2e94204dc948e3e4dd54d2232002`. Results were read back from the
completed jobs and the downloaded test artifacts, not inferred from workflow
configuration. The later commit `5be4aa4eaa8ec7fcf32429a19d12b9371d2952a5`
only removes a temporary engineering snapshot workflow. It does not change the
runtime, launcher, tests, model schema, or server pins.

| Actual environment | Completed evidence |
| --- | --- |
| Ubuntu 24.04, JDK 21 | Fresh source build against actual server libraries, pure Java tests, inference/training artifact separation |
| Windows runner, JDK 21 | Same source/build/numerical checks using the Windows entrypoint |
| Folia 1.21.11 build 14, Linux, Java 21 | 1,024 actual NPC learners; fresh learning, exact optimizer/model resume, export, 18 full-difficulty scripted fixtures, separate 64-NPC inference deployment |
| Paper 1.21.1 build 133, Linux, Java 21 | Same inference JAR and exported policy; 64 actual NPCs, lifecycle/invalid-model checks, all 18 scripted fixtures |
| Paper 1.21.11 build 132, Linux, Java 21 | Same inference JAR and exported policy; 64 actual NPCs, lifecycle/invalid-model checks, all 18 scripted fixtures |
| Paper 26.2 build 128, Linux, Java 25 | Same inference JAR and exported policy; 64 actual NPCs, lifecycle/invalid-model checks, all 18 scripted fixtures |
| Folia 1.21.11 build 14, Windows, Java 21 | Source launch, 32-NPC learning, exact resume, export, 18 fixtures and separate inference deployment |

The Paper build IDs are from each artifact's `paper-cache/download.json`; server
versions were not substituted. Deployment tests use only the production inference
JAR and the policy, without training.jar, an external actor process or training
state. They exercise real action progress, pause/resume, removing all agents and
releasing their chunk leases, respawn, and corrupt-model rejection. An invalid
model disables the plugin but leaves its host server running and does not replace
the bad data with a random policy.

## Paper integration repair

The preceding run [35845419220](https://github.com/lkjsxc/botsclustersmc/actions/runs/35845419220)
failed on Paper 1.21.11 and 26.2 with `chunk lease needs a loaded owned chunk`.
It is retained as failed evidence; it is not relabeled as a pass.

The old handoff always deferred async chunk completion to a later region callback.
A temporary loading ticket could expire before that callback acquired the plugin's
lease. `LoadedChunks` now uses completion immediately if already on the owner;
otherwise it schedules the owned handoff, checks residency and ownership again,
and performs bounded asynchronous retries if the chunk has unloaded. It does not
synchronously load a chunk, wait on a tick thread or silently omit a failed NPC.
Spawn, training arena preparation and the separate diagnostic fixtures use this
same handoff. The named actual Paper versions now pass the previously failing
path and complete the rest of their tests.

The public API contracts consulted were [Paper/Folia scheduling](https://docs.papermc.io/paper/dev/folia-support/)
and [World plugin chunk tickets](https://jd.papermc.io/paper/1.21.10/org/bukkit/World.html).
API documentation explains the implementation; actual server tests establish the
named compatibility results. No Bukkit mocks were used as compatibility evidence.

## CI throughput, not a universal performance claim

The Linux Folia CI job used four effective CPUs, a 3 GiB heap, two region threads,
one inference worker and one learner worker. Its `capacity.json` records:

| Interval | Fresh | Resume |
| --- | ---: | ---: |
| Measured elapsed seconds | 44.999 | 14.985 |
| Actual newly trained samples | 185,366 | 63,144 |
| Trained samples per second | 4,119.34 | 4,213.81 |
| Mean JVM CPU-core equivalents | 0.9665 | 1.0484 |
| Maximum sampled heap occupancy, MiB | 761 | 621 |
| Minimum ticking and progressing agents | 1,024 | 1,024 |
| Retired agents / inference failures | 0 / 0 | 0 / 0 |
| Rejected / stale learning samples | 0 / 0 | 0 / 0 |

These are short early-stage measurements. Sampled heap is not peak RSS. CPU-core
equivalents measure process CPU time, not a single thread's percentage. There is
no intentional CPU burn. The previous [2,048-NPC local measurement](20260923-java-runtime.md)
is a separate source-bound experiment, not a replacement for this final CI run.
Neither measurement establishes faster sample-efficient learning than the old
network-player implementation.

## Limits that remain

This is a breaking Java NPC experiment, not a complete vanilla-player simulation.
Bodies use Zombies; pockets and bodies are ephemeral. Crafting and world actions
are a disclosed bounded mechanic set. All gameplay choices come from the neural
policy in production, but the scripted fixture passes establish reachability,
not learned mastery. The short-trained exported test model is not certified to
complete all 18 tasks, generalize, survive autonomously or live cooperatively.

Only the named versions and operating-system combinations above are accepted.
There is no claim for every intermediate release, arbitrary Paper forks, macOS,
ARM64, rendered client performance, WAN accessibility, dense multi-thousand-NPC
settlements or indefinite uptime. No old operator world, configuration or model
is migrated or deleted by this delivery.
