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
