# Click-conditioned slot projection: implementation checkpoint

Date: 2026-09-29.

Implementation source: `66fe3ce388a41caca6a0788adcbe535f39728121`.
Implementation tree: `5ea5cea75eff6942922d5ce1ce76d379f0d8cf48`.

**Status: offline implementation and initial-function verification only. No new
Minecraft learning or frozen gameplay comparison was run. Not a deployment
candidate and not evidence of improved pickaxe completion.**

## Decision and scope

The previous protected placement study preserved other task IDs but lost
within-task placement competence without improving ordinary pickaxe completion.
This motivates testing the action representation, not merely repeating the same
reset exposure for more samples.

The current actor uses one shared 64-slot projection for three click types.
Conditional legality can remove different slots, but the shared projection
cannot express opposite preferences between two slots that are legal under both
click types in the same observation. The prototype unties those three output
projections. This is a representational restriction and an engineering hypothesis,
not proof that it caused the observed gameplay bottleneck.

Entry main: `294f8ff67dd142d64cbf39e3fe08449b91565c33`.
The matched experimental reference is the protected-continuation source
`e1f8e6567116c02e0af18e64a466f7e0289e5a14`. Both reference and candidate use the
same fixed task-11 learning scope and an immutable source anchor for other task
IDs. This experiment does not compare protection against no protection.
Relative to that reference, runtime source changes are exactly:

- `core/.../Schema.java`: three neural slot blocks; explicit new schema.
- `core/.../Distribution.java`: separate conditional logits and gradients.
- `training/.../Exploration.java`: the same prior objective on separate outputs.

Observations, primitive action meanings, legality, previous-action encoding,
reward, curriculum, reset distribution, environment and horizons are unchanged.
No recipe oracle, teacher action, pathfinder, goal-answer mask or replay data
is introduced. The fixed-frontier protection is research machinery, not an
automatically growing production expert bank.

## Function-preserving warm expansion

The 41 parent logits are retained. The old 64-slot projection and its biases are
copied to each of three click-specific projections. The value row is moved,
not treated as a slot. The same coefficient mapping is applied to both Adam
moment arrays, preserving the optimizer clock, policy counters and course/RNG
bytes. There is no random reinitialization or substitution of a favorable
checkpoint.

The network remains 512 -> 96 -> 96. Outputs change from 106 to 234 (including
the value output); parameters change from 68,842 to 81,258, an increase of
12,416 floats (about 18.0%). The existing transient conditional probability and
mask arrays remain 233 elements. Primitive `HEADS` remains
`[9,5,5,3,4,9,6,64]`.

The 48 active snapshots plus their one shared immutable anchor occupy
15,926,568 weight bytes (about 15.2 MiB), below the tested 16 MiB weight-payload
bound. This is not the total process or optimizer memory footprint.

Old and expanded runtime schemas reject one another's checkpoints.
`SlotExpansion.java` is an explicit offline trusted-runtime tool and is absent
from both runtime JARs. Loading an operator-selected historical JAR is not a
general untrusted-model import feature.

## Completed verification

The full local `./test.sh` completed successfully. Evidence is retained at
`/home/coder/workspace/botsclustersmc-click-conditioned-slots/.build/source-tests-1.log`
(SHA-256 `9b4881d9d62e2e0de4fb16a8e0c986a252da46bd81b9ad0b0a0b5f13506074db`).

Relevant checks include:

- Independent finite joint enumeration and finite differences for likelihood,
  entropy, exploration and KL across all three child projections.
- Batched inference and transitions, immutable prior-task routing, active-only
  gradients, optimizer continuity, persistence and artifact separation.
- An independent coefficient-position oracle, moved critic checks, malformed
  dimensions/nonfinite coefficients, and opposite click preferences on identical
  legal supports. A selected click's advantage does not update another click's
  slot output directly.
- 18 tasks times 16 finite input fixtures, with task masks and overlapping
  conditional supports, compared against the actual historical runtime in an
  isolated classloader. Logits, hidden activations, probabilities, value output,
  nine same-seed action choices per fixture and mixed-task batch/scalar outputs
  matched. Save/decode and both optimizer moments were also checked.

The new coefficient/expressiveness test reports 81,850 checks; the conditional
distribution test reports 150,554 checks; the cross-runtime initialization audit
reports 287,128 checks. These are numerical/data-boundary assertions, mostly
coefficient comparisons, not independent learned gameplay trials.

The original protected input was rechecked against its original unfocused
runtime and checkpoint as well: 288 function fixtures and exact Adam/course
initialization passed. Source counters remain 332,305 updates and 92,574,669
trained samples; the expansion itself adds zero training samples.

### Retained identities

| Object | SHA-256 |
| --- | --- |
| Historical protected runtime | `49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9` |
| Original unfocused training checkpoint | `495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855` |
| Historical protected warm checkpoint | `dcd2e73084869ea275642739db13c7e00501677133c5c50697b842eac5ea90b4` |
| Expanded training checkpoint | `36e668ddc1fae904a95a19c5c7e9fcedd8de190f65d827c2ef239f4f6596024f` |
| Expanded inference policy | `03d06a4a5d0b81abc687158ceb1e9325b151621b6712ae85e694452a3a9fe96e` |
| Candidate training runtime | `fd35fa6408d5b56bc7ffd4b0822281732bae8ba1a367067232ca4378667f6741` |
| Candidate inference runtime | `f43f4986c87d34a87a05d417dfc53c5e9e0ce89a5b82165d04e296e980df8637` |

Candidate inputs and the expansion manifest remain under
`/home/coder/workspace/botsclustersmc-click-conditioned-slots/.build/slot-input`.
Historical inputs, worktrees and failed-study evidence were not removed.

## Proposed finite gameplay protocol -- NOT executed

The staged runner draft specifies the following prospective comparison; no
outcomes from it exist.

| Dimension | Declared draft |
| --- | --- |
| Input | The exact protected warm source above; expanded by copying weights and moments |
| Training | 512 actors, task 11 only, seed 7; identical original resets in both arms |
| Sample boundaries | +250,000 accepted samples, then +1,500,000 cumulative; maximum +50,000 overshoot |
| Evaluation seeds | 2026092991 and 2026092992 |
| Ordinary evaluation | Ordered tasks 0-11, 64 cases per task per seed |
| Separate diagnostics | Each of five missing pickaxe cells, 32 cases per cell per seed |
| Evaluation points | Both arms before learning, early, and final |
| Retention gate | Both arms' old tasks 0-10 at least source minus 4/64 on each seed |
| Within-task gate | Candidate at least source minus 4/32 for every missing-cell condition on each seed |
| Ordinary acquisition gate | Candidate at least 8/64 and at least control plus 4/64 on each seed |
| Descriptive assisted gain | Every candidate cell at least 4/32 and combined cell successes at least control plus 32/160 |
| Stop | Baseline or early retention failure prevents further learning; keep complete current-phase evidence |
| Resource bounds | One learner or at most two evaluations; 2 GiB available-memory floor; 30-minute training-segment operational cap |

Control loss of partial skills is measured but does not itself fail the
candidate within-task gate; otherwise the matched control could prevent testing
the proposed improvement. Both arms must still preserve other task IDs.
Ordinary and assisted results must never be pooled into a skill certificate.
The margins are fixed engineering screens, not confidence intervals or proof
of universal retention. Two evaluation seeds are not two independent training
replicates. Initial function preservation does not imply identical subsequent
optimization: each new projection inherits the old aggregated Adam moments.

## Why the runtime remains unadopted

The study runner and its support module passed syntax compilation only.
Saving the additional `tests/studies/test_click_slots.py` regression file was
blocked by the tool's safety verification. That write was not retried through a
different tool or shell. The missing file was not treated as completed evidence.

The runner's command-line entry point is disabled with an explicit
not-qualified error. No new Academy, learner, evaluation server, sample stream
or gameplay result was started by this study. The numerical prototype is
preserved for review, but the runner must receive boundary regression tests and
qualification before its finite protocol is activated.

The integration boundary is documentation only: this implementation record and
the next-step criteria. Experimental runtime code is not accepted on numerical
equivalence alone. The live accepted runtime and its training state were neither replaced
nor restarted. At the recorded operational check (epoch 1790688105905), it still
had 512 active/progressing actors, 229,911,208 trained samples, approximately
2,059 learned samples/s, zero inference failures/rejections and zero rejected
or stale learner samples. These are availability counters, not a frozen skill
measurement.

The next decision is whether the qualified matched comparison demonstrates
ordinary acquisition without within-task forgetting. Until then, neither more
reset exposure nor deployment of this expanded actor is justified by this
checkpoint.
