# botsclustersmc 0.5.0

A source-first, mostly-Rust Minecraft/Folia reinforcement-learning Academy with
64 default actors, 18 integrated task environments, and human-only observer tools.
[日本語の起動手順](README.ja.md)

The real `app/` actors and learner use the canonical mechanisms in `learning/`.
`experimental/rl-next` now re-exports that implementation for standalone numerical
checks; it is not a separate update to install. No pretrained policy is included.
Implemented tasks are not a claim of learned skills, retention, human likeness,
open-world survival or cooperative living. Read [validation](docs/VALIDATION.md).

## Clone and start on Linux

Linux x86_64 and a Java 21 JDK are the tested target. The operator allocation is
16 logical CPUs, 12 GiB RAM and 120 GB storage, not a measured capacity guarantee.
Reserve at least 20 GiB before the first Rust build and 10 GiB during operation.
The runtime requires neither Python nor a GPU nor an LLM/API key.

```sh
sudo apt-get update
sudo apt-get install -y git curl ca-certificates build-essential pkg-config cmake unzip util-linux openjdk-21-jdk
git clone https://github.com/lkjsxc/botsclustersmc.git
cd botsclustersmc
cp .env.example .env
# Only after reading and personally accepting https://aka.ms/MinecraftEULA:
EULA=true ./start.sh
```

The initial source build fetches pinned Rust/Azalea dependencies, Gson, and
Folia, compiles the actual native application and bridge, then creates a new
owned `academy-v2/` world. Subsequent starts resume its checkpoint. No old ZIP,
GitHub artifact or manually transferred binary is needed.

Default actors: `bcmc00` through `bcmc63`. Port: **25565**. Offline-mode binding:
`0.0.0.0`, with explicit `OFFLINE_ACCESS_ACK=true`. This flag provides **no
identity authentication or firewall protection**. Restrict connections to trusted
LAN/VPN/firewall sources. Public unrestricted offline servers are not a safe
operator configuration. The launcher does not configure DNS or port forwarding.

```sh
# In another terminal at the same source root:
./status.sh
./console.sh "list"
./stop.sh
# Wait for the foreground supervisor's clean-shutdown confirmation.
./start.sh
```

Ctrl+C also requests checkpoint-first shutdown. Preserve the entire stopped
`academy-v2/` directory for a backup. `state/training.bcmc` atomically binds the
policy, Adam, trainer/actor RNG and adaptive curriculum. The Minecraft world and
that file are not one atomic distributed transaction. In-flight experiences
are counted, not invented as terminal rewards or replayed after restart.

## Watch with Minecraft Java Edition 1.21.11

Observers join as spectators. `/academy` opens a two-page clickable actor list.
`/academy watch 0..63`, `/academy next`, `/academy prev`, `/academy overview`,
`/academy tour` and `/academy view 3..16` provide individual rooms, a whole-campus
view and a 12-second automatic tour. The action bar shows task, state, difficulty,
training-success EMA and policy version. The TAB footer shows the course stage,
trained samples and collection barrier.

Observer view distance defaults to **12 chunks**, independently of the actors'
3-chunk view and the 3-chunk simulation distance. Set the human client's render
distance at least as high as the requested server distance. Entity tracking is
expanded for viewing the campus; the server reserves 16 extra connection slots.
Observer controls do not change actor gameplay, curriculum or reward state.

## Existing 0.4.0 installations

Stop the old process **before** updating its scripts. Run `git pull --ff-only`,
and change an existing `.env` from `BOTS=32` to `BOTS=64` explicitly: Git does not
overwrite private configuration. After personal EULA consent, start the new
Academy using `EULA=true ./start.sh`.

The old `academy/` world and split checkpoints remain untouched. v2 observations,
GUI distribution and checkpoint schema are incompatible: start a new v2 policy,
not silently imported v1 weights. Existing v2 population changes also fail closed.
Use an independent source directory for another population or incompatible trial.

## Learning contract

A randomly initialized shared 1420→64→64 MLP chooses eight categorical vanilla
input heads. No pathfinding, auto-aim, recipe macro, demonstrations, imitation,
LLM controller or scripted failure fallback drives the normal actors. This is
privileged state/goal-conditioned RL, not pixels-only control.

Strict bounded cohorts freeze the behavior policy until every actor reaches its
quota and a real episode boundary. Collection never silently trains a partial
population. PPO uses measured server-tick durations, conditional GUI likelihoods
and entropy gradients, and a final whole-batch KL transaction that retries model,
Adam and RNG together. Numerical guards do not establish improved learning speed.

Adaptive practice and rehearsals feed separate full-difficulty probes. Frozen
exams never enter the learner. Every actor must pass 14/16 current trials and 3/4
for each previous skill. Merely letting time pass cannot promote a stage.

The 18 environments cover motion, aiming, log breaking/collection, placement,
planks/sticks/workbench, wooden and stone pickaxes, cobblestone, iron extraction,
a designated chest, a three-block platform and a log-to-workbench chain. Furnished
raw resources, tools and stations are disclosed initial conditions. Some easy
training resets prefill recipe ingredients or open menus; full probes/exams do
not. These are bounded exercises, not a free-living settlement.

See [learning details](docs/LEARNING.md), [validation](docs/VALIDATION.md) and
[troubleshooting](docs/TROUBLESHOOTING.md). Developer tests use Python 3; normal
runtime does not. `tests/live-client.rs` is a separately built **scripted diagnostic**
for reachability and observer commands, never linked into the learned actors.
