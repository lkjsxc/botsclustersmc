#!/usr/bin/env python3
"""Opt-in, disposable real-server tests. Python is a developer dependency only."""
from __future__ import annotations
import argparse, json, os, re, shutil, subprocess, time, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = os.environ.get('JAVA_BIN', 'java')
FLAT = json.dumps({'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:stone', 'height': 127}, {'block': 'minecraft:grass_block', 'height': 1}], 'biome': 'minecraft:plains'})


def run(command, **kwargs):
    print('+', *map(str, command), flush=True)
    return subprocess.run(list(map(str, command)), check=True, **kwargs)


def host(command, env, *args):
    return run([JAVA, 'host/Host.java', command, *args], cwd=ROOT, env=env)


def read_status(path):
    try:
        return json.loads(path.read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def wait_status(process, path, predicate, started, seconds=240):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise AssertionError(f'server exited early: {process.returncode}')
        status = read_status(path)
        if status and status.get('epoch_millis', 0) >= started:
            if status['state'] == 'failed':
                raise AssertionError(f'runtime failure: {status}')
            if predicate(status):
                return status
        time.sleep(.5)
    raise AssertionError(f'status timeout: {read_status(path)}')


def stop_direct(process):
    if process.poll() is None:
        try:
            process.stdin.write('stop\n'); process.stdin.flush()
            process.wait(timeout=60)
        except (OSError, subprocess.TimeoutExpired):
            process.kill(); process.wait(timeout=10)
            raise


def copy_libraries(cache, directory):
    for name in ('libraries', 'versions', 'cache'):
        source = cache / name
        if source.exists():
            # No symlinks: the launcher intentionally rejects them for owned paths.
            def copy(source, target):
                try: os.link(source, target)
                except OSError: shutil.copy2(source, target)
                return target
            shutil.copytree(source, directory / name, copy_function=copy, dirs_exist_ok=True)


def server_dir(directory, cache, port):
    directory.mkdir(parents=True, exist_ok=True)
    copy_libraries(cache, directory)
    (directory/'plugins').mkdir(exist_ok=True)
    (directory/'config').mkdir(exist_ok=True)
    (directory/'eula.txt').write_text('eula=true\n')
    (directory/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=true\nlevel-type=minecraft:flat\ngenerator-settings={FLAT}\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\ndifficulty=normal\n')
    (directory/'config/paper-global.yml').write_text('_version: 31\nthreaded-regions:\n  threads: 2\nchunk-system:\n  worker-threads: 1\n  io-threads: 1\n')


def direct(directory, cache, log, flag=None):
    command = [JAVA, '-Xms256m', '-Xmx2G']
    if flag: command += [flag]
    command += ['-jar', str(cache/'server.jar'), '--nogui']
    return subprocess.Popen(command, cwd=directory, stdin=subprocess.PIPE, stdout=open(log,'w'), stderr=subprocess.STDOUT, text=True)


def probe_policy_partition(status):
    """Validate one atomically published production-plugin snapshot, including zero counts."""
    assert status.get('probe_policy_scope') == 'completed-probes-applied-behavior-versions-this-process'
    keys=('single_policy_trials','single_policy_successes','mixed_policy_trials','mixed_policy_successes',
          'behavior_decisions','policy_changes','maximum_policy_span','last_minimum_policy','last_maximum_policy',
          'trials_this_process','successes_this_process')
    arrays=[json.loads(status['probe_'+key]) for key in keys]
    assert all(isinstance(a,list) and len(a)==18 for a in arrays)
    for single,win_single,mixed,win_mixed,decisions,changes,span,lo,hi,total,wins in zip(*arrays):
        assert all(type(n) is int and n>=0 for n in (single,win_single,mixed,win_mixed,decisions,changes,span,total,wins))
        assert total==single+mixed and wins==win_single+win_mixed
        assert win_single<=single and win_mixed<=mixed and decisions>=total
        assert mixed<=changes<=decisions-total and ((mixed==0)==(span==0))
        if total==0:assert (decisions,changes,span,lo,hi)==(0,0,0,-1,-1)
        else:assert type(lo) is int and type(hi) is int and 0<=lo<=hi and hi-lo<=span


def train(output, count, seconds):
    academy = output/'academy'
    env = os.environ | {'EULA':'true', 'ACADEMY':str(academy), 'BOTS':str(count), 'HEAP_GB':'3', 'REGION_THREADS':'2', 'LEARNER_THREADS':'1', 'INFERENCE_THREADS':'1', 'PORT':'25578', 'BIND_ADDRESS':'127.0.0.1'}
    status_path = academy/'server/plugins/BotsClustersMC/status.json'
    previous = None
    for phase in ('fresh', 'resume'):
        restore_expected = previous
        started = int(time.time()*1000)
        process = subprocess.Popen([JAVA, 'host/Host.java', 'start'], cwd=ROOT, env=env, stdin=subprocess.DEVNULL, stdout=open(output/f'train-{phase}.log','w'), stderr=subprocess.STDOUT)
        try:
            first = wait_status(process,status_path,lambda s:s['active_agents']==count and s['ticking_agents']==count and s['progressed_agents_since_status']==count and s['trained_samples']>(previous['trained_samples'] if previous else 0),started)
            probe_policy_partition(first)
            (output/f'{phase}-start.json').write_text(json.dumps(first,indent=2))
            samples = [first]
            end = time.monotonic() + (seconds if phase=='fresh' else 12)
            while time.monotonic()<end:
                current = wait_status(process,status_path,lambda s:s['epoch_millis']>samples[-1]['epoch_millis'],started,seconds=30)
                assert current['active_agents']==count and current['ticking_agents']==count, current
                assert current['progressed_agents_since_status']==count, current
                assert current['inference_failed']==0 and current['retired_agents']==0, current
                probe_policy_partition(current)
                samples.append(current)
            last = samples[-1]
            assert last['trained_samples']>first['trained_samples'] and last['policy_updates']>first['policy_updates']
            (output/f'{phase}-series.json').write_text(json.dumps(samples,indent=2))
            host('status',env); host('console',env,'bots','status'); host('stop',env)
            process.wait(timeout=75); assert process.returncode==0
            previous=read_status(status_path)
            assert previous and previous['state'] != 'failed', previous
            probe_policy_partition(previous)
            (output/f'{phase}-stopped.json').write_text(json.dumps(previous,indent=2))
            assert not (status_path.parent/'policy.bcmc').exists(), 'training must save one canonical checkpoint, not a second policy copy'
            if phase=='resume':
                restored = re.search(r'Restored exact optimizer/model: updates=(\d+), samples=(\d+), optimizer-step=(\d+)', (output/'train-resume.log').read_text(errors='replace'))
                assert restored, 'missing exact resume receipt'
                assert tuple(map(int,restored.groups())) == (restore_expected['policy_updates'], restore_expected['trained_samples'], restore_expected['policy_updates']), (restored.groups(),restore_expected)
                (output/'resume-identity.json').write_text(json.dumps({'expected_updates':restore_expected['policy_updates'],'expected_samples':restore_expected['trained_samples'],'restored':list(map(int,restored.groups()))},indent=2))
        finally:
            if process.poll() is None:
                try: host('stop',env); process.wait(timeout=60)
                except Exception: process.kill(); process.wait()
    # A leftover/corrupt derived file must not become the export authority.
    obsolete=status_path.parent/'policy.bcmc';obsolete.write_bytes(b'obsolete-derived-policy-is-not-a-checkpoint\n')
    host('export',env,str(output/'deploy'))
    run([JAVA,'-cp',ROOT/'dist/training.jar','org.botsclustersmc.training.CheckpointTool','verify-export',status_path.parent/'training.bcmc',output/'deploy/plugins/BotsClustersMC/policy.bcmc'])
    assert obsolete.read_bytes()==b'obsolete-derived-policy-is-not-a-checkpoint\n'
    obsolete.unlink()
    elapsed=(last['epoch_millis']-first['epoch_millis'])/1000
    print(f'PASS real training + exact-state resume + export: {count} actual NPCs; final phase trained/s={(last["trained_samples"]-first["trained_samples"])/elapsed:.2f}',flush=True)


def fixtures(output, cache):
    directory=output/'fixtures';server_dir(directory,cache,25579)
    classes=output/'fixture-classes';classes.mkdir()
    libraries=[str(p) for name in ('libraries','versions') for p in (cache/name).rglob('*.jar')]
    cp=os.pathsep.join([str(ROOT/'dist/training.jar'),*libraries])
    javac=str(Path(JAVA).with_name('javac')) if os.path.sep in JAVA else 'javac'
    run([javac,'--release','21','-proc:none','-cp',cp,'-d',str(classes),*sorted((ROOT/'tests/live').rglob('*.java'))])
    with zipfile.ZipFile(directory/'plugins/fixtures.jar','w',compression=zipfile.ZIP_DEFLATED) as dest:
        with zipfile.ZipFile(ROOT/'dist/training.jar') as source:
            for item in source.infolist():
                if item.filename.startswith('org/'): dest.writestr(item,source.read(item.filename))
        for path in classes.rglob('*.class'):dest.write(path,path.relative_to(classes).as_posix())
        for path in (ROOT/'tests/live-resources').iterdir():dest.write(path,path.name)
    process=direct(directory,cache,output/'fixtures.log','-Dbcmc.fixtures=true')
    try:
        deadline=time.monotonic()+240
        while time.monotonic()<deadline and process.poll() is None:
            failure=directory/'plugins/BotsClustersMC/fixtures-failed.txt'
            if failure.exists():raise AssertionError(failure.read_text())
            time.sleep(.5)
        process.wait(timeout=10)
        assert process.returncode==0
        assert (directory/'plugins/BotsClustersMC/fixtures-passed.txt').is_file()
        log=(output/'fixtures.log').read_text(errors='replace')
        assert log.count('FIXTURE PASS ')==18, log[-4000:]
        status=read_status(directory/'plugins/BotsClustersMC/status.json')
        assert status['trained_samples']==0 and status['inference_completed']==0
        print('PASS 18 full-difficulty scripted real-world fixtures; zero learner/inference samples',flush=True)
    finally:stop_direct(process)


def inference(output, cache, deploy, count=64, seconds=45):
    directory=output/'inference';server_dir(directory,cache,25580)
    data=directory/'plugins/BotsClustersMC';data.mkdir()
    shutil.copy2(deploy/'plugins/botsclustersmc.jar',directory/'plugins/botsclustersmc.jar')
    shutil.copy2(deploy/'plugins/BotsClustersMC/policy.bcmc',data/'policy.bcmc')
    (data/'config.yml').write_text(f'count: {count}\nmax-agents: {max(128,count)}\nmax-loaded-chunks: {max(128,count)}\ninference-threads: 1\nworld: world\norigin: {{x: 4.5, y: 65, z: 4.5}}\ngoal: {{x: 4.5, y: 65, z: 60.5}}\ntask: 0\nspacing: 2\nworld-edits: false\n')
    sentinel=directory/'operator-sentinel.txt';sentinel.write_bytes(b'operator-owned settings remain unchanged\n')
    started=int(time.time()*1000);process=direct(directory,cache,output/'inference.log')
    def send(text):process.stdin.write(text+'\n');process.stdin.flush()
    try:
        first=wait_status(process,data/'status.json',lambda s:s['active_agents']==count and s['progressed_agents_since_status']==count,started)
        series=[first];deadline=time.monotonic()+seconds
        while time.monotonic()<deadline:
            last=wait_status(process,data/'status.json',lambda s:s['epoch_millis']>series[-1]['epoch_millis'],started,30)
            assert last['active_agents']==count and last['ticking_agents']==count and last['progressed_agents_since_status']==count, last
            assert last['inference_failed']==0 and last['retired_agents']==0, last
            series.append(last)
        last=series[-1]
        assert last['decision_transitions']>first['decision_transitions']+count*16, last
        (output/'inference-series.json').write_text(json.dumps(series,indent=2))
        assert last['moved_agents']>0, last
        assert last['trained_samples']==first['trained_samples'] and last['policy_updates']==first['policy_updates']
        (output/'inference-active.json').write_text(json.dumps(last,indent=2))
        send('bots pause');wait_status(process,data/'status.json',lambda s:s['state']=='paused',started,30)
        send('bots resume');wait_status(process,data/'status.json',lambda s:s['state']=='running',started,30)
        # Rapid goal replacement invalidates request tickets without letting old replies
        # overwrite a current result. Check EVERY actor resumes useful decisions.
        for index in range(24):
            send(f'bots goal all 3 {4.5 + index % 2 * 8} 65 60.5')
            if index % 4 == 0:send('bots pause');send('bots resume')
            time.sleep(.03)
        boundary=int(time.time()*1000)
        healthy=wait_status(process,data/'status.json',lambda s:s['active_agents']==count and s['progressed_agents_since_status']==count,boundary,45)
        assert healthy['inference_failed']==0 and healthy['retired_agents']==0, healthy
        (output/'inference-after-replacement.json').write_text(json.dumps(healthy,indent=2))
        send('bots remove all');wait_status(process,data/'status.json',lambda s:s['active_agents']==0 and s['pending_agents']==0 and s['leased_chunks']==0,started,30)
        respawn=min(64,count);send(f'bots spawn {respawn} world 260.5 65 260.5')
        again=wait_status(process,data/'status.json',lambda s:s['active_agents']==respawn and s['progressed_agents_since_status']==respawn,started,60)
        assert again['state']=='running'
        send('bots remove all');wait_status(process,data/'status.json',lambda s:s['active_agents']==0 and s['leased_chunks']==0,started,30)
        assert sentinel.read_bytes()==b'operator-owned settings remain unchanged\n'
    finally:stop_direct(process)
    policy=data/'policy.bcmc';valid=policy.read_bytes();bad=bytearray(valid);bad[20]^=1;policy.write_bytes(bad)
    process=direct(directory,cache,output/'inference-corrupt-model.log')
    try:
        deadline=time.monotonic()+120
        while time.monotonic()<deadline:
            log=(output/'inference-corrupt-model.log').read_text(errors='replace')
            if 'Runtime stopped accepting work; no fallback policy' in log and 'Done (' in log:break
            if process.poll() is not None:raise AssertionError('invalid plugin model must not shut down the operator server')
            time.sleep(.5)
        else:raise AssertionError('corrupt model did not fail closed')
        assert 'Ready: in-JVM NPC inference' not in log and 'Disabling BotsClustersMC' in log
        assert policy.read_bytes()==bad and not (data/'training.bcmc').exists()
    finally:stop_direct(process);policy.write_bytes(valid)
    print(f'PASS plugin + policy only; {count} actual NPCs, pause/resume, rapid goal replacement, ticket release, respawn, corrupt-model fail-closed without server shutdown',flush=True)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('mode',choices=('train','fixtures','inference','all'));parser.add_argument('--output',type=Path,required=True);parser.add_argument('--count',type=int,default=1024);parser.add_argument('--seconds',type=int,default=45);parser.add_argument('--cache',type=Path,default=Path(os.environ.get('BCMC_SERVER_CACHE',ROOT/'.cache/server')));parser.add_argument('--deploy',type=Path);parser.add_argument('--inference-count',type=int,default=64);parser.add_argument('--inference-seconds',type=int,default=45)
    args=parser.parse_args()
    if not 1<=args.count<=10000 or not 1<=args.inference_count<=10000 or args.seconds<1 or args.inference_seconds<5:parser.error('counts must be 1..10000, training duration positive and inference duration at least 5 seconds')
    if os.environ.get('EULA')!='true':raise SystemExit('Explicit EULA=true is required for disposable real-server acceptance.')
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=False)
    cache=args.cache.resolve();assert (cache/'server.jar').is_file(), cache
    if args.mode in ('train','all'):train(output,args.count,args.seconds)
    if args.mode in ('fixtures','all'):fixtures(output,cache)
    if args.mode in ('inference','all'):inference(output,cache,args.deploy.resolve() if args.deploy else output/'deploy',args.inference_count,args.inference_seconds)
    print('PASS acceptance mode='+args.mode,flush=True)
if __name__=='__main__':main()
