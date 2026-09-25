# Operation-conditioned menu inputs

## Mechanical defect and scope

Before this change, the operation head selected left, right or shift click
independently of a slot head masked by the **union** of all three operations'
useful slots. A useful operation and a useful slot did not necessarily form a
useful pair. For example, with planks on the cursor and one occupied crafting
cell, placing into an empty cell is useful, and shift-clicking the occupied cell
is useful, but shift-clicking an empty cell does nothing.

`MenuPairRegression` constructs exactly that inventory using literal `Pocket`
clicks, not a recipe macro. With zero neural logits, the old implementation
assigns **0.195** joint probability to ineffective click/slot pairs. The same
test with the corrected implementation assigns **0**. These are exact enumerated
probabilities in one constructed state, **not measured live failure rates** or
evidence that this defect explains all workbench-learning failures.

The filter remains independent of the task goal. It uses `Pocket.wouldChange`,
including visible crafting-result availability and ordinary inventory capacity,
not the desired output, a preferred ingredient or a preferred destination.
Waiting, closing a menu, undesirable but effective clicks and wrong recipes
remain possible. No teacher, planner, recipe action, reward bonus or success
threshold is introduced. This is assistance through mechanical input filtering;
it is not a claim that every vanilla Minecraft click is exposed.

## One joint distribution, throughout inference and learning

For the menu operation `o` and the optional slot `s`:

```text
pi(o, s | x) = pi(o | x) pi(s | x, o)       for left/right/shift click
pi(o | x)                                 for wait/open/close
```

The network still emits one shared 64-element slot-logit vector. Each click type
normalizes that vector over its own mechanically effective slots. An operation
with no usable slots is disabled. An inactive branch has zero probabilities;
it need not invent a dummy slot distribution. The externally visible inactive
slot action remains the canonical zero.

`Schema.LOGITS` is still 105, and the value output remains at that index.
`Schema.DISTRIBUTION` is 233: the seven ordinary heads followed by three
64-element conditional slot blocks. `Schema.slotOffset(operation)` identifies
each block. Transient masks, probability workspaces, inference requests and
trajectory masks use this shape. Obsolete transient shapes fail validation.
There is one current implementation, not a compatibility path.

The following all use the same joint distribution:

- Sampling, behavior log likelihood and target-policy likelihood for V-trace.
- Entropy, including the parent derivative of the expected child entropy.
- Gradients into the shared slot logits, summing the reached action term and
  all probability-weighted child entropy terms.
- The update guard's exact joint KL: parent KL plus each child KL weighted by
  the **before-policy probability of that particular operation**.

Unreached child distributions contribute nothing to KL; in particular, an
inactive branch cannot create `0 * infinity`. Loss of reached support is infinite
KL. Greedy decoding chooses the best parent and then the best legal child; it
does not claim a global joint-argmax search.

The weak exploration prior is now explicitly a fixed joint prior: uniform over
legal parent controls, then uniform over legal slots for each click type.
Its cross entropy and shared-logit gradient weight children by that fixed parent
prior, not by the learned parent probabilities. This **changes the old
unconditional slot regularizer**; it is not merely logging or a numerically
identical optimizer update. The prior coefficient and update-guard thresholds
are unchanged.

## State and delivery boundary

The 512 -> 96 -> 96 network, 105 action logits, value output, learned parameter
layout, observation encoding and serialized checkpoint format are unchanged.
There is no weight conversion, reset, optimizer migration or added dependency.
All live world interaction still uses the existing owning Folia scheduler;
inference workers only receive immutable arrays. Training and deployment use
the same corrected decoder. Optimizer and curriculum code remain outside the
inference JAR.

Unchanged weights **do not imply unchanged behavior**: the decoder now has
different support/normalization, and continued learning uses the corrected prior.
A policy update counter or old course certificate cannot certify this new
runtime. Retain the exact evaluated JAR with its weights and re-evaluate after
a runtime update, including earlier skills, using the existing fixed-policy
evaluation/export path.

This change does not turn server-side NPCs into logged-in players, implement
hunger/persistent lives, or demonstrate open-world survival/cooperation.

## Verification

The baseline was GitHub main `cb58259f2dc2923d5238a7c94e6a55eba54ee984`.
For local isolated tests, the necessary original Java files were read through
GitHub and checked against their Git blob identities. This is not a claim that
a partial local source reconstruction is a complete API build.

Completed isolated Java 21 checks before PR publication:

| Check | Result |
| --- | --- |
| Identical literal-pocket witness against old and new implementations | Old fails at 0.195; corrected passes at 0 |
| Joint enumeration, sampling frequencies, finite-difference policy/entropy/prior gradients, KL and invalid-input boundaries | 133,914 assertions passed |
| Goal-independent differential inventory/menu mechanics, existing and strengthened checks | 3,873,742 assertions passed |
| Real asynchronous batched inference, scalar likelihood agreement and transition-mask validation | 2,056 assertions passed |
| Deliberately broken parent entropy coupling, sampled child branch, child KL weight and exploration child-gradient weight | All four mutations detected |

The new tests are invoked from `CoreTest`; strengthened menu tests retain the
actual-click differential oracle. Full source/API compilation, the existing
optimizer/learner/checkpoint tests, Windows checks and browser regressions are
separate CI evidence, not implied by the isolated table. No Minecraft world is
used by these mathematical/mechanical assertions.

## Operator-machine observation and uncompleted evaluation

The authorized machine was identified through Home Coder as workspace
`minecraft-agents`, hostname `cw-c76e701404f844d8876a4d50`. Its source checkout was
`/home/coder/workspace/botsclustersmc-source`, clean at the baseline commit.
One live snapshot reported 1,024 active/ticking/moved NPCs, no inference failures
or rejections and no burning bodies. This was **baseline runtime status**, not
a capacity result for the changed decoder.

An independent evaluation was started after existing EULA acceptance was read:

```sh
EULA=true ./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10 \
  --cases 32 --seed 2026092511 \
  --export dist/baseline-20260925-home-coder.zip
```

The connection closed during that call, but subsequent status confirmed that
the evaluator continued. The last successfully read status was running at
**317 / 352** trials for frozen policy **84808**, **33,687,043** trained samples.
A later remote command was blocked by the tool safety check before execution.
No alternate route was used to perform the blocked remote operations. Final
evaluation scores, successful completion and export existence are **unconfirmed**.

Before that block, an isolated development worktree was created and only the
standalone witness source was written there. No candidate implementation was
deployed to the machine, no production source was changed, no training server
was restarted, and no model/world reset was performed. Subsequent source work
and verification were confined to a separate local container and GitHub.

No workbench success improvement, saturation reduction, candidate 1,024-NPC
throughput or autonomous-survival improvement is claimed.

## 運用上の注意

この変更は「何も起きないクリックの組み合わせ」を除くものであり、作業台製作の
習得を証明するものではありません。学習済みの重みの形式は変えませんが、同じ
重みでもクリックの選び方が変わるため、更新後は固定モデルの独立試験で以前の
課題も含めて再確認してください。動作中の JAR の差し替えやホットリロードは
行わず、通常の停止・最終保存・バックアップを経て更新してください。

この作業では指定マシンへの候補導入は行っていません。途中まで実行を確認した
独立試験についても、最終結果・ZIP の作成成功は未確認です。
