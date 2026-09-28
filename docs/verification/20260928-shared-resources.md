# 2026-09-28 — Shared resource correctness

## Decision and exact source

Implementation: `c688e55fb0ba17e96799f86761e2f3723aa64d97`.
Tree: `2dc0edd67fd9a9ec6e295784eae3557bbce20c68`.
Base: `c70ce01a1ad7bf53b8d7578973783d364c926770`.

This change fixes the resource-operation substrate needed by later cooperative
survival. It does **not** adopt the failed conditional-menu or stone-reward
experiments, alter the neural policy, or establish learned teamwork.

The production changes are limited to `Pocket`, the operator-only `PocketView`,
`ExternalInventory`, and pickup handling in `WorldActions`. The source-test
launcher registers the new regression. Model dimensions, action meanings,
checkpoint format, learner, reward, curriculum, completion rules and evaluation
thresholds remain unchanged. Behavior deliberately differs for stale actions,
unrepresentable items and changed pickup callbacks.

## Reproduced defect

Against the unmodified deployment JAR from the base checkout, the new
`SharedInventoryTest` fails with exit 1:

```text
AssertionError: write inaccessible external slot
  SharedInventoryTest$Chest.set
  Pocket.set
  Pocket.click
  SharedInventoryTest.disappeared
```

A previously available chest/furnace is removed between observation and action.
The old bulk-withdrawal path still writes to the unavailable external inventory.
The corrected implementation rechecks the live mechanical affordance before
either inventory is changed.

The adapter also previously reconstructed every item from only its material and
count. The new boundary rejects lossy conversions and preserves inaccessible
world items instead of treating them as empty destinations. Pickup callbacks
cannot cause the old item snapshot to be inserted after the source was changed.

## Checks performed

All commands ran in a separate worktree under the shared development workspace.
The production Academy, its running process and its checkpoint were not restarted,
replaced or used as a disposable fixture.

| Check | Result | Meaning |
| --- | --- | --- |
| Full `./test.sh`, Java 21, 768-MiB heap / 2 effective processors | Exit 0 | Core, mechanics, ownership, action distributions, optimizer, persistence, evaluation and packaging tests |
| New fake-inventory regression | 3,437,225 assertions; 32,768 randomized two-pocket interleavings | Exact by-material resource conservation, limits, blocked slots and no fabricated crafting/extraction credit |
| Existing mechanical menu checks | 3,873,742 assertions | Predictions still agree with literal clicks |
| Operator pocket view | 28 assertions | Immutable contents and explicit unavailable slots |
| Folia 1.21.11 build 14 | Exit 0; 18/18 scripted fixtures | Real API/container/item/event execution, not learned trials |
| Folia item/resource diagnostics | 1,504 default materials; 3,109 assertions; 100 transfer cycles | Default-item round trips, seven unsupported-item categories, invalid counts, lowered container limit, revoked permission and removed chest |
| Paper 1.21.1 build 133 | Exit 0; 18/18 scripted fixtures | Named older server compatibility, not a claim about every Paper version |
| Paper item/resource diagnostics | 1,332 default materials; 2,829 assertions; 100 transfer cycles | Same checks on the actual older server |
| Pickup callback diagnostics, each server | 31 assertions; 10 modes | Normal, cancelled, removed, replaced, count changed, metadata changed, provenance changed, delayed, permission revoked, initially unsupported |
| Live fixture learner/inference work | 0 trained samples / 0 inference completions | Scripted reachability diagnostics are not neural-policy evidence |

The live transfer test uses two independent core pockets and two adapters over
one real chest, on its owner thread. It is **not** a learned two-NPC episode.
The pickup tests do use the actual fixture NPC and real server item entities.
All test mutations are restored before the ordinary task fixture runs.

The public JARs contain neither these diagnostic classes nor a scripted gameplay
fallback. The inference artifact separation check passes.

## Reproduction

From the exact implementation checkout:

```sh
JAVA_TOOL_OPTIONS="-Xmx768m -XX:ActiveProcessorCount=2" ./test.sh
```

After personally accepting the Minecraft EULA, with a new output directory for
each disposable run:

```sh
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures --output .build/shared-folia

python3 tests/download_server.py 1.21.1 .build/shared-paper-cache
EULA=true JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=2" \
  python3 tests/acceptance.py fixtures \
  --cache .build/shared-paper-cache --output .build/shared-paper
```

This run reused the production checkout's already pinned server **cache** for
Folia, not its Academy. The Paper downloader selected exactly 1.21.1 build 133
from the official listing without changing the production pin.

Retained local evidence in the implementation worktree:

- `.build/shared-red.log`
- `.build/shared-final-source.log`
- `.build/shared-final-folia-driver.log` and `.build/shared-final-folia/fixtures.log`
- `.build/shared-paper-download.log`
- `.build/shared-final-paper-driver.log` and `.build/shared-final-paper/fixtures.log`

The first source invocation emitted a build message without a completed test
receipt; it is not counted. The first two live attempts exposed a test-setup
mistake: the server normalized an inserted two-count wooden pickaxe to one
before the actuator ran. Oversized conversion rejection is now tested before
insertion, and container-limit changes are applied after inserting the fixture
stock. Exact preservation assertions remain in place for supported fixture
inputs and all nonrepresentable metadata categories. The final full source,
Folia and Paper runs above completed successfully.

## Limits and next meaningful work

These are resource-correctness results, not a higher learned success rate.
They do not resolve the existing cobblestone bottleneck, guarantee retention,
add autonomous role selection, add hunger/tool wear, or persist NPC inventories.

No production-learning restart is required to retain this evidence. A running
server continues using its loaded JAR until a controlled restart; updating source
alone does not hot-patch it. Previous restart-related retention failures remain
a separate concern.

Before claiming cooperation, evaluate neural decisions in a continuous,
shared-resource episode against matched controls and retain every trial.
Before claiming durable survival, test the joint save/recovery boundary between
world items and actor inventories. See [shared resource mechanics](../SHARED_RESOURCES.md)
and [the long-term gates](../COOPERATIVE_SURVIVAL.md).

## Subsequent activation

At the operator's later explicit request, the merged runtime was applied to the
learning server at 13:43:40 JST on September 28 with a completely fresh Academy.
Old weights, optimizer state and certificates were not restored. See the
[fresh main activation record](20260928-fresh-main-activation.md) for the deployed
JAR identity, 512-actor learning checks and the new policy's independent evaluation.
The earlier non-deployment statements above describe this implementation study,
not the server's state after that subsequent activation.
