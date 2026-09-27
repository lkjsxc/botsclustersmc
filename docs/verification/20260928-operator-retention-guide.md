# Operator retention guidance and continuity — 2026-09-28

## Scope

Base main: `661d69018daf3c0529a931c6a57a719c98d94fdc`.
Workspace: `lkjsxc/tomato-ocelot-73`.
Checkout: `/home/coder/workspace/botsclustersmc`.

The Japanese README still required stopping training to retain the exact evaluated
policy, although the existing native `evaluate --export` already retains that
snapshot while training continues. Correct that guidance. Distinguish the evaluated
inference ZIP from a complete stopped Academy backup; the ZIP has no optimizer,
course state or world. Preserve the no-overwrite, single-evaluator and timer rules.

Add the measured checkpoint-resume retention caveat to the normal service guide
and link it from the README. Describe early fixed-policy comparison, unchanged
task/case/seed specifications, actual task samples, and the difference between a
pre-stop evaluation snapshot and the final stopped checkpoint. This is a documentation
correction, not a new backup feature or a claim that every restart loses skills.

PR 20 remains closed unmerged after measured regression. PR 21 remains closed
unmerged with retention outcomes unverified. Do not retry or reroute the prior
blocked early-experiment-result inspection. Do not infer retention from improved
review allocation. Neither experimental implementation is in the production tree.
The production learner, world, checkpoint, monitor and evaluation timer are unchanged.

## Completed verification

`git diff --check` passed. Native `./evaluate.sh --help` confirms the documented
`--tasks`, `--cases`, `--seed` and `--export` interface. A complete source suite ran
with the two operator-document changes present, on the base commit above:
exit 0, no timeout, stable base and diff, 30.328704566 seconds.
Its start was 2026-09-28 03:43:38.435803 JST.

The retained local receipt and log are:

- `.build/operator-guide-source-tests-20260928.json`
- `.build/operator-guide-source-tests-20260928.log`

An earlier unlogged tool connection closed before returning a result; that
unobserved invocation is not counted as a pass. The later retained receipt is the
evidence for completion. This final verification record adds prose after that run.

A directory diff confirmed no changes to core, plugin, training, host or tests.
The rebuilt training JAR byte-matched the installed production training plugin.
The inference JAR byte-matched the unchanged control build.
No new source/browser CI is claimed for these Markdown-only commits; the existing
main workflow excludes Markdown/docs paths. Previous candidate CI is recorded in
the separate experiment reports, not attributed to a new production implementation.

## Independent production observations

At 03:39:35 JST, both production units were active. Training MainPID remained
141387 with NRestarts 2; monitor MainPID remained 261 with NRestarts 0.
The two training restarts predate this continuation. Only the production launcher,
production server and monitor appeared in the Java-process inventory.

The completed evaluation read at that time was dated 03:29:29.820 JST:
policy 676204, 192546969 accepted training samples, seed -2141602078712907032,
tasks 0–12 in order, 32 full-condition cases per task, 416 complete trials and
zero evaluation training. Tasks 0–10 each scored 32/32; wooden-pickaxe crafting
scored 27/32; cobblestone mining scored 0/32. Policy identity:
`bb9ef3d6ba29575a88f3bcac5418cef436582ca2ddcade280cbbc3d69416e648`.
This is the existing production learner, not the withheld reservation candidate.

At 03:44:08.356 JST the live monitor reported policy 683433, 194720363 accepted
samples and all 512 actors active, ticking and progressing. One actor was at
task 11 and 511 at task 12. Burning/retired actors, inference failures/rejections
and rejected/stale learner samples were all zero. The latest interval measured
2074.399887568 accepted samples/second, not a sustained-capacity benchmark.
The live policy differs from the evaluated snapshot and is not certified by it.

The operational repair does not establish improved mining, robust restart retention,
open-world survival or cooperation. No experimental learner or test process was
left running; the existing production learning service continues.
