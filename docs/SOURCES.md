# Primary technical references

These references guided interfaces and deployment choices, not empirical claims
that this package was run successfully. Exact pinned source was inspected.

- Folia README and resource/region guidance:
  https://github.com/PaperMC/Folia/blob/ver/1.21.11/README.md
- Folia region overview:
  https://docs.papermc.io/folia/reference/overview/
- PaperMC official download-service schema, stable channels and User-Agent rule:
  https://docs.papermc.io/misc/downloads-service/
- Fixed-version service endpoint used by the bootstrap:
  https://fill.papermc.io/v3/projects/folia/versions/1.21.11/builds
- Paper server.properties reference:
  https://docs.papermc.io/paper/reference/server-properties/
- Java version compatibility table (1.20–1.21.11: Java 21):
  https://docs.papermc.io/paper/getting-started/
- Java installation guidance:
  https://docs.papermc.io/misc/java-install/
- Adoptium API:
  https://api.adoptium.net/
- rustup:
  https://rustup.rs/
- Azalea pinned source:
  https://github.com/azalea-rs/azalea/tree/f8ddefa70cc53e6385785fb56e7a688a389cf0ab
- Pinned statistics packet definition (Mined/Crafted vs PickedUp/Dropped):
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea-protocol/src/packets/game/c_award_stats.rs
- Pinned ordinary statistics request:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea-protocol/src/packets/game/s_client_command.rs
- PPO paper, Schulman et al.:
  https://arxiv.org/abs/1707.06347
- GAE paper, Schulman et al.:
  https://arxiv.org/abs/1506.02438
- Minecraft EULA (must be accepted by the operator):
  https://aka.ms/MinecraftEULA

## Continuation source checks (2026-09-20 JST)

All following checks used the same pinned revision, not Azalea `main` or a
newer Minecraft protocol. Source inspection is not a successful compilation.

- Public `LookDirection::y_rot()` / `x_rot()` accessors (fields are private):
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea-entity/src/lib.rs
- Mining lifecycle, `LeftClickMine`, `Mining`, `MiningQueued`, abort semantics:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea-client/src/plugins/mining.rs
- Pinned protocol constants: 774 / 1.21.11:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea-protocol/src/packets/mod.rs
- Spawn/death/packet event boundaries:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea/src/events.rs
- Shared-entity reconnect behavior:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea/src/auto_reconnect.rs
- Container reference and client movement APIs:
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea/src/container.rs
  https://github.com/azalea-rs/azalea/blob/f8ddefa70cc53e6385785fb56e7a688a389cf0ab/azalea/src/client_impl/movement.rs


## Academy references consulted for v0.2.0 (2026-09-20 JST)

- PaperMC, Folia-aware scheduling:
  https://docs.papermc.io/paper/dev/folia-support/
- Exact-version EntityScheduler API:
  https://jd.papermc.io/paper/1.21.11/io/papermc/paper/threadedregions/scheduler/EntityScheduler.html
- Exact-version RegionScheduler API:
  https://jd.papermc.io/paper/1.21.11/io/papermc/paper/threadedregions/scheduler/RegionScheduler.html
- Paperclip extraction-only bootstrap implementation (consulted, not vendored):
  https://github.com/PaperMC/Paperclip/blob/main/java17/src/main/java/io/papermc/paperclip/Paperclip.java
- Florensa et al., Reverse Curriculum Generation for Reinforcement Learning:
  https://arxiv.org/abs/1707.05300
  This supports curriculum as a research direction, NOT this package's results.
  botsclustersmc uses an explicit fixed course, not an implementation of that algorithm.

API/document inspection does not establish plugin type compatibility or real-world
runtime correctness. No performance or learned-skill measurements are inferred
from these sources.

## 0.3.1 lifecycle repair

The exact-version EntityScheduler documentation above specifies retired callbacks
and their critical-code restrictions. A disconnected entity is a lifecycle event,
not by itself an environment failure. The repair changes only map/session cleanup
in those callbacks. Empirical evidence comes from the recorded real runs, not from
this API documentation.
