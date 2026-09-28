# 2026-09-28 — Cooperative world operations and a continuous two-body chain

## Exact source and decision

Implementation: `54c8bf24df4f1be71ab3c62e82533e818063d881`.
Tree: `e29cf5b22314ae5440ae8132b788fb0c2f6106e8`.
Base: `b49eb7a97415c92633dbc528702cbff89fd52598`.

The production change is confined to `plugin/.../WorldActions.java`. The core,
learner, rewards, task completion rules, curriculum, observation/action schema,
checkpoint format, host and external inventory adapter are unchanged. Both runtime
JARs use the corrected actuator; no diagnostic classes are packaged in them.

Before introducing team rewards or a larger controller, this increment establishes
that multiple bodies can conserve useful resources through a continuous operation
chain. It also removes two reproduced ways to destroy shared-world state. This is
not a learned-team-policy result, and no task-allocation or reward experiment is
being silently promoted into production.

## Reproduced failures

### A callback replaced the mining target

The new live checks compiled against the unmodified base runtime and failed with:

```text
java.lang.AssertionError: stale world overwrite: false replace
```

An `EntityChangeBlockEvent` listener replaced the original log with a diamond
block. The old mining action still overwrote that replacement with air. The new
callback boundary retains block data and relevant actor/inventory state and
rechecks current permissions and ownership before applying the action. A rejected
operation leaves the listener's result alone rather than trying to roll it back.

### A filled chest lost its reserve

With the first callback/occupancy guards already applied, an additional storage
regression exposed the unchanged `getDrops`/`setType` mining path. A chest holding
seven named diamonds was destroyed; the diagnostic observed only:

```text
CONTAINER REMOVAL DIAGNOSTIC material=CHEST mode=full-before drops=[ItemStack{CHEST x 1}]
java.lang.AssertionError: shared stock container destroyed: CHEST full-before
```

The reserve was neither kept in the chest nor emitted as an item. The fix is
explicitly conservative: an NPC cannot mine a nonempty local chest or furnace.
Its snapshot is checked before accumulating mining progress and again after the
block-change callback. Empty storage remains mineable. This does not pretend to
implement native block-entity spilling, loot-table handling or durable inventory
transactions.

## Same-source verification

The final source run and both final real-server runs used implementation `54c8bf2`
with Java 21.0.12.1. Source tests used a 768-MiB heap cap and two effective processors;
the fixture launcher retains its own explicit server heap setting. Server caches
were reused, but disposable worlds and output directories were new. The existing
Academy was not used as a fixture.

| Check | Result | Scope |
| --- | --- | --- |
| Full `./test.sh` | Passed | Core, mechanics, numerical/gradient checks, ownership, curriculum, persistence, evaluation and artifact separation |
| Folia 1.21.11 build 14 | `PASS acceptance mode=fixtures`; 18/18 task fixtures | Actual pinned server, scripted reachability only |
| Paper 1.21.1 build 133 | `PASS acceptance mode=fixtures`; 18/18 task fixtures | Actual named older server, not universal version support |
| World mutation diagnostics, each server | 180 assertions, 29 cases | Callback replacement/cancellation, same-material block data, permission, held/selected stack, goal, aim, menu/removal state, peer occupancy and filled containers |
| Two-body continuous chain, each server | 155 assertions, 68 scheduled ticks | Two actual NPC bodies and distinct pockets; one actuator call per actor per action tick |
| Existing native inventory checks | Folia: 3,109; Paper: 2,829; 100 transfer cycles each | Supported/default items and preservation of unrepresentable stock |
| Existing pickup callback checks, each server | 31 assertions, 10 modes | Callback-safe native item pickup |
| Fixture neural work | 0 trained samples; 0 inference completions | No scripted work is counted as a learned-policy rollout |

The 29 cases comprise ten modes (including ordinary success) for each of mining
and placement, a same-material orientation mutation, peers present before/during placement, and
empty/full-before/full-during-event checks for both chest and furnace. Rejected
edits must not spawn new drops, consume placement stock or create success credit.

The two-body fixture starts A with three planks and B with two sticks. A's first
incomplete recipe yields nothing and preserves the ingredients. B deposits sticks;
A withdraws them, crafts one wooden pickaxe with literal menu clicks and deposits
it. B withdraws the pickaxe, mines one real stone block over actual server ticks,
picks up one cobblestone and deposits it in the shared chest. The final tool stays
with B, the chest contains exactly one cobblestone, raw ingredients are consumed
once, and transfer operations produce no extra crafting/extraction/harvest credit.

There are no intermediate teleports, pocket resets or extra resources. Actions,
timing and aiming are scripted; the nearby stations, stone, supplies and invulnerable
training bodies are reset fixtures. This test exercises the current simplified
pocket and actuator, not vanilla-player equivalence, navigation, neural observations,
role selection, partner generalization or learned cooperation. The failed partial
recipe is a mechanical negative check, not a matched learned-policy control.

## Reproduction and retained evidence

After personally accepting the Minecraft EULA, from the implementation checkout:

```sh
JAVA_TOOL_OPTIONS="-Xmx768m -XX:ActiveProcessorCount=2" ./test.sh

EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --output .build/cooperative-folia

python3 tests/download_server.py 1.21.1 .build/cooperative-paper-cache
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --cache .build/cooperative-paper-cache \
  --output .build/cooperative-paper
```

Final source and driver logs in the isolated worktree are
`.build/cooperative-source-final.log`, `.build/cooperative-folia-final.log` and
`.build/cooperative-paper-final.log`. Each final driver ends in
`PASS acceptance mode=fixtures`. Final server evidence is retained in the sibling
`botsclustersmc-verified-54c8bf2-folia` and `botsclustersmc-verified-54c8bf2-paper`
directories, including `fixtures.log`, pass markers and zero-neural-work status.

The original callback failure is retained in sibling
`botsclustersmc-world-mutation-baseline`; the storage failure is retained in
`botsclustersmc-container-mining-baseline`. Earlier successful development runs
are not substituted for the final pinned-source reruns. One combined tool call
lost its output connection while its tests continued; the final runs were repeated
with retained driver logs rather than treating the transport error as a pass.

## Existing learning process and remaining work

The live Academy was not stopped, reset, given new weights or replaced with a
scripted demonstration. At 2026-09-28 14:25:01 JST its status reported 512 active
actors, all 512 progressing during the latest interval, 4,499,220 trained samples,
16,433 updates, and zero inference failures, rejections or retired actors. The
course population had advanced to task 2 (aiming). Historical course certificates
for tasks 0/1 are not a new independent test of the current weights.

This source change does not hot-patch that already-running JVM. It continues using
the previously loaded implementation until a controlled restart. Keeping this
healthy, freshly initialized learning process intact avoids conflating a mechanics
change with known restart/retention concerns. No new learned success rate or
retention improvement is claimed by this increment.

The next cooperation gate remains a neural-policy experiment with one common
resource objective, continuous inventories, completion-based outcomes and matched
non-cooperative controls. It must retain failures and must not use this scripted
sequence as a runtime fallback. Autonomous goal selection, food/tool maintenance,
persistent lives and world/pocket recovery are still separate unimplemented gates.
See [shared resource semantics](../SHARED_RESOURCES.md) and
[the development order](../COOPERATIVE_SURVIVAL.md).
