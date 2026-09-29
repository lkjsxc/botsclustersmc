"""Evidence boundaries for the predeclared, warm station-entry learning comparison."""
from pathlib import Path
from types import SimpleNamespace
import hashlib, json, os, signal, subprocess, sys
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tests'))
import holdout as H
require = H.require
CONTROL = ROOT.parent/'botsclustersmc-station-entry-control'
MAIN = ROOT.parent/'botsclustersmc'
CACHE = MAIN/'.cache/server'
OUT = ROOT/'.build/station-entry-study'
INPUT = MAIN/'.build/frontier-snapshot-20260930/source-training.bcmc'
POLICY = MAIN/'.build/frontier-snapshot-20260930/server/plugins/BotsClustersMC/policy.bcmc'
INPUT_SHA = '294684242df33f8547406fe561a91be2b1fb0eff7c682e96dbb3038ce32280a1'
POLICY_SHA = '2c9297f28855c081ed0516933f01fdc6406f57b0409794cecbaffebc3cf34720'
CONTROL_SOURCE = '63fcde679bd3188716193787aa11098eccd3df0e'
CONTROL_RUNTIME = '7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc'
BASE = 297530150
TARGETS = (500000, 2000000)
OVERSHOOT = 50000
SEEDS = (2026093011, 2026093012)
ARMS = ('control', 'candidate')
CONDITIONS = ('none', 'workbench-open')
ENV = dict(os.environ, JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
for key in ('GH_TOKEN', 'GITHUB_TOKEN'): ENV.pop(key, None)


def managed(path):
    path = Path(path).absolute()
    require(not any(p.is_symlink() for p in (path, *path.parents)), 'Symlinked study path')
    return path


def read(path, maximum=32*1024*1024): return H.read_bounded(managed(path), maximum)
def load(path): return H.read_report(managed(path))
def sha(path): return hashlib.sha256(read(path)).hexdigest()

def write(path, data):
    with managed(path).open('xb') as stream:
        stream.write(data); stream.flush(); os.fsync(stream.fileno())


def save(path, data): write(path, (json.dumps(data, indent=2, allow_nan=False)+'\n').encode())
def git(root, *args): return subprocess.check_output(['git', *args], cwd=root, text=True).strip()


def execute(command, log, cwd=ROOT, env=ENV, timeout=180):
    with managed(log).open('x') as output:
        process = subprocess.Popen(list(map(str, command)), cwd=cwd, env=env, stdin=subprocess.DEVNULL,
            stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            require(process.wait(timeout=timeout) == 0, 'Command failed; retained log: '+str(log))
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try: process.wait(timeout=45)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait(timeout=15)
    return read(log).decode('utf-8', errors='replace')


def memory():
    available = next(int(line.split()[1])*1024 for line in Path('/proc/meminfo').read_text().splitlines() if line.startswith('MemAvailable:'))
    maximum = Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum != 'max': available = min(available, int(maximum)-int(Path('/sys/fs/cgroup/memory.current').read_text()))
    require(available >= 2*1024**3, 'Available memory below 2 GiB')


def spec(seed, condition):
    require(type(seed) is int and seed in SEEDS and type(condition) is str and condition in CONDITIONS, 'Declared seed and condition')
    return SimpleNamespace(seed=seed, reset_intervention=condition, tasks=list(range(12)) if condition == 'none' else [11], cases=32)


def budget(samples, target):
    require(type(target) is int and target in TARGETS, 'Declared accepted-sample boundary')
    H.integer(samples, BASE+target+OVERSHOOT, 'Stopped accepted-sample budget', BASE+target)


def task_counts(value):
    require(type(value) is str and len(value) <= 512, 'Bounded task counters')
    values = json.loads(value)
    require(type(values) is list and len(values) == 19, '18 tasks plus unlabelled bucket')
    for n in values: H.integer(n, 2**63-1, 'Accepted task samples')
    return values


def training_samples(status, initial, epoch, now, previous=None):
    require(type(status) is dict, 'Status object')
    stamp = H.integer(status.get('epoch_millis'), 2**63-1, 'Status epoch')
    if stamp < epoch:
        require(previous is None, 'Returned to old status'); return None
    require(status.get('state') == 'running' and status.get('schema') == 'bcmc-citizen-egocentric-context'
        and -5000 <= now-stamp <= 45000, 'Wrong schema or stale/unhealthy learner')
    for key in ('inference_failed','inference_rejected','learner_rejected_samples','learner_stale_samples','retired_agents'):
        H.integer(status.get(key), 0, key)
    samples = H.integer(status.get('trained_samples'), 2**63-1, 'Accepted samples', initial)
    H.integer(status.get('active_agents'), 512, 'Active actors')
    counts = task_counts(status.get('learned_task_samples_this_process'))
    if previous is not None:
        require(stamp >= previous['epoch_millis'] and samples >= previous['trained_samples'], 'Counters/clock regressed')
        require(all(a >= b for a,b in zip(counts, task_counts(previous['learned_task_samples_this_process']))), 'Task counters regressed')
    return samples


def verify_evaluation(directory, policy, seed, condition, port):
    args = spec(seed, condition)
    report = H.verify_report(args, load(directory/'result.json')); metadata = load(directory/'metadata.json')
    require(metadata.get('policy_sha256') == sha(policy), 'Frozen policy binding')
    require(metadata.get('runtime_jar_sha256') == sha(directory/'runtime.jar') == CONTROL_RUNTIME, 'All evaluation uses unchanged control runtime')
    require(metadata.get('exam_jar_sha256') == sha(directory/'server/plugins/exam.jar'), 'Exam binding')
    tasks = metadata.get('tasks')
    require(type(tasks) is list and all(type(t) is int for t in tasks) and tasks == args.tasks, 'Ordered task binding')
    require(H.integer(metadata.get('seed'), 2**63-1, 'seed', -2**63) == seed and H.integer(metadata.get('cases_per_task'),256,'cases',1) == 32, 'Seed/cases')
    require(metadata.get('reset_intervention') == condition and metadata.get('diagnostic_only') is (condition != 'none'), 'Separate assistance')
    require(metadata.get('bind') == '127.0.0.1' and H.integer(metadata.get('port'),65535,'port',1024) == port
        and metadata.get('input_kind') == 'inference-policy', 'Isolated immutable input')
    data = directory/'server/plugins/BotsClustersMC'
    require(read(data/'policy.bcmc') == read(policy), 'No evaluated weight changed')
    require(not (data/'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')), 'No failure or evaluation learning')
    return [row['passed'] for row in report['tasks']]


def vectors(rows, arms):
    require(type(rows) is list, 'Complete report collection')
    expected = {(a,s,c) for a in arms for s in SEEDS for c in CONDITIONS}; result = {}
    for row in rows:
        require(type(row) is dict and type(row.get('arm')) is str, 'Report identity')
        args = spec(row.get('seed'), row.get('condition')); key = row['arm'],row['seed'],row['condition']
        require(key in expected and key not in result, 'Unexpected or duplicate report')
        scores = row.get('scores'); require(type(scores) is list and len(scores) == len(args.tasks), 'Ordered scores')
        for n in scores: H.integer(n, 32, 'Success count')
        require(H.integer(row.get('trials'),2048,'trial count',1) == 32*len(args.tasks), 'Complete trials')
        H.integer(row.get('new_training_samples'),0,'Frozen evaluations')
        result[key] = scores
    require(set(result) == expected, 'Missing seed/condition/arm')
    return result


def qualify_baseline(rows):
    base = vectors(rows, ('parent',))
    return all(min(base['parent',s,'none'][:11]) >= 28 and base['parent',s,'workbench-open'][0] >= 28 for s in SEEDS)


def gate(baseline, current):
    base = vectors(baseline, ('parent',)); actual = vectors(current, ARMS); details = []
    for seed in SEEDS:
        retained = {}
        for arm in ARMS:
            scores = actual[arm,seed,'none']; opened = actual[arm,seed,'workbench-open'][0]
            retained[arm] = all(a >= max(28,b-3) for a,b in zip(scores[:11],base['parent',seed,'none'][:11]))
            retained[arm] &= scores[11] >= max(0,base['parent',seed,'none'][11]-2)
            retained[arm] &= opened >= max(26,base['parent',seed,'workbench-open'][0]-4)
        candidate = actual['candidate',seed,'none'][11]; control = actual['control',seed,'none'][11]
        details.append(dict(seed=seed, retained=retained, candidate_successes=candidate, control_successes=control,
            ordinary_gain=candidate >= 8 and candidate >= control+4))
    return dict(retained=all(all(row['retained'].values()) for row in details),
        acquisition=all(row['ordinary_gain'] for row in details), details=details)
