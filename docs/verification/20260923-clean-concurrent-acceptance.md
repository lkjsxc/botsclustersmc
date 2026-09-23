# Final clean build and concurrent observer acceptance — 2026-09-23

## Identity and result

[Final integration acceptance, run 35817819877](https://github.com/lkjsxc/botsclustersmc/actions/runs/35817819877)
completed successfully. The actual tested source was
`c5aef6951fa104a4313a37cfb10ba7100ca81483`, created from trigger
`95a70a96567d41dfed6467c5322b3d6e8c58c60c` by the reviewed finalization step.
That step removed the one-shot maintenance generators and temporary import
workflows before building and testing. Subsequent delivery commits add only
research/verification documentation and an opt-in acceptance workflow, not a
new runtime implementation.

Job `107043634757` passed every step. Evidence artifact `10732746856`
(`final-integration-evidence`, 62,237 bytes) contains the build manifest and
whitelisted disposable logs/status. It contains no operator worlds, credentials,
private config, neural weights or demonstrations. Artifact retention is temporary;
this record and the committed tests preserve the result and reproduction path.

The preceding [complete integration record](20260923-rl64-integration.md) retains
failed attempts, exact corrections, task definitions and an independent passing
cached-build run. This record adds an empty-project-cache build and an actual
observer connected simultaneously with the 64 reward-trained actors.

## Uncached source build

The test asserted absence of `.maintenance`, `bin`, `runtime`, `.build`,
`academy`, `academy-v2`, `server`, `state` and `.env` before building. No project
Rust/Cargo/native/runtime cache or old distribution executable was restored.
The Ubuntu 24.04 runner supplied standard build tools and Temurin Java 21.0.12;
project dependencies were downloaded and the actual application was compiled.

Passed suites: 19 package checks, 8 bootstrap checks, 24 recovery checks,
97 native core tests in both debug and optimized builds, 118 launcher tests and
106 actual Azalea adapter tests. Native counts overlap and must not be summed
as unique tests. Pure Java checks covered all 18 task requests, actor 63, unsigned
seeds, invalid inputs, resource-conserving assistance and 2,654,208 bounded geometry
positions. Real Folia API compilation, plugin loading and server operation were
then exercised by the live tests, separately from Java syntax checks.

The explicit Azalea inventory/cursor and self-identity picker patch was applied
to the immutable pinned source during the normal build, not supplied as an old
binary. Runtime pins remain Minecraft/Folia 1.21.11 build 14, protocol 774,
Azalea f8ddefa70cc53e6385785fb56e7a688a389cf0ab, Rust nightly-2026-02-04 and JDK 21.

## Repeated full-difficulty reachability and reset check

All 18 task fixtures again reached their authoritative success gates on real
Folia using the separately compiled scripted literal-input diagnostic. They used
full difficulty with training-only recipe/menu reset assistance disabled. No
policy was initialized or trained by this diagnostic.

Elapsed server ticks, in zero-based task order 0 through 17:

```
72, 88, 37, 137, 90, 91, 105, 78, 25, 48, 96, 180, 92, 177, 288, 44, 184, 244
```

These are scripted execution durations, not neural learning durations. The extra
same-actor crafted-plank-cursor to fresh stick-task reset also passed (48 ticks).
The platform had all three target cells occupied; the chest retained four logs;
the final composed task had actual log destruction/pickup and acquired crafted
workbench evidence. The separate observer menu, page two, watch63, wraparound,
overview, tour and 12/16-chunk radius checks passed too.

## Public 64-actor learner with a simultaneous observer

The fresh normal Academy used the real public source-root `start.sh`, `status.sh`,
`console.sh` and `stop.sh`, not the scripted task controller. Settings were
64 actors, 5 Hz, 64-step fragments and a 4,096-sample minimum cohort quota. Full
current episodes are completed before updates, so actual batches exceed that
minimum. This is the normal learning configuration. Only test heap/region threads
were reduced to 3 GiB / 2, socket binding was loopback, and a 600-second safety
cap was set. The script stopped each run explicitly after its acceptance checks;
it did not claim a 600-second steady-state run.

After two initial PPO updates, a 65th read-only observer connected to the same
running server. It exercised the actual observer UI and received both the live
per-actor task/difficulty/PPO action bar and the live trained-sample TAB footer.
It also received radius 12 and 16 packets, used both menu pages, selected actor63,
wrapped next/previous, entered overview and toured. Production training remained
active; the test checked another PPO update (version 2 to 3) through the observer
visit and following progress check, with no recorded actor disconnect or learner
error. The observer wrote no lesson requests, rewards or training parameters.

| Measurement | Initial run with observer | Resumed run |
| --- | --- | --- |
| Run ID | 1790137761601552200-12048 | 1790137955977524578-15184 |
| Policy version | 0 to 3 | 3 to 5 |
| Trained samples | 0 to 26,692 | 26,692 to 46,196 |
| Adam steps | 0 to 1,257 | 1,257 to 2,175 |
| Initial policy fingerprint | 5d45a3b661be8981 | f84e19b6ec130616 |
| Final policy fingerprint | f84e19b6ec130616 | 6bf781568fad0b3b |
| Last whole-batch sampled KL | 0.016057 | 0.000999 |
| Last optimizer transaction | 5,339 ms | 5,869 ms |
| Unexpectedly dropped rollouts | 0 | 0 |
| Recorded learner error | empty | empty |

All 64 actors had completed real server-observed episodes (at least three each
in the first run and two each in the resumed run). Both runs saved valid atomic
policy/course/RNG bundles and shut the world down cleanly. The resumed run restored
the exact previous policy fingerprint, version, sample count and Adam step count
before two further updates. Native serialization tests cover optimizer moments
and RNG round trips; this is not deterministic replay of a live world trajectory.

The legacy `academy/` sentinel and root `.env` stayed byte-for-byte unchanged.
V2 state is independent. Untrained cohort/local samples and unfinished actions
at shutdown are explicitly accounted for in course status; zero unexpected
rollout drops does not mean unfinished actions were turned into training data.

## Interpretation and reproduction

This establishes an actual source build, all task reachability, 64-actor normal
learning/save/resume and concurrent observation without a recorded disruption in
the bounded test. It does not establish trained mastery of those 18 tasks, faster
learning than the old algorithm, indefinite reliability, high client FPS, WAN
connectivity or a capacity guarantee on different hardware.

The headless observer diagnostic disables only its own unsupported spectator
survival-physics simulation. Normal RL actors and gameplay fixture clients use
ordinary physics. Server teleports, menus and packets are real; no desktop-client
rendered screenshot or frame-rate measurement was performed.

[Validation instructions](../VALIDATION.md) reproduce the native and live gates.
The permanent `live-acceptance.yml` workflow provides the same live commands with
an explicit EULA-consent input. It may reuse native compilation caches and is not
being relabeled as this uncached run. Normal source-runtime CI never auto-accepts
an operator EULA or launches a server.
