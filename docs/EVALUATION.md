# Independent evaluation during training

A historical curriculum pass is not a permanent certificate for the continuously
updated policy. The operator can now test one immutable checkpoint snapshot in a
separate real Minecraft server, using the same canonical holdout environment and
primitive neural actuator as `tests/holdout.py`.

## Commands

From the same checkout and Academy configuration:

```sh
./evaluate.sh
./evaluate.sh --tasks 0,1,2,3,4,5,6 --cases 32
./evaluate.sh --watch --interval 600
./evaluate.sh --tasks 0,1,2,3,4,5,6,7,8,9,10 --export dist/evaluated.zip
./evaluate.sh --from dist/evaluated.zip --seed 2026092607 --export dist/retested.zip
```

Windows uses `evaluate.cmd`. The evaluation command requires an already accepted
Minecraft EULA and an existing owned Academy. Ordinary checkpoint evaluation
requires a valid `training.bcmc`; `--from` uses a retained evaluated ZIP instead.
It uses the JDK and Java libraries in the pinned server distribution; no Python,
npm, external inference server or API key is required for these operator commands.
The optional developer acceptance scripts still use Python and a browser driver.

Without `--tasks`, evaluation covers every task from zero through the highest
currently reached curriculum stage in that checkpoint. This includes the stage
still being learned, not only historically passed stages. The default is 32 new
cases per task. Explicit tasks are distinct comma-separated IDs from 0 to 17;
`--cases` accepts 1 to 64. `--seed` fixes the reset/action seed for a repeatable
case specification; without it, each evaluation draws a new random seed.

`--watch` repeats a check after the preceding evaluation plus the interval,
600 seconds by default. Unchanged weights and selected tasks are not retested.
Type `stop` and press Enter in that terminal, or use Ctrl+C, to stop evaluation.
This stops only its disposable test server, not the training server. Watcher
heartbeats and failed/cancelled evaluations are distinct from completed results.
Only one evaluation process may own an Academy at a time.

The command snapshots the current runtime JARs and canonical evaluator source
when it starts. Restart a watcher after updating repository source; an old watcher
does not silently replace its runtime while it is measuring a policy.

## Resource and filesystem boundaries

The command can run while training continues. It reads a complete, atomically
published `training.bcmc`, not a potentially mismatched loose `policy.bcmc`.
It never changes that checkpoint, the live world, course certificates or gameplay.

Evaluation starts an additional JVM with a default maximum heap of 2 GiB, two
Folia region threads and one inference worker. `--heap-gb` accepts 1 to 8. The
launcher is capped at 256 MiB when using the supplied shell/Windows wrappers.
Allow additional operating-system and native-memory headroom; a heap limit is not
a total-process-memory limit. Training may slow while evaluation is running.

The test server binds only to loopback, normally on a selected ephemeral port.
It enables online authentication and an empty whitelist; no humans are admitted.
`--port` may explicitly select a spare port, but never the production default
25565. The live server's port, authentication and permissions are unchanged.
The temporary test world is separately owned and removed after successful or
cancelled shutdown. A shutdown failure preserves scratch files for investigation.

## Results and interpretation

The read-only browser monitor has a separate fixed-policy panel. It displays
successes and denominators for each tested task, the tested policy identity,
time and evaluator status, alongside the independent live training metrics.
A missing or malformed evaluation does not suppress the live training panel.
The evaluator does not grade success by CPU usage, loss, elapsed time or motion.

Files in `academy/server/plugins/BotsClustersMC/`:

- `evaluation.json`: last completed summary and exact artifact identities.
- `evaluation-details.json`: bounded complete per-trial outcomes and diagnostics.
- `evaluation-status.json`: preparing/running/waiting/completed/stopped/failed state.
- `evaluation.log`: the last 64 KiB of the latest disposable server's log.

The monitor exposes only summary/status at `/api/evaluation`. Detailed files,
checkpoints, credentials and a command endpoint are not served. Completed output
is bounded; unsuccessful attempts leave the previous completed summary intact.
Every trial identity, seed, denominator, policy identity and terminal outcome is
validated before publication. The evaluator verifies that no new training samples
or training checkpoint were produced and that the frozen weights are unchanged.

A completed experiment can legitimately report 0/N. That is a failed skill,
not a failed experiment. Results are for that exact frozen policy in full-difficulty
Academy rooms, not all future policies, open-world generalization or cooperation.
The command does not automatically promote actors, select models or deploy them.
To keep a particular tested snapshot while learning continues, use the explicit
one-shot `--export` option below. The ordinary `export.sh` still exports the stopped
canonical training checkpoint, which can differ from an earlier evaluated policy.


## Retain the exact evaluated policy

`./evaluate.sh --export dist/evaluated.zip` tests one canonical snapshot, then
packages the weights actually tested together with the inference JAR from the
same immutable build, the complete trial report, a summary and deployment notes.
The live learner may keep changing its own checkpoint throughout. No model is
reread from that changing checkpoint when the artifact is produced.

The destination must be a new `.zip` file outside the Academy. Existing files,
symlinked paths and destinations inside the Academy are rejected. `--export`
cannot be combined with `--watch`: a repeating evaluation must not silently
replace a deliberately retained snapshot. A failed or cancelled test creates no
export. A completed test with failed trials CAN export; the ZIP is not a mastery
certificate or an automatic deployment.

Every trial is validated again before packaging, and the exact policy and JAR
bytes must match the identities in the completed report. The ZIP has only:

```text
README.txt
evaluation.json
evaluation-details.json
plugins/botsclustersmc.jar
plugins/BotsClustersMC/policy.bcmc
```

No optimizer, training checkpoint, world, Minecraft server JAR or private control
credentials are included. Extract the ZIP, inspect the scores and scope, then
copy the two `plugins/` files to a stopped compatible server. Installing the JAR
does not turn Academy task performance into general survival or cooperation.

The completed temporary ZIP is flushed, then atomically published using a
same-filesystem hard link. Even a target created concurrently is not replaced.
A filesystem without this operation fails closed and cleans the temporary file;
choose a local filesystem supporting hard links instead of a network/FAT volume.
The private monitor does not serve the ZIP or expose a download/command endpoint.

## Re-evaluate the same saved model

`--from EVALUATED.zip` pins the model from a previously exported evaluated bundle.
It does not read, restore or replace the live checkpoint or optimizer. Use this
when comparing different case seeds without silently switching to a newer model.
The source ZIP is read once, validated and left unchanged. Repeated evaluation
still uses stochastic primitive actions, the full task reset and no learning.

```sh
./evaluate.sh --from dist/evaluated.zip --cases 64 --seed 2026092607 \
  --export dist/retested.zip
```

Without `--tasks`, the new run uses the task list in the source report, not the
live curriculum's reached stage. `--cases` still defaults to 32 and `--seed` still
defaults to a fresh random seed; neither is implicitly copied from the old report.
Specify both to reproduce a case specification. Identical seeds do not promise
identical wall-clock scheduling in a real asynchronous Minecraft server.
`--from` cannot be combined with `--watch`. The ordinary single-evaluator Academy
lock and existing EULA, resource, publication and no-overwrite rules still apply.
An existing owned Academy is used as a result location, but its checkpoint may
be absent or unreadable as model data; there is no fallback to that checkpoint.

The source must contain exactly the five exported entries. The reader bounds
both compressed archive and decoded entry sizes, rejects duplicate/unknown names,
validates model format, model/plugin byte identities, all old trial identities
and counts, and requires the summary to agree with the complete report. Explicitly
assisted or reset-intervention reports cannot be accepted as standard evaluations.
This checks internal consistency, not external authenticity of someone else's
claimed results. The new run supplies its own independently completed trials.

**Only model bytes are reused. The archived JAR is never extracted or executed.**
The current checkout supplies the inference, environment and evaluator code.
A model-schema mismatch is rejected, without conversion or random initialization.
Each new report records `policy_source: evaluated-bundle`, source evaluation seed
and timestamp, the source inference build identity, and
`same_inference_build_as_source`. A false value means the same model was tested
with different inference code; it must not be described as an identical-build
replication. Server, training-runtime and evaluator identities in the new report
always describe the actual new run. Even a true value does not establish identical
server/evaluator/environment versions; inspect those identities separately.

`--export` retains a new bundle containing the unchanged model and the **current**
inference build with its new outcomes. The prior bundle and failed trials remain
untouched. Replaying a strong model neither installs it in the learner nor changes
course certificates. A replay still publishes to the selected Academy's last
completed evaluation panel, so its tested policy may be older than live training.

### 運用メモ

保存済みの評価ZIPに `--from` を指定すると、学習中に重みが更新されても、同じ重みを
別の乱数条件で試せます。ZIP内のプラグインは実行せず、現在のソースから作った実行環境を
使います。入力ZIPは変更しません。再評価結果を残す `--export` には別の新しい名前を指定し、
成功数だけでなく失敗した試行も確認してください。学習用データや進級記録には戻しません。
