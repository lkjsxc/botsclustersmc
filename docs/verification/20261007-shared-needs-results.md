# Shared-needs source qualification: observed results

Date: 2026-10-07 (Japan). PR #69.
Accepted base: `5d12ad9beecf4fc96f25abb6f6e830533bf98294`.
Tested implementation: `292d60c5eb1ace1c8533adc0cb1de35494ff828e`.
Implementation tree: `b7936291445f417929f71d718da32406a0aa8dc8`.
Prospective contract commit: `ec64a5a4dcd7dc49732ff67870081c2f96ba19f9`.

The [machine-readable record](20261007-shared-needs-results.json) retains source
and local evidence hashes. GitHub Actions run `37613450847` for the exact
implementation head completed successfully: Linux source, Windows source and
observatory jobs passed. Retention and live-server jobs were skipped, not passed.
A documentation-only descendant must be checked separately before integration.
The full semantics and limitations are in [Shared needs](../SHARED_NEEDS.md).

## Result

The reviewed Java source/API/separation suite completed successfully. Public-demand
encoding passed 85 checks. Time accounting passed 646,602 checks, including
98,304 exhaustive combinations of eight-tick binary coverage histories,
all temporal partitions and three discounts. The fixed fractional seed
`2026100721` supplied 200 further traces. Cohorts 1, 2, 8, 64 and 512 passed the
normalized-return checks. A 512-member, 12,000-tick case used only 128 retained
ticks and completed the declared delayed-decision schedule without lost samples.
These are generated-stock **accounting** tests, not Minecraft population scaling.

The software distinguishes unknown observations, ring overrun, duplicate claims,
future/missing/cross-episode frames, demand or membership changes, and incomplete
outcomes. Rejected requests leave prior accounting state unchanged. Explicit
sample discards remain visible separately from the measured team outcome.
Per-material shortage durations and the all-material coverage count prevent a
partial mean score from being reported as full supply. An unknown interval breaks
a confirmed shortage run rather than inventing either supply or a shortage.

All six deliberate corruptions compiled and failed the intended assertion:
population normalization, unknown-interval acceptance, per-decision averaging,
replayed cursors, premature final scoring and nonzero terminal bootstrap. The
same child environment first passed the unmodified positive test. Compilation
failure, an unrelated exception and a child timeout are not accepted detections.

The existing Python report suite passed 47 tests normally and the same 47 under
`-O`. Browser tests initially exited with `ModuleNotFoundError: playwright`; that
failed invocation remains retained. A checkout-local virtual environment and
browser cache were then prepared with the CI-pinned Playwright 1.57.0. The actual
rerun exited zero and passed all five synthetic dashboard fixture groups,
including desktop/mobile, stale/invalid data and failure isolation.

## Artifact and deployment boundary

The exact source changes in `core`, `plugin`, `training` and `.github` are empty.
Only `Host.test()` registers and checks the new research code. Both public JARs
exclude `org/botsclustersmc/needs` and were rebuilt with unchanged SHA-256 values:

- Inference: `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0`.
- Training: `31e74a15dd139f0ecdc8ea97f912350d6589dac7404d8d9da6b3c2f16f34615f`.

No Minecraft learning, saved-policy preparation, deployment, server restart,
service change, existing Academy access or progress reset occurred in this qualification.
No incomplete physical result from PRs #66–#68 was read, resumed or reclassified.
Those drafts are not prerequisites for shipping this separate source contract,
and their gates are not waived by it.

## What this establishes, and what it does not

There is now a tested Java contract for observable common demand, stock-time
utility, bounded shared history and separate actor reward delivery. There is no
installed demand-conditioned policy or real resource transfer result. A supplied
`Stock` object is not a physical conservation certificate. A collective mean is
not proof of equal individual welfare, and the 1/N accounting share is not a proof
that optimizer behavior is invariant to population or decision frequency.

The next real intervention must explicitly couple a revised model schema to
owner-thread public-bank observations and physical material evidence, then use
predeclared independent learned trajectories and full frozen evaluation. Ordinary
tool-use retention, productive borrowing, replenishment, consumption and partner
replacement remain separate requirements; no synthetic pass is substituted for
any of them.
