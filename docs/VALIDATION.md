# Validation — integrated Academy v2 (0.5.0)

The actual source runtime connects the canonical RL mechanisms, 64 actors,
all 18 task environments and the read-only observer UI. Implementation,
scripted reachability and learned skill are different claims. No pretrained
skill, learning-speed improvement or cooperative-living result is shipped.

## Completed acceptance

[Final clean build and concurrent observer acceptance](verification/20260923-clean-concurrent-acceptance.md)
records successful run 35817819877 and its actual tested source
`c5aef6951fa104a4313a37cfb10ba7100ca81483`. It downloaded and built project
dependencies without restored project caches, passed native/host/Java checks,
started real Folia, reached all 18 full-difficulty tasks with separate scripted
diagnostics, and ran the normal 64-actor learner with a simultaneous observer.
The observer received actual task/PPO HUD and trained-sample TAB data while the
learner continued. Normal start/status/console/stop and exact model-state resume
passed: five total new PPO updates and 46,196 trained samples across two runs.

[Complete integration and failure history](verification/20260923-rl64-integration.md)
records the preceding independent successful cached-build run, all 18 task
evidence cases, protocol corrections, observer execution and 64-actor resume.
The [first cohort record](verification/20260923-first-64-cohort.md) describes an
earlier intermediate smoke build and must not replace the final acceptance.

Native suites include overlapping core tests: 97 core tests (debug and optimized),
118 launcher tests and 106 actual Azalea adapter tests. Package/bootstrap/recovery
checks passed 19/8/24 respectively. Java syntax, pure fixtures, actual Folia API
compilation and real plugin execution are separately identified in the records.
The [research scope](RESEARCH.md) distinguishes foundations from empirical claims.

## Reproduce checks

Developer tests require Python 3 in addition to normal build prerequisites.
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

`tests/public_entry_checks.py` exercises the normal start/status/console/stop
entrypoints and resume with 64 actors, 64-step fragments and 4,096-minimum batches.
It requires an otherwise fresh checkout without `.env`, `academy/` or `academy-v2/`,
already-built native and diagnostic-client binaries, and both EULA=true and
BCMC_TEST_NEW_ACADEMY=true. It refuses an existing operator installation.
The first run adds a read-only observer to all 64 actors, checks real HUD/TAB and
viewing operations, and requires continued PPO updates. A test-only legacy sentinel
and private example config must remain unchanged.

```sh
# Run after build.sh and test-live-fixtures.sh in an unused checkout only.
EULA=true BCMC_TEST_NEW_ACADEMY=true python3 tests/public_entry_checks.py
```

To audit a stopped ordinary Academy:

```sh
BCMC_ROOT="$PWD/academy-v2" ./bin/botsclustersmc-run verify-academy
# Optional precise model/version/optimizer-count/sample resume baseline:
BCMC_ROOT="$PWD/academy-v2" ./bin/botsclustersmc-run verify-academy /path/to/previous-status.json
```

The permanent source-runtime CI uses a clean checkout and no restored project
cache for native compilation and actual Folia API checking. It does not start
Minecraft or accept an operator EULA. Standalone numerical CI checks the canonical
modules; its synthetic bandit is not Minecraft evidence. The opt-in
`live-acceptance.yml` workflow requires an affirmative EULA-consent input and runs
the separate scripted fixtures plus normal 64-actor/concurrent-observer lifecycle.
That manual workflow may reuse compilation caches; the final uncached acceptance
is the separately identified completed run above.

## Limits

Historical 20260922 records describe the six-task/32-actor runtime. No run here
establishes human-like behavior, long-run reliability, ARM64 support, WAN human
connectivity, rendered desktop-client FPS, hardware capacity, faster learning than
the old algorithm or neural completion of all 18 tasks. Pending samples/actions
at shutdown are explicitly accounted for; zero unexpected drops is not a claim
that unfinished interactions were trained. Old weights are not silently migrated.
