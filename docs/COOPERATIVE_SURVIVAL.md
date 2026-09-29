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
some crafting in these rooms. The [latest parameter diagnosis](verification/20260929-retention-attribution.md)
completed 6,912 frozen trials and localised workbench-retention sensitivity to the
shared hidden representation in one preserved parent/control pair. It did not
train or adopt a new policy; ordinary wooden-pickaxe success remained zero.
The later [protected-frontier pilot](verification/20260929-protected-frontier-study.md)
implemented an isolated warm-copy candidate but stopped after 1,536 baseline
trials: the source and protected initial model both missed the declared aim-hold
floor on one seed. No experimental continuation ran, and the candidate runtime
was not integrated. This is an initial-qualification failure, not evidence of
forgetting caused by its learning.
The separate [relative-continuation study](verification/20260929-protected-continuation.md)
then completed 18,432 ordinary frozen trials and about 1.5 million additional
accepted task-11 samples per arm. Protection retained source workbench scores
of 119/128 and 117/128, while the unprotected final control scored 37/128 and
39/128. Both arms still had zero ordinary pickaxe completions. This establishes
bounded observed retention, not new-skill acquisition. PR #36 is closed without
merging its fixed-frontier runtime; it is not accepted for deployment.
The preceding [continuation screen](verification/20260929-placement-practice-study.md)
rejected a placement-practice candidate after early workbench-retention failures.
The [earlier stone comparison](verification/20260928-conditional-menu-study.md)
reported zero cobblestone successes; stone was not retested in the latest screen.
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

The latest resumed placement study copied one exact checkpoint into two separate
512-actor Academies. After roughly 253,000 additional samples per arm, workbench
crafting fell from 31/32 on each seed to 27/32 and 23/32 in the unchanged control,
and 22/32 and 29/32 in the candidate. Tasks 0–9 stayed at least 30/32. The declared
early gate stopped the study before its larger continuation and final placement
comparisons; this is not an accepted reset mixture or demonstrated pickaxe gain.
Prioritize retaining learned crafting during next-skill continuation over adding
more exposure to the diagnosed placement bottleneck. Both arms initially issued
all 512 actors frontier task 11; that observation alone does not establish the
cause of the retention loss.

The subsequent parameter diagnosis used the exact saved parent and unchanged
control, without new training. On two new seeds the parent scored 28/32 and 31/32
on workbench, versus the exact control's 23/32 and 22/32. Replacing only the shared
trunk reduced success in all four goal/output backgrounds: by 5-7 successes on
one seed and 9-13 on the other. Goal-column and actor-head changes did not show
that repeated material loss. The evidence localises sensitivity to the shared
representation in this model pair; it does not identify the responsible training
examples, loss terms, Adam momentum or startup schedule. In particular, an
unchanged actor output head does not protect it from changes in its input features.

The next learning gate is therefore **function preservation during training**,
not only at initialization: keep the previously learned behavior protected while
adding learnable capacity for the new skill and reusing prior features. Include
critic-to-trunk updates in this boundary; a separate final value output does not
isolate its training gradients. The subsequent one-frontier prototype implemented
this boundary with a shared immutable source policy and a warm trainable copy.
Its source checks established function preservation through synthetic actor and
critic updates, but that original pilot stopped before real-server training at
its declared initial qualification gate. PR #35 is closed without merging.
The subsequent, separately declared PR #36 relative study did perform continuation:
its protected behavior passed all initial, early and final retention screens,
while the active copy changed. Ordinary pickaxe completion remained zero on both
seeds in both arms, failing acquisition. Preserve this distinction rather than
calling protection untested or calling the learning system successful. Freezing
the whole learner cannot demonstrate new learning; reinitializing it discards
the transfer we need. Any modular representation must also bound snapshot memory
and concurrent inference costs, rather than multiply every immutable snapshot
by a full bank of experts. Evaluate ordinary new-skill completion and every
retained skill under a declared sample budget. Neither a selected hybrid nor
relaxed retention thresholds substitutes for that experiment.

Separate three prospective questions: does the input meet an absolute competence
requirement, does continued learning preserve its measured behavior, and does the
new skill improve? The protected pilot's original and protected initial models
both scored 29/32 on aim-hold under one fresh seed, below its declared 30/32 floor.
Do not treat that as forgetting, change the stopped pilot's rule, or substitute
a favorable seed. The subsequent relative study explicitly used that imperfect
input, 128 cases per task and two new seeds, with all rules published before training. It did not
retroactively change the stopped pilot. Exact function preservation also does
not guarantee success under changed world/partner-state distributions; ordinary
rollouts remain necessary. Keep validated bounded retention separate from a
claim of new-skill acquisition.

The next bottleneck is now acquiring and composing primitive material-selection
and slot-placement behavior while maintaining that protection. In the final
protected ordinary reports, only two cases per seed reached two correct recipe
cells; none reached three or a target preview. These are post-hoc observations,
not randomized attribution of the failure. Test representation/exploration
hypotheses with matched controls and prospectively bounded ordinary completion
criteria, not longer identical continuation, more actors, or an answer-supplying
recipe mask. A reusable multi-task learning path is still required before a
fixed-frontier protection mechanism becomes a deployable architecture.

The [goal-column transfer screen](verification/20260928-goal-transfer-study.md)
preserves non-recipient goal functions and changes only 96 parameters. Copying
collect-log's goal representation increased observed pick/stone contact from
1/32 to 29/32 trials, but both arms still completed zero stone tasks. It is a
rejected zero-training initialization, not an accepted mining controller. Future
transfer-preserving learning must test sustained tool/contact control with the
recipient's real action affordances; importing an easier lesson's restrictive
mask would not establish that capability. The new offline tool is diagnostic-only
and is absent from both runtime JARs.

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
