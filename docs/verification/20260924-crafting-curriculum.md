# Reachable crafting practice — 2026-09-24

## Source and delivery boundary

Base main: `9db2510f00722a0b2d7c57b942318aef87c688e3`.
Candidate: `56a4496b4f95bed7eaf1db4aa9c995ea7fe23007`.
Exact locally tested and published source tree:
`7e308cac755b1057f27fe2af98c7b28acd178c1b`.
[PR #7](https://github.com/lkjsxc/botsclustersmc/pull/7) was squash-merged to main as
`a1ac6e8914b1784da7caca11f994e6aed208d28d`. Independent branch re-read
confirmed that the integrated source tree is exactly the tested tree above.

This is a source/curriculum correction, not a newly trained policy or a survival
release. The previous frozen-policy result remains tasks 0–10 at 32/32 each,
workbench opening at 32/32 and wooden-pickaxe completion at 0/32, as recorded in
[the station-learning record](20260924-station-learning.md). Those numbers were
not remeasured here and must not be attributed to this candidate.

The designated operator machine was inspected once. A subsequent terminal call
was blocked by the tool's safety check; it was not retried or bypassed. Development
continued on a tracked-source snapshot in a separate sandbox and on hosted GitHub
CI. No operator world, checkpoint, configuration or running process was changed.
No new Minecraft server was started. The existing affirmative EULA gate remains;
no new personal EULA acceptance was supplied by this session.

## Defect reproduced

The previous phase selector made every station practice below difficulty 0.55 an
opening-only lesson. Assembly was therefore eligible only when `ceil(5*d)` was at
least 3. Its intended collection-only, one-cell and two-cell practice was absent
from the composed curriculum. Successful opening also increased the same EMA that
selected assembly difficulty, rather than measuring actual item completion.

A baseline diagnostic exercised 1,000 lesson seeds at each of six difficulties.
After the old opening filter, it deliberately granted a usable workbench to every
remaining operation case. Even this optimistic reproduction showed the gap:

| Difficulty | Opening cases | Operation cases | Missing-cell count in operations |
| --- | ---: | ---: | --- |
| 0.10 | 1,000 | 0 | None |
| 0.32 | 1,000 | 0 | None |
| 0.54 | 1,000 | 0 | None |
| 0.55 | 0 | 1,000 | 3 only |
| 0.70 | 0 | 1,000 | 4 only |
| 0.99 | 0 | 1,000 | 5 only |

These are reset-coverage counts, not learned success rates. The code establishes
a structural learning opportunity defect; it does not establish that this is the
only cause of the historical crafting failure.

## Adopted correction

At each assisted workstation difficulty below 1, a deterministic lesson-seeded
draw assigns approximately one quarter of practices to opening and three quarters
to operation. Opening begins closed with raw stock. Operation begins at the actual
open station and requires the original complete item outcome. Furnace/chest
operation uses the same split, without crafting-grid preparation.

Pickaxe reset preparation mixes the current frontier `ceil(5*d)` with earlier
starting counts, including zero missing cells. Half the operation starts use the
frontier and half sample earlier counts uniformly; missing positions are shuffled.
Only raw stock is redistributed. A complete ingredient grid supplies a preview,
not an owned product: the neural policy still must select a result-slot click.
No teacher action, pathfinder, recipe macro or gameplay fallback was introduced.

Opening outcomes still increment real episode and success totals but no longer
update the completion EMA. Completed operation practice and full probes still
update it. Probe cadence, readiness, frozen-exam thresholds and promotion gates
are unchanged. Existing checkpoint history is retained; historical EMA values are
not reclassified, reset or fabricated. New process-local bucket counters begin
at zero after a restart.

Full-difficulty practice, PROBE and EXAM retain closed menus, raw inventories,
original world/pose random streams and original terminal item requirements. The
512-value observation schema, core policy, action likelihoods, V-trace, optimizer
and shared deployment actuator were not changed. Reset metadata is not a policy
input. The training-only changes do not enter the inference-only JAR.

## Composed reset coverage and regression checks

The new test uses both wooden and stone pickaxes, seven difficulty levels and
2,048 seeds per level: 28,672 composed reset cases. Each task/level has 494 opening
and 1,554 operation cases with these seeds. The operation distribution for each
task is:

| Difficulty | 0 missing | 1 | 2 | 3 | 4 | 5 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0.00 | 1,554 | 0 | 0 | 0 | 0 | 0 |
| 0.10 | 776 | 778 | 0 | 0 | 0 | 0 |
| 0.32 | 383 | 393 | 778 | 0 | 0 | 0 |
| 0.54 | 252 | 280 | 244 | 778 | 0 | 0 |
| 0.55 | 252 | 280 | 244 | 778 | 0 | 0 |
| 0.70 | 189 | 194 | 217 | 176 | 778 | 0 |
| 0.99 | 149 | 150 | 176 | 156 | 145 | 778 |

The 155,729 new assertions also cover all individual missing positions, material
conservation, preview/ownership separation, deterministic pocket resets, full
condition exclusions across all 18 tasks, absent/wrong stock, and a checkpoint
round trip. In two otherwise identical 500-lesson course simulations, making all
opening attempts succeed versus fail leaves completion EMA, difficulty, next
lesson and advancement identical. There are still exactly 100 full probes;
opening wins alone cannot unlock an exam. These course simulations are not
learned behavior.

Four concurrent writers record 24,000 assisted outcomes; counts and successes
remain exact, snapshots cannot mutate internal storage, invalid buckets are
rejected, and a new instance begins at zero. Telemetry records only finished
assisted attempts and includes failures/timeouts, not interrupted attempts,
probes or exams.

Three compiled negative controls were deliberately introduced in disposable test
copies. The new regression rejected each: the old phase threshold, frontier-only
resets, and opening outcomes updating assembly difficulty. None of these mutants
was adopted.

## Software verification

The separate Linux/JDK 21 sandbox passed the pure Java suite: core math, mechanics,
menu focus, controls, immutable pocket views, aiming, harvesting, station rewards,
composed crafting curriculum, task balance, updates, independent course/review,
persistence and concurrency. Five synthetic contextual-bandit seeds each ran
25,600 accepted samples and 400 updates; their final correct-action probabilities
were 0.998489, 0.998273, 0.997669, 0.997902 and 0.998233. This is a numerical smoke
test, not Minecraft learning evidence.

Offline Playwright tests passed against synthetic metrics, with no server or
external host contacted. Desktop (1,280-pixel width) and mobile (390-pixel width)
checks cover bucket mapping, separation from opening outcomes, assistance labels,
malformed/inconsistent counters, zero-attempt display, page bounds and stale-state
warnings. No browser errors were observed. The source validation workflow now
runs this regression automatically and retains screenshots/results.

[Independent PR run 35995387600](https://github.com/lkjsxc/botsclustersmc/actions/runs/35995387600)
passed on the published candidate: Linux source job `107618960472`, Windows source
job `107618960708` and offline observatory job `107618960661`. Both source logs
confirm the full public launcher test path, real Folia 1.21.11 build 14 API
compilation, 48 ownership checks, the new curriculum regression, optimizer/course
checks, export (16), exact evaluated artifact (19), evaluation integrity (38) and
inference/training JAR separation. The real-API diagnostic fixtures compiled but
did not execute. The `live`, `paper` and `windows-live` jobs were skipped, not
counted as successful live tests. Artifacts retain the two source logs and the
observatory screenshots/JSON for 14 days.

The hosted import reconstructed the exact local candidate. Its first publication
attempt failed because the CI token lacked workflow-edit permission. Workflow
changes were subsequently made through the authorized GitHub connector; the
runner was restricted to ordinary source files, without altering token scopes.
Temporary transport/import files are absent from the candidate. No force push or
branch-protection change was used.

## Operator-visible interpretation

The observatory separates station opening from assisted crafting outcomes for
planks, sticks, workbenches, wooden picks and stone picks. Columns show the number
of recipe cells missing at reset. A `0 / 0` cell means no completed observations,
not a measured zero-percent success rate. A five-cell pickaxe practice still has
an initially open workbench, so it is not a full-condition trial. The HUD labels
opening, operation and full probes distinctly. Missing or invalid telemetry is
unavailable rather than guessed.

## Next empirical boundary

The next acceptance must compare exact frozen policies on the original full
conditions, while retaining the earlier skills. Preserve a known baseline
checkpoint, use a separate owned Academy, record actual accepted task samples and
resource settings, and compare the same full-condition cases before testing new
seeds. Retain every failure and case-level result. Report opening, assisted
assembly, full item completion and earlier-skill retention separately. An assisted
practice curve alone cannot justify exporting a general-survival policy.

The current evidence supports merging the software correction. It does not yet
show improved independent crafting, indefinite retention, open-world survival,
cooperation, higher server capacity, or new Paper/macOS/ARM live compatibility.
