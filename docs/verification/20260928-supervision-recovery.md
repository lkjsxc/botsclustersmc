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
Fresh synthetic-task training here is lifecycle coverage, not a learning claim.

The real checks, mutation results, final identities and publication decision are
recorded below once completed; no pending check is counted as passing.
