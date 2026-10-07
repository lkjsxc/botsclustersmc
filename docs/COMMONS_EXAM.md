# Two-citizen commons: a resource-interdependence test

This is an independent frozen neural-policy experiment under `tests/commons`,
not a new training runtime or evidence of a self-sufficient settlement. It opens
a parallel cooperative-composition track rather than requiring the final individual
curriculum task before even measuring resource interdependence.

## Three matched resource conditions

Each case has two in-server Villager NPCs and exactly three oak planks and two
sticks. No finished pickaxe is supplied. Both actors use the same frozen policy,
the existing `CRAFT_WOOD_PICK` goal, and a fixed workbench target throughout the
trial. No role, condition label, partner inventory, routing instruction, scripted
aiming, or recipe sequence is added to their 512-float observations. Actor random
streams and initial orientations are paired across conditions. Four-case blocks
balance which actor initially holds the planks and mirror station positions.

* `split-shared`: complementary ingredients in different pockets, one room and
  one common chest. Transfers may use literal chest clicks or native item drops.
* `pooled-shared`: the same total ingredients start with one actor; the other
  starts empty, in the same shared room. This is a solo-sufficiency control, not
  evidence of cooperation merely because another actor is present.
* `split-isolated`: the same complementary ingredients are in two disconnected,
  otherwise identical rooms. Each actor has its own chest and workbench and the
  same per-actor tick budget. Room-local pickup provenance prevents partner items
  from crossing this boundary. This control has different geometry and two station
  copies; do not interpret a score subtraction alone as an isolated causal effect.

The first task goal remains raw pickaxe crafting. **The demand to bank the tool
is an external evaluation criterion, not a newly learned/observable banking goal.**
Failure can reflect this transfer-of-objective boundary; it does not diagnose an
intrinsic inability to cooperate. Autonomous shared-goal selection, a training
objective for collective stock and partner-policy generalization are further work.
A supplied policy is not claimed to have been trained on any of these conditions.

## What is and is not a completed trial

A completed team result needs exactly one newly crafted wooden pickaxe in a
shared chest, no other retained planks/sticks/pickaxes, and no resource loss.
Private inventory, cursor/grid contents and dropped items are included in the
closed resource budget. Preview outputs are not inventory. Each actor's final
carried counts are reported separately; a carried count includes hidden grids and
cursors, so it is not itself proof of immediately usable hotbar equipment.

The only resource conversions possible from this closed initial stock are two
planks into four sticks and three planks plus two sticks into one pickaxe. Count
one stick as one unit, a plank as two and a pickaxe as eight. The initial total is
eight. Transfers never mint resources or crafting credit. Lost units are retained
as failure evidence; excess units, duplicated crafted outputs and inconsistent
reported totals fail integrity instead of manufacturing completion. An isolated
actor cannot combine the missing ingredients even with ideal clicks.

The common-room final snapshot pauses both actors on the same owning region,
then captures both pockets, chest and dropped items. Isolated rooms publish
immutable independent final records; their combined result is NOT described as
an atomic snapshot of one shared region. Every room has a 3,000-tick ceiling by
default. The report retains actual actor ticks, completed decision observations,
GUI input counts, chest source-observation counts, termination reasons and every
failure. GUI selections and chest observations are not verified transfers.

## Disclosed environmental assistance and boundaries

This is a small fixed-infrastructure crafting/stock task. Bedrock rooms, workbenches
and chests are supplied; world block changes are refused. Chest inventory clicks
remain available. Bodies are invulnerable test NPCs. There is no mining objective,
food, wear, combat, persistent life database, natural-terrain generalization or
multi-day survival test. No mid-trial reset, teleport, supply injection, auto-equip,
menu-closing controller or scripted role is used. All actions go through the
normal `Npc` inference loop, sensors and primitive actuator.

Room admission starts shared actors together. Owner-thread checks guard body and
inventory reads; no tick waits for another region or inference. Only immutable
results leave a room. Cases, actors, rooms, horizon, status history, and test runtime
are bounded. The experimental plugin is built from the inference-only artifact,
not the training JAR: optimizer/curriculum classes are absent. Both public runtime
JARs remain unchanged and exclude the commons evaluator.

## Developer execution

Production remains Java-only. The opt-in orchestration/independent report checker
uses the repository's existing Python developer-test infrastructure. No checkpoint
exporter is added. Read and accept the Minecraft EULA before executing a live test.
Use a new output under this checkout's `.build/`; old evidence is never replaced.

```sh
./test.sh
python3 -m unittest discover -s tests -p test_commons.py
EULA=true python3 tests/commons.py --fresh-policy-seed 340711 \
  --seed 2026100711 --cases 4 --horizon 3000 \
  --output .build/commons-seed-a
```

`--fresh-policy-seed` deliberately creates an untrained immutable neural policy.
It tests the software path and is NEVER current learned-policy qualification.
An explicitly supplied inference-only `--policy` may be used instead; the runner
does not open, export or train a canonical checkpoint. The test server binds only
to loopback, refuses port 25565, enables a whitelist and admits no humans.

`metadata.json` records source-file hashes, source commit, exact policy/runtime/
exam/server digests and the policy origin. `result.json` retains every trial;
`integrity.json` exists only after the independent validator succeeds. Failure
logs, temporary worlds, configuration, immutable input and status observations
remain in the output. Exit zero means complete, self-consistent evidence, NOT
positive cooperation or acquisition. Optimized Python does not disable checks.

## Research direction

The unit of progress should become a functioning group, not a sum of historical
individual certificates. The next learned objective should describe shared stock
and needs without assigning permanent roles. Then test a frozen population with
new partners, shortages and replacement members: a group that only works with
one fixed partner is a fragile basis for communal life. This follows the evaluation
motivation of Leibo et al., *Scalable Evaluation of Multi-Agent Reinforcement
Learning with Melting Pot* (2021), https://arxiv.org/abs/2107.06857, rather than
claiming that this small Minecraft fixture implements Melting Pot or its results.

After real resource interdependence is learned, compose replenishment without
inventory resets, then food/tool consumption, recoverable lives and long-lived
shared worlds. Neural temporal abstraction remains a separate possible response
to the current tool-use bottleneck; it is not silently bundled into this exam.
