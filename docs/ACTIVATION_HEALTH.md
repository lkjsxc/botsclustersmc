# Hidden activation health

The observatory's **Hidden activation diagnostics** panel describes the last
learner batch. It is a diagnostic, not an optimization method or a skill score.
There is no model reset, extra loss, teacher, scripted action, reward change,
checkpoint migration, observation-schema change or deployment dependency.

## What is measured

For each current state already evaluated by `Gradient.compute`, both hidden
activation arrays are read exactly once. The bootstrap forward passes are not
counted. The diagnostic neither retains the observation nor writes to the
network workspace. It does not call `Policy.forward` itself or consume randomness.

For each task and hidden layer, the panel reports:

- **States:** raw observations in this learner batch, not NPCs or episodes.
- **Saturated:** the fraction of hidden-unit observations whose absolute float
  activation is strictly greater than `.99f`.
- **Mean slope:** the mean local tanh derivative, `1 - h*h`, calculated in double
  precision from the already-computed float activation. This is not the full
  network gradient, Jacobian, representation rank or plasticity measure.

A neuron saturated in one state can behave differently in another. Even a high
fraction cannot establish permanently inactive neurons, explain a crafting
failure, or demonstrate that reducing saturation would improve learning.

Task labels come from the existing `TaskBalance.task` observation classifier.
Indices 0 through 17 are the current tasks; index 18 is an unclassified
observation. An absent task has zero states and `-1` fractions/slopes, **not a
measured zero saturation rate**. The panel omits its row instead of fabricating
health. Full-difficulty probes can train; frozen certification exams do not enter
the learner queue. Future-version or overly stale trajectories are excluded
before gradient calculation and consequently before measurement.

## Batch identity and publication

Each gradient worker owns a bounded `ActivationHealth.Accumulator`. Workers
return immutable snapshots. The learner merges raw integer counters and slope
sums, not worker averages, so unequal partitions cannot distort the denominator.
Neither task-loss balancing nor importance weights multiply these measurements.
The final observation count must equal the actual computed gradient sample count.

One immutable `Measurement` is published after the guard decides whether to accept
that update. Its policy identity is the **target policy before the update**. An
accepted update normally publishes the next policy version; diagnostics must not
be relabeled as observations of that new version. A rejected batch still has a
valid measurement, but is explicitly described as measured, not learned.

Only the last batch is retained. A task missing from it has no new measurement;
this is not a task-wide running average. The status/history transport can retain
its usual bounded snapshots, but those snapshots are not additional independent
learner batches and should not be summed as training effort.

Before the first computed nonempty batch, no activation fields are published.
The fields are not persisted in `training.bcmc` and never change restart identity.
The existing inference-only plugin does not depend on this training-only class.

## Status fields

All fields have the `activation_health_` prefix:

| Suffix | Meaning |
| --- | --- |
| `scope` | Exactly `last-learner-batch-unweighted` |
| `policy_updates` | Measured pre-update target policy version |
| `epoch_millis` | Time this completed batch measurement was published |
| `update_accepted` | Whether the guard accepted the corresponding update |
| `samples` | Total raw current-state observations |
| `hidden_units` | Number of hidden units per layer |
| `saturation_threshold` | Strict absolute-activation threshold |
| `task_samples` | Nineteen raw per-task counts |
| `first_saturation`, `second_saturation` | Nineteen per-task fractions per layer |
| `first_mean_slope`, `second_mean_slope` | Nineteen per-task local-slope averages |

Array fields use the runtime's existing serialized-array convention. The browser
also accepts native JSON arrays. It validates lengths, count integrality and
bounds, total conservation, absence sentinels, fraction ranges, identity,
disposition and scope before constructing table cells with `textContent`.
Invalid or missing input clears the table and shows unavailable, not healthy.

Freshness is checked against both the outer runtime status timestamp and the
batch's own timestamp. A measurement more than 15 seconds old, a stopped/failed
runtime, or a clock more than five seconds ahead is labeled historical. A fresh
outer status cannot make an old last-batch measurement look current. Fetch
failure clears this panel rather than retaining an apparently live result.

## Cost and validation

Collection adds a bounded scan of two hidden arrays per gradient sample, plus
per-task worker counters and one last immutable publication. It adds no world
access, inference task, queue, checkpoint bytes or per-agent retained history.
There is still nonzero arithmetic/allocation cost; no production throughput
improvement or zero-overhead claim is made.

The ordinary `UpdateTest` entrypoint invokes `ActivationHealthTest` and
`ActivationGradientTest`. The former tests arithmetic, signs/thresholds,
immutability, invalid inputs, absence and unequal partition merging. The latter
compares instrumented/uninstrumented gradients, scalar losses and Adam updates
bit-for-bit, including task balancing and synthetic saturated networks, and
exercises the real asynchronous learner with accepted, guard-rejected and
future-version trajectories. These are synthetic numerical/concurrency tests,
not demonstrations of Minecraft skill.

`tests/crafting_monitor.py` also invokes `tests/activation_monitor.py`. It checks
render wiring, task counts, identity, rejected/absent/stale states, malformed
telemetry and desktop/mobile layout using synthetic offline responses. Its
screenshots are UI fixtures, never live Academy measurements.
