"""Developer-only acceptance of the public source-root entrypoints.
Requires an otherwise unused checkout and explicit disposable-test/EULA consent.
The real neural learner chooses every action; this wrapper only supervises it.
"""
from pathlib import Path
import json, os, shutil, subprocess, time

root=Path.cwd().resolve()
if os.environ.get('EULA')!='true' or os.environ.get('BCMC_TEST_NEW_ACADEMY')!='true':
    raise SystemExit('Read the EULA and explicitly allow a NEW disposable Academy: EULA=true BCMC_TEST_NEW_ACADEMY=true')
for name in ['academy-v2','academy','.env']:
    if (root/name).exists() or (root/name).is_symlink():
        raise SystemExit(f'Refusing a checkout with existing {name}; use a separate clean test checkout')
if not (root/'bin/botsclustersmc-run').is_file():raise SystemExit('Build the actual native application first')
out=Path(os.environ.get('BCMC_EVIDENCE_DIR',root/'.runtime/public-entry-evidence'))
out.mkdir(parents=True,exist_ok=True)
# A sentinel represents legacy data. The v2 launcher must leave it untouched.
old=root/'academy';old.mkdir();(old/'preserve.txt').write_text('old-world-and-policy-sentinel\n')
shutil.copyfile(root/'.env.example',root/'.env')
private=(root/'.env').read_bytes()
env=dict(os.environ,EULA='true',BIND_ADDRESS='127.0.0.1',OFFLINE_ACCESS_ACK='false',JAVA_HEAP_GB='3',FOLIA_THREADS='2',RUN_SECONDS='600',BOTS='64',ROLLOUT_STEPS='64',BATCH_SAMPLES='4096')
for name in ['BCMC_ROOT','BCMC_RUN_ID','BCMC_ACADEMY_LOCK','BCMC_CURRICULUM','BOT_SERVER','BCMC_MODE']:
    env.pop(name,None)
previous=None
for round_number in [1,2]:
    output=out/f'public-round{round_number}.log'
    with output.open('w') as log:
        process=subprocess.Popen(['./start.sh'],cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT)
        ok=False
        try:
            until=time.monotonic()+360
            while time.monotonic()<until:
                if process.poll() is not None:raise RuntimeError(f'public entrypoint exited before acceptance: {process.returncode}')
                try:
                    status=json.loads((root/'academy-v2/state/status.json').read_text())
                except (FileNotFoundError,json.JSONDecodeError):
                    time.sleep(0.5);continue
                agents=status.get('agents',[])
                fresh=previous is None or status['run_id']!=previous['run_id']
                if fresh and status.get('error'):raise RuntimeError(status['error'])
                if fresh and status['version']>=status['initial_version']+2 and len(agents)==64 and all(a['steps']>=32 for a in agents):
                    ok=True;break
                time.sleep(0.5)
            if not ok:raise RuntimeError('public entrypoint did not complete two normal-batch PPO updates')
            if round_number==1:
                # An actual 65th connection uses the production observer UI while
                # all 64 reward-trained actors continue. No test writes a lesson
                # request, world action, model parameter or reward in this run.
                binary=root/'.build/azalea/target/release/examples/bcmc_fixture_check'
                if not binary.is_file():raise RuntimeError('Build the isolated live diagnostic client first')
                initial=status['version']
                observer_env=dict(env,BCMC_ROOT=str(root/'academy-v2'),BCMC_RUN_ID=status['run_id'],
                    BOT_SERVER='127.0.0.1:25565',BCMC_TEST_MODE='observer',BCMC_OBSERVER_REQUIRE_STATUS='true')
                with (out/'public-observer.log').open('w') as observer_log:
                    result=subprocess.run([str(binary)],cwd=root,env=observer_env,stdout=observer_log,stderr=subprocess.STDOUT,timeout=110)
                if result.returncode:raise RuntimeError('simultaneous observer failed: '+(out/'public-observer.log').read_text()[-8000:])
                until=time.monotonic()+100
                while True:
                    if process.poll() is not None:raise RuntimeError('learner exited during observer acceptance')
                    current=json.loads((root/'academy-v2/state/status.json').read_text())
                    if current.get('error'):raise RuntimeError(current['error'])
                    if len(current['agents'])!=64 or any(a['disconnects'] for a in current['agents']):raise RuntimeError('observer disturbed the 64-actor population')
                    if current['version']>initial:break
                    if time.monotonic()>until:raise RuntimeError('no additional real PPO update during/after observer visit')
                    time.sleep(0.5)
                print('PASS: simultaneous observer plus 64 learning actors; real HUD/TAB, menu/tour/view packets; PPO advanced '+str(initial)+' -> '+str(current['version']),flush=True)
            for name,args in [('status',['./status.sh']),('console',['./console.sh','list'])]:
                result=subprocess.run(args,cwd=root,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30)
                (out/f'public-round{round_number}-{name}.txt').write_text(result.stdout)
                if result.returncode:raise RuntimeError(f'public {name} failed: {result.stdout}')
        finally:
            stop=subprocess.run(['./stop.sh'],cwd=root,env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30)
            (out/f'public-round{round_number}-stop.txt').write_text(stop.stdout)
            try:process.wait(timeout=180)
            except subprocess.TimeoutExpired:
                process.terminate()
                try:process.wait(timeout=30)
                except subprocess.TimeoutExpired:process.kill();process.wait()
                raise RuntimeError('public stop did not complete within its bounded grace period')
    text=output.read_text(errors='replace')
    if process.returncode or 'Clean shutdown: model, curriculum and world saved.' not in text:
        print(text[-16000:]);raise RuntimeError(f'public entrypoint did not stop cleanly: {process.returncode}')
    args=['bin/botsclustersmc-run','verify-academy']
    if previous is not None:args.append(str(out/'public-round1-status.json'))
    verified=subprocess.run(args,cwd=root,env=dict(env,BCMC_ROOT=str(root/'academy-v2')),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60)
    (out/f'public-round{round_number}-acceptance.txt').write_text(verified.stdout)
    print(verified.stdout,flush=True)
    if verified.returncode:raise RuntimeError('public run/resume acceptance failed')
    current=json.loads((root/'academy-v2/state/status.json').read_text())
    shutil.copyfile(root/'academy-v2/state/status.json',out/f'public-round{round_number}-status.json')
    shutil.copyfile(root/'academy-v2/state/academy-status.json',out/f'public-round{round_number}-course.json')
    summary={k:v for k,v in current.items() if k!='agents'}
    print('PUBLIC DEFAULT-BATCH RESULT',round_number,json.dumps(summary),flush=True)
    assert (old/'preserve.txt').read_text()=='old-world-and-policy-sentinel\n'
    assert list(old.iterdir())==[old/'preserve.txt']
    assert (root/'.env').read_bytes()==private, 'operator config unexpectedly rewritten'
    previous=current
print('PASS: fresh source-root start, 64 normal-batch learners, status/console/stop, exact restart and legacy/config preservation. No learned-skill claim.',flush=True)
