# Preserved critic-detachment actor: frozen representation/output attribution

Date: 2026-10-02 (Japan). Prospective protocol, fixed before new gameplay.
This is a finite parameter-sensitivity diagnosis, not another learning experiment,
a deployable policy, or a claim that actor gradients are the cause of forgetting.

## Question and unchanged boundary

The completed [critic-feature detachment](20260930-critic-detachment.md) preserved
tasks 0-10 but lost ordinary wooden-pickaxe crafting. The same preserved parent
and stopped candidate are used here; neither a moving live model nor an older,
more favorable baseline is substituted. No training runs in this study.

The existing offline `PolicyBlocks` tool partitions every weight into GOAL,
TRUNK, ACTOR and CRITIC. Representation means GOAL plus TRUNK together, including
the goal input columns and both hidden layers. Output means all final actor rows
and biases. The final critic row is held at parent weights in all three hybrids;
its predictions can still change when features change. It does not choose frozen
policy actions. These hybrids do not identify individual layers, gradient terms,
losses, Adam moments, or independent causes.

| Policy | Representation | Actor output | Critic row | Counters |
| --- | --- | --- | --- | --- |
| Original parent | Parent | Parent | Parent | Parent |
| Original stopped candidate | Candidate | Candidate | Candidate | Candidate |
| Representation-only, mask 3 | Candidate | Parent | Parent | Parent |
| Output-only, mask 4 | Parent | Candidate | Parent | Parent |
| Actor combined, mask 7 | Candidate | Candidate | Parent | Parent |

All 16 masks are generated for exact parameter-support validation, but only the
five policies above are evaluated. The original candidate and mask 7 intentionally
share all actor weights and features; their logits must agree numerically. Their
separate physical runs are a control, not assumed deterministic Minecraft replay.
The inherited counters are provenance labels, not newly earned training progress.

## Source identities

Accepted source at entry: `08a9963dd10a125310af6c31f61f6e7c5c1a764f`.
No core, plugin, training, host, holdout or ordinary block-compositor source changes.
Every evaluator uses the exact runtime from the preceding critic diagnosis,
SHA-256 `782fb3bcde791745672b5d1433fa3f3efb190737dc594578bbc796079e8233c3`.
That research runtime contains the stronger reward but no learning runs here;
physical controls, reset distribution, observations and terminal predicates are
unchanged. No experimental runtime is installed on the development Academy.

| Input | SHA-256 |
| --- | --- |
| Parent policy | `ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc` |
| Parent canonical model/Adam/course | `a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e` |
| Stopped candidate policy | `bd8fc04cc2e1e5b770cffecd6e833b3c26020d1af41c40c3652fc3cf10cff9b9` |
| Stopped candidate canonical model/Adam/course | `ea196e219c8fd0d1ddcf3156df6811dfb57832cd94cbcdbb367266c11066fb40` |

Both canonical states remain untouched. Every original and mixture policy is
copied into create-only evidence storage with a digest manifest. Native validation
uses a separate range-copy oracle on every actual parameter of all 16 masks,
verifies inherited counters, and checks candidate/mask-7 actor-logit equality on
256 fixed synthetic observations. Synthetic checks are not gameplay evidence.

## Complete, finite protocol

Two fresh evaluation seeds: **2026100201 and 2026100202**. Ordinary ordered tasks
**0-12**, **32 trials per task**, `reset_intervention=none`, unchanged stochastic
primitive controls. First run all four original parent/candidate reports.
Qualification requires parent tasks 0-10 at least 28/32 and task 11 at least 26/32
on each seed; candidate tasks 0-10 at least 28/32 and a task-11 loss of at least
8/32 against its seed-matched parent. A failed qualification stops all hybrids
and is reported without changing seeds or floors.

If qualified, complete all six hybrid reports, including every earlier task and
mining. Maximum **10 reports / 4,160 frozen trial executions**, **zero new training
samples**, four loopback evaluators, two visible processors each, existing bounded
holdout lifetimes, ports 31501-31510. At least 10 GiB currently available memory is
required before each evaluator. Retain incomplete logs on failure; do not replace
a failed run silently or label an incomplete matrix complete.

Per-task preservation floors are `max(28, parent - 3)` for tasks 0-10,
`max(26, parent - 4)` for task 11, and `max(0, parent - 2)` for task 12, separately
on both seeds. These are engineering screens, not confidence intervals.

Representation sensitivity requires a task-11 loss of at least 8/32 under **both**
actor backgrounds: parent minus mask 3, and mask 4 minus mask 7, on **both** seeds.
Output sensitivity uses parent minus mask 4, and mask 3 minus mask 7, with the
same separate thresholds. Also report output-only and representation-only
preservation and all interactions; do not pool inconsistent directions.
Original candidate versus mask 7 is reported as a physical-run comparison,
without imposing score equality after the numerical actor-equivalence check.

A positive sensitivity screen does not identify the responsible training signal
or authorize using a hybrid. Actual mining completions, target contact, broken
blocks and generic item pickups remain distinct. No outcome automatically
triggers training, a larger budget, deployment, or changed acceptance criteria.

## Engineering and execution

The controller is research-only under `tests/studies`. Offline regressions cover
full matrices, strict types, duplicate/missing cases, both seeds and backgrounds,
input rejection, per-task retention, immutable inputs, create-only output,
symlink rejection, and an explicit EULA-gated CLI. The full local source suite,
normal and optimized controller tests, and numerical checks must pass before
new gameplay. Source is committed and pushed prospectively.

```sh
EULA=true python3 tests/studies/actor_drift.py --run
```

The owned evidence root is `.build/actor-drift-study/`. Every completed report is
bound to its policy, runtime, complete executed configuration and exact evaluator
payload. All original reports, receipts, failures and counterfactual identities
are retained. The shared mainline Academy and unrelated projects are not stopped
or changed. Results are appended only after the declared execution.
