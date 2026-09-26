# Harvesting practice and failure diagnosis

## What counts as success

`break-log` still requires an actual designated log block to be removed by the
normal primitive mining actuator. `collect-log` additionally requires the same
session's own dropped item to reach the NPC inventory. Merely selecting dig,
looking at a block, accumulating some mining ticks, or touching a foreign drop
is not a success. Mining duration, action masks, full-condition reset positions,
exam eligibility, exam denominators and promotion thresholds are unchanged.

The first five motor tasks do not teach this sustained interaction automatically.
A frozen model can reach a target reliably yet repeatedly interrupt its digging.
Independent trials therefore retain selected-dig counts, the observed maximum
contact with the designated block, angular errors, closest distance and actual
broken/collected counts. These are diagnostics, not alternate success criteria.
Mining progress is observed at decision boundaries; its recorded maximum can miss
the final tick when a real break resets the actuator counter.

## Practice-only initialization

For the two log tasks, easier practice resets use the existing aiming curriculum
to start near the target orientation. This assistance changes only the initial
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
count. Repeatedly starting and abandoning contact cannot farm positive shaped
reward. The genuine terminal outcome retains its normal +3 reward. After a real
break, the collect task instead uses a small bounded distance cost toward the
original drop location; success still requires the provenance-checked pickup.
This is documented task-specific reward shaping, not sparse-reward-only RL.

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

Complete evaluation bundles contain a read-only `harvest` object for `break-log`,
`collect-log` and `mine-cobblestone`. It records an explicit observation denominator,
menu-focused selections, world-dig selections, held-pick observations and separate
target-contact samples/maxima for pickaxes and other held items. The scope is
`decision-boundary-not-every-tick`. These counters do not choose actions, consume
randomness, change observations or access the learner; they are not in either
public plugin JAR.

Menu focus uses the preceding observation and selected menu operation; held items
and mining counters use the ending observation. These are not exact within-interval
event counts. A selected dig is not proof of target contact. The final break tick
resets the mining counter, and changes between boundaries can be missed. Actual
broken/collected counts and the unchanged terminal outcome establish completion.

A proposed stone reward/reset extension was rejected after measured retention loss
on 2026-09-26. The log-only reward and reset behavior remains the production default.
The new diagnostics are not evidence that cobblestone harvesting has been learned.

The actuator now rejects blocks already disallowed by the runtime's edit policy
before accumulating mining progress. This fixes misleading progress on protected
arena floors and on inference servers with world edits disabled. The completion
permission check and third-party block-change event remain in place. A later
third-party cancellation can still reject a previously permissible attempt.
