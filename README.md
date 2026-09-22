# botsclustersmc

Source-first Minecraft reinforcement learning. Clone this repository, build the
pinned Rust client, and train 32 bots inside separate Folia training cells.
**This root application is runnable.** `experimental/rl-next/` remains a separate
research library; its 18 task definitions are not 18 playable runtime stages.

Japanese installation and operation: [README.ja.md](README.ja.md).

## Start from a fresh clone

Target: Linux x86_64 with a Java 21 JDK, 16 logical CPUs, 12 GiB RAM and a
120 GB allocation. Reserve at least 20 GiB free for the initial source build.
Linux aarch64 is a source-build path but is not a live-tested target.
No Python, GPU, paid API, LLM, demonstrations or pretrained weights are needed
for normal operation. Python 3 is used only by optional developer tests.

On Ubuntu 24.04 (or an equivalent Linux distribution with Java 21):

```sh
sudo apt-get update
sudo apt-get install -y git curl ca-certificates build-essential pkg-config cmake unzip util-linux openjdk-21-jdk

git clone https://github.com/lkjsxc/botsclustersmc.git
cd botsclustersmc
cp .env.example .env
# Read https://aka.ms/MinecraftEULA and accept it yourself before this command.
EULA=true ./start.sh
```

The first start obtains a pinned Rust toolchain and Azalea source, compiles/tests
the actual Rust application, downloads pinned Gson/Folia dependencies, compiles
the Java environment bridge against the real server API, and creates a new owned
`academy/`. It builds and reads back every enclosed cell **before** joining bots.
Nothing needs to be copied from an old ZIP, a GitHub Actions artifact, or a
previous installation. Downloads require internet access on first use; artifacts
and toolchains are cached locally. A failed download/build can be retried with
the same command without deleting the directory.

Startup success messages:

```text
All 32 enclosed training cells verified.
All 32 bots are online, spawned and making policy decisions.
```

These messages prove readiness and policy decisions, not skill mastery. Check
`./status.sh` for increasing `policy_version` and `trained_samples` to verify PPO
updates. The process stays in the foreground. Use another terminal for controls.

## Connect and operate

Use **Minecraft Java Edition 1.21.11**, TCP **25565**. Human visitors are spectators;
`/academy watch 0` through `/academy watch 31` selects a training cell.
The default identities are `bcmc00` through `bcmc31`.

```sh
./status.sh
./console.sh "list"
./stop.sh
# Wait for the original terminal to report a clean shutdown before restarting.
./start.sh
```

The default `BIND_ADDRESS=0.0.0.0`, `OFFLINE_ACCESS_ACK=true` is intentional.
**Offline mode has no account authentication.** Restrict TCP 25565 to trusted
LAN/VPN/firewall sources. This application does not configure a firewall, DNS
or router. Use `BIND_ADDRESS=127.0.0.1` for local-only access.

`academy/server/` holds the world and `academy/state/` the policy, optimizer and
curriculum. Back up the **whole stopped `academy/`**, not only a weight file.
Unowned directories, changed population markers, incomplete checkpoint pairs
and incompatible observations fail rather than silently resetting training.
Do not overwrite or delete an existing ZIP installation; a new clone starts
independently unless you deliberately copy a complete compatible stopped Academy.

## What currently learns

Six real Minecraft tasks: move forward and stop, turn/navigate and stop, aim and
hold, planar navigation, a one-block step, and destruction of the designated log.
Gameplay inputs are sampled from a randomly initialized shared neural policy;
CPU PPO updates it using server-grounded outcomes. The bridge constructs/reset
fixtures and reports telemetry. It does not choose actions. No pathfinding,
auto-aim, recipe macro, imitation or LLM controller is substituted for RL.

Every bot must qualify on the current skill and retain every earlier skill
under a frozen evaluation policy; elapsed time alone never promotes a bot.
See [docs/LEARNING.md](docs/LEARNING.md) for observations, engineered rewards,
action masks, and evaluation semantics. Crafting, iron smelting, settlement and
multi-agent cooperation are **not implemented runtime stages** in this version.

## Verification and development

```sh
./scripts/preflight.sh
python3 tests/package_checks.py
python3 tests/bootstrap_checks.py
./scripts/build-host-tools.sh
python3 tests/recovery_checks.py
./scripts/test-bridge.sh
./scripts/build.sh
# Separate live test, only after the operator accepts the Minecraft EULA:
EULA=true SMOKE_BOTS=32 SMOKE_HEAP_GB=6 ./smoke.sh
```

The CI source build starts with no project binaries/runtime caches and compiles
the bridge against downloaded, pinned Folia. Live smoke is a separate acceptance
level. See [docs/VALIDATION.md](docs/VALIDATION.md) for measured evidence and limits.
See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for failure recovery.

Pins: Minecraft/Folia 1.21.11 build 14, protocol 774, Java 21,
Azalea `f8ddefa70cc53e6385785fb56e7a688a389cf0ab`, Rust `nightly-2026-02-04`.
Folia is not silently upgraded to a different Minecraft version or newest build.
