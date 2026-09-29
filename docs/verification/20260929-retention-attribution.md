# Retention attribution - 2026-09-29

## Prospective protocol

This section is committed before evaluating any parameter counterfactual. It is
a diagnostic study, not a proposed replacement learner or deployment candidate.
Mainline source at declaration is `54679245521ecc39435c8e44caa0aeb11d1a51f4`.

The [placement-practice screen](20260929-placement-practice-study.md) preserved a
shared-policy parent and an unchanged-runtime control whose workbench skill fell
on both evaluation seeds after 252,985 additional accepted samples. Use those
already retained files, not a newly selected favorable checkpoint:

- Base: policy 332305, 92,574,669 accepted samples, original policy identity
  `894a84745625919548620e0a47698d38153234e9b73d7a0a9acb14a0aac4acb1`.
- Donor: the stopped **unchanged control**, policy 333159, 92,827,654 samples.
  Its path is the former experiment's
  `.build/placement-study/control/250000/stopped-policy.bcmc`.
- Preserve both exact inputs and all generated policies. Record both input
  identities, tested source and common runtime before evaluation. Do not modify
  the old experiment, its optimizer/course files, or the live Academy.

The offline `PolicyBlocks` utility partitions all 68,842 parameters into four
nonoverlapping blocks. A set mask bit selects the donor's complete block; an
unset bit selects the base. It never interpolates weights, chooses actions,
changes masks, supplies a recipe, or changes reset/reward/learner semantics.

| Bit | Block | Definition | Parameters |
| ---: | --- | --- | ---: |
| 1 | Goal | First-layer columns 16 through 33 | 1,728 |
| 2 | Trunk | Other first-layer weights, first bias, second layer and bias | 56,832 |
| 4 | Actor | Final action-logit rows and their biases | 10,185 |
| 8 | Critic | Final scalar value row and its bias | 97 |

Generate all 16 combinations. Mixtures inherit **base counters only as provenance**;
these counters do not turn mixtures into trained checkpoints. Preserve the exact
donor separately with its own counters. The utility and its tests remain absent
from both runtime JARs.

The critic output is not an input to the action logits. Verify bit-exact action
probability invariance for each mask paired with its critic-bit toggle, over all
18 tasks, open/closed masks and scalar/batch evaluation. This is an analytical
negative control, **not** a claim that critic gradients cannot change the trunk
during training. Evaluate the eight behavior-distinct combinations (masks 0-7),
plus the exact unmodified donor, as the nine prespecified models.

Run every model on ordered tasks **0 through 11**, **32 cases per task**, with
full-difficulty, ordinary resets and frozen stochastic neural decisions. Use new
evaluation seeds **2026092931** and **2026092932** for every model. This produces
18 reports and **6,912 trials**; retain failed trials and the complete task order.
Use the unchanged current holdout implementation and one identical training
runtime JAR. No neural training is performed: accepted-sample budget is **zero**.
These two seeds are layout/sampling replications, not independent training runs.

Run at most four disposable loopback evaluation servers concurrently, using the
existing pinned Minecraft cache and the holdout runner's per-run deadline.
Keep the live development server and unrelated projects running. No world, input,
partial result or adverse policy is erased. A failed/incomplete report makes the
study incomplete; do not substitute a new seed, relax cases or report only a
successful subset. Check every input/output identity, all trial denominators,
positive observations, full completion and zero new training samples.

## Prespecified interpretation

Report all nine ordered score vectors on both seeds, not just a best hybrid.
The primary endpoint is task 10 workbench completion. For each of the three
behavioral blocks, report all four within-background donor-minus-base contrasts
and their mean on each seed. Report the factorial interactions as descriptive
contrasts, not as independent training evidence or adjusted significance tests.

For an individual contrast, call a change *material and repeated* only when its
sign agrees and its absolute difference is at least four successes out of 32 on
**each** seed. This is an engineering description, not a confidence guarantee.
Report the smaller and inconsistent contrasts too. First check whether the exact
parent/donor endpoints reproduce lower donor workbench performance; without that,
localisation of the earlier failure is inconclusive on these new tests.

Compare mask 7 with the exact donor as a value/counter negative control. Its actor
function is identical by construction, but real-server timing can vary between
runs. Report any disagreement rather than treating equal probabilities as proof
of identical server trajectories. Scores on tasks 0-9 and 11 are retained-skill
and unfinished-frontier context, not optional columns.

This intervention can localise sensitivity to blocks in this particular pair of
networks, including interactions. It cannot identify whether frontier examples,
review examples, critic loss, exploration pressure, Adam momentum or startup
allocation generated those changes. It does not show that freezing any block
would learn the next skill, nor that a high-scoring mixture should be deployed.
A subsequent learning intervention must preserve the already learned function
at initialization and separately measure transfer and retention. No mixture from
this diagnostic is adopted as the live model.

## Reproduction and evidence

Worktree: `/home/coder/workspace/botsclustersmc-retention-attribution` in the
operator-designated `lkjsxc/tomato-ocelot-73` workspace. Full results, generated
policies and execution receipts remain in its ignored `.build/` directory; source
and the measured record are committed. Local retention is not off-machine archival.
After `./test.sh`, the offline entrypoint is:

```sh
java -cp .build/tests:dist/training.jar \
  org.botsclustersmc.diagnostic.PolicyBlocks \
  BASE_POLICY DONOR_POLICY NEW_OUTPUT_DIRECTORY
```

The three path arguments must use separate input/output directories. Existing
output, symlinked managed paths and malformed policies are rejected. The final
`counterfactuals.json` manifest is written only after both inputs and all sixteen
policies are saved. Full-condition evaluation still uses `tests/holdout.py`;
its reset label remains ordinary, while the study-level parameter intervention
is explicitly diagnostic and is not exported as a skill certificate.

## Completed measurement

The implementation and prospective protocol were committed as
`0f4ce0fdf153b7f9edc0508e8621988d812a4358`, tree
`ccce7aab6456831ad29f44612f9a0f7c6eba69f2`, and published in PR #34 before
these evaluations began. All **18 reports / 6,912 trials** completed successfully,
including every failed gameplay trial, with **zero new training samples**.
An independent audit rechecked trial identities, task order, denominators,
positive observations, input/output/runtime identities and the factorial arithmetic.
The exact donor policy identity was
`5da37cbce86d73b036e99748e11e5309d206b7369bf8ba36d877e2b09a57e32f`.

The unchanged training runtime was
`7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc`;
the unchanged inference JAR was
`e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0`.
Both were byte-identical to the preceding control artifacts. The generated
counterfactuals were evaluated using the common runtime, not installed in the
live Academy. No training state, runtime configuration or service was changed.

### Workbench endpoint: all prespecified models

Every entry is successes out of **32**. A set mask bit selects the later donor
block; all other blocks remain from the parent. G, T and A mean goal, trunk and
actor respectively. Seeds A and B are **2026092931** and **2026092932**.

| Model | Later behavioral blocks | Seed A | Seed B |
| --- | --- | ---: | ---: |
| mask 0: exact parent | None | 28 | 31 |
| mask 1 | G | 29 | 31 |
| mask 2 | T | 23 | 19 |
| mask 3 | G + T | 22 | 18 |
| mask 4 | A | 30 | 30 |
| mask 5 | G + A | 30 | 30 |
| mask 6 | T + A | 24 | 20 |
| mask 7 | G + T + A | 23 | 21 |
| Exact donor, including its critic and counters | All | 23 | 22 |

The exact donor underperformed the exact parent by **5 and 9 successes** on the
two new seeds. Thus the endpoint retention loss reproduced; the earlier 31/32
parent scores are not substituted for these newly measured 28/32 and 31/32.
The parent is not perfect, and equal weights do not guarantee identical results
across different evaluation seeds.

For completeness, these are the full ordered scores for **tasks 0 through 11**.
Every denominator is 32; no retained task or unfinished frontier is omitted.

```text
Model        Seed A                                      Seed B
mask 0       32 32 32 32 32 32 32 32 32 32 28 0         32 32 31 32 32 32 32 32 32 32 31 0
mask 1       32 32 32 32 32 32 32 32 32 32 29 0         32 32 31 32 32 32 32 32 32 32 31 0
mask 2       32 31 32 32 32 32 32 31 32 32 23 0         32 32 32 31 32 32 32 32 32 32 19 0
mask 3       32 32 32 30 31 32 32 31 32 32 22 0         32 32 32 32 32 32 32 31 32 32 18 0
mask 4       32 31 32 32 32 32 32 32 32 32 30 0         32 32 32 32 32 32 32 32 32 32 30 0
mask 5       32 30 32 32 32 32 32 32 32 32 30 0         32 32 32 32 32 32 32 32 32 32 30 0
mask 6       32 32 32 30 32 32 32 32 32 32 24 0         32 32 32 32 32 32 32 32 32 32 20 0
mask 7       32 31 32 32 32 32 32 32 32 32 23 0         32 30 32 32 32 32 32 32 32 32 21 0
exact donor  32 31 32 31 32 32 32 32 32 32 23 0         32 32 32 32 32 32 32 32 32 32 22 0
```

Tasks 0-9 remained at least 30/32 in every report. Ordinary wooden-pickaxe task 11
was **0/32 in every report**. This study demonstrates no new skill acquisition.

### All matched block contrasts and interactions

Each contrast changes only the named behavioral block and leaves the other
blocks fixed. Values are donor minus parent in success counts out of 32.
Backgrounds are listed by the lower mask, without the selected bit.

| Changed block | Background masks | Seed A contrasts | Seed B contrasts | Mean A | Mean B |
| --- | --- | --- | --- | ---: | ---: |
| Goal | 0, 2, 4, 6 | +1, -1, 0, -1 | 0, -1, 0, +1 | -0.25 | 0.00 |
| Trunk | 0, 1, 4, 5 | **-5, -7, -6, -7** | **-12, -13, -10, -9** | **-6.25** | **-11.00** |
| Actor | 0, 1, 2, 3 | +2, +1, +1, +1 | -1, -1, +1, +3 | +1.25 | +0.50 |

All four trunk contrasts meet the prespecified material-and-repeated rule.
None of the goal or actor contrasts meets that rule. The trunk result does not
depend on choosing only one favorable background or averaging away an adverse seed.

The complete pair interactions are differences of differences, with the remaining
bit first unset and then set. The means are descriptive, not hypothesis-test
statistics; these contrasts share models/trials and are not independent samples.

| Interaction | Remaining background masks | Seed A values (mean) | Seed B values (mean) |
| --- | --- | --- | --- |
| G x T | 0, 4 | -2, -1 (-1.5) | -1, +1 (0.0) |
| G x A | 0, 2 | -1, 0 (-0.5) | 0, +2 (+1.0) |
| T x A | 0, 1 | -1, 0 (-0.5) | +2, +4 (+3.0) |

The three-way difference is +1 on seed A and +2 on seed B. These smaller
interactions do not erase the negative trunk effect in any measured background.
Parameter-distance statistics are retained in the generated manifest, but larger
block L2 distance is not treated as evidence of greater causal importance.

Mask 7 and the exact donor have the same actor function, while their critic and
provenance counters differ. Their workbench scores are 23 versus 23 on A and 21
versus 22 on B. Their complete score-vector differences (mask 7 minus donor)
are `[0,0,0,1,0,0,0,0,0,0,0,0]` and
`[0,-2,0,0,0,0,0,0,0,0,-1,0]`. This negative control shows real-run variation of
up to two successes on another task; it is not an estimate of all uncertainty.
It does not support attributing the small differences to value outputs. The
independent numerical test verifies exact actor-probability invariance.

### Post-hoc crafting traces

The unchanged read-only trace also distinguishes incorrect recipe collection
from failure to form or collect a workbench. In parent trials, observed stick
gains occurred in 2/32 and 1/32 trials, all failures. In the exact donor there were
**no observed stick gains on either seed**, despite 9/32 and 10/32 failures.
Workbench previews were reached in 25/32 and 23/32 donor trials, while only 23/32
and 22/32 completed. A previously observed tendency to make sticks is therefore
not a sufficient explanation for this donor's retention loss.

These are post-hoc observed transitions, not a prespecified adoption endpoint or
a complete inventory-event history. Absence of an observed stick gain is not
proof that no such event could occur between decision-boundary observations.

### Decision

**Accept the offline diagnostic and its evidence, not any hybrid model.** For this
particular continued model, the dominant measured sensitivity is in the shared
hidden representation, not merely the goal embedding or final action head.
This localises where the already learned function changed; it does not identify
which training examples, loss terms, optimizer momentum or startup schedule
caused that change. Critic-output invariance during inference does not exonerate
critic gradients during training through the shared trunk.

The next learning design should preserve the old function at initialization
**and protect it throughout new-skill updates**, while giving the frontier its
own learnable capacity and retaining access to prior features. Merely copying
old weights and then updating the same shared trunk does not meet that condition.
Freezing everything would prevent interference but would not establish useful
new learning. A warm, function-preserving modular candidate is a more directly
motivated next experiment than another placement-exposure increase or a repeat
of the rejected cold expert bank. It still needs a separately declared training
budget, unchanged full-condition tests, retained-skill gates and a memory bound
for concurrent inference/frozen exams. No such candidate is implemented here.

Do not equate retention loss with proven loss of plasticity: the former measures
old performance, while the latter concerns the ability to learn new tasks. This
study measures the former and does not establish the latter.

### Evidence retained

The worktree retains `.build/attribution-policies/` (both exact source files,
all sixteen policies and final manifest), `.build/attribution-evaluation/`
(all eighteen complete reports, worlds, logs, metadata, start/finish receipts,
prospective declaration and `completed.json`), and the local runner/auditor
`.build/attribution_study.py` and `.build/attribution_audit.py`.
The auditor also wrote `.build/attribution-public-summary.json` with all ordered
scores, input identities, block statistics, contrasts and post-hoc trace summaries.
Both original preceding-study policy files were rechecked and remained unchanged.
These local paths are not public downloads or permanent off-machine archival.

### Source verification and final correction

The full local `./test.sh` suite passed before measurement and again after the
final diagnostic correction. The new block tests execute **2,840,441 assertions**:
all parameter choices, signed zeros, scalar/batch equality, all task masks,
critic-only action invariance, file identities, rejection boundaries and absence
from both runtime JARs. These are numerical/data-boundary checks, not additional
Minecraft skill trials. The local factorial auditor separately passed an
independent polynomial oracle containing main, pair and three-way effects with
both signs before auditing every saved gameplay result.

After all evaluations finished, a new Arabic-format-locale regression test
reproduced non-ASCII digits in the diagnostic JSON counters and failed at
`locale-independent base updates`. Commit
`9f4b5baab6fa033ac92d5284e5f14cc569efadbb` makes that manifest use `Locale.ROOT`
and adds four counter checks. Regenerating both retained inputs and all sixteen
policies into a new directory produced a **byte-identical nineteen-file output**
to the evaluated output, including the manifest. Thus this correction does not
change any measured policy or require replacing any report.

The final tests also confirmed that `core/`, `plugin/`, `training/`, `host/`,
`tests/holdout/` and `tests/holdout.py` are unchanged from the declared mainline
base. Rebuilt training/inference JAR identities are the same as those recorded
above. The implementation therefore requires no live-model replacement or
training-server restart. A combined auxiliary process/status inspection was
blocked by the tool service and was not retried; it is not evidence of a fresh
live health check. The experiment and its completed results were unaffected.
