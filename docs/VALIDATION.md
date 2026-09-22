# Validation — source-first 0.4.0

The root contains the actual Minecraft application, not only RL-next components.
The native sources are rebuilt with Rust nightly-2026-02-04 and Azalea revision
f8ddefa70cc53e6385785fb56e7a688a389cf0ab. The reviewed server pin is Folia
1.21.11 build 14 / protocol 774. No prebuilt binary or runtime cache is committed.

Local source checks completed during restoration: 19 shell/filesystem tests,
8 clean-bootstrap negative/pin tests, 24 host recovery tests, pure Java protocol,
configuration and campus planner tests, and the separate 63-test RL-next suite.
These checks do not prove a Minecraft skill or all advanced tasks are playable.

Native build and live results are recorded separately in
`docs/verification/20260922-source-runtime.md` when completed. Inspect the current
record and the associated GitHub Actions run before treating a build as verified.
Historical REPAIR-REPORT.md and Japanese repair records describe the supplied
0.3.1 bundle, not a rebuild of this source release.

## Reproduce a live acceptance run

Only after independently accepting the Minecraft EULA:

```sh
EULA=true SMOKE_BOTS=32 SMOKE_HEAP_GB=6 ./smoke.sh
```

This creates a separate `experiments/smoke-*` world and model; it does not use or
overwrite `academy/`. It runs real Folia and the Rust policy, validates current-run
campus/protocol/population receipts and real PPO updates, stops cleanly, then
restarts to check the exact previous policy/optimizer/sample counters and new
updates. Six-stage mastery, human-like behavior and collective living are not
implied by a successful short lifecycle test. The 18 experimental task contracts
remain separate from the six playable foundations.

To audit the actual stopped Academy:

```sh
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy
# Optional exact resume comparison against a saved prior run status:
BCMC_ROOT="$PWD/academy" ./bin/botsclustersmc-run verify-academy /path/to/previous-status.json
```

CI compiles from a fresh checkout without project runtime/native caches and
checks the bridge against downloaded Folia API libraries using Paperclip patchonly.
It does not start a Minecraft server or accept a Minecraft EULA on the operator's
behalf. Local offline tests may reuse byte-verified dependency caches; that is
explicitly distinct from independent network bootstrap evidence in CI.
