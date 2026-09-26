# Task-expert cold-start pilot — declared before training

Date: 2026-09-26. Production reference: fa3cebe816a8d90b56e838d60031547c90c49fb8.

## Observed baseline, not an architecture comparison

The unchanged shared policy 789705 / 370738571 samples was tested with seed
2026092631, 32 full-condition cases for each task 0 through 12 (416 total).
Tasks 0,1,3,4,5,6,7,8,9,10 passed 32; task 2 passed 31; task 11 passed 16;
task 12 passed zero. Cobblestone had 16717 menu-focused observations out of
19200, 474 held-pick observations, and zero target-pick contact observations.
These are decision-boundary samples, not tick-exact causes.

Production stopped normally at policy/Adam 800351 and 375827378 accepted samples.
The original Academy remains unchanged after shutdown. A requested full-Academy
copy was blocked by the tool safety check and was NOT retried through another
command, encoding or tool. No claim of a copied backup or imported model is made.

## Changed plan: fully new data

The separate worktree will create a new, initially absent `academy-experts-fresh`.
It must not read or copy production world, credentials, course or checkpoint.
Use the existing pinned public server cache, loopback port 25631, online mode,
seed 7, 1024 NPCs, maximum heap 8 GiB, region/inference/learner workers 9/1/5.
Reuse the operator's already accepted EULA for the same pinned server.
All expert weights, Adam moments, local clocks and course records start fresh.
No actor is awarded a prior-task certificate or moved directly to a later task.

The candidate has 18 independent task-routed MLPs, using the existing observed
task and unchanged primitive controls, rewards, reset assistance and exam rules.
Only sampled experts update. Mean gradients, clipping and Adam bias correction
are expert-local. Normal startup rejects the previous schema; no migration or
legacy loader is added.

## Bounded measurement and rejection

Measure the first checkpoint at or after 1000000 accepted samples and the second
at or after 5000000. Status is sampled every 5 seconds: report actual saved
counts and any overshoot rather than calling these exact budgets. Stop for
failure, sustained queue rejection, nonfinite values, or failure to progress.
A hard elapsed cap of 40 minutes bounds this pilot independently of throughput.

At each boundary stop normally and evaluate the exact saved model with 32
full-condition cases per task: all tasks zero through the highest reached task,
including the unfinished frontier. Seed 2026092632 is the early test; 2026092633
is the later test. Retain complete failures and trial denominators.
For continuation past the pilot, tasks 0 and 1 must each reach at least 28/32
in the later test AND a fresh-seed replication 2026092634; any task reported
as mastered must meet that same threshold on both later snapshots/tests.
This is a cold-start learning-feasibility gate, NOT evidence of retaining the
old shared model's task-0-through-11 skills or of learning cobblestone.

Do not merge a replacement or change production deployment solely because
numerical tests pass. Preserve and report a failed pilot. Any reward/reset
change, expert reseeding, longer budget or selected-task study requires a
separately declared experiment; it cannot overwrite this one.
