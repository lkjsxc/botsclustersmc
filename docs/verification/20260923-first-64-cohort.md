# First integrated 64-actor lifecycle — 2026-09-23

## Exact source and execution

GitHub Actions run [35809663531](https://github.com/lkjsxc/botsclustersmc/actions/runs/35809663531),
job107018153287, completed successfully. Its workflow event began at commit
3091067d3cf337bdcb82e5316595685958760954 and applied its reviewed plaintext
compatibility patch before testing. The actual built/tested source was
**5a0ea13f03b7e4f285ba736b7289c198932bce70**.

This is an intermediate integration revision, before the later explicit GUI-menu
identity observation and item-merge guard. Do not describe it as verification of
subsequent source changes. Final acceptance is tracked separately in VALIDATION.md.
The local execution container was unavailable; these are actual GitHub-hosted
builds/runs, not claims of local compilation.

The runner used Ubuntu24.04 x86_64, Java21, pinned Rust nightly-2026-02-04 and
Azalea f8ddefa70cc53e6385785fb56e7a688a389cf0ab. It built the actual application,
fetched Folia1.21.11 build14/protocol774 and compiled the bridge against its real
extracted API. No project native/runtime cache or imported application binary
substituted for that build. Folia ran with a 3 GiB heap and two region threads;
this differs from the operator defaults and is not a 12 GiB allocation benchmark.

Package, bootstrap and recovery regression suites passed19,8 and24 tests.
The corresponding native suites passed96 learning-core,115 launcher and100 actual
adapter tests, with overlapping core tests. Standalone checks covered62 library
and one synthetic CLI test. Pure Java checks covered64 cells,18 fixtures and
2,654,208 reset positions; those pure checks are not evidence of live advanced-task
reachability or learned skill.

## Real clients, real updates, real restart

After explicit consent for the disposable CI server, the workflow ran:

```sh
EULA=true SMOKE_BOTS=64 SMOKE_HEAP_GB=3 \
  SMOKE_SECONDS=240 SMOKE_RESTART_SECONDS=180 ./smoke.sh
```

This uses smoke settings: 32-step fragments and total minimum batch64. Actors
still finish real episodes and all64 must contribute. These are not normal
64-step/4096-minimum batches and not a controlled comparison against v0.4.0.
Both actual Folia rounds passed the native validators and stopped cleanly.

| Measurement | First run | Resumed run |
| --- | --- | --- |
| Run ID | 1790130054171523256-11720 | 1790130331409569220-15394 |
| Policy version | 0→6 | 6→10 |
| New PPO updates | 6 | 4 |
| Trained samples | 0→44,526 | 44,526→58,559 |
| Adam steps | 0→2,097 | 2,097→2,763 |
| Initial fingerprint | 5d45a3b661be8981 | 650d5df29528db6d |
| Final fingerprint | 650d5df29528db6d | 2252014553dd0cf9 |
| Actual online actors | 64 | 64 |
| Minimum decisions per actor | 405 | 100 |
| Disconnections before saved status | 0 | 0 |
| Discarded-rollout counter | 0 | 0 |
| Learner error | empty | empty |

The resumed run restored the exact prior model fingerprint, policy version,
optimizer count and sample count before performing four more updates. The bundle
also serializes the optimizer RNG, actor RNGs and course; native round-trip tests
cover their serialization. Both validators accepted completed authoritative
episodes for every actor. No population was silently reduced.

All ten accepted whole-batch sampled KL values were below0.03, and this particular
run needed one optimizer attempt per update. Update times ranged from1,648ms to
4,591ms while batch sizes ranged from3,026 to9,004. These are observed values for
this run, not a maximum, typical performance promise or baseline comparison.

**Zero discarded rollouts does not mean all in-flight work was trained.** At the
resumed shutdown, status retained1,727 cohort-buffered transitions,198 local
untrained transitions and8 unfinished actions, with56 actors sealed. Those
unfinished episodes are reset after restart; no final reward/state is invented.
The distinction is now exposed in the normal Academy status.

## Limits and subsequent work

Both runs remained at task0. No frozen exam, skill mastery, retention,
generalization, autonomous crafting, cooperative living or indefinite uptime was
established. Advanced task contracts/environments were wired into this source,
but actual scripted reachability is a separate gate. The test used the smoke
entrypoint in owned disposable directories; a fresh normal source-root Academy
is also a separate acceptance path.

The run's evidence artifact10729182993 contains disposable logs and status,
not operator worlds, weights or secrets. Historical32-actor records remain
historical and are not reused as64-actor evidence. Compiler/API failures during
integration and subsequent patches are retained in Actions history and the final
acceptance record rather than hidden by disabling tests.
