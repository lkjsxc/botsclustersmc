# Cooperative survival: the target and the next gates

The target is a group that maintains a shared settlement through learned decisions:
obtaining and sharing resources, replacing tools, meeting basic needs and recovering
from disruption. This document is an engineering direction, not an implemented
feature list or a claim that the current policy can survive.

## Current decision

The immediate bottleneck is **acquiring and retaining useful behavior within a
skill**, not increasing the NPC population. Protecting other task IDs can preserve
old workbench behavior while useful partial pickaxe behavior is lost during updates
to task 11. A numerically correct, more expressive action head is not enough to
establish a learning benefit.

The [qualified click-slot comparison](verification/20260930-click-slots-comparison.md)
completed 48 reports and 7,424 frozen trial executions with one immutable source,
matched protected control and candidate, and all five missing-cell conditions.
After +257,051 control and +261,444 candidate samples, ordinary pickaxe completion
was zero in both arms on both seeds. Candidate top-left and top-center behavior
failed the declared early retention floors; other tasks remained protected.
The study stopped before its larger budget. Its runtime is not accepted for
deployment. This is an observed early rejection, not merely an untested prototype.

The current system trains 18 individually specified tasks in disposable rooms.
Task goals and target coordinates are supplied externally. A neural policy chooses
primitive movements, looks, world interactions and inventory clicks. Bodies are
server-side Villager NPCs, not logged-in vanilla players. Historical certificates
and counters from a changing live policy do not certify its current competence.

There is no autonomous goal selector, durable NPC/pocket database, hunger, tool
durability or complete combat. Local entity observations and chest clicks are
mechanical prerequisites, not learned cooperation. The
[shared-resource boundary](SHARED_RESOURCES.md) checks stale container operations
and preserves items the simplified pocket cannot represent; its conservation and
Folia/Paper adapter tests do not pass the cooperative-policy gate below.

## Evidence that determines the next decision

Keep the exact failed candidates and source states. Do not turn a stopped study
into a success by changing its thresholds, seeds, denominators or input afterward.

| Evidence | What it established; what it did not |
| --- | --- |
| [Goal-column transfer](verification/20260928-goal-transfer-study.md) | Changing 96 goal weights increased observed stone contact from 1/32 to 29/32, but both arms completed zero stone tasks. Contact is not mining acquisition. |
| [Conditional-menu screen](verification/20260928-conditional-menu-study.md) | The tested candidate did not establish ordinary cobblestone completion. This is not evidence that an answer-supplying mining mask would be acceptable. |
| [Placement continuation](verification/20260929-placement-practice-study.md) | Both candidate and unchanged continuation lost workbench performance at the early screen. The larger budget was not run; loss cannot be attributed solely to the new reset mixture. |
| [Parameter attribution](verification/20260929-retention-attribution.md) | In one preserved parent/control pair, shared-trunk replacement repeatedly reduced workbench success across goal/output backgrounds. This localises parameter sensitivity, not the responsible examples, loss terms or optimizer moments. |
| [Protected-frontier pilot](verification/20260929-protected-frontier-study.md) | The source and initial protected model missed an absolute baseline requirement. No continuation ran. This was not forgetting caused by candidate learning. |
| [Protected continuation](verification/20260929-protected-continuation.md) | About 1.5 million additional samples per arm: protection retained measured workbench behavior; the unprotected control did not. Both had zero ordinary pickaxe completions. |
| [Protected placement](verification/20260929-protected-placement.md) | With protection constant, more placement practice still gave zero ordinary pickaxe completions. Source assisted totals of 56/160 and 48/160 fell to candidate 0/160 and 1/160. Other-task protection did not protect all behavior within the active task. |
| [Click-slot implementation](verification/20260929-click-conditioned-slots.md) | Separate click-specific projections preserved the original initial actor, critic and optimizer state under independent numerical checks. PR #41 preserved an implementation checkpoint, not a Minecraft learning result. |
| [Qualified matched comparison](verification/20260930-click-slots-comparison.md) | Candidate partial totals fell from 51/160 and 50/160 to 17/160 and 20/160 after about 261,000 new samples. Top-left and top-center retention failed on both seeds; ordinary pickaxe success remained zero. All 48 baseline/early reports completed, then the declared early stop prevented further training. |

These are bounded observations under their recorded conditions. They are not a
ranking of all possible architectures, evidence that a larger model must work, or
a reason to discard reward-trained primitive control for a scripted recipe driver.

## Development order

### 1. Reliable reusable skills, with measured retention

A useful learning change must improve complete frozen-policy results under an
explicit budget while retaining earlier useful behavior. Count actual target
breaks, provenance-correct pickups, complete crafts and retained usable products,
not selected dig/click operations or any-item pickup.

Preserve the learned actor and critic functions at initialization, then measure
what learning changes. Protecting a final actor head does not protect its inputs
from trunk updates. A separate final value output does not isolate critic-to-trunk
gradients. A cold expert bank did not establish useful transfer; freezing the whole
learner cannot establish new learning. Warm modular capacity must reuse prior
features and bound snapshot memory and concurrent inference costs.

Task-ID protection is a research control, not a complete continual-learning
architecture. Within the active goal, measure material selection, each placement
position, preservation of already placed ingredients and output collection.
Do not pool improving substeps with regressing ones to manufacture retention.
Diagnostic reset assistance must be labelled and cannot enter the policy as a
routing label or a mask that supplies the missing answer.

The click-slot hypothesis removes a specific restriction: a shared slot projection
cannot reverse the ranking of two commonly legal slots between click types in the
same observation. It does not follow that the original policy lacked a physical
action, that conditioning alone controls forgetting, or that copied Adam moments
will yield identical optimization trajectories after the heads are untied.

Use the saved initial and stopped models to distinguish changed active-goal
representations from changed output projections before adding more mechanisms.
A protected-policy attribution tool must explicitly preserve and validate the
immutable anchor and the active routing boundary; the existing unfocused
counterfactual compositor must not be used as though it already supports that.
Any hybrid is diagnostic, not a trained replacement or a deployment candidate.
Parameter sensitivity alone still does not identify a gradient-level cause.

Separate input qualification, retention during learning and new-skill acquisition.
Absolute mastery and relative preservation answer different questions. Use matched
stopped/resumed controls when restart behavior is part of a study. Account for
physical controls, previous-action observations, optimizer initialization and the
actual source checkpoint when changing a representation.

Accepted transitions, completed episodes and frozen trials are different
denominators. A short segment with many concurrent actors may collect substantial
learning data while long episodes remain unfinished; a high success ratio among
only the completed episodes is not a mastery measurement. Record temporal and
context coverage rather than inferring it from the total sample counter.

Keep operational failures, declared early stops and full-budget results distinct.
Predeclare complete condition matrices and stopping rules; preserve partial logs
without presenting them as complete evidence. Engineering screening margins are
not confidence intervals, and multiple evaluation seeds are not independent
training replicates. Do not substitute longer identical continuation, more actors
or relaxed thresholds for a separately justified experiment.

Full-condition probes are not necessarily frozen evaluations: in the current
implementation probes also generate learning samples; exams do not. A reusable
multi-task path is required before fixed-frontier protection becomes a deployable
architecture.

### 2. Multi-step work without inventory or world resets

Teach and test one continuous resource chain: obtain logs, make planks and sticks,
make and place a workbench, make and retain a pickaxe, mine stone and bring usable
materials back. No supplied intermediate products, per-step teleportation,
automatic tool selection or recipe macro may silently complete the chain.

Task 17 already names a smaller log-to-workbench chain, but its existence is not
a learned result. Curriculum allocation should follow prerequisites rather than
treating enum order as a permanent design. Reachable composition or resource
sharing need not wait behind an unrelated later skill. Changing allocation still
requires an owned experiment, matched budgets and retention evidence.

### 3. The smallest genuine cooperative task

Start with two NPCs, one shared resource objective and a bounded world. Provide
complementary observable initial resources and access to the same chest. Every
subsequent move, deposit, withdrawal and craft is selected by a neural policy.
A common team goal is acceptable for this first experiment; autonomous goal
selection is a separate later capability.

Do not assign permanent gatherer/crafter roles through a script and call that
emergent teamwork. Measure team completion, each actor's usable inventory, net
useful shared stock, resource losses and time to completion. Repeated transfers
must not farm reward, and one actor must not receive fresh credit for another's
previous item. Item transfers are mechanics, not an independent success signal.

Compare with controls having the same total resources and interaction budget,
including isolated agents. Withholding transfers or removing a partner can test
dependence but cannot replace the ordinary completion test. Retain all trials on
new layout and sampling seeds. Success here would demonstrate bounded cooperation,
not a self-sufficient settlement.

### 4. Self-maintenance and recoverable lives

Add food/energy, tool wear, injury/death and appropriate combat or avoidance
deliberately. Distinguish simplified NPC mechanics from vanilla player behavior.
Declare training assistance and remove it from any claimed unassisted test.

Persist bodies, pockets and goals with explicit restart and crash semantics.
Preserving a policy is not preserving a life. NPC-to-chest transfers must not
duplicate or silently destroy resources across save boundaries. Test failure and
bounded recovery before entrusting valuable worlds to the plugin.

### 5. An enduring shared world and learned goal selection

Only then expand to multiple day/night cycles in unseen worlds. A policy or learned
higher-level controller must decide what to work on from observable local and
shared state. An external sequence of target coordinates is not autonomous planning.

Measure survival duration and population, usable food/tool/material stocks,
consumption versus replenishment, useful structures and recovery after losing an
actor or tool. Count reset supplies and operator interventions explicitly.
Evaluate with frozen weights and no external rescue, then separately investigate
learning during life.

## Boundaries that remain in force

No language-model gameplay driver, pathfinder, teacher-action sequence or
autocrafting fallback substitutes for learned behavior. Keep deployment free of
the learner and curriculum. Bounded local observations remain the starting point;
do not require broadcasting complete chunks to every NPC. New communication needs
a measured information and compute budget.

World edits remain opt-in. Preserve operator worlds, failed studies and the last
measured-good training state. The acceptance measure is useful learned behavior
per controlled experiment, not CPU saturation, historical badges, actor count,
attractive telemetry or the number of merged changes.
