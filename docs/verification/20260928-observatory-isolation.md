# Observatory failure isolation and activation — 2026-09-28

## Decision and identity

Keep current training telemetry, historical charts and completed fixed-policy
evaluation available independently. A history-reader failure is not a training
failure; an old `running` snapshot is not proof of current liveness. This fixes
operator-visible errors without changing what agents learn or how they act.

Base: `80ae4a727bc8534736fa70037957435250997e2b`.
Runtime implementation: `b82823fe3d17e20cd03d351d036819bdb8cfba4a`,
tree `2e1f55df25f9440d399260735354e252a92e1e1b`.
PR #30 merged as `647af80e50c2ec2e8d44dce27b6f149b2678008d`, with the same tree.
GitHub main was independently re-read before activation.

Only `host/Monitor.java` and the dashboard alter operational runtime behavior.
`Host.java` only invokes two new tests. `core/`, `plugin/`, `training/`, rewards,
curriculum, model/schema, gameplay mechanics, server pin, service configuration
and network exposure are unchanged.

## Reproduced failures and fix

The real loopback HTTP test ran the original compiled monitor with disposable
metrics and a child JVM: **13 failures / 56 checks**. Oversized history suppressed
otherwise valid status and evaluation. Missing status suppressed historical data
and fixed-policy evaluation. Malformed UTF-8 in one evaluation file also hid its
readable sibling.

The new browser checks ran against original main HTML: **64 failures / 138
checks**. History failures prevented current-status rendering and evaluation
refresh. Status failures left old actor counts, throughput and CPU cards visible.
Fresh stopped/failed snapshots retained old throughput. Far-future or coerced
string timestamps could be treated as fresh. The existing ordinary fifteen-second
staleness warning was already present; this change does not claim to introduce it.

Each API component now has separate availability. An unavailable status/history
returns 503 only for that component; missing history remains empty. Report and
evaluator-heartbeat file read failures are isolated. No source metrics are
repaired, overwritten, truncated or deleted by the monitor.

Object reads are bounded to 128 KiB, each history segment to 9 MiB, retained lines
to 32,768 characters and combined history to the latest 720 lines. Limits apply
while reading, including growth after opening. Overlong lines are drained without
allocating their full content. Invalid UTF-8, non-regular files and symlinked
metric paths are rejected.

The page still refreshes fixed-policy evaluation when status is unavailable,
without inventing a live policy identity. Failed status requests clear previous
current cards. Stopped, failed, stale and invalid-clock snapshots do not present
old activity as current. Freshness is inclusive at fifteen seconds of age and
allows up to five seconds of future clock skew.

HTTP 200 still means a recorded snapshot was readable, **not a process-health
certificate**. Browser clocks must be reasonably synchronized. Envelope checks
are not a full JSON grammar or semantic validator; browser parsing and panel
validation remain necessary. These bounds do not protect against a stalled
filesystem or make concurrent ancestor-directory replacement race-free.

## Verification and the Windows follow-up

| Check | Result |
| --- | --- |
| Original loopback monitor | 13 failures / 56 checks |
| Corrected loopback monitor | 56 checks passed |
| Deterministic byte/line/rotation/growth checks | 50 passed on Linux |
| Original dashboard against new cases | 64 failures / 138 checks |
| Corrected dashboard against new cases | 138 passed |
| Existing crafting/activation/probe/harvest browser suite | Passed; no browser exceptions |
| Full local source suite | Passed, including real API compilation and artifact separation |
| PR CI 36424988689 | Ubuntu, Windows and observatory passed |
| Live browser smoke check after activation | 28 passed |

Four symlink cases are explicitly skipped on Windows when privilege is not
assumed; the other read-boundary cases run there. Opt-in Minecraft/retention CI
jobs were skipped, not counted as gameplay successes. No new Minecraft fixture
or learned-skill evaluation was run for this host/UI-only change.

First post-merge main CI **36425710850** passed Ubuntu and observatory but failed
on Windows deleting disposable `monitor.log`: the OS reported that another
process still used it. This was test teardown, not a monitor HTTP assertion.
The failed log is retained and is not relabeled as a successful run.

The follow-up changes only `MonitorTest`: close the HTTP client; confirm child
exit; close process streams and the directory walk; retry transient filesystem
locks for at most five seconds per test-owned path. A live child or persistent
deletion failure still fails. Runtime data permissions, deletion rules and
runtime timeouts are unchanged. Final-head CI is the authoritative validation
of this test-only follow-up.

## Artifact identity and activation

Fresh JARs were compared byte-for-byte with previous main artifacts. Training
also matched the installed server plugin:

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `botsclustersmc.jar` | 120533 | `e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0` |
| `training.jar` | 212945 | `7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc` |

Production was fast-forwarded to merged main. At **2026-09-28 13:02:09 UTC
(22:02:09 JST)** only `botsclustersmc-monitor.service` restarted: PID 84532 became
207536. Training supervisor **121202**, Minecraft child **121289**, the training
invocation identity and 12:26:03 UTC start time were unchanged. The evaluation
timer remained active. No weights, optimizer, curriculum or world data were reset.

Across snapshots **13:02:05.906–13:02:35.905 UTC**:

| Measurement | First | Last |
| --- | ---: | ---: |
| Active / recently progressing actors | 512 / 512 | 512 / 512 |
| Accepted training samples | 53867468 | 53929308 |
| Policy updates | 197803 | 198004 |
| Decision transitions | 4372375 | 4433814 |
| Inference failures / rejections | 0 / 0 | 0 / 0 |
| Pending / retired actors | 0 / 0 | 0 / 0 |
| Frontier task | 10 | 10 |

Served HTML matched merged main byte-for-byte. The live browser checked actual
512-body status, eighteen course rows, history, completed evaluation, desktop/
mobile width, rejected write/model routes and recovery from history/status 503
responses injected **only into that isolated browser page**. No server metrics
were edited to induce failures. Its displayed report belonged to tested policy
191953, not the live policy; this was not a new evaluation.

## Retained evidence and limits

Owned worktree: `/home/coder/workspace/botsclustersmc-observatory-isolation`.
Its ignored `.build/` retains `monitor-baseline.log`,
`availability-baseline/availability-result.json`,
`main-windows-cleanup-failure.log`, `source-final.log`, `source-final.exit`,
`source-verification.json`, `browser-final/`, `live-observatory/`,
`monitor-activation.json`, and `source-cleanup-fixed.log` with its exit record.
Screenshots and JSON reports are included there. Original failed HTTP fixture:
`/tmp/bcmc-monitor-test-10597286016224142117`. Local evidence is not a promise of
permanent off-machine archival.

No improvement in learned skill, retention, autonomous survival or cooperation
is claimed. The next learning priority remains reliable primitive crafting
while retaining earlier tasks, measured with fixed-policy tests rather than
throughput, frontier counters or attractive dashboard values.
