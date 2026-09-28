# 2026-09-28 — Keep harvest provenance across spawn callbacks

## Source and scope

Implementation: `75e6706f70d1f1220803ea88a27433ee126bfcbf`.
Tree: `6d988f0efbc4d8e965f17a7f2faa2f8f4d370852`.
Base: `47a857ae88982d19706cf725b42fa0f4ffb54133`.
Pull request: #29.
Workspace: `lkjsxc/tomato-ocelot-73`.
Worktree: `/home/coder/workspace/botsclustersmc-harvest-spawn`.

The runtime change is eight changed lines in `WorldActions.mine`, not a learner,
policy, reward, curriculum, action/observation schema, checkpoint or server-pin
change. It initializes the original harvest token and zero pickup delay before
`ItemSpawnEvent`, rather than overwriting the event's result afterward. Both
runtime artifacts use the same adapter. No diagnostic code is packaged in them.

## Reproduced old-runtime failure

The new fixture was first compiled against the existing mainline training JAR,
without applying the fix. Its 103 runtime class entries were byte-compared with
that mainline artifact. The old runtime on real Folia 1.21.11 build 14 produced
50 failed assertions across 20 cases and 268 checks:

| Failure | Assertions |
| --- | ---: |
| Source token absent inside the spawn event | 20 |
| Intended zero pickup delay absent inside that event | 20 |
| Listener's requested pickup delay overwritten | 2 |
| Provenance changed after the listener | 6 |
| Old harvest eligible for a new episode | 2 |

The last two cases are the consequential episode-boundary regression. An
`ItemSpawnEvent` listener advances the actor's episode while a log or cobblestone
is being spawned. Previously `npc.token()` was read only after the listener,
so an item from episode 1 received episode 2's token. The same token-equality
predicate used by `TrainingPlugin.canPickup` then admitted it for the new episode.
This is an actual adapter reproduction, not evidence that ordinary uninterrupted
training frequently takes this callback path. The test checks pickup eligibility;
it does not report a fabricated downstream reward measurement.

## Why harvest differs from a held-item drop

The source block has already been removed before its yield is spawned. Harvest
therefore does not use the provisional pocket debit in `DroppedItems` and does
not invent an `EntityDropItemEvent` for an inventory item that was never dropped.

The original episode token is captured before obtaining drops and removing the
block. Each item receives that token and the ordinary zero pickup delay in the
pre-spawn consumer. Afterward, the adapter does not reset the spawned item's
provenance, delay, contents, pickup permission, owner or thrower. Cancellation
remains cancellation. A listener's replacement or intentionally withheld yield
is not overwritten, recreated or compensated by restoring the block.

This is scoped synchronous-event correctness. It is not a security sandbox for
arbitrary plugins, crash-atomic world persistence, full vanilla harvesting, or
a validation of all post-edit physics and cross-region relocation cases. The
pre-existing block-edit validation and its limits are unchanged.

## Same-source verification

The final source suite, Folia run and Paper run all used implementation `75e6706`.
The source suite exited zero with the final artifact-separation marker. Source
tests compile the live fixture but do not execute Minecraft; the two separate
acceptance runs below do. Both use fresh disposable worlds, not the Academy.

| Test | Folia 1.21.11 build 14 | Paper 1.21.1 build 133 |
| --- | ---: | ---: |
| New harvest callbacks | 20 cases / 268 checks | 20 cases / 268 checks |
| Existing held-item drop callbacks | 47 cases / 1,870 checks | 47 cases / 1,870 checks |
| Existing native pickup callbacks | 15 modes / 46 checks | 15 modes / 46 checks |
| Existing world mutation checks | 29 cases / 180 checks | 29 cases / 180 checks |
| Shared inventory checks | 3,109 / 100 transfer cycles | 2,829 / 100 transfer cycles |
| Two-body continuous resource chain | 155 checks / 68 ticks | 155 checks / 68 ticks |
| Full-difficulty scripted task fixtures | 18/18 | 18/18 |
| Fixture learning samples / inference completions | 0 / 0 | 0 / 0 |
| Matching runtime class entries | 103 | 103 |
| Acceptance exit | 0 | 0 |

The ten modes, each tested for log and stone, are ordinary spawn, cancellation,
delay change, provenance replacement/removal, episode change, custom item-stack
replacement, mob-pickup denial, owner and thrower changes. Callback exceptions
are captured and checked outside the event bus. Assertions additionally require
one source block break, unchanged held tool, no spawn-only collection credit,
normal yield for unchanged events and no invented inventory-drop event.

The acceptance launcher now requires both the harvest marker and the existing
held-item drop marker; skipping either suite cannot silently pass. Both public
JARs were checked for absence of diagnostic/holdout classes, and the inference
JAR for absence of training classes. All 103 runtime classes embedded in each
fixture JAR were byte-identical to the final training JAR.

These scripted fixtures prove mechanics, not neural mining, retained skills,
learned cooperation, or a self-maintaining settlement. No frozen-policy retention
comparison or learner experiment was conducted in this change.

## Reproduction and retained evidence

The compact machine-readable record is
`data/20260928-harvest-spawn-boundary.json`. The isolated worktree retains the
baseline failure in `.build/harvest-baseline/`, initial passing run in
`.build/harvest-folia-fixed/`, final source log in `.build/harvest-source-final.log`,
and final server directories `.build/harvest-folia-final/` and
`.build/harvest-paper-final/`. Final driver logs and explicit exit records are
beside them. `.build/verify_harvest.py` verifies source, counts and byte identity
before writing `.build/harvest-verified.json`.

Java was OpenJDK 21.0.12.1. The final source suite used a 768-MiB heap cap and two
effective processors. Both live fixtures used two effective processors and their
explicit 2-GiB heap. Only the two named server versions are claimed as tested.

After explicit EULA consent, use unused output directories:

```sh
BCMC_SERVER_CACHE=/path/to/folia-cache \
  JAVA_TOOL_OPTIONS="-Xmx768m -XX:ActiveProcessorCount=2" ./test.sh
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --cache /path/to/folia-cache \
  --output .build/harvest-folia-reproduction
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --cache /path/to/paper-cache \
  --output .build/harvest-paper-reproduction
```

Do not replace retained failures with later successful output. The shared
Academy's controlled activation is recorded below after mainline integration;
source/JAR tests alone are not an activation claim.

## Mainline integration and shared Academy activation

PR #29 merged as `4b1839c89cc038c6cb47f50c3b4bc6c20b9049c8`, independently
re-read through the GitHub ref endpoint. The production checkout was clean and
fast-forward synchronized. CI runs `36420956606` (implementation), `36421350203`
(final PR head) and `36421657388` (merged main) completed successfully, including
Ubuntu, Windows and the synthetic observatory checks. Optional cloud live jobs
were skipped; the Folia/Paper evidence above comes from the separately executed
local real-server runs, not from skipped CI jobs.

The merged mainline was rebuilt and both artifacts matched the tested JARs.
The evaluation timer/service were stopped, then the training service stopped
normally and saved 183,216 updates and 49,503,195 accepted samples. Its log also
reported 5,023 buffered-untrained samples; these are not claimed as saved learned
state. No checkpoint, course, configuration or operator world was deleted.

Before starting the new process, the activation driver waited for an exclusive
bind check on the unchanged existing port 25566 to succeed. It succeeded on the
32nd check. The driver did not change socket policy, network exposure or kill a
competing process. The exact reason for the earlier unavailable port was not
established by this check. This activation started successfully without automatic
service retries; the previous service's one retry belongs to the earlier record.

The new supervisor PID is 121202 and its Minecraft child is 121289. Installed
`academy/server/plugins/training.jar` matches the tested training artifact. The
startup log restored exactly 183,216 updates, 49,503,195 samples and Adam step
183,216. Compatible learning state was retained; a reset was unnecessary.

| Live observation, 2026-09-28 JST | First: 21:26:25 | Last: 21:27:00 |
| --- | ---: | ---: |
| Active / progressed actors | 512 / 512 | 512 / 512 |
| Pending actors | 0 | 0 |
| Accepted training samples | 49,519,720 | 49,590,867 |
| Policy updates | 183,308 | 183,542 |
| Decision transitions in new process | 24,643 | 96,198 |
| Inference failures / rejections / retired actors | 0 / 0 / 0 | 0 / 0 / 0 |

Across the 35-second observation, accepted samples increased by 71,147 and
policy updates by 234. Both statuses reported `running`, restored checkpoint
state and frontier task 10. The supervisor PID, invocation and zero automatic
restart count remained unchanged throughout the observation. The unchanged
monitor PID 84532 returned fresh status and required no restart. The previously
active evaluation timer was restored after successful checks.

The compact activation receipt is `data/20260928-harvest-activation.json`.
The isolated worktree retains `.build/activate_harvest.py`, the full
`.build/harvest-activation.json`, driver/build logs and old/new invocation
journals. This proves deployment and resumed learning, not retention of every
skill or increased task success. The final activation record changes only
Markdown/JSON documentation; runtime inputs remain byte-identical, so it does
not require another service restart.
