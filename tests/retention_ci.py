#!/usr/bin/env python3
"""Opt-in, pinned checkpoint continuation in a disposable CI runner. No deployment."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import socket
import subprocess
import time
import zipfile

INPUT_SHA = 'a9e4dc21e676a3af5a69e1d2ff6ff401814ecf026fd4490eb54fe08c3c447693'
PINS = {'control': 'bb0dcfd1d1c5e94f8276e92367187e064145e1a9',
        'candidate': '8a2cc24061090a4be210c5332c32f1c9d848d715'}
SPEC = {'format': 'bcmc-retention-ci-input', 'initial_samples': 184230639,
        'additional_target': 1500000, 'maximum_overshoot': 50000, 'training_seed': 7,
        'cases': 32, 'evaluation_seeds': [2026092822, 2026092823], 'actors': 512,
        'heap_gb': 2, 'threads': [2, 1, 1], 'policy_source': 'stopped-interrupted-experiment'}
MIB = 1024 * 1024


def safe(path: Path) -> None:
    if any(p.is_symlink() for p in (path, *path.parents)):
        raise ValueError('Symlinked experiment path')


def bounded(path: Path, maximum: int = 8 * MIB) -> bytes:
    safe(path)
    with path.open('rb') as stream:
        value = stream.read(maximum + 1)
    if len(value) > maximum:
        raise ValueError('Oversized experiment file')
    return value


def create(path: Path, value: bytes) -> None:
    safe(path)
    with path.open('xb') as stream:
        stream.write(value)
        stream.flush()
        os.fsync(stream.fileno())


def document(path: Path, value: object) -> None:
    create(path, (json.dumps(value, indent=2) + '\n').encode())


def count(value: object) -> int:
    if type(value) is not int or value < 0:
        raise ValueError('Expected nonnegative integer')
    return value


def payload(path: Path, expected: str = INPUT_SHA) -> tuple[dict, dict[str, bytes]]:
    archive = bounded(path, 16 * MIB)
    if hashlib.sha256(archive).hexdigest() != expected:
        raise ValueError('Input archive identity differs')
    import io
    limits = {'manifest.json': 65536, 'control.bcmc': 8*MIB, 'candidate.bcmc': 8*MIB}
    files = {}
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        for info in z.infolist():
            if info.filename not in limits or info.filename in files or info.is_dir():
                raise ValueError('Unexpected or duplicate archive entry')
            limit = limits[info.filename]
            if info.file_size < 0 or info.file_size > limit:
                raise ValueError('Oversized decoded entry')
            with z.open(info) as stream:
                data = stream.read(limit + 1)
            if len(data) != info.file_size or len(data) > limit:
                raise ValueError('Decoded size differs')
            files[info.filename] = data
    if set(files) != set(limits):
        raise ValueError('Missing archive entry')
    manifest = json.loads(files['manifest.json'])
    if set(manifest) != set(SPEC) | {'arms'}:
        raise ValueError('Unexpected manifest fields')
    for key, value in SPEC.items():
        if manifest[key] != value or type(manifest[key]) is not type(value):
            raise ValueError('Declared study specification changed: ' + key)
    if set(manifest['arms']) != set(PINS):
        raise ValueError('Study arms differ')
    for arm, pin in PINS.items():
        meta = manifest['arms'][arm]
        if meta['source'] != pin or meta['checkpoint_sha256'] != hashlib.sha256(files[arm+'.bcmc']).hexdigest():
            raise ValueError('Checkpoint/source identity differs')
        samples = count(meta['samples'])
        count(meta['updates'])
        if not SPEC['initial_samples'] < samples < SPEC['initial_samples'] + SPEC['additional_target']:
            raise ValueError('Input is not an interrupted checkpoint before the target')
        counts = meta['prefix_accepted_task_samples']
        if not isinstance(counts, list) or len(counts) != 19:
            raise ValueError('Prior task shape')
        for value in counts:
            count(value)
        count(meta['prefix_review_ticks']); count(meta['prefix_frontier_ticks'])
    return manifest, files


def memory_guard() -> None:
    values = Path('/proc/meminfo').read_text().splitlines()
    available = next(int(line.split()[1]) for line in values if line.startswith('MemAvailable:'))
    if available < MIB:
        raise RuntimeError('Available memory below the unchanged 1 GiB floor')


def execute(args: list, root: Path, env: dict, log: Path, timeout: int = 900) -> str:
    with log.open('x') as stream:
        result = subprocess.run([str(arg) for arg in args], cwd=root, env=env,
            stdout=stream, stderr=subprocess.STDOUT, timeout=timeout)
    if result.returncode:
        raise RuntimeError('Command failed; see ' + log.name)
    return bounded(log, 4*MIB).decode(errors='replace')


def source_guard(root: Path, arm: str) -> None:
    actual = subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
    dirty = subprocess.check_output(['git','status','--porcelain'],cwd=root,text=True).strip()
    if actual != PINS[arm] or dirty:
        raise RuntimeError('Source is not the clean pinned implementation')


def identity(root: Path, env: dict, checkpoint: Path, evidence: Path, label: str) -> dict:
    text = execute(['java','-Xmx256m','-cp',root/'dist/training.jar',
        'org.botsclustersmc.training.CheckpointTool','export',checkpoint,evidence/(label+'-policy.bcmc')],
        root,env,evidence/(label+'-export.log'),60)
    match = re.search(r'updates=(\d+), trained_samples=(\d+)',text)
    if match is None:
        raise RuntimeError('Native checkpoint validation did not return identity')
    return {'updates':int(match[1]),'samples':int(match[2]),
        'checkpoint_sha256':hashlib.sha256(bounded(checkpoint)).hexdigest()}


def task_counts(status: dict) -> list[int]:
    values = json.loads(status['learned_task_samples_this_process'])
    if len(values) != 19:
        raise ValueError('Observed task shape')
    return [count(value) for value in values]


def workload(prefix: dict, last: dict) -> dict:
    counts = task_counts(last)
    review = count(last['review_ticks_this_process'])
    frontier = count(last['frontier_ticks_this_process'])
    def summary(c, r, f):
        if not sum(c) or not r+f:
            raise ValueError('No observed training work')
        return {'accepted_task_samples':c,'review_ticks':r,'frontier_ticks':f,
                'review_fraction':r/(r+f),'stone_sample_fraction':c[12]/sum(c)}
    return {'ci_segment':summary(counts,review,frontier),
        'combined_final_phase':summary([a+b for a,b in zip(counts,prefix['prefix_accepted_task_samples'])],
            review+prefix['prefix_review_ticks'],frontier+prefix['prefix_frontier_ticks'])}


def run(arm: str, root: Path, archive: Path, out: Path) -> None:
    if os.environ.get('EULA') != 'true':
        raise ValueError('Explicit EULA consent is required')
    manifest, files = payload(archive)
    source_guard(root,arm);safe(out);out.mkdir()
    evidence=out/'evidence';evidence.mkdir()
    academy=out/'academy';data=academy/'server/plugins/BotsClustersMC';data.mkdir(parents=True)
    create(academy/'.botsclustersmc-academy',b'botsclustersmc-owned-training\n')
    create(academy/'.gitignore',b'*\n')
    checkpoint=data/'training.bcmc'
    create(checkpoint,files[arm+'.bcmc']);create(evidence/'input-training.bcmc',files[arm+'.bcmc'])
    document(evidence/'input-manifest.json',manifest)
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
    env=os.environ | {'ACADEMY':str(academy),'BOTS':'512','HEAP_GB':'2','REGION_THREADS':'2',
        'INFERENCE_THREADS':'1','LEARNER_THREADS':'1','SEED':'7','PORT':str(port),
        'BIND_ADDRESS':'127.0.0.1','ONLINE_MODE':'true','EULA':'true',
        'BCMC_SERVER_CACHE':str(out/'cache'),'JAVA_TOOL_OPTIONS':'-XX:ActiveProcessorCount=2'}
    # Credentials belong only to the preceding workflow download step, not to Minecraft.
    env.pop('GH_TOKEN',None);env.pop('GITHUB_TOKEN',None)
    execute(['./build.sh'],root,env,evidence/'build.log')
    initial=identity(root,env,checkpoint,evidence,'initial')
    meta=manifest['arms'][arm]
    if initial['updates']!=meta['updates'] or initial['samples']!=meta['samples']:
        raise RuntimeError('Native input counters differ from the transfer manifest')
    document(evidence/'run.json',{'arm':arm,'source':PINS[arm],'input':initial,
        'input_archive_sha256':INPUT_SHA,'scope':'CI recovery after two shared-host memory aborts',
        'java':subprocess.check_output(['java','-version'],stderr=subprocess.STDOUT,text=True),
        'total_memory':Path('/proc/meminfo').read_text().splitlines()[0],
        'started_epoch_millis':int(time.time()*1000)})
    target=SPEC['initial_samples']+SPEC['additional_target'];last=None
    memory_guard();started=time.monotonic()
    with (evidence/'training.log').open('x') as log, (evidence/'status.jsonl').open('x') as history:
        process=subprocess.Popen(['./start.sh'],cwd=root,env=env,stdin=subprocess.DEVNULL,
            stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        try:
            while True:
                elapsed=time.monotonic()-started
                if elapsed>2400:raise TimeoutError('Original per-phase time bound')
                memory_guard()
                if process.poll() is not None:raise RuntimeError('Training exited before requested stop')
                status=data/'status.json'
                if status.exists():
                    last=json.loads(bounded(status,MIB))
                    if last.get('state')!='running' or time.time()*1000-last['epoch_millis']>45000:
                        raise RuntimeError('Unhealthy or stale experiment')
                    history.write(json.dumps({'elapsed':elapsed,'status':last})+'\n');history.flush()
                    if last['trained_samples']>=target:break
                time.sleep(5)
            execute(['./stop.sh'],root,env,evidence/'stop.log',60)
            process.wait(timeout=120)
            if process.returncode:raise RuntimeError('Unclean training shutdown')
        finally:
            if process.poll() is None:
                try:
                    execute(['./stop.sh'],root,env,evidence/'failure-stop.log',60)
                    process.wait(timeout=120)
                except Exception:
                    os.killpg(process.pid,signal.SIGTERM);process.wait(timeout=30)
            # Retain only complete model state, never control credentials or the world.
            if checkpoint.is_file():create(evidence/'stopped-training.bcmc',bounded(checkpoint))
    stopped=identity(root,env,checkpoint,evidence,'stopped')
    if not target<=stopped['samples']<=target+SPEC['maximum_overshoot']:
        raise RuntimeError('Original sample budget/overshoot bound')
    source_guard(root,arm)
    work=workload(meta,last);document(evidence/'workload.json',work)
    reports=[]
    for index,seed in enumerate(SPEC['evaluation_seeds']):
        memory_guard();label='final' if index==0 else 'confirmation'
        args=['./evaluate.sh','--tasks',','.join(map(str,range(13))),'--cases','32',
              '--seed',str(seed),'--export',evidence/(label+'.zip')]
        if index:args+=['--from',evidence/'final.zip']
        execute(args,root,env,evidence/(label+'.log'))
        report=json.loads(bounded(data/'evaluation.json',MIB))
        if not report['complete'] or report['new_training_samples']!=0 or report['seed']!=seed:
            raise RuntimeError('Invalid fixed-model evaluation')
        if report['policy_updates']!=stopped['updates'] or report['policy_trained_samples']!=stopped['samples']:
            raise RuntimeError('Evaluation did not measure the stopped model')
        if [t['task'] for t in report['tasks']]!=list(range(13)) or any(t['cases']!=32 for t in report['tasks']):
            raise RuntimeError('Task/case coverage differs')
        document(evidence/(label+'-summary.json'),report);reports.append(report)
        print(label,arm,'policy',report['policy_updates'],'scores',[t['passed'] for t in report['tasks']],flush=True)
    if reports[0]['policy_sha256']!=reports[1]['policy_sha256']:
        raise RuntimeError('Confirmation changed model')
    if bounded(checkpoint)!=bounded(evidence/'stopped-training.bcmc'):
        raise RuntimeError('Evaluation modified the checkpoint')
    document(evidence/'completed.json',{'complete':True,'arm':arm,'stopped':stopped,
        'additional_samples':stopped['samples']-SPEC['initial_samples'],
        'elapsed_seconds':time.monotonic()-started,'scope':'One resumed model, not a general mastery certificate'})


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--arm',choices=PINS,required=True)
    parser.add_argument('--source',type=Path,required=True)
    parser.add_argument('--input',type=Path,required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    run(args.arm,args.source.absolute(),args.input.absolute(),args.out.absolute())
