#!/usr/bin/env python3
"""Fixed crossed-policy / same-seed physical repeats. No learner or runtime changes."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from types import SimpleNamespace
import argparse
import hashlib
import json
import os
import signal
import subprocess
import sys
import time
import traceback

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tests'))
import holdout as H

MAIN = ROOT.parent/'botsclustersmc'
ENTRY = '2726b81bae1985daf525b920bca3b33255ba6753'
OUT = ROOT/'.build/control-repeatability-study'
RUNTIME = ROOT.parent/'botsclustersmc-review-admission-control-20261007/dist/training.jar'
RUNTIME_SHA = '82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9'
CACHE = MAIN/'.cache/server'
SEEDS = (2026100741, 2026100742)
REPEATS = (0, 1)
TASKS = tuple(range(13))
CASES = 32
MODELS = ('parent', 'control-earlier', 'control-later', 'review-later')
PARENT = MAIN/'.build/context-activation-20260930/stopped-policy.bcmc'
EARLIER = ROOT.parent/'botsclustersmc-cohort-review-20261006/.build/cohort-review-study'
LATER = ROOT.parent/'botsclustersmc-shared-review-20261007/.build/shared-review-study'
POLICIES = {
    'parent': (PARENT, 'ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc'),
    'control-earlier': (EARLIER/'control/250000/stopped-policy.bcmc', '7b9f51d605c89d34cd13c3e3cbd4a36ec281afad62156962e2e6b49b3a974af1'),
    'control-later': (LATER/'control/250000/stopped-policy.bcmc', 'd6543d1205b2507c23e15ffe844d4e11cf47656df70840bb185669cd29df433a'),
    'review-later': (LATER/'candidate/250000/stopped-policy.bcmc', '57144e26e1af87fa286d7eaf037f928d4fa005e1251bc5155b55e6c87936ea22'),
}
require = H.require


def safe(path):
    path = Path(path).absolute()
    require(not any(p.is_symlink() for p in (path, *path.parents)), 'Symlinked managed study path')
    return path


def sha(path):
    return hashlib.sha256(H.read_bounded(safe(path), 32*1024*1024)).hexdigest()


def save(path, value):
    with safe(path).open('x') as out:
        json.dump(value, out, indent=2, allow_nan=False)
        out.write('\n')
        out.flush()
        os.fsync(out.fileno())


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def guard(source):
    require(git('rev-parse', 'HEAD') == source and not git('status', '--porcelain'), 'Pinned clean study source')
    require(not git('diff', ENTRY, source, '--', 'core', 'plugin', 'training', 'host', 'tests/holdout', 'tests/holdout.py', 'tests/acceptance.py', '.github'), 'No runtime/evaluator/workflow intervention')
    require(sha(RUNTIME) == RUNTIME_SHA, 'Exact common frozen evaluator runtime')
    for path, digest in POLICIES.values():
        require(sha(path) == digest, 'Exact immutable saved policy: '+str(path))


def schedule():
    """One policy per slot, rotated across four waves; port is never a policy label."""
    waves = []
    for repeat in REPEATS:
        for seed_index, seed in enumerate(SEEDS):
            wave = len(waves)
            order = MODELS[wave:] + MODELS[:wave]
            waves.append([dict(model=model, seed=seed, repeat=repeat, port=31710+slot, wave=wave)
                          for slot, model in enumerate(order)])
    return waves


def validate_rows(rows):
    expected = {(model, seed, repeat) for model in MODELS for seed in SEEDS for repeat in REPEATS}
    require(type(rows) is list and len(rows) == len(expected), 'Complete 16-report matrix')
    found = {}
    for row in rows:
        require(type(row) is dict and type(row.get('model')) is str, 'Named model')
        seed, repeat = row.get('seed'), row.get('repeat')
        require(type(seed) is int and type(repeat) is int, 'Exact seed/repeat integers')
        key = row['model'], seed, repeat
        require(key in expected and key not in found, 'Unique declared model/seed/repeat')
        require(row.get('new_training_samples') == 0 and type(row.get('new_training_samples')) is int, 'No evaluation learning')
        scores, outcomes = row.get('scores'), row.get('outcomes')
        require(type(scores) is list and type(outcomes) is list and len(scores) == len(outcomes) == 13, 'Full ordered task vectors')
        for score, bits in zip(scores, outcomes):
            H.integer(score, 32, 'Task score')
            require(type(bits) is list and len(bits) == 32 and all(type(bit) is bool for bit in bits), 'All 32 physical outcomes, not a success summary')
            require(sum(bits) == score, 'Outcome/score marginal')
        require(row.get('trials') == 416 and type(row.get('trials')) is int, 'Every report has 416 trials')
        found[key] = row
    return found


def analyze(rows):
    data = validate_rows(rows)
    within = []
    for model in MODELS:
        for seed in SEEDS:
            first, second = (data[model, seed, r] for r in REPEATS)
            score_deltas = [b-a for a, b in zip(first['scores'], second['scores'])]
            flips = [sum(a != b for a, b in zip(x, y)) for x, y in zip(first['outcomes'], second['outcomes'])]
            within.append(dict(model=model, seed=seed, score_deltas=score_deltas, outcome_flips=flips,
                               pickaxe_score_repeatable=abs(score_deltas[11]) <= 3,
                               pickaxe_case_repeatable=flips[11] <= 4))
    control_gap = []
    for seed in SEEDS:
        old = [data['control-earlier', seed, r]['scores'][11] for r in REPEATS]
        new = [data['control-later', seed, r]['scores'][11] for r in REPEATS]
        control_gap.append(dict(seed=seed, earlier=old, later=new,
                                later_min_minus_earlier_max=min(new)-max(old),
                                earlier_min_minus_later_max=min(old)-max(new)))
    later_better = all(row['later_min_minus_earlier_max'] >= 4 for row in control_gap)
    earlier_better = all(row['earlier_min_minus_later_max'] >= 4 for row in control_gap)
    retention = []
    for model in MODELS[1:]:
        for seed in SEEDS:
            for repeat in REPEATS:
                parent = data['parent', seed, repeat]['scores']
                scores = data[model, seed, repeat]['scores']
                floors = [max(28, n-3) if t<11 else max(26, n-4) if t==11 else max(0, n-2) for t, n in enumerate(parent)]
                failures = [dict(task=t, source=parent[t], score=n, floor=floors[t]) for t, n in enumerate(scores) if n<floors[t]]
                retention.append(dict(model=model, seed=seed, repeat=repeat, failures=failures,
                                      pickaxe_loss=parent[11]-scores[11]))
    parent_qualified = all(min(data['parent', s, r]['scores'][:11]) >= 28 and data['parent', s, r]['scores'][11] >= 26
                           for s in SEEDS for r in REPEATS)
    review_gap = [dict(seed=s, repeat=r, candidate_minus_control=data['review-later', s, r]['scores'][11]-data['control-later', s, r]['scores'][11])
                  for s in SEEDS for r in REPEATS]
    return dict(parent_qualified=parent_qualified, within_policy=within, control_state_gaps=control_gap,
                control_separation='later-better' if later_better else 'earlier-better' if earlier_better else 'not-separated',
                pickaxe_score_repeatable=all(row['pickaxe_score_repeatable'] for row in within),
                pickaxe_case_repeatable=all(row['pickaxe_case_repeatable'] for row in within),
                retention=retention, review_vs_later_control=review_gap,
                review_positive_in_every_pair=all(row['candidate_minus_control'] >= 4 for row in review_gap),
                new_training_samples=0, deployment=False)


def receipt(job, folder):
    args = SimpleNamespace(tasks=list(TASKS), cases=CASES, seed=job['seed'], reset_intervention='none')
    result = H.verify_report(args, H.read_report(folder/'result.json'))
    metadata = H.read_report(folder/'metadata.json')
    require(metadata.get('policy_sha256') == POLICIES[job['model']][1], 'Correct frozen model binding')
    require(metadata.get('runtime_jar_sha256') == sha(folder/'runtime.jar') == RUNTIME_SHA, 'Identical frozen runtime binding')
    require(metadata.get('exam_jar_sha256') == sha(folder/'server/plugins/exam.jar'), 'Exact exam payload')
    require(metadata.get('tasks') == list(TASKS) and metadata.get('cases_per_task') == CASES and metadata.get('seed') == job['seed'], 'Exact evaluator task/seed binding')
    require(metadata.get('port') == job['port'] and metadata.get('bind') == '127.0.0.1' and metadata.get('input_kind') == 'inference-policy', 'Isolated input and listener')
    require(metadata.get('reset_intervention') == 'none' and metadata.get('diagnostic_only') is False, 'Ordinary unassisted reset condition')
    data = folder/'server/plugins/BotsClustersMC'
    require(sha(data/'policy.bcmc') == POLICIES[job['model']][1], 'Policy unchanged after evaluation')
    require(not (data/'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')), 'No failed evaluation or learner state')
    by_id = {trial['actor']: trial for trial in result['trials']}
    outcomes = [[by_id[t*CASES+c]['success'] for c in range(CASES)] for t in TASKS]
    mining = [by_id[12*CASES+c]['diagnostics'] for c in range(CASES)]
    observations = sum(d['observations'] for d in mining)
    menus = [sum(d['menu_observations'][i] for d in mining) for i in range(5)]
    for n in menus:
        H.integer(n, observations, 'Mining menu count')
    require(sum(menus) == observations, 'Mining context denominator')
    diagnostic = dict(observations=observations, menu=menus,
                      held_pick=sum(d['harvest']['held_pick_observations'] for d in mining),
                      pick_contact=sum(d['harvest']['target_pick_contact_observations'] for d in mining),
                      world_dig=sum(d['harvest']['world_dig_selections'] for d in mining),
                      broken=sum(d['blocks_broken'] for d in mining))
    return dict(**job, scores=[sum(bits) for bits in outcomes], outcomes=outcomes, mining=diagnostic,
                trials=416, new_training_samples=0, policy_sha256=POLICIES[job['model']][1],
                report_sha256=sha(folder/'result.json'), metadata_sha256=sha(folder/'metadata.json'))


def execute(job, source):
    guard(source)
    label = f"{job['model']}-{job['seed']}-repeat{job['repeat']}"
    folder = OUT/label
    env = dict(os.environ, EULA='true', JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
    for key in ('GH_TOKEN', 'GITHUB_TOKEN'):
        env.pop(key, None)
    args = ['python3', str(ROOT/'tests/holdout.py'), '--policy', str(POLICIES[job['model']][0]),
            '--runtime', str(RUNTIME), '--cache', str(CACHE), '--output', str(folder), '--tasks',
            *map(str, TASKS), '--cases', str(CASES), '--seed', str(job['seed']), '--port', str(job['port'])]
    save(OUT/(label+'-started.json'), dict(**job, source=source, command=args, epoch_millis=int(time.time()*1000)))
    with (OUT/(label+'.log')).open('x') as log:
        process = subprocess.Popen(args, cwd=ROOT, env=env, stdin=subprocess.DEVNULL,
                                   stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            require(process.wait(timeout=900) == 0, 'Evaluation process failed: '+label)
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=45)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=15)
    guard(source)
    row = receipt(job, folder)
    save(OUT/(label+'-receipt.json'), row)
    print('FROZEN', label, row['scores'], flush=True)
    return row


def resource_admission():
    available = next(int(line.split()[1])*1024 for line in Path('/proc/meminfo').read_text().splitlines() if line.startswith('MemAvailable:'))
    maximum = Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum != 'max':
        available = min(available, int(maximum)-int(Path('/sys/fs/cgroup/memory.current').read_text()))
    require(available >= 10*1024**3, 'Four evaluators require 10 GiB available; do not stop unrelated projects')
    return available


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    source = git('rev-parse', 'HEAD')
    guard(source)
    if args.check:
        print('PASS pinned source, four saved policies and unchanged evaluator/runtime; no gameplay.')
        return
    if not args.run:
        parser.print_help()
        return
    # Reuse explicit stored operator consent, not a newly accepted agreement.
    require(any(line.strip() == 'EULA=true' for line in (MAIN/'.env').read_text().splitlines()), 'Existing explicit Minecraft EULA consent required')
    available = resource_admission()
    OUT.mkdir()
    save(OUT/'declaration.json', dict(source=source, entry=ENTRY, policies={m:dict(path=str(p), sha256=h) for m,(p,h) in POLICIES.items()},
         available_memory_at_start=available, runtime_sha256=RUNTIME_SHA, tasks=list(TASKS), cases=CASES, seeds=list(SEEDS), repeats=list(REPEATS),
         waves=schedule(), reports=16, trials=6656, new_training_samples=0,
         pickaxe_score_repeatability_margin=3, pickaxe_case_flip_margin=4, separated_control_envelope_margin=4,
         limitation='Selected historical training trajectories, not randomized new learning replicates; engineering screens are not confidence intervals.', deployment=False))
    rows = []
    try:
        for wave in schedule():
            resource_admission()
            with ThreadPoolExecutor(max_workers=4) as pool:
                futures = [pool.submit(execute, job, source) for job in wave]
                rows.extend(f.result() for f in futures)
        save(OUT/'reports.json', rows)
        result = analyze(rows)
        save(OUT/'outcome.json', result)
        guard(source)
        print('COMPLETE reports=16 frozen_trials=6656 new_training_samples=0', flush=True)
    except BaseException as failure:
        save(OUT/'failure.json', dict(error=type(failure).__name__, detail=str(failure), traceback=traceback.format_exc(), completed_reports=len(rows), deployment=False))
        raise


if __name__ == '__main__':
    main()
