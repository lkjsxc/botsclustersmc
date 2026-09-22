# Validation — source-first 0.4.0

The actual Minecraft runtime is committed to `main`, not only RL-next components.
The complete measured record, source identity, settings, failed CI attempt and
limits are in [verification/20260922-source-runtime.md](verification/20260922-source-runtime.md).

## Completed evidence

Independent fresh-dependency CI successfully built the real Rust actor/learner
and launcher, downloaded pinned Folia and Gson, and compiled the Java bridge
against the real server API. Rust suites passed (34 core / 53 launcher / 38
adapter tests, with overlapping core tests); package/bootstrap/recovery suites
passed (19 / 8 / 24). Pure Java fixture checks also passed.

Real local Folia ran 32 bots through two bounded smoke rounds: 16 PPO updates,
clean checkpoint/world save, exact state restoration, then 17 further updates.
A separate new Academy created through the normal `start.sh` entrypoint with
normal learning settings completed four PPO updates and a clean shutdown.
Local execution reused verified dependency caches; independent first-download
behavior was checked separately in CI. Neither is being mislabeled as the other.

This is execution and actual learning-update evidence, NOT a claim of skill
mastery or faster learning. The six foundation tasks are the runtime curriculum.
The 18 experimental task contracts and cooperative survival are not integrated.
Discarded rollouts, short test durations and untested targets are documented.

## Reproduce a live acceptance run

Only after independently accepting the Minecraft EULA:

```sh
EULA=true SMOKE_BOTS=32 SMOKE_HEAP_GB=6 ./smoke.sh
```

This creates a separate `experiments/smoke-*` world and model; it does not use or
overwrite `academy/`. It runs real Folia and the Rust policy, validates current-run
campus/protocol/population receipts and actual PPO updates, stops cleanly, then
restarts to check the exact previous policy/optimizer/sample counters and new
updates. Smoke uses smaller batches than ordinary training. A pass does not
imply human-like behavior, course completion or collective living.

To audit the actual stopped Academy:

```sh
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy
# Optional exact resume comparison against a saved prior run status:
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy /path/to/previous-status.json
```

`.github/workflows/source-runtime.yml` verifies a fresh repository checkout with
no restored native/runtime dependency cache. CI uses Paperclip patch-only to
check the actual API; it does not start a server or accept an EULA on the user's
behalf. Developer regression suites use Python 3, but normal runtime does not.
Historical REPAIR-REPORT.md and Japanese repair records describe the old 0.3.1
bundle, not new verification of this source delivery.
