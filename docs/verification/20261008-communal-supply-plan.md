# Prospective communal-supply learning qualification

Immutable accepted source: `4734fa31413d40f742e3412ec5f5b91e377351fc`.
Branch: `study/commons-learning-20261008`.

## Question and execution boundary

Can the existing neural policy/gradient/guarded-Adam implementation learn useful
complementary supply from a shared consumption reward and observable public demand,
without another member's private inventory or a scripted role? Compare an otherwise
identical demand-hidden ablation. This is a fresh, finite, synthetic learning study,
NOT Minecraft cooperation, skill retention or adoption of an existing draft study.

The initial combined shared-environment state read was blocked by execution
screening. It is not retried, split into retries or retrieved through another tool.
This work uses specific immutable source files and an independently owned source
worktree. Do not stop, query, copy checkpoints from, restart or modify the existing
Academy/services for this study. No inference policy or checkpoint is exported.
No deployment or current-runtime identity can be certified from this study.

## Closed synthetic environment

Use the actual core Pocket, Stack, mechanical affordance and conditional action
code. A one-slot synthetic shared chest and initially open menus are disclosed
assistance. Each member initially owns one plank or one stick, in shuffled slot
zero or one; exactly half the members own each material. Initial ownership and
action application order are shuffled independently. No member ID or partner's
pocket is present in policy observations.

All members observe their own pocket and the shared chest. The visible condition
also observes current public material demand. A community requests all units of
one material, then all units of the other, in a balanced random order. At the end
of a joint step, a synthetic public consumer irreversibly removes a complete
requested batch. This is an explicit service objective, not a crafting recipe,
hunger simulation or a scripted citizen. Successful consumption produces one common
reward for every member. Deposits and withdrawals have no reward of their own.
There are no resources injected after reset and no hidden material conversions.

One joint step samples ALL members from the same pre-action state, then applies
one selected primitive operation per member in a fresh shuffled serial order.
A stale transfer must remain conservative through Pocket.click. Allowed choices
are no click or a literal shift-click on own slot 0/1 or shared slot 36, with only
current goal-independent mechanical feasibility masking. Other action heads are
fixed in this intentionally inventory-only fixture. The fixed four-step horizon
continues even if both requests finish early. Each transition is four simulated
ticks, and only the final horizon boundary is terminal. Discounting is the existing
VTrace.discount; trajectories cannot cross episodes.

Synthetic observations have a separate explicit in-memory identity. Reusing the
network's dimensions in tests does NOT make them the Minecraft schema. They must
never be serialized by PolicyFile/TrainingState or packaged as deployable models.
No fresh policy is substituted for the deployment's fail-closed model behavior.

## Fixed experiment, declared before training

- Independent learning seeds: 2026100801, 2026100802, 2026100803.
- Arms: visible public demand, hidden public demand. Same initial weights, initial
  resource schedules and independently keyed action-application orders within a
  seed. Each policy samples from its own stream; no teacher labels/actions.
- Training: two members, 600 updates, 32 episodes/update, 4 joint steps/episode,
  exactly 256 actor transitions per offered batch; 153,600 offered transitions/arm.
- Compute gradients through the existing Gradient and update via UpdateGuard and
  Adam, using unchanged default learning-rate/guard semantics. Record accepted
  and rejected samples, guard backtracks and actual policy identities separately.
- Checkpoints for metrics: before learning, update 100, update 300, update 600.
  No early outcome-based stopping, seed replacement, budget extension or tuning.
- At every checkpoint, evaluate 256 cases at each of two fresh fixed scenario
  seeds (2026100881 and 2026100882), for populations 2 and 8, without learning.
  In each complete case block, balance first requested material, shuffle ownership
  and private slots, and use fresh action streams and serial-order schedules.
- Primary measurement: both requests served within the first two joint steps.
  Report four-step completion, first-step service, per-material consumed/remaining
  stock, per-member donated/withdrawn units, and wrong-material deposits separately.
- Exploratory learning gate: each of the THREE visible trajectories must reach
  at least 90% two-step completion on BOTH evaluation seeds with two members,
  and exceed its hidden matched trajectory by at least 20 percentage points.
  Require at least 80% two-step completion on BOTH seeds with eight members,
  without changing weights. A failure is retained, not relabelled as success.
- These are bounded mechanism gates, not statistical proof of a general algorithm,
  independent-test skill retention or Minecraft/social scaling qualifications.

## Software qualification

Exhaustively enumerate short two-member primitive traces and verify exact
material conservation including consumed units after EVERY step. Reject forged
or repeated evidence, invalid member/action/step boundaries and non-finite data.
Demand visibility must not change mechanical action masks. Swapping another
member's private slot must not change the focal actor's observation. Returned
observations/results must own their arrays, not mutate simulator state. Resource
circulation cannot create consumption credit. Include stale withdrawal races and
member permutation checks. Preserve a complete report with all declared rows;
independent validation rejects missing/duplicate/misbound cases and recomputes
metrics rather than trusting printed summaries.

Run the ordinary source/API tests with the new pure checks and confirm both
public JARs exclude this synthetic environment/learner controller. No Bukkit
worlds or saved Academy learning inputs are part of this qualification.

## Research context, not borrowed evidence

Shared reward alone has credit-assignment/partial-observability difficulties;
VDN studies that problem, but this experiment does not implement VDN:
https://arxiv.org/abs/1706.05296
The existing per-actor V-trace implementation concerns off-policy correction;
it is not by itself a multi-agent joint-policy correction or a proof of cooperation:
https://arxiv.org/abs/1802.01561
