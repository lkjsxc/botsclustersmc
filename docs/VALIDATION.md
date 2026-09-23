# Validation — integrated Academy v2

The source runtime connects the shared RL mechanisms and all18 task environments.
Implementation, actual execution, scripted reachability and learned skill are
different claims. No pretrained skill or cooperative-living result is shipped.

## Recorded execution

[First64-actor cohort/update/restart record](verification/20260923-first-64-cohort.md)
identifies the exact intermediate source and settings of a successful real Folia
run:64 actual actors, six PPO updates, clean save, exact model/optimizer-count/sample
restoration and four additional updates. The record distinguishes smoke batches,
unfinished shutdown work, later observation changes and untested mastery.

The final source is subject to separate native, live fixture/observer and normal
public-entrypoint acceptance. Results must identify their actual source revision;
an earlier passing run is not evidence for untested later patches. See the dated
verification records added with final delivery for their completed results.

## Reproduce checks

Developer tests require Python3 in addition to normal build prerequisites.
Ordinary startup requires neither Python nor a GPU/LLM API.

```sh
./scripts/preflight.sh
./scripts/build-host-tools.sh
python3 tests/package_checks.py
python3 tests/bootstrap_checks.py
python3 tests/recovery_checks.py
./scripts/test-bridge.sh
./scripts/build.sh
```

Only after personally accepting the Minecraft EULA:

```sh
# Actual neural learner; independent world and model, not academy-v2/.
EULA=true SMOKE_BOTS=64 SMOKE_HEAP_GB=3 \
  SMOKE_SECONDS=240 SMOKE_RESTART_SECONDS=180 ./smoke.sh

# Separate scripted client: full-difficulty task reachability and observer UI.
# No learner model, demonstrations or learned-skill claim.
EULA=true ./scripts/test-live-fixtures.sh
```

`tests/public_entry_checks.py` verifies normal start/status/console/stop and
resume with64 actors,64-step fragments and4096-minimum batches. It explicitly
requires a fresh checkout without `.env`, `academy/` or `academy-v2/`, already-built
native and isolated diagnostic-client binaries and both EULA=true and BCMC_TEST_NEW_ACADEMY=true. It refuses to
run over an operator installation. The first run also connects a read-only observer alongside all 64 actors and
checks actual HUD/TAB packets, viewing operations and continued PPO updates.
Its test-only sentinel and example config
check that legacy data and private config are preserved.

```sh
EULA=true BCMC_TEST_NEW_ACADEMY=true python3 tests/public_entry_checks.py
```

To audit a stopped ordinary Academy:

```sh
BCMC_ROOT="$PWD/academy-v2" ./bin/botsclustersmc-run verify-academy
# Optional precise model/version/optimizer-count/sample resume baseline:
BCMC_ROOT="$PWD/academy-v2" ./bin/botsclustersmc-run verify-academy /path/to/previous-status.json
```

Permanent source-runtime CI uses a clean checkout and no project dependency cache
for native compilation and the actual Folia API check. It does not start a server
or accept the operator's EULA. Standalone numerical CI tests canonical modules;
its synthetic bandit is not Minecraft evidence. A full live gate additionally
needs explicit consent for an isolated server and separate measured results.

Historical20260922 records refer to the six-task/32-actor runtime, not the new
64-actor system. No run here establishes human-like behavior, long-run reliability,
ARM64 support, external human-client connectivity, hardware capacity, faster
learning than the old algorithm, or learned completion of all18 tasks.
