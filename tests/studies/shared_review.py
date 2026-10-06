#!/usr/bin/env python3
"""Finite shared-policy retention experiment using the unchanged review-admission runtime."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse
import json
import os
import traceback
import unittest
import zipfile
import mining_control as M
import mining_control_support as S

ROOT = Path(__file__).resolve().parents[2]
CONTROL_SOURCE = '7f18c3b83dd16dede2ff7f2337b3ab84465fe027'
CONTROL_RUNTIME = '82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9'
CANDIDATE_RUNTIME = 'd30ef666d97ffa2b5de32729f4171b8bb4c5f81d4c4d431a55feb5f5d13a3430'
INFERENCE = 'e16b1ade2e0a1a3d17f5532c3476929ecb8e45137bc636c6344698190d555ed0'
SEEDS = (2026100721, 2026100722)
TARGET = 250000
BOUNDARIES = (50000, 100000, 150000, 200000, 250000)
STATUS_OVERSHOOT = 25000
OUT = ROOT / '.build/shared-review-study'
for module in (M, S):
    module.ROOT = ROOT
    module.CONTROL = ROOT.parent / 'botsclustersmc-review-admission-control-20261007'
    module.CONTROL_SOURCE = CONTROL_SOURCE
    module.CONTROL_RUNTIME = CONTROL_RUNTIME
    module.CANDIDATE_RUNTIME = CANDIDATE_RUNTIME
    module.SEEDS = SEEDS
    module.TARGETS = (TARGET,)
    module.OUT = OUT


def coverage(history, startup, arm):
    """Reconstruct disjoint observed windows, not an invented exact sample/time cutoff."""
    S.require(arm in S.ARMS and type(history) is list and bool(history), 'Complete status history')
    S.require(type(startup) is dict and startup.get('startup_restored_checkpoint') is True, 'Restored startup')
    for key in ('startup_expected_agents', 'startup_observed_agents'):
        S.require(S.H.integer(startup.get(key), 512, key) == 512, 'Complete initial population')
    population = json.loads(startup.get('startup_training_task_population'))
    S.require(type(population) is list and len(population) == 18, 'Startup task array')
    for count in population:
        S.H.integer(count, 512, 'Startup count')
    S.require(sum(population) == 512, 'Startup denominator')
    for key in ('startup_review_agents', 'startup_frontier_agents'):
        S.H.integer(startup.get(key), 512, key)
    initial = (startup['startup_review_agents'] == 512 and startup['startup_frontier_agents'] == 0
               and sum(population[:12]) == 512 and not any(population[12:]))
    S.require(startup in history, 'Startup must be a retained actual status observation')
    previous = None
    old_cells = old_ticks = None
    observed = []
    first_review = None
    epoch = history[0].get('epoch_millis')
    for row in history:
        S.training_samples(row, S.BASE, epoch, row.get('epoch_millis'), previous)
        cells, ticks = S.context_counts(row, S.BASE)
        if old_cells is not None:
            S.require(all(a >= b for a, b in zip(cells, old_cells))
                      and all(a >= b for a, b in zip(ticks, old_ticks)), 'Context cells regressed')
        counts = [sum(cells[t*6:t*6+6]) for t in range(19)]
        item = dict(accepted_samples=sum(cells), by_task=counts, observed_actor_ticks=sum(ticks), epoch_millis=row['epoch_millis'])
        observed.append(item)
        if first_review is None and sum(counts[:12]) > 0:
            first_review = item
        previous, old_cells, old_ticks = row, cells, ticks
    picked = [next((row for row in observed if row['accepted_samples'] >= boundary), None) for boundary in BOUNDARIES]
    timely = all(row is not None and row['accepted_samples'] <= boundary + STATUS_OVERSHOOT
                 for boundary, row in zip(BOUNDARIES, picked))
    windows = []
    start = dict(accepted_samples=0, by_task=[0]*19, observed_actor_ticks=0, epoch_millis=epoch)
    for boundary, end in zip(BOUNDARIES, picked):
        if end is None:
            windows.append(dict(boundary=boundary, complete=False))
            continue
        delta = [b-a for a, b in zip(start['by_task'], end['by_task'])]
        total = end['accepted_samples'] - start['accepted_samples']
        S.require(total >= 0 and sum(delta) == total and all(n >= 0 for n in delta), 'Disjoint window accounting')
        if total == 0:
            windows.append(dict(boundary=boundary, complete=False, reason='One observation crossed multiple boundaries'))
            continue
        windows.append(dict(boundary=boundary, complete=True, start_samples=start['accepted_samples'],
                            end_samples=end['accepted_samples'], samples=total, by_task=delta,
                            review_share=sum(delta[:12])/total, frontier_share=delta[12]/total,
                            actor_ticks=end['observed_actor_ticks']-start['observed_actor_ticks']))
        start = end
    late = None
    if picked[1] is not None and picked[-1] is not None:
        a, b = picked[1], picked[-1]
        delta = [y-x for x, y in zip(a['by_task'], b['by_task'])]
        total = b['accepted_samples']-a['accepted_samples']
        S.require(total >= 0 and sum(delta) == total, 'Late-window accounting')
        if total > 0:
            late = dict(start_samples=a['accepted_samples'], end_samples=b['accepted_samples'], samples=total,
                        by_task=delta, review_share=sum(delta[:12])/total, frontier_share=delta[12]/total)
    early = picked[0]
    delivered = bool(initial and timely and early is not None and all(n > 0 for n in early['by_task'][:13])
                     and all(row['complete'] and .05 <= row['review_share'] <= .40 and row['frontier_share'] >= .50 for row in windows)
                     and late is not None and all(n >= 100 for n in late['by_task'][:12]))
    return dict(arm=arm, startup_match=initial, initial_training_population=population,
                timely_windows=timely, sustained_accepted_review=delivered, windows=windows, late=late,
                first_observed_review=first_review, last=observed[-1], status_observations=len(history))


def interpretation(baseline, current, exposure):
    base = S.vectors(baseline, ('parent',))
    actual = S.vectors(current, S.ARMS)
    S.require(type(exposure) is dict and set(exposure) == set(S.ARMS), 'Both measured exposure arms')
    for arm in S.ARMS:
        S.require(type(exposure[arm]) is dict and type(exposure[arm].get('startup_match')) is bool
                  and type(exposure[arm].get('sustained_accepted_review')) is bool, 'Measured exposure booleans')
        population = exposure[arm].get('initial_training_population')
        S.require(type(population) is list and len(population) == 18, 'Measured initial distribution')
        for value in population:
            S.H.integer(value, 512, 'Measured initial task count')
        S.require(sum(population) == 512, 'Measured initial denominator')
        late = exposure[arm].get('late')
        if late is not None:
            S.require(type(late) is dict, 'Measured late exposure')
            S.H.number(late.get('review_share'), 1, 'Measured late review share')
    retained = {}
    details = []
    reproduced = acquisition = True
    for seed in SEEDS:
        parent = base['parent', seed, 'none']
        floors = [max(28, p-3) if t < 11 else max(26, p-4) if t == 11 else max(0, p-2) for t, p in enumerate(parent)]
        for arm in S.ARMS:
            scores = actual[arm, seed, 'none']
            failed = [dict(task=t, source=parent[t], score=n, floor=floors[t]) for t, n in enumerate(scores) if n < floors[t]]
            retained[arm, seed] = not failed
            details.append(dict(arm=arm, seed=seed, failures=failed, pickaxe=scores[11], mining=scores[12]))
        control, candidate = actual['control', seed, 'none'], actual['candidate', seed, 'none']
        reproduced &= parent[11]-control[11] >= 8 and all(control[t] >= floors[t] for t in range(11))
        acquisition &= candidate[12] >= 8 and candidate[12] >= control[12]+4
    control, candidate = exposure['control'], exposure['candidate']
    same_start = control['startup_match'] and candidate['startup_match'] and control['initial_training_population'] == candidate['initial_training_population']
    contrast = bool(same_start and candidate['sustained_accepted_review'] and control['late'] is not None
                    and candidate['late'] is not None and candidate['late']['review_share'] >= control['late']['review_share']+.05)
    kept = all(retained['candidate', seed] for seed in SEEDS)
    return dict(candidate_retained=kept, control_retained=all(retained['control', seed] for seed in SEEDS),
                control_pickaxe_loss_reproduced=reproduced, sustained_exposure_contrast=contrast,
                shared_review_retention_supported=kept and reproduced and contrast, acquisition=acquisition,
                details=details, deployment=False)


class Study(M.Study):
    def guard(self):
        super().guard()
        S.require(not S.git(ROOT, 'diff', '3d6cd3ac69f47ffb2d8062a9cbd444f8705052b6', '--', 'core', 'plugin', 'training'),
                  'Admission runtime must remain the exact previously observed candidate')
        prefix = 'training/src/org/botsclustersmc/training/'
        changed = set(S.git(ROOT, 'diff', '--name-only', CONTROL_SOURCE, '--', 'core', 'plugin', 'training').splitlines())
        S.require(changed == {prefix+'Course.java', prefix+'ReviewEffort.java', prefix+'TrainingPlugin.java'}, 'Only admission and its lifecycle may differ')
        payload = []
        for arm in S.ARMS:
            with zipfile.ZipFile(self.runtimes[arm]) as jar:
                payload.append({name: jar.read(name) for name in jar.namelist()})
        delta = {name for name in set(payload[0]) | set(payload[1]) if payload[0].get(name) != payload[1].get(name)}
        S.require(delta == {'org/botsclustersmc/training/'+name+'.class' for name in ('Course', 'Course$Agent', 'Course$Metrics', 'Course$Progress', 'Course$StageMetrics', 'ReviewEffort', 'TrainingPlugin')}, 'Training artifact boundary')
        for root in self.roots.values():
            S.require(S.sha(root/'dist/botsclustersmc.jar') == INFERENCE, 'Exact unchanged inference artifact')

    def exposure(self):
        result = {}
        for arm in S.ARMS:
            folder = OUT/arm/str(TARGET)
            history = [json.loads(line) for line in S.read(folder/'status.jsonl').decode().splitlines()]
            result[arm] = coverage(history, S.load(folder/'first-active.json'), arm)
        S.save(OUT/'exposure.json', result)
        return result

    def run(self):
        S.require(S.ENV.get('EULA') == 'true', 'Explicit EULA=true required')
        S.memory()
        OUT.mkdir()
        S.save(OUT/'declaration.json', dict(kind='shared-policy-review-retention', prior_individual_qualification='failed, not reclassified; PR60', source=self.source,
            control_source=CONTROL_SOURCE, control_runtime=CONTROL_RUNTIME, candidate_runtime=CANDIDATE_RUNTIME,
            input_checkpoint=S.INPUT_SHA, input_policy=S.POLICY_SHA, baseline_samples=S.BASE,
            baseline_updates=S.BASE_UPDATES, seeds=SEEDS, ordinary_tasks=list(range(13)), cases=32,
            reset_intervention='none', accepted_sample_budget=TARGET, overshoot=S.OVERSHOOT, actors=512,
            learning_seed=7, training_ports=[31681, 31682], evaluation_ports=list(range(31690,31694)),
            maximum_training_servers=2, maximum_evaluators=4, observed_boundaries=BOUNDARIES,
            status_boundary_overshoot=STATUS_OVERSHOOT, review_window_share=[.05,.40], minimum_frontier_window_share=.50,
            late_interval=[100000,250000], minimum_late_samples_per_earlier_task=100, minimum_late_review_share_advantage=.05,
            intervention='Use the unchanged PR60 non-preemptive admission cap: max(1,floor(4/5 same-frontier participating trainees)); keep individual actual effort and least-reviewed task choice.',
            unchanged='Both arms use the rejected review-first resume plus stronger mining reward and task-12 critic-feature detachment. The control is NOT ordinary accepted-main training.',
            excludes='No invented effort, episode interruption, per-actor full-review-cycle claim, action teacher, reward, gradient, model, optimizer or reset change between arms.',
            support='All candidate per-task floors on both seeds, reproduced control pickaxe loss >=8/32 on each with earlier tasks retained, identical initial task population and sustained accepted exposure contrast.',
            limitation='One learner trajectory per arm. Two evaluation seeds are not independent learning replicas. Window boundaries are observed, not exact first-gradient timestamps.',
            deployment=False))
        try:
            for arm in S.ARMS:
                self.identity(arm, S.INPUT, OUT, arm+'-input', True)
            baseline = self.evaluate('baseline')
            qualified = S.qualify_baseline(baseline)
            phases = [baseline]
            decision = None
            if qualified:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    futures = [pool.submit(self.train, arm, TARGET) for arm in S.ARMS]
                    for future in futures:
                        future.result()
                exposure = self.exposure()
                current = self.evaluate('early', TARGET)
                phases.append(current)
                decision = interpretation(baseline, current, exposure)
                S.save(OUT/'interpretation.json', decision)
            self.guard()
            result = dict(source=self.source, stage='complete' if qualified else 'input-rejected',
                          input_qualified=qualified, reports=sum(map(len, phases)),
                          frozen_trials=sum(row['trials'] for phase in phases for row in phase),
                          accepted_sample_budget=TARGET, decision=decision, new_training_after_boundary=False, deployment=False)
            S.save(OUT/'outcome.json', result)
            print('SHARED REVIEW STUDY COMPLETE', result, flush=True)
        except BaseException as error:
            S.save(OUT/'failure.json', dict(error=type(error).__name__, detail=str(error), traceback=traceback.format_exc(), deployment=False))
            raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true')
    args = parser.parse_args()
    if not args.run:
        parser.print_help()
        return
    S.require(os.environ.get('EULA') == 'true', 'Explicit EULA=true before study activity')
    suite = unittest.defaultTestLoader.discover(str(ROOT/'tests/studies'), pattern='test_shared_review.py')
    S.require(suite.countTestCases() >= 16 and unittest.TextTestRunner().run(suite).wasSuccessful(), 'Offline qualification failed')
    Study().run()


if __name__ == '__main__':
    main()
