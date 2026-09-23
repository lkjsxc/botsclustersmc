# Authorized Linux device acceptance — 2026-09-23

The operator supplied a Linux device for botsclustersmc verification and delivery.
A separate checkout of `work/runtime-separation-20260923` was built from source.
The initial pure Java and actual Folia API checks passed on OpenJDK 21.0.12;
`java`, `javac`, Git and Python 3.12.3 were available. Python is required only by
the developer acceptance harness, not normal build/start/export operation.

The complete real-server acceptance ran against
`5be4aa4eaa8ec7fcf32429a19d12b9371d2952a5`. Its runtime, launcher and tests are
identical to full CI source `4cfc06d9c43c2e94204dc948e3e4dd54d2232002`;
only the temporary engineering workflow was removed. A subsequent explicit
`git diff --exit-code` across core, plugin, training, host, tests, entry scripts
and `.env.example` confirmed no program changes in the documentation-only
integration commits.

## Isolation and test command

The device exposed 16 effective CPUs and a 12 GiB memory limit. The existing
operator server remained running on port 25565 throughout acceptance. The test
used a newly created, separate Academy, loopback-only test ports, a 3 GiB heap,
two region threads, one inference worker and one learner worker. It neither
reused nor modified the old operator world or learning state.

```sh
EULA=true python3 tests/acceptance.py all --count 1024 --seconds 45 \
  --output /absolute/path/to/new-disposable-verification-directory
```

The explicit EULA consent was already recorded in the operator's existing
Minecraft installation. The test's raw logs, worlds and models remain local;
operator configuration and private files are not published here.

## Completed results

```text
PASS real training + exact-state resume + export: 1024 actual NPCs; final phase trained/s=4245.47
PASS 18 full-difficulty scripted real-world fixtures; zero learner/inference samples
PASS plugin + policy only; 64 actual NPCs, pause/resume, ticket release, respawn, corrupt-model fail-closed without server shutdown
PASS acceptance mode=all
```

The final exported test policy contained 1,927 updates and 343,194 actually
trained samples. This is a short-trained test model, not an all-skills artifact.

| Measured interval | Fresh | Resume |
| --- | ---: | ---: |
| Elapsed seconds | 45.000 | 15.000 |
| Newly trained samples | 183,378 | 63,682 |
| Trained samples per second | 4,075.07 | 4,245.47 |
| Mean process CPU-core equivalents | 0.5169 | 0.5633 |
| Maximum sampled heap occupancy, MiB | 853 | 644 |
| Minimum ticking agents | 1,024 | 1,024 |
| Minimum progressing agents | 1,024 | 1,024 |
| Retired agents / inference failures | 0 / 0 | 0 / 0 |
| Stale / rejected learning samples | 0 / 0 | 0 / 0 |
| Highest course stage / passed exams | 0 / 0 | 0 / 0 |

CPU numbers are JVM process-time measurements, not a single-thread screenshot;
heap occupancy is not peak RSS. The old server and other device services were
also active, so this is not an isolated hardware benchmark. Sample throughput
and every-agent progress were measured; high CPU utilization was not fabricated
by adding work unrelated to learning. The trial did not establish learned
completion of even the first frozen course exam, all 18 skills, generalization,
long-run uptime or a dense permanent settlement.

The production cutover is a separate operational action after mainline
integration. These acceptance results do not by themselves claim that a
production server was started or that its learning state is this test state.
