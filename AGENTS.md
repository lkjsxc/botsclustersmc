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

Before changing rewards, curriculum allocation or learner semantics, fork the complete
known-good checkpoint into a separately owned Academy. Do not make the only live
training state the first learning experiment. Declare the retained tasks, task order,
case counts, seeds, accepted-sample budget and rejection thresholds before training.
Compare complete frozen full-condition reports, then confirm on a fresh seed.
Mechanical reachability, valid gradients, a batch KL bound, update counts and old
certificates do not prove that previously learned skills have been retained.

Preserve failed experiments and the last measured-good model, optimizer and course.
A rejected rollout requires restoring state as well as code; an old JAR does not
undo damaging gradients. Exact model/Adam restore is not proof of post-resume skill
retention: inspect the initial task coverage and early fixed-policy results.
Never hide a failed early checkpoint by reporting only a favorable later one.

Ship stable artifact names, explicit model schemas and atomic checked writes.
Inference fails closed without a valid model. Normal startup requires explicit
Minecraft EULA consent; existing production server configuration is not rewritten
by installing the inference plugin. Test pure Java math, API compilation, real
Paper/Folia lifecycle, export/deploy and measured scaling separately. Report
actual source revisions, hardware, active entities, throughput and limitations.
Use English for code and technical records, Japanese for operator instructions.
