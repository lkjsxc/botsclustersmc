# Menu focus and recoverable crafting inputs

## Mechanism and evidence boundary

An open menu now owns input focus: look, movement, jumping, hotbar selection,
mining, placement and world dropping cannot run concurrently with its clicks.
Opening takes focus immediately; closing does not replay a held world action.
Generic menu operations and every existing slot remain available. No recipe,
ingredient or destination is selected by this mechanism. The current neural
schema, reward, full-difficulty resets and terminal success predicates are unchanged.
Training and independent evaluation now allow recovery of an actor's own
session-tagged drops in every task. Deployment retains its world-edit opt-in.

## Fixed-policy comparison

Policy 183278 (71,333,887 trained samples) was copied from one atomically
published canonical checkpoint, then tested without learning in real Folia
1.21.11 build 14 rooms. Both runs used seed 924401, 32 cases per task, full
difficulty and stochastic neural actions. All failed trials remain in the reports.

| Task | Previous input mechanics | Menu focus and recoverable drops |
| --- | --- | --- |
| Craft planks | 30 / 32 | 30 / 32 |
| Craft sticks | 16 / 32 | 20 / 32 |
| Craft workbench | 0 / 32 | 0 / 32 |

This is one small mechanics comparison, not evidence of newly learned weights,
statistical significance, generalization or completed crafting mastery. The two
mechanics changes were tested together, not as separate causal ablations.

## Software acceptance

A clean sequential source suite passed on Linux with the pinned real API.
It includes 48 owner-before-read checks and 7,609 menu-focus checks, alongside
existing numerical, course, persistence, concurrency, export and evaluation tests.
All 18 full-difficulty scripted fixtures passed in a real Folia server, with
zero learner and inference samples. Added adversarial checks verified that
opening, using and closing a menu cannot drop stock, move, jump, rotate or
change the selected hotbar. These are mechanical reachability tests, not RL.

The ownership guard checks the current region before querying world height or
chunk state, rejects out-of-height coordinates, and never requests a chunk.
A radial probe that reaches unknown space clears its entire eight-field record.
The ordering is covered by API-interface doubles that reject foreign reads;
real fixtures additionally check loaded local and unavailable distant locations.

Evaluation reports now retain requested GUI/world operations, clicked slots,
visible recipe opportunities, grid/crafting counts and the first 24 GUI inputs.
These diagnostics only observe; they never choose an action. Requested world
operations are not counts of executed drops, and consuming ingredients is not
itself evidence of losing them. Partial grid progress is not a success claim.

Full reports remain on the operator's device in the development worktree under
`.acceptance/craft-baseline` and `.acceptance/craft-focused`; the clean source
log is `/tmp/bcmc-focused-suite.log`. Independent candidate learning was started
from the same canonical checkpoint in `.acceptance/craft-learning`, without
replacing the live Academy. Its later weights require their own evaluation.
No open-world survival, high-level goal selection or cooperation is established.
