# Goal-independent menu affordances

An open menu already owns input focus. The shared sensor now additionally removes
menu choices that cannot change the current pocket or external inventory. This
is a mechanical input filter, not a recipe assistant or a new policy architecture.
The neural weights, input schema, primitive click semantics, rewards, resets,
course promotion thresholds and frozen exams are unchanged by this filter.

## What the filter knows

`Pocket.wouldChange` predicts the effect of a literal left, right or shift click.
It reads the actual cursor, stack contents, capacities, menu type, recipe preview
and container acceptance rules. It does not read a task, target item, preferred
ingredient, curriculum difficulty or correct destination. Checking it cannot
modify any inventory, crafting counter, external slot or random state.

With an empty cursor, empty-slot clicks disappear. Every occupied accessible
slot remains available when at least one operation can affect it. With a carried
item, every accepting destination remains available, including unrelated empty
storage slots. Even a recipe output irrelevant to the task remains selectable.
Full-inventory shift transfers and incompatible cursor extraction are excluded
only when they have no mechanical effect. Waiting and closing remain choices;
opening an already-open menu is removed. Passive world effects are not blocked.

The model shares one slot head across the three click operations. The slot mask
is their UNION; operation availability is the union across that operation's slots.
Thus some operation/slot combinations can still be ineffective. This is not an
operation-conditioned slot policy, and the distribution must not be described
as one. When no click can change anything, the inactive slot head has its canonical
zero dummy and only non-click operations remain possible.

The resulting mask is captured with each observation and retained in the actual
trajectory. Sampling, logged behavior likelihood, V-trace correction, gradients
and update checks all use that recorded mask. No behavior action or likelihood
is retroactively substituted. Training and deployment call the same sensor.

## Verification boundaries

Differential tests compare the prediction with actual primitive clicks across
closed, inventory, workbench, furnace and chest menus; empty/full slots; mixed
items; carried items; valid/invalid output extraction; and random reachable click
sequences. They also verify task independence, exact union masks, nonmutation
and finite conditional likelihoods. These are software tests, not learned skills.
The real-server fixtures check that the production sensor actually applies the
filter and still admit all 18 explicitly scripted task solutions.

At frozen policy 234045 (93,937,480 training samples), 32 full-difficulty trials
per task on seed 924603 measured the following before/after the filter:

| Task | Previous mask | Mechanical effect mask |
| --- | ---: | ---: |
| Forward-stop, turn-stop, navigation, step-over, break-log, collect-log, placement | 32/32 each | 32/32 each |
| Aim-hold | 23/32 | 23/32 |
| Craft planks | 31/32 | 32/32 |
| Craft sticks | 28/32 | 32/32 |
| Craft workbench | 0/32 | 1/32 |

The same weights were used; this comparison is not a claim of new learning or
statistical significance. Workbench creation is still unreliable. Full-condition
held-out trials and retention checks remain necessary after additional learning.
