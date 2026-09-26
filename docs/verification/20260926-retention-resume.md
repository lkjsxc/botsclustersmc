# Production resume, retained-skill checks, and final pause

Date: 2026-09-26. This continues the completed experiment documented in
[the task-expert pilot record](20260926-task-expert-pilot.md). The independent
expert candidate was rejected, not deployed. The only accepted implementation
change is the Academy data guard from PR #18.

## Final operating state

At the final observed status, approximately **17:16 JST**, the designated
`minecraft-agents` workspace has the original training server running in tmux
`bcmc-training-main`, but **NPC decisions and learning updates are paused** through
the existing `bots pause` command. This is an intentional operator pause, not a
crash or a claim that learning is progressing in the background.

| Field | Observed value |
|---|---|
| Working repository | `/home/coder/workspace/botsclustersmc-source` |
| Academy | `academy-current` |
| Runtime schema | `bcmc-citizen-egocentric-context` |
| Status | `paused` |
| Policy updates | 802777 |
| Accepted training samples | 376971442 |
| Active / ticking NPCs | 1024 / 1024 |
| Current decisions / learning samples per second | 0 / 0 |
| Inference queue / learner queue / actor buffer | 0 / 0 / 0 |
| Inference failures / rejections | 0 / 0 |
| Learner rejected / stale samples | 0 / 0 |
| Highest course task | 12: mine-cobblestone |
| Full course complete | false |
| Minecraft listener | port 25565 |
| Read-only monitor | loopback port 8765 |

The final status timestamp was `1790410617306`; the supervisor status age was
0.9 seconds when read. The same update/sample counters were observed before and
after the final evaluation. The canonical-checkpoint evaluator also selected
exactly policy 802777 / 376971442, rather than an earlier loose model file.
All new one-shot evaluations completed; the rejected pilot server is stopped.

The server process remains available, but the NPCs are deliberately not acting.
The learner reports `collecting` with an empty queue while the runtime is paused;
that label does not mean training is consuming samples. This pause is runtime
state, not a new persistent `.env` default. An ordinary restart or explicit
`bots resume` can resume normal learning; do not do that blindly before the next
experiment or operating decision.

## In-place restore and unchanged implementation

After PR #18 was merged as `46cfa1ccb04070ffbcc8643011c9da1a597c564d`, the original
checkout was fast-forwarded and the original Academy was restarted in place.
The normal startup log at **17:04:31 JST** reported:

```text
Restored exact optimizer/model: updates=800351, samples=375827378, optimizer-step=800351
```

All 1024 actors were observed on startup, with
`startup_restored_checkpoint=true`. Actual accepted samples subsequently grew;
then the existing pause command stopped further decisions and drained the
remaining trajectories. No production model reset, expert-model import,
certificate injection or schema migration occurred.

The merged change has no differences from the original baseline in `core/`,
`plugin/` or `training/`. The rebuilt inference JAR and the original baseline
bundle's inference JAR were compared directly: their archive bytes and member
payloads were identical. Their digest was:

```text
1804e928f3e75ba870504379181171f18d1cea9aedb130ebb31da62900857c42
```

The new launcher created `academy-current/.gitignore` with exactly `*` and LF.
Ordinary Git ignore checks excluded both the canonical training-state path and
the local console-control path. Those files were not staged or published.
The current network/authentication configuration was not changed by this fix.

## Keep every evaluated policy distinct

All rows below used 32 full-condition cases for every task 0 through 12.
Each evaluation server completed 416 cases and reported **zero new training
samples**. During the first post-resume evaluation, the separate production
server was still training; zero evaluation samples must not be read as zero
production learning during that interval.

| Policy | Accepted samples | Seed | Craft workbench | Craft wood pick | Mine cobblestone |
|---:|---:|---:|---:|---:|---:|
| 789705, original baseline | 370738571 | 2026092631 | 32/32 | 16/32 | 0/32 |
| 800526, first post-resume snapshot | 375906866 | 2026092635 | 25/32 | 1/32 | 0/32 |
| Same 800526, matched baseline seed | Same 375906866 | 2026092631 | 29/32 | 2/32 | 0/32 |
| 802777, latest paused checkpoint | 376971442 | 2026092636 | 30/32 | 10/32 | 0/32 |

For the **latest paused checkpoint**, tasks **0 through 9 each passed 32/32**.
These cover forward/turn/navigation stopping, aiming, stepping over, breaking and
collecting logs, placement, planks and sticks. Workbench crafting was 30/32,
wood-pick crafting 10/32, and cobblestone 0/32. This is the current measured
snapshot, not the older 1/32 result.

The matched-seed run used `--from` the first post-resume evaluated bundle. It
retained the same policy and inference JAR. Policy 800526's digest was
`c61afd4c6654c801d6239a5f4ebf6d001dca36f8e31a6a0f6276af93abc79ba1`.
The latest policy 802777's digest was
`819ededd389dbbcdc34d9d046cbecd6fad1d04564534e8c39cf3527b00e32479`.
The standard evaluator validated and completed these runs, preserving all failed
trials. The three post-resume runs account for 1248 completed cases, not 1248
successes. This record does not claim an additional independent audit of every
post-resume ZIP member beyond the standard evaluation checks.

## Interpretation and reason for pausing

The newer policy's 1/32 wood-pick result was not treated as a current live metric:
it belonged to a frozen snapshot. Repeating that same snapshot with the baseline
seed produced 2/32, so the changed seed alone is not a satisfactory explanation
for the observed weakness. A still-later paused model scored 10/32, illustrating
why neither a past success nor a past failure should certify a different policy.

The results warrant a retained-skill investigation, not an assertion that the
Academy ignore rule changed behavior or that a single neural mechanism has been
proved responsible. Fixed seeds do not control every real-server scheduling
variation, and 32 cases are limited measurements, not universal success rates.

Live changing-policy review probes in this restarted process counted 130/130
wood-pick successes, while the final immutable checkpoint scored 10/32 in the
independent rooms. These differ in policy versions, timing and sampled initial
conditions. Do not substitute those live probe totals or historical certificates
for the fixed-policy result. The next investigation should align policy identity,
initial state and success criteria before attributing the difference solely to
representation interference or changing rewards again.

Cobblestone remained 0/32 in every listed fixed-policy test. Continuing the same
unconditional update loop has not demonstrated useful frontier progress and
can leave a different checkpoint with weaker earlier skills. The deliberate
pause retains one exact current baseline for a separately declared, transfer-
and-retention-aware study. It does not solve stone acquisition or general
survival. No accepted learner redesign is hidden in this operational decision.

## Evidence and boundaries

The following completed bundles remain under the original checkout's
`.build/evidence/` directory:

```text
20260926-mining-continuity-baseline.zip
20260926-post-resume-evaluation.zip
20260926-retention-matched-seed.zip
20260926-paused-current-evaluation.zip
```

`academy-current/server/plugins/BotsClustersMC/evaluation.json` and
`evaluation-details.json` currently describe the latest paused model, while
`status.json` reports live paused-state health. The post-resume, matched-seed
and latest-evaluation logs are retained in `/tmp/bcmc-post-resume-evaluation.log`,
`/tmp/bcmc-retention-matched-seed.log` and `/tmp/bcmc-paused-current-evaluation.log`.
The normal restore line is in `academy-current/console.log`. None of these
workspace paths is presented as a public download link.

An optional resume-audit helper stopped on an erroneously transcribed reference
digest. A direct read-only archive comparison separately established the matching
inference bytes reported above. A request to repair that helper was then blocked
by the tool safety check and was not retried by another route. No successful
helper run or generated `20260926-production-resume.json` is claimed. This record
instead identifies the normal restore log, fresh runtime status and completed
standard fixed-policy evaluations that were actually observed.

The earlier blocked full-Academy copy also remains unexecuted; no copied backup
or migration of the original Academy is claimed. The original data stayed in
place, the candidate stayed separate, and main contains only the reviewed launcher
fix plus these verification records.
