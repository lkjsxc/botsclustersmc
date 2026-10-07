# Closed communal-supply learning qualification

An opt-in **synthetic**, inventory-only experiment with the production `Pocket`,
conditional action sampler, `Gradient`, `VTrace`, `UpdateGuard` and `Adam` math.
It is not a Minecraft room, saved-policy replay or autonomous settlement. Its
512-float observations are explicitly incompatible synthetic inputs held only in
memory. The runner has no checkpoint import, model export, training-server start,
world API or deployment action. Both shipped JARs exclude this directory.

From the repository root, use a Java 21 JDK:

```sh
./test.sh
mkdir -p .build/qualification
java -Xmx1G -cp '.build/tests:dist/training.jar' \
  org.botsclustersmc.commonslearning.SupplyStudy \
  --new-output .build/qualification/communal-supply
python3 tests/supply_evidence.py .build/qualification/communal-supply \
  --output .build/qualification/communal-supply-results.json
```

Use `;` instead of `:` in the classpath on Windows. Python is the independent
development-evidence checker, not a dependency of either deployed runtime.
The output directory must not exist; never overwrite partial/failed evidence.
The fixed 600-update, three-seed, visible/hidden-demand comparison and learning
gates are in `docs/verification/20261008-communal-supply-plan.md`. A successful
runner exit means complete execution; only independent replay computes whether
learning gates passed. The checker rejects incomplete case or update matrices.

Each agent initially has one private material. A synthetic public consumer needs
all planks and all sticks in an unpredictable, observable order. The shared chest
has one material slot, so depositing the wrong material obstructs useful supply.
Actors choose ordinary no-click or shift-click inputs; mechanical masks do not
encode what is needed. All choices are sampled before any action is applied.
Random serial ordering exposes stale plans. No mid-episode stock is injected,
and transfers earn no reward. Complete requested consumption alone gives the same
reward to every member. Exact material balances include irreversible consumption.

The explicitly supplied open menus, tiny inventory action subspace, synchronous
joint steps, common batch policy, fresh episodes, synthetic consumer and balanced
initial supplies are assistance and limitations. No locomotion, crafting, mining,
hunger, personal utility, persistent inventory or physical Folia scheduling is
learned here. This experiment can validate an observable-demand mechanism and its
learning path, not certify community survival or replace real-world qualification.
