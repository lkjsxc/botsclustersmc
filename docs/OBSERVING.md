# Observe training without confusing motion with competence

## In Minecraft

Use a client matching the training server (the current pin is Java Edition
1.21.11). Training joins become spectators and receive an initial overview.
Observation does not require operator privileges in the owned training server.
Inference defaults to operator-only observation: its camera can change game mode.

| Command | Effect |
| --- | --- |
| `/bots watch` | Follow the highest-stage actor; probe readiness breaks ties |
| `/bots watch 17` | Follow the chosen actor using a third-person camera |
| `/bots overview` | Move above an actor, then allow free flight |
| `/bots tour` | Follow successive actors, changing every ten seconds |
| `/bots unwatch` | Release tracking; inference also restores the prior location/mode |
| `/bots progress` | Read the population, readiness and historical exam totals |
| `/bots list 0` | Read ten actors, ranked by current stage/readiness; pages start at zero |
| `/bots inspect 17` | Read task, action, location, speed, health, fire and policy identity |

The HUD distinguishes a frozen exam from training. Small client-only particles
mark the actual lesson target. They neither modify the world nor appear in the
policy's observations. Spectator players are excluded from nearby-entity inputs.
The camera reads immutable actor snapshots and moves the player on the player's
own scheduler. Slow or retired actors are marked stale instead of being chased
using old positions. Full server restart, not `/reload`, is supported.

`botsclustersmc.observe` grants these read-only views. `botsclustersmc.admin` is
still required for pause/resume, spawning, removal and manually assigning goals.

## In a browser

Run `./monitor.sh [private-address] [port]` (`monitor.cmd` on Windows) in another
terminal. It defaults to loopback port 8765. Explicit LAN or Tailscale IPv4 binds
allow trusted peers to read it; wildcard/public binds are refused. This is not an
authenticated public website. Do not expose it through a public reverse proxy.
The JDK-only service exposes only `/`, `/api/status` and `/api/history`; non-GET
requests are rejected. There is no command endpoint, model download or CORS grant.

The plugin produces `status.json` every five seconds and appends compact records
to `history.jsonl`. History rotates at 8 MiB and retains one previous file. The
monitor serves at most 720 valid snapshots and bounds its request queue. Metric
history is not a checkpoint; it cannot restore optimizer or curriculum state.
Snapshots older than fifteen seconds are marked stale. A cached `state=running`
is not evidence that a stopped process is still alive.

## Interpret the numbers

`trained_samples` counts accepted updates, not gameplay competence. Practice and
full-difficulty probes both contribute training data; their moving averages are
readiness estimates. Each actor's frozen exam uses one immutable policy and does
not feed the learner. A pass is historical evidence for that actor's exam policy,
not a guarantee that every future exported policy retains the same skill.

The dashboard averages readiness over actors at their **current** stages. When
actors advance into harder tasks, those averages can fall without implying that
all earlier skills were forgotten. Use stage populations and the individual HUD
alongside the curves. No survival or cooperation certificate is implied.
