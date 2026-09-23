# Independent frozen-policy trials

This opt-in developer test evaluates one saved policy in a disposable real
Minecraft server. Every gameplay action is sampled from the same immutable
neural policy. There is no scripted motor controller, optimizer, trajectory
training queue, curriculum promotion, or modification of the source checkpoint.
The regular training reset and success predicates are used at full difficulty.

After personally accepting the Minecraft EULA and building the current runtime:

```sh
./build.sh
EULA=true python3 tests/holdout.py \
  --policy academy/server/plugins/BotsClustersMC/policy.bcmc \
  --output .build/holdout \
  --tasks 0 1 2 --cases 64 --seed 618203
```

The output directory must be new. `--runtime` can identify an independently built
training JAR; `--cache` selects the prepared server cache. They default to
`dist/training.jar` and `.cache/server`. The evaluator is compiled separately and
is never included in either production JAR. Python is a test-only dependency.
The script requires explicit `EULA=true`, binds to loopback, rejects port 25565,
and disables human admission to this test server. No production service stops.

The per-task case count is bounded to 1–256, with at most 2048 trials total.
Each task uses a fixed initial seed schedule. Cases, task IDs and seed are stored
with every result. Full-difficulty tasks still have the documented academy
assistance: supplied targets/resources, rooms, resets and training invulnerability.

The results are written to `result.json`, including every failure. `metadata.json`
identifies the exact runtime JAR, evaluator JAR and immutable policy bytes.
The copied policy and runtime remain in the disposable output for reproduction.
The runner verifies the policy is unchanged, no training checkpoint exists, all
trials completed exactly once, and task aggregates match the individual cases.

**Exit code zero establishes experiment integrity, not learned mastery.** A report
with zero successful cases is a valid completed evaluation. Read the actual
success counts. Historical per-actor training certificates are not consulted.
A report for one frozen policy never certifies the continually changing live
policy or all tasks not selected for the experiment. To compare snapshots, use
the same runtime, tasks, case count and seed; still allow for asynchronous server
timing and stochastic action variation. New seeds test more than one fixed suite.

The test does not establish natural-terrain generalization, unrestricted survival,
long-lived NPC inventories, combat, food production, or multiplayer cooperation.
Do not label scripted reachability diagnostics as these neural-policy results.
