# 2026-09-28 — Callback-safe NPC drops and native pickup restrictions

## Source and scope

Implementation: `85be01d9c8935065d9893f1a469ccd36325fc8e7`.
Tree: `5d6b74453148af1e7dea466626310030e1619656`.
Base: `5fa2a2b99a7691b7b0a7203b0ef79c25f67c2b08`.
Workspace: `lkjsxc/tomato-ocelot-73`.
Isolated worktree: `/home/coder/workspace/botsclustersmc-drop-conservation`.
Pull request: #28.

The runtime change is limited to the server adapter: a new `DroppedItems` helper,
one transient owner-thread reentrancy flag in `Npc`, and `WorldActions` drop/pickup
boundaries. Core inference, observation/action schema, rewards, learner, curriculum,
completion rules, checkpoint format and host source are unchanged. Both public
runtime JARs use the corrected adapter; neither contains diagnostic classes.

This repairs resource conservation before later cooperation experiments. It is
not a learned-policy improvement and does not promote the rejected stone-transfer
initialization, teach mining, or change the running Academy's weights.

## Failure reproduced before the fix

The new live checks were compiled against a freshly built, unmodified base
runtime. Real Folia 1.21.11 build 14 reported:

```text
java.lang.AssertionError: drop consumed stale inventory: drop held-kind slot=0
```

The NPC selected a drop from eight logs. During `EntityDropItemEvent`, the test
listener replaced that held slot with four planks. The old code had already
created the log entity, then consumed one item from the new held stack. It thus
left a stale log in the world and reduced the replacement planks to three.
Cancellation alone did not protect this uncancelled callback boundary.

There is a second callback before that event: `World.dropItem` fires
`ItemSpawnEvent`. Previously the newly spawned item did not yet have its NPC
provenance or final pickup settings, and recursive actuator entry was unguarded.
The fix handles both boundaries rather than only the reproduced final debit.

## Commit semantics

A drop creates one provisional item, initialized with the episode provenance,
a pickup delay of 32767 and disabled mob pickup before the spawn event. The
original held stack, selected slot, goal/token and actor pose are retained.

After each event the adapter checks actor ownership, validity and running state,
unchanged source inventory/goal/pose, and the provisional item's ownership,
validity, position, exact contents, provenance, native owner/thrower and pickup
settings. Cancellation or a relevant change rejects the operation. Only the
new provisional entity is discarded; a callback's replacement pocket is not
consumed or rolled back. Unrelated pocket slots may change without rejecting an
otherwise valid drop. Recursive actuator calls on this dropping actor are ignored.

A successful drop consumes exactly one unchanged held item, enables ordinary
mob pickup, and applies the existing 20-tick pickup delay. No crafting, extraction
or collection credit is earned by dropping. This adapter deliberately rejects
listeners that transform the provisional output's item, count or ownership.

Custom NPC pickup now respects native `canMobPickup` and native item owners,
before and after its own event: an owner must be absent or match that NPC. An
owner changed in the callback invalidates the earlier pickup snapshot. These
checks also keep the provisional drop unavailable to another ordinary NPC.

## Same-source verification

The final full source suite and both final live runs used implementation
`85be01d`. The source suite exited zero. All 103 runtime class entries in each
fixture JAR were byte-compared with the final training JAR. Both runtime JARs
were checked for absence of diagnostic/holdout classes. Disposable worlds were
new and distinct from the production Academy.

| Check | Folia 1.21.11 build 14 | Paper 1.21.1 build 133 |
| --- | ---: | ---: |
| Drop/spawn checks | 47 cases, 1,870 assertions | 47 cases, 1,870 assertions |
| Native pickup checks | 15 modes, 46 assertions | 15 modes, 46 assertions |
| Existing world mutation checks | 29 cases, 180 assertions | 29 cases, 180 assertions |
| Existing continuous two-body chain | 155 assertions, 68 scheduled ticks | 155 assertions, 68 scheduled ticks |
| Existing full-difficulty task fixtures | 18/18 | 18/18 |
| Fixture learning samples / inference completions | 0 / 0 | 0 / 0 |
| Acceptance driver exit | 0 | 0 |

The 47 drop cases comprise empty-hand rejection and 23 modes at each callback
boundary: ordinary success, cancellation, changed held kind/count/selection,
menu, goal, actor/global pause, reset/removal flags, item removal/kind/count/
metadata/provenance/delay/mob-pickup/owner/thrower changes, an unrelated inventory
change, recursive actuator entry, and checks of pre-callback initialization.
Assertions inside event listeners are explicitly captured and rethrown outside
the event bus, so the server cannot swallow a failed callback assertion and
report that case as passed.

The five additional pickup modes exercise mob pickup disabled before/during the
callback, a foreign owner, the NPC as rightful owner, and an owner changed during
the callback. Existing metadata/provenance/count/cancellation checks remain.
The continuous chain still crafts a pickaxe from two pockets' complementary
supplies, shares it through a chest, mines stone, collects cobblestone and deposits
it without intermediate resource injection. These actions are scripted test
inputs, not neural decisions or evidence of learned cooperation.

Java was OpenJDK 21.0.12.1. Source tests used a 768-MiB heap cap and two effective
processors. Live launchers used their own explicit 2-GiB heap and two effective
processors. The two named server versions passed; universal version or plugin
compatibility is not claimed.

## Retained evidence and reproduction

`data/20260928-drop-conservation.json` records source, actual server identities,
all task IDs, counts, zero neural work, runtime-class matches and log identities.
The worktree retains `.build/drop-source-final.log`, `.build/drop-folia-final/`,
`.build/drop-paper-final/`, both driver logs and their explicit exit records.
The verifier `.build/drop_receipt.py` checks these inputs before emitting that
receipt. The failed old-runtime run is retained in `.build/drop-baseline-built/`.

An earlier invocation before building the new worktree failed because
`dist/training.jar` was absent; `.build/drop-baseline/` is setup-failure evidence,
not a reproduced runtime failure. Two foreground source invocations lost their
MCP output connection and are not counted as full successes. A separately logged
source run completed with an explicit zero exit, followed by the final pinned
source rerun. Initial corrected live evidence remains in `.build/drop-folia-first/`
and is not substituted for the final committed-source results.

After explicit EULA consent, the final commands were:

```sh
JAVA_TOOL_OPTIONS="-Xmx768m -XX:ActiveProcessorCount=2" ./test.sh
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --cache /path/to/folia-cache \
  --output .build/drop-folia-final
python3 tests/download_server.py 1.21.1 .build/drop-paper-cache
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --cache .build/drop-paper-cache \
  --output .build/drop-paper-final
```

Use new output directories when reproducing; do not overwrite retained evidence.
The live fixture commands require the preceding build/source suite.

## Production state and limits

The production learner, monitor, worlds and weights were not stopped, replaced
or reset. At 2026-09-28 20:44:08 JST it reported 512 active/progressing actors,
44,612,760 accepted samples, 166,989 policy updates, task 10, and zero inference
failures, rejections or retired actors. The task number and changing-policy
training telemetry are not a frozen competence or retention certificate.

A mainline merge does not hot-patch the existing JVM. This process continues
using its previously loaded implementation until a controlled restart. Installing
this mechanics change must not be reported as already completed in that JVM.

Owner-thread cleanup has a scheduled branch for a provisional item moved to
another Folia region. The final tests do not perform cross-region migration or
an asynchronous actor teleport; that branch is not live relocation evidence.
These synchronous checks are not a sandbox for arbitrary plugins, protection
against plugins directly duplicating/transferring items, or crash-atomic
world/pocket persistence. Durable lives and learned cooperative behavior remain
separate gates in [the development order](../COOPERATIVE_SURVIVAL.md).
