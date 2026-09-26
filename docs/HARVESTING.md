# Harvesting practice and failure diagnosis

## What counts as success

`break-log` still requires an actual designated log block to be removed by the
normal primitive mining actuator. `collect-log` additionally requires the same
session's own dropped item to reach the NPC inventory. Merely selecting dig,
looking at a block, accumulating some mining ticks, or touching a foreign drop
is not a success. `mine-cobblestone` requires a real, session-owned cobblestone
pickup that remains in the inventory; a stone break without a suitable tool is
not sufficient. Mining duration, action masks, full-condition reset positions,
exam eligibility, exam denominators and promotion thresholds are unchanged.

The first five motor tasks do not teach this sustained interaction automatically.
A frozen model can reach a target reliably yet repeatedly interrupt its digging.
Independent trials therefore retain selected-dig counts, the observed maximum
contact with the designated block, angular errors, closest distance and actual
broken/collected counts. These are diagnostics, not alternate success criteria.
Mining progress is observed at decision boundaries; its recorded maximum can miss
the final tick when a real break resets the actuator counter.

## Practice-only initialization

For the two log tasks and `mine-cobblestone`, easier practice resets use the
existing aiming curriculum to start near the target orientation. This assistance changes only the initial
pose. The policy still chooses every later movement, look and interaction input.
Full probes and exams keep their original orientation and random stream exactly;
no aiming correction is inserted into their reset or gameplay.

No tree search, recipe macro, script, demonstration, LLM or preferred-action mask
is used to choose normal gameplay. The bodies remain in-server NPCs rather than
vanilla player clients. Food, complete combat and persistent shared lives are not
provided by this curriculum change.

## Dense measured state cost

Before the target is broken, the additional reward is nonpositive in every state.
For each nominal four-tick interval it subtracts:

- `0.04 * (1 - target_mining_progress)` for unfinished work;
- bounded horizontal/vertical angular-error costs, each at most 0.025;
- an out-of-reach cost at most 0.015 and aligned-motion cost at most 0.004.

Elapsed time scales these costs by `actual_ticks / 4`. Target mining progress is
read from real contact with that designated block; digging empty air does not
count. Log progress uses the actuator's 60-tick duration. Stone progress uses its
40-tick duration with a wooden or stone pickaxe; bare-hand contact is not credited
because it cannot yield the required cobblestone. The observation layout and
actuator remain unchanged; this normalization is only for training rewards.
Repeatedly starting and abandoning contact cannot farm positive shaped reward.
The genuine terminal outcome retains its normal +3 reward. After a real break,
`collect-log` and `mine-cobblestone` instead use a small bounded distance cost
toward the original drop location; success still requires the provenance-checked pickup.
This is documented task-specific reward shaping, not sparse-reward-only RL.

The stone task also receives the same bounded contact potential (weight 0.15) as
log collection. It enters the existing discounted potential difference, not an
unconditional per-step positive bonus. Contact lost before completion loses that
potential, and terminal handling sets the next potential to zero. Actual stone
breaking switches the additional state cost to collection, not to a log-specific
condition. Tests tie the 39/59 last pre-break ticks to actual actuator behavior.
None of these changes selects a hotbar slot, closes a menu, steers, or holds dig
on behalf of the policy. Full-condition frozen evaluations still perform no learning.

A preliminary small positive contact bonus improved neither log task in the first
measured frozen-policy test; those failures are retained in the verification
record. The sustained-contact cost is a later intervention, not a randomized
ablation establishing an isolated causal effect. A new shaping function is not
itself proof that a skill has been learned. Read the actual held-out outcomes.

## Inspect actual attempts

`/bots inspect <id>` now names the selected primitive controls and reports the
held item, current mining block/ticks and this episode's real broken, collected
and crafted counts. The resource-task spectator HUD shows digging progress.
These are immutable copies made on the owning entity thread; the camera and
console do not read another region's mutable inventory or world.

The dashboard separates current-cohort readiness from historical certificates.
A cohort's trial count includes its full-difficulty training probes. Empty cohorts
are absent, not 0%-success cohorts. Cohort statistics can change when actors
advance or regress; historical certificates do not certify the latest live model.

### Frozen harvesting diagnostics

The complete report from `evaluate.sh` contains a `harvest` object for the two log
tasks and `mine-cobblestone`. It distinguishes selected world-dig actions from
selections suppressed by menu focus, records the held pickaxe at the ending
observation, and splits target-contact samples and their maxima by pickaxe versus
other held items. Each object carries its observation denominator and the scope
`decision-boundary-not-every-tick`. These counters do not sample actions, alter
observations or consult the learner. They are not packaged in the inference JAR.

Menu focus describes the preceding observation and chosen menu operation; held
items and mining counters describe the ending observation. Thus these are not
exact within-interval event counts. The final tick of a successful break resets
the mining counter, and tool/item changes between boundaries can also be missed.
A selected dig is not evidence that a block was hit, and a contact maximum is not
proof of a successful break or pickup. Use the unchanged terminal outcome and
actual broken/collected counts for those claims.

The actuator now rejects blocks already disallowed by the runtime's edit policy
before accumulating mining progress. This fixes misleading progress on protected
arena floors and on inference servers with world edits disabled. The completion
permission check and third-party block-change event remain in place. A later
third-party cancellation can still reject a previously permissible attempt.

## 運用メモ

原木採取と丸石採掘は、対象へ照準を合わせて掘り続け、実際に落ちた資源を拾う練習です。
易しい練習の開始方向だけを補助し、本試験ではランダムな向きから全操作を方策が選びます。
報酬の変更を反映するには学習サーバーを通常停止・再起動してください。チェックポイントや
ワールドを消す必要はありません。評価ZIPで同じ重みを再試験しても、それだけで学習は
進みません。段階の人数や掘る回数ではなく、固定方策の成功数と試行数を確認してください。
