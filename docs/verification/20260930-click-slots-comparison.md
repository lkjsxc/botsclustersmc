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

No gameplay outcome is claimed by this declaration. Exact runner revision,
immutable identities and completed results will be recorded by the create-only
study declaration and per-phase receipts. The live accepted runtime is not replaced
by running or qualifying this research candidate.
