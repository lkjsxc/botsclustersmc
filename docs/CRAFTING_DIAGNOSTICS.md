# Frozen crafting diagnostics

The ordinary native `evaluate` command now records a `diagnostics.crafting`
object for wooden/stone pickaxe trials (task 11/13). This code lives in the
evaluation harness, not either production JAR. It does not change observations,
masks, rewards, reset assistance, actions, weights, optimizer or exam gates.

```sh
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 32 \
  --export .build/new-crafting-check.zip
```

Use a new export filename. The normal full-condition results still appear in the
observatory. Detailed per-trial measurements are in `evaluation-details.json`,
both inside the exact-model ZIP and in the Academy's plugin data directory.
There is no easier diagnostic reset, policy action override or training in this
evaluation. Older reports without this object remain historical reports; their
missing measurements cannot be reconstructed from old grid-unit counts.

## State and timing

The trace decodes the already-captured **pre-action** and actual next observation.
A correct cell contains the appropriate ingredient bucket, with at least one unit.
A wrong cell is occupied by the wrong ingredient, including any of the four cells
which must remain empty. Surplus units in otherwise correct cells are separate.
Five correct cells alone are not completion: extra wrong cells can prevent a
recipe, and the actual preview still has to be collected.

The five-element arrays refer to physical workbench slots `[36,37,38,40,43]`:

```text
36 37 38
39 40 41
42 43 44
```

`correct_mask_states` has 32 entries. Bit `i` means that ingredient cell `i` is
correct, so mask 0 means none and mask 31 means all five. Its sum is
`workbench_states`, not all observations. `max_correct_cells` also includes the
next state, including the last transition. Ordinary-inventory 2x2 cells and result
slots never count as a pickaxe grid.

`filled_cell_transitions` and `removed_cell_transitions` compare a cell only when
**both** frames show a workbench. Opening a workbench can reveal grid contents
retained by the current NPC mechanics, so these counters need not explain every
observed maximum. `partial_workbench_exits` separately counts leaving with one to
four correct cells. Exits can be chosen closes or mechanical range/station
invalidation. `chosen_close_actions` counts only actual close choices.

A removal is not intrinsically a failure: collecting a correct result legitimately
consumes ingredients. The trace reports what changed, not a teacher's verdict on
which action should have been chosen.

## Exact probabilities and their denominators

A private workspace re-evaluates the **same frozen model** on the stored
pre-action observation and mechanical mask. Its chosen-action log likelihood
must match the applied inference result within `1e-6`; otherwise the evaluation
fails instead of publishing mismatched diagnostics. This does not sample an
action or consume the actor's random stream.

For an empty required cell and a cursor holding its matching ingredient:

```text
fill mass = P(left) P(slot | left) + P(right) P(slot | right)
```

The single-unit variant always includes right click; it includes left click only
when the cursor holds one unit. Putting a whole stack in one slot can deprive other
cells of material, so neither measure is a claim of an optimal action or eventual
recipe completion. Legal conditional probabilities already exclude inert clicks.

Per-cell probability sums divide by `compatible_cursor_states_by_cell[i]`.
A state may offer several cells; summing those denominators double-counts such
states. To measure the chance of filling **any** compatible empty cell, divide
the sum of the five fill masses by the union count `compatible_cursor_states`.
Zero opportunities mean **unmeasured**, not zero competence.

`needed_stock_pickup_probability_sum` covers left/right pickup of storage stock
matching any currently empty required cell, on `empty_cursor_states`. Each
storage slot counts once even when several cells need it. Shift-click moves
storage stock but does not pick it up onto the cursor, so it is excluded.

`target_collection_probability_sum` sums all three legal result-slot click types
on `target_preview_states`. `close_probability_sum` divides by all
`workbench_states`. These denominators differ; do not compare the resulting means
as though they described the same states.

All are visit-weighted state statistics. Repeated observations in one trial are
correlated; 19,200 decisions do not become 19,200 independent tests. They narrow
behavioral failure modes, but cannot by themselves isolate representation,
optimization, reward, curriculum or exploration as the causal learning defect.

## Verification and cost

Source checks cover exhaustive synthetic raw-grid configurations, actual Pocket
click effects, joint rather than conditional-only probability, preview separation,
malformed values, and unchanged observations/masks/actions/random streams/weights.
The normal source command also compiles the real-API holdout harness now.

The accumulator has fixed arrays; no full-state trajectory or unbounded list is
retained. The evaluator performs one extra forward pass per pickaxe transition.
Logical action/RNG noninterference does not promise identical wall-clock
scheduling. Production training and deployment perform no additional pass.

## 運用上の読み方

成功率が変わらなくても、「正しい材料を一つも置けない」「途中で作業台を閉じる」
「完成品は出るのに回収しない」を区別できます。成功数は従来どおり補助なし試行の
結果です。診断上の正しい配置数や確率を、習得・進級・共同生活の成功に置き換えません。
