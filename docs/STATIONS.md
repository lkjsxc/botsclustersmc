# Learning to use the actual workstation

A full-condition crafting trial begins with a closed menu, raw inventory and a
random facing direction. Success in an already-open, partly supplied practice
menu does not show that a policy can find and open the workstation itself.
The station curriculum makes that prerequisite measurable without supplying
in-episode actions or relaxing any independent exam.

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

## Interleaved opening and operation

At every assisted practice difficulty below 1, a lesson-seeded draw assigns about
one quarter of workstation lessons to opening and three quarters to operation.
Opening lessons always start closed with unprepared raw stock; the policy must
open the actual target station. Operation lessons start with that station open
and require the original complete item outcome. Pickaxe operation starts also
use the raw-cell preparation above. Furnace and chest operation practice use the
same phase split, without crafting-grid preparation. Practice at difficulty 1,
full probes and frozen exams all start closed and require the full outcome.

The previous composition was defective: below 0.55 every station practice ended
at opening; operation only became eligible at difficulties requiring at least
three missing pickaxe cells. Its intended collection-only and one-/two-cell
practice was therefore absent. Moreover, opening successes raised the same EMA
used for assembly difficulty. More training alone could not fill that reset gap.

Opening outcomes now remain in actual episode/success counters but do **not**
update the completion EMA used to select difficulty. Completed operation practice,
ordinary nonstation practice and full probes still update it. Readiness, probe
cadence, frozen exam thresholds and promotion requirements are unchanged. Opening
alone cannot supply the full-probe results required to begin an exam.

Existing valid checkpoints retain weights, Adam, RNG, course history and existing
EMA values. On resumption, new EMA updates exclude opening; old EMA contributions
are not retrospectively reclassified or silently reset. The new process-local
crafting buckets start at zero. A schema-compatible resume is not evidence that
the modified curriculum already improves a learned policy.

## See the separate outcomes

The HUD labels opening as `practice: open target station` and operation as
`practice: use open station`. The observatory reports finished assisted crafting
attempts and successes by task and the number of recipe cells missing **at
reset**, alongside a separate station-opening counter. Zero missing means output
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
