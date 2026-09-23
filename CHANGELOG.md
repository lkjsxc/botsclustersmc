# Changelog

## 0.6.0 — 2026-09-23

Breaking Java-only redesign. Logged-in Azalea players are replaced by public-API
in-server NPC bodies. This changes the physical/observation experiment; no old
weights, optimizer, configuration or runtime directory are migrated.

- Separate stable `botsclustersmc.jar` inference and `training.jar` artifacts;
  export a model plus plugin into a production-style plugins directory.
- JDK-only source build and clone/start/status/console/stop/export tools, including
  Windows wrappers; explicit EULA, online human authentication and owned state.
- Bounded shared batched inference, fixed gradient workers, short asynchronous
  trajectories, elapsed-tick V-trace and explicit policy-lag accounting.
- Independent adaptive courses and frozen per-actor exams; no population-wide
  collection or promotion barrier. Conserved raw-material reset assistance is
  absent in full probes and exams.
- CPU-aware separated training islands, bounded spawn admission, moving chunk
  leases, actual all-agent progress, throughput, CPU and queue telemetry.
- Numeric/mechanics/course tests, five-seed synthetic learning checks,18 separate
  scripted real-world task fixtures, source lifecycle and two-file deployment tests.

Known limitations: NPCs are not players; recipe/mechanical coverage is limited;
NPC pockets/bodies are ephemeral; hot reload is unsupported; universal versions,
full18 learned mastery and thousands of cooperative persistent settlers are not
claimed. Read `docs/VALIDATION.md` for measured versions, source and exact scope.

Previous implementation history and its evidence remain in Git and in dated
historical verification records, not in active compatibility wrappers.
