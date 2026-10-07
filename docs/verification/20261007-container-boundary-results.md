# Protected communal storage: qualification and deployment boundary

Date: 2026-10-07 (Japan).
Accepted parent: `5d12ad9beecf4fc96f25abb6f6e830533bf98294`.
Prospective tests/plan: `9254664`.
Implementation: `d5aac3d27162944a14031b7c8439b26d402fe3d8`.
Implementation tree: `cfb0037bee0341228183443af1b6592ddc6b1557`.

## Reproduced defect and implemented boundary

The new disposable Folia test, built with byte-identical accepted-parent runtime
classes, failed with `locked CHEST exposed its inventory`. The failure and server
shutdown logs are retained; this was not a compilation failure or a neural trial.

One common production `ContainerAccess` gate now requires current owner/loading
access, placed state, absence of a native lock and absence of a deferred loot
table before accessing native inventory. Chest access uses the selected block's
local half. Current click resolution and opening use this gate; the simplified
empty-container mining path checks it both before progress and after the native
block-change callback. Locked or loot-bearing stock is not treated as empty.
The current adapter also verifies actor ownership/validity before reading its
location. No protection is removed and no loot is populated to make access pass.

The live status field `container_access_contract` identifies the gate as
`owned-unlocked-no-loot-local-v1`. This does not certify complete permission-plugin
integration, player-key semantics, all block components or crash-atomic saves.
No core model, training source, reward, curriculum, server pin, plugin resource
configuration or workflow definition changed. Host only registers one source test.
No compatibility layer, teacher action or high-level role dispatcher was added.

## Source and independent checks

The initial complete local Java suite printed its final all-tests-completed marker
before a subsequent completion-poll tool call was blocked by execution screening.
That poll was not retried or obtained through another route. Its process exit code
remains unconfirmed. Do not count the surrounding observation as a confirmed
zero-exit suite; exact pushed-source CI is a separate predeclared gate.

A separately executed positive `ContainerAccessTest` exited zero: 8,385 checks,
192 combinations of source state, all menu types and 100 lock-revocation cycles.
Real API proxies enforce owner/loading and protection-before-inventory ordering,
local chest halves, snapshot-only mining, unsupported states and exception
propagation. They do not count as real world trials or measured actor scaling.

Six intentionally incorrect copies of the production helper compiled and each
exited nonzero at its intended assertion: omitted lock, omitted loot, combined
chest inventory, omitted placed-state check, block reads before ownership and
live rather than snapshot inventory for mining. The unchanged positive control
passed in the same harness. Production and test assertions were not relaxed.

The first mutation-driver attempt expected the later protected-inventory assertion
for an omitted lock, but the test failed earlier at the more specific read-order
assertion. The driver rejected this mismatch and did not finish its matrix. Its
files remain under `container-mutations/`; the surrounding shell continued to the
Python tests, so its final zero exit was not a mutation qualification. A new
fail-fast invocation corrected only the expected diagnostic strings and completed
all six negatives plus the positive under `container-mutations-confirmed/`.
This corrected result does not relabel the initial driver invocation as successful.

Independent Python report/lifecycle/retention tests passed 47/47 normally and
47/47 under `-O`. Both public JARs exclude diagnostic/holdout/test classes; the
inference JAR also excludes training. Every packaged runtime class was compared
byte-for-byte with both successful physical fixture plugins.

## Actual disposable Minecraft checks

| Server | Immutable distribution | Observed result |
| --- | --- | --- |
| Folia 1.21.11 build 14 | `f52c408490a0225611e67907a3ca19f7e6da2c6bc899e715d5f46844e7103c39` | Runner exit 0; all 18 fixtures and continuous two-body chain passed. |
| Paper 1.21.11 build 132 | `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba` | Runner exit 0; all 18 fixtures and continuous two-body chain passed. |

The new container regression passed 42 native checks on each server, including
two callback-added protections and zero unexpected loot-generation events.
It covers locked chest and furnace access, lock revocation after a feasible
observation, rejected cursor deposits, invalid-menu closure with carried stock
preserved, deferred chest loot, mining and callback-added locks/loot. Ordinary
unlocked storage still opens and an ordinary empty chest remains mineable.

These focused actuator calls are deliberately scripted and several are made
within one owning callback. They are not neural decisions or elapsed-duration
mining measurements. Separately, the existing continuous two-NPC chain completed
155 checks over 68 scheduled ticks on each server: complementary materials,
chest sharing, wooden-pickaxe crafting, tool transfer, stone mining and shared
cobblestone deposit. Its actions and aim are scripted. No learning occurred in
these diagnostic servers. The existing item suite also checked 1,504 native
material defaults and 100 serialized transfer cycles on each server.

The failed parent trial, successful candidate trials, test-only worlds and logs
remain in the new worktree's `.build/qualification/`. Existing Academies and
unmerged research evidence were not used as these diagnostic inputs.

## Artifacts and integration gate

- Inference JAR SHA256: `1e9c88cd12587f9a6944acc90169e819bd17db493e1be389178e3db4ee9cef36`.
- Training JAR SHA256: `aae1fb27130b8c520dfc89f0de1cff7eab3c18febd95c043bfdef989e700f826`.

The accompanying JSON preserves hashes for these independent qualification
artifacts and source inputs. This report commit alone does not establish remote
CI success, integration or activation. Those identities and live checks must be
recorded after observing the actual operations. Only integrated, validated
mainline is eligible for the shared development service.

The target is [communal living](../COMMUNAL_LIVING.md): public needs rather than
scripted occupations, actual useful supply rather than circular transfers, and
partner/resource replacement tests. This runtime correction protects a necessary
mechanical boundary. It is not adoption of draft PRs #66-#69, a trained communal
policy, a retention benefit or autonomous survival. Current bodies/pockets are
still ephemeral NPC state, not durable vanilla-player lives.
