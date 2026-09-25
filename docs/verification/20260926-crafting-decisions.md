# 2026-09-26 — frozen crafting decisions

## Scope

This is a measurement change, not a learned-crafting improvement. The native
evaluator now identifies correct ingredient cells, wrong placements, cell changes,
menu exits and exact joint click/slot probabilities. Production training,
inference, observations, mechanical masks, rewards, reset assistance, optimizer
and certification requirements are unchanged.

See [the diagnostic contract](../CRAFTING_DIAGNOSTICS.md) for field meanings,
denominators and limits. NPC mechanics are not full vanilla-player semantics.

## Source and tests

Baseline: `e9e31065c857a8c647919ad875b83c2116861055`.

Implementation: `00cb39562c1f2e5d7f3ddbd640dd879f5bb1f980`, with exact tested
tree `cdc1bb0d897d829d1147f36afb793ea15236f372`.
[PR #12](https://github.com/lkjsxc/botsclustersmc/pull/12) integrates the same tree
as `163a15e9b20451002b59ff79b683a4d6ea1ce61d`.

The machine used was the operator's designated `cw-c76e701404f844d8876a4d50`
(Home Coder workspace `minecraft-agents`), with the pinned Folia 1.21.11 build 14,
Java 21 and Linux amd64. The JVM has 16 available processors on the Ryzen 9 9955HX
host. Work was isolated in `/home/coder/workspace/botsclustersmc-crafting-trace`;
the live source checkout is `/home/coder/workspace/botsclustersmc-source`.

Completed checks:

- Full `./test.sh` passed, including **65,864** new diagnostic assertions and
  the existing math, mechanics, curriculum, persistence, concurrency and
  source-launcher/export checks. The normal test command now also compiles the
  real-API holdout harness.
- The new test covers 7,776 synthetic grid configurations (two pickaxe tasks,
  243 ingredient-state combinations and 16 extra-cell patterns), actual Pocket
  clicks, correct/wrong/surplus counts, preview separation and malformed input.
- Across 256 seeded sampling checks, the diagnostic preserved observations,
  masks, actions, random-stream state, frozen probabilities, policy weights and
  policy counters. Deliberately mismatched applied likelihoods were rejected.
- Three isolated mutation controls failed for the expected reason: ignoring
  wrong cells; omitting the parent click probability; disabling the applied
  likelihood identity check. The conditional-only mutation produced 2.0 where
  the fixture requires a joint mass of 0.75.
- `dist/training.jar` **and** `dist/botsclustersmc.jar` compared byte-identical
  with the baseline, before and after the live evaluation.
- [PR CI 36199850226](https://github.com/lkjsxc/botsclustersmc/actions/runs/36199850226)
  passed Linux/Windows source checks and the synthetic browser observatory tests.
  Optional dispatch-only server jobs were skipped, not counted as passes.

Assertions and synthetic grids are not independently learned Minecraft trials.
The actual server experiment below is reported separately.

## Complete frozen-policy server experiment

The ordinary native evaluator was run with the following explicit configuration
from the verification worktree, using the existing EULA consent and a snapshot
of the live Academy's canonical checkpoint:

```sh
EULA=true \
ACADEMY=/home/coder/workspace/botsclustersmc-source/academy-current \
BCMC_SERVER_CACHE=/home/coder/workspace/botsclustersmc-source/.cache/server \
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10,11 --cases 32 \
  --seed 2026092605 --export .build/pickaxe-trace.zip
```

The model was frozen at update **581,011**, with **272,564,721** accepted training
samples. The evaluator performed **zero** new training samples. All **384**
stochastic full-condition trials completed; all failures were retained. The
result timestamp was **2026-09-26 07:57:49.868 JST**.

Tasks 0–10 each scored **32/32**: forward-stop, turn-stop, aim-hold,
navigate-stop, step-over, break-log, collect-log, place-block, craft-planks,
craft-sticks and craft-workbench. Wooden-pickaxe task 11 scored **0/32**.

These are bounded Academy-room results, not unrestricted survival or cooperative
settlement. No curriculum intervention, teacher action, scripted recipe or easier
reset was introduced into the evaluated trials.

## What the pickaxe trace measured

The 32 failed pickaxe trials contained 19,200 action transitions.
**27 trials never displayed any correctly occupied workbench ingredient cell;
the other five displayed at most one.** None displayed two or more correct cells
at once, and none reached a target output preview.

There were only **211 pre-action workbench states**. The existing post-action menu
counter separately reported 4,372 closed-menu, 14,617 personal-inventory and
211 workbench observations; furnace/chest counts were zero. The coincidence of
pre/post workbench totals does not make their timing interchangeable.

Twelve trials displayed wrong occupied cells. No trial displayed surplus units
in an otherwise correct workbench ingredient cell. The pre-action correct-cell
mask histogram was: mask 0 = 184, mask 1 = 2, mask 2 = 9, mask 8 = 16; all other
masks were zero. Slots and bit meanings are in the diagnostic contract.

The current NPC pocket retains its grid across menu switches. A workbench entry
can therefore reveal existing material rather than represent a new workbench
placement. Cell-fill/removal counters compare only transitions whose **two**
frames are workbench states. This distinction explains why observed maxima need
not be reconstructible by summing those transitions.

### Placement opportunities

There were **98** pre-action states with a cursor carrying a matching ingredient
and at least one compatible empty recipe cell. Across those states, the mean
joint probability of filling any such cell was **0.0270579578**, or about **2.71%**.

This is a mean of policy probabilities on 98 correlated visited states, **not**
a 2.71% crafting-success estimate and not 98 independent trials. It includes
placing a whole stack, which may deprive other cells of materials; it is not
an optimal-action oracle.

| Workbench slot | Compatible-cursor states | Mean fill probability | Mean single-unit probability | Observed fills | Observed removals |
| --- | ---: | ---: | ---: | ---: | ---: |
| 36, upper left | 48 | 0.013618 | 0.012858 | 0 | 0 |
| 37, upper center | 48 | 0.017065 | 0.016172 | 0 | 1 |
| 38, upper right | 48 | 0.011855 | 0.011199 | 0 | 0 |
| 40, middle center | 45 | 0.008897 | 0.007864 | 2 | 1 |
| 43, lower center | 50 | 0.004190 | 0.003741 | 0 | 0 |

The five denominators overlap; they must not be added to produce the union
denominator of 98. Every pair mass is `P(click type) * P(slot | click type)`,
using the same frozen observation and mechanical mask as the applied action.

### Exits and output collection

The policy selected close 31 times in pre-action workbench states. Mean close
probability was **0.1392363626 over all 211 workbench states**. Four transitions
left the workbench with one to four correct cells. Exits include mechanical
invalidation as well as chosen closes; these are distinct counters.

The close-probability denominator differs from the compatible-placement denominator.
Do not compare 13.92% and 2.71% as though they were alternatives measured on an
identical state set. A removed ingredient can also be legitimate recipe
consumption; the generic diagnostic does not label every removal a mistake.

Because the target preview was never present, the probability of collecting
a completed pickaxe is **unmeasured**, not zero. These trials cannot diagnose
collection behavior once a complete recipe exists.

## Interpretation and preservation

The observations narrow the failure to much earlier than the final recipe cells:
full-condition behavior rarely remained in the workbench, and even visited states
with a usable cursor assigned little mass to compatible empty cells. They do
not isolate representation, optimizer, reward shaping, curriculum or exploration
as the causal defect. No speculative learner or reward change was deployed.

The exact evaluated weights, matching inference plugin, full 384-trial report,
and bounded summary remain in the verification worktree:

```text
.build/pickaxe-trace.zip
.build/pickaxe-trace-summary.json
.build/crafting-mutations/result.json
```

Full source and evaluator logs were copied into `.build/evidence/crafting-decisions/`.
The summary audit checked ZIP integrity, the policy/report identity, all 384
unique trials, per-trial trace counts, mask-histogram totals and probability
bounds. The detailed report remained within the existing 2 MiB bound.
The native evaluator also published the new completed result to the live
observatory; the displayed result belongs to policy 581,011, not a later live
checkpoint.

At **2026-09-26 08:10:01.125 JST**, the separately read live status showed
policy 588,761 with 276,243,353 accepted samples; all 1,024 actors were active,
ticking and had progressed since the prior status. Inference failures/rejections,
rejected/stale learner samples and burning bodies were all zero. The measured
status interval showed 4,076.84 accepted samples/s and 1.234 CPU cores used.
All actors remained at stage 11. This is a live snapshot, not a sustained-rate
guarantee or a skill certificate.

The live checkout was fast-forwarded to the merged source without restarting the
learner or monitor: neither production JAR nor their source directories changed.
No weights, Adam state, Academy configuration, port or historical trial files
were reset. Earlier backups and exact-model bundles were retained.

The next controlled question is to separate reaching/remaining in the correct
workbench from manipulating raw ingredients after entry, while holding the policy
fixed and retaining the original full-condition exam as the success criterion.
Any assisted diagnostic condition must be labeled separately and cannot replace
the ordinary exam. A new architecture or reward scale should be justified against
these specific failure modes and re-tested for retention of earlier skills.
