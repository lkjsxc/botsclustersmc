"""Run explicit scripted client tests in a NEW server; never touch learner state.
This is a developer-only Python lifecycle wrapper, not a training controller.
All game input in these diagnostics is sent by a separately built Rust client.
"""
from pathlib import Path
import os, shutil, subprocess, sys, tempfile, time, socket

root=Path.cwd().resolve()
if os.environ.get('EULA')!='true': raise SystemExit('Explicit EULA consent required')
(root/'experiments').mkdir(exist_ok=True)
lab=Path(tempfile.mkdtemp(prefix='fixtures-',dir=root/'experiments'))
print('DIAGNOSTIC DIRECTORY',lab,flush=True)
(lab/'.bcmc-diagnostic-only').write_text('scripted fixture checks; no learned model\n')
(lab/'.botsclustersmc-academy-v2').write_text('botsclustersmc-academy-v2\nbots=64\ncampus=8x16\n')
for name in ['bridge','scripts','pins']:
    shutil.copytree(root/name,lab/name)
if (root/'runtime').exists():shutil.copytree(root/'runtime',lab/'runtime')
for name in ['libraries','versions','cache']:
    if (root/'server'/name).exists():shutil.copytree(root/'server'/name,lab/'server'/name)
(lab/'logs').mkdir(exist_ok=True)
(lab/'.runtime/lab').mkdir(parents=True)
run=f'diagnostic-{time.time_ns()}'
env=dict(os.environ,BCMC_ROOT=str(lab),BCMC_RUN_ID=run,BCMC_CURRICULUM='true',BOTS='64',BOT_PREFIX='bcmc',BIND_ADDRESS='127.0.0.1',OFFLINE_ACCESS_ACK='false',JAVA_HEAP_GB='2',FOLIA_THREADS='2',SERVER_PORT='25566',BCMC_MODE='random',VIEW_DISTANCE='3',SIMULATION_DISTANCE='3',SPECTATOR_VIEW_DISTANCE='12',BOT_VIEW_DISTANCE='3',BOT_SERVER='127.0.0.1:25566',EULA='true')
# Reject a busy port rather than connecting diagnostic actions to another server.
with socket.socket() as test:test.bind(('127.0.0.1',25566))
prep=subprocess.run([sys.argv[2]],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(lab/'logs/prepare.log').write_text(prep.stdout)
if prep.returncode:print(prep.stdout[-12000:]);raise SystemExit(prep.returncode)
paths=dict(line.split('=',1) for line in prep.stdout.splitlines() if line.startswith(('DIAGNOSTIC_JAVA=','DIAGNOSTIC_JAR=')))
log=(lab/'logs/folia.log').open('w')
server=subprocess.Popen([paths['DIAGNOSTIC_JAVA'],'-Xms1G','-Xmx2G','-XX:ActiveProcessorCount=4','-XX:ParallelGCThreads=2','-XX:ConcGCThreads=1','-Dio.netty.eventLoopThreads=2','-Dterminal.jline=false','-Dterminal.ansi=false','-jar',paths['DIAGNOSTIC_JAR'],'--nogui'],cwd=lab/'server',env=env,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
result=1
try:
    until=time.monotonic()+300
    while time.monotonic()<until:
        if server.poll() is not None:raise RuntimeError('Folia exited during fixture initialization')
        ready=lab/'.runtime/lab/bridge.ready'
        if ready.exists() and ready.read_text()==f'BCMCLAB3 {run} ready\n':break
        time.sleep(0.25)
    else:raise RuntimeError('fixture campus readiness timeout')
    for mode in ['fixtures','observer']:
        with (lab/f'logs/{mode}.log').open('w') as output:
            test=subprocess.run([sys.argv[1]],env=dict(env,BCMC_TEST_MODE=mode),cwd=lab,stdout=output,stderr=subprocess.STDOUT,timeout=1200 if mode=='fixtures' else 120)
        text=(lab/f'logs/{mode}.log').read_text(errors='replace')
        print(text[-24000:],flush=True)
        if test.returncode:raise RuntimeError(f'{mode} failed with exit {test.returncode}')
        fatal=lab/'.runtime/lab/fatal.txt'
        if fatal.exists():raise RuntimeError(fatal.read_text())
    assert not (lab/'state/training.bcmc').exists(), 'diagnostic unexpectedly created a learner checkpoint'
    result=0
finally:
    (lab/'.runtime/stop').write_text('stop\n')
    if server.poll() is None:
        try:server.stdin.write('stop\n');server.stdin.flush()
        except BrokenPipeError:pass
    try:server.wait(timeout=180)
    except subprocess.TimeoutExpired:server.kill();server.wait();result=1
    log.close()
    print('DIAGNOSTIC SERVER EXIT',server.returncode,'EVIDENCE',lab,flush=True)
    if result or server.returncode:print((lab/'logs/folia.log').read_text(errors='replace')[-16000:],flush=True)
if server.returncode:raise SystemExit(1)
raise SystemExit(result)
