#!/usr/bin/env python3
"""Run a disposable frozen neural-policy exam; never update policy or course state."""
from __future__ import annotations
import argparse, hashlib, json, os, shutil, socket, subprocess, zipfile
from pathlib import Path
import acceptance

ROOT = Path(__file__).resolve().parents[1]

def arguments():
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
    args = parser.parse_args()
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
    with zipfile.ZipFile(server/'plugins/exam.jar', 'w', compression=zipfile.ZIP_DEFLATED) as jar:
        with zipfile.ZipFile(output/'runtime.jar') as source:
            for item in source.infolist():
                if item.filename.startswith('org/'):
                    entry(jar, item.filename, source.read(item.filename))
        for path in sorted(classes.rglob('*.class')):
            entry(jar, path.relative_to(classes).as_posix(), path.read_bytes())
        entry(jar, 'plugin.yml', descriptor); entry(jar, 'config.yml', config)

def verify_result(args, data, frozen, process):
    failure = data/'exam-failed.txt'
    if failure.exists():
        raise AssertionError(failure.read_text())
    result = json.loads((data/'exam-result.json').read_text())
    assert process.returncode == 0 and result['complete'] and result['new_training_samples'] == 0
    trials, total = result['trials'], args.cases*len(args.tasks)
    assert result['stochastic'] and result['seed'] == args.seed and result['cases_per_task'] == args.cases
    assert len(trials) == total and {trial['actor'] for trial in trials} == set(range(total))
    assert [task['task'] for task in result['tasks']] == args.tasks
    for summary in result['tasks']:
        selected = [trial for trial in trials if trial['task'] == summary['task']]
        assert len(selected) == args.cases and summary['cases'] == args.cases
        assert all(type(trial['success']) is bool for trial in selected)
        assert sum(trial['success'] for trial in selected) == summary['passed']
    for trial in trials:
        detail=trial['diagnostics']
        assert detail['observations'] > 0 and 0 <= detail['dig_decisions'] <= detail['observations']
        assert detail['observed_max_target_mining_ticks'] >= 0
        assert 0 <= detail['mean_abs_yaw_error'] <= 180 and 0 <= detail['mean_abs_pitch_error'] <= 180
        assert detail['blocks_broken'] >= 0 and detail['items_collected'] >= 0
    assert (data/'policy.bcmc').read_bytes() == frozen
    assert not list(data.glob('training*.bcmc')), 'A holdout exam must never create a training checkpoint.'
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
        'claim': 'Fixed-policy full-difficulty stochastic trials; no learning or certificate mutation.',
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
            assert hashlib.sha256((output/'source-training.bcmc').read_bytes()).hexdigest()==metadata['checkpoint_sha256']
        result = verify_result(args, data, frozen, process)
        (output/'result.json').write_text(json.dumps(result, indent=2)+'\n')
        print(json.dumps({k: v for k, v in result.items() if k != 'trials'}, indent=2), flush=True)
        print('PASS evaluation integrity: every trial completed, weights unchanged, zero new training samples.', flush=True)
        print('Exit success means the experiment completed, not that every skill passed.', flush=True)
    finally:
        acceptance.stop_direct(process)

if __name__ == '__main__':
    main()
