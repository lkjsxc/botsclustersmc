# Learning contract

[Aiming practice and unchanged full-condition exams](AIMING.md).
[Resource practice and real-contact diagnostics](HARVESTING.md).

## Observations and primitive actions

A shared immutable 512→96→96 tanh MLP has 68,842 parameters and eight categorical
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

### Local context and motor controllability

The current schema is `bcmc-citizen-egocentric-context`. Features 334–345 add
body-relative target coordinates and bearing, inverse target distance, normalized
forward/lateral velocity, continuous physical stillness and water/lava state.
Features 384–447 describe at most eight nearby owned-region entities; 448–511
contain eight coarse, at-most-twelve-block radial probes with visibility, material,
collision, liquid, hazard, floor and ledge information. Fields 382–383 report
context availability. Spectator players are omitted. No scan loads a chunk.
This is local privileged state; entities can be sensed without visual line-of-sight.

The old curriculum-difficulty input is zeroed: deployed policies should not
condition motor control on a training-only difficulty knob. Forward-stop permits
stop, forward and backward throughout every episode, allowing recovery from
overshoot without a hidden actuator choosing a direction. The arrival potential
includes bounded settling progress as well as distance. Physical stillness is an
observation, not an instruction to stop. Exam eligibility and pass counts have
not been relaxed. Crafting success uses exact required produced-item counts;
partial recipe progress does not silently raise the terminal threshold.

## Asynchronous actor-learner updates

Actors publish consecutive episode fragments of at most32 transitions, with
behavior policy version and log probability, elapsed ticks, genuine terminals
and the final next observation. A fragment never crosses a reset/episode boundary.
Learning does not wait for every actor or episode. The learner alone groups
fragments for up to 100 milliseconds or until the 512-sample target is reached;
the last whole fragment may cross that target by at most 31 samples. Entity tick
threads never wait for this batching window. Fixed gradient workers process the
resulting independent trajectory groups.

The learner computes V-trace targets with importance ratios clipped at1 for both
rho and trace continuation. Discount is `0.997 ** (elapsed_ticks / 4)`, zero at
real finite-horizon terminals. Fragment truncation bootstraps its actual next state
but does not invent a terminal. Actor loss uses corrected next targets, entropy
coefficient 0.002 and a Huber critic. A separate, weak head-wise uniform legal-
control prior (cross entropy coefficient 0.005) has a nonvanishing gradient when
a control becomes almost impossible. It is not a demonstration, task solution,
or per-state answer mask. The conditional gameplay likelihood is unchanged.

Adam starts at `0.00015 * sqrt(min(1, actual_batch_samples / 512))`; the global
gradient norm is clipped at 0.5. Before publication, up to 128 evenly spaced
observed states compare the old and candidate policies using the exact
conditional categorical KL. Mean KL must be at most 0.005 and maximum KL at
most 0.05. A rejected candidate halves the learning rate, up to twelve attempts;
all attempts start from the same original weights and Adam state. A fully
rejected batch is explicitly counted and is not added to trained samples.
This is a local, sampled update guard, not a proof of skill retention.
Model/optimizer publication is one checked immutable transaction; NaN/Inf never
produces a new published version. Status exposes the accepted rate, sample count,
KL values, backtracking and rejected samples.

This is an IMPALA-inspired implementation of V-trace, not PPO or a reproduction
of the whole IMPALA training system. It deliberately processes bounded stale
experience with correction. Configured policy lag bounds reject excess-age data
with explicit counters; never describe it as strictly on-policy or silently claim
all offered experience was trained. Increasing the population also raises the
explicit lag budget, rather than coupling admission to one global barrier.

Primary source: Espeholt et al., *IMPALA: Scalable Distributed Deep-RL with
Importance Weighted Actor-Learner Architectures*, ICML2018:
https://proceedings.mlr.press/v80/espeholt18a.html

## Loss allocation across tasks

The learner uses one capped task-loss allocation per complete batch, shared across
all gradient workers. Present tasks receive equal loss mass where possible, with
per-transition weights capped at eight and the total mass conserved. This does
not fabricate experience, include exam data, change V-trace targets or inflate
`trained_samples`. Weighted loss/entropy/importance statistics describe this new
objective. [Task balance](TASK_BALANCE.md) explains the bounds and exact tests.

The distinction matters because review is selected by episode while gradients
consume transitions: long failures can outnumber short successful reviews in the
learner. Episode promotion remains independent and unchanged. Balanced updates
are not themselves a guarantee that the latest model retains every earlier skill.

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
|0|Forward-stop|Reach and settle near a forward goal; backward recovery is available|
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
