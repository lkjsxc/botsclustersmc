# Changelog

## 0.7.3 — workstation progress and observable menus

- Correct recipe progress and raw reset placement for physical 2x2/3x3 grids;
  never count a result preview as an ingredient or credit a pickaxe in a 2x2 menu.
- Add bounded, discounted workstation potentials and practice-only staged raw
  preparation without changing full-condition exams or choosing gameplay actions.
- Show immutable menu, cursor, preview and numbered cells in NPC inspection;
  distinguish actual menu types in independent neural trial diagnostics.
- Preserve failed reward-scale trials in verification evidence. A source/runtime
  update or successful software test is not a learned-survival certificate.


## 0.7.2 — independently measured progress

Add the native `evaluate` command and an independent evaluation panel.
Test each frozen policy in actual Minecraft rooms and show its measured task
success counts separately from the continuously changing learning process.
Periodic evaluation supports graceful stopping and skips unchanged policies.
The software does not claim completed survival or cooperative settlement skills.


## 0.7.1 — resource attempts and retained-skill diagnostics

- Display named selected controls, held items, actual mining contact and episode
  break/pickup/crafting counts without reading another region's mutable state.
- Separate cohort readiness, exact process-local outcomes and historical frozen
  certificates. Record actual learner sample allocation by task.
- Reject already disallowed mining before reporting partial progress.
- Add practice-only log orientation and bounded nonpositive resource state costs;
  real block/drop success criteria and full-condition exams are unchanged.
- Balance present-task loss mass within each learner batch, with bounded weights,
  unchanged behavior correction and raw accepted-sample accounting.
- Evaluate a frozen policy directly from a copied canonical checkpoint; retain
  every failed trial and its contact/aim diagnostics. This is not a survival or
  all-skills mastery release.


## 0.7.0 — 2026-09-23

- Replace the undead body with a neutral Villager. Do not cancel real combustion
  events or alter the operator world to hide a daylight-body mismatch.
- Add permission-separated spectator tracking, tours, goal markers, per-actor
  diagnostics and a JDK-only private, read-only web observatory.
- Expand the shared model to 512 inputs and 96-unit hidden layers. Add bounded
  body-relative motion, stillness, liquid, nearby-entity and radial terrain inputs.
  No per-NPC chunk transfer, pathfinder, auto-aim or teacher actions are introduced.
- Allow backward recovery in forward-stop; fix unreachable crafting count checks.
- Stabilize V-trace learning with bounded batch formation, a weak uniform legal
  control prior and measured conditional-KL backtracking of candidate Adam updates.
- Introduce progressive aiming practice and explicit angular-error costs; keep
  full-condition exam requirements and record an independent fixed-policy test.
- Retain canonical checkpoint export, ancestor-path checks, unique inference reply
  tickets and the persistence/concurrency regressions from 0.6.1.

The observation/body schema is intentionally incompatible with earlier weights.
Preserve old Academies separately; never silently reset them. Actual operator
learning and software acceptance are documented separately in Validation. The
project still does not implement complete vanilla-player survival or a persistent
autonomous settlement.

## 0.6.1 — 2026-09-23

- Save one authoritative training checkpoint and derive deployment weights from
  it under the Academy run lock. Rebuild current JARs before export; refuse live,
  missing or corrupt-state exports rather than copying an older cached policy.
- Reject symlinked ancestors before managed reads or directory creation, including
  the plugin configuration path. Do not modify files behind rejected links.
- Give each inference submission a non-reused reply ticket so delayed responses
  and failures from cancelled goals cannot overwrite or poison current requests.
- Use a common interval for JVM CPU-core and normalized-utilization measurements.
- Suppress run-scoped unattributed combustion that killed deployed NPCs in daylight
  on the pinned server despite its daylight flag. Preserve block/entity fire events,
  expose the suppression counter, and extend real inference acceptance windows.
- Add actual-filesystem and source-launcher export regressions, deterministic
  inverted callback completion tests, exact resume-counter checks and real-server
  rapid-goal-replacement acceptance. Live CI always requires affirmative dispatch.

Artifact names and the current observation/action schema are unchanged. There
are no legacy loaders, alternate generations or automatic state migrations.

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
