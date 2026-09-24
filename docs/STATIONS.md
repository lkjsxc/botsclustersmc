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

For pickaxes, easier practice leaves a prefix of `ceil(5 * difficulty)` ingredient
cells empty and prepositions the remaining raw suffix. This creates a progression
from a small number of missing cells to the unchanged full empty-grid problem.
The policy still chooses every in-episode click; no action sequence is replayed.
Full probes/exams never use this preparation. Other small recipes retain random
raw-subset preparation, now using their actual active-menu grid coordinates.

Experiments with an extra cumulative unfinished-work penalty were not adopted:
the stronger penalty degraded previously learned tasks in frozen-policy checks.
The retained method adds no such per-tick station penalty. Finite-run comparisons
and failed attempts belong in the verification record, not in a mastery claim.

The shared actuator, 512-value observation schema, action probabilities, V-trace
correction, exam gates and terminal item requirements are unchanged. No pathfinder,
auto-aim, recipe macro, action demonstration or hidden fallback is added. Existing
valid checkpoints retain their actual weights, Adam state and course history.

## Acquisition before assembly

Below practice difficulty 0.55, workstation lessons explicitly train only opening
the actual target station. These lessons always reset with a closed menu and
unprepared raw stock; the policy must perform the opening itself. The spectator
HUD labels them `practice: open target station`, and separate trial/success
counters prevent confusing them with finished recipes. Harder practice requires
the complete item outcome and uses the staged raw-cell preparation above.

This deliberately changes practice completion, not certification: full probes and
frozen exams ALWAYS require the original crafted item, smelted ingot or chest
contents, and never complete merely because a station opened. Existing readiness
and promotion thresholds are unchanged. Practice averages now include short
acquisition lessons and must not be reported as full recipe success rates.

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
