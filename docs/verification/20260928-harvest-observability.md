# Frozen-policy harvesting observability — 2026-09-28

## Delivered scope

PR [22](https://github.com/lkjsxc/botsclustersmc/pull/22) was merged at
2026-09-28 10:30:10 JST. Tested implementation:
`f3c18e88a4c1b69fdd29d8d90922e80e24710c8d`, tree
`8d8449f2b5153be11de6affe7d92dfb05660dcb5`; squash main:
`fe24319f95c017dde23cdad06e3a88d4b91c5f40`.

The native evaluator now validates and aggregates the existing harvesting traces
for tasks 5, 6 and 12. The private observatory displays observation denominators,
menu-focused/world-dig selections, held-tool observations, target contact and its
observed maximum duration, and trials with any actual break or pickup. Missing
and partial trace coverage are explicit unavailable states, not measured zeros.
Present invalid traces or aggregates that disagree with full trials are rejected.
The browser also refuses assisted-reset reports in the ordinary evaluation panel.

No core, plugin or training source changed. No reward, curriculum, model format,
optimizer, reset or inference behavior changed. Rebuilt training JAR bytes equal
the installed production plugin; inference JAR bytes equal the unchanged build.
This is not a learned-skill improvement or a replacement for the rejected studies.

## Completed checks

- Complete local source tests, including 93 new harvesting report integrity checks.
  The final merged-main log ends with `source_test_exit=0`:
  `.build/harvest-main-source-tests-20260928.log` in the production checkout.
- Existing synthetic observatory regression plus new harvesting tests: pooled
  denominators, integer/partition/maxima consistency, saved-aggregate mismatch,
  missing/partial coverage, malformed or assisted report clearing, stale evaluator,
  and desktop/mobile layout. Missing data are never filled with inferred zeros.
- PR CI [36365939035](https://github.com/lkjsxc/botsclustersmc/actions/runs/36365939035)
  and merged-main CI [36366212337](https://github.com/lkjsxc/botsclustersmc/actions/runs/36366212337)
  completed successfully: Ubuntu source/API checks, Windows source/API checks,
  and synthetic observatory checks. The optional CI live/Paper/retention jobs were
  not requested; their skipped state is not a claimed compatibility result.
- A separate arithmetic verifier matched every published harvesting aggregate to
  the real report's individual trials. Candidate and deployed read-only monitors
  rendered the actual report at 1280x1000 and 390x844, without horizontal page
  overflow or JavaScript errors. POST returned 405; model, full-detail and console
  file routes returned 404. The temporary candidate monitor was terminated.

## Actual frozen-policy evaluation

Declared before execution: tasks 0-12, 32 full-condition cases each, seed
`2026092841`. Native evaluation completed all 416 trials and exported the exact
snapshot, with zero new training samples. Policy update `846632`, trained samples
`243568648`; policy identity
`780689fa11cce4c11c17960e3af681733f278d763b18dd33627e7101cab8216e`.

| Tasks | Completed task successes |
| --- | --- |
| 0-6: movement, aiming, navigation, log breaking/collection | Each 32/32 |
| 7: block placement | 31/32 |
| 8-10: planks, sticks, workbench | Each 32/32 |
| 11: wooden pickaxe | 23/32 |
| 12: cobblestone mining | 0/32 |

Stone task diagnostics cover every one of its 32 trials:

| Measurement | Observed value |
| --- | --- |
| Decision-boundary observations | 19,200 |
| Menu-focused selections | 16,760 / 19,200 (87.3%) |
| World-dig selections | 594 / 19,200 (3.1%) |
| Held-pickaxe observations | 424 / 19,200 (2.2%) |
| Target contact with a held pickaxe | 0 observations; 0/32 trials |
| Target contact with another held item | 4 observations across 2/32 trials |
| Maximum observed other-item target-contact duration | 4 ticks |
| Trials with any block broken | 0/32 |
| Trials with any item collected | 12/32 |

The last row is deliberately not called successful mining. The unchanged actual
cobblestone success predicate still reports 0/32. For comparison, log breaking
recorded target contact in 32/32 trials and actual breaks in 32/32; log collection
recorded actual pickups and successful completion in 32/32.

Shares overlap and are not elapsed-time percentages or independent statistical
samples. Input focus is measured at the selected action, while held tool and
contact describe the ending observation. Brief contact or the final successful
break can fall between observations. These data localize a failure pattern but
are not a causal intervention, an A/B learning improvement, or a survival result.

The next investigation should prioritize maintaining a useful held tool and
returning from menus to world interaction before treating sustained mining contact
as the sole problem. Any learning or action-space candidate still needs an isolated
Academy and prospectively specified early retention checks, especially for learned
crafting. Do not activate earlier rejected candidates or turn task-specific teacher
actions into an apparent mining solution.

## Evidence retained on the workspace

Workspace: `lkjsxc/tomato-ocelot-73`.
Production checkout: `/home/coder/workspace/botsclustersmc`.
Verified implementation/evidence worktree:
`/home/coder/workspace/botsclustersmc-harvest-observability`.

Within the evidence worktree:

- `.build/harvest-evaluation-20260928.log` ends with `evaluation_exit=0`.
- `.build/harvest-evaluated-20260928.zip` retains the exact evaluated model,
  matching inference JAR, full trials and summary. It is not a training backup.
- `.build/evidence/harvest-native/` retains full and compact reports, arithmetic
  verification, candidate/deployed browser receipts, and desktop/mobile images.
- `.build/verify-harvest-live.py` is the independent arithmetic/browser verifier.
- `.build/evidence/crafting-monitor/` retains the synthetic browser evidence.

Full real report identity:
`51a91b2c70d10f88a6abe6c149609e8f28c123122965d4a22f85ffcd9a5f35f7`.
The production evaluation files can later be replaced by normal periodic runs;
the retained evidence above belongs to this exact snapshot.

## Production continuity

At **2026-09-28 10:32:48.356 JST**, the deployed monitor reported all 512 NPCs
active, ticking and progressing. All were at task 12. Live policy update `849785`,
accepted samples `244510693`, latest interval about `1936.18` samples/second.
Burning actors, failed/rejected inference, rejected/stale learner samples and
update-rejected samples were all zero. This interval is not a throughput benchmark.

The training supervisor remained MainPID `141387`, with NRestarts `2` both before
and after this work; those two automatic restarts predate this continuation. No
training reset, training-service restart, production weight replacement or
production-world modification was performed by this change. Only the read-only monitor was restarted to load the new
page (new MainPID `398082`). The evaluation timer was paused during the manual
check and restored active afterward. No experimental learner or extra monitor was
left running. This final documentation record changes no executable source.
