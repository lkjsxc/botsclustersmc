# Java runtime acceptance — 2026-09-23

## Source and environment

The measured core/plugin/training Java sources and resources have canonical
SHA256 `ab877860d0370a38060a31142bc50c59d167527965159d251e45a330ce768bbe`. The digest is computed over sorted relative paths,
each followed by NUL, original file bytes and NUL. This binds this local capacity
measurement to the actual modules, independently of later documentation commits.
The checked-in [status samples](data/java-capacity.json) retain the counters used
below. Compatibility CI results are recorded separately when completed.

Linux x86_64; OpenJDK21.0.11;4 effective CPUs exposed to the JVM; Folia1.21.11build14;
3GiB maximum heap;2 region,1 inference and1 learner worker;2,048 actual NPC bodies
in8 separated islands of256 rooms. No external actor clients or learned-policy
server ran outside the JVM. Raw server and acceptance logs were produced in the
disposable local acceptance directory, not an operator installation.

## Completed real training measurement

The public JDK source launcher created a fresh Academy, built the real JARs, started
the actual server and waited for every body to tick AND progress. The measured
fresh interval contains49 status snapshots over239.995seconds:

| Measure | Result |
| --- | ---: |
| Active/ticking/progressing NPCs at every sampled point |2,048/2,048/2,048|
| Actual samples newly used by optimizer |1,925,293|
| Real trained samples/second |8,022.2213|
| Real action transitions/second |8,022.6630|
| Mean process CPU-core equivalents |1.9052 out of4|
| Maximum sampled heap use |1,348MiB|
| Minimum actions per NPC in a status interval |18|
| Retired NPCs / inference failures |0 /0|
| Stale/rejected learning samples |0 /0|
| Highest current course stage / passed frozen exams |0 /0|

The heap number is sampled occupancy, **not peak RSS**. CPU is process-time delta
across the interval, not a screenshot of one idle thread. All-body progress is
measured; merely registering2,048 IDs would not pass. There is no intentional
CPU burn. The modestCPU result is consistent with useful tick-rate-limited work,
not a claim that unusedCPU can always improve sample-efficient learning.

A clean stop saved state. The same Academy was restarted with exact restored
model/Adam counters; a further14.999second measured interval trained117,824
samples (7,855.4570/s). The exported final snapshot had6,782 updates and2,361,311
trained samples. Samples outside the measurement intervals account for the
larger final total. Unfinished actors/experiments were not fabricated as trained.
Independent exams had begun while other actors continued learning, but none had
passed by this endpoint. **No first-stage or18-stage mastery claim is made.**

## Completed real task reachability and deployment

A separately compiled diagnostic plugin, never included in either production
JAR, passed all18 tasks at full difficulty using explicitly scripted primitive
inputs. Its model training/inference counters remained zero. This establishes
mechanical reachability, **not learned competence, demonstrations or imitation**.

```text
[09:11:41 INFO]: [BotsClustersMC] FIXTURE PASS 7 PLACE_BLOCK ticks=15
[09:11:41 INFO]: [BotsClustersMC] FIXTURE PASS 8 CRAFT_PLANKS ticks=19
[09:11:41 INFO]: [BotsClustersMC] FIXTURE PASS 9 CRAFT_STICKS ticks=23
[09:11:42 INFO]: [BotsClustersMC] FIXTURE PASS 10 CRAFT_WORKBENCH ticks=31
[09:11:42 INFO]: [BotsClustersMC] FIXTURE PASS 2 AIM_HOLD ticks=43
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 0 FORWARD_STOP ticks=47
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 16 BUILD_PLATFORM ticks=47
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 15 SUPPLY_CHEST ticks=47
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 11 CRAFT_WOOD_PICK ticks=51
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 3 NAVIGATE_STOP ticks=63
[09:11:43 INFO]: [BotsClustersMC] FIXTURE PASS 4 STEP_OVER ticks=63
[09:11:44 INFO]: [BotsClustersMC] FIXTURE PASS 13 CRAFT_STONE_PICK ticks=67
[09:11:44 INFO]: [BotsClustersMC] FIXTURE PASS 5 BREAK_LOG ticks=71
[09:11:44 INFO]: [BotsClustersMC] FIXTURE PASS 1 TURN_STOP ticks=83
[09:11:45 INFO]: [BotsClustersMC] FIXTURE PASS 12 MINE_COBBLESTONE ticks=87
[09:11:46 INFO]: [BotsClustersMC] FIXTURE PASS 6 COLLECT_LOG ticks=107
[09:11:48 INFO]: [BotsClustersMC] FIXTURE PASS 17 LOG_TO_WORKBENCH ticks=147
[09:11:53 INFO]: [BotsClustersMC] FIXTURE PASS 14 SMELT_IRON ticks=243
```

The exported inference JAR and policy were copied to an independent real server
without training.jar, a training flag, Academy data or an external inference
process.64 actual NPCs ticked, moved and responded to pause/resume. Removing them
released all owned chunk leases; console spawning64 more and removing them also
passed. A corrupted-model restart disabled only the plugin, left the operator
server running, did not modify the corrupt bytes and wrote no training checkpoint.
An operator-file sentinel remained unchanged. The test ended with:

```text
PASS real training + exact-state resume + export: 2048 actual NPCs
PASS 18 full-difficulty scripted real-world fixtures; zero learner/inference samples
PASS plugin + policy only; 64 actual NPCs, pause/resume, ticket release, respawn,
corrupt-model fail-closed without server shutdown
PASS acceptance mode=all
```

## Numerical and synthetic checks

The numerical/pure suites passed7,213 core assertions,10,319 mechanics assertions
and165,116 independent-course assertions. These are assertion counts, not that
many independent experiments. Coverage includes finite-difference gradients,
conditional GUI likelihood/entropy, batch/scalar parity, invalid model rejection,
Adam exact roundtrip,1,000-request queue accounting,10,000 unique arena addresses,
raw-material conservation and isolated frozen-exam progression/retention rules.

Five synthetic conditional-bandit seeds each performed400 actual updates using
25,600 samples. Final correct-action probabilities were0.997919,0.997056,0.997408,
0.997355 and0.997708. This is a learning sanity check, **not Minecraft learning**.
The shorter120-update diagnostic did not reach its0.95 criterion; the retained
400-update test specifies its budget explicitly instead of lowering the criterion.

## Repairs and earlier attempts

An initial1,024-body run encountered a real reset failure. Rare vanilla randomized
Zombie initialization created passenger/jockey state, preventing reset teleport.
The repair disables random spawn initialization and delays first reset until
entity registration has completed; the same count then passed. Training failed
closed rather than silently excluding the failed body.

A first scripted task run failed several vertical aiming/crafting interactions.
Vanilla mob awareness was overwriting selected pitch despite removed goal entries.
Disabling awareness fixed the competing control loop; all18 then passed on actual
Folia. These altered mob semantics are documented, not presented as vanilla players.

Earlier1,024- and2,048-body measurements used intermediate curriculum/lease code;
they do not replace the exact final module-bound measurement above. A design with
many tiny training islands also caused unnecessary chunk-region overhead; the
final layout groups rooms according to the actual region/CPU budget.

## Limits

This proves a short real-server early-stage throughput/lifecycle result under
stated conditions, not hours/days of uptime, all later lessons at this throughput,
human-like motion, full survival, dense settlements, many human observers or
long-run policy retention/generalization. NPCs are not logged-in players; bodies
and pockets are ephemeral, crafting is a bounded catalogue and full player
mechanics are absent. The old client-based experiment differs too much for this
to be a controlled algorithm-only speedup claim. Physical hardware allocation and
other plugins can change results substantially.
