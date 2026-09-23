# Troubleshooting — integrated Academy v2

Start from the source root using `./start.sh`. Do not invoke the native actors
or launcher directly as a substitute for the public startup guards. Ordinary
operation uses `academy-v2/`; the old `academy/` remains separate and untouched.

## Capture the first failure

```sh
./scripts/doctor.sh
./status.sh
./scripts/diagnostics.sh
```

Read the first error, not only the last stack trace. Current logs are under
`academy-v2/logs/` and `academy-v2/server/logs/`. A bridge fatal receipt is in
`academy-v2/.runtime/lab/fatal.txt`. A receipt from another run is not evidence
about the present run. Generated diagnostics exclude `.env`, worlds and model
weights, but review names, addresses and paths in logs before sharing them.

## First build or dependency download fails

The supported runtime target is Linux; macOS/Windows users should execute the
commands on their Linux server. Java must be a version21 **JDK**, not only a JRE:
`java`, `javac` and `jar` must be available together. Missing shell/compiler tools
are reported by `scripts/preflight.sh` with installation commands.

Initial compilation needs at least20 GiB free storage. It uses pinned Rust
nightly-2026-02-04 and the recorded Azalea revision; do not fix compiler errors by
silently changing protocol or dependency versions. Check DNS/HTTPS access to
GitHub, Rust, Maven Central and the pinned Folia/Minecraft download hosts. Normal
restarts reuse verified dependencies; they do not require an old distribution ZIP.
The Gson host dependency and Folia pin are part of source-first bootstrap.

A build receipt is a local integrity check, not proof of compilation on a different
machine. Source or executable mismatch causes a rebuild. Preserve full error logs
rather than copying an older executable over the new source.

## Population or state-schema mismatch

The defaults and supported range are64 actors and1..64 respectively. A private
`.env` is not changed by `git pull`: change `BOTS=32` to `BOTS=64` explicitly when
upgrading the old installation. Stop the old process using its original scripts
**before** updating the checkout.

New source creates `academy-v2/` and does not migrate v1 observations, weights or
split checkpoints. The old `academy/` is preserved. Do not copy its model into
v2 or edit ownership/context markers to bypass a guard. Existing v2 actor counts
must also match exactly. Use another source directory for a different population.
An incompatible model is not repaired by deleting only a marker or one state file.

`state/training.bcmc` atomically binds model, Adam, optimizer/actor RNG and adaptive
course. If it is corrupt or incompatible, restore a **complete stopped backup**.
Do not combine a world and checkpoint from arbitrary different times. World and
checkpoint persistence are not a single distributed atomic transaction.

## No learning progress, waiting actors, or unchanged stage

`./status.sh` shows the policy version, actual trained samples and each actor's
current task/state. The raw files are `academy-v2/state/status.json` and
`academy-v2/state/academy-status.json`. Old v1 fields such as `foundations_passed`
are not used; the v2 historical completion field is `course_completed`.

Actors collect under one frozen behavior policy. Once an actor reaches its quota
AND a real episode end it releases input and waits for all others. This pause is
expected. The status includes cohort samples, sealed actors, local untrained
transitions and unfinished actions. Zero discarded rollouts does not mean that
all in-flight work has been trained at shutdown. A missing or stalled actor
cannot authorize training on an accidental subset.

If policy versions advance but the stage does not, inspect per-actor full-difficulty
probe and exam results. Forty practice trials alone do not establish readiness.
Every actor must qualify for the frozen exam and pass14/16 current trials plus
3/4 for EACH earlier skill. Runtime does not promote by elapsed wall time. Failed
trials are valid learning data but not evidence of mastered behavior.

All18 task environments are integrated. A stage existing in code is still not a
claim that the current model learned it. Furnished stations/tools, easy-reset
assistance, real vanilla-input reachability and actual learned competence are
distinct. Consult the source-specific evidence in [VALIDATION.md](VALIDATION.md).

## Human observers cannot see the whole campus

Connect with Minecraft **Java Edition1.21.11** to the server IP and port25565.
Human observers join as spectators. Use `/academy` for the two-page actor menu,
`/academy watch 63` for the last room, `/academy overview` for the campus, and
`/academy tour` for automatic12-second visits. Next/previous commands wrap around.

Human view defaults to12 chunks and can be changed with `/academy view 3..16`.
Set the client render distance at least as high. Very low client entity distance
can hide remote actors even when their rooms are visible. The actors' own view
and the simulation distance stay at3; increasing all64 actors' view is not the
intended way to improve spectator visibility. Actual graphical frame rate and
external-player network conditions require testing on the operator's setup.

## Network and port failures

Port25565 must not already belong to another server. The launcher checks this
rather than sending learners to an unrelated world. `BIND_ADDRESS=0.0.0.0` accepts
connections on available interfaces; it does not configure a router or DNS.
Offline mode has **no account authentication**. `OFFLINE_ACCESS_ACK=true` is an
acknowledgement, not protection: restrict access to trusted LAN/VPN/firewall peers.
Training names `bcmc00`..`bcmc63` are reserved for local learner connections.

## Safe stop and recovery

`./stop.sh` submits a stop request. Wait until the foreground `start.sh` process
reports `Clean shutdown: model, curriculum and world saved.` and exits. Ctrl+C
also requests checkpoint-first shutdown. Do not kill Java first, start a second
instance, update source, or copy backups while the first process is stopping.

Take a backup of the complete stopped `academy-v2/`. Restart with `./start.sh`;
EULA consent previously recorded for that Academy is reused. A new Academy needs
personal EULA consent again. A requested shutdown may leave untrained in-flight
experience; those counters remain visible, and new episode tokens are used on
restart rather than inventing a final transition.

## Developer acceptance, not repair commands

`smoke.sh` creates an isolated world and real neural learners for lifecycle/resume
checks. `scripts/test-live-fixtures.sh` builds a separate scripted client for
full-difficulty task and observer-command reachability; it creates no model or
training data. `tests/public_entry_checks.py` requires a new disposable checkout
and refuses an existing `.env` or Academy. These scripts are not substitutes for
operator backups or long-run learned-skill benchmarks. See VALIDATION.md for
explicit consent requirements, exact source revisions and remaining limits.
