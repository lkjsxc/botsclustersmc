# Source runtime restoration — 2026-09-22

This record accompanies the restoration of the runnable application from the
supplied v0.3.1 archive. It is not the RL-next synthetic component benchmark.

Completed local checks so far: 19 shell/filesystem tests, 8 bootstrap negative
and pin tests, 24 Java host recovery tests, Java planner/protocol/configuration
checks, 34 native learning-core tests, 53 native launcher tests, and 63 separate
RL-next component tests. Native adapter compilation and the new live lifecycle
are still pending in this intermediate record; no completion is implied here.

Dependency context: the local sandbox has no GitHub/Maven DNS access. It reuses
the supplied pinned Rust/Azalea dependency kit and byte-verified Folia caches.
Actual Rust application binaries are compiled from restored source, not copied
from the old executable. Independent network bootstrap is a separate CI gate.

Target runtime: 32 bots, 25565, a new owned Academy, six foundations. There is no
claim of 18 playable stages, faster learning, full-course mastery or cooperation.
