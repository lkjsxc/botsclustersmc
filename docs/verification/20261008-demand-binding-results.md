# Full demand-inventory products: complete fresh comparison, gate failed

## Decision and scope

The generic tensor-product candidate is not a reliable communal-supply learner
under the declared budget. All three independently learned pairs failed every
compound gate. Its nominal two-member improvement over its matched plain-input
control was only 1.171875-5.859375 percentage points, not the required 20 points.
No candidate is adopted, no budget is extended and no Minecraft runtime is changed.

This is the second separately predeclared synthetic experiment in PR #72, not a
continuation that overwrites the [earlier common-reward failure](20261008-communal-supply-results.md).
The useful deliverable is a bounded relational-input implementation and a complete
negative comparison using the existing neural learning math and inventory code.
It does not establish learned Minecraft cooperation or durable communal life.

## Prospective source and fixed execution

Prior rejected implementation: `4fc06837e29c2d1e3c02c10dcd5314ba2d297dbe`.
Prior complete result commit: `7873455dc40c8ccbc1314d750d3194c57fcddba5`.
New prospective plan commit: `fb386bc`.
Frozen tensor/plain implementation: `8121c321791bd18ca87953a6a02dc4947b59dd15`.
Tree: `c00825fba933ca88db3138598957926e60eb9cb3`.
The [new plan](20261008-demand-binding-plan.md) was committed before implementing
or executing this comparison. Both plans and both failures remain separately named.

Fresh learning seeds were 2026100811, 2026100812 and 2026100813; fresh evaluation
seeds were 2026100891 and 2026100892. Each tensor/plain pair shared initial weights,
resource schedules and application-order keys. Both arms observed public demand.
All six arms completed 600 update attempts with 256 offered transitions per batch:
153,600 per arm, 921,600 total. Accepted optimizer/sample identities are retained
in the accompanying JSON rather than inferred from the number of offered batches.

At attempts 0, 100, 300 and 600, the immutable policy was evaluated at populations
2 and 8 on both fixed evaluation seeds, with 256 cases per combination. All
24,576 frozen synthetic trials completed. Earlier checkpoints, failed cases and
secondary endpoints remain in the full report; there was no outcome-based
stopping, additional training, replacement seed or threshold change.

## What changed, and what did not

`TensorInputs.bind` appends the FULL Cartesian product of 108 own-inventory
features with the two public demand features: 216 floats in reserved synthetic
columns 220 through 435. Every pair is present, including unequal material pairs
and count components. There is no equality test, selected recipe/material/slot,
preferred action, imitation target or per-citizen reward inserted by this code.
It is a deterministic transformation of information the actor already observes,
not a source of another actor's private information.

The plain control leaves those columns zero. Both arms use exactly the same
network size, initialization, reward, four-step horizon, actual Pocket operations,
mechanical masks, sampling semantics, Gradient, VTrace, UpdateGuard and Adam.
Current and next observations follow the same treatment. Reserved-column
contamination and non-finite products are rejected before returning a new array.

The observation identity is explicitly
`synthetic-communal-tensor-binding-memory-only`. Models are never serialized as
Minecraft policies. The original plain/hidden study remains callable with its
unchanged default representation. Its complete old evidence, replayed through
the new explicitly profiled validator, produced byte-identical result JSON.
The checker does not select a profile by guessing from evidence or by changing
global seed/arm constants. Cross-profile evidence is rejected.

This feature treatment also changes input magnitudes and optimization geometry.
The experiment does not distinguish relational expressiveness from those effects;
a small observed advantage is not a unique causal diagnosis of the original
learning bottleneck. There is no sham-product or same-norm representation control
in this declared comparison, and one is not added after seeing these results.

## Final primary results

Primary success means BOTH requested material batches were irreversibly consumed
within the FIRST TWO joint steps. A and B below are evaluation seeds 2026100891
and 2026100892 respectively; every denominator is 256.

| Independent learning seed | Tensor, 2 members A / B | Plain, 2 members A / B | Tensor, 8 members A / B |
| --- | --- | --- | --- |
| 2026100811 | 133/256 / 121/256 | 119/256 / 107/256 | 52/256 / 81/256 |
| 2026100812 | 136/256 / 120/256 | 121/256 / 111/256 | 61/256 / 97/256 |
| 2026100813 | 129/256 / 117/256 | 120/256 / 114/256 | 72/256 / 100/256 |

Tensor two-member success was 45.70%-53.13%, versus 41.80%-47.27% in its matched
plain controls. Every tensor result missed 90%; every paired difference missed
20 percentage points. Eight-member success was 20.31%-39.06%, missing 80% on
both evaluation seeds for every learned policy. All three compound gates failed.
These new control scores must not be compared as if they shared evaluation or
learning seeds with the prior visible/hidden experiment.

The machine-readable report separately retains four-step completion, first-step
service, wrong-material deposits, consumed stock, remaining private stock and
bank stock at every checkpoint. Raw cases include every member's selected
primitive actions, serial application orders and material-specific transfers.
A favorable secondary count would not replace the failed primary endpoint.

## Software and evidence qualification

The exact frozen source passed the complete local Java/API suite with exit zero.
Supply mechanics retained 1,351,423 checks, including 262,144 exhaustive short
histories. Full tensor-product tests passed 145,038 checks. A separately compiled
fixed-policy wiring fixture checked 720 actual before/next transition encodings
and initial plain/tensor mask equality without training. Python tests passed
58/58 normally and 58/58 under -O.

Five intentionally incorrect tensor implementations compiled and failed their
intended assertions: no products, equal-pairs-only products, aliasing the source,
overwriting the bias and ignoring reserved-column contamination. The unmodified
positive passed the same harness. Compilation failure was not counted as detection.
All scratch sources and exact diagnostic logs remain under
`.build/qualification/tensor-mutations/`.

The independent integer checker reconstructed every complete trial without
calling Java/Pocket. It validated exact initial schedules, legal sampled support,
random application order, serialized transfers, irreversible consumption and
material conservation, then recomputed all aggregate metrics and gates. Full
normal and -O replays exited zero with byte-identical reports.

A positive full-matrix check was additionally required before six corruptions:
missing final case, duplicate case, forged consumed stock, changed frozen-policy
hash, a missing update and a wrong study profile. Each was rejected for its
intended reason. The original raw files were not mutated or overwritten.
These checks validate supplied histories and source bindings; no exported weights
exist for independent neural action-probability reconstruction or arbitrary-file
authentication. That limitation is not hidden by a policy digest.

Both public runtime JARs are unchanged and exclude all synthetic study classes:

- Inference: `1e9c88cd12587f9a6944acc90169e819bd17db493e1be389178e3db4ee9cef36`.
- Training: `aae1fb27130b8c520dfc89f0de1cff7eab3c18febd95c043bfdef989e700f826`.

The new source has no changes to core/plugin/training/runtime resources or the
pinned Minecraft distribution. The host change registers pure tests only.
No compatibility wrapper, deployed observation change or learner replacement is
hidden in this source-only research PR.

## CI and execution boundaries

The first implementation's CI run 37669203959 completed with a CANCELLED overall
conclusion, not a pass. Its Linux and Windows source jobs passed. The available
observatory log shows Playwright installed, followed by delayed/retried Ubuntu
package-index reads during native dependency preparation, then cancellation.
It contains no successful browser fixture run. Do not infer browser correctness
or a uniquely established infrastructure root cause from that cancellation.

For the tensor source, run 37671423996 was independently observed with Linux and
Windows source jobs successful; the browser job was still preparing dependencies
at that observation. It subsequently completed with overall conclusion CANCELLED, also not a full pass.
Optional Minecraft/retention jobs were not requested and remain skipped, not passed.

The initial combined shared-environment state check and the later optional
first-study conditional-donation diagnostic were blocked earlier in this turn.
Neither was retried, split or obtained elsewhere. This second experiment was a
new, explicitly declared source-only action, not recovery of either blocked read.
No shared Academy, saved deployment model, service status, restart or deployment
was accessed for it. No current live-health claim is made from historical results.

## Next engineering decision

Neither shared outcome reward alone nor this full relational feature expansion
has met a reliable supply gate. Keep both as measured baselines. A subsequent
experiment should separate action credit, temporal return assignment and policy
conditioning rather than strengthen rewards or increase population by assumption.
Any such change needs its own declared control, independent seeds and budget.
It is not permission to continue either completed run until a convenient success.

The inventory-only fixture supplies open menus, a tiny action subspace, short
synchronous lives, balanced initial materials and an artificial public consumer.
Physical gathering, crafting, hunger, durable identities, real Folia scheduling,
continuous replenishment and partner replacement remain unqualified. PR #72 is
kept as an unmerged research draft; neither synthetic policy is a deployment file.


## CI follow-up after both completed experiments

Both frozen source runs passed their Linux/Windows source jobs but did not finish
the browser job successfully. The post-study workflow now runs ALL independent
Python evidence tests normally and under -O BEFORE browser dependency installation.
Their separate logs are uploaded even if browser preparation fails. Browser
fixtures remain required in their own following step; no assertion, failure exit,
package pin, EULA gate or overall job deadline was relaxed.

The disposable GitHub runner's APT HTTP/HTTPS connection and data timeout is set
to 15 seconds with one retry. This bounds a transport stall rather than treating
unavailable dependencies as installed. Repository mirrors, signature verification,
TLS trust and runtime-host configuration are unchanged. This is a CI-only
operational follow-up, not a new learning treatment or a deployment. Actual
final-source browser execution must still be observed separately.

APT settings reference:
https://manpages.ubuntu.com/manpages/noble/man5/apt.conf.5.html
https://manpages.ubuntu.com/manpages/noble/man1/apt-transport-http.1.html
Playwright CI reference:
https://playwright.dev/python/docs/ci
