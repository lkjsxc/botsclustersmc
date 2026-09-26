#!/usr/bin/env python3
"""Opt-in fixed-policy lifecycle study; never reads or copies an Academy/checkpoint."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import time
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = os.environ.get('JAVA_BIN', 'java')


def run(args, **kwargs):
    return subprocess.run([str(arg) for arg in args], check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', type=Path, required=True)
    parser.add_argument('--cache', type=Path, required=True)
    parser.add_argument('--seed', type=int, default=2026092641)
    parser.add_argument('--cases', type=int, default=32, choices=range(1, 65))
    parser.add_argument('--label', default='baseline')
    args = parser.parse_args()
    if os.environ.get('EULA') != 'true':
        raise ValueError('Set EULA=true only after accepting the Minecraft EULA')
    if not args.label or any(c not in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in args.label):
        raise ValueError('Use a lowercase alphanumeric study label')
    bundle = args.bundle.absolute()
    cache = args.cache.absolute()
    for path in (bundle, cache):
        if any(parent.is_symlink() for parent in (path, *path.parents)):
            raise ValueError('Symlinked study input')
    if not bundle.is_file() or bundle.stat().st_size > 16*1024*1024:
        raise ValueError('Use a bounded evaluated ZIP bundle')
    required = {'README.txt','evaluation.json','evaluation-details.json','plugins/botsclustersmc.jar','plugins/BotsClustersMC/policy.bcmc'}
    with zipfile.ZipFile(bundle) as z:
        if len(z.namelist()) != 5 or set(z.namelist()) != required:
            raise ValueError('Unexpected evaluated-bundle entries')
        for info in z.infolist():
            if info.is_dir() or info.file_size > 8*1024*1024:
                raise ValueError('Oversized evaluated entry')
        if z.testzip() is not None:
            raise ValueError('Damaged evaluated ZIP')
        summary = json.loads(z.read('evaluation.json'))
        details = json.loads(z.read('evaluation-details.json'))
        if summary != {k:v for k,v in details.items() if k != 'trials'}:
            raise ValueError('Inconsistent evaluation summaries')
        if not details['complete'] or details['new_training_samples'] != 0:
            raise ValueError('An actual completed fixed-policy evaluation is required')
        model = z.read('plugins/BotsClustersMC/policy.bcmc')
        source_plugin = z.read('plugins/botsclustersmc.jar')
        if hashlib.sha256(model).hexdigest() != details['policy_sha256'] or hashlib.sha256(source_plugin).hexdigest() != details['inference_jar_sha256']:
            raise ValueError('Bundle model/plugin identity mismatch')
    output = ROOT / '.build/evidence' / ('lifecycle-%d-%s' % (args.seed,args.label))
    if output.exists() or any(p.is_symlink() for p in (output,*output.parents)):
        raise ValueError('Study output must be new and not symlinked')
    output.mkdir(parents=True)
    source = subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    dirty = subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()
    if dirty:
        raise ValueError('Commit source before measuring this study')
    with (output/'build.log').open('x') as log:
        run([JAVA,'host/Host.java','build'],cwd=ROOT,env=os.environ|{'BCMC_SERVER_CACHE':str(cache)},stdout=log,stderr=subprocess.STDOUT)
    inference = (ROOT/'dist/botsclustersmc.jar').read_bytes()
    server=output/'server';data=server/'plugins/BotsClustersMC';data.mkdir(parents=True)
    (server/'.botsclustersmc-lifecycle').write_text('owned-disposable-lifecycle\n')
    # Only pinned public runtime libraries are reused. No operator-world or credential copy.
    for name in ('libraries','versions','cache'):
        for src in sorted((cache/name).rglob('*')):
            if src.is_symlink():raise ValueError('Symlink in public library cache')
            dest=server/name/src.relative_to(cache/name)
            if src.is_dir():dest.mkdir(parents=True,exist_ok=True)
            elif src.is_file():
                dest.parent.mkdir(parents=True,exist_ok=True)
                os.link(src,dest)
    (data/'policy.bcmc').write_bytes(model)
    (server/'eula.txt').write_text('eula=true\n')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
    flat=json.dumps({'layers':[{'block':'minecraft:bedrock','height':1},{'block':'minecraft:dirt','height':2},{'block':'minecraft:grass_block','height':1}],'biome':'minecraft:plains'})
    (server/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=%d\nonline-mode=true\nmax-players=0\nwhite-list=true\nenforce-whitelist=true\nlevel-type=minecraft:flat\ngenerator-settings=%s\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\ndifficulty=normal\nallow-flight=true\n'%(port,flat))
    (server/'config').mkdir()
    (server/'config/paper-global.yml').write_text('_version: 31\nthreaded-regions:\n  threads: 2\nchunk-system:\n  worker-threads: 1\n  io-threads: 1\n')
    classes=output/'classes';classes.mkdir()
    libraries=[str(p) for name in ('libraries','versions') for p in sorted((cache/name).rglob('*.jar'))]
    cp=os.pathsep.join([str(ROOT/'dist/training.jar'),*libraries])
    javac=str(Path(JAVA).with_name('javac')) if os.path.sep in JAVA else 'javac'
    run([javac,'--release','21','-encoding','UTF-8','-proc:none','-cp',cp,'-d',classes,*sorted((ROOT/'tests/lifecycle').rglob('*.java'))],cwd=ROOT)
    with zipfile.ZipFile(server/'plugins/lifecycle.jar','x',compression=zipfile.ZIP_DEFLATED) as dest:
        with zipfile.ZipFile(ROOT/'dist/training.jar') as z:
            for item in z.infolist():
                if item.filename.startswith('org/'):dest.writestr(item,z.read(item.filename))
        for file in sorted(classes.rglob('*.class')):dest.write(file,file.relative_to(classes).as_posix())
        dest.writestr('plugin.yml',"name: BotsClustersMC\nversion: 'diagnostic'\nmain: org.botsclustersmc.diagnostic.LifecycleExam\napi-version: '1.21'\nfolia-supported: true\ncommands:\n  bots:\n    description: Fixed-policy lifecycle diagnostics\npermissions:\n  botsclustersmc.observe:\n    default: op\n  botsclustersmc.admin:\n    default: op\n")
        dest.writestr('config.yml','max-agents: 128\nmax-loaded-chunks: 128\ninference-threads: 1\nworld-edits: false\nseed: %d\ncases-per-arm: %d\n'%(args.seed,args.cases))
    metadata={'source':source,'policy_sha256':details['policy_sha256'],'policy_updates':details['policy_updates'],'policy_samples':details['policy_trained_samples'],
              'inference_jar_sha256':hashlib.sha256(inference).hexdigest(),'source_inference_jar_sha256':details['inference_jar_sha256'],
              'same_inference_bytes':inference==source_plugin,'seed':args.seed,'cases_per_arm':args.cases,'order':[11,12,11],'started_epoch_millis':int(time.time()*1000)}
    (output/'manifest.json').write_text(json.dumps(metadata,indent=2))
    process=None
    with (output/'server.log').open('x') as log:
        try:
            process=subprocess.Popen([JAVA,'-Xms256m','-Xmx2G','-Dbcmc.lifecycle=true','-jar',str(cache/'server.jar'),'--nogui'],cwd=server,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
            process.wait(timeout=660)
        finally:
            if process and process.poll() is None:
                process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=60)
            if process and process.stdin:process.stdin.close()
    if process.returncode or (data/'lifecycle-failed.txt').exists():
        raise RuntimeError('Study failed; preserved server.log and scratch data')
    result=json.loads((data/'lifecycle-result.json').read_text())
    assert result['complete'] and result['new_training_samples']==0
    assert result['policy_updates']==metadata['policy_updates'] and result['policy_samples']==metadata['policy_samples']
    assert len(result['trials'])==args.cases*6 and len({(t['actor'],t['phase']) for t in result['trials']})==args.cases*6
    for t in result['trials']:
        assert t['elapsed_ticks']>0 and t['decisions']>0 and t['task']==[11,12,11][t['phase']]
    summary={'manifest':metadata,'counts':[]}
    for arm in ('EXAM','PROBE'):
        for phase in range(3):
            trials=[t for t in result['trials'] if t['arm']==arm and t['phase']==phase]
            assert len(trials)==args.cases
            summary['counts'].append({'arm':arm,'phase':phase,'task':[11,12,11][phase],'passed':sum(t['success'] for t in trials),'cases':len(trials)})
    (output/'summary.json').write_text(json.dumps(summary,indent=2))
    print(json.dumps(summary,indent=2),flush=True)


if __name__=='__main__':main()
