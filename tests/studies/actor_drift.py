#!/usr/bin/env python3
"""Finite, frozen attribution of the preserved critic-detachment actor drift."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from types import SimpleNamespace
import argparse, hashlib, json, os, signal, subprocess, sys, traceback, unittest, zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tests'))
import holdout as H
require = H.require
MAIN = ROOT.parent / 'botsclustersmc'
BASE_SOURCE = '08a9963dd10a125310af6c31f61f6e7c5c1a764f'
PARENT = MAIN / '.build/context-activation-20260930'
DONOR = ROOT.parent / 'botsclustersmc-critic-detachment/.build/critic-detachment-study/candidate/250000'
RUNTIME = ROOT.parent / 'botsclustersmc-critic-detachment-control/dist/training.jar'
CACHE = MAIN / '.cache/server'
OUT = ROOT / '.build/actor-drift-study'
SEEDS = (2026100201, 2026100202)
TASKS = list(range(13))
CASES = 32
ARMS = ('parent', 'candidate', 'representation', 'output', 'actor')
MASKS = {'representation': 3, 'output': 4, 'actor': 7}
PINS = {
    PARENT / 'stopped-policy.bcmc': 'ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc',
    PARENT / 'stopped-training.bcmc': 'a0ef5870276abc8f2cb6f7a30c95e3a57d20c0d9d3f8097a98132517b7e0e05e',
    DONOR / 'stopped-policy.bcmc': 'bd8fc04cc2e1e5b770cffecd6e833b3c26020d1af41c40c3652fc3cf10cff9b9',
    DONOR / 'stopped-training.bcmc': 'ea196e219c8fd0d1ddcf3156df6811dfb57832cd94cbcdbb367266c11066fb40',
    RUNTIME: '782fb3bcde791745672b5d1433fa3f3efb190737dc594578bbc796079e8233c3',
}
ENV = dict(os.environ, JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
for key in ('GH_TOKEN', 'GITHUB_TOKEN'):
    ENV.pop(key, None)
JAVA = os.environ.get('JAVA_BIN', 'java')
JAVAC = str(Path(JAVA).with_name('javac')) if os.path.sep in JAVA else 'javac'


def managed(path):
    path = Path(path).absolute()
    require(not any(p.is_symlink() for p in (path, *path.parents)), 'Symlinked evidence path')
    return path


def read(path):
    return H.read_bounded(managed(path), 32 * 1024 * 1024)


def sha(path):
    return hashlib.sha256(read(path)).hexdigest()


def save(path, data):
    with managed(path).open('xb') as stream:
        stream.write((json.dumps(data, indent=2, allow_nan=False) + '\n').encode())
        stream.flush()
        os.fsync(stream.fileno())


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def execute(command, log, timeout=480):
    with managed(log).open('x') as output:
        process = subprocess.Popen(list(map(str, command)), cwd=ROOT, env=ENV,
            stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            require(process.wait(timeout=timeout) == 0, 'Command failed; retained log: ' + str(log))
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=15)


def memory():
    available = next(int(line.split()[1]) * 1024 for line in Path('/proc/meminfo').read_text().splitlines()
                     if line.startswith('MemAvailable:'))
    maximum = Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum != 'max':
        available = min(available, int(maximum) - int(Path('/sys/fs/cgroup/memory.current').read_text()))
    require(available >= 10 * 1024**3, 'Available memory below four-evaluator safety floor')


def spec(seed):
    require(type(seed) is int and seed in SEEDS, 'Undeclared seed')
    return SimpleNamespace(tasks=TASKS.copy(), cases=CASES, seed=seed, reset_intervention='none')


def vectors(rows, arms):
    require(type(rows) is list, 'Report collection')
    expected = {(arm, seed) for arm in arms for seed in SEEDS}
    found = {}
    for row in rows:
        require(type(row) is dict and type(row.get('arm')) is str, 'Report object')
        spec(row.get('seed'))
        key = row['arm'], row['seed']
        require(key in expected and key not in found, 'Unexpected or duplicate report')
        values = row.get('scores')
        require(type(values) is list and len(values) == len(TASKS), 'Complete ordered task vector')
        for value in values:
            H.integer(value, CASES, 'Task success count')
        require(H.integer(row.get('trials'), 2048, 'Trials', 1) == CASES * len(TASKS), 'Complete trials')
        H.integer(row.get('new_training_samples'), 0, 'Frozen samples')
        require(row.get('condition') == 'none', 'No reset assistance')
        found[key] = values
    require(set(found) == expected, 'Incomplete condition matrix')
    return found


def qualify(rows):
    matrix = vectors(rows, ('parent', 'candidate'))
    return all(min(matrix['parent', seed][:11]) >= 28 and matrix['parent', seed][11] >= 26
        and min(matrix['candidate', seed][:11]) >= 28
        and matrix['parent', seed][11] - matrix['candidate', seed][11] >= 8 for seed in SEEDS)


def interpret(rows):
    matrix = vectors(rows, ARMS)
    details = []
    for seed in SEEDS:
        p, c, r, o, a = (matrix[arm, seed] for arm in ARMS)
        floor = max(26, p[11] - 4)
        details.append(dict(seed=seed, pickaxe=[v[11] for v in (p, c, r, o, a)], pickaxe_floor=floor,
            representation_losses=[p[11] - r[11], o[11] - a[11]],
            output_losses=[p[11] - o[11], r[11] - a[11]],
            output_only_retained=o[11] >= floor,
            representation_only_retained=r[11] >= floor,
            actor_vs_original_candidate=[a[t] - c[t] for t in TASKS],
            retained={arm: all(v[t] >= max(28, p[t] - 3) for t in range(11))
                and v[11] >= floor and v[12] >= max(0, p[12] - 2)
                for arm, v in zip(ARMS, (p, c, r, o, a))},
            mining={arm: v[12] for arm, v in zip(ARMS, (p, c, r, o, a))}))
    return dict(input_qualified=qualify([row for row in rows if row['arm'] in ('parent', 'candidate')]),
        representation_consistent_loss=all(min(row['representation_losses']) >= 8 for row in details),
        output_consistent_loss=all(min(row['output_losses']) >= 8 for row in details),
        output_only_pickaxe_retained=all(row['output_only_retained'] for row in details),
        representation_only_pickaxe_retained=all(row['representation_only_retained'] for row in details),
        details=details, new_training_samples=0, deployment=False,
        claim='Counterfactual parameter sensitivity only; no gradient-cause or learning-acquisition inference.')


def exam_payload(path):
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        require(len(names) == len(set(names)), 'Duplicate exam JAR entry')
        return {name: hashlib.sha256(jar.read(name)).hexdigest() for name in names if name != 'config.yml'}


def verify(directory, policy, arm, seed, port):
    report = H.verify_report(spec(seed), H.read_report(directory / 'result.json'))
    metadata = H.read_report(directory / 'metadata.json')
    require(metadata.get('policy_sha256') == sha(policy), 'Exact policy binding')
    require(metadata.get('runtime_jar_sha256') == sha(directory / 'runtime.jar') == PINS[RUNTIME], 'Runtime binding')
    exam = directory / 'server/plugins/exam.jar'
    require(metadata.get('exam_jar_sha256') == sha(exam), 'Exam JAR binding')
    require(metadata.get('tasks') == TASKS and all(type(t) is int for t in metadata['tasks']), 'Task order')
    require(type(metadata.get('seed')) is int and metadata['seed'] == seed, 'Metadata seed')
    require(type(metadata.get('cases_per_task')) is int and metadata['cases_per_task'] == CASES, 'Metadata cases')
    require(metadata.get('reset_intervention') == 'none' and metadata.get('diagnostic_only') is False, 'Reset scope')
    require(metadata.get('bind') == '127.0.0.1' and type(metadata.get('port')) is int
        and metadata['port'] == port and metadata.get('input_kind') == 'inference-policy', 'Isolation')
    count = CASES * len(TASKS)
    config = (f'max-agents: {count}\nmax-loaded-chunks: {count}\ninference-threads: 1\nseed: {seed}\n'
        f'cases-per-task: {CASES}\ntasks: {TASKS}\nworld-edits: false\n').encode()
    with zipfile.ZipFile(exam) as jar:
        require(jar.read('config.yml') == config, 'Executed config differs from complete declaration')
    data = directory / 'server/plugins/BotsClustersMC'
    require(read(data / 'policy.bcmc') == read(policy), 'Weights changed during evaluation')
    require(not (data / 'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')), 'Evaluation failure or learning')
    selected = [trial for trial in report['trials'] if trial['task'] == 12]
    harvest = [trial['diagnostics']['harvest'] for trial in selected]
    mining = dict(observations=sum(t['diagnostics']['observations'] for t in selected),
        held_pick=sum(t['held_pick_observations'] for t in harvest),
        pick_contact=sum(t['target_pick_contact_observations'] for t in harvest),
        max_pick_ticks=max(t['max_target_pick_ticks'] for t in harvest),
        world_dig=sum(t['world_dig_selections'] for t in harvest),
        broken=sum(t['diagnostics']['blocks_broken'] for t in selected),
        generic_items_collected=sum(t['diagnostics']['items_collected'] for t in selected))
    return dict(arm=arm, seed=seed, condition='none', scores=[t['passed'] for t in report['tasks']],
        trials=len(report['trials']), new_training_samples=0, policy_sha256=sha(policy),
        result_sha256=sha(directory / 'result.json'), metadata_sha256=sha(directory / 'metadata.json'),
        exam_sha256=sha(exam), mining=mining, directory=str(directory))


class Study:
    def __init__(self):
        self.source = git('rev-parse', 'HEAD')
        self.guard()

    def guard(self):
        require(git('rev-parse', 'HEAD') == self.source and not git('status', '--porcelain'), 'Study source changed or dirty')
        require(not git('diff', BASE_SOURCE, '--', 'core', 'plugin', 'training', 'host', 'tests/holdout',
            'tests/transfer', 'tests/holdout.py', 'tests/acceptance.py'), 'Runtime or evaluation intervention')
        for path, digest in PINS.items():
            require(sha(path) == digest, 'Preserved source identity changed: ' + str(path))

    def phase(self, arms, policies):
        rows = []
        with ThreadPoolExecutor(max_workers=4) as pool:
            futures = []
            for arm in arms:
                for seed in SEEDS:
                    index = ARMS.index(arm) * len(SEEDS) + SEEDS.index(seed)
                    futures.append(pool.submit(self.evaluate, arm, seed, policies[arm], 31501 + index))
            for future in as_completed(futures):
                row = future.result()
                rows.append(row)
                print('FROZEN', row['arm'], row['seed'], row['scores'], flush=True)
        vectors(rows, arms)
        return rows

    def evaluate(self, arm, seed, policy, port):
        memory()
        name = f'{arm}-{seed}'
        directory = OUT / 'evaluations' / name
        execute([sys.executable, ROOT / 'tests/holdout.py', '--policy', policy, '--runtime', RUNTIME,
            '--cache', CACHE, '--output', directory, '--tasks', *TASKS, '--cases', CASES,
            '--seed', seed, '--port', port, '--reset-intervention', 'none'], OUT / (name + '.log'))
        row = verify(directory, policy, arm, seed, port)
        save(OUT / (name + '-receipt.json'), row)
        return row

    def run(self):
        require(os.environ.get('EULA') == 'true', 'Explicit EULA=true required')
        memory()
        managed(OUT).mkdir()
        save(OUT / 'declaration.json', dict(source=self.source, base_source=BASE_SOURCE,
            pins={str(path): digest for path, digest in PINS.items()}, seeds=SEEDS, tasks=TASKS, cases=CASES,
            masks=MASKS, original_arms=['parent', 'candidate'], maximum_reports=10, maximum_frozen_trials=4160,
            maximum_evaluators=4, new_training_samples=0, reset_intervention='none', deployment=False))
        try:
            classes = OUT / 'classes'
            classes.mkdir()
            sources = sorted((ROOT / 'tests/transfer').rglob('*.java'))
            execute([JAVAC, '--release', 21, '-proc:none', '-cp', RUNTIME, '-d', classes,
                *sources, ROOT / 'tests/studies/ActorDriftCheck.java'], OUT / 'compile.log')
            cp = os.pathsep.join(map(str, (classes, RUNTIME)))
            mixtures = OUT / 'counterfactuals'
            execute([JAVA, '-cp', cp, 'org.botsclustersmc.diagnostic.PolicyBlocks',
                PARENT / 'stopped-policy.bcmc', DONOR / 'stopped-policy.bcmc', mixtures], OUT / 'compose.log')
            execute([JAVA, '-cp', cp, 'ActorDriftCheck', mixtures, OUT / 'native-check.json'], OUT / 'native-check.log')
            policies = dict(parent=mixtures / 'base-policy.bcmc', candidate=mixtures / 'donor-policy.bcmc')
            policies.update({arm: mixtures / f'mask-{mask}.bcmc' for arm, mask in MASKS.items()})
            baseline = self.phase(('parent', 'candidate'), policies)
            save(OUT / 'baseline.json', baseline)
            qualified = qualify(baseline)
            rows = baseline + (self.phase(tuple(MASKS), policies) if qualified else [])
            reference = exam_payload(Path(rows[0]['directory']) / 'server/plugins/exam.jar')
            for row in rows:
                require(exam_payload(Path(row['directory']) / 'server/plugins/exam.jar') == reference, 'Evaluator payload drift')
            self.guard()
            save(OUT / 'reports.json', rows)
            result = dict(source=self.source, stage='complete' if qualified else 'input-rejected',
                input_qualified=qualified, reports=len(rows), frozen_trials=sum(row['trials'] for row in rows),
                decision=interpret(rows) if qualified else None, new_training_samples=0, deployment=False)
            save(OUT / 'outcome.json', result)
            print('ACTOR_DRIFT_COMPLETE', json.dumps(result), flush=True)
        except BaseException as error:
            save(OUT / 'failure.json', dict(error=type(error).__name__, detail=str(error),
                traceback=traceback.format_exc(), deployment=False))
            raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true')
    args = parser.parse_args()
    if not args.run:
        parser.print_help()
        return
    require(os.environ.get('EULA') == 'true', 'Explicit EULA=true required before study activity')
    suite = unittest.defaultTestLoader.discover(str(ROOT / 'tests/studies'), pattern='test_actor_drift.py')
    require(suite.countTestCases() >= 12 and unittest.TextTestRunner().run(suite).wasSuccessful(), 'Offline qualification failed')
    Study().run()


if __name__ == '__main__':
    main()
