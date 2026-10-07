# Communal container boundary: implementation and activation plan

Date: 2026-10-07. Accepted parent: `5d12ad9beecf4fc96f25abb6f6e830533bf98294`.

The operator's target is a population maintaining a shared settlement. Shared
world stock must not be silently taken from protected storage or destroyed before
its native loot exists. The current production adapter checks region, range and
edit permission but does not distinguish locked or loot-table-bearing containers.
The empty-container mining check likewise omits those block-entity properties.
This is a source observation; the live reproduction is still to be run.

Implement one common owner-thread gate for menu opening, current click resolution
and container mining. A placed, unlocked chest/furnace with no attached loot table
keeps ordinary operations. Locked, loot-bearing, unplaced, unavailable or foreign
containers are not usable storage and are not silently treated as empty/mineable.
Use only the selected chest block's local half. Do not unlock, generate loot,
change permissions, force chunk loads, add teacher actions or change the model.
Check again after a block-change listener and after an earlier action observation.

Qualification planned before implementation:

1. Add a real-server regression fixture and require an unmodified parent runtime
   to fail the locked-container assertion. Preserve this failure separately.
2. Pure real-API proxy tests require ownership/loading before state reads,
   lock/loot checks before inventory access, and local half/snapshot selection.
3. Actual disposable Folia and Paper fixtures cover chest/furnace access,
   revocation between observation and action, locked/loot-bearing mining,
   listener-added protection and still-functional ordinary operations. Existing
   full 18 scripted tasks and the two-body resource chain must still pass.
4. Full Java/API/artifact-separation suite and independent Python source tests.
   Exact-source Linux/Windows/browser CI is separate from physical qualification.
5. Normal reviewed commit/push/PR integration, followed by an independently read
   main ref. Build/install only the integrated source via the established owned
   development service, controlled restart, and fresh 512-actor progress, learning
   and failure-counter checks. Preserve configuration, authentication, unrelated
   worlds and failed research branches. Do not force-push or hot reload.

No reward, curriculum, learner, observation schema or checkpoint format is changed.
The source-only research PRs #66-#69 and the public-bank plan are independent and
are not adopted or treated as physically qualified by this work. This is a new
production mechanical regression, not a retry of their saved-policy experiments.
No new learning-benefit study is declared. Fresh post-deployment learning is a
health check, not proof of retention, learned teamwork or autonomous survival.

API references:
- https://jd.papermc.io/paper/1.21.11/org/bukkit/block/Chest.html
- https://jd.papermc.io/paper/1.21.11/org/bukkit/loot/Lootable.html
- https://docs.papermc.io/paper/dev/folia-support/
