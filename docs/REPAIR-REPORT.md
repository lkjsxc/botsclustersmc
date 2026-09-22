> Historical record: supplied v0.3.1 ZIP only. Referenced evidence was in that ZIP; this is not proof for the current source-built runtime. See VALIDATION.md.

# Repair and live validation report — 2026-09-22 JST

## Outcome

The supplied project now starts its real Folia Academy at **0.0.0.0:25565**, constructs and reads back all 32 enclosed cells, connects32 Rust policy actors, learns with the original PPO implementation, saves, stops and resumes the exact prior policy/optimizer state. Both acceptance audits passed. This is an operational result on the tested Linux host, not an absolute guarantee for every host or a certificate of six-stage skill mastery.

## Recovered cause and changes

The input server log records `invalid environment` while enabling the original plugin. The default name prefix `botsclustersmc` has 14 characters but the plugin and client permit at most 13. The repaired public default is `bcmc`, yielding bcmc00..bcmc31. The plugin previously failed before starting its asynchronous error writer; failure receipts are now written synchronously, and the supervisor watches them even before readiness. The original native launch path hard-coded 25615; the new host supervisor is the effective port owner for the real server, client and protocol probe, defaulting to 25565.

A diagnostic run with only the prefix corrected exposed another defect: ordinary Folia entity task retirement during shutdown was treated as a fatal reset failure. The bridge now discards retired sessions without generating observations, rewards or success. An actual kick/reconnect and two clean shutdowns exercised the repaired lifecycle. A late genuine fatal still makes a run abnormal. The new host tools add current-run/all-agent checks, bounded logs, requested-port occupancy checks, resource guards and a root console. Existing owned Academies receive new host helpers without overwriting world/model state.

## Real runs, not synthetic fixtures

| Item | Fresh run | Resumed run |
|---|---:|---:|
| Port / protocol | 25565 / 774 | 25565 / 774 |
| Learning agents / cells | 32 / 32 | 32 / 32 |
| Initial policy version | 0 | 9 |
| Final policy version | 9 | 18 |
| New completed PPO updates | 9 | 9 |
| Initial trained samples | 0 | 17,957 |
| Final trained samples | 17,957 | 34,810 |
| Initial optimizer steps | 0 | 824 |
| Final optimizer steps | 824 | 1628 |
| Minimum decisions by any agent | 794 | 729 |
| Maximum decisions by any agent | 1061 | 1018 |
| Completed episodes | 257 | 276 |
| Successful training episodes | 87 | 180 |
| Frozen evaluation episodes | 0 | 0 |
| Final shutdown | clean | clean |

Fresh-run identifier: `1790034244004100904-6020`. Resumed-run identifier: `1790034645316340940-11207`.
Fresh-run final / resumed initial policy fingerprint: `120ae64d85459342`.
Resumed final fingerprint: `507531d49d595cf6`. Restored counters also match exactly; they are not reset or estimated. Every requested agent spawned, produced at least 64 policy decisions, received real server statistics and completed at least one server-observed episode in each run. Native audits loaded the actual final saved policy and its paired curriculum checkpoint before accepting them. Raw audit output, status, episode rows, server logs, protocol/campus receipts and checkpoint hashes are under `evidence/repair/round1/` and `round2/`.

The fresh run used a 240-second supervised learner interval including staggered joins. It included an intentional `kick bcmc00 lifecycle-reconnect-test` at 23:46:29 UTC; the same identity rejoined at 23:46:49 UTC, with two spawns and one disconnect, then continued policy decisions without a bridge fatal. The resumed run was stopped through the root `stop.sh`, not by killing a server. All32 were active before requesting the stop. The final foreground log confirms saving and clean shutdown.

These training-episode counts are observations, not a controlled comparison or proof of generalization. No frozen exam occurred in these runs, and the course remained on stage 0. A training success is not an evaluation pass. No claim of human-like movement, survival, crafting, building or cooperation follows.

## Additional real checks

A separate untrained diagnostic client named watcher00 joined as a spectator at the observer location (8,110,8). The real saved player NBT record has `playerGameType=3`. It was not one of the 32 training agents and did not train. This checks the observer join path, not a human GUI session or external internet reachability; evidence is under `evidence/repair/observer/`.

Three isolated copies of a stopped checkpoint were tested with the actual native executable: a corrupt policy checksum, a missing reward-pair file, and a mismatched curriculum population were all rejected with exit 1. The remaining checkpoint bytes were not overwritten. No test connected those faulty clients to the server; their target was an unused loopback port. Results are in `checks/native-checkpoint-rejections.json`.

A real cold Folia boot was fault-injected with the original invalid prefix. Only this disposable test copy bypassed the *outer* shell prefix validation, so the actual Java plugin initialization and supervisor failure propagation were exercised. It exited 1, published the explicit BOT_PREFIX failure, launched no learner, removed its owned PID marker and recorded an abnormal shutdown. Total wall time was **54.023 seconds including bridge compilation, cold server startup and shutdown**; this is not the delay between the failure and detection. The full 600-second startup timeout was not awaited. The invalid test copy is not in the delivery; `fault-injection/` contains the result and logs.

## Executed regression checks

19 shell/filesystem packaging tests passed, including state preservation while refreshing host helpers. Their launch dispatch uses labelled dummy executables and is not Minecraft evidence. 24 host-tool fixtures passed (configuration, real socket occupancy, missing/partial/stale campus, exact identities, missing/stale/future heartbeats, learner errors and bounded log ordering). Host fixtures do not substitute for the real runs above.

Actual Java 21 compilation/execution passed the environment configuration cases, protocol fixtures (one accepted, seven rejected), and the real pure campus planner (73,728 base positions,1,152 reset transitions). The environment plugin was also compiled against the actual extracted Folia 1.21.11 API and loaded by the real server. Parsing-only checks are separately labelled. Six independent Python numerical-reference tests passed; these are not newly executed Rust unit tests.

## Platform, provenance and limits

The validation host was Linux x86_64 / glibc 2.41, OpenJDK 21.0.11, constrained to 4 CPU and 4 GiB memory. Both real 32-bot runs explicitly used heap 2 GiB, Folia threads 2 and minimum-free-disk 2 GiB. The user-facing defaults remain heap 6 GiB/threads 6/minimum-free 10 GiB for the requested 16 CPU/12GiB/120GB allocation. Snapshot RSS is recorded, but neither peak memory nor indefinite capacity was certified. Final samples of Java RSS were approximately1.46GiB and1.35GiB; these exclude log helpers, clients, cache and other host memory. They are not peak or total-cgroup measurements.

No Rust compiler was present in this repair environment, and network downloads failed at DNS. All 26 native Rust/Cargo source files and both native executables were compared byte-for-byte with the uploaded archive and left unchanged. Original receipts and source identity remain under `evidence/import/`; the new native-component receipt is explicitly not a claim of local compilation. Host/Java code was repaired and the imported real native executables were exercised. Direct legacy native `run`, `probe` and `console` paths retain old assumptions; only the root public scripts are supported launch/control entrypoints.

The bundle requires Linux x86_64, glibc >=2.39 (maximum version needed by the imported native utility), Java 21 JDK and standard shell tools. It ships recovered pinned Folia and dependency caches, so the tested unchanged path needs no Rust/Cargo/runtime download. Mojang public-key refresh failed at DNS in this isolated environment; offline operation remained functional. The report does not pretend every log is error-free or that external Mojang connectivity was tested.

No external router/NAT/DNS, human desktop rendering, ARM/Windows/macOS execution, sustained multi-day operation, six-stage evaluation or cooperative settlement was validated. Offline mode has no account authentication. Keep TCP 25565 restricted to trusted networks. The original upload and its world were not deleted; no saved learning-state files were present in that upload. The delivery contains neither old/test worlds nor learned weights, .env, accepted EULA, runtime PID/lock files or the deliberately faulty test copy.

The preliminary shortened-prefix diagnostic pilot failed its Academy audit because of the original retired-task error; that failure is preserved under `evidence/repair/failures/`, not counted as a pass. The first accepted run predates the final-console-message and existing-helper-refresh cleanups; the second includes them. Final package review additionally reproduced an abnormal terminal-group SIGINT stop. The final host revision isolates owned child sessions using setsid, normalizes interrupted operator cancellation and ignores repeated INT/TERM during bounded cleanup. That later signal revision must not be attributed to the two earlier staging runs above: exact-archive signal/fresh/restart checks are recorded in the separate delivery verification supplied alongside the ZIP. The native and Java environment code remains identical; the signal diagnostic failure and separate per-run code hashes are preserved. Documentation/license metadata changes do not affect gameplay.

Local receipts are corruption/audit aids, not tamper-proof evidence against the host administrator. No GitHub push, CI dispatch or release publication was performed in this ZIP repair.
