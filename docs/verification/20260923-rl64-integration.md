# RL64 integration — execution and reachability evidence

## What this record establishes

The actual Minecraft runtime now uses the canonical `learning/src/next` mechanisms
through native PPO, the coordinator, atomic checkpoint bundle and Folia adapter.
There are 64 supported actors and 18 implemented task environments. The observer
UI and its view-distance settings run inside the actual Folia plugin.

Implementation, scripted task reachability and learned skill are different
claims. No pretrained policy, mastered 18-stage curriculum, learning-speed
improvement, indefinite uptime or cooperative settlement is claimed here.

## Complete passing integration run

[GitHub Actions run 35816758014](https://github.com/lkjsxc/botsclustersmc/actions/runs/35816758014)
completed successfully on 2026-09-23 at 04:21:36 UTC. The workflow began at commit
`df451dc9e2e2820c31956f5a914861f7c5ca07a5` and generated the reviewed plaintext
patch before testing. Its **actual tested source** was
`7f3f80b6baa81b992335b57e5701760a4d5eee8c`, not merely the triggering revision.

This run reused pinned Rust/Cargo compilation caches. It compiled the current
application and Java bridge, fetched the Folia runtime, started real Minecraft,
and exercised both diagnostics and normal learning. It must not be presented as
an uncached native build. The subsequent clean-source acceptance is recorded
separately when completed.

The whitelisted logs and statuses are artifact `10731303964`
(`native-integration-tests`, 50,819 bytes). Artifacts expire; this source record
and the reproducible tests are the persistent summary. No operator world,
credentials, `.env`, neural weights or demonstrations were published.

## Regression suites

| Suite | Result |
| --- | --- |
| Package/filesystem checks | 19 passed |
| Bootstrap prerequisites/pins/failure handling | 8 passed |
| Host recovery checks | 24 passed |
| Native learning core | 97 passed |
| Native launcher/audit, including embedded core | 118 passed |
| Actual Azalea adapter, including embedded core | 106 passed |
| Pure Java configuration/protocol/geometry/fixtures | passed |
| Actual Folia API compilation and plugin loading | passed |

The native suites contain overlapping tests; these counts are **not** independent
unique-test totals. Java geometry checked 64 rooms and 18 tasks over 2,654,208
bounded reset/readback positions. JDK syntax parsing was reported separately
from compiling/loading against the real Folia API. The adapter suite includes
four tests executing the real patched packet dispatcher for cursor, full snapshot,
hotbar click-state and player-inventory index corrections.

## All 18 tasks are reachable through ordinary inputs

`tests/live-client.rs` is an explicitly scripted diagnostic, compiled as a
separate Azalea example. It uses the production literal input adapter and real
server evidence, but initializes no model and contributes no training data.
Eighteen clients each exercised one full-difficulty task; actor 8 additionally
reset from a crafted-plank cursor to a fresh stick-crafting task.

Every fixture used difficulty 1.0 and `full=true`. Training-only recipe-grid and
open-menu assistance was disabled. Task-specific raw supplies, tools and fixed
stations remained part of each disclosed task definition. These are not all
empty-inventory survival tasks.

| Index (zero-based) | Task | Result / authoritative evidence | Elapsed server ticks |
| --- | --- | --- | --- |
| 0 | Forward and stop | settled hold | 71 |
| 1 | Turn and stop | settled hold | 171 |
| 2 | Aim and hold | settled hold | 41 |
| 3 | Navigate and stop | settled hold | 136 |
| 4 | Step over | settled hold beyond obstacle | 91 |
| 5 | Break log | one target removed | 85 |
| 6 | Collect log | break + tagged pickup + one log owned | 103 |
| 7 | Place block | one placement, current target occupied | 72 |
| 8 | Craft planks | four crafted and acquired planks | 27 |
| 9 | Craft sticks | four crafted and acquired sticks | 51 |
| 10 | Craft workbench | one crafted and acquired workbench | 96 |
| 11 | Craft wooden pickaxe | one crafted and acquired wooden pickaxe | 180 |
| 12 | Mine cobblestone | break + tagged pickup + cobblestone owned | 96 |
| 13 | Craft stone pickaxe | one crafted and acquired stone pickaxe | 176 |
| 14 | Smelt iron | one confirmed extraction and iron ingot owned | 288 |
| 15 | Supply chest | four transferred logs still in designated chest | 45 |
| 16 | Build platform | three placements, all three target cells occupied | 184 |
| 17 | Log to workbench | break + tagged pickup + workbench crafted/acquired | 245 |

The additional actor-8 reset check passed the stick task in 48 server ticks.
A stale four-plank cursor did not survive into the newly furnished two-plank task.
The platform occupancy was `7` (all three bits), not merely a historical placement
counter. These measurements concern scripted diagnostic completion, **not neural
policy learning time or success rate**.

## Actual normal-batch learning, save and resume

A separate fresh Academy used the public `start.sh`, `status.sh`, `console.sh`
and `stop.sh` entrypoints. It did not reuse a fixture world or model. Settings:
64 actors, 5 Hz decisions, 64-step fragments, 4,096 minimum batch quota, randomly
initialized shared 64x64 MLP, ordinary adaptive curriculum and PPO. The test
reduced the Java heap to 3 GiB and Folia threads to 2 and bound the socket to
loopback on the Ubuntu 24.04 GitHub runner. This is not a hardware-capacity claim
for the operator's 16-CPU/12-GiB allocation.

| Measurement | First normal run | Resumed normal run |
| --- | --- | --- |
| Run ID | 1790137010395288678-5985 | 1790137149571178234-8504 |
| Policy version | 0 to 2 | 2 to 4 |
| Trained samples | 0 to 18,591 | 18,591 to 37,655 |
| Adam steps | 0 to 873 | 873 to 1,770 |
| Initial policy fingerprint | 5d45a3b661be8981 | c699fe8765867862 |
| Final policy fingerprint | c699fe8765867862 | aba57af48cb9a817 |
| Last whole-batch sampled KL | 0.009098 | 0.008235 |
| Last PPO transaction duration | 4,822 ms | 5,031 ms |
| Unexpectedly dropped rollouts | 0 | 0 |
| Recorded learner error | empty | empty |

Both validators established all 64 actors, at least two completed server-observed
episodes per actor, two new real PPO updates, a valid atomic checkpoint and clean
world/model shutdown. The second run restored the exact saved model fingerprint,
policy version, sample count and Adam step count before further updates. The
bundle's Adam moments and trainer/actor RNG serialization are additionally covered
by native round-trip tests. Do not interpret this as deterministic replay of a
live Minecraft trajectory after restart.

The test also verified that the legacy `academy/` sentinel and the root private
configuration stayed byte-for-byte unchanged. New v2 state resides separately in
`academy-v2/`. The old 32-actor observation/policy schema is not silently migrated.
A zero unexpected-drop counter is not a promise to train unfinished actions at
shutdown: untrained buffered samples and unfinished actions remain explicitly
accounted for in the separate course status.

## Observer execution

A real networked diagnostic observer received 12- and 16-chunk cache-radius
packets and passed menu opening, second-page contents, selection of bot 63,
watch 63, next/previous wraparound, overview and automatic tour checks. The server
used its actual spectator UI and asynchronous teleports.

The pinned headless Azalea client does not implement spectator flight. Only this
observer diagnostic disables its survival-physics plugin; gameplay fixture clients
and all normal RL actors retain ordinary physics. No visual screenshot, desktop
Minecraft frame rate, WAN connectivity or client-specific graphics setting was
validated by this network test. This run tested the observer separately from
normal learning; simultaneous observation is a separate acceptance case.

## Failures retained and corrections

- Runs 35814040202 and 35815749097 passed native builds and normal 64-actor learning
  but failed portions of advanced task/observer diagnostics. Those were failures,
  not accepted complete integration.
- The pinned client ignored SetCursorItem and SetPlayerInventory corrections and
  parts of complete inventory/click-state snapshots. A crafted cursor survived
  a server reset in the diagnostic. `pins/azalea-client.patch` now applies the
  authoritative notifications in the actual packet dispatcher; four direct
  regression tests and the live reset check cover the correction.
- Run 35816541127 passed the corrected packet tests and 64-actor learning/resume,
  but some advanced clients targeted an entity at their own eye origin instead
  of the block. The shared-client picker now excludes another ECS representation
  of the same server entity ID as the local player, while retaining occlusion by
  other identities. The subsequent complete run passed all station interactions.
- The scripted platform probe placed near-to-far and occluded its later floor
  targets with earlier blocks. The diagnostic now places far-to-near; the normal
  neural controller was not given this script or a construction macro.
- The observer-only headless gravity mismatch was fixed in the diagnostic, not by
  changing the normal actors' physics or falsifying server position observations.

The explicit patch applies to the immutable Azalea revision recorded in the
build manifest; normal source builds use `git apply`, not the removed development
patch generator. Upstream MIT attribution is retained in `licenses/Azalea-MIT.txt`.

## Remaining limits

No controlled old-versus-new learning-speed ablation, long-run retention curve,
full neural curriculum graduation, multi-seed Minecraft learning study, persistent
cooperative living, ARM64 execution, WAN human login or rendered-client FPS test
has been completed. Full probes and exams remain real gates, not timed unlocks.
Task and observer reachability must not be marketed as learned competence.
