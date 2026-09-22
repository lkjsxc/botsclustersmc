# Learning and Academy contract — six-task source runtime

## Mechanism, not a pretrained result

One randomly initialized shared 1292→64→64 MLP with eight categorical action heads
and a critic is updated by the existing dependency-free CPU PPO/Adam/GAE core.
The 640-value single frame has 617 legacy sensor features and a 23-value academy
tail. The input includes two frames, previous actions and agent identity. Old
checkpoints with different observation/action meanings fail the environment guard.

The academy tail includes goal displacement, sine/cosine relative yaw, relative
pitch, task identity, elapsed fraction of the finite horizon, settled-hold time,
ground contact, authoritative orientation and estimated linear/angular speed.
This is state-based, goal-conditioned RL with privileged task information, not
pixels-only learning or inference of objectives from natural language.

`restrict()` in `learning/src/curriculum.rs` intersects mechanical masks with a
TASK-WIDE action availability mask. It never inspects a target to select the
correct direction. All choices, including neutral input, are sampled from the
model in training. Held angular velocity is integrated every client tick;
no auto-aim, interpolated target angle, A*, task macro or imitation dataset exists.

## Episode and reward

One owned cell fits in one chunk, with a 12x12 accessible interior, solid floor,
glass walls/ceiling and optional step/target. Initial conditions and fixtures are
part of the environment. Rust requests resets; the server acknowledges completed
teleport setup through a new run/episode-token-tagged observation stream. Client
position must catch up before collecting actions. Old run/token data is ignored;
nonfinite or malformed telemetry fails. Missing samples cannot extend hold time.
A reset deadline and stale-observation deadline stop faulty experiments.

Minecraft position, grounded state and accepted target destruction are observed on
the player's owning Folia entity scheduler. A successful mine needs an uncancelled
server break event AND the resulting AIR block checked on the owning region's next
tick. Sending a mining action or predicting a client-side removal is insufficient.
No server-side learned action is executed by the Java bridge.

Nav success: horizontal goal distance <=0.65 blocks, speed <=0.025 blocks/tick,
angular speed <=0.15 degrees/tick and grounded, maintained for 20 server ticks.
Aim success: yaw and pitch errors <=8 degrees and the same angular speed threshold,
maintained for 20 ticks. These are explicit engineered criteria, not judgments of
human likeness. Sample gaps >8 server ticks reset the hold. Tasks 0–3 end after
600 ticks, 4–5 after 900. Leaving the cell fails. Inventory resets and invulnerability
make these foundational input exercises, not normal survival tasks.

The academy disables the open-world exploration/frontier/shared mining rewards.
For transition s→s', the task reward is:

```
(success ? +2 : failure ? -1 : 0)
+ gamma * Phi(s') - Phi(s)
- 0.002
- 0.00005 * absolute_yaw_travel_degrees
- (movement_button_changed ? 0.003 : 0)
```

`gamma=0.997`; terminal potential is zero. Navigation potential is
`-0.15 * min(horizontal_distance,20)`. Aim/mine potential is
`-0.003 * (abs(yaw_error)+abs(pitch_error))`. Server-frame intervals can be longer
than four ticks under load; discounting remains per recorded decision, not exact
continuous-time discounting. Do not claim identical objectives at arbitrary TPS.
Input-change/turn costs are small regularizers, not human movement data.

Timeout is a true terminal in this finite-horizon task, whose elapsed time is
observed. Bootstrapping across an episode reset is prohibited. Collected training
fragments include real terminal rewards. Generation transitions discard pending
old-phase fragments, and PPO rejects stale behavior-policy versions.

## Synchronized stages and held-out evaluation

Stages are in code, not hardcoded policy skills. Train until every agent has 40
completed CURRENT-stage trials. Approximately every fifth training episode reviews
an earlier task; these do not count toward the 40. Review selection cycles using
`serial/5`, avoiding the modulo-five alias that would review only one task at stage5.

Only the learner, between updates, switches phase. It clears queued training
fragments, freezes the model and starts evaluation. All agents choose greedy actions
under that frozen model. Their evaluation trajectories never reach PPO. Current
scores and retention scores are per-agent, not pooled. Each agent needs 14/16 on
its current task and 3/4 on EACH prior task. Stage0 uses an extra base set of four.
Separate deterministic PRNG domains supply train/evaluation task initializations;
the distributions overlap, and discrete coordinates can repeat by chance.

Pass one stage at a time; failure returns to training at the same stage. Success
on the final stage records a historical foundations pass, but does not release
bots, implement crafting or certify ongoing mastery. Training/retention checks
continue. Increasing elapsed runtime never bypasses a failed gate.

`BCMC_MODE=eval` loads weights and runs one frozen evaluation cohort without saving
or promoting; when all trials end it waits until stopped. `BCMC_MODE=random` is an
explicit no-training diagnostic baseline, never a failure fallback. Automatic
train/evaluate/promotion cycles use the default `BCMC_MODE=train` only.

## Persistence and evidence

`policy.bcmc` stores model/optimizer/RNG; `academy.bcmc` stores stage, serials and
training-count progress bound to the policy version and weight fingerprint.
The exact bot count must match. Incomplete exam scores are not resumed; a new
frozen exam is triggered when restored training counts already qualify. Per-file
fsync/rename is not an atomic world+model+course transaction. Restore stopped FULL
backups on pair mismatch, never silently initialize over damaged evidence.

`academy-episodes.csv` records run, agent, serial, phase, stage, policy version,
result, duration, yaw travel and movement changes. Rotation/idle statistics should
be compared over repeated seeds, not interpreted alone as competence. Logs are
bounded; preserve benchmark runs separately. `verify-academy` checks recorded
lifecycle and within-exam policy-version consistency when exam rows exist, not
successful learning. Failed tasks count as real experience, not as skill passes.
