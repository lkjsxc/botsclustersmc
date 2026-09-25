# Learning to use the actual workstation

A full-condition crafting trial begins with a closed menu, raw inventory and a
random facing direction. Success in an already-open, partly supplied practice
menu does not show that a policy can find and open the workstation itself.
Full probes and frozen exams measure that prerequisite together with completion,
without supplying in-episode actions or relaxing any independent exam.

## Correct grid coordinates

`InitialCrafting` now defines raw-ingredient layouts in physical grid coordinates,
then maps them to the active menu's actual slots. A pickaxe requires a 3x3 grid:
a 2x2 inventory, chest, furnace or closed menu receives zero pickaxe progress.
A result/preview slot is never counted as an ingredient cell. Small recipes use
the same valid translations in 2x2 and 3x3 menus as the mechanical recipe matcher.
Wrong occupied cells reduce progress; matching a partial row while leaving extra
items elsewhere cannot report a complete recipe.

This fixes a specific misleading reward: the old pickaxe progress code reused
slot numbers across menus. In the personal inventory, slot 40 is a recipe preview,
not the middle ingredient cell of a workbench. That preview could be mistaken for
a placed stick. Neither the policy input nor legal click masks receive a recipe
answer; this correction affects reset setup and training reward measurement only.

## Disclosed training assistance

Practice-only initial orientation uses the existing progressively widened aiming
distribution for workstation tasks. Full probes and frozen exams preserve their
original random pose and random stream. Reset assistance still only redistributes
the furnished raw stock into a menu or cursor; it never supplies a crafted item.

Before the required station is open, a bounded state potential measures orientation
and reach. Opening the correct target station contributes 0.3, and a correctly
assembled recipe contributes up to another 1.0. An unrelated personal inventory
cannot receive this workstation credit. The workstation contribution remains in [-0.4, 1.3].
The existing reward adds only `discount * next_potential - previous_potential`,
with zero terminal potential. The discounted sum telescopes to a fixed initial
term, so opening/closing or rearranging ingredients cannot increase total episode
return by cycling. This is shaping assistance, not a change to success criteria.

For pickaxes, operation practice samples a starting number of missing recipe cells.
Let `frontier = ceil(5 * difficulty)`. Half the starts use that frontier and half
sample uniformly from earlier counts `0..frontier-1` (a zero frontier stays zero).
The subset of missing cells is shuffled: any ingredient position can be practiced,
not only a fixed prefix. Zero missing supplies raw ingredients in the grid, **not
an owned product**. The policy must still choose a result-slot click to obtain it.
Thus output collection and one-cell completion remain reachable even after the
frontier advances. This is a reverse-start-state curriculum, not a teacher policy.

The policy chooses every in-episode click; no action sequence is replayed. Full
probes/exams never use this preparation. Other small recipes retain random
raw-subset preparation using their actual active-menu grid coordinates. A separate,
lesson-seeded pocket RNG makes reset diagnostics independent of pose RNG usage.
Full probes/exams consume no pocket-assistance RNG and retain their world/pose
stream, closed menu and original raw inventory.

Experiments with an extra cumulative unfinished-work penalty were not adopted:
the stronger penalty degraded previously learned tasks in frozen-policy checks.
The retained method adds no such per-tick station penalty. Finite-run comparisons
and failed attempts belong in the verification record, not in a mastery claim.

The shared actuator, 512-value observation schema, action probabilities, V-trace
correction, exam gates and terminal item requirements are unchanged. No pathfinder,
auto-aim, recipe macro, action demonstration or hidden fallback is added. Existing
valid checkpoints retain their actual weights, Adam state and course history.

## One completion goal, different starting states

Every workstation lesson now requires the same task outcome: obtaining the crafted
pickaxe, extracting the iron ingot, or supplying the target chest. At assisted
practice difficulty below 1, the correct station starts open. Pickaxe practice
also uses the raw-cell preparation above. Furnace and chest practice do not use
crafting-grid preparation. Difficulty-1 practice, full probes and frozen exams
retain their original closed menu, raw stock and pose. Full probes still provide
learning experience for opening and operating the station as one complete task.

The previous opening/operation split assigned an unobserved lesson-seeded goal:
about one quarter of assisted station lessons succeeded immediately on opening,
while the same task and observable state required item completion in other
lessons. The success branch changed the terminal flag and terminal reward even
though neither the actor nor the critic received that goal distinction. Excluding
opening wins from the completion EMA did not remove this inconsistent learning
signal. A fixed-state regression now checks success and potential across lesson
kinds, difficulties and seeds. This identifies a goal-contract defect, not proof
that it was the sole cause of failed wooden-pickaxe learning.

The correction removes the opening-only success branch rather than adding a
hidden option, a teacher action or another model input. Every new task-completion
outcome updates the difficulty EMA. Opening still contributes the existing bounded
potential, but never ends an episode successfully on its own. The potential,
terminal reward formula, raw-cell curriculum, probe cadence, frozen exam gates,
observations, inference and optimizer are otherwise unchanged.

Existing valid checkpoints retain weights, Adam, RNG, course history and existing
EMA values. Historical episode/success totals can include opening-only outcomes
from older training; they are not retrospectively reclassified or erased. New
process-local crafting buckets start at zero. Successful resume and contract tests
are not evidence of improved learned completion; that requires frozen full trials.

## See the separate outcomes

The HUD labels assisted operation as `practice: complete at open station`. The
observatory reports the `task-completion` station goal and finished assisted
crafting attempts and successes by task and the number of recipe cells missing
**at reset**. There is no opening-only success counter. Zero missing means output
collection is required, not that the task succeeded before an action. A five-cell
practice is still assisted by an initially open workbench; it is not a full probe.

These exact counters cover the current process only. They exclude probes, exams
and interrupted attempts. They include unsuccessful completed/time-limited
practice, not just wins. Empty buckets show `0 / 0`, never an invented success
percentage. Missing, malformed or inconsistent diagnostics display unavailable.
Historical trial counts, moving averages and frozen-policy results remain separate.

## Inspect what the policy is doing

`/bots inspect <id>` now displays the menu, cursor, recipe preview and actual
numbered menu cells from an immutable owner-thread snapshot. The spectator HUD
also shows an open menu and the carried cursor item. Viewing cannot click slots
or change the NPC pocket. This is a diagnostic mirror, not a player inventory.
Independent trial reports count observations in each of the five menu types, so
opening a personal inventory is distinguishable from opening the real workbench.

Mechanical/unit tests check grid dimensions, translations, raw-material
conservation, preview separation, immutable views, full-probe reset invariance
and bounded potentials and telescoping cycle invariance. Learned ability still requires fresh complete
trials of one frozen policy. Do not interpret successful assisted practice as
workstation mastery, open-world survival or cooperation.
