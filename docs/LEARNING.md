# Learning contract

## Observations and primitive actions

A shared immutable 384→64→64 tanh MLP has35,690 parameters and eight categorical
heads: movement9, yaw5, pitch5, posture3, interaction4, hotbar9, GUI operation6 and
GUI slot64. The output also includes a scalar critic. GUI slot probability is
active only for click operations; sampling, likelihood, entropy and gradients
use the same conditional distribution. Masks expose task-wide availability and
mechanical slot legality, not the correct target, direction, tool or recipe.

The numeric state includes local blocks, body velocity/orientation, pocket and
menu contents, selected input, goal displacement, task, remaining finite horizon
and selected environment progress. This is privileged state-based RL, not pixels
or raw client inputs. Four actual body ticks are the nominal decision interval;
queue/region delays extend a held action and are recorded, not relabelled as four
ticks. Positions are authoritative Minecraft positions, not a lightweight simulator.

## Asynchronous actor-learner updates

Actors publish consecutive episode fragments of at most32 transitions, with
behavior policy version and log probability, elapsed ticks, genuine terminals
and the final next observation. A fragment never crosses a reset/episode boundary.
Learning starts on available bounded work instead of waiting for all actors or
all episodes to terminate. Currently available trajectories form updates of up
to512 samples; fixed gradient workers parallelize useful computation.

The learner computes V-trace targets with importance ratios clipped at1 for both
rho and trace continuation. Discount is `0.997 ** (elapsed_ticks / 4)`, zero at
real finite-horizon terminals. Fragment truncation bootstraps its actual next state
but does not invent a terminal. Actor loss uses corrected next targets, entropy
coefficient0.002, a Huber critic and Adam base rate0.0003. Global gradient norm is
clipped at0.5. Model/optimizer publication is one checked immutable transaction;
NaN/Inf never produces a new published version.

This is an IMPALA-inspired implementation of V-trace, not PPO or a reproduction
of the whole IMPALA training system. It deliberately processes bounded stale
experience with correction. Configured policy lag bounds reject excess-age data
with explicit counters; never describe it as strictly on-policy or silently claim
all offered experience was trained. Increasing the population also raises the
explicit lag budget, rather than coupling admission to one global barrier.

Primary source: Espeholt et al., *IMPALA: Scalable Distributed Deep-RL with
Importance Weighted Actor-Learner Architectures*, ICML2018:
https://proceedings.mlr.press/v80/espeholt18a.html

## Independent courses, frozen individual exams

Each NPC owns stage, task statistics, random state and completed certificates.
Other actors keep learning while one actor evaluates a fixed immutable snapshot.
No actor's failure can be bypassed by a population-average score, but one weak
actor also cannot stall the entire population. This avoids an all-thousands-must-
pass-at-once probability bottleneck.

Practice difficulty follows that actor/task's outcome moving average. Roughly20%
of practice choices revisit an older skill; every fifth selection is a
full-difficulty probe. **Probes still train**; they are a readiness heuristic,
not an independent held-out evaluation. Easy practice may preposition a conserved
subset of raw ingredients/cursor state at reset. Full probes and exams use the
full initial raw stock, closed menus and no recipe/menu assistance. No reset
provides crafted outputs or chooses an in-episode neural action.

Readiness requires at least40 current-task practice episodes, eight full probes,
full-probe success EMA≥0.70, at least20 practice episodes since the last exam and
four new probes. A completed-course actor uses a longer256-episode interval.
A ready actor pins the currently published policy and tests16 current-task cases
plus four cases for EACH previous task. It must pass14/16 and3/4 respectively.
Only that actor advances on success. Exam transitions never enter the learner.
The frozen model remains fixed while other actors update the shared live model.

Completed certificates name the policy version actually examined. They do not
certify the latest continually changing shared policy forever. Full-difficulty
review failures can demote an actor to a forgotten earlier task. Population
completion means individual historical passes, not proof that one exported latest
model simultaneously passes every task for every actor. Export itself is never a
mastery gate. Long-run held-out retention/generalization remains a separate study.

Pause/restart abandons the affected actor's whole unfinished exam rather than
keeping a favorable partial subset. Completed certificates/statistics and RNG
persist. In-flight world actions are not replayed. Shutdown records buffered
untrained samples and unfinished actions; accepted learner work is drained before
final checkpoint when graceful shutdown succeeds. The single canonical file is
`training.bcmc`; stopped-state export derives a policy directly from that file
instead of depending on a separately committed policy copy. This is not an atomic
transaction with the Minecraft world, and export is not a learned-skill claim.

## Real-server task catalogue

| ID | Task | Reset resources and required outcome |
| --- | --- | --- |
|0|Forward-stop|Reach and settle near a forward goal|
|1|Turn-stop|Rotate, reach and settle|
|2|Aim-hold|Hold target yaw and pitch|
|3|Navigate-stop|Reach a random planar goal and settle|
|4|Step-over|Cross a one-block obstacle and settle|
|5|Break-log|Remove the designated log|
|6|Collect-log|Break the log and acquire its own session-tagged drop|
|7|Place-block|Use provided planks to occupy one target cell|
|8|Craft-planks|Use a raw log; acquire four crafted planks|
|9|Craft-sticks|Use two planks; acquire four sticks|
|10|Craft-workbench|Use four planks; acquire a table|
|11|Craft-wood-pick|Furnished table, three planks and two sticks|
|12|Mine-cobblestone|Furnished wooden pick; acquire the mined stone's drop|
|13|Craft-stone-pick|Furnished table, three cobblestone and two sticks|
|14|Smelt-iron|Furnished furnace, raw iron and coal; extract iron after native cooking|
|15|Supply-chest|Provided logs; increase actual contents of the furnished chest|
|16|Build-platform|Provided planks; occupy all three designated cells|
|17|Log-to-workbench|Start with a log block; break, collect and craft a table|

Invulnerability, enclosed illuminated rooms, supplied goals/resources/stations,
reset teleportation and shaped reward are explicit environment assistance.
Supplying a pick does not teach how to obtain it. A chest task is not cooperation.
Normal motion still comes from the policy. Arrival requires sustained physical
settling, not entering a radius at full speed. Rewards use actual state changes
and session-scoped drop provenance; old/foreign pickups are not new achievements.

Primitive pocket crafting supports a bounded catalogue, not every vanilla recipe.
The finite NPC actuator differs from a survival player (no hunger/durability or
complete combat). Disabled mob awareness also disables some native autonomous
behaviors; never present this as unmodified player physics or full Minecraft RL.

## Verification categories

Numerical gradient tests, synthetic bandit improvement, real learner updates,
scripted task reachability, learned task mastery, retention, generalization and
multiplayer cooperation are different claims. The diagnostic driver is a separate
plugin, uses explicitly scripted primitive inputs, and cannot generate normal
training checkpoints. Its18 passes show that the implemented tasks are reachable;
they are not demonstrations, training data or learned skill certificates.
