# 2026-09-28 — Quota incident, recovery and bounded supervision

Base: `cef3daeb7f9dc761c7c9e9e4634d8f63c1f4bbb9`.
Workspace: `lkjsxc/tomato-ocelot-73`.
Implementation worktree: `/home/coder/workspace/botsclustersmc-supervision`.

## Resolve the preceding session's unknown state

The system service journal, not the overwritten `academy/console.log`, establishes
this incident sequence on 2026-09-28 (JST):

- **16:40:06:** `PolicyFile.atomicWrite` could not create the temporary file used
  by `RuntimePlugin.writeStatus`: `Disk quota exceeded`. The runtime failed
  closed and stopped accepting work.
- **16:40:12:** the supervisor reported an unsuccessful server shutdown.
- **16:40:46:** an automatic service restart failed with `Disk quota exceeded`.
- **16:41:18:** another automatic restart failed writing `VTrace.class`, again
  with `Disk quota exceeded`.
- **16:41:54:** the server restored policy/Adam 69276 and 17,428,920 samples, then
  reported ready on its existing port 25566.

The failed status observed in the preceding session had reported 17,439,866
samples. The restored count was 10,946 lower; no loss-free final save is claimed.
The available journal contains the program errors; reading system unit metadata
shows three automatic restarts. This was not a stop/start issued by this work.
The shared home's Btrfs mount is distinct from the host filesystem's displayed
free space. A large `df` figure did not guarantee a writable quota. The exact
quota limit, peak usage and the actor/process that consumed or released space
were not established, and no quota enlargement or unrelated-project deletion
was performed.

A new read at **16:49:25.718 JST** reported fresh running status (1.2 seconds old),
512 active/ticking/progressing actors, policy 72283 and 18,264,900 samples. The
system service was active with supervisor 756010, server 756100 and `NRestarts=3`.
These counters are operational progress, not a skill evaluation.

The canonical repository then completed a normal fetch and fast-forward from
52d335e to cef3dae, with a clean worktree. No force update, lock deletion or
alternate remote was used. The earlier ref-write error is consistent with the
same interval of storage-write failures, but Git did not supply a quota-specific
cause, so it is not independently attributed with certainty.

## Host-only changes

The source change is confined to `host/Host.java`, source tests and documentation.
Core, plugin, learner, reward, task allocation, action/observation meanings and
checkpoint contents/formats are unchanged. Both runtime JARs must compare exactly
to the base and to the installed training runtime. Updating the source does not
hot-patch an already running supervisor.

1. The child console is drained in fixed 8-KiB byte buffers. A local log write,
   flush or close failure is recorded, not silently swallowed. The broken sink
   is disabled once; the surviving sink continues to receive child output. A
   `PrintStream` error is detected even when its API suppresses an IOException.
   The supervisor requests a normal stop on capture failure and returns failure,
   including when the child exits zero. It does not claim checkpoint success.
2. `console.log` and `console.previous.log` each hold at most 8 MiB. Rotation
   preserves the preceding nonempty segment at restart and does not replace it
   with an empty retry. These are bounded byte segments, not complete run archives:
   a segment boundary can split a line or a UTF-8 sequence. Concatenating previous
   then current restores the retained byte order. External journal retention and
   explicit experiment evidence remain separate responsibilities.
3. A first fresh status has the existing 180-second startup grace. Once a valid
   advancing report has been seen, 60 seconds without another advancing timestamp
   requests a normal stop. Re-reading the same file, touching its mtime, prior-run
   data, missing/ambiguous timing fields and far-future timestamps do not renew
   liveness. Fresh `paused` reports remain valid; this is not a policy-progress or
   lesson timer. Status reads are capped at 64 KiB and refuse symlinks.
4. A requested stop gets a 35-second grace, followed by bounded termination
   attempts. Escalation explicitly states that the final checkpoint is not
   guaranteed. Control metadata is removed after cleanup; a child failure,
   capture failure or incomplete drain cannot be reported as successful shutdown.

Console paths are checked before launch and rotation. Existing nonregular,
symlinked or oversized console files are not followed, truncated or silently
deleted; startup refuses them until the operator explicitly archives/resolves
those paths. Atomic log renames have no non-atomic fallback. The two rolling
segments may replace older managed segments by design; they are not backups of
all failed experiments. No experiment evidence was deleted by this change.

A throwing I/O error is not the same as an indefinitely blocked OS write. This
change does not promise to interrupt an uninterruptible filesystem, make a quota
larger, prevent every possible pipe/OS stall, or make Minecraft world and neural
checkpoint saves one transaction. The control channel remains authenticated and
loopback-only. No new runtime/deployment dependency is added.

## Tests completed before the real-server checks

The focused suite passed **583 checks** using **seven synthetic child JVMs**.
A child emitted 24 MiB with no newline after an injected local log failure; all
bytes reached the mirror and the child exited normally. Other tests cover exact
byte preservation, write/flush/close/input/PrintStream errors, no repeated writes
to a failed sink, log bounds and restart rotation, safe file paths, heartbeat
freshness, paused reports, clean failure stops and an unresponsive child.

The initial foreground full-source run lost its tool connection and left an
incomplete receipt after the supervisor test. It is not counted as a pass. A
separate bounded run completed the entire source/API/export/evaluation suite with
`source_test_exit=0`, retained as `.build/supervision-full-repeat.log`.

The Java Process API documents why subprocess output must be drained:
[Java 21 Process](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Process.html).
Filesystem free-space observations are hints, not future-write guarantees:
[Java 21 FileStore](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileStore.html).
The disk-error test uses an injected failing stream, not deliberate exhaustion
of the shared home or an operator filesystem.

## Declared read-only post-recovery skill check

Before running it, fix the following diagnostic: freeze ONE current canonical
live checkpoint through the existing holdout harness; evaluate ordered tasks
0–7, **32 full-condition stochastic cases each**, seed **2026092881**. Preserve
all 256 trials and the exact frozen checkpoint/model/runtime. Use a disposable
loopback-only Folia server on port 25588, two JVM-visible processors and the
existing 2-GiB holdout heap. No intervention, donor copy, training, modified
certificate, replacement seed or later favorable result substitutes for it.

This is a timestamped operational skill snapshot, not a matched before/after
retention experiment, a supervisor effect on skill, or an adoption screen for a
new learner. Tasks beyond 7 are not assessed. The live Academy keeps running and
receives no model or state from this check.

## Declared supervisor lifecycle check

Separately start the new source launcher with a fresh, test-owned Academy,
eight NPCs, seed 7, one GiB heap, region/inference/learner threads 1/1/1, two
JVM-visible processors and loopback-only port 25589. Verify actual actor progress
and a canonical checkpoint, issue a normal stop through its own authenticated
console, and require a successful supervisor exit with no listener or live child.
Do not install or restart the production service to manufacture this test.
Fresh task-zero training here is lifecycle coverage, not a learning claim.

## Completed real checks and publication decision

Accept the host-only correction after source/CI and lifecycle verification. Do not
change the live policy or learner. The production supervisor is intentionally
left running; the corrected launcher takes effect at its next controlled start,
not through a source checkout update. Underlying quota capacity was not repaired
or enlarged by this increment.

Implementation/declaration source: `18965aad1c25db56655318eb6f290d4206092a72`, tree
`166b2b0aaad7b347ee745f4f2ba7edec7c980d02`. A subsequent test-only enhancement
makes missing expected reasons fail by assertion rather than NullPointerException
and covers a failed parent mirror through the complete supervisor lifecycle.
`host/Host.java`, both runtime JARs and the tested Folia launcher remain identical
to this declared implementation.

### Frozen post-recovery skill snapshot

The declared evaluation completed all **256 trials** without intervention or
new training. The frozen input was policy **76690**, accepted samples
**19,097,871**. The report timestamp is **2026-09-28 17:02:05.221 JST**.

| Task | Passed /32 |
| --- | ---: |
| 0 forward-stop | 32 |
| 1 turn-stop | 32 |
| 2 aim-hold | 30 |
| 3 navigate-stop | 32 |
| 4 step-over | 28 |
| 5 break-log | 32 |
| 6 collect-log | 32 |
| 7 place-block | 29 |

All failures remain in the [256 public trial outcomes](data/20260928-recovery-trials.json).
The fixed policy is not the continually changing live model. Without a paired
pre-interruption model, these results cannot measure how much skill the restart
retained or lost. Later tasks, including stone mining and cooperation, were not
assessed by this snapshot. No second seed was substituted or pooled with it.

An independent audit verified all 256 unique actor identities, eight task partitions,
32 trials per task, the exact seed, positive elapsed ticks, summary success counts,
unchanged policy/runtime/checkpoint identities, and the absence of evaluation
training checkpoints. The disposable server exited and port 25588 was released.

- Frozen policy: `2ee02c2e6887dd747fa43416b6b18a8da3c3f540d32956d8b82d3831f45da426`.
- Frozen canonical checkpoint: `bc6b8aafa509e8ea62c7bbd677803781b7e8f970cf19e769513212917a1c46dd`.
- Full report: `4c96e3af435f8845a475129a7d1243c2ffc32fa3d0a76bee937735b1b675032b`.
- Exam JAR: `9675c40ce9ac300d563e8602477b428ebc5ab65712caf5860c685ebae2d47021`.

### Real launcher lifecycle

The new launcher ran the declared fresh eight-NPC Folia 1.21.11 build 14 Academy
on loopback port 25589 from **17:06:01 to 17:06:36 JST**. Two fresh status samples
showed all eight actors progressing, no inference failures/rejections, and
accepted samples increasing **256 -> 512** (updates 1 -> 2). Both reported
`startup_restored_checkpoint=false`, as required for this fresh fixture.

Its own authenticated console accepted `stop`; the supervisor returned **0**.
The server process exited, the control metadata was removed and port 25589 could
be bound again. Strict checkpoint-to-policy export returned **0**. No force stop
was needed for this real-server check. These few task-zero updates establish
lifecycle operation, not learned competence or vanilla-player equivalence.

The exact installed test runtime matched the base, build and production-installed
training JAR: `bf75154c58e56c91918aa1e05baaa6cf1465a9afac119da01f59e0d21b45b006`.
The inference JAR remains `d076ec6e18bc9be7fe6da825d01e01930dd86573ff6be147d52243620948dee5`.
The test-owned final checkpoint is
`3f63e4d4fde764368679ca334c5584cef2df1d87834571b4966bc10e0d5a97ea`.

### Assertion-based regressions and CI

The final focused suite performs **589 checks with eight synthetic child JVMs**,
including the 24-MiB output flood and the additional mirror-failure stop/last-output
check. Four deliberately incorrect, separately compiled implementations were
rejected by semantic assertions: abandoning the child pipe after log failure,
renewing liveness from an unchanged timestamp, removing the steady-state watchdog,
and overwriting the previous log with an empty restart. The unchanged copy passed.
Compile failures, crashes unrelated to the assertion and test timeouts did not
count as detected mutants.

Initial PR CI [36394837532](https://github.com/lkjsxc/botsclustersmc/actions/runs/36394837532)
passed Ubuntu and Windows full-source/API checks and the synthetic observatory.
Optional broad live, Paper, Windows-live and retention jobs were skipped, not
passed. The Folia and frozen-policy checks above were separate workspace runs.

### Preserved evidence and continuing production

[Compact lifecycle and mutation receipts](data/20260928-supervision-checks.json)
are published without control tokens or credentials. The worktree retains:

- `.build/quota-incident-journal.log`, the original observed error/restart sequence;
- `.build/recovery-fixed-policy/`, complete trials, frozen inputs and server output;
- `.build/supervision-live-academy/`, its owned world, logs and canonical checkpoint;
- `.build/supervision-live-receipt.json`, both status samples and the lifecycle driver;
- `.build/supervisor-mutants/`, independently compiled control/mutants and assertion logs;
- `.build/supervision-full-repeat.log`, the complete source-test receipt.

At **17:07:10.718 JST**, production still used supervisor **756010**, server
**756100**, and `NRestarts=3`, unchanged throughout this continuation. It reported
**512** active/ticking/progressing actors, policy **79397**, samples **19,462,631**,
and zero inference failures/rejections, retired or burning actors. This is a
later operational snapshot, not an extension of the fixed-policy skill results.
No study server remained listening, no candidate weights were installed, and
no production restart, checkpoint reset, quota change or unrelated deletion was
performed. The live supervisor's newly implemented protections are not yet active;
changing source and testing a separate process is not hot deployment.
