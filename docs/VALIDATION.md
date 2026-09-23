# Validation — current Java runtime (0.6.1)

Bots are zombie-bodied server NPCs, not logged-in Minecraft players. Source
correctness, mechanical reachability, short-run capacity and learned competence
are separate claims. No pretrained mastery or full survival is shipped.

## Current completed acceptance

[Runtime hardening acceptance](verification/20260923-runtime-hardening.md) records
the exact source, measurements, failed daylight test and repair, canonical export,
file-path checks and late-inference isolation.

[CI run 35861174286](https://github.com/lkjsxc/botsclustersmc/actions/runs/35861174286)
passed all seven jobs on source `169d37ba6fb18495f8e1c25bb5da5b80771fd3c3`:

| Environment | Actual result |
| --- | --- |
| Linux x86_64, Java 21 | Clean source build, numerical/mechanical/course/filesystem/concurrency/export checks, real Folia API compilation |
| Windows x86_64, Java 21 | Same source and API checks, including actual symlink tests |
| Folia 1.21.11 build 14, Linux/Java 21 | 1,024-body training, exact weight/Adam resume, canonical export, 18 scripted full-difficulty fixtures, separate 64-body inference |
| Paper 1.21.1 build 133, Linux/Java 21 | Same exported JAR/model, 64-body inference and 18 scripted fixtures |
| Paper 1.21.11 build 132, Linux/Java 21 | Same exported JAR/model, 64-body inference and 18 scripted fixtures |
| Paper 26.2 build 128, Linux/Java 25 | Same exported JAR/model, 64-body inference and 18 scripted fixtures |
| Folia 1.21.11 build 14, Windows/Java 21 | Source launcher, 32-body training/resume/export, 18 fixtures, separate 64-body inference |

Deployment checks include real movement, pause/resume, 24 rapid goal replacements,
continued all-body progress, chunk-lease release, respawn and corrupt-policy
fail-closed without stopping the host server or writing training checkpoints.
Paper download metadata and exact tested builds are retained in CI artifacts.
No API stubs or synthetic-only tests stand in for these live results.

## Measured local capacity, not a hardware guarantee

The same final runtime also passed a fresh 2,048-body Folia training/resume/export
and fixture/deployment sequence on Linux with 4 effective CPUs, a 4 GiB container
and a 3 GiB training heap. During 244.999 seconds it used 1,985,595 new samples
(8,104.5025/s), averaged 1.7325 server-JVM CPU cores and retained all 2,048 ticking
and progressing bodies. Maximum sampled heap was 1,291 MiB, not peak RSS.

An independent plugin-only 2,048-body trial used no training process, averaged
8,192 decisions/s over 90 seconds and 0.5469 CPU cores, with maximum sampled heap
678 MiB. All bodies progressed; retirements and inference failures were zero.
These short flat-world measurements are **not** evidence for thousands of
connected players, complex dense settlements or indefinite performance.

Full numeric results, source binding and counter endpoints are in
[data/runtime-hardening.json](verification/data/runtime-hardening.json). No
individual frozen exam passed in these capacity windows; no learned-skill claim
is inferred from throughput or scripted fixture success.

## Reproduce

Ordinary build/start/export require a Java 21+ JDK and Git, not Rust, Maven,
Gradle, Python or an external inference service. Python is used only by live tests.

```sh
./test.sh
# After personally accepting the Minecraft EULA, in an unused checkout:
EULA=true python3 tests/acceptance.py all --count 1024 --seconds 45 --output acceptance
python3 tests/report.py acceptance
```

The verification record includes the larger 2,048-body commands. Windows uses
`test.cmd` and `build.cmd`; live test orchestration still uses Python. Live CI
runs only after affirmative `workflow_dispatch` EULA consent. Normal main/PR CI
builds and tests source without starting a Minecraft world.

## Scope and historical records

Earlier [Java redesign acceptance](verification/20260923-java-runtime.md) and
[initial compatibility CI](verification/20260923-mainline-acceptance.md) describe 0.6.0, before
the canonical-export and daylight fixes. Rust/Azalea verification records describe
the previous player-client experiment, not the current NPC actuator.

No result certifies every Paper fork/version, macOS/ARM64 execution, arbitrary
other plugins, hot reload, NPC inventory persistence, long-run uptime, vanilla
player mechanics, human likeness, learned completion of all 18 tasks, retention
or generalization. World edits default off in deployment; use copied worlds for
experiments. Skill learning requires separate held-out evaluations.
