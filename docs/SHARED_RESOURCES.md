# Shared resource mechanics

This is a boundary for later cooperative-survival experiments, not a learned
team policy. NPCs still need externally supplied goals and do not yet have
durable lives, hunger, tool wear or autonomous settlement planning.

## An observed click is not a reservation

A neural decision is applied after the observation was captured. Another NPC,
a player or the world may change a chest in between. The action mask only
describes the earlier observation.

`Pocket.click` checks the current, goal-independent mechanical affordance before
mutating either inventory. A missing or inaccessible external slot is a no-op.
The server adapter is resolved on the owning region thread for the current
container, world, distance and world-edit permission. It uses one chest block's
inventory, not an unchecked cross-region double-chest view.

Two NPC pockets may interact with the same chest through serialized owner-thread
operations. They read the current stock at execution time; an earlier successful
withdrawal does not reserve another copy for a second actor. Chest deposits and
withdrawals do not create crafting or furnace-extraction credit.

## Native protection is not communal stock

The same `ContainerAccess` gate is used when opening chest/furnace menus, resolving
current clicks and checking whether the simplified mining path may remove a
container. It checks owner/loading boundaries before block state, then requires
a placed, unlocked container without an attached native loot table before any
inventory getter. A chest exposes only the selected block's local half.

A lock added after observation makes the current transfer unavailable. The next
observation closes the invalid external menu without discarding carried items.
Empty-looking locked storage and deferred loot are not mineable; protection is
checked again after the block-change callback. No code clears locks, populates
loot or loads neighboring chunks to obtain access. Unexpected API errors are
not swallowed as successful empty reads.

This is a conservative NPC policy for native locks, not player-key matching,
complete block-component preservation or integration with every protection plugin.
Other block metadata and crash-atomic world/pocket saves are still unsupported.
The status field `container_access_contract` records `owned-unlocked-no-loot-local-v1`
for the deployed gate. It certifies neither learned cooperation nor world safety
outside the explicitly tested mechanics.

## Items must round-trip without losing information

The current core pocket represents an exact material name and a count, not all
Minecraft item components. Taking a named, damaged, enchanted or otherwise
customized item and rebuilding it from the material name would destroy data.
Stack limits also differ: the core currently represents pickaxes with a limit
of one and other materials with a limit of 64.

The server adapter admits an item only when its non-count state matches a
default item of the same material and its actual stack limit matches the core
representation. Counts must fit both the item and the external inventory.
Examples:

| World item | Current treatment |
| --- | --- |
| Plain logs, planks, sticks and cobblestone | Ordinary pickup and container operations |
| Plain, undamaged pickaxe | Ordinary operations, maximum one per slot |
| Named, damaged, enchanted or custom-data item | Left untouched in the world/container |
| Item with a different stack limit, such as an ender pearl or shears | Left untouched |
| A container slot exceeding a newly lowered inventory limit | Inaccessible until its state is representable |

An inaccessible slot is not an empty destination. Shift transfers skip it.
Operator pocket views show `unavailable` rather than misreporting the slot as
empty. The actor's mechanical action mask excludes it without adding a preferred
recipe, ingredient, destination or team role.

This is intentionally conservative. It does not add general component support,
repair already-stripped items or make the simplified pocket a vanilla inventory.
A future richer representation must retain components, stack limits and bounded
serialization consistently in training, inference and durable storage.

## Pickup callbacks may change the item

The runtime snapshots a supported item before firing `EntityPickupItemEvent`.
After callbacks it checks current ownership, actor/item validity, pickup delay,
native mob-pickup permission, native item owner, item contents, provenance,
plugin permission and proximity again. A native item owner must be absent or
match this NPC; changing that owner during the callback rejects the pickup.
Cancellation, removal, replacement or other relevant changes abort that pickup without inserting the
old snapshot into the pocket or overwriting the callback's result.

This protects the supported synchronous owner-thread operation. It is not a
database transaction, a guarantee against plugins violating server-thread rules,
or crash-safe persistence across world and NPC saves.

## Dropping is a two-event operation

An NPC drop creates one provisional item, then checks both `ItemSpawnEvent` and
`EntityDropItemEvent` boundaries. Before the spawn callback, the new entity has
its provenance token and temporarily blocked pickup settings. Neither another
NPC nor the ordinary native pickup path should collect it before the pocket is
debited. Recursive actuator calls on the dropping NPC are ignored until the
outer operation finishes.

After each event, the runtime rechecks the original selected slot and held stack,
goal/episode, actor location, validity, pause/reset/removal state, and the item's
location, contents, provenance, owner, thrower and pickup settings. A cancelled
or changed operation removes its own provisional entity without consuming the
new contents of a changed pocket. Unlike pickup of a pre-existing world item,
this is cleanup of an uncommitted newly created drop, not restoration of a
listener's earlier inventory. On success exactly one item is consumed, normal
mob pickup is enabled and the existing 20-tick pickup delay is applied.

A listener cannot transform this operation into a different item, quantity or
ownership assignment: such changes reject the drop. Unrelated pocket slots may
change without invalidating it. Cleanup of an item moved to another Folia region
is scheduled on that item's owner rather than accessing its mutable state from
the caller's region. This is bounded synchronous-event handling, not a security
sandbox for arbitrary plugins, a native-player drop implementation, or an atomic
world/pocket save protocol.

## World edits must still describe the current world

`EntityChangeBlockEvent` is a callback boundary, not a reservation. Mining and
placement retain the original block data, actor location/rotation, goal, selected
slot and held stack. After listeners return, the runtime checks these values,
entity/region ownership, removal/reset/pause state, closed-menu state and current
edit permission before applying the change. A listener's replacement block or
inventory is not overwritten with the old decision, and rejected edits do not
consume a placement item, spawn mining drops or earn completion credit.

Placement also checks the target block's unit cube for living bodies, both before
and after the callback. Other NPCs, players and living mobs cannot be enclosed by
that placement; spectator players are excluded. An unavailable neighboring region
causes refusal, not cross-thread access or a forced chunk load. This is a mechanical
collision check, not a learned decision about where a settlement should build.

The simplified mining path does not spill a chest/furnace's stored items. Therefore
an NPC must empty that local container before mining it. The runtime checks the
local inventory snapshot before accumulating mining progress and again after the
callback, including items the core pocket cannot represent. Empty containers are
still mineable. This deliberately conservative rule prevents silent stock loss;
it does not implement vanilla container spilling or general block-entity component
preservation. Loot tables, richer block components and post-edit physics remain
separate mechanics to validate. These checks are not a crash-atomic transaction.

## Harvest provenance precedes the spawn callback

Harvest is not a held-item drop: the block has already been removed when its
items are spawned. The adapter captures the original episode token and sets it,
along with the usual zero pickup delay, before `ItemSpawnEvent`. A callback that
advances the actor's goal cannot cause this old harvest to acquire the new
episode's token. The runtime does not overwrite the listener's provenance,
delay, contents, pickup permission or ownership changes after spawning.

Spawn cancellation is respected without recreating the yield or restoring the
source block. This does not provide arbitrary-plugin isolation, crash-atomic
saves, or a proof that neural actors can mine. The
[harvest callback record](verification/20260928-harvest-spawn-boundary.md)
reproduces the old episode misattribution and tests both named server versions.

## A continuous two-body mechanical chain

The opt-in real-server fixtures now include two actual NPC bodies, initialized
with complementary supplies: three planks in one pocket and two sticks in the
other. The first actor's incomplete recipe cannot produce a tool. The second
actor deposits its sticks in a real chest; the first withdraws them, finishes a
wooden pickaxe through literal grid clicks, and deposits the tool. The second
withdraws that tool, mines a real stone block, picks up its drop and deposits the
cobblestone in the shared chest.

There are no mid-chain inventory resets, teleports or injected resources. The
driver issues one actuator call per actor per actual scheduled server tick.
Action selection, timing and aiming are scripted, and the reset supplies, nearby
stations and stone are provided by the fixture. This is an actuator integration
check, not a neural observation/inference test or a learned cooperation trial.
Transfers must not duplicate stock or invent crafting, pickup or smelting credit.
All scripted code stays under `tests/live` and out of both public runtime JARs.

## Validation and remaining gates

`./test.sh` includes two-pocket conservation tests, stale actions and inaccessible
slots. Opt-in `tests/acceptance.py fixtures` executes the real chest adapter,
item conversions, drop/spawn/pickup/block callbacks, native pickup restrictions,
occupied placement and nonempty storage
inside a disposable server, then completes the two-body resource chain and all
18 scripted mechanics fixtures. Test scripts are never included in the public
inference or training JARs.

The [inventory record](verification/20260928-shared-resources.md),
[world-edit/continuous-chain record](verification/20260928-cooperative-world-actions.md), and
[drop conservation record](verification/20260928-drop-conservation.md)
distinguish fake-inventory checks, actual server mechanics and learned behavior. These tests
do not establish that neural policies choose useful transfers. The next genuine
cooperation experiment still needs a common resource objective, continuous
inventories, completion-based measurements and matched non-cooperative controls,
as described in [the development order](COOPERATIVE_SURVIVAL.md).
