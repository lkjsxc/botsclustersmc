# Integration status — Academy v2

The previous component-only boundary has been removed. Canonical source resides
in `learning/src/next`, and the normal Minecraft application is wired to it:

- `learning/src/control.rs`: 64-actor policy/lesson leases, strict bounded cohorts,
  chronological fragments, drained update/exam publication and RNG ownership.
- `learning/src/ppo.rs` + `network.rs`: shared conditional action distribution,
  duration-aware GAE, whole-batch final KL and model/Adam/RNG retry transactions.
- `learning/src/bundle.rs`: one atomic policy/optimizer/actor-RNG/course checkpoint.
- `learning/src/curriculum.rs` + `app/academy.rs`: all 18 task geometries,
  authoritative episode rewards, bridge protocol and recorded results.
- `bridge/src/.../BotsClustersMCLab.java`: owning-region/entity resets and real
  block, item, craft, furnace, chest and placement evidence.
- `ObserverUI.java`: read-only human viewing without increasing all actors' view.

Full-difficulty probes/exams do not receive recipe-grid or menu-opening assistance.
Some easy training resets do; this is explicitly disclosed initial-state shaping,
not gameplay performed for the policy. Supplied stations/tools are not skills.

The public runtime is `./start.sh`; defaults are 64 actors and port25565. It owns
`academy-v2/`, not the old 32-actor `academy/`. v1 input/action/checkpoint semantics
are incompatible and are not silently reused. Standalone checks re-export the
same canonical code and must run inside the complete repository checkout.

This integration status is **not a claim that every task has been learned**.
Native tests, actual API compilation, real learning lifecycle, scripted fixture
reachability and learned skill are distinct evidence levels. The current source
revisions, completed tests, failed attempts and untested cases are recorded in
[docs/VALIDATION.md](../../docs/VALIDATION.md). No timed promotion or automatic
release into a free-living settlement is implemented.
