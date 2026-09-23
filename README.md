# botsclustersmc

[日本語の起動・運用手順](README.ja.md) · [Architecture](docs/ARCHITECTURE.md) · [Learning](docs/LEARNING.md) · [Validation](docs/VALIDATION.md)

A Java-only, reward-trained Minecraft NPC experiment, separated into an isolated
training server and a self-contained Paper/Folia inference plugin. One current
implementation, stable filenames, no legacy compatibility or migration layers.

**The bodies are server-side Zombie NPCs, not logged-in Minecraft players.** This
is an intentional redesign: no external clients, per-agent sockets, NMS adapters,
Rust toolchain, native inference libraries, Python runtime, Maven or Gradle.
Player skins, hunger, complete vanilla mechanics and persistent NPC lives are
not implemented. There is no pretrained general-survival policy in the repository.

## Start from source

Install Git and a **Java 21 JDK**, then:

```sh
git clone https://github.com/lkjsxc/botsclustersmc.git
cd botsclustersmc
cp .env.example .env
# Read https://aka.ms/MinecraftEULA. Set EULA=true in .env only after accepting it.
./start.sh
```

Windows uses `copy .env.example .env`, then `start.cmd`. The JDK source launcher
downloads a pinned official Folia server, compiles against its real API libraries,
and creates `academy/`. Port **25565**; online authentication remains enabled for
human observers because NPCs do not log in. Startup is foreground. In another
terminal, use `./status.sh`, `./console.sh bots status`, and `./stop.sh`.
Wait for final save and process exit before backing up the whole `academy/`.

`BOTS=auto` selects `min(2048, effective CPUs * 64, selected heap GiB * 128)`.
Explicit settings are never silently reduced. A different population needs a new
empty `ACADEMY` or a fresh clone: checkpoints bind actor count. For a 16-CPU,
12-GiB allocation, `BOTS=1024` and `HEAP_GB=8` are a starting configuration, **not
a hardware-capacity guarantee**. See the measured tests before increasing count.

## Export and deploy

```sh
./export.sh
```

Copy only these files to a stopped, tested Paper/Folia server:

```text
plugins/botsclustersmc.jar
plugins/BotsClustersMC/policy.bcmc
```

Start the server normally. Do **not** install `training.jar` alongside the
inference plugin. No other process or training directory is needed. As an operator
in a non-Peaceful world, use `/bots spawn 16`, then e.g.
`/bots goal all 3 100 65 120`. Goal arguments are actor ID or `all`, task0..17 and
world coordinates. Console spawn accepts `bots spawn 16 world 100 65 100`.
`/bots status`, `pause`, `resume`, `watch <id>` and `remove <id|all>` are available.

Defaults: zero automatically spawned NPCs, `world-edits: false`, bounded count and
loaded chunks. Configure `plugins/BotsClustersMC/config.yml` while stopped.
Enabling edits can damage your world; first test on a copy. Player protection
plugins do not necessarily cover entity-driven operations. **NPC bodies and their
pockets are ephemeral, not restart-persistent**. Full server restart is supported;
plugin hot reload is not. Missing/corrupt models fail closed, without an inference
random-policy fallback or shutting down the operator's server.

## Useful scaling, not artificial CPU load

One immutable shared 384→64→64 neural policy drives eight primitive action heads.
Bounded batched inference and fixed gradient workers replace per-agent processes.
Short trajectories train asynchronously with tick-aware V-trace correction;
there is no population-wide episode-completion barrier. Each actor has its own
course and immutable-policy exam. One weak actor cannot halt everybody else.
Training islands are separated for Folia region parallelism. Paper supports the
same API but does not acquire Folia's region parallelism by installing a plugin.

Observe actual trained samples/s, policy updates, all-agent tick and action
coverage, queue pressure, rejected/stale samples and JVM CPU-core equivalents.
CPU100% is not the objective. The redesigned bodies, observations and learning
algorithm differ from the previous system, so throughput is not a controlled
old/new learning-efficiency comparison.

All18 real-server tasks have separate scripted reachability diagnostics. These
are **not learned skill evidence** and are excluded from production JARs. Curriculum
assistance, exposed state, primitive crafting limitations and frozen per-actor
exams are described in [Learning](docs/LEARNING.md).

## Development

`./build.sh` builds the two JARs; `./test.sh` adds numerical, mechanics, curriculum,
serialization, asynchronous queue and synthetic-learning tests. Only opt-in live
acceptance uses Python3. Real Paper/Folia and OS tests are in CI; inspect
[Validation](docs/VALIDATION.md) for actual completed versions, counts and limits.

Old state/configuration is deliberately rejected, never silently migrated or
deleted. Use a new clone. Historical verification records describe earlier code;
they do not certify the current architecture. Apache-2.0 project code. Minecraft
and Paper/Folia server distributions are obtained separately under their terms.
