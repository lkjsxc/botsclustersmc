# Tool-use diagnostics: distinguish inputs, handoff and physical outcomes

This is a read-only extension to the independent frozen-policy exam for task 12
(`mine-cobblestone`). It is not a training intervention or a demonstrated mining
improvement. The observer is in `tests/holdout`, outside both delivered plugin
JARs. Its native report validator is in `host`; production requires no Python.

## What is measured

`diagnostics.tool_use` describes each **pre-action observation used for inference**
and the next decision-boundary observation. The observer re-evaluates the same
immutable policy and verifies the applied action's joint log likelihood to
`1e-6`. It does not draw random numbers, choose actions, change masks, edit the
pocket/world, or update weights. An inference wait can separate the source
observation from physical action application; the trace is not an exact
application-time inventory snapshot or a per-tick event log.

The four states, in array order, are:

| Index | Menu | Selected hotbar item |
| --- | --- | --- |
| 0 | Closed | Not a wooden/stone pickaxe |
| 1 | Closed | Wooden/stone pickaxe |
| 2 | Open | Not a wooden/stone pickaxe |
| 3 | Open | Wooden/stone pickaxe |

`state_visits` counts pre-action states. `state_transitions` is a flattened 4x4
matrix, indexed `4 * before + after`. `initial_state` and `final_state` bound the
single trial. A row sum must equal its visit count; outgoing minus incoming
counts must match the trial endpoints. Recorded states must be reachable from
the initial state in this observed transition graph. These checks reject a
plausible-looking histogram made of disconnected trajectories. They do not
reconstruct the order of all visits from aggregate counts.

`visible_pick_location_states` has five entries: any hotbar pick, any reserve
storage pick, cursor pick, visible carried-grid pick, and no pick visible in
these locations. The first four entries may overlap. Preview outputs and chest/
furnace contents are not carried tools. A closed screen hides its retained grid:
**no visible pick is not proof of losing the pickaxe**. The diagnostic intentionally
does not inspect hidden grid contents unavailable in the policy observation.

## Exact one-decision input probabilities

For closed-menu observations, `closed_cascade_probability_sums` and
`closed_cascade_selections` count the following nested sets, in order:

1. Leave the inventory closed: GUI operation `none`.
2. Additionally select the world `dig` input.
3. Additionally select a hotbar slot that contains a pick in the source observation.
4. Additionally request no movement or turning and no jump: move 0, yaw 2,
   pitch 2, and pose normal/crouch.

For example, the third probability is
`P(gui=none) * P(interaction=dig) * sum(P(hotbar=s), s containing a pick)`.
The fourth also multiplies the independent movement, yaw, pitch and non-jump
head probabilities. This is exact for the current factored one-decision action
distribution; the tests independently enumerate all seven parent heads to check
it. A future joint/conditional world-action architecture must revise this
observer rather than silently reuse the product formula.

These sums are **not** the probability of hitting the target, staying still,
maintaining contact, breaking a block, or completing a multi-step sequence.
Quiet inputs can be selected while facing away, falling or sliding. A tool can
change between observation and application. Do not raise an average one-step
probability to a power to estimate a full mining sequence: future observations
and action distributions change. The sets deliberately omit a goal-conditioned
"correct action" mask, scripted aiming and automatic equipment.

All four closed probability sums use the same denominator:
`state_visits[0] + state_visits[1]`. `closed_hotbar_pick_states` separately counts
closed observations in which tool selection was available. Zero opportunity
means no conditional measurement, not zero learned ability. The ratio of two
nested sums describes the observed mixture of states, not a causal intervention.

For open-menu observations, `open_gui_probability_sums` and
`open_gui_selections` use the existing six GUI operation indices. Their sums must
match `state_visits[2] + state_visits[3]`; the already-open `open` operation has
zero support. `open_selected_pick_close_probability_sum` and its selection count
use only state 3. Divide by `state_visits[3]` to describe closing opportunities
with a currently selected pick. Closing a screen does not apply the simultaneously
sampled hotbar head: existing menu focus owns that input. Nothing here closes the
screen or selects a tool on behalf of the actor.

## Relationship to existing diagnostics

Ordinary task completion and actual broken/collected counts remain authoritative.
The existing `diagnostics.harvest` reports sampled physical contact and effective
world-dig selections. Its held-item sample is from the next physical boundary;
`tool_use` uses the source observation. Do not equate their denominators or use
one to overwrite the other. The older `menu_observations` are also post-action
samples. Both views remain available rather than rewriting historical evidence.

The native evaluator derives `tasks[].tool_use` only from validated per-trial
traces. Full coverage yields `state=recorded` and summed metrics. Missing historical
observations yield `not-recorded` or `partial`, without invented zero totals.
An aggregate already present in a report must exactly equal the independently
recomputed aggregate. Malformed present traces always fail validation. The current
Python developer runner requires the new trace in every current task-12 trial;
it rejects this field on other tasks. Both validators remain effective without
language assertion flags, including optimized Python interpreters.

## Verification boundary and next experiment

The unit tests establish measurement arithmetic, field integrity and nonmutation,
not Minecraft performance. The observer does extra network evaluation on the
owning thread, following the existing crafting diagnostic design. Wall-clock
cost and physical repeatability still require a bounded real frozen-exam gate.
No inference/training plugin receives this observer.

Before adopting this extension, qualify an unchanged immutable policy and the
ordinary ordered case bank with and without the observer in isolated exams,
retaining complete reports, physical outcomes, identities and timeout evidence.
Declare seeds, case counts and the allowed repeatability differences before
execution. Use a fresh seed for confirmation. A live, changing policy and two
historically selected policies are not matched learning trajectories. No new
learning or causal retention claim belongs in this observer qualification.

A subsequent learning hypothesis should depend on the measured bottleneck. If
useful world input has appreciable probability but is repeatedly abandoned,
consider a **policy-selected bounded primitive hold duration**: the neural policy,
not a tool-aware controller, chooses both the primitive input and how long to
hold it. This would require explicit duration likelihoods, elapsed-tick returns,
discontinuity handling and matched independent learning replicas with crafting
retention gates. It is a proposed experiment, not implemented code or a promise
that temporal abstraction solves mining. If the actual failure is inaccessible
tool stock or inability to leave the menu, this hypothesis may be inappropriate.
