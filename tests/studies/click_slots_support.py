"""Always-on evidence boundaries for the fixed click-conditioned-slot experiment."""
from pathlib import Path
from types import SimpleNamespace
import hashlib, json, os, signal, subprocess, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tests'))
import holdout as H

CONTROL = ROOT.parent/'botsclustersmc-protected-continuation'
OLD_INPUT = CONTROL/'.build/focus-input'
INPUT = ROOT/'.build/slot-input'
OUT = ROOT/'.build/click-slots-study'
CACHE = ROOT.parent/'botsclustersmc/.cache/server'
ARMS = ('control', 'candidate')
SEEDS = (2026092991, 2026092992)
TASKS = list(range(12))
CELLS = ('top-left', 'top-center', 'top-right', 'handle-upper', 'handle-lower')
CONDITIONS = ('none', *('pickaxe-missing-'+cell for cell in CELLS))
BASE = 92574669
TARGETS = (250000, 1500000)
OVERSHOOT = 50000
CONTROL_SOURCE = 'e1f8e6567116c02e0af18e64a466f7e0289e5a14'
CONTROL_RUNTIME = '49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9'
CONTROL_INPUT = 'dcd2e73084869ea275642739db13c7e00501677133c5c50697b842eac5ea90b4'
ENV = dict(os.environ, EULA='true', JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
for key in ('GH_TOKEN', 'GITHUB_TOKEN'):
    ENV.pop(key, None)
require = H.require


def managed(path):
    path = Path(path).absolute()
    require(not any(p.is_symlink() for p in (path, *path.parents)), 'Symlinked study path')
    return path


def read(path, maximum=32*1024*1024):
    return H.read_bounded(managed(path), maximum)


def load(path):
    return H.read_report(managed(path))


def sha(path):
    return hashlib.sha256(read(path)).hexdigest()


def write(path, data):
    with managed(path).open('xb') as stream:
        stream.write(data); stream.flush(); os.fsync(stream.fileno())


def save(path, value):
    write(path, (json.dumps(value, indent=2, allow_nan=False)+'\n').encode())


def execute(command, log, cwd=ROOT, env=ENV, timeout=180):
    with managed(log).open('x') as output:
        process = subprocess.Popen(list(map(str, command)), cwd=cwd, env=env,
            stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
            require(code == 0, 'Command failed; complete log retained: '+str(log))
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=45)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL); process.wait(timeout=15)
    return read(log).decode('utf-8', errors='replace')


def memory():
    lines = Path('/proc/meminfo').read_text().splitlines()
    available = next(int(line.split()[1])*1024 for line in lines if line.startswith('MemAvailable:'))
    maximum = Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum != 'max':
        available = min(available, int(maximum)-int(Path('/sys/fs/cgroup/memory.current').read_text()))
    require(available >= 2*1024**3, 'Below 2 GiB available-memory floor')
    return available


def task_counts(value):
    require(type(value) is str and len(value) <= 512, 'Bounded task accounting string')
    counts = json.loads(value)
    require(type(counts) is list and len(counts) == 19, '18 tasks plus the unlabelled bucket required')
    for index, number in enumerate(counts):
        H.integer(number, 2**63-1, 'Task sample counter')
        require(index == 11 or number == 0, 'Accepted sample outside task 11')
    return counts


def training_samples(status, initial, started_epoch, now_epoch, previous=None):
    """Validate one process-local snapshot without confusing old disk state with this run."""
    require(type(status) is dict, 'Training status object')
    stamp = H.integer(status.get('epoch_millis'), 2**63-1, 'Status epoch')
    if stamp < started_epoch:
        require(previous is None, 'Status returned to pre-start history')
        return None
    require(status.get('state') == 'running' and -5000 <= now_epoch-stamp <= 45000, 'Stale/unhealthy learner')
    require(type(status.get('learning_task_scope')) is int and status['learning_task_scope'] == 11
        and status.get('protected_prior_policy') is True, 'Wrong learning scope')
    for key in ('inference_failed', 'inference_rejected', 'learner_rejected_samples', 'learner_stale_samples', 'retired_agents'):
        H.integer(status.get(key), 0, key)
    counts = task_counts(status.get('learned_task_samples_this_process'))
    samples = H.integer(status.get('trained_samples'), 2**63-1, 'Accepted training samples', initial)
    H.integer(status.get('active_agents'), 512, 'Active actors')
    if previous is not None:
        require(stamp >= previous['epoch_millis'] and samples >= previous['trained_samples'], 'Training clock/counter went backwards')
        require(counts[11] >= task_counts(previous['learned_task_samples_this_process'])[11], 'Task counter went backwards')
    return samples


def budget(samples, target):
    require(type(target) is int and target in TARGETS, 'Undeclared sample boundary')
    H.integer(samples, BASE+target+OVERSHOOT, 'Accepted sample budget', BASE+target)


def spec(seed, condition):
    require(seed in SEEDS and type(seed) is int and condition in CONDITIONS, 'Declared evaluation condition')
    return SimpleNamespace(seed=seed, reset_intervention=condition,
        tasks=TASKS.copy() if condition == 'none' else [11], cases=64 if condition == 'none' else 32)


def verify_evaluation(directory, policy, runtime, seed, condition, port):
    args = spec(seed, condition)
    report = H.verify_report(args, load(directory/'result.json'))
    metadata = load(directory/'metadata.json')
    require(metadata.get('policy_sha256') == sha(policy), 'Evaluation policy identity')
    require(metadata.get('runtime_jar_sha256') == sha(runtime) == sha(directory/'runtime.jar'), 'Evaluation runtime identity')
    require(metadata.get('exam_jar_sha256') == sha(directory/'server/plugins/exam.jar'), 'Evaluation exam identity')
    tasks = metadata.get('tasks')
    require(type(tasks) is list and all(type(task) is int for task in tasks) and tasks == args.tasks, 'Metadata task binding')
    require(H.integer(metadata.get('seed'), 2**63-1, 'Metadata seed', -2**63) == seed
        and H.integer(metadata.get('cases_per_task'), 256, 'Metadata cases', 1) == args.cases, 'Metadata seed/case binding')
    require(metadata.get('reset_intervention') == condition
        and metadata.get('diagnostic_only') is (condition != 'none'), 'Separate assisted classification')
    require(metadata.get('bind') == '127.0.0.1' and H.integer(metadata.get('port'), 65535, 'Metadata port', 1024) == port
        and metadata.get('input_kind') == 'inference-policy', 'Isolated frozen evaluation')
    data = directory/'server/plugins/BotsClustersMC'
    require(read(data/'policy.bcmc') == read(policy), 'Evaluation changed weights')
    require(not (data/'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')), 'Evaluation failure or learning')
    return [row['passed'] for row in report['tasks']]


def vectors(rows):
    require(type(rows) is list, 'Complete report collection required')
    expected = {(arm, seed, condition) for arm in ARMS for seed in SEEDS for condition in CONDITIONS}
    result = {}
    for row in rows:
        key = row['arm'], row['seed'], row['condition']
        require(key in expected and key not in result, 'Unexpected or duplicate arm/seed/condition')
        args = spec(row['seed'], row['condition'])
        scores = row['scores']
        require(type(scores) is list and len(scores) == len(args.tasks), 'Ordered complete task scores')
        for count in scores:
            H.integer(count, args.cases, 'Success count')
        require(H.integer(row['trials'], 2048, 'Trial count', 1) == args.cases*len(args.tasks), 'Incomplete trial denominator')
        H.integer(row['new_training_samples'], 0, 'Frozen evaluations only')
        result[key] = scores
    require(set(result) == expected, 'Missing report condition')
    return result


def gate(baseline, current):
    before, after = vectors(baseline), vectors(current)
    details = []
    for seed in SEEDS:
        source = before['control', seed, 'none']
        control, candidate = (after[arm, seed, 'none'] for arm in ARMS)
        partial_source = [before['control', seed, c][0] for c in CONDITIONS[1:]]
        partial_control = [after['control', seed, c][0] for c in CONDITIONS[1:]]
        partial_candidate = [after['candidate', seed, c][0] for c in CONDITIONS[1:]]
        retained = all(a >= b-4 for scores in (control, candidate) for a, b in zip(scores[:11], source[:11]))
        candidate_partial = all(a >= b-4 for a, b in zip(partial_candidate, partial_source))
        control_partial = all(a >= b-4 for a, b in zip(partial_control, partial_source))
        details.append(dict(seed=seed, source=source, control=control, candidate=candidate,
            source_cells=partial_source, control_cells=partial_control, candidate_cells=partial_candidate,
            other_tasks_retained=retained, candidate_partial_retained=candidate_partial,
            control_partial_retained=control_partial,
            acquisition=candidate[11] >= 8 and candidate[11] >= control[11]+4,
            assisted_gain=min(partial_candidate) >= 4 and sum(partial_candidate) >= sum(partial_control)+32))
    return dict(retained=all(d['other_tasks_retained'] and d['candidate_partial_retained'] for d in details),
        acquisition=all(d['acquisition'] for d in details), assisted_gain=all(d['assisted_gain'] for d in details), details=details)
