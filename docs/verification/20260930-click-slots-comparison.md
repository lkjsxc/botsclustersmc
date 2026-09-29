# Click-conditioned slots: qualified matched comparison

Date: 2026-09-30 (Japan). This record is declared before new gameplay runs.

## Question and unchanged boundary

Continue the exact proposal in [the preserved checkpoint](20260929-click-conditioned-slots.md):
can three separately learnable click-specific slot projections acquire ordinary
wooden-pickaxe crafting while retaining the source's partial placement behavior?
Both arms use the same immutable prior-task policy and warm task-11 copy. This
is not a test of protection versus no protection and not a deployable expert bank.

The original numerical implementation remains `66fe3ce388a41caca6a0788adcbe535f39728121`.
The historical matched control remains `e1f8e6567116c02e0af18e64a466f7e0289e5a14`.
Its protected checkpoint is `dcd2e73084869ea275642739db13c7e00501677133c5c50697b842eac5ea90b4`;
the expanded checkpoint is `36e668ddc1fae904a95a19c5c7e9fcedd8de190f65d827c2ef239f4f6596024f`.
Both start at 332,305 policy updates and 92,574,669 trained samples. No newer or
more favorable source is substituted. Reward, resets, observations, physical
controls, masks, optimizer initialization and the two candidate JARs are unchanged.

## Runner qualification

The preserved runner had no boundary regression file. New offline regressions
first reproduced seven failing subcases: an initial checkpoint export different
from the baseline policy was accepted; a changed exam JAR or incorrect exam JAR
metadata was accepted; and floating-point task, seed, case and port metadata was
accepted as equal to integer identifiers. These were experiment-evidence defects,
not measured gameplay failures.

The runner now binds the baseline policy to its native checkpoint export,
rechecks the compiled exam identity, validates integer metadata without coercion,
and checks process-local status freshness, monotone clocks/counters, task-11
accounting and accepted-sample budgets explicitly. Twenty-four regression methods
cover complete condition matrices, each retention/acquisition margin, stopped
phases, no training after failed qualification, immutable input/source identities,
create-only bounded evidence, strict JSON, isolated command cleanup and memory
headroom. Normal and Python-optimized execution are required. CLI help and missing
`--run` do not start a study. An explicit run executes the offline suite before
constructing the study. CI runs the same offline tests; it does not start Minecraft.

## Fixed prospective protocol

- Both arms: 512 actors, original task-11 resets, training seed 7, one learner at a time.
- Accepted samples: +250,000 early, +1,500,000 cumulative final; maximum +50,000 overshoot.
- Frozen evaluation seeds: 2026092991 and 2026092992; ordered ordinary tasks 0-11,
  64 cases per task; separately, each of five missing pickaxe cells, 32 cases per cell.
- Evaluate both arms at baseline, early and final. Each complete phase is 24 reports
  and 3,712 unique trials. At most two evaluation servers run concurrently.
- Both arms must retain every old task 0-10 at source minus 4/64 on each seed.
  Candidate must retain every missing-cell condition at source minus 4/32 on each seed.
  Control partial-skill loss is reported but is not a candidate veto.
- Ordinary acquisition requires candidate at least 8/64 and at least control plus
  4/64 on each seed. Assisted gain is only descriptive: each candidate cell at least
  4/32 and combined candidate at least control plus 32/160 on each seed.
- A baseline or early retention failure stops further training only after retaining
  the complete current-phase matrix. Operational failures retain partial logs and
  failure details, never a success outcome. Memory floor is 2 GiB of effective
  available headroom; each training segment has a 30-minute operational cap.

These margins are fixed engineering screens, not confidence intervals. Two
sampling/layout seeds are not independent training replicates. Initial function
preservation does not imply identical subsequent optimizer trajectories. Assisted
and ordinary cases never pool into a certificate. Experimental Academies are new,
owned directories under `.build/click-slots-study`, never the live Academy.

## Execution record

At declaration no gameplay outcome was claimed. The following observations are
recorded after execution; they do not modify the prospective rules above.

### Qualified source and checks

Runner implementation: `06b63d54899c984b9a8457c7b935d75e06954a96`.
Actual study source: `97384a3d88097568c47ad1e2f204f4ad2d350922`,
tree `ac882bf000a3ad9c87b46c59f353be5adcc984ce`. The latter merge retains the
accepted main documentation and changes no executable file from the runner commit.
Both commits were authored as `lkjsxc` and pushed normally before gameplay.
A direct revision comparison of `core`, `plugin` and `training` against the matched
control changes only `Distribution.java`, `Schema.java` and the slot-logit indexing
in `Exploration.java`. The plugin, `TrainingEnvironment.java`, `Pocket.java` and
`Policy.java` are unchanged. The conditional head and its probability/gradient
mapping are the runtime mechanism varied, not a new reward or physical controller.
Research PR: #43. The original PR #41 remains closed at its original checkpoint.

The 24 new regression methods passed in normal and optimized Python. The explicit
study entry point reran all 24 successfully before constructing the experiment.
A complete local `./test.sh` ended with `SOURCE_TEST_EXIT 0`; elapsed time was
71.588 seconds. A preceding foreground attempt lost its tool connection and has
only a partial log; it is not the basis for the full-suite claim.
GitHub run `36590564633` passed source checks on Ubuntu and Windows and the
observatory regression, including both new Python invocations. Dispatch-only live,
Paper, retention and Windows-live jobs were skipped, not counted as executed tests.

The candidate runtime bytes remain the original numerical checkpoint's bytes:

| Artifact | SHA-256 |
| --- | --- |
| Candidate training JAR | `fd35fa6408d5b56bc7ffd4b0822281732bae8ba1a367067232ca4378667f6741` |
| Candidate inference JAR | `f43f4986c87d34a87a05d417dfc53c5e9e0ce89a5b82165d04e296e980df8637` |
| Complete local source-test log | `96a9c207cde7daf7014ece7adce0dabfcb8502d704de63b854f3b67ef4352bcb` |
| Normal regression log | `dd7e2bb5d1f7fc9f30668a77d51237f93a1b3c59b90d976b1fe0780429e3e456` |
| Optimized regression log | `502de65fa7904e5ef4b8233e52b1c335e6b448f90a76fc644594ace835fbcb8e` |
| Create-only study declaration | `b9b9d80f578bdb9d6ed5ffd234c58815c1a52e51a16145d117bd544ad0f7669a` |
| Baseline gate | `21f4df32eae26fc3e41ff6b2daf38d1ee97d643d478f16a7503d55896a74fe6f` |

### Completed baseline

All 24 baseline reports and 3,712 frozen trial executions completed. Native export
of each initial training checkpoint equalled the corresponding evaluated policy.
The independent historical-runtime audit of the expanded input passed 287,128
checks. No evaluation contributed training samples.

Seed A is `2026092991`; seed B is `2026092992`. Ordinary scores below are ordered
by tasks 0-11; every entry is out of 64.

```text
control   A: [64,64,62,64,64,64,64,63,64,64,58,0]
candidate A: [64,64,62,63,64,64,64,63,64,64,58,0]
control   B: [64,64,61,64,64,64,64,64,63,64,60,0]
candidate B: [64,64,61,64,64,64,64,64,63,64,60,0]
```

Each assisted condition leaves exactly the named ingredient cell missing. Every
entry below is a full pickaxe completion out of 32, not a placement counter.

| Missing cell | Control A | Candidate A | Control B | Candidate B |
| --- | ---: | ---: | ---: | ---: |
| Top left | 15 | 15 | 19 | 19 |
| Top center | 18 | 18 | 19 | 19 |
| Top right | 18 | 18 | 13 | 12 |
| Handle upper | 0 | 0 | 0 | 0 |
| Handle lower | 0 | 0 | 0 | 0 |

The initial relative-retention screen passed. Numerical function preservation did
not produce identical real-server outcomes in every case: navigation on seed A
and top-right completion on seed B differed by one. These runs are stochastic,
asynchronously executed gameplay, not deterministic action-replay certificates.
The baseline establishes partial plank-placement completion but not stick-placement
completion or ordinary pickaxe acquisition. It precedes all experimental learning.

### Early accepted-sample boundary

Both separately owned 512-actor Academies completed the first declared learning
segment and shut down cleanly. The unchanged source starts at 92,574,669 samples
and 332,305 updates. Stopped artifacts, rather than a live status snapshot, determine
the exact additional sample counts used for evaluation.

| Arm | Additional accepted samples | Stopped total samples | Stopped updates | Segment wall seconds |
| --- | ---: | ---: | ---: | ---: |
| Control | 257,051 | 92,831,720 | 333,060 | 156.935 |
| Candidate | 261,444 | 92,836,113 | 333,173 | 152.943 |

Both sample counts fall within the declared +250,000 to +300,000 range. They are
not identical; wall time and update counts are not a throughput benchmark or a
matched optimizer-trajectory claim. The control native audit confirms its protected
anchor is unchanged. The candidate continued-state audit passes 282,649 checks,
including the same protected-function boundary.

| Stopped artifact | SHA-256 |
| --- | --- |
| Control training checkpoint | `3b06797e76ecb9f786cea05410973e63d4d3d3507d42c0fc950e77a35abfbc03` |
| Control exported policy | `59f9ba780fa1baf1ca1c9c71062fce1aa97ed1126d64a4841fae2ea67329e8a8` |
| Candidate training checkpoint | `ed4ba59ca19d32d932f7a6f5b927cbfa96db37aa2acb782e6aeca9c6a2df50f6` |
| Candidate exported policy | `6e42a9bd398cea4ac147359e315d1ea6ce9b60036e3bb45ee8a0dcda1b7b5576` |

The final pre-stop status in each arm reports 512 active/progressing actors,
all 512 first issued task 11, scope 11, a protected prior policy, and nonzero
accepted task-11 samples with every other task/unknown bucket zero. Inference
failures/rejections, stale/rejected learner samples and retired actors are zero.
These process-local observations are in each arm's `250000/last-observed.json`.

This early boundary is a transition budget, not a completed-episode quota. In the
last observed status the control had 500-529 decisions per actor and 207 completed
task-11 practice episodes; the candidate had 514-540 and 206. All of those finished
practice episodes were successes, while no task-11 probe or exam had completed in
either process. Do not report those selected completed episodes as 100% mastery,
infer that all long episodes finished, or count inherited course totals as new
experience. Learning uses bounded trajectory fragments, so unfinished episodes
can still contribute accepted samples.

The four ordinary early reports completed with the same vectors in both arms:

```text
control/candidate A: [64,64,62,64,64,64,64,63,64,64,57,0]
control/candidate B: [64,64,61,64,64,64,64,64,63,64,60,0]
```

Every ordinary task 0-10 retained its declared source-relative floor on each seed.
Ordinary pickaxe acquisition remained zero in both arms on both seeds. This result
alone cannot establish within-task retention; the assisted matrix is separate.

### Complete early assisted matrix and declared stop

All 20 early assisted reports also completed. Each entry is a full pickaxe
completion out of 32 under the named one-missing-cell reset intervention.

| Missing cell | Control A | Candidate A | Control B | Candidate B |
| --- | ---: | ---: | ---: | ---: |
| Top left | 3 | 2 | 0 | 3 |
| Top center | 0 | 1 | 1 | 6 |
| Top right | 13 | 14 | 9 | 11 |
| Handle upper | 0 | 0 | 0 | 0 |
| Handle lower | 0 | 0 | 0 | 0 |

Candidate top-left success fell from 15/32 to 2/32 on seed A and from 19/32 to
3/32 on seed B. Top-center success fell from 18/32 to 1/32 and from 19/32 to 6/32.
These four cells fail the preregistered source-minus-four retention floors of
11/32, 15/32, 14/32 and 15/32, respectively. Top-right candidate success met its
floors on both seeds; do not report all positions as equally impaired. The two
stick-placement conditions were already zero at baseline and remained zero.

Descriptive five-cell totals are candidate 17/160 and 20/160, versus its own
baseline 51/160 and 50/160. Control totals are 16/160 and 10/160, versus source
51/160 on each seed. The candidate's higher observed totals than control are not
hidden, but neither those totals nor one retained position repair the failed
per-cell retention criteria. Assisted gain and ordinary acquisition both failed.

The create-only `outcome.json` records:

```text
stage=early
full_budget_completed=false
retained=false
acquisition=false
assisted_gain=false
useful_pilot=false
reports=48
unique_trials=7424
deployment=false
```

The runner completed both 24-report matrices before making the declared early
stop. No +1,500,000 continuation or final phase ran. This is a completed early
rejection, not a full-budget result or an operational failure. All 7,424 trial
executions are accounted for once; all evaluations contributed zero learning
samples. The study supervisor and its evaluation jobs exited after writing the
outcome, without a live-runtime substitution.

### Interpretation and acceptance decision

Do not adopt this research runtime. The extra click-specific capacity preserves
initial actor/critic behavior but did not preserve useful task-11 partial behavior
through this early learning segment. Other-task protection remained intact while
the active task lost previously useful behavior. Ordinary completion stayed zero.
This falsifies the practical acceptance claim for this exact protocol; it does
not establish that conditional slot heads can never help, that the candidate
would never recover with a different training design, or which gradients caused
the regression. The declared early stop must not be removed after seeing it.

The next useful diagnosis is to compare preserved initial and stopped active
representations against output projections while retaining the identical protected
anchor and routing. The existing `PolicyBlocks.compose` explicitly rejects focused
policies, so a protected-policy diagnosis needs its own tested artifact boundary;
silently stripping protection would confound the comparison. Hybrid weights would
remain counterfactual diagnostics, not trained replacements. Any subsequent
learning change also needs observed temporal/context coverage, not only an accepted
transition count. Critic effects, optimization, observation encoding and coverage
remain hypotheses rather than established causes in this study.

The source, failed trained checkpoints, full matrices and qualification tests are
retained rather than merged as a deployable runtime. Main receives this evidence
and the decision-oriented cooperative roadmap only. No production `core`, `plugin`,
`training`, `host`, test or workflow change is part of that documentation update.

### Retained evidence

Raw evidence is in the shared Home Coder workspace `lkjsxc/tomato-ocelot-73`, under
`/home/coder/workspace/botsclustersmc-click-conditioned-slots/.build/`.
`click-slots-study/` contains the fixed declaration, per-arm native audits and
checkpoints, process-local status histories, both complete phase matrices,
per-report metadata/results/receipts, and the early gate and outcome.
`click-slots-gameplay.log` is the complete experiment log. The preceding failed
or partial qualification attempts remain distinct from the successful logs cited
above. Raw worlds and all trial artifacts are retained in that workspace, not
embedded in this GitHub Markdown record.

| Evidence | SHA-256 |
| --- | --- |
| Complete gameplay log | `6dfa62300da4dc1c353f638e91d8fc9120db5fb5b712944964c93a3f9a9a90e8` |
| Early gate | `eb993b7ad6dd947a5910d914a5650e65c7d6f5586d033dcbd6154a88f8c46510` |
| Outcome | `2c46130a23ca15882117ecf225d60ca0c48e02220c5091d1abcb09901197297c` |
| Complete baseline receipt matrix | `1d488618d74c8dcf51511b50ab79cdf2fa3c100d78830231e433a31ba51821af` |
| Complete early receipt matrix | `c6d000e60433d85693c8a4adc6b4c2b93cb54fd5ddfec357f676871c97f51cd7` |
| Control learning receipt | `c679c165628737ee6cb2119d8b969cf5ae00735a3df580c0af6041c1fb883dd0` |
| Candidate learning receipt | `14baedaeffa12b6cc18f8c3e1d729f9f42ac9acbd10cf0eadc83763e1bc9a259` |
