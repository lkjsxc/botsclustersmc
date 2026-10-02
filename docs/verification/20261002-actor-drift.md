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

## Completed result

The prospective implementation and declaration were committed and pushed as
`64afa5e3c3524478a9df7bd1cf85944cd2d142d2`, tree
`2551ad220508146bd9051f1e1d37cb5036ca7753`, before gameplay. Research PR **#54**
preserves that source. The command above belongs to that research worktree;
accepted mainline retains this record, not the study controller or hybrid policies.

The local source suite completed, the 13 controller regressions passed in normal
and optimized Python, and the independent native oracle passed **1,155,507**
actual-weight/counter/logit checks over all 16 masks before evaluation. Both
original policies also passed native `CheckpointTool verify-export` against their
unchanged canonical model/Adam/course states. Source CI **36985672553** passed
Ubuntu, Windows and observatory; its optional retention and live-server jobs were
skipped, not counted as live evidence.

All four original reports qualified on the declared new seeds. All six hybrid
reports then completed: **10 reports / 4,160 frozen trial executions**, zero new
training samples, no reset assistance, no retries, and no changed thresholds.
A separate read-only closeout reconstructed every receipt, independently counted
32 trials per task per report, checked all source and mixture digests, reproduced
the declared decision, and checked identical non-configuration evaluator payloads.
No failure file was produced. All ten loopback evaluator ports were closed after
completion; no study server remains running.

### Complete ordinary task matrix

Columns are success counts out of 32, ordered **tasks 0-12**. A and B denote
seeds 2026100201 and 2026100202 respectively. These are stochastic physical trials,
not a deterministic replay or independent training replicas.

| Policy / seed | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Parent A | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 30 | 32 | 32 | 32 | 27 | 0 |
| Parent B | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 30 | 0 |
| Original candidate A | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 31 | 32 | 32 | 31 | 8 | 0 |
| Original candidate B | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 11 | 0 |
| Representation only A, mask 3 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 24 | 0 |
| Representation only B, mask 3 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 29 | 0 |
| Output only A, mask 4 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 17 | 0 |
| Output only B, mask 4 | 32 | 32 | 31 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 15 | 0 |
| Actor combined A, mask 7 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 31 | 8 | 0 |
| Actor combined B, mask 7 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 32 | 9 | 0 |

### Declared sensitivity and retention screens

Output replacement reduced task-11 success under **both representation
backgrounds on both seeds**: parent minus mask 4 was **10 and 15**, and mask 3
minus mask 7 was **16 and 20** successes. All four differences met the declared
8/32 threshold. This is a positive output-parameter sensitivity screen for this
preserved pair. It does not isolate an output head, a gradient term, an optimizer
moment, or a general source of forgetting.

Representation replacement did **not** meet its consistency screen: parent minus
mask 3 was **3 and 1**, and mask 4 minus mask 7 was **9 and 6**. Do not replace the
failed two-background/two-seed requirement with a favorable pooled difference.
This result also does not show that representation changes are harmless:
representation-only success of **24/32** missed seed A's **26/32** preservation
floor, although **29/32** passed on B. Output-only failed on both seeds. All tasks
0-10 retained their floors, but **no hybrid retained the full required skill set
on both seeds**.

Mask 7 and the original candidate had bit-identical actor logits in the native
check. Their independent physical pickaxe runs were **8/32 versus 8/32** on A
and **9/32 versus 11/32** on B. These bounded physical differences were retained,
not relabelled as an exact replay. In particular, final critic-row replacement
cannot be inferred to improve crafting from these separate physical runs.

### Mining remains unacquired

Every policy scored **0/32 mining** on each seed and recorded **zero broken
blocks**. Every report had 19,200 task-12 decision observations. The number of
selected effective world-dig inputs was:

| Policy | Seed A | Seed B |
| --- | ---: | ---: |
| Parent | 811 | 753 |
| Original candidate | 25 | 28 |
| Representation only | 832 | 793 |
| Output only | 30 | 29 |
| Actor combined | 24 | 30 |

Thus the loss of world-dig selections also followed candidate output replacement
in these frozen comparisons. This is not mining success: all hybrids had zero
observed pickaxe target contacts and zero block breaks. The parent had zero
contacts on A and only two observed contact states, with a maximum of three
continuous pickaxe-contact ticks, on B. Generic item pickups are archived
separately and are not labelled cobblestone acquisition.

### Preserved evidence and decision

Outcome SHA-256: `bdb95f234324651dced601ce1fba784bf3262e552922a6d3d807c0128835a3e2`.
The unpublished research draft **`actor-drift-study-20261002`** holds
**`actor-drift-evidence-20261002.zip`**, 13,003,913 bytes, 130 entries, SHA-256
**`1388fde86bb47906c0f3ebb4343b6ce9935eaaaecb03af57291b261417f0e7d6`**.
Asset ID: `605204863`. A fresh GitHub download matched the local archive byte for
byte and by digest. The internal manifest was also verified member by member.
It contains the prospective source, both complete original canonical states,
original and counterfactual policies, all raw reports/configurations/receipts,
evaluator/runtime identities, numerical checks, local qualification logs and the
read-only closeout audit. Minecraft server binaries, worlds, caches, credentials
and unrelated project data are excluded. This draft is not a software release.

**Decision: preserve the diagnosis; adopt no runtime or hybrid.** Unlike the older
protected-pickaxe pair, this preserved detachment pair met the output sensitivity
screen, not the representation consistency screen. Do not transfer a past
attribution to a different model or assume that protecting only shared features
protects old behavior. Equally, the representation-only seed-A failure prevents
claiming that freezing the output alone is a verified repair.

The next learning comparison must account for the complete actor function and
actual earlier-task exposure. Initial review timing is a concrete, separately
testable concern: the preceding critic diagnosis had no earlier-task review
before its early boundary, and the existing restored course begins at zero
review credit. That fact does not explain this output drift by itself. Keep any
review-timing change separate from reward, representation and output protection;
measure early accepted examples and both retention and genuine mining acquisition
before adoption. This study did not run such a learning intervention.

The shared development Academy was neither stopped nor reset. The accepted
installed training artifact remains
`31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.
At the archived closeout snapshot (epoch milliseconds `1790931178723`), all **512**
actors progressed, accepted samples were **721,095,907**, and inference failures,
inference rejections, learner rejections/stale samples and retired actors were
zero. This is live process health, not a frozen qualification of that later
moving model; the existing course was still on mining with no recorded mining
success. Mainline changes for this result are documentation only, so no runtime
restart or learned-progress reset is required.
