# Mining target-facing reset: prospective frozen-policy diagnosis

Date: 2026-09-30 (Japan). This protocol is declared before any gameplay result
from the new intervention. It is a diagnostic-only frozen-policy comparison, not
a training change, deployment candidate or claim of autonomous mining.

## Question

The current stopped mainline policy at 344,488,915 trained samples completed
ordinary wooden-pickaxe crafting 31/32 on both fresh seeds, but completed
cobblestone mining 0/32 on both. In those 64 mining trials, no decision-boundary
sample recorded pickaxe contact with the target and only about 2.8% of observations
had a pickaxe held. The policy also spent about 73% of decision observations with
the personal inventory open.

This experiment asks one narrower question: **does removing only the random
initial facing materially improve actual cobblestone completion or sustained
pickaxe contact?** It does not test automatic tool selection, inventory closing,
pathfinding, action masking, reward shaping or a learned mining curriculum.

## Exact intervention and invariants

Add one holdout-only reset label, `mine-target-facing`. It is legal only for task
12 and only with an immutable `--policy`, never a moving canonical checkpoint.
After the ordinary task-12 reset finishes and before policy decision zero, the
exam verifies all of the following:

- menu closed;
- selected hotbar slot 0;
- cursor empty;
- exactly one supplied `WOODEN_PICKAXE` in storage slot 0;
- every other storage slot empty;
- no crafted/extracted counter;
- the ordinary owned cobblestone target and spawn remain unchanged.

The intervention then rotates the NPC once so its eye faces the centre of the
ordinary target block. It does not move the entity, alter inventory, select an
action, keep the policy facing the block, inhibit later inventory opening, modify
the goal, add reward, change the action mask, or run after decision zero. Every
subsequent movement, look, inventory operation and dig action is sampled from the
same immutable neural policy as control.

The existing `none` condition remains the ordinary control. Workbench reset
diagnostics retain their original task-11/13 scope. The Python runner and native
exam both reject cross-task use. The report remains explicitly
`diagnostic_only=true` for the target-facing arm.

## Immutable source and model

Research branch source begins at accepted main
`2e44e2e8e87b3200268d23b979c2b1e3e7800fb8`. The policy is the previously
preserved stopped current-policy export:

- policy updates: **1,182,364**
- trained samples: **344,488,915**
- policy SHA-256:
  `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc`
- canonical checkpoint SHA-256:
  `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e`

The intervention changes only holdout/test source. Rebuilt production artifacts
must remain byte-identical to accepted main:

- training JAR:
  `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`
- inference JAR:
  `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0`

No live Academy, model, optimizer, course, service or production plugin is changed
by this study.

## Fixed evaluation matrix

Use fresh seeds **2026093041** and **2026093042**. For each seed run:

1. task 12, 32 cases, `reset_intervention=none`;
2. task 12, 32 cases, `reset_intervention=mine-target-facing`.

Thus the complete matrix is four reports and **128 unique frozen trial executions**.
At most two loopback evaluation servers may run concurrently. All reports use the
same saved policy, production runtime, ordinary task target/resources/success
predicate and stochastic policy. Every trial is retained, including failures.
No evaluation contributes training samples.

The ordinary and assisted conditions remain separate. An exit code of zero means
the evaluation completed with intact evidence, not that mining succeeded.

## Predeclared interpretation

For each seed, record complete-task success, pickaxe-held observations, observed
target pickaxe contact, maximum target pickaxe mining ticks, effective world-dig
selections, menu-state observations, broken blocks and provenance-correct task
completion.

Call the exact tested policy **pose-sensitive under this diagnostic** only if, on
**both** seeds, the target-facing condition:

- completes at least **8/32** task-12 cases, and
- exceeds its matched ordinary control by at least **4/32**.

This is an engineering diagnostic screen, not a confidence interval or a
deployment gate. If it fails, do not conclude that pose is irrelevant: later
policy actions may immediately destroy the supplied alignment. If it passes, do
not conclude that mining has been learned under ordinary starts: the intervention
supplied the initial facing.

Contact and held-tool improvements are descriptive even if the completion screen
fails. In particular, selected dig input is not target contact, generic pickup is
not cobblestone acquisition, and one favorable seed cannot be pooled with another
to manufacture a positive result.

The next training intervention, if any, will be chosen only after the complete
matrix is recorded. No result from this diagnostic automatically changes reward,
curriculum, runtime or main.

## Pre-execution verification

Before gameplay:

- Java reset-boundary tests cover the untouched mining pocket, every wrong task,
  malformed inventory/cursor/selection/counter states, exact target-facing
  geometry and non-finite rejection.
- Python runner tests cover task-12-only CLI scope, checkpoint rejection and
  diagnostic report classification.
- the ordinary workbench diagnostic suite remains passing;
- production training and inference JAR hashes remain exactly the accepted hashes
  above;
- the full local source suite must pass from the committed research source.

Execution results are appended below without changing seeds, case counts,
conditions or interpretation thresholds.

## Execution results

No gameplay result was claimed at declaration.

### Completed matrix

Actual prospective source: `1de63d070184bd3e55476836e5d5815373b1659b`,
tree `0d3ba0aa712305c1597887f3ab434b497dead55e`. It was authored as
`lkjsxc` and pushed before gameplay. The complete local source suite passed from
that exact commit and ended `MINING_TARGET_FACING_SOURCE_EXIT 0`. The focused
reset-boundary test passed 1,347 checks and the Python reset-diagnostic group
passed seven methods. Rebuilt training and inference JAR hashes remained exactly
the accepted values declared above.

All four reports completed and account for exactly 128 frozen trial executions.
The source policy stayed at 1,182,364 updates / 344,488,915 trained samples and
all reports recorded zero new training samples.

| Seed | Ordinary | Target-facing | Pose-sensitive screen |
| --- | ---: | ---: | --- |
| 2026093041 | 0/32 | 0/32 | fail |
| 2026093042 | 0/32 | 0/32 | fail |

The predeclared pose-sensitive classification is therefore **false** for this
exact policy and intervention. This is a completed diagnostic rejection, not an
operational failure and not evidence that orientation never matters.

### Read-only behavior differences

The table below aggregates decision-boundary diagnostics across each 32-case
report. Each report has 19,200 pre-action observations.

| Seed / condition | Pick held | Pick target contact | Cases with pick contact | Max pick mining ticks | Effective world dig | Closed / inventory observations |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| A ordinary | 556 | 1 | 1 | 3 | 829 | 5,251 / 13,949 |
| A target-facing | 579 | 1 | 1 | 4 | 812 | 5,208 / 13,992 |
| B ordinary | 580 | 0 | 0 | 0 | 813 | 5,123 / 14,077 |
| B target-facing | 614 | 0 | 0 | 0 | 800 | 5,118 / 14,082 |

No target block was broken in any of the 128 trials. Generic item-pickup counts
were 18/17 on seed A and 14/15 on seed B for ordinary/target-facing respectively;
they are not cobblestone completion and are not credited as mining.

Mean absolute yaw/pitch error decreased from 72.58°/45.63° to 61.47°/41.23° on
seed A and from 75.26°/50.82° to 73.06°/44.97° on seed B. This confirms that the
one-time pose perturbation can affect subsequent measured orientation, but it did
not create sustained target contact or completion. The policy continued spending
about 73% of decision observations in the personal inventory under both
conditions. A one-time good starting pose was rapidly overwhelmed by later
policy-selected look/menu/tool behavior.

The useful interpretation is narrower than “aim does not matter.” Initial random
facing is not sufficient to explain the current failure. The existing practice
environment already gives low-difficulty task-12 lessons a favorable starting
pose, while task 12 lacks the harvesting control-cost shaping used by log tasks.
The next learning candidate should therefore address observable mining readiness
and sustained target progress without scripting tool selection, forcing menus
closed, masking actions or adding an in-episode auto-aim controller.

No production runtime or model from this research branch is accepted for
deployment. PR #49 remains a research record and should be closed unmerged after
retaining this evidence.

### Retained evidence

The unpublished GitHub research draft `mining-target-facing-study-20260930`
targets the prospective source and retains
`mining-target-facing-evidence-20260930.zip`: **26 members, 1,520,285 bytes**,
SHA-256
`a92928c09c255e73c6847dd1ad739861d6b02bed21b8d6746374cf360f63998f`.
A fresh download matched the local archive byte-for-byte. ZIP CRC and every
manifest member size/digest passed. The explicit allowlist contains the immutable
policy, unchanged production JARs, protocol, source-test log, all four raw
result/metadata pairs, seed-specific evaluator JARs and runner logs. It excludes
Minecraft server binaries, worlds, caches, environment files and credentials.
The draft is research evidence, not a plugin release or skill certificate.
