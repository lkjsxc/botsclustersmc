# Workbench baseline and activation diagnostics — 2026-09-25 JST

## Operator environment inspected

The authorized machine was `cw-c76e701404f844d8876a4d50`. The active source
checkout was `/home/coder/workspace/botsclustersmc-source`, not the older
`/home/coder/workspace/botsclustersmc` path. It was clean at
`bb6f4585d114212f944b2e2ac48873caec48e0b6` (including the prior crafting
curriculum change).

A runtime status snapshot around 06:35 JST reported 1,024 active, ticking and
moved NPC bodies, no inference failures/rejections and no burning bodies. All
actors' current course stage was 10, craft-workbench. These are runtime counts,
not 1,024 logged-in players or 1,024 successful autonomous survivors.

The same inspection found a large difference between assisted completion and
material placement. Task-10 practice counters, successful/finished attempts by
missing recipe cells at reset, were:

| Missing cells | Successful / finished |
| --- | --- |
| 0 | 19,423 / 19,672 |
| 1 | 3,732 / 31,603 |
| 2 | 370 / 19,580 |
| 3 | 28 / 5,533 |
| 4 | 1 / 628 |

These are process-local assisted-training outcomes, not independent evaluation
or causal evidence about the model. Zero missing cells still requires the policy
to collect the preview; supplied materials/layout are assistance, not learned
placement. The existing completion EMA and task certificates must not substitute
for an independently frozen test.

## Completed independent baseline

The launcher ran this command after existing operator EULA acceptance had been
confirmed in the running launcher environment and owned server configuration:

```sh
cd /home/coder/workspace/botsclustersmc-source
EULA=true ./evaluate.sh --tasks 8,9,10 --cases 32 --seed 2026092501 \
  --export dist/baseline-20260925.zip
```

The evaluation process completed with exit code 0 (reported duration 163.49
seconds). Its unchanged tested policy was **258878**, with **117,597,674** trained
samples. The evaluator used the existing independent disposable Folia rooms and
primitive action path; the frozen policy did not learn inside the evaluation.

| Task | Passed / cases |
| --- | --- |
| 8 — Craft planks | 32 / 32 |
| 9 — Craft sticks | 32 / 32 |
| 10 — Craft workbench | 0 / 32 |

This is a result for this particular snapshot, seed and task suite. It is not a
claim about every later live policy, statistically universal success/failure, or
open-world survival/cooperation. The command saved the **tested** deployment
bundle, not an arbitrary later live checkpoint, at:

`/home/coder/workspace/botsclustersmc-source/dist/baseline-20260925.zip`

The evaluator reported its summary and details under
`academy/server/plugins/BotsClustersMC/evaluation.json` and
`evaluation-details.json`. The command's completion/result output was read;
a subsequent request to summarize the detail file was refused by the remote
execution safety check. No per-click detail analysis or downloaded local copy
of that remote bundle is claimed here.

## Hypothesis, not diagnosis

Two `/bots inspect` commands re-evaluated cached observations with live policies:
actor 0 at policy 259186 displayed first/second-layer saturation of 25%/79%, and
actor 257 at policy 259189 displayed 25%/83%. These are **two observations**, not
a population-wide distribution, longitudinal study, or proof that saturation
caused the workbench failures. They motivate measuring representative learner
batches before selecting an optimizer/representation intervention.

The subsequent source change therefore adds read-only per-task activation
measurements, as specified in [Activation health](../ACTIVATION_HEALTH.md).
It does not introduce an activation penalty or change the learner objective.

## Delivery and limits

The implementation candidate `e9c177d3cfd5042261e12880a8a27e37d235fe2e` was
merged through [PR #8](https://github.com/lkjsxc/botsclustersmc/pull/8) as main
commit `307f7badcbb1f50c031367ae34b4fde4bdb62815`. Its ten source/test files
were checked against the isolated local candidate bytes before publication.
The merged main tree is exactly the tested candidate tree.

Local completed checks at initial publication: 185 standalone diagnostic
arithmetic/immutability/partition assertions, JavaScript syntax validation, and
the existing crafting dashboard suite plus 55 invalid activation input cases,
with desktop/mobile fixtures and no browser errors. Full Java API compilation
and learner integration are separate CI results, not implied by those checks.

[Java runtime validation 36066072274](https://github.com/lkjsxc/botsclustersmc/actions/runs/36066072274)
completed successfully on the candidate: Linux source/API compilation, Windows
source/API compilation and the offline observatory regression all passed.
The Linux source log explicitly reports **185** activation-arithmetic checks and
**904** instrumented/uninstrumented gradient/Adam and learner integration checks.
Existing source tests, checkpoint/export separation checks and five synthetic
bandit seeds also passed. Live Minecraft fixtures were API-compiled, **not run**
by this PR workflow. Neither those synthetic tests nor the browser screenshots
are evidence of candidate skill improvement.

Remote code editing was refused by a safety check before any edit was applied;
a later read-only shell call was also refused. No alternative route was used to
perform the refused remote operations. The diagnostics source work and CI are
separate from that machine. **No candidate plugin was deployed, no server was
restarted, and no model/world reset was performed.** The baseline evaluation
export above completed before those restrictions. An empty development worktree
was created, but it contains no implementation changes.

No new workbench skill, saturation reduction, 1,024-agent candidate throughput,
or survival/cooperation improvement is claimed. The next empirical step is to
collect per-task diagnostics after an authorized deployment and compare a
single justified intervention against the saved baseline with independent
full-reset crafting tests and retained-earlier-skill tests. Do not replace that
comparison with a prettier dashboard, easier certification, or more actors.
