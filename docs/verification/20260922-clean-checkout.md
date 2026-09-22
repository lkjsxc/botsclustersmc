# Final clean-checkout acceptance — 2026-09-22

## Actual GitHub checkout, no source-import artifact

[Source runtime verification run 35717700371](https://github.com/lkjsxc/botsclustersmc/actions/runs/35717700371)
completed successfully for `main` commit
`21ab8eff093b6f0d0fe17c3cc34bc262df74f560`.

This is the permanent, read-only CI workflow checking out the published plain
source tree, NOT the one-shot source import. It explicitly asserted that `bin`,
`runtime`, `.build`, `academy`, `server`, `state` and `.env` were absent at checkout.
It compiled the host helper after fetching Gson, passed all regression suites,
built and tested the real pinned Rust application, downloaded pinned Folia and
its Minecraft/Paperclip dependencies, compiled the actual Java bridge, and
finished with no changes to tracked files. No binary artifact, old ZIP or source
import blobs are required by the normal clone/build path. The one-shot importer
has been removed from the active tree.

CI did not start Minecraft or accept an EULA. Actual live server/learning evidence
comes from the separate local tests below and the
[full source-runtime record](20260922-source-runtime.md). Local live tests used
verified dependency caches, not an uncached network bootstrap.

## Normal entrypoint: exact resume with default learning batches

After the first normal `start.sh` run documented in the full record, the same
entrypoint was started again, using a copy of the previous status as the resume
acceptance baseline. Commands were:

```sh
cp academy/state/status.json /path/to/previous-status.json
EULA=true BIND_ADDRESS=127.0.0.1 JAVA_HEAP_GB=2 FOLIA_THREADS=2 \
  RUN_SECONDS=120 ./start.sh
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy /path/to/previous-status.json
```

Both commands exited zero. There were 32 bots, 64-step rollouts, a 2048-sample
update threshold and a 5 Hz action rate. Unlike the smoke fixtures, these are
normal learning settings. Heap/thread limits were reduced for the disposable
4-CPU / 4-GiB test host, and binding was loopback-only.

| Measurement | First normal run | Normal resumed run |
| --- | --- | --- |
| Run ID | 1790073334763380055-15141 | 1790074129873902817-18489 |
| Policy version | 0 to 4 | 4 to 8 |
| New PPO updates | 4 | 4 |
| Trained samples | 0 to 7456 | 7456 to 14943 |
| Optimizer steps | 0 to 299 | 299 to 659 |
| Policy fingerprint | 21e4b8fa7407bb6a to 4aa81593497e1d6c | 4aa81593497e1d6c to 26e1065fc162cb29 |
| Per-bot decisions | 272 to 471 | 231 to 471 |
| Minimum statistics packets per bot | 29 | 29 |
| Disconnects before stopped-run status | 0 | 0 |
| Discarded rollouts | 123 | 122 |
| Learner error | empty | empty |

The second run restored the exact previous policy, optimizer and sample counts,
then made new updates. All cells, paired checkpoints and completed episode
records passed acceptance. Both runs saved the model, curriculum and world and
exited cleanly. The console command-submission path was exercised while running.

Additional integration guards passed: a fresh source invocation with `EULA=false`
refused before creating an Academy or build output; changing `BOTS` from 32 to 31
refused the existing Academy without changing any saved state file.

These are bounded execution/update/resume checks, not a controlled learning-speed
comparison or a claim of mastered skills, long-run stability, external human
connectivity, ARM64 support or autonomous cooperative survival. The active
curriculum still contains six foundation tasks. The 18 experimental contracts
and RL-next mechanisms remain separate. See the full record for the retained
failed CI attempt and the rollout-efficiency limitations.
