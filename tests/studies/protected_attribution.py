"""Fixed, evaluation-only two-factor diagnosis of the rejected click-slot policy."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from types import SimpleNamespace
import argparse, os, subprocess, sys
import click_slots_support as S

ROOT = Path(__file__).resolve().parents[2]
ARCHIVE = ROOT.parent/'botsclustersmc-click-conditioned-slots'
OUT = ROOT/'.build/protected-attribution'
RUNTIME = ROOT/'dist/training.jar'
BASE = ARCHIVE/'.build/click-slots-study/candidate-input-policy.bcmc'
DONOR = ARCHIVE/'.build/click-slots-study/candidate/250000/stopped-policy.bcmc'
PINS = {
    RUNTIME: 'fd35fa6408d5b56bc7ffd4b0822281732bae8ba1a367067232ca4378667f6741',
    BASE: '03d06a4a5d0b81abc687158ceb1e9325b151621b6712ae85e694452a3a9fe96e',
    DONOR: '6e42a9bd398cea4ac147359e315d1ea6ce9b60036e3bb45ee8a0dcda1b7b5576',
    ARCHIVE/'.build/slot-input/protected-training.bcmc': '36e668ddc1fae904a95a19c5c7e9fcedd8de190f65d827c2ef239f4f6596024f',
    ARCHIVE/'.build/click-slots-study/candidate/250000/stopped-training.bcmc': 'ed4ba59ca19d32d932f7a6f5b927cbfa96db37aa2acb782e6aeca9c6a2df50f6',
}
MASKS = (0, 3, 4, 7)
SEEDS = (2026093002, 2026093003)
CONDITIONS = S.CONDITIONS
CASES = 32
require = S.require


def source():
    require(not subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip(), 'Commit source before running')
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()


def qualify():
    for path, digest in PINS.items():
        require(S.sha(path) == digest, 'Pinned source changed: '+str(path))
    return source()


def specification(seed, condition):
    require(type(seed) is int and seed in SEEDS and type(condition) is str and condition in CONDITIONS, 'Undeclared condition')
    return SimpleNamespace(seed=seed, reset_intervention=condition,
        tasks=[10, 11] if condition == 'none' else [11], cases=CASES)


def matrix(rows):
    require(type(rows) is list, 'Report collection')
    expected = {(m, s, c) for m in MASKS for s in SEEDS for c in CONDITIONS}
    values = {}
    for row in rows:
        require(type(row) is dict and type(row.get('mask')) is int and type(row.get('seed')) is int
            and type(row.get('condition')) is str, 'Strict identifiers')
        key = row['mask'], row['seed'], row['condition']
        require(key in expected and key not in values, 'Unexpected or duplicate result')
        args = specification(row['seed'], row['condition'])
        scores = row.get('scores')
        require(type(scores) is list and len(scores) == len(args.tasks), 'Task score count')
        for score in scores: S.H.integer(score, CASES, 'Success count')
        require(S.H.integer(row.get('trials'), 2048, 'Trial count', 1) == CASES*len(args.tasks), 'Complete trial denominator')
        S.H.integer(row.get('new_training_samples'), 0, 'No learning')
        values[key] = scores
    require(set(values) == expected, 'Incomplete matrix')
    cells = []
    for condition in CONDITIONS[1:]:
        representation = [values[3, s, condition][0]-values[0, s, condition][0] for s in SEEDS]
        representation += [values[7, s, condition][0]-values[4, s, condition][0] for s in SEEDS]
        actor = [values[4, s, condition][0]-values[0, s, condition][0] for s in SEEDS]
        actor += [values[7, s, condition][0]-values[3, s, condition][0] for s in SEEDS]
        cells.append(dict(condition=condition, representation_deltas=representation, actor_deltas=actor,
            consistent_representation_drop=all(d <= -8 for d in representation),
            consistent_actor_drop=all(d <= -8 for d in actor)))
    return dict(diagnostic_only=True, deployment=False, new_training_samples=0,
        reports=len(rows), trials=sum(r['trials'] for r in rows), cells=cells, rows=rows)


def verify(directory, policy, expected_policy, args, port):
    report = S.H.verify_report(args, S.load(directory/'result.json'))
    metadata = S.load(directory/'metadata.json')
    require(metadata.get('policy_sha256') == S.sha(policy) == expected_policy, 'Policy identity')
    require(metadata.get('runtime_jar_sha256') == S.sha(directory/'runtime.jar') == PINS[RUNTIME], 'Runtime identity')
    require(metadata.get('exam_jar_sha256') == S.sha(directory/'server/plugins/exam.jar'), 'Exam identity')
    tasks = metadata.get('tasks')
    require(type(tasks) is list and all(type(t) is int for t in tasks) and tasks == args.tasks, 'Ordered tasks')
    require(S.H.integer(metadata.get('seed'), 2**63-1, 'Seed', -2**63) == args.seed, 'Seed binding')
    require(S.H.integer(metadata.get('cases_per_task'), 256, 'Cases', 1) == CASES, 'Case binding')
    require(S.H.integer(metadata.get('port'), 65535, 'Port', 1024) == port and metadata.get('bind') == '127.0.0.1', 'Isolation')
    require(metadata.get('input_kind') == 'inference-policy' and metadata.get('reset_intervention') == args.reset_intervention
        and metadata.get('diagnostic_only') is (args.reset_intervention != 'none'), 'Assistance/input binding')
    data = directory/'server/plugins/BotsClustersMC'
    require(S.read(data/'policy.bcmc') == S.read(policy), 'Policy changed during evaluation')
    require(not (data/'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')), 'Evaluation failure or learning')
    return [row['passed'] for row in report['tasks']]


def run():
    require(os.environ.get('EULA') == 'true', 'Explicit EULA=true is required')
    revision = qualify()
    subprocess.run([sys.executable, '-m', 'unittest', 'discover', '-s', 'tests/studies',
        '-p', 'test_protected_attribution.py'], cwd=ROOT, check=True)
    S.managed(OUT).mkdir()
    S.save(OUT/'declaration.json', dict(source=revision, input_identities={str(k): v for k, v in PINS.items()},
        masks=MASKS, seeds=SEEDS, conditions=CONDITIONS, ordinary_tasks=[10, 11], cases=CASES,
        maximum_concurrent_evaluators=4, new_training_samples=0, deployment=False,
        representation_bits=3, actor_bit=4, critic_bit_always_unset=True,
        sensitivity_margin=-8, scope='All backgrounds and both seeds per cell; no gradient-cause or mastery claim.'))
    try:
        for label, checkpoint, policy in (
                ('base', ARCHIVE/'.build/slot-input/protected-training.bcmc', BASE),
                ('donor', ARCHIVE/'.build/click-slots-study/candidate/250000/stopped-training.bcmc', DONOR)):
            S.execute(['java', '-cp', RUNTIME, 'org.botsclustersmc.training.CheckpointTool', 'verify-export', checkpoint, policy], OUT/(label+'-export.log'))
        classes = OUT/'classes'
        S.execute(['javac', '-cp', RUNTIME, '-d', classes,
            ROOT/'tests/transfer/org/botsclustersmc/diagnostic/GoalTransfer.java',
            ROOT/'tests/transfer/org/botsclustersmc/diagnostic/PolicyBlocks.java'], OUT/'compile.log')
        S.execute(['java', '-cp', str(classes)+os.pathsep+str(RUNTIME), 'org.botsclustersmc.diagnostic.PolicyBlocks',
            '--protected', BASE, DONOR, OUT/'policies'], OUT/'compose.log')
        manifest = S.load(OUT/'policies/counterfactuals.json')
        require(manifest.get('protected_anchor') is True and type(manifest.get('learning_task')) is int
            and manifest['learning_task'] == 11, 'Native protected boundary')
        require(manifest.get('base_policy_sha256') == PINS[BASE] and manifest.get('donor_policy_sha256') == PINS[DONOR], 'Native source identity')
        identities = {m: S.sha(OUT/f'policies/mask-{m}.bcmc') for m in MASKS}
        require(identities[0] == PINS[BASE], 'Exact baseline endpoint')
        def worker(index, mask):
            rows = []; port = 25592+index; policy = OUT/f'policies/mask-{mask}.bcmc'
            for seed in SEEDS:
                for condition in CONDITIONS:
                    require(qualify() == revision, 'Source changed during study'); S.memory()
                    args = specification(seed, condition); name = f'mask-{mask}-{seed}-{condition}'
                    directory = OUT/name
                    S.execute([sys.executable, ROOT/'tests/holdout.py', '--policy', policy,
                        '--runtime', RUNTIME, '--cache', S.CACHE, '--output', directory,
                        '--tasks', *args.tasks, '--cases', CASES, '--seed', seed,
                        '--port', port, '--reset-intervention', condition], OUT/(name+'.log'), timeout=1200)
                    scores = verify(directory, policy, identities[mask], args, port)
                    row = dict(mask=mask, seed=seed, condition=condition, scores=scores,
                        trials=CASES*len(args.tasks), new_training_samples=0,
                        result_sha256=S.sha(directory/'result.json'), metadata_sha256=S.sha(directory/'metadata.json'))
                    S.save(OUT/(name+'.receipt.json'), row); rows.append(row)
                    print(name, scores, flush=True)
            return rows
        rows = []
        with ThreadPoolExecutor(max_workers=4) as pool:
            futures = [pool.submit(worker, i, m) for i, m in enumerate(MASKS)]
            for future in as_completed(futures): rows.extend(future.result())
        require(qualify() == revision, 'Final source qualification')
        for mask, digest in identities.items(): require(S.sha(OUT/f'policies/mask-{mask}.bcmc') == digest, 'Final mixture identity')
        S.save(OUT/'outcome.json', matrix(rows))
        print('COMPLETE: 48 diagnostic reports, 1792 frozen trials; no training or deployment.', flush=True)
    except BaseException as error:
        S.save(OUT/'failure.json', dict(error=type(error).__name__, detail=str(error), complete=False, deployment=False))
        raise


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true', help='Run the predeclared frozen diagnostic matrix')
    args = parser.parse_args()
    if args.run: run()
    else: parser.print_help()
