# Runtime hardening acceptance — 2026-09-23

## Tested source

The runtime and test sources are commit
`169d37ba6fb18495f8e1c25bb5da5b80771fd3c3`. Removing its one-shot CI helper produces
exact tree `c061cf276abe43248956fbf614ada6d363a2a3db`, also the tree measured locally.
Commit `aec15b66625db77ec8575080bc91e223adf77972` only removes that helper. Subsequent
verification/changelog edits do not change the tested executable sources.

[CI run 35861174286](https://github.com/lkjsxc/botsclustersmc/actions/runs/35861174286)
passed all seven jobs: clean source builds, actual server execution, and
exported-artifact tests.
The source and live jobs do not restore project build caches. Server libraries are
real official downloads, not Bukkit stubs. The compact local measurements and
counter endpoints are retained in [runtime-hardening.json](data/runtime-hardening.json).

## Repairs, including a failed real test

The previous exporter trusted a separately saved `policy.bcmc`. An interrupted
pair of writes could leave that policy behind `training.bcmc`, making deployment
use different weights from training resume. Training now saves only the complete
canonical checkpoint. Export holds the stopped Academy's run lock, rebuilds the
current artifacts, validates that checkpoint and derives deployment weights from
it. Tests deliberately supply an older valid policy and a corrupt loose policy;
neither is used or modified. A damaged canonical checkpoint never falls back to
random or cached weights and leaves a previous completed export unchanged.

Managed model/configuration paths now check symlinked ancestors before reads or
creating directories. Actual filesystem tests verify rejection and absence of
writes behind those links. This is not protection against a concurrent malicious
process with the same filesystem permissions. Destination JAR and model writes
are individually atomic, not a two-file transaction: deployment follows only a
successfully completed export and a stopped destination server.

Each inference request now owns a non-reused reply ticket. An old result/error
cannot overwrite or poison a newer request after a reset, pause or goal change.
Deterministic tests complete callbacks in the wrong order. Real servers also run
24 rapid goal replacements interleaved with pause/resume and require renewed
progress from every NPC afterward.

An extended **pre-repair inference test failed**: all 64 bodies died in daylight
(`active_agents=0`, `retired_agents=64`, while inference failures remained zero).
The pinned server's actual Mob daylight-tag path ignored the Zombie daylight flag.
The repair cancels plain, unattributed combustion only for this run's tagged NPCs;
block-attributed and entity-attributed events are preserved. This is disclosed body
assistance, not a learned survival skill. Actual-server fixtures assert that event
scope. The final daylight runs below pass without auto-respawning failed bodies.

## Numerical, file and launcher checks

Linux and Windows source jobs passed 7,220 core, 10,319 mechanics, 165,116 course,
24 filesystem/persistence, 14 concurrency and 16 source-launcher/export assertions.
These are assertion counts, not independent learning experiments. They include
finite-difference gradients, conditional GUI distributions, model validation,
optimizer roundtrips, stale/corrupt export rejection, run-lock exclusion and
late-callback isolation. Both OS jobs ran the symlink fixtures without skipping.
The inference JAR is checked for absence of training/reset classes.

Five synthetic conditional-bandit seeds each used 400 updates and 25,600 samples.
Their final correct-action probabilities were 0.997919, 0.997056, 0.997408, 0.997355
and 0.997708. This is a numerical learning sanity check, **not Minecraft mastery**.

## Final local real-server training

Linux x86_64, OpenJDK 21.0.11, 4 effective CPUs and a 4 GiB container; official
Folia 1.21.11 build 14. Training used a 3 GiB maximum heap, 2 region workers,
1 inference worker and 1 gradient worker. The 2,048 NPC bodies occupied eight
separated islands of 256 individual rooms. No external actor or inference process
was used. This is not a test of 2,048 connected Minecraft players.

| Final fresh training measurement | Result |
| --- | ---: |
| Measured interval / status snapshots | 244.999 seconds / 50 |
| Active, ticking and progressing NPCs at every snapshot | 2,048 / 2,048 / 2,048 |
| Newly trained samples | 1,985,595 |
| Actual trained samples per second | 8,104.5025 |
| Action transitions per second | 8,116.3393 |
| Average server-JVM CPU-core equivalents | 1.7325 of 4 |
| Maximum sampled heap occupancy | 1,291 MiB |
| Retired bodies / inference failures | 0 / 0 |
| Stale / rejected learning samples | 0 / 0 |
| Highest course task / passed individual exams | 0 / 0 |

CPU comes from process CPU-time deltas over the measured wall interval. The heap
number is sampled Java heap occupancy, **not peak process RSS or total machine
memory**. Unused CPU is not filled with busy work. These measurements do not prove
an algorithm-only speedup or better sample efficiency than the old player runtime.

Clean stop and restart restored exactly 5,677 policy/Adam updates and 2,109,481
trained samples before new learning. A subsequent 14.999-second interval added
123,264 samples (8,218.1479/s). The final export exactly matched the canonical
checkpoint with 6,681 updates and 2,408,617 samples. Shutdown reported 31,257
actor-buffered, untrained samples and 2,044 unfinished lessons; those are not
silently counted as trained or resumed as continuous Minecraft trajectories.

The same final source then passed all 18 full-difficulty scripted fixtures and a
separate 64-body plugin-only deployment, including pause/resume, rapid replacement,
lease release, respawn and corrupt-model restart. The full acceptance exited zero.
Scripted fixtures used no learner/inference samples and never supply training data.

## Separate 2,048-body inference-only capacity

The identical repaired inference JAR and an independently exported real checkpoint
(6,635 updates, 2,414,566 samples) were placed in a separate real Folia server.
There was no training JAR, Academy, optimizer or external inference service.
The server used a 2 GiB maximum heap and one inference worker. Bodies initially
occupied a two-block-spaced flat-world grid, not a complex simulated settlement.

Over 90.000 seconds and 19 status snapshots, all 2,048 bodies remained active,
ticking and progressing. There were 737,280 action transitions (8,192.0/s), average
server-JVM use of 0.5469 CPU cores out of 4, and maximum sampled heap occupancy of
678 MiB. Retirements and inference failures were zero. Loaded training metadata
stayed constant: **zero new training samples** were produced by deployment.
Natural/unattributed combustion suppression was counted, not hidden.

Rapid goal replacement, removal with all owned chunk leases released, respawning
64 bodies, and corrupt-model fail-closed restart also passed. A corrupt policy
disables only the inference plugin, preserves the supplied bytes, does not shut
down the operator server, and does not write a training checkpoint.

## Actual compatibility and reproduction

The CI matrix tests the same exported JAR/model on Paper 1.21.1 build 133 with
Java 21, Paper 1.21.11 build 132 with Java 21, and Paper 26.2 build 128 with Java 25.
Each passes a 64-body deployment and all 18 separate scripted mechanical fixtures.
This is evidence for these named versions, not every intervening version or fork.
The Windows live job tests source build, 32-body training, exact resume, export,
18 fixtures and a separate 64-body inference server.

```sh
./test.sh
# After personally accepting the Minecraft EULA, in an unused checkout:
EULA=true python3 tests/acceptance.py all --count 2048 --seconds 240 --output acceptance
EULA=true python3 tests/acceptance.py inference --deploy "$PWD/acceptance/deploy" \
  --inference-count 2048 --inference-seconds 90 --output inference-capacity
python3 tests/report.py acceptance
python3 tests/report.py inference-capacity
```

Python is a test-only dependency; ordinary build/start/export use the JDK. Output
paths must be fresh, test ports 25578–25580 free, and sufficient memory/disk present.
Do not run these disposable tests on an operator Academy or production world.

## Limits

No result here proves hours/days of uptime, all later lessons at this throughput,
neural completion of the first or all 18 skills, retention/generalization,
human-like motion, vanilla player mechanics, dense cooperative settlements,
many connected spectators, ARM64/macOS execution, or arbitrary plugin coexistence.
NPC bodies and pockets are ephemeral. Limited recipes, modified mob physics and
explicit goals remain part of this experiment. High sample throughput and passing
mechanical fixtures are not substitutes for held-out learned-skill evaluation.
