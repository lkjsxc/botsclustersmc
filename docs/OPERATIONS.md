# Source-first operations

The public entrypoints are `start.sh`, `status.sh`, `console.sh`, `stop.sh`.
Run them at the clone root. `start.sh` exclusively launches an owned Academy.
Do not invoke a binary directly to bypass lifecycle or environment checks.

First start obtains and builds the pinned native dependency graph. It downloads
Gson 2.13.2 from Maven Central for host-only JSON/status helpers and Folia 1.21.11
build 14 from PaperMC. Minecraft's upstream Paperclip obtains its own dependencies.
Jars, compiler caches and binaries are not tracked in Git. Keep 20 GiB available
for the initial build and the configured runtime disk reserve. Native artifacts
are reused only while their platform/source/artifact receipts match. Rustup does
not contact an update service when the exact pinned compiler already works.

Edit the root `.env`, never the generated `academy/.env`. Root defaults are
32 bots, TCP25565, public bind, offline acknowledgement, Java heap 6 GiB and six
region threads. A heap is not the total process/cgroup usage. Smaller test hosts
must explicitly reduce heap/thread settings without pretending to validate the
recommended allocation's capacity.

The learner is a randomly initialized CPU PPO model. Policy, optimizer/RNG,
reward ledger, curriculum and world live under the independent `academy/`.
Stop first and wait for the clean shutdown message before copying the entire
Academy. Do not independently swap checkpoint components. Incomplete or
incompatible evidence fails closed instead of silently starting a new model.
An interruption during initial downloads/builds is retryable with `start.sh`.
No process that happens to occupy TCP25565 is killed automatically.

Offline mode is deliberate and unauthenticated. Limit inbound TCP25565 to a
trusted LAN/VPN/firewall. Human spectators cannot use bot identities from a
remote address, but this is not a substitute for network isolation. DNS and
router port forwarding remain the operator's configuration.

For a user service, first complete an interactive EULA-approved start/stop,
then edit the paths in `deploy/botsclustersmc.service`, install it under
`~/.config/systemd/user/`, and enable it with systemctl --user. No system service,
package manager, firewall or router write is performed by startup scripts.
