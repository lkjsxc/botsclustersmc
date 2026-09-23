# Delivery — 0.5.0 integrated source runtime

Use the repository's normal `main` checkout and `./start.sh`; no component ZIP,
prebuilt artifact, teacher model or manual source transplant is required.
The real actor/learner imports the shared RL-next mechanisms and 18 task adapters.
Default population is64; observer view defaults to12 chunks while actor view and
simulation remain3. See README.ja.md for setup and observation commands.

Old `academy/` data is preserved. New state belongs to `academy-v2/`, whose atomic
`state/training.bcmc` includes policy, optimizer, RNG and adaptive curriculum.
v1 models are incompatible and are not auto-migrated. Existing `.env` files are
preserved, so an explicit BOTS=64 change is needed when they still specify32.

Validation results are in docs/VALIDATION.md and its dated records. Integration,
scripted reachability, learned mastery, retention, generalization and capacity
must never be presented as interchangeable claims. The distribution contains
source and documentation, not operator worlds, credentials or trained weights.
