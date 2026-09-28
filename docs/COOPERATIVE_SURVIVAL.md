# Cooperative survival: the target and the next gates

This is an engineering direction, not an implemented feature list or a promise
that the current policy can survive. The target is a group that maintains a
shared settlement through learned decisions, including obtaining and sharing
resources, replacing tools, meeting basic needs and recovering from disruption.

## What exists, and what does not

The current system trains 18 individually specified tasks in disposable rooms.
Task goals and target coordinates are supplied externally. The policy chooses
primitive movements, looks, world interactions and inventory clicks. Its bodies
are server-side Villager NPCs, not logged-in vanilla players.

Independent frozen-policy evaluations demonstrate movement, log harvesting and
some crafting in these rooms. The [latest declared comparison](verification/20260928-conditional-menu-study.md)
still records zero cobblestone successes and incomplete wooden-pickaxe reliability.
Historical course certificates do not certify a changing live policy.

The inference plugin has no autonomous goal selector, durable NPC/pocket database,
hunger, tool durability or complete combat. Local entity observations and chest
clicks make later cooperation experiments possible; they do not constitute
learned cooperation. Increasing NPC count cannot fill these gaps.

The [shared-resource boundary](SHARED_RESOURCES.md) now checks stale container
operations and preserves items that the simplified pocket cannot represent.
Two-pocket conservation tests and real Folia/Paper adapter tests establish
mechanical prerequisites only; they do not pass the cooperative-policy gate below.

## Development order

### 1. Reliable reusable skills, with measured retention

Resolve the tool/menu/world-interaction bottleneck without teacher actions or a
goal-specific mask that simply supplies the answer. Measure actual target breaks
and provenance-correct pickups, not dig selections or any-item pickup.

A useful learning change must improve complete frozen-policy results under an
explicit sample budget while retaining earlier skills, including crafting.
A stopped/resumed control is necessary when restart behavior is part of an
experiment. Representation changes must account for both physical actions and
previous-action observations; equal network dimensions or one-state marginals
are not enough to establish compatibility.

The earlier cold expert bank failed its next-task gate; simply separating
networks did not establish useful transfer. Shared-policy continuation has also
shown retention failures. A future modular study should preserve the
already learned function at initialization and measure both transfer and
interference. Neither reusing an old failed candidate nor zeroing an entire
learner is evidence that either problem has been solved.

### 2. Multi-step work without inventory or world resets

Teach and test the complete resource chain with one continuous inventory:
obtain logs, make planks and sticks, make and place a workbench, make and retain
a pickaxe, mine stone, and bring usable materials back. No supplied intermediate
products, per-step teleportation, automatic tool selection or recipe macro may
silently complete the chain.

Task 17 already names a smaller log-to-workbench chain, but its existence is not
a learned result. The long-term curriculum should follow prerequisites, not
treat the current enum order as a permanent scientific design. Useful reachable
composition need not wait behind an unrelated later skill. Changing allocation
still requires a separately owned experiment and retention evidence.

### 3. The smallest genuine cooperative task

Start with two NPCs and one shared resource objective in a bounded world. Give
them complementary observable initial resources and access to the same chest.
Keep episode budgets and total resources explicit. Every subsequent move,
deposit, withdrawal and craft is selected by a neural policy.

Do not assign permanent gatherer/crafter roles through a script and call the
result emergent teamwork. The first experiment may expose a common team goal;
autonomous goal selection is a later, separately tested capability.

Measure team completion, each actor's usable inventory, net useful shared stock,
resource losses and time to completion. Repeated deposit/withdrawal cycles and
one NPC taking credit for another's previous item must not farm reward. Record
item transfers as mechanics, not as an independent success signal.

Compare against controls with the same total resources and interaction budget,
including isolated agents. Withheld transfers or removal of a partner can test
dependence, but should not replace the ordinary completion test. Use new layout
and policy-sampling seeds, with every trial retained. Passing this task would
show bounded cooperation, not a self-sufficient settlement.

### 4. Self-maintenance and recoverable lives

Add the missing survival mechanics deliberately: food/energy, tool wear,
injury/death and appropriate combat or avoidance. Distinguish intentionally
simplified NPC mechanics from vanilla player behavior. Training assistance must
be declared and absent from the claimed unassisted test.

Persist bodies, pockets and goals with explicit restart and crash semantics.
Preserving a policy is not preserving a life. Inventory persistence alone is
insufficient: transfers between an NPC and a world chest must not duplicate or
silently destroy resources across save boundaries. Test failure cases and bounded
recovery before entrusting valuable worlds to the plugin.

### 5. An enduring shared world and learned goal selection

Only then expand to a group living through multiple day/night cycles in unseen
worlds. The policy or a learned higher-level controller must decide what to work
on from observable local and shared state; an external sequence of target
coordinates is not autonomous settlement planning.

Measure survival duration and population, usable food/tool/material stocks,
resource consumption versus replenishment, completed useful structures, and
recovery after losing an actor or tool. Count dependence on reset supplies and
operator interventions explicitly. Evaluate with frozen weights and no external
rescue, then separately investigate learning during life.

## Boundaries that remain in force

No language-model gameplay driver, pathfinder, teacher-action sequence or
autocrafting fallback substitutes for learned behavior. Keep deployment free of
the learner and curriculum. Do not require broadcasting complete chunks to each
NPC: the current bounded local observations remain the starting point; new
communication must have a measured information and compute budget.

World edits remain opt-in. Preserve the operator's worlds, failed studies and
the last measured-good training state. The acceptance measure is useful learned
behavior per controlled experiment, not CPU saturation, historical badges,
actor count, attractive telemetry, or the number of merged changes.
