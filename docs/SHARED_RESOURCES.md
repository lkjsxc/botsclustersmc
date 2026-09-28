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
item contents, provenance, permission and proximity again. Cancellation, removal,
replacement or other relevant changes abort that pickup without inserting the
old snapshot into the pocket or overwriting the callback's result.

This protects the supported synchronous owner-thread operation. It is not a
database transaction, a guarantee against plugins violating server-thread rules,
or crash-safe persistence across world and NPC saves.

## Validation and remaining gates

`./test.sh` includes two-pocket conservation tests, stale actions and inaccessible
slots. Opt-in `tests/acceptance.py fixtures` executes the real chest adapter,
item conversions and pickup callbacks inside a disposable server, then completes
all 18 scripted mechanics fixtures. Test scripts are never included in the public
inference or training JARs.

The [verification record](verification/20260928-shared-resources.md) distinguishes
fake-inventory checks, actual server mechanics and learned behavior. These tests
do not establish that neural policies choose useful transfers. The next genuine
cooperation experiment still needs a common resource objective, continuous
inventories, completion-based measurements and matched non-cooperative controls,
as described in [the development order](COOPERATIVE_SURVIVAL.md).
