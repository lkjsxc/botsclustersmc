# Prospective tensor-binding comparison after the failed supply baseline

Prior implementation: `4fc06837e29c2d1e3c02c10dcd5314ba2d297dbe`.
Complete rejected result recorded at `7873455dc40c8ccbc1314d750d3194c57fcddba5`.
This is a NEW hypothesis with fresh learning/evaluation seeds, not an extension,
threshold change or relabelling of the completed visible/hidden experiment.

## Hypothesis and one change

The completed baseline gave visible demand at most a 1.5625-point advantage.
It did not identify a causal failure. Test whether an explicit generic tensor
product between local inventory features and public demand makes their relation
learnable under the same shared consumption reward and fixed sample budget.

Candidate appends ALL 108 local inventory feature x 2 public-demand feature
products (216 floats) in currently unused SYNTHETIC input columns 220..435.
Include every material pair, equal and unequal, and count components alike.
Do not implement an equality/matching predicate, choose a material/slot, change
an action mask, add preferred actions, introduce labels or reward contributions.
The control leaves these columns zero. Both arms observe identical public demand,
own pockets, common stock and timing. No new partner-private information is added.

The new features have a separately named in-memory synthetic identity. They are
not a Minecraft observation schema, a compatibility wrapper or a deployable model.
Changing the representation is the experimental treatment; old policies are not
silently interpreted under it. The network size, initialization, common rewards,
room mechanics, sampler, V-trace, Gradient, UpdateGuard and Adam stay unchanged.

## Fixed declaration before any new training

- Learning seeds: 2026100811, 2026100812, 2026100813, each with fresh paired initial
  weights. Arms: plain visible-demand control and full tensor-product candidate.
- Exactly 600 update attempts x 256 offered actor transitions per arm, two-member
  training, 32 episodes/update, four joint steps/episode. No outcome-based stopping
  or extension. Preserve accepted/rejected samples and optimizer identities.
- Evaluate at attempts 0,100,300,600 using seeds 2026100891 and 2026100892, each
  with 256 cases for populations 2 and 8, without learning or changing weights.
- Same primary definition as the first study: both requested batches consumed in
  the first two joint steps. Candidate must reach >=90% on each two-member set,
  exceed its paired plain control by >=20 percentage points on each, and reach
  >=80% on each eight-member set. ALL three independently learned pairs must pass.
- The full 24,576 evaluation cases and six training histories must be independently
  replayed. Earlier common-reward failure stays failed regardless of this outcome.
- A pass is confined to this closed inventory-only synthetic mechanism, not
  learned Minecraft cooperation, skill retention, physical scaling or survival.

## Software and execution boundaries

Verify every output product including unequal material pairs, untouched non-product
columns, zero products when demand is hidden, finite values, rejected contaminated
reserved columns, input ownership and no mutation of the primitive action mask.
The full product is a deterministic function of information already visible to the
actor, not an information oracle. Comparing policies at initialization is still
required because the input change can alter behavior before learning.

Keep the first study runner available as its same current experiment, not as a
legacy runtime. Share its mechanics and rollout code without changing the default
plain-input behavior. The independent validator must require an explicit exact
study profile; never accept old evidence by switching global constants or silently
renaming arms. Separate output directories, identity headers and hash receipts
must bind each result to its actual source/profile.

The existing Academy/service and optional conditional-donation diagnostic blocks
remain in force and are not retried. Only new source tests and this new declared
synthetic run are authorized here. No Minecraft server, checkpoint preparation,
main integration, service restart or deployment is part of this qualification.
