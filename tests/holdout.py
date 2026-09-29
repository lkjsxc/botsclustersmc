#!/usr/bin/env python3
"""Run a disposable frozen neural-policy exam; never update policy or course state."""
from __future__ import annotations
import argparse, hashlib, json, math, os, shutil, socket, subprocess, zipfile
from pathlib import Path
import acceptance

ROOT = Path(__file__).resolve().parents[1]
RESET_INTERVENTIONS = ('none', 'workbench-open', 'pickaxe-grid',
    'pickaxe-missing-top-left', 'pickaxe-missing-top-center', 'pickaxe-missing-top-right',
    'pickaxe-missing-handle-upper', 'pickaxe-missing-handle-lower')

def arguments(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    source=parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--policy', type=Path)
    source.add_argument('--checkpoint', type=Path, help='Freeze and export one canonical training checkpoint without stopping live learning.')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, default=ROOT/'dist/training.jar')
    parser.add_argument('--cache', type=Path, default=ROOT/'.cache/server')
    parser.add_argument('--tasks', type=int, nargs='+', default=[0, 1])
    parser.add_argument('--cases', type=int, default=64)
    parser.add_argument('--seed', type=int, default=19517)
    parser.add_argument('--port', type=int, default=25584)
    parser.add_argument('--reset-intervention', choices=RESET_INTERVENTIONS, default='none',
                        help='Diagnostic only: modify the initial workbench state; never a standard skill evaluation.')
    args = parser.parse_args(argv)
    if args.reset_intervention != 'none' and (args.checkpoint or any(t not in (11, 13) for t in args.tasks)):
        parser.error('Reset diagnostics require one saved --policy and only pickaxe tasks 11 or 13.')
    if not 1 <= args.cases <= 256 or args.cases*len(args.tasks) > 2048:
        parser.error('Use 1..256 cases per task, with at most 2048 trials total.')
    if len(set(args.tasks)) != len(args.tasks) or any(t < 0 or t > 17 for t in args.tasks):
        parser.error('Task IDs must be distinct and in 0..17.')
    if not -(2**63) <= args.seed < 2**63 or not 1024 <= args.port <= 65535 or args.port == 25565:
        parser.error('Use a signed 64-bit seed and a non-production port in 1024..65535.')
    if os.environ.get('EULA') != 'true':
        parser.error('Read and personally accept the Minecraft EULA before setting EULA=true.')
    return args

def prepare(args):
    policy, runtime, cache = (p.resolve(strict=True) for p in (args.checkpoint or args.policy, args.runtime, args.cache))
    output = args.output.absolute()
    for parent in (output, *output.parents):
        if parent.is_symlink():
            raise ValueError('The output must not use a symbolic link.')
    output = output.resolve()
    if output == ROOT or ROOT/'academy' in output.parents or output == ROOT/'academy':
        raise ValueError('Use a new disposable output, not the checkout or its live academy.')
    if output == policy.parent or policy.parent in output.parents:
        raise ValueError('Do not place the exam inside the source policy directory.')
    if not policy.is_file() or not runtime.is_file() or not (cache/'server.jar').is_file():
        raise ValueError('A readable policy, built training runtime and prepared server cache are required.')
    with socket.socket() as check:
        check.bind(('127.0.0.1', args.port))
    output.mkdir(parents=True, exist_ok=False)
    server = output/'server'
    acceptance.server_dir(server, cache, args.port)
    props = server/'server.properties'
    lines = [line for line in props.read_text().splitlines() if not line.startswith(('generator-settings=', 'level-type='))]
    flat = {'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:dirt', 'height': 2}, {'block': 'minecraft:grass_block', 'height': 1}], 'biome': 'minecraft:plains'}
    lines += ['level-type=minecraft:flat', 'generator-settings='+json.dumps(flat), 'max-players=0', 'white-list=true', 'enforce-whitelist=true']
    props.write_text('\n'.join(lines)+'\n')
    (server/'.botsclustersmc-exam').write_text('Disposable frozen-policy exam only.\n')
    data = server/'plugins/BotsClustersMC'; data.mkdir()
    if policy.stat().st_size > 32*1024*1024:
        raise ValueError('The source policy/checkpoint exceeds the evaluation size bound.')
    shutil.copy2(runtime, output/'runtime.jar')
    if args.checkpoint:
        checkpoint=output/'source-training.bcmc';checkpoint.write_bytes(policy.read_bytes())
        java=os.environ.get('JAVA_BIN', 'java')
        with (output/'export.log').open('w') as log:
            subprocess.run([java,'-cp',str(output/'runtime.jar'),'org.botsclustersmc.training.CheckpointTool',
                'export',str(checkpoint),str(data/'policy.bcmc')],check=True,stdout=log,stderr=subprocess.STDOUT)
        frozen=(data/'policy.bcmc').read_bytes()
    else:
        frozen=policy.read_bytes();(data/'policy.bcmc').write_bytes(frozen)
    return output, server, data, cache, frozen

def entry(jar, name, data):
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    jar.writestr(info, data)

def compile_exam(args, output, server, cache):
    java = os.environ.get('JAVA_BIN', 'java')
    javac = str(Path(java).with_name('javac')) if os.path.sep in java else 'javac'
    classes = output/'classes'; classes.mkdir()
    libraries = [str(p) for name in ('libraries', 'versions') for p in (cache/name).rglob('*.jar')]
    cp = os.pathsep.join([str(output/'runtime.jar'), *libraries])
    sources = sorted((ROOT/'tests/holdout').rglob('*.java'))
    with (output/'compile.log').open('w') as log:
        subprocess.run([javac, '--release', '21', '-proc:none', '-cp', cp, '-d', str(classes), *map(str, sources)], check=True, stdout=log, stderr=subprocess.STDOUT)
    descriptor = '''name: BotsClustersMC
version: '0.7.0'
main: org.botsclustersmc.holdout.FrozenPolicyExam
api-version: '1.21'
folia-supported: true
commands:
  bots:
    description: Independent frozen policy experiment
permissions:
  botsclustersmc.observe:
    default: op
  botsclustersmc.admin:
    default: op
'''
    count = args.cases*len(args.tasks)
    config = f'max-agents: {max(64, count)}\nmax-loaded-chunks: {max(64, count)}\ninference-threads: 1\nseed: {args.seed}\ncases-per-task: {args.cases}\ntasks: {args.tasks}\nworld-edits: false\n'
    if args.reset_intervention != 'none':
        config += f'reset-intervention: {args.reset_intervention}\n'
    with zipfile.ZipFile(server/'plugins/exam.jar', 'w', compression=zipfile.ZIP_DEFLATED) as jar:
        with zipfile.ZipFile(output/'runtime.jar') as source:
            for item in source.infolist():
                if item.filename.startswith('org/'):
                    entry(jar, item.filename, source.read(item.filename))
        for path in sorted(classes.rglob('*.class')):
            entry(jar, path.relative_to(classes).as_posix(), path.read_bytes())
        entry(jar, 'plugin.yml', descriptor); entry(jar, 'config.yml', config)

def require(condition, message):
    """Evidence checks must remain active under python -O / PYTHONOPTIMIZE."""
    if not condition:
        raise AssertionError(message)


def integer(value, maximum, label, minimum=0):
    require(type(value) is int and minimum <= value <= maximum, label)
    return value


def number(value, maximum, label):
    require(type(value) in (int, float) and 0 <= value <= maximum and math.isfinite(value), label)
    return value


def read_bounded(path, maximum):
    with path.open('rb') as stream:
        value = stream.read(maximum + 1)
    require(len(value) <= maximum, 'Oversized evidence file: ' + path.name)
    return value


def read_report(path):
    def pairs(entries):
        result = {}
        for key, value in entries:
            require(key not in result, 'Duplicate JSON field: ' + key)
            result[key] = value
        return result
    def nonfinite(value):
        raise AssertionError('Non-finite JSON value: ' + value)
    def finite(value):
        parsed = float(value)
        require(math.isfinite(parsed), 'Non-finite JSON number')
        return parsed
    try:
        return json.loads(read_bounded(path, 64*1024*1024).decode('utf-8'),
                          object_pairs_hook=pairs, parse_constant=nonfinite, parse_float=finite)
    except (ValueError, RecursionError) as failure:
        raise AssertionError('Malformed evaluation JSON') from failure


def verify_table_trace(trace, observations):
    """Validate denominators and bounded measured sums, not learned competence."""
    require(type(trace) is dict and trace.get('scope') == 'table-inventory-pre-action', 'Table trace scope')
    def count(key, maximum):
        return integer(trace.get(key), maximum, key)
    def vector(key, length, maximum, integral=True):
        values = trace.get(key)
        require(type(values) is list and len(values) == length, key)
        for value in values:
            (integer if integral else number)(value, maximum, key)
        return values
    def mass(key, denominator):
        number(trace.get(key), denominator + 1e-7, key)
    integer(observations, 2**63-1, 'observations', 1)
    require(count('transitions', observations) == observations, 'Table transitions')
    inventory = count('inventory_states', observations)
    patterns = vector('correct_mask_states', 16, inventory)
    require(sum(patterns) == inventory, 'Table mask denominator')
    maximum = count('max_correct_cells', 4)
    require(all(not n or mask.bit_count() <= maximum for mask, n in enumerate(patterns)), 'Table maximum')
    count('max_surplus_units', 252)
    count('partial_inventory_exits', inventory)
    count('carried_planks_below_four_without_table_states', inventory)
    opportunities = vector('compatible_cursor_states_by_correct_cells', 5, inventory)
    require(opportunities[4] == 0, 'Completed grid is not a fill opportunity')
    fill = vector('fill_probability_sum_by_correct_cells', 5, inventory + 1e-7, False)
    single = vector('single_unit_fill_probability_sum_by_correct_cells', 5, inventory + 1e-7, False)
    for cells in range(5):
        require(opportunities[cells] <= sum(n for mask, n in enumerate(patterns) if mask.bit_count() == cells), 'Fill denominator')
        require(single[cells] <= fill[cells] + 1e-7 and fill[cells] <= opportunities[cells] + 1e-7, 'Fill probability mass')
    vector('filled_cell_transitions', 4, inventory)
    vector('removed_cell_transitions', 4, inventory)
    previews = 0
    for kind in ('target', 'other'):
        denominator = count(kind + '_preview_states', inventory)
        previews += denominator
        mass(kind + '_collection_probability_sum', denominator)
        count('chosen_' + kind + '_result_clicks', denominator)
    require(previews <= inventory, 'Preview denominator')
    gains = count('observed_stick_gain_transitions', inventory)
    units = count('observed_stick_units_gained', 64 * gains)
    require(units >= gains, 'Stick units gained')
    count('observed_table_units_gained', 64 * inventory)


def verify_report(args, result):
    """Bind each trial to its declared task/case/Java-long seed, not just totals."""
    require(type(result) is dict, 'Evaluation object')
    cases = integer(args.cases, 256, 'Declared cases', 1)
    require(type(args.tasks) is list and 1 <= len(args.tasks) <= 18, 'Declared tasks')
    for task in args.tasks:
        integer(task, 17, 'Declared task')
    require(len(set(args.tasks)) == len(args.tasks), 'Duplicate declared task')
    integer(args.seed, 2**63-1, 'Declared seed', -2**63)
    total = cases * len(args.tasks)
    require(total <= 2048, 'Trial limit')
    require(args.reset_intervention in RESET_INTERVENTIONS, 'Declared intervention')
    diagnostic = args.reset_intervention != 'none'
    require(not diagnostic or all(task in (11, 13) for task in args.tasks), 'Diagnostic task scope')
    require(result.get('complete') is True and result.get('stochastic') is True, 'Complete stochastic evaluation required')
    integer(result.get('new_training_samples'), 0, 'Holdout must not train')
    require(integer(result.get('seed'), 2**63-1, 'Report seed', -2**63) == args.seed, 'Report seed differs')
    require(integer(result.get('cases_per_task'), 256, 'Report cases', 1) == cases, 'Case count differs')
    require(result.get('diagnostic_only', False) is diagnostic, 'Diagnostic/control classification')
    require(result.get('reset_intervention', 'none') == args.reset_intervention, 'Intervention differs')
    coverage = result.get('reset_intervention_trials', 0)
    require(integer(coverage, total, 'Intervention coverage') == (total if diagnostic else 0), 'Intervention coverage differs')
    trials, summaries = result.get('trials'), result.get('tasks')
    require(type(trials) is list and len(trials) == total, 'Missing or excess trials')
    require(type(summaries) is list and len(summaries) == len(args.tasks), 'Task coverage')
    seen, passed = set(), {task: 0 for task in args.tasks}
    for trial in trials:
        require(type(trial) is dict, 'Trial object')
        actor = integer(trial.get('actor'), total-1, 'Actor identity')
        require(actor not in seen, 'Duplicate actor'); seen.add(actor)
        task = integer(trial.get('task'), 17, 'Trial task')
        require(task == args.tasks[actor // cases], 'Actor/task assignment differs')
        expected_seed = args.seed + task*1000003 + (actor % cases)*104729
        expected_seed = (expected_seed + 2**63) % 2**64 - 2**63
        require(integer(trial.get('seed'), 2**63-1, 'Trial seed', -2**63) == expected_seed, 'Trial seed differs')
        require(type(trial.get('success')) is bool, 'Trial success must be boolean')
        passed[task] += trial['success']
        integer(trial.get('elapsed_ticks'), 2**63-1, 'Elapsed ticks', 1)
        number(trial.get('distance'), 1e308, 'Final distance')
        detail = trial.get('diagnostics')
        require(type(detail) is dict, 'Trial diagnostics')
        observations = integer(detail.get('observations'), 2**63-1, 'Observations', 1)
        integer(detail.get('dig_decisions'), observations, 'Dig decisions')
        for key in ('observed_max_target_mining_ticks', 'blocks_broken', 'items_collected'):
            integer(detail.get(key), 2**63-1, key)
        for key in ('mean_abs_yaw_error', 'mean_abs_pitch_error'):
            number(detail.get(key), 180, key)
        if task == 10:
            verify_table_trace(detail.get('table_crafting'), observations)
        else:
            require('table_crafting' not in detail, 'Table diagnostics belong only to task 10.')
    for task, summary in zip(args.tasks, summaries):
        require(type(summary) is dict, 'Task summary object')
        require(integer(summary.get('task'), 17, 'Summary task') == task, 'Task order differs')
        require(integer(summary.get('cases'), 256, 'Summary cases', 1) == cases, 'Task denominator differs')
        require(integer(summary.get('passed'), cases, 'Summary passed') == passed[task], 'Success total differs')
    return result


def verify_result(args, data, frozen, process):
    require(type(process.returncode) is int and process.returncode == 0, 'Exam process did not exit successfully')
    failure = data/'exam-failed.txt'
    require(not failure.exists(), 'Exam runtime reported a failure: ' + str(failure))
    result = verify_report(args, read_report(data/'exam-result.json'))
    require(read_bounded(data/'policy.bcmc', len(frozen)) == frozen, 'Frozen policy changed')
    require(not list(data.glob('training*.bcmc')), 'A holdout exam must never create a training checkpoint.')
    return result

def main():
    args = arguments(); output, server, data, cache, frozen = prepare(args)
    compile_exam(args, output, server, cache)
    metadata = {
        'policy_sha256': hashlib.sha256(frozen).hexdigest(),
        'runtime_jar_sha256': hashlib.sha256((output/'runtime.jar').read_bytes()).hexdigest(),
        'exam_jar_sha256': hashlib.sha256((server/'plugins/exam.jar').read_bytes()).hexdigest(),
        'tasks': args.tasks, 'cases_per_task': args.cases, 'seed': args.seed,
        'bind': '127.0.0.1', 'port': args.port, 'terrain': 'academy-flat',
        'diagnostic_only': args.reset_intervention != 'none',
        'reset_intervention': args.reset_intervention,
        'claim': ('Assisted reset diagnostic only; not standard competence or a certificate.' if args.reset_intervention != 'none'
                  else 'Fixed-policy full-difficulty stochastic trials; no learning or certificate mutation.'),
    }
    metadata['input_kind']='canonical-checkpoint' if args.checkpoint else 'inference-policy'
    if args.checkpoint:
        metadata['checkpoint_sha256']=hashlib.sha256((output/'source-training.bcmc').read_bytes()).hexdigest()
    (output/'metadata.json').write_text(json.dumps(metadata, indent=2)+'\n')
    print('Running independent holdout:', output, flush=True)
    process = acceptance.direct(server, cache, output/'server.log', '-Dbcmc.holdout=true')
    try:
        process.wait(timeout=300+args.cases*len(args.tasks)//4)
        if args.checkpoint:
            require(hashlib.sha256(read_bounded(output/'source-training.bcmc', 32*1024*1024)).hexdigest() == metadata['checkpoint_sha256'], 'Frozen source checkpoint changed')
        result = verify_result(args, data, frozen, process)
        (output/'result.json').write_text(json.dumps(result, indent=2)+'\n')
        print(json.dumps({k: v for k, v in result.items() if k != 'trials'}, indent=2), flush=True)
        print('PASS evaluation integrity: every trial completed, weights unchanged, zero new training samples.', flush=True)
        print('Diagnostic reset results are NOT full-condition skill results.' if args.reset_intervention != 'none'
              else 'Exit success means the experiment completed, not that every skill passed.', flush=True)
    finally:
        acceptance.stop_direct(process)

if __name__ == '__main__':
    main()
