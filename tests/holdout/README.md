# Independent frozen-policy trials

This opt-in developer test evaluates one saved policy in a disposable real
Minecraft server. Every gameplay action is sampled from the same immutable
neural policy. There is no scripted motor controller, optimizer, trajectory
training queue, curriculum promotion, or modification of the source checkpoint.
By default, the regular training reset and success predicates are used at full
difficulty. Explicit reset-intervention diagnostics below are not standard exams.

After personally accepting the Minecraft EULA and building the current runtime:

```sh
./build.sh
EULA=true python3 tests/holdout.py \
  --checkpoint academy/server/plugins/BotsClustersMC/training.bcmc \
  --output .build/holdout \
  --tasks 0 1 2 --cases 64 --seed 618203
```

`--checkpoint` first copies one complete canonical checkpoint, then exports its
policy inside the disposable evaluation directory. The live Academy need not be
stopped, and later training cannot change that frozen copy. An inference-only
model can instead be supplied with `--policy`; the two inputs are mutually exclusive.
No new training checkpoint is created by the evaluation plugin. The input copy is
retained as `source-training.bcmc` and checked for modification when using a checkpoint.

The output directory must be new. `--runtime` can identify an independently built
training JAR; `--cache` selects the prepared server cache. They default to
`dist/training.jar` and `.cache/server`. The evaluator is compiled separately and
is never included in either production JAR. Python is a test-only dependency.
The script requires explicit `EULA=true`, binds to loopback, rejects port 25565,
and disables human admission to this test server. No production service stops.

The per-task case count is bounded to 1–256, with at most 2048 trials total.
Each task uses a fixed initial seed schedule. Cases, task IDs and seed are stored
with every result. Full-difficulty tasks still have the documented academy
assistance: supplied targets/resources, rooms, resets and training invulnerability.

The results are written to `result.json`, including every failure. Each trial
also records selected-dig frequency, observed peak contact with the designated
block, mean angular error, closest distance and real broken/collected counts.
These diagnose failures; they do not replace the original success predicates. `metadata.json`
identifies the exact runtime JAR, evaluator JAR and immutable policy bytes.
The copied policy and runtime remain in the disposable output for reproduction.
The runner verifies the policy is unchanged, no training checkpoint exists, all
trials completed exactly once, and task aggregates match the individual cases.

**Exit code zero establishes experiment integrity, not learned mastery.** A report
with zero successful cases is a valid completed evaluation. Read the actual
success counts. Historical per-actor training certificates are not consulted.
A report for one frozen policy never certifies the continually changing live
policy or all tasks not selected for the experiment. To compare snapshots, use
the same runtime, tasks, case count and seed; still allow for asynchronous server
timing and stochastic action variation. New seeds test more than one fixed suite.

The test does not establish natural-terrain generalization, unrestricted survival,
long-lived NPC inventories, combat, food production, or multiplayer cooperation.
Do not label scripted reachability diagnostics as these neural-policy results.

## Read-only workbench-construction diagnostics

Task 10 (`craft-workbench`, the 2x2 personal inventory recipe) now records
`diagnostics.table_crafting`. This is an observer, not an assisted reset or a
recipe selector. It re-evaluates the immutable policy distribution on the actual
pre-action observation and checks the applied action likelihood. It never samples,
modifies an action, accesses a world, updates a weight or enters a training JAR.

The sixteen `correct_mask_states` describe which physical grid cells contain
planks. Four units stacked in one cell are one correct cell, not four. Target
workbench previews and other previews (notably the two-plank stick recipe) have
separate counts, selected result clicks and joint collection-probability sums.
Clicks are requests, not proof of crafting. `crafted_stick_units` and
`crafted_workbench_units` in the enclosing diagnostics are actual final
`Pocket.crafted` counters. `observed_*_units_gained` inside the trace instead sum
positive changes of carried stock while the inventory remains visible; picking
up a previously dropped item can increase these again. Do not equate those sums
with manufactured output or use them as a resource-conservation ledger.

Fill opportunities and probability sums are grouped by the number of already
correct cells. Divide each probability sum by its matching opportunity count;
zero opportunities mean no measurement, not zero competence. The single-unit
sum distinguishes right-click placement from depositing an entire cursor stack.
Cell removal counters exclude observed output gains so ordinary ingredient
consumption is not mislabeled as dismantling. Partial menu exits are separate.

`carried_planks_below_four_without_table_states` counts pre-action **visible
inventory** states with fewer than four carried planks and no carried table.
It excludes the preview and does not treat a closed menu's hidden grid as lost
stock. It is not an impossibility certificate: world items might be recoverable.
`final_carried_plank_units` in the enclosing diagnostics includes the retained
hidden grid through the ordinary pocket count. These are observation counts,
not elapsed-time or training-sample fractions.

The Python runner checks required fields, types, finite probability sums,
array sizes and denominators for this trace, and rejects it on other tasks.
The native evaluator retains the detailed trace but its operator-facing skill
summary continues to use the ordinary completion predicate. The diagnostics do
not select a policy, promote a lesson, change any success threshold, or establish
why a learner failed to acquire the behavior.

## Reset-only workbench diagnostics

`--reset-intervention` is an opt-in developer diagnostic, not a training option.
The default `none` leaves the full-condition evaluator unchanged. The other
choices require a saved `--policy` and only pickaxe tasks 11 or 13; a moving
`--checkpoint` is rejected so a matrix cannot silently compare different models.

| Condition | Initial menu | Initial ingredients | What is still selected by the policy |
| --- | --- | --- | --- |
| `none` | Closed | Raw stock | Opening, assembly and collection |
| `workbench-open` | Workbench | Identical raw stock | Assembly and collection; closing remains possible |
| `pickaxe-grid` | Workbench | Five supplied units already arranged | Output collection and all subsequent actions |
| `pickaxe-missing-top-left` / `-top-center` / `-top-right` | Workbench | Four arranged units; missing head unit in storage slot 0 | Pick up, place and collect; earlier cells can still be disturbed |
| `pickaxe-missing-handle-upper` / `-handle-lower` | Workbench | Four arranged units; missing stick in storage slot 1 | Pick up, place and collect; earlier cells can still be disturbed |

The single-cell labels identify physical recipe slots 36, 37, 38, 40 and 43,
respectively. Use the full prefix `pickaxe-missing-` for every command-line label.
The missing unit is deliberately **not** placed on the cursor. These conditions
separate positions, not just a count of unfinished cells. They do not establish
that a model can assemble the preceding four cells or sequence the whole recipe.
Task 13 uses cobblestone instead of planks, with the same two handle sticks.

No completed item or crafted counter is supplied. All assisted conditions leave
the random pose and empty cursor unchanged. Assistance runs once, on the owning
entity thread, after the normal asynchronous reset and before any policy request.
The test checks the stock, station ownership, zero decisions and applied coverage.
It does not consume the action RNG, keep the menu open, move an agent, choose
later clicks or change the success predicate. `pickaxe-grid` deliberately bypasses
assembly and must never be used as evidence that assembly has been learned.

First retain one model using the ordinary native evaluator, then extract only its
policy data. Do not run the archived plugin; use the current built runtime:

```sh
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 32 \
  --seed 2026092703 --export .build/station-baseline.zip
unzip -n -j .build/station-baseline.zip \
  plugins/BotsClustersMC/policy.bcmc -d .build/station-policy

for condition in none workbench-open pickaxe-grid; do
  EULA=true JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2 \
    python3 tests/holdout.py \
      --policy .build/station-policy/policy.bcmc \
      --output ".build/station-cases/$condition" \
      --tasks 11 --cases 32 --seed 2026092704 \
      --port 25584 --reset-intervention "$condition"
done
```

Use a new output path for every suite and another predeclared seed to replicate.
The saved policy directory must not contain the output directory. Preserve task
order and case counts as well as seed: actor IDs also seed policy sampling.
Different task lists are not paired-action controls. Asynchronous server timing
can still produce differences even with the same initial seed specification.
Declare the matrix before looking at results and keep unsuccessful conditions.
The developer runner uses Python; neither production JAR gains that dependency.

Assisted `result.json` reports include `diagnostic_only: true`, the exact
`reset_intervention` and `reset_intervention_trials`. The runner rejects missing
or mismatched labels/coverage. Its metadata and console output also identify
assistance. These local files do not replace the live observatory's standard
result. The native evaluation validator and its evaluated-bundle/replay consumers
reject assisted reports even when their success counts are high.

Compare identical fixed models, not the continually changing learner. A large
open-menu improvement would implicate entry/starting-state transfer; a large
supplied-grid improvement would distinguish collection ability from raw assembly.
Neither result alone identifies a reward, representation or optimizer defect.
The initial measured study and its limitations are recorded in
[the September 27 verification record](../../docs/verification/20260927-station-reset-study.md).
The five-cell extension and newer frozen model are recorded separately in
[the September 29 single-cell study](../../docs/verification/20260929-pickaxe-cell-diagnosis.md).

## Read-only tool-use diagnostics

Task 12 also records `diagnostics.tool_use`: pre-action tool-location states,
menu/tool handoff counts, and exact one-decision probability mass for increasingly
specific world inputs. These are not target-contact probabilities or a scripted
equipment/mining mechanism. See the [field and interpretation contract](../../docs/TOOL_USE_DIAGNOSTICS.md)
for array ordering, denominators, hidden-grid limitations and native validation.
The extension still requires its real frozen-exam qualification; source-test
success alone is not physical repeatability or learned mining evidence.
