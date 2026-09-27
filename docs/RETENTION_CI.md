# Pinned retention experiment on isolated CI

`tests/retention_ci.py` continues the declared September 28 checkpoint experiment
on a disposable GitHub-hosted runner. It is developer evidence tooling, not a new
learner, a deployment command or a normal startup dependency. Python remains
unnecessary for ordinary Academy and inference operation.

The reservation candidate **failed its original confirmation threshold**. The
[complete study record](verification/20260928-reserved-review-study.md) distinguishes
that failed scientific gate from a successfully completed CI execution. The
allocator itself is not merged into the production implementation. Keeping this
runner does not authorize reclassifying the failed experiment or changing its seeds.

## Explicit invocation and boundaries

The existing `validate.yml` has two affirmative dispatch inputs. Ordinary pushes
and pull requests run source/browser checks only. After accepting the Minecraft
EULA and obtaining access to the retained draft experiment input, an operator can
explicitly reproduce the declared continuation:

```sh
gh workflow run validate.yml --ref main \
  -f eula_consent=true -f retention_study=true
```

This selects the retention job instead of the broad deployment-acceptance jobs.
The two arms have separate runners, fixed source commit IDs, 512 actors, seed 7,
a 2-GiB heap and region/inference/learner threads 2/1/1. Existing complete model,
Adam and genuine course state are restored without copying an operator world.
The runner has a 55-minute job limit; each training phase has a 2400-second limit
and keeps the 1-GiB available-memory floor. These are rejection bounds, not a
capacity guarantee. A failed check does not create random replacement weights.

The source pins, input archive identity, total accepted-sample budget, maximum
overshoot, task order, case counts and two evaluation seeds are fixed in the
controller. This is a pinned experiment, not an arbitrary checkpoint-upload API.
A new study needs its own declared protocol and reviewed input identity. It must
not silently overwrite this study or start its sample budget again after a restart.
The input is the unpublished `retention-input-20260928` draft asset; the workflow
must not publish it or label it a product release.

Only three input entries are accepted: `manifest.json`, `control.bcmc` and
`candidate.bcmc`. The compressed archive, decoded entries and manifest are bounded.
Unknown, duplicate and missing names, changed identities, altered protocol fields,
symlinked local paths and invalid counters are rejected. Archive paths are never
extracted, and archive-supplied code is never executed. The ordinary native
checkpoint reader validates model/Adam identity before a learner starts.
No `.env`, world, console credentials, server JAR or other project data is included.

Draft-release access requires push access, so this opt-in job requests
`contents: write`. Only the fixed asset-download step receives that token; no
step pushes commits or deploys to production. `GH_TOKEN` is scoped to that step,
checkout credentials are not persisted, and the controller removes GH_TOKEN and
GITHUB_TOKEN from the Minecraft environment. Normal source/browser jobs retain
read-only permissions. Do not weaken this boundary to accept a user-supplied URL,
script, archive path or arbitrary executable.

## Results are not a mastery certificate

Each learner stops and saves normally before two fixed-policy evaluations.
The second evaluation uses `--from` the first bundle, so it tests the same model
on the separately declared confirmation seed. The controller verifies the policy
identity, complete task coverage, no evaluation training and unchanged checkpoint.
It does not hide 0/N results or adopt a policy automatically.

Actions artifacts `retention-control` and `retention-candidate` contain only the
separate evidence directory: exact input/final checkpoints, model exports, complete
evaluated bundles, bounded status history and logs. No Academy/world/control files
are uploaded. Artifact access follows the repository's normal Actions permissions;
this is not encrypted private storage. Artifacts have 14-day retention, so preserve
needed evidence before expiry. A checkpoint artifact is not an Academy-world backup.

A green CI result means the experiment ran successfully. It does **not** mean the
retention thresholds passed. Independently validate all trial bundles, exact model
reuse and raw workload counters, then compare every declared skill threshold on
each seed separately. The completed original run `36343677879` was green while the
candidate scored 21/32 wooden pickaxe on its confirmation seed, below the required
24/32. That failure remains recorded; subsequent favorable runs cannot erase it.

The transfer crossed hosts and resumed after two resource interruptions. Those
changes and unobserved shutdown-drain counters are explicitly documented. Do not
present this as an uninterrupted equal-update experiment, a hardware benchmark,
general survival or cooperative behavior.

## Local boundary tests

```sh
python3 -m unittest discover -s tests -p test_retention_ci.py
```

These six synthetic test methods exercise archive/protocol rejection, exact file
sets, no-overwrite and symlink handling, and conservation of actual workload.
They neither start Minecraft nor certify learned behavior. The source/browser CI
runs them without EULA consent; actual Minecraft execution remains opt-in.
