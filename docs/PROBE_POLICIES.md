# Which policies completed a probe?

Full-difficulty **probes are not frozen exams**. They can contribute training
samples, and the shared policy may change between decisions inside one probe.
A successful changing-policy attempt does not certify the final checkpoint.

The runtime now records the behavior-version IDs from **actually applied
inference results**, not the learner's current version at the time of reporting.
For every completed PROBE episode, it counts decisions, changes between
consecutive applied versions, and the minimum/maximum version seen. The trace accepts nonmonotonic recorded sequences rather
than approximating minimum/maximum by the first/last version. It does not infer
which version was applied from a newer global status snapshot.

## Reading the dashboard and status

The `Policies used inside full probes` panel reports successful / completed
attempts per task, split into:

- **Single policy:** one version was applied throughout that episode. Different
  episodes in this group can still have different versions.
- **Mixed policies:** at least two versions were applied during the episode.
  Version span is not the number of different models or a performance score.

Neither group certifies the current policy or open-world survival. Use the
separate immutable-policy evaluator for that snapshot's measured task results.
Zero completed probes have no estimated success percentage. Missing/malformed
measurements are unavailable, never inferred from old cumulative counters.
Paused or stale status displays historical process totals, not active learning.

The status scope is
`completed-probes-applied-behavior-versions-this-process`. All task arrays have
18 entries, in the normal task order. `probe_single_policy_trials` plus
`probe_mixed_policy_trials` equals `probe_trials_this_process`; the analogous
success counts equal `probe_successes_this_process`. These totals and their
partitions are sampled under **one shared monitor**, avoiding torn reports.

`probe_behavior_decisions`, `probe_policy_changes` and
`probe_maximum_policy_span` describe all completed probes in the process.
`probe_last_minimum_policy` / `probe_last_maximum_policy` describe the last
completed probe for that task, with -1 for an absent task. They are not the live
model version. Normal source tests cover concurrent reporting and invalid/
overflowing updates without partial mutation.

## What does not change

No action, RNG draw, mask, policy weight, gradient, optimizer rule, curriculum
allocation, reward, success predicate or certificate is selected by this
measurement. Traces are reset with lesson state. Interrupted/unfinished episodes
are not counted as completed probes. Practice and frozen exams are excluded
from the probe partition. Counts reset on process restart; canonical
model/Adam/course formats are unchanged. No historical identities are invented.
The inference-only JAR contains none of this training measurement code.

## Paired lifecycle diagnostic

`tests/lifecycle.py` is an opt-in developer diagnostic using the same NPC body,
reset and success implementation in a separate loopback Folia server. It loads
only validated policy bytes from an evaluated bundle; it never executes that
bundle's JAR or reads/copies an Academy or optimizer checkpoint. Existing public
server libraries are reused. EULA consent is explicit.

For each of 32 cases in each arm, the task order is wood pick (11), cobblestone
(12), wood pick again (11). EXAM and PROBE reset RNG inputs are matched after
accounting for their distinct salts, as are action RNG seeds. The model remains
fixed, no learner exists, and every failed trial is retained. The first
observations/masks and actual elapsed transition ticks are recorded. Reports are
bounded; source tests compile the real-API fixture without running a server.

```sh
EULA=true python3 tests/lifecycle.py \
  --bundle /absolute/path/to/evaluated.zip \
  --cache /absolute/path/to/public-server-cache \
  --seed 2026092641 --cases 32 --label baseline
python3 tests/lifecycle_summary.py \
  .build/evidence/lifecycle-2026092641-baseline/server/plugins/BotsClustersMC/lifecycle-result.json
```

The summary reports **all** initial-tensor differences. Matching random seeds
alone does not prove identical physics or scheduling. This is a diagnostic,
not a curriculum promotion or a learned-skill acceptance gate.
