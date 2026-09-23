# Validation — citizen runtime

Software correctness, real-server reachability, learned exam outcomes and
survival/cooperation are separate claims. Bodies are in-server Villager NPCs,
not network-connected Minecraft players. No full-survival model is shipped.

## Resource learning and native evaluation (0.7.2)

[The evaluated-progress record](verification/20260924-evaluated-progress.md)
separates software acceptance from measured skill outcomes, including failures
and older-skill regressions. The native command reuses the canonical real-server
holdout evaluator without changing live weights or historical certificates.
`./evaluate.sh` is the operator entry point; [Evaluation](EVALUATION.md) explains
its resource limits, periodic mode and snapshot identity.

The checks below originally established 0.7.0. They do not by themselves certify
all later changes, every server release, or open-world survival.

## Local completed checks

On Linux x86_64, OpenJDK 21 and official Folia 1.21.11 build 14, the integrated
citizen runtime passed numerical, mechanics, curriculum, filesystem, late-reply
concurrency and canonical-export checks. The tests include the new body-relative
observation contract, nonvanishing legal-control prior, conditional policy KL,
backtracking and rejection without mutating the original optimizer/model.

The disposable real-server acceptance passed fresh 64-NPC training, exact
model/Adam resume, canonical export despite an obsolete loose policy, all 18
scripted full-difficulty fixtures, and a separate 64-NPC inference deployment.
Deployment exercised pause/resume, rapid goal replacement, chunk-ticket release,
respawn and corrupt-policy rejection without shutting down the operator server.
The 18 fixtures performed no learning and are not learned skill certificates.

Non-operator observer and browser tests are independently reproducible below.
Cross-platform results for 0.6.1 are historical, not an automatic certification
of this changed observation/body schema. Delivery evidence identifies the exact
source and completed environments.

## Reproduce

Normal build, training, monitoring and export require only Git and a Java JDK.
Python and Node dependencies below are optional developer test tools only.
```sh
./test.sh
# Only after personally accepting the Minecraft EULA:
EULA=true python3 tests/acceptance.py all --count 1024 --seconds 45 --output acceptance
python3 tests/report.py acceptance

# Optional, pinned observer/browser test dependencies:
npm install --prefix .build/browser --no-audit --no-fund playwright@1.63.0
npm install --prefix .build/observer-client --no-audit --no-fund mineflayer@4.39.0
.build/browser/node_modules/.bin/playwright install chromium
EULA=true python3 tests/observe.py --output observer-acceptance
```

Output directories must not already exist. The observer test starts its own
128-NPC loopback Academy on port 25581 and a read-only loopback monitor on 8766.
Its fictional, non-operator client checks spectator entry, actual goal particles,
cross-island following, overview, unwatch and denial of administration. It never
uses a human Minecraft account or generates gameplay policy training examples.
Browser checks cover live metrics, 18 stage rows, charts, desktop/mobile bounds,
stale-state warnings, denied writes and inaccessible model paths.

## What the learning evidence means

The operator's 1,024-actor run passed the first frozen-policy motor exam for every
actor without weakening eligibility or passing thresholds. This is stronger than
movement or a scripted fixture, but each actor examines its own frozen policy
version. It does not establish that one latest exported model passed every actor's
exam, or that future updates preserve every earlier skill. Progress and regression
are continuously visible; no stage is promoted merely because time has elapsed.
## Historical evidence and limits

[Runtime hardening acceptance](verification/20260923-runtime-hardening.md) records
0.6.1's canonical export, path protections, reply isolation and seven successful
compatibility jobs. Those mechanisms are retained; its zombie-body workaround is
replaced, not silently left as a second runtime. Earlier Rust/Azalea records refer
to the former connected-player experiment and cannot certify this NPC system.

A completed lesson catalogue or an exported model does not provide hunger,
complete tool durability/combat, every recipe, persistent NPC inventories,
self-selected long-horizon goals or a cooperative settlement. Those remain
unimplemented or unverified. Dense shared-world performance, arbitrary plugins,
macOS/ARM64, hot reload and indefinite uptime are not certified by short flat-world
trials. World edits default off; test inference on copied worlds first.
