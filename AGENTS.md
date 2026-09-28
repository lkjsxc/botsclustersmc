# botsclustersmc engineering contract

Build a clean, Java-operated RL training system and a self-contained Paper/Folia
inference plugin. There is one current implementation. Do not maintain legacy
checkpoints, alternate generations, migration wrappers, or version-suffixed paths.
Git history retains earlier implementations and historical verification records.
Never erase an operator's worlds or overwrite damaged/incompatible state.

The deployment body is an in-server NPC, not a network-connected Minecraft player.
Say so. Its neural policy selects normal motor, look, interaction and inventory
inputs. No pathfinder, recipe macro, teacher actions, imitation or hidden fallback
may choose gameplay. Environment goals and mechanical assistance are documented.
Do not equate NPC simulation with vanilla player semantics or autonomous survival.

Core inference has no Bukkit, native library or training dependency. The inference
JAR must not contain curriculum/reset/optimizer code. Training uses the same model,
observations and primitive actuator as deployment, in real disposable Minecraft
rooms. All world/entity access belongs to the owning Folia scheduler. Background
threads handle immutable snapshots and tensors, never Bukkit world objects.
Queues, in-flight requests, population and persistent files are bounded. Never
block a tick thread waiting for inference, learning, file I/O or another region.

Asynchronous learning must record behavior likelihood, policy identity, true
terminal flags, elapsed ticks and discontinuities. Correct policy lag explicitly;
never describe off-policy data as strictly on-policy. Exams do not train. Do not
promote skills by wall-clock time or label reachability as learned competence.

## Development deployment mandate (operator, 2026-09-28)

The shared development server in `lkjsxc/tomato-ocelot-73` must run the latest
validated, integrated mainline runtime, not merely have an updated checkout.
For an accepted runtime/configuration change, completion includes building,
installing, controlled restart and fresh live learning/health verification.
Restart the monitor or other affected services when their implementation changes.
Do not defer activation to preserve accumulated learning. The operator explicitly
accepts losing all development learning data, weights and attained stages, and
restarting from scratch; no additional permission is needed for that tradeoff.
Reuse compatible state when convenient, but do not add compatibility machinery
or keep an obsolete runtime solely to retain it. A fresh start must be explicit,
use an owned development Academy, and keep monitor/evaluator paths consistent.
Record the activated source and artifact identity and distinguish deployment
health from learned competence. Documentation-only changes with identical runtime
inputs do not require a restart. Failed builds or unhealthy activation are not
latest-version success; diagnose and repair them rather than claim completion.
This is a development completion rule, not an unattended remote-branch polling
service. Do not deploy unmerged experimental candidates or change the pinned
Minecraft server version merely because a newer upstream version exists.
Preserve failed experiment evidence and non-disposable operator worlds; the
permission to discard development learning does not authorize deleting other
projects or unrelated server data. State explicitly when learned progress resets.

## Learning experiments

Before changing rewards, curriculum allocation or learner semantics, use a separately
owned Academy, either a complete checkpoint copy or an explicitly fresh start.
Do not silently reinterpret an incompatible checkpoint or invent earned certificates. Declare the retained tasks, task order,
case counts, seeds, accepted-sample budget and rejection thresholds before training.
Compare complete frozen full-condition reports, then confirm on a fresh seed.
Mechanical reachability, valid gradients, a batch KL bound, update counts and old
certificates do not prove that previously learned skills have been retained.

Preserve failed experiments and the last measured-good model, optimizer and course
as evidence. A rejected experiment requires restoring state as well as code when
claiming a rollback; an old JAR does not undo damaging gradients. For the shared
development server, an explicitly fresh start on the latest accepted runtime is
also permitted and must not be described as restored learned progress. Exact model/Adam restore is not proof of post-resume skill
retention: inspect the initial task coverage and early fixed-policy results.
Never hide a failed early checkpoint by reporting only a favorable later one.

Ship stable artifact names, explicit model schemas and atomic checked writes.
Inference fails closed without a valid model. Normal startup requires explicit
Minecraft EULA consent; existing production server configuration is not rewritten
by installing the inference plugin. Test pure Java math, API compilation, real
Paper/Folia lifecycle, export/deploy and measured scaling separately. Report
actual source revisions, hardware, active entities, throughput and limitations.
Use English for code and technical records, Japanese for operator instructions.
