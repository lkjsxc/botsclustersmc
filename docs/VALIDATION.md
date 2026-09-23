# Validation — current Java NPC runtime

The [mainline acceptance record](verification/20260923-mainline-acceptance.md)
records completed run [35847346541](https://github.com/lkjsxc/botsclustersmc/actions/runs/35847346541)
against runtime source `4cfc06d9c43c2e94204dc948e3e4dd54d2232002`.
All seven jobs passed. The Paper chunk-handoff failures from the preceding run
were repaired and the same real deployment/fixture checks were rerun successfully.

## Confirmed combinations

| Server / operating system | Java | Completed real-server checks |
| --- | --- | --- |
| Folia 1.21.11 build 14 / Linux | 21 | 1,024-NPC learning, exact resume, export, 18 fixtures, independent 64-NPC inference |
| Paper 1.21.1 build 133 / Linux | 21 | Exported inference JAR plus policy; lifecycle and all 18 fixtures |
| Paper 1.21.11 build 132 / Linux | 21 | Exported inference JAR plus policy; lifecycle and all 18 fixtures |
| Paper 26.2 build 128 / Linux | 25 | Exported inference JAR plus policy; lifecycle and all 18 fixtures |
| Folia 1.21.11 build 14 / Windows | 21 | 32-NPC learning, exact resume, export, 18 fixtures, independent inference |

Fresh source builds, real API compilation and numerical tests also passed on
Linux and Windows. This table names tested combinations, not every release
between them. No ARM64, macOS, arbitrary fork, desktop rendering or WAN connection
claim is made. Inference can be installed without a training server or an external
inference process; startup defaults to zero NPCs and disabled world edits.

The separate [local capacity record](verification/20260923-java-runtime.md)
documents 2,048 actual ticking/acting NPCs over approximately 240 seconds. It
retains its exact source binding, settings, scope and earlier failed attempts.
It does not replace the final independent compatibility run.

## Evidence categories

- Pure Java tests: model/distribution numerical gradients, batch/scalar parity,
  bounded queues, atomic files and invalid-state rejection, primitive mechanics,
  raw-material conservation, independent course/exam rules and optimizer resume.
- Five-seed synthetic bandit learning: actual optimization, not Minecraft evidence.
- Real training: all-agent ticks/actions plus real weight updates and persistence.
- Scripted real-server 18-task reachability: separate diagnostic JAR, zero training.
- Inference deployment: only plugin+policy, actual movement, commands, ticket cleanup,
  and invalid-model rejection without shutting down an operator server.
- Server/OS compatibility: only completed live jobs certify named combinations.

CI's `validate.yml` builds/tests on Linux and Windows. Opt-in live jobs run fresh
Folia training and deployment, then test named Paper versions against the same
inference artifact. Source-only main/PR runs do not silently accept an EULA or
start Minecraft. Ordinary users must explicitly accept the EULA before startup.

## Reproduce

Normal build and startup need only a JDK and Git; opt-in acceptance additionally
uses Python 3. After reading and personally accepting the Minecraft EULA:

```sh
./test.sh
EULA=true python3 tests/acceptance.py all --count 1024 --seconds 45 --output /tmp/bcmc-acceptance
python3 tests/report.py /tmp/bcmc-acceptance
```

The output directory must not already exist. The test never uses an operator
Academy. It starts real servers, checks all agents, stops/resumes and exports to
its disposable `deploy/`. It does not set EULA consent for ordinary startup.
The download cache defaults to `.cache/server`; `./test.sh` creates it.

For a named Paper compatibility test, use the Java version required by that server:

```sh
python3 tests/download_server.py 1.21.1 /tmp/paper-test-cache
EULA=true python3 tests/acceptance.py inference --cache /tmp/paper-test-cache \
  --deploy /tmp/bcmc-acceptance/deploy --output /tmp/paper-inference
EULA=true python3 tests/acceptance.py fixtures --cache /tmp/paper-test-cache \
  --output /tmp/paper-fixtures
```

The downloader records exact stable build metadata and verifies the official
checksum. It never falls back to a different Minecraft version. Model export
contains only a short-trained test policy, not a certified all-skills policy.

## Limits

Completed throughput measurements concern early navigation on disposable flat
worlds. They do not prove indefinite operation, faster sample-efficient learning
than old code, dense settlements, every later skill at that throughput, thousands
of observers, or full survival. Historical records from the Rust/player runtime
describe different software and cannot substitute for current evidence. No
18-task neural mastery or generalization result is shipped. Bodies and pockets
are ephemeral; the model and complete training state are separately persisted.
