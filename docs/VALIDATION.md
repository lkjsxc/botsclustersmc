# Validation — current Java NPC runtime

The [local acceptance record](verification/20260923-java-runtime.md) documents
completed real Folia tests, including2,048 actual ticking/acting NPCs, asynchronous
learning, exact training-state resume,18 scripted task fixtures and independent
plugin+policy deployment. It records scope and failed intermediate attempts.

## Evidence categories

- Pure Java tests: model/distribution numerical gradients, batch/scalar parity,
  bounded queues, atomic files and invalid-state rejection, primitive mechanics,
  raw-material conservation, independent course/exam rules and optimizer resume.
- Five-seed synthetic bandit learning: actual optimization, not Minecraft evidence.
- Real training: all-agent ticks/actions plus real weight updates and persistence.
- Scripted real-server18-task reachability: separate diagnostic JAR, zero training.
- Inference deployment: only plugin+policy, actual movement, commands, ticket cleanup,
  and invalid-model rejection without shutting down an operator server.
- Server/OS compatibility: only completed live CI jobs certify named combinations.

CI's `validate.yml` builds/tests on Linux and Windows. Opt-in live jobs run fresh
Folia training and deployment, then test named Paper versions against the same
inference artifact. These configured jobs are not evidence until their run passes.
The dated record is updated with exact successful runs/builds after execution.
No ARM64, macOS, arbitrary fork, desktop rendering or WAN connection claim is made.

## Reproduce

Normal build and startup need only a JDK and Git; opt-in acceptance additionally
uses Python3. After reading and personally accepting the Minecraft EULA:

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

The current completed scaling measurement is240seconds at an early navigation
stage on a disposable flat world. It does not prove indefinite operation, faster
sample-efficient learning than old code, dense settlements, every later skill at
that throughput, thousands of observers, or full survival. Historical records
from the Rust/player runtime describe different software and cannot substitute
for current evidence. No18-task neural mastery or generalization result is shipped.
