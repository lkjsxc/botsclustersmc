#!/usr/bin/env python3
"""Opt-in, isolated lifecycle acceptance for the native evaluation command."""
from __future__ import annotations
import argparse, hashlib, json, os, subprocess, time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=os.environ.get('JAVA_BIN','java')

def read(path):
    try:return json.loads(path.read_text())
    except (FileNotFoundError,json.JSONDecodeError):return None

def wait(process,path,predicate,seconds=240):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        value=read(path)
        if value and predicate(value):return value
        if process.poll() is not None:raise AssertionError('Evaluation exited early; inspect its log')
        time.sleep(.25)
    raise AssertionError('Timed out waiting for evaluation status')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--checkpoint',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--cache',type=Path,default=ROOT/'.cache/server')
    args=parser.parse_args()
    if os.environ.get('EULA')!='true':parser.error('Explicit EULA=true consent is required')
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=False)
    academy=output/'academy';data=academy/'server/plugins/BotsClustersMC';data.mkdir(parents=True)
    (academy/'.botsclustersmc-academy').write_text('botsclustersmc-owned-training\n')
    original=args.checkpoint.resolve(strict=True).read_bytes();checkpoint=data/'training.bcmc';checkpoint.write_bytes(original)
    sentinel=academy/'operator-sentinel.txt';sentinel.write_text('Do not change operator data.\n')
    env=os.environ|{'ACADEMY':str(academy),'BCMC_SERVER_CACHE':str(args.cache.resolve()),'EULA':'true'}
    command=[JAVA,'-Xmx256m','host/Host.java','evaluate','--tasks','0','--cases','2','--seed','7183']
    with (output/'watch.log').open('w') as log:
        process=subprocess.Popen([*command,'--watch','--interval','60'],cwd=ROOT,env=env,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
        try:
            wait(process,data/'evaluation-status.json',lambda s:s['state']=='waiting')
            result=read(data/'evaluation.json');details=read(data/'evaluation-details.json')
            assert result['complete'] and result['new_training_samples']==0 and result['cases_per_task']==2
            assert len(details['trials'])==2 and result['tasks'][0]['passed']==sum(t['success'] for t in details['trials'])
            report=(data/'evaluation.json').read_bytes();stamp=(data/'evaluation.json').stat().st_mtime_ns
            finished=time.monotonic()
            second=subprocess.run(command,cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=60)
            (output/'concurrent.log').write_text(second.stdout)
            assert second.returncode!=0 and 'already owns' in second.stdout
            assert (data/'evaluation.json').read_bytes()==report
            time.sleep(max(0,66-(time.monotonic()-finished)))
            assert process.poll() is None and (data/'evaluation.json').stat().st_mtime_ns==stamp
            process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=60)
            assert process.returncode==0 and read(data/'evaluation-status.json')['state']=='stopped'
            assert checkpoint.read_bytes()==original and sentinel.read_text()=='Do not change operator data.\n'
        finally:
            if process.poll() is None:
                process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=90)
    with (output/'active-stop.log').open('w') as log:
        before={p.name for p in (ROOT/'.build').glob('evaluation-*') if p.is_dir()}
        active_command=command.copy();active_command[active_command.index('--tasks')+1]='14'
        cancelled=subprocess.Popen([*active_command,'--watch','--interval','60'],cwd=ROOT,env=env,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
        begun=int(time.time()*1000)
        try:
            wait(cancelled,data/'evaluation-status.json',lambda s:s['state']=='running' and s.get('trials_total')==2 and s['started_epoch_millis']>=begun-1000)
            cancelled.stdin.write('stop\n');cancelled.stdin.flush();cancelled.wait(timeout=90)
            assert cancelled.returncode==0
            assert read(data/'evaluation-status.json')['state']=='stopped'
            assert (data/'evaluation.json').read_bytes()==report
            after={p.name for p in (ROOT/'.build').glob('evaluation-*') if p.is_dir()}
            assert after==before, 'Cancelled evaluation left owned scratch or tools behind'
        finally:
            if cancelled.poll() is None:
                cancelled.stdin.write('stop\n');cancelled.stdin.flush();cancelled.wait(timeout=90)
    consent=subprocess.run(command,cwd=ROOT,env=env|{'EULA':'false'},capture_output=True,text=True,timeout=30)
    assert consent.returncode!=0 and 'EULA' in consent.stderr and 'Built dist/' not in consent.stdout
    invalid=b'Invalid checkpoint in the disposable acceptance fixture.'
    checkpoint.write_bytes(invalid)
    rejected=subprocess.run(command,cwd=ROOT,env=env,capture_output=True,text=True,timeout=60)
    (output/'rejected.log').write_text(rejected.stdout+rejected.stderr)
    assert rejected.returncode!=0 and checkpoint.read_bytes()==invalid
    assert (data/'evaluation.json').read_bytes()==report
    assert read(data/'evaluation-status.json')['state']=='failed'
    checkpoint.write_bytes(original)
    summary={'completed':True,'watch_stop':True,'active_stop':True,'unchanged_policy_skipped':True,'single_writer':True,'canonical_checkpoint_unmodified':True,'invalid_checkpoint_rejected':True,'consent_required':True,'source_copy_sha256':hashlib.sha256(original).hexdigest()}
    (output/'acceptance.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS native evaluation lifecycle, frozen result accounting, single writer, unchanged-model skip, graceful stop, consent and fail-closed checkpoint handling')

if __name__=='__main__':main()
