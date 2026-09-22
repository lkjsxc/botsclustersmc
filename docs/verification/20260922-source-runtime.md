# Source-first runtime verification — 2026-09-22

## Delivered scope

The runnable source application was published to `main` as
`10692dde0fae063e72ac5436db2f752f3f3745a6` (0.4.0). It contains the real Rust
actors/CPU PPO learner, Java Folia bridge and foreground Academy launcher.
Subsequent evidence/workflow commits do not change that native source.
`experimental/rl-next` is separate and is not wired into this runtime.

## Source builds and network bootstrap

Both the local build and the independent GitHub runner compiled the actual
application from source with Rust nightly-2026-02-04 and Azalea revision
`f8ddefa70cc53e6385785fb56e7a688a389cf0ab`. No old native executable was used as
a substitute for compilation. Both builds recorded native source receipt
`9ee8ceae0deff98ae7ed9b9ad94cb4cfa8e7bd58fb99eb5eef44a2ffd6f158b4`.
This is a local integrity identifier, not a publisher signature or a promise
that binaries from different absolute build paths will be byte-identical.

The local sandbox could not resolve dependency hosts and reused the supplied
pinned Rust/Azalea and byte-verified Folia dependency caches. Independent
network bootstrap therefore ran on Ubuntu 24.04 in GitHub Actions, run
[35716725121](https://github.com/lkjsxc/botsclustersmc/actions/runs/35716725121).
It completed successfully on 2026-09-22, downloaded dependencies without project
native/runtime caches, built the actual binaries, fetched pinned Folia 1.21.11
build 14, and compiled the real environment bridge against its extracted API.
Paperclip's patch-only step downloaded and patched the original Minecraft jar.
This CI did not start Minecraft or accept a Minecraft EULA for the operator.
The initial source-import workflow is removed after delivery; normal checkout
verification lives in `.github/workflows/source-runtime.yml`.

Independent suites passed locally and in the network bootstrap:

| Suite | Result |
| --- | --- |
| Shell/filesystem/package regressions | 19 passed |
| Fresh-bootstrap prerequisites, pins and failure guards | 8 passed |
| Java host recovery regressions | 24 passed |
| Native learning core | 34 passed |
| Native launcher | 53 passed |
| Actual Azalea adapter example | 38 passed |
| Pure Java configuration/protocol/campus checks | passed |

Native suites include overlapping core tests and must not be summed as unique
tests. Pure Java checks include one valid plus seven rejected protocol fixtures,
six rejected configuration cases, and 73,728 planner positions across 1,152 reset
transitions. They are not a substitute for loading the plugin on real Folia.
The separate 63-test RL-next library also passed locally; those tests do not
establish integration into the Minecraft application.

## Live test A: 32 bots, clean save and exact resume

Command (after EULA consent in the disposable test environment):

```sh
EULA=true SMOKE_BOTS=32 SMOKE_HEAP_GB=2 \
  SMOKE_SECONDS=120 SMOKE_RESTART_SECONDS=120 ./smoke.sh
```

Test host: Linux x86_64 sandbox, 4 CPUs / 4 GiB memory limit, Java heap 2 GiB,
Folia region threads 2, action rate 5 Hz. Each run was bounded by 120 seconds
after learner startup; bot logins were staggered, not all simultaneous at time
zero. Smoke deliberately uses 32-step rollouts and a 64-sample update threshold
so a short lifecycle run contains several updates. These are NOT default
training batch settings and are NOT a speed comparison against v0.3.1.

Both runs started real Folia 1.21.11 build 14/protocol 774, initialized and verified
all 32 enclosed cells, connected all 32 named bots, observed server statistics
and policy actions, updated actual model weights, and saved model/curriculum/world
before a clean exit. The script and both acceptance validators exited zero.

| Measurement | First run | Resumed run |
| --- | --- | --- |
| Run ID | 1790072266673294446-9979 | 1790072433433670948-12679 |
| Policy version | 0 to 16 | 16 to 33 |
| New PPO updates | 16 | 17 |
| Trained samples | 0 to 1178 | 1178 to 2444 |
| Optimizer steps | 0 to 84 | 84 to 171 |
| Policy fingerprint | 21e4b8fa7407bb6a to 4307c65914b9f029 | 4307c65914b9f029 to 5514dc2f7c0ae113 |
| Per-bot policy decisions | 234 to 460 | 267 to 481 |
| Minimum statistics packets per bot | 29 | 29 |
| Disconnects before stopped-run status | 0 | 0 |
| Discarded rollouts | 422 | 439 |
| Resumed checkpoint | false | true |
| Recorded learner error | empty | empty |

The resumed run restored the exact prior policy fingerprint, version, optimizer
step count and sample count, then performed new updates. Validators also checked
paired curriculum state and completed episodes for every bot. This is a
checkpoint/learning-lifecycle test, not proof that an exam happened or a skill
was mastered. Discarded-rollout counters are retained: the original runtime's
collection efficiency has not been fixed or hidden by these bootstrap changes.

## Live test B: the public start.sh path with normal learning settings

A separate new source folder with no Academy, world or model was created from
the exact published source payload. It reused the locally compiled binaries and
verified dependency caches because outbound DNS was unavailable. It did not
reuse test A's world or policy. The normal public entrypoint was invoked:

```sh
cp .env.example .env
EULA=true BIND_ADDRESS=127.0.0.1 JAVA_HEAP_GB=2 FOLIA_THREADS=2 \
  RUN_SECONDS=120 ./start.sh
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy
```

Both commands exited zero. Apart from loopback binding, test-host memory/threads,
and the finite run length, the example configuration was retained: 32 bots,
64-step rollouts, 2048-sample update threshold and 5 Hz actions.

- Run ID: `1790073334763380055-15141`.
- All 32 cells verified; all 32 bots spawned and acted.
- Policy version 0 to 4; trained samples 0 to 7456; optimizer steps 0 to 299.
- Policy fingerprint `21e4b8fa7407bb6a` to `4aa81593497e1d6c`.
- Per-bot decisions 272 to 471; minimum 29 statistics packets; zero disconnects.
- 123 discarded rollouts; learner error empty; valid paired checkpoints and
  clean world shutdown. All agents had completed at least one curriculum episode.

This tests actual directory creation, source-root routing and normal training
settings. It is deliberately distinguished from an entirely uncached local
`git clone`/download/start test: fresh downloads were validated separately in CI.

## Failed attempts and remaining limits

The first import CI, run
[35716538639](https://github.com/lkjsxc/botsclustersmc/actions/runs/35716538639),
failed because recovery tests were scheduled before compiling their Java host
helper. Reordering that prerequisite fixed it; no tests were disabled. The
corrected full run above passed.

This work establishes clone/build/start/train/save/resume infrastructure. It
has not established six-stage mastery, long-run learning stability, faster
learning than v0.3.1, human-like motion, an optimal resource configuration, or
indefinite uptime. Local runs used a smaller memory/CPU allocation than the
operator's target; they are not a capacity guarantee for that server. Linux
ARM64, old-world/checkpoint migration, public internet exposure and external
human-client connectivity were not tested. No operator world or secret was
uploaded. The smoke worlds remained disposable local fixtures.

The six runtime tasks are motor/aim/navigation/step/log-breaking foundations.
The experimental 18-task contracts, crafting, iron smelting, building, cooperative
living and RL-next's adaptive curriculum/cohort collection remain unintegrated.
Installing 0.4.0 does not silently activate those research components.
