# Current stopped policy: ordinary pickaxe qualification and mining diagnosis

Date: 2026-09-30 (Japan). This assesses one immutable saved model, not an
experiment showing that the new context observer improves learning.

## Why the current model was measured again

During activation of [accepted-context measurement](20260930-learning-contexts.md),
the live course displayed task 12. That historical progression did not establish
current competence. The earlier station-entry studies used older checkpoints and
could not determine the latest model's bottleneck. Before any new evaluation, a
fixed declaration selected two fresh seeds, all ordinary tasks 0-12, 32 cases per
task, no reset intervention, and retention of every trial regardless of outcome.
No learning rule or adoption threshold was changed for this assessment.

The source was the complete development checkpoint saved at controlled shutdown,
**1,182,364 updates and 344,488,915 trained samples**. It was learned on the
previous accepted runtime before the observer was installed. The same complete
model/Adam/course state was then restored for live training, while these exams
used its separate immutable exported policy. The old station-entry baseline was
297,530,150 samples: an extra 46,958,765 mainline samples separate the two models.
This is not a sample-matched comparison with the rejected fade candidate and does
not predict how that candidate would behave under another protocol.

| Identity | Value |
| --- | --- |
| Assessment source | `d2180141c0543a7b8e5386dd18425c19fcf729fb` |
| Canonical checkpoint SHA-256 | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Exported policy SHA-256 | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Assessment runtime SHA-256 | `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f` |
| Seeds | A: `2026093031`; B: `2026093032` |
| Tasks/cases | Ordered 0-12, 32 per task, 416 per seed |
| Assistance | Ordinary full Academy conditions; `reset_intervention=none` |

The accepted runtime changes no core policy, sensors, physical actuator, reset or
success predicate. Exams use the separate frozen evaluator, not the training
learner. Both loopback-only processes exited successfully with the original policy
unchanged and zero new training samples. The full matrix contains exactly two
reports and 832 unique trial executions; no unsuccessful cases were discarded.

## Complete ordinary results

Every vector is ordered by task 0 through task 12; each entry is out of 32.

```text
A: [32,32,32,32,32,32,32,32,32,32,32,31,0]
B: [32,31,32,32,32,32,32,32,32,32,32,31,0]
```

Wooden-pickaxe crafting (task 11) completed **31/32 on each seed without the
additional workbench-open intervention**. Materials and the station are still
supplied by the ordinary individual Academy task; opening, ingredient arrangement
and result collection are selected by the policy. This does not demonstrate the
continuous log-to-tool resource chain, natural-terrain survival or cooperation.

Cobblestone mining (task 12) completed **0/32 on both seeds**. The current focus is
therefore useful tool handling and mining acquisition while preserving the
observed earlier behavior, rather than continuing to describe workbench entry as
the current model's unqualified leading deficit. The old fade rejection remains
valid for its exact model and declared early budget; it is not retroactively
accepted, rerun or relabelled as a success.

## Read-only mining observations

These counts aggregate all 32 task-12 trials per seed. They are decision-boundary
observations, not every Minecraft tick or independent observations for statistical
inference. GUI and held-tool counts must use their own 19,200-observation denominator.

| Observation | Seed A | Seed B |
| --- | ---: | ---: |
| Pre-action observations | 19,200 | 19,200 |
| Closed menu | 5,154 | 5,135 |
| Personal inventory open | 14,046 | 14,065 |
| Other menus | 0 | 0 |
| Pickaxe held | 527 | 548 |
| Selected dig input, including inactive world channels | 1,495 | 1,448 |
| World-effective dig selections | 813 | 773 |
| Observed target contact with a pickaxe | 0 | 0 |
| Observed target contact with another held item | 18 | 10 |
| Maximum observed target pickaxe mining ticks | 0 | 0 |
| Maximum observed target other-item mining ticks | 7 | 4 |
| Blocks broken | 0 | 0 |
| Generic item pickups | 14 | 15 |

About 73% of the observations show the personal inventory and only about 2.8%
show a held pickaxe. None of the 64 cases recorded pickaxe contact at a decision
boundary. Brief between-boundary contacts are not ruled out; completed mining
remained zero. The generic pickup counter is not a cobblestone-acquisition result
and must not be credited as one. Input selection alone is not effective digging.

These observations prioritize testing learned inventory exit, retaining/selecting
a useful held tool, and sustained target contact. They do not identify the
responsible loss term, prove a mechanical fault, or justify automatically closing
menus/equipping tools. A next learning proposal needs matched warm controls,
explicit budgets and complete retention checks for the now-useful task-11 behavior
as well as earlier tasks. More actors or a recipe/mining macro is not that test.

## Integrity checks and retained artifacts

The read-only audit reran the existing full-report validator and additionally
bound each report to its requested task order, integer seed/case/port metadata,
ordinary-condition flags, policy counters, actual copied runtime, unchanged policy
and actual evaluator JAR. Neither exam created a training checkpoint.

An initial audit assumption that the two entire evaluator JARs should be equal
failed: their `config.yml` contains the two different declared seeds. All archive
member names and timestamps matched, and all non-configuration payloads were
byte-identical. The qualified audit checked each complete configuration against
its exact requested seed and verified every other payload unchanged. The original
JARs and raw reports were not edited or rerun. Both seed-specific JARs are retained,
not normalized into a fictitious common binary.

Raw artifacts are in `/home/coder/workspace/botsclustersmc/.build/`, principally
`context-activation-20260930/`, `context-current-policy-20260930/`, the prospective
`context-current-policy-declaration.json` and `current-policy-audit-qualified.log`.
The original stopped canonical state includes optimizer and course, not just a
policy-only reconstruction. This is a usable retained source for a subsequent
controlled experiment, not a checkpoint that certifies all 18 tasks.

The unpublished research draft `current-policy-context-20260930` targets the
assessment source and retains `current-policy-context-evidence-20260930.zip`:
31 members, 1,911,511 bytes, SHA-256
`383bfa03f6f70b7ecb8e4967588843f0ff19e2404492982c3b5db7e43bd9c170`.
ZIP integrity and every manifest member's size/digest passed; a fresh download
matched the local archive. Its explicit allowlist includes both frozen reports,
seed-specific evaluators, canonical input, runtime identities and activation
observations; it excludes Minecraft server binaries, worlds, caches, credentials
and environment files. It is not a published plugin release.
