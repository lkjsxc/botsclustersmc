# Stone contact learning study — 2026-09-28

## Prospective gate (fixed before either experimental learner starts)

Baseline source: `bb0dcfd1d1c5e94f8276e92367187e064145e1a9`.
Workspace: `lkjsxc/tomato-ocelot-73`.
Candidate worktree: `/home/coder/workspace/botsclustersmc-stone-study`.

The live periodic evaluator measured policy 636089, 180614593 accepted samples:
tasks 0–10 each 32/32, wooden pickaxe 27/32, cobblestone 0/32. This is one
complete full-condition report, not proof about the continually changing model.
Its seed was 2430005306958487075. The earlier failed crafting diagnosis is not
a reason to replace now-successful crafting behavior.

A separate initial one-shot evaluation was launched with tasks 0–12, 32 cases
and seed 2026092801. Additional ZIP analysis was rejected by the tool safety
check; that operation was not retried or routed through another tool. No
diagnostic conclusions are drawn here from its uninspected per-trial data.

## Candidate and unchanged boundaries

Extend the existing log-harvesting practice to MINE_COBBLESTONE:
reset-only aiming assistance for practice; observed target-contact progress
normalized by the existing actuator's 40-tick pick duration; nonpositive
alignment/reach/motion/unfinished-contact costs; collection-distance cost after
breaking stone. Wrong-tool contact receives zero cobblestone progress. Add
0.15 times that progress to the existing discounted potential.

This changes the training objective and practice starts. The state costs are
not all potential-based, so policy-invariance is NOT claimed. No learned
improvement is implied by their sign, mechanical reachability or passing tests.
The final outcome still requires a real provenance-checked cobblestone pickup.
No teacher actions, automatic tool selection, mining macro, modified action
mask, relaxed exam, changed network, optimizer or checkpoint schema is added.
Core/plugin source and inference JAR bytes must remain unchanged.
Full-condition probes/exams preserve reset poses and RNG consumption.

## Isolated comparison

Read one complete atomically published canonical training checkpoint ONCE into
a new study-owned evidence directory. Validate it with the existing checkpoint
exporter. Copy those exact bytes, including Adam and course state, into two new
explicitly owned Academies. Do not edit certificates or copy live credentials
or worlds. Preserve the initial checkpoint and both arms' early/final checkpoints.
The production learner, world, weights and monitor stay untouched.

Control executes baseline source; candidate executes the tested candidate.
Each arm uses 512 actors, seed 7, a 2-GiB heap and region/inference/learner threads
2/1/1, with two JVM-visible processors and a distinct loopback-only port.
A normal launcher/console stop drains each experimental learner. Record actual
sample overshoot. At most two experiment-owned servers run together; evaluate stopped arms
sequentially. Leave the existing production evaluator timer unchanged and account
for its transient extra server. Stop the experiment if available memory falls
below 1 GiB or any study I/O/health check fails.

Before training, evaluate the exact copied initial state on ordered tasks
[0,1,2,3,4,5,6,7,8,9,10,11,12], 32 cases each, seed 2026092810.
After 250,000 additional accepted samples per arm, stop and retain both states.
Evaluate all 13 tasks, 16 cases each, seed 2026092811. Reject continuation if
either arm scores below 14/16 on any task 0–10 or 10/16 on task 11; additionally
reject the candidate if more than two successes below control on any task 0–10,
or more than three below control on task 11. Task 12 is recorded, not gated early.
A failed early report may not be hidden by a later result.

If the early screen passes, resume each arm from its own complete state up to
1,500,000 total additional accepted samples. Stop and evaluate the final states
on all 13 tasks, 32 cases each, seed 2026092812. Re-evaluate the SAME saved model
of each arm with `--from` on fresh seed 2026092813, also 32 cases per task.
Do not substitute seeds, discard failures, pool seeds or adapt the budget.
Each training phase has a 2,400-second execution bound, with all failures retained.

On BOTH final seeds separately, require candidate tasks 0–10 >=30/32 and no more
than two successes below control; task 11 >=24/32 and no more than three below
control; task 12 >=16/32 and at least eight successes above control. All reports
must be complete, use zero evaluation training samples, and preserve all trials
and artifact identities. These are prospective engineering screens, not a
multi-training-seed statistical superiority claim or open-world survival proof.

A rejected candidate is not installed in production. Even a passing study does
not authorize silently replacing a live checkpoint: any rollout needs an intact
pre-rollout state, explicit source/state identity and an early post-resume
full-condition retention check. Archive a rejected implementation rather than
leaving it as an unexplained pending production branch.

## Operational limits

A production disk-quota error at 19:47 JST on September 27 was followed by a
service restart at 19:52; it restored its exact saved optimizer/model.
The host filesystem's free space does not establish the home-volume quota.
Do not delete other projects, worlds or earlier evidence. Keep this experiment's
files bounded and stop on an I/O/health failure. The live service restart count
was already two at the start of this session; it must not be reported as zero.
