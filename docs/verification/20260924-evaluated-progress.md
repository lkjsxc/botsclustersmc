# Independently measured learning progress

This record covers resource-learning changes and the native evaluation command.
Resource source: `e9478e7`. The evaluation command began at `6731992`.
The experiments used Java 21 and Folia 1.21.11 build 14 on a Linux allocation
with 16 effective CPUs and 12 GiB memory. Training had 1,024 Villager NPCs.

[The aggregate results](data/evaluated-progress.json) retain six experiments and
identify the exact policy and runtime artifacts. Counts were checked against
all 1,952 trial records. Full trial details are retained on the operator's device.
These are historical measurements, not guarantees for subsequent live policies.

## Retention and resource outcomes

Policy 120887 scored 0/32 on log breaking and 0/32 on collection. A later
sustained-contact policy 132845 scored 32/32 on both, but only 3/32 on aim-hold
and 11/32 on step-over. The failed older-skill cases remain in the evidence.
After bounded task-loss balancing, policy 140831 was tested on two new 64-case
sets. Forward-stop, turn-stop, navigation, step-over, log breaking and collection
scored 64/64 in both sets. Aim-hold scored 62/64 and 63/64. Placement scored 0/64
and 2/64. These runs are not a controlled ablation: policies, seeds and accumulated
training differed. No universal sample-efficiency improvement is claimed.

A subsequent native evaluation froze live policy 144297. It did not train or
modify the live curriculum. Its full-difficulty results were:

| Task | Successes / trials |
| --- | --- |
| Forward-stop | 32 / 32 |
| Turn-stop | 32 / 32 |
| Aim-hold | 31 / 32 |
| Navigate-stop | 32 / 32 |
| Step-over | 26 / 32 |
| Break-log | 32 / 32 |
| Collect-log | 32 / 32 |
| Place-block | 25 / 32 |
| Craft-planks | 0 / 32 |

The native command also retained the earlier policy 151842 measurement, taken
before adopting the resource checkpoint: the first five tasks scored 32, 32, 31,
32 and 32 out of 32; breaking a log scored 0/32. Policy counters are not globally
unique run identities or a ranking across different checkpoint lineages.

The later step-over result of 26/32 and the zero crafting result are significant
limits. Historical course certificates cannot replace a current-policy test.
The real Minecraft success predicates and exam thresholds were not relaxed.

## Software acceptance

The normal source test suite passed with the native command included. It covers
core numerics, mechanics, coordinates, aiming and harvesting reset invariants,
bounded task balancing, guarded updates, course state, persistence, late replies,
canonical export and evaluation-result integrity. Synthetic bandit improvement
is a separate numerical check, not evidence of Minecraft competence.

The native command was exercised against copied canonical checkpoints and against
an immutable snapshot of the active Academy. Lifecycle acceptance checked the
single-writer lock, unchanged-policy skipping, explicit EULA requirement, stopping
while waiting, stopping an active real test server, complete cleanup of owned
scratch files, unchanged source checkpoints and retention of completed results
when a later checkpoint is invalid. Evaluation does not produce learner samples.

Browser acceptance checked actual result rows, tested/live policy identities,
desktop and mobile layout, a stale evaluator heartbeat, invalid-report handling,
GET-only routes and refusal to serve model or console files. Synthetic browser
responses test UI error handling only; they are not learning measurements.

## Remaining limitations

This is not a completed survival settlement. Independent trials still show
incomplete placement/crafting and changing retention. There is no demonstrated
open-world cooperation or learned high-level choice of survival goals. NPCs are
modified server-side Villagers, not logged-in players. Full hunger, durability,
combat, all recipes and persistent individual lives remain outside the current
actuator. More samples or historical course passes do not erase these limits.

The evaluator's detailed operating contract is in [Evaluation](../EVALUATION.md).
The named commands and the read-only dashboard make these limits observable
while real learning continues; they do not silently turn a failure into a pass.
