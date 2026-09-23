# Architecture and deployment boundaries

## One runtime, two artifacts

`core/` contains model math, masked conditional action distributions, portable
model files, reusable batched workspaces, random state and bounded inference.
It imports neither Bukkit nor the learner. `plugin/` samples authoritative state
and applies primitive NPC actions. `training/` adds disposable environments,
rewards, per-actor curricula, V-trace and Adam. `host/Host.java` is a JDK-only
source-launched builder and foreground server supervisor.

`botsclustersmc.jar` includes only core + plugin. `training.jar` includes all three
modules, with a different entrypoint. Both use the name/data directory
`BotsClustersMC`; installing both in the same server is invalid. The trainer is
additionally gated by an owned-server marker and a JVM training flag. The
inference JAR contains no optimizer, curriculum or environment reset implementation.
Diagnostics are separately compiled test plugins, never production dependencies.

The build uses Java21 bytecode and real libraries extracted from the fixed official
Folia jar in `host/server.properties`. It does not resolve moving Maven snapshots
or use mock Bukkit APIs as compatibility evidence. Inference uses only public
Paper APIs. Compatible forks are plausible, not certified without live tests.
Paper26.1+ itself requires Java25. Production training does not auto-upgrade its
server pin; CI may resolve a named test version and record its exact build/digest.

## Why NPCs instead of protocol clients

Each body is an adult, age-locked Villager with the nitwit profession. It has no
undead daylight-burning classification and does not offer a trading menu. Native
mob goals and awareness are disabled; otherwise autonomous look control can
replace neural inputs. Physics remains active, but these are deliberately modified
mob semantics, not vanilla players. Peaceful worlds are allowed. Real fire/lava
are not globally disabled, and deployment bodies are not made invulnerable.
The plugin never changes operator difficulty or authentication.

The policy controls literal horizontal velocity, yaw/pitch increments, jump or
crouch, dig/use/swing, hotbar selection and individual inventory/menu clicks.
Neither a pathfinder nor a recipe/build macro decides actions. Mining uses bounded
mechanical durations and native block drops, not a complete player mining model.
A pocket/cursor/grid implements the finite recipe catalogue needed by the course.
Native furnace and chest inventories are touched only by selected clicks, with
native fuel/cooking used for smelting. Hunger, tool durability, full combat,
all recipes, skins and network/player-specific events are not provided.

Bodies and pockets are ephemeral and are not written to a persistent NPC database.
Restart creates new bodies; only policy/optimizer/course state is resumable.
Do not give production valuables to experimental NPC pockets. Full server restart
is required; Bukkit `/reload` and dynamic plugin loaders are unsupported.

## Ownership and bounded concurrency

An entity's owning scheduler reads state and applies actions. Region schedulers
handle arena blocks and spawned-chunk ownership. Chunk creation is asynchronous;
no tick callback waits for a learner, inference response, I/O or another region.
Background work receives numeric immutable observations, never Bukkit world data.
Native block/item interactions check owning-region access before reading/mutating.

Inference drains immediately available requests into batches of at most32; it
never waits artificially to fill a batch. Fixed workers share an immutable model,
reuse workspaces, and group requests with the same model identity. There is at most
one pending request per NPC. The last selected motor is held while waiting; elapsed
ticks and previous input are observed. Each submitted request captures its own
non-reused reply ticket. Replacing a goal, pausing or resetting discards that
ticket; an old result or error cannot overwrite the next request's reply. Polling
is nonblocking. An obsolete in-flight computation may finish, but is never applied.
Admission, queues and loaded chunks have explicit upper bounds and failure counters.

Moving chunk leases are reference counted. Old tickets are released on movement,
death or removal, using the appropriate region scheduler and guarding ticket reuse.
Spawns are rate-limited and pending work is cancellable. Removing all NPCs releases
tickets; it does not delete operator blocks or unload chunks forcibly.

Training rooms are one chunk each, clustered in islands of16/64/256 rooms according
to the CPU/region budget. Islands are64 chunks apart. This avoids one contiguous
campus becoming one Folia region without creating thousands of tiny isolated
regions and oversized chunk halos. Crowding every NPC in a single settlement can
still limit world parallelism. Paper world ticks remain single-main-thread work.

## Fixed local inputs and independent observation

Each decision uses 512 floats, not a copy of Minecraft chunks. Terrain already
lives in the server. Near-body block samples, local inventory/menu state,
ego-relative target/velocity, physical settling time, eight nearby entities and
eight coarse radial probes are assembled on the owner thread. Unknown chunks or
foreign regions are not loaded or read to complete an observation. This is a
bounded local state interface, not a global map, raw video or a pathfinding oracle.
It supplies information needed for future survival/cooperation experiments; it
does not itself train those behaviors or add a high-level autonomous planner.

Operator cameras consume immutable snapshots rather than reaching into another
actor's entity. Player movement uses the player's own scheduler and asynchronous
teleportation, with only one camera move in flight. Spectators do not enter the
policy's nearby-entity inputs. A separate JDK-only, private-bind HTTP monitor reads
bounded metric history. It cannot issue game commands or expose checkpoints.
See [Observing](OBSERVING.md) for permissions and the meaning of progress metrics.

## Model files and supervision

The observation/action schema has a descriptive identity, dimensions and explicit
numeric bounds. Same-sized incompatible weights are not accepted. Policy files
have checksums, finite-value checks, size limits and exact trailing-byte checks;
Java object deserialization is not used. Writes use temporary files, fsync and
atomic rename. Every managed path component, not only the final filename, is
checked for symlinks before reading or creating directories. This rejects static
misconfigured links; it is not a security boundary against a concurrent malicious
process with the same filesystem permissions. Checkpoints are not world transactions.

The full training checkpoint binds weights, Adam moments/step, individual course
and random state. Actor count must match. A corrupt/incompatible checkpoint never
silently initializes a new policy. `training.bcmc` is the single save authority;
training does not maintain a second independently committed policy file. The
stopped-Academy exporter holds the run lock, rebuilds current artifacts, validates
the complete checkpoint and derives the small deployable `policy.bcmc` from it.
A leftover loose policy is neither trusted nor migrated. Export does not ship Adam
or the course, and contains no promise of mastery. Wait for successful export
before copying the two files; two destination files are not one filesystem-wide
atomic transaction, and an interrupted export must be rerun before deployment.

The launcher uses strict key/value configuration, exclusive run/build file locks,
port checks, explicit EULA consent and authenticated loopback console control.
It refuses to initialize a nonempty unowned Academy or overwrite old configuration
through a migration shim. Public server ports, worlds and auth are untouched by
the separately deployed inference plugin. Training logs/status are observational;
status snapshots after stopping are not live-health claims. CPU core equivalents
and normalized CPU fraction use the same process-time/wall-time interval.

## References and what is not claimed

Paper scheduler/ownership guidance:
https://docs.papermc.io/paper/dev/folia-support/

Paper Java requirements:
https://docs.papermc.io/paper/getting-started/

Mob awareness semantics:
https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/Mob.html

These references justify API usage, not this project's measured correctness or
capacity. Real acceptance evidence is listed in `VALIDATION.md`. There is no claim
of universal version compatibility, linear scaling, full vanilla player behavior,
thousands of cooperating settlers, indefinite uptime or completed learning of all
18 tasks. Increasing CPU utilization is useful only when accepted experience and
held-out skill outcomes improve under comparable conditions.

## NPC daylight semantics

The current body is a Villager rather than an undead mob. It does not require
cancelling ambient combustion or changing time/difficulty. Deployment remains
vulnerable to ordinary fire and lava; training invulnerability is explicit arena
assistance. Status records current burning bodies. Real fixtures require a
Villager body and verify that generic, block-attributed and entity-attributed
combustion events are not cancelled by this plugin. Historical zombie-body
workarounds are retained in Git history, not as a second implementation.
