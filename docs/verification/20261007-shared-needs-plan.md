# Shared-need semantics: prospective source-only qualification

Date: 2026-10-07 (Japan). Accepted base:
`5d12ad9beecf4fc96f25abb6f6e830533bf98294`.
Branch: `work/shared-needs-20261007`.

## Scope before executing the qualification

Implement a public-demand sidecar and a bounded, tick-accounted shared-stock
objective under `tests/needs`. This is executable Java research infrastructure,
not a new deployed sensor, actor, learner, checkpoint schema or learned behavior.
It deliberately has no dependency on draft PRs #66, #67 or #68. Their unfinished
physical and saved-policy operations are neither retried nor waived. Existing
worktrees, runtime artifacts, services, worlds and saved learning inputs are not
modified or inspected by this qualification. No Minecraft server is started.

The first objective is public stock availability, not autonomous needs generation:
fixed positive targets for oak planks, sticks and/or wooden pickaxes, a fixed
cohort and a finite horizon. No roles, teacher actions, navigation, recipe macro,
private partner inventory, automatic consumption or supply injection is included.
The sidecar is separate from the current observation/model format. Wiring it into
a policy requires an explicit new schema and prospective real learning tests.

A unit of score is coverage-time, not a deposit/click/crafting event. Reward claims
must partition actual completed room-tick intervals without duplication and with
member normalization. Unknown history and slow-actor overrun require an explicit
trajectory discontinuity; they are never scored as empty stock or zero reward.

## Fixed software acceptance matrix

- Input validation, signal immutability, unknown-state clearing, excess-stock
  saturation, and independence from member identity and population metadata.
- Exhaustive eight-tick binary coverage paths, every temporal partition, and
  discounts 0, 0.97 and 1. Compare a direct per-tick oracle using `Math.pow`, not
  another copy of the production interval recurrence.
- Random fractional-demand paths with seed `2026100721`, fixed before execution.
- Cohorts 1, 2, 8, 64 and 512; one-tick versus delayed decision schedules; actual
  elapsed-time and total normalized-return equivalence. Population checks are
  offline accounting checks, NOT real Minecraft scaling evidence.
- Duplicate, missing, future, stale, cross-episode and changed-contract frames;
  incomplete/unknown final evidence; ring overwrite; rejected-operation
  noninterference; explicit discontinuities and terminal bootstrap semantics.
- Deliberately corrupt normalization, unknown handling, reward timing and member
  cursors in disposable compiled copies. Compilation failure is not detection.
- Complete existing Java/API/artifact separation checks and existing offline
  report/browser checks. Neither public JAR may package `org/botsclustersmc/needs`.

Do not reinterpret a failed test, extend a failed physical experiment, or claim
learning from these generated stock traces. Exact source/results will be recorded
only after observed execution. A source-only merge would ship this contract and
its qualification, not adopt a gameplay runtime or close any earlier draft gate.
