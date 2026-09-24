# Workstation learning and immutable menu observation — 2026-09-24

## Source and environment

Locally verified source: `ec326aa24fbc9b9357cb60107f894ed6c9cb95ab`.
Published implementation: `ad4bae0dd193bcebe61c06dabf3ffce3072ada64`.
Both have the exact Git tree `7e523c9ba386bd7ea13d4ceaff6194424394835d`.
Base main: `ce7c5f7a0796a4cba68d32769e2ab617f5970add`.

The authorized Linux x86-64 machine has 16 effective CPUs and 12 GiB RAM.
Tests used OpenJDK 21 and Folia 1.21.11 build 14. Isolated learning comparisons
used 1,024 actual NPCs, a 2 GiB heap, 4 region threads, 1 inference thread and
2 learner workers on loopback port 25582. The original Academy and its online
authentication continued unchanged during these experiments. Experimental worlds
and checkpoints were not substituted for the original live Academy.

## Failure found and correction

The actor population was stuck at task 11, wooden-pickaxe crafting. Case traces
showed repeated personal-inventory use instead of working at the actual table.
The old progress function reused GUI slot numbers across 2x2 and 3x3 menus.
Personal-inventory slot 40 is an output preview, but could be counted as a stick
ingredient of a workbench recipe. The correction maps physical grid coordinates,
checks that a recipe fits the active grid, excludes previews, recognizes valid
translations and reduces progress for wrong occupied cells.

The retained source combines bounded discounted workstation potentials with
explicit acquisition practice. Below practice difficulty 0.55, a shorter lesson
starts closed and requires opening the actual target station. It is separately
labelled and counted. Full PROBE and EXAM trials still require the original
crafted item, native smelted output or chest contents. Neither opening a table nor
an assisted practice success is a full-recipe certificate. No pathfinder, teacher
actions, imitation, recipe macro or hidden fallback was introduced.

The policy schema remains 512-value `bcmc-citizen-egocentric-context`. The new
menu/cursor/grid view is an immutable operator snapshot, not additional policy
instructions or a player menu that can click on behalf of an actor.

## Independent outcomes, including rejected reward scales

Every listed evaluation froze a stochastic neural policy and used zero new
training samples. The baseline, small-cost and first acquisition evaluations used
seed 9241535 with 16 cases per task. The strong-cost trial used seed 9241525.
The final 32-case check used new seed 924716. All failures were retained.

| Candidate | Policy | Accepted samples | Measured outcome |
| --- | ---: | ---: | --- |
| Copied baseline | 511504 | 226,397,839 | Tasks 0–10 each 16/16; wooden pick 0/16 |
| Rejected strong cumulative station cost | 514535 | 227,827,834 | Forward-stop 16/16; aim, workbench and wooden pick each 0/16 |
| Rejected smaller cumulative station cost | 514282 | 227,700,265 | Aim 11/16; other tasks 0–10 each 16/16; wooden pick 0/16 |
| Bounded potential plus acquisition practice | 518549 | 229,734,928 | Tasks 0–10 each 16/16; wooden pick 0/16 |
| Later new-case check | 526401 | 233,519,710 | Tasks 0–10 each 32/32; wooden pick 0/32 |

The strong and smaller-cost implementations were not adopted. The final source
uses `discount * next_potential - previous_potential`, with zero terminal potential,
not an additional cumulative station penalty. Acquisition training continued from
the bounded-potential trial; earlier reward-scale trials restarted from the same
baseline. These finite asynchronous comparisons are not a single-variable causal
proof, a universal ranking or evidence of lifelong retention.

### A narrower, real improvement: opening the workstation

In the matched 16 full-condition cases, baseline policy 511504 opened a workbench
in 3/16 trials, whereas policy 518549 opened one in 16/16. Policy 526401 also opened
one in all 32 new full-condition cases. These are closed-menu/random-facing,
nontraining trials, not shortened practice. All three sets still completed zero
pickaxes. The later trace contains 8,273 closed-menu, 5,410 personal-inventory and
5,517 workbench observations. Two trials exposed some recipe preview; that does
not prove it was a pickaxe or that the result was collected.

One live snapshot recorded 8,857/8,888 successful short acquisition practices.
That count is not a crafted-pickaxe count. Perfect observed scores on the first
11 tasks also do not establish universal skill, survival, cooperation, or the
competence of an arbitrary later policy.

## Software acceptance

The full local Java suite passed, including 4,848 workstation invariants and
25 immutable-pocket-view checks, alongside the existing numerical, menu,
ownership, review allocation, curriculum, persistence, concurrency and exact
export tests. Five synthetic bandit seeds passed; they are not Minecraft evidence.

Disposable Folia acceptance completed 64-agent fresh learning, exact optimizer
resume, canonical export, all 18 full-difficulty scripted mechanical fixtures,
and 64-agent inference lifecycle, rapid goal replacement, chunk release, respawn
and corrupt-model failure isolation. The latest station fixtures additionally
assert that wrong menus/stations fail acquisition, and that opening the right
station alone never completes a full PROBE or EXAM. They restore diagnostic state
before continuing the original scripted fixture. Scripted reachability is not
learned skill evidence.

A real non-operator protocol client and headless browser passed spectator joining,
follow/tour/overview, cross-island observation, target particles, denied admin
commands, private-dashboard/stale-state checks and immutable pocket diagnostics.
These are software-interface checks, not a human video review.

[Import verification run 35968339891](https://github.com/lkjsxc/botsclustersmc/actions/runs/35968339891)
reconstructed the exact tested source tree, passed the full Java suite and a fresh
64-agent real-server lifecycle/18-fixture/inference acceptance, then published a
normal branch commit. All temporary transport files and their workflow were
removed before publication. No branch protection or force update was used.
PR source validation and rollout results are recorded separately after completion.
No new Paper, macOS or ARM compatibility claim follows from these Folia/Linux tests.

## Retained artifacts and remaining boundary

The experiment worktree retains stopped rejected Academies, the initial canonical
checkpoint, exact source patches, logs and case-level reports under `.build/station-*`.
`dist/station-final-measured.zip` binds policy 526401 to its inference JAR, summary
and all 384 detailed cases. Its policy digest is
`5a890d944cdaadec72c9d8b3c365bc60eb23ee455b8b98f87ad7e43f205d2407`.
It is a measured experiment, not a general-survival release. The isolated learning
server was gracefully stopped after verification; the original Academy remained
running. The next unresolved skill is reliable 3x3 assembly and output collection
after reaching the workbench. Full autonomous cooperative survival is not claimed.
