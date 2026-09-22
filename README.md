# botsclustersmc

The project goal is 32 Minecraft Java/Folia bots learning from rewards using native Rust agents and ordinary client actions.

## Current repository deliverable

[RL-next components](experimental/rl-next/README.md) contains research-informed Rust learning components and a reproducible synthetic verification executable. It includes adaptive practice and rehearsal, frozen per-actor exams, on-policy collection checks, conditional action likelihoods, elapsed-tick GAE, guarded PPO updates, checkpoint helpers and advanced task evidence validators.

**This is not an integrated update to the separately supplied v0.3.1 Minecraft distribution.** The `bcmc-rl-check` executable is a component test program, not a Minecraft bot client or launcher. Defining 18 task contracts does not make 18 stages playable or learned. Do not replace a working Minecraft installation with this component artifact.

See [remaining integration and acceptance requirements](experimental/rl-next/INTEGRATION.md) for the missing Azalea/Folia application work. CI reports component tests and reward-only synthetic learning separately from Minecraft evidence. There is no measured improvement over v0.3.1 in a live Minecraft learning run yet.

Normal application integration must retain port **25565**, **32** bots, the `bcmc` bot prefix, explicit EULA/offline-access acknowledgments, and preservation of existing worlds/checkpoints. The experimental crate itself does not connect to a server or alter operator state.
