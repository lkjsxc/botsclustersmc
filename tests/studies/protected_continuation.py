#!/usr/bin/env python3
"""Finite relative-retention continuation study; does not reopen the stopped qualification pilot."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib, json, os, re, signal, subprocess, time, traceback

ROOT=Path(__file__).resolve().parents[2]
INPUT=ROOT/'.build/focus-input'
OUT=ROOT/'.build/continuation-study'
CACHE=ROOT.parent/'botsclustersmc/.cache/server'
RUNTIME=ROOT/'dist/training.jar'
SOURCE=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
RUNTIME_SHA='49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9'
BASE_SAMPLES=92574669
SEEDS=[2026092961,2026092962]
CASES=128
RELATIVE_MARGIN=8
GROWTH_FLOOR=16
TASKS=list(range(12))
ARMS=['control','protected']
ENV=dict(os.environ,EULA='true',JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
for key in ('GH_TOKEN','GITHUB_TOKEN'): ENV.pop(key,None)


def read(p,maximum=32*1024*1024):
    if any(q.is_symlink() for q in (p,*p.parents)): raise ValueError('symlinked study path')
    with p.open('rb') as f: data=f.read(maximum+1)
    if len(data)>maximum: raise ValueError('bounded file too large')
    return data

def sha(p): return hashlib.sha256(read(p)).hexdigest()
def load(p): return json.loads(read(p))
def write(p,data):
    if any(q.is_symlink() for q in (p,*p.parents)): raise ValueError('symlinked output')
    with p.open('xb') as f: f.write(data);f.flush();os.fsync(f.fileno())
def save(p,data): write(p,(json.dumps(data,indent=2,allow_nan=False)+'\n').encode())
def execute(command,log,env=ENV,timeout=180):
    with log.open('x') as f:
        result=subprocess.run(list(map(str,command)),cwd=ROOT,env=env,stdout=f,stderr=subprocess.STDOUT,timeout=timeout)
    if result.returncode: raise RuntimeError(f'Command failed ({result.returncode}); retained {log}')
    return read(log).decode(errors='replace')
def guard():
    head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    dirty=subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()
    if head!=SOURCE or dirty or sha(RUNTIME)!=RUNTIME_SHA: raise RuntimeError('source or runtime changed during study')
def memory():
    lines=Path('/proc/meminfo').read_text().splitlines()
    available=next(int(line.split()[1])*1024 for line in lines if line.startswith('MemAvailable:'))
    maximum=Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum!='max': available=min(available,int(maximum)-int(Path('/sys/fs/cgroup/memory.current').read_text()))
    if available<2*1024**3: raise RuntimeError('below 2 GiB available-memory floor')
    return available

def identity(checkpoint,out,label,initial=False,arm=None):
    if arm not in ARMS: raise ValueError('explicit audit arm required')
    audit=execute(['java','-Xmx256m','-cp',RUNTIME,ROOT/'tests/studies/FocusAudit.java',INPUT,checkpoint,'initial' if initial else 'continued',arm],out/(label+'-audit.log'))
    text=execute(['java','-Xmx256m','-cp',RUNTIME,'org.botsclustersmc.training.CheckpointTool','export',checkpoint,out/(label+'-policy.bcmc')],out/(label+'-export.log'))
    match=re.search(r'updates=(\d+), trained_samples=(\d+)',text)
    if not match: raise RuntimeError('missing native export counters')
    record={'updates':int(match[1]),'samples':int(match[2]),'checkpoint_sha256':sha(checkpoint),
            'policy_sha256':sha(out/(label+'-policy.bcmc')),'audit':audit.strip()}
    save(out/(label+'-identity.json'),record)
    return record

def evaluate(phase):
    directory=OUT/('evaluation-'+phase);directory.mkdir()
    models={'base':INPUT/'base-policy.bcmc','protected':INPUT/'protected-policy.bcmc'} if phase=='baseline' else {
        arm:OUT/arm/('250000' if phase=='early' else '1500000')/'stopped-policy.bcmc' for arm in ARMS}
    jobs=[(arm,path,seed) for arm,path in models.items() for seed in SEEDS]
    def one(index,job):
        arm,path,seed=job;memory();guard();name=f'{arm}-{seed}';output=directory/name
        command=['python3','tests/holdout.py','--policy',path,'--output',output,'--runtime',RUNTIME,'--cache',CACHE,
                 '--tasks',*map(str,TASKS),'--cases',str(CASES),'--seed',str(seed),'--port',str(30860+index)]
        save(directory/(name+'-started.json'),{'command':list(map(str,command)),'epoch':time.time(),'policy':sha(path),'runtime':sha(RUNTIME)})
        text=execute(command,directory/(name+'.log'),timeout=960)
        report=load(output/'result.json');meta=load(output/'metadata.json')
        assert report['complete'] and report['new_training_samples']==0 and report['cases_per_task']==CASES and report['seed']==seed
        assert meta['reset_intervention']=='none' and meta['policy_sha256']==sha(path) and meta['runtime_jar_sha256']==RUNTIME_SHA
        assert [t['task'] for t in report['tasks']]==TASKS and len(report['trials'])==CASES*len(TASKS)
        assert {t['actor'] for t in report['trials']}==set(range(CASES*len(TASKS)))
        scores=[]
        for task in TASKS:
            trials=[t for t in report['trials'] if t['task']==task]
            assert len(trials)==CASES and all(type(t['success']) is bool and t['diagnostics']['observations']>0 for t in trials)
            count=sum(t['success'] for t in trials);assert count==report['tasks'][task]['passed'];scores.append(count)
        assert sha(output/'server/plugins/BotsClustersMC/policy.bcmc')==sha(path)
        assert not list((output/'server/plugins/BotsClustersMC').glob('training*.bcmc'))
        receipt={'arm':arm,'seed':seed,'scores':scores,'policy_sha256':sha(path),'report_sha256':sha(output/'result.json'),
                 'path':str(output/'result.json'),'trials':CASES*len(TASKS),'new_training_samples':0}
        save(directory/(name+'-receipt.json'),receipt);print('EVALUATED',phase,arm,seed,scores,flush=True)
        return receipt
    with ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(one,i,job) for i,job in enumerate(jobs)]
        reports=[f.result() for f in futures]
    guard();save(directory/'completed.json',reports);return reports

def relative_retention(parent,protected):
    """A prespecified descriptive screen, not mastery or a statistical noninferiority test."""
    if len(parent)!=len(TASKS) or len(protected)!=len(TASKS):
        raise ValueError('complete ordered task vectors required')
    if any(type(n) is not int or not 0<=n<=CASES for n in [*parent,*protected]):
        raise ValueError('invalid success count')
    return all(after>=before-RELATIVE_MARGIN for before,after in zip(parent,protected))

def vectors(rows,arms):
    expected={(arm,seed) for arm in arms for seed in SEEDS}
    result={}
    for row in rows:
        key=(row['arm'],row['seed'])
        if key not in expected or key in result: raise ValueError('unexpected/duplicate model-seed')
        relative_retention(row['scores'],row['scores'])
        if row['trials']!=CASES*len(TASKS) or row['new_training_samples']!=0:
            raise ValueError('incomplete or training evaluation')
        result[key]=row['scores']
    if set(result)!=expected: raise ValueError('missing model-seed')
    return result

def gate(phase):
    base=vectors(load(OUT/'evaluation-baseline/completed.json'),['base','protected'])
    rows=vectors(load(OUT/('evaluation-'+phase)/'completed.json'),['base','protected'] if phase=='baseline' else ARMS)
    results=[]
    for seed in SEEDS:
        parent=base['base',seed]
        protected=rows['protected',seed]
        retained=relative_retention(parent,protected)
        results.append({'seed':seed,'parent':parent,'protected':protected,'retained':retained})
    save(OUT/(phase+'-gate.json'),results)
    if not all(r['retained'] for r in results):
        save(OUT/'rejected.json',{'phase':phase,'reason':'prespecified relative retention gate; not an absolute qualification claim','results':results});return False
    return True

def train(arm,target):
    guard();memory();out=OUT/arm;out.mkdir(exist_ok=True);evidence=out/str(target);evidence.mkdir()
    academy=out/'academy';data=academy/'server/plugins/BotsClustersMC';checkpoint=data/'training.bcmc'
    if target==250000:
        data.mkdir(parents=True);write(academy/'.botsclustersmc-academy',b'botsclustersmc-owned-training\n');write(academy/'.gitignore',b'*\n')
        write(checkpoint,read(INPUT/(arm+'-training.bcmc')))
        assert sha(checkpoint)==sha(INPUT/(arm+'-training.bcmc'))
    else:
        assert target==1500000 and (out/'250000/completed.json').is_file()
        assert sha(checkpoint)==sha(out/'250000/stopped-training.bcmc')
    initial=identity(checkpoint,evidence,'start',target==250000,arm)
    env=dict(ENV,ACADEMY=str(academy),BOTS='512',HEAP_GB='2',REGION_THREADS='2',INFERENCE_THREADS='1',LEARNER_THREADS='1',
             SEED='7',PORT='30851' if arm=='control' else '30852',BIND_ADDRESS='127.0.0.1',ONLINE_MODE='true',BCMC_SERVER_CACHE=str(CACHE))
    threshold=BASE_SAMPLES+target;epoch=int(time.time()*1000);started=time.monotonic();first=None;last=None;last_print=0
    with (evidence/'training.log').open('x') as log,(evidence/'status.jsonl').open('x') as history:
        process=subprocess.Popen(['./start.sh'],cwd=ROOT,env=env,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        try:
            while True:
                if time.monotonic()-started>1800: raise TimeoutError('declared 30-minute training-segment cap')
                memory()
                if process.poll() is not None: raise RuntimeError('learner exited before sample boundary')
                if (data/'status.json').is_file():
                    status=load(data/'status.json')
                    if status.get('epoch_millis',0)>=epoch:
                        if status.get('state')!='running' or time.time()*1000-status['epoch_millis']>45000: raise RuntimeError('unhealthy or stale learner')
                        assert status['learning_task_scope']==11 and status['protected_prior_policy']==(arm=='protected')
                        if any(status.get(k,0) for k in ('inference_failed','inference_rejected','learner_rejected_samples','learner_stale_samples','retired_agents')):
                            raise RuntimeError('runtime error counters')
                        counts=json.loads(status['learned_task_samples_this_process']);assert all(n==0 for i,n in enumerate(counts) if i!=11)
                        if last is None or status['epoch_millis']!=last['epoch_millis']:
                            last=status;history.write(json.dumps(status)+'\n');history.flush()
                            if first is None and status['active_agents']==512:
                                first=status;save(evidence/'first-active.json',first)
                            if time.monotonic()-last_print>30:
                                print('TRAIN',arm,target,status['trained_samples']-BASE_SAMPLES,'scope_skipped',status['learner_scope_skipped_samples'],flush=True);last_print=time.monotonic()
                        if status['trained_samples']>=threshold:break
                time.sleep(2)
            execute(['./stop.sh'],evidence/'stop.log',env,90);process.wait(timeout=120)
            if process.returncode:raise RuntimeError('unclean learner stop')
        finally:
            if process.poll() is None:
                try:execute(['./stop.sh'],evidence/'failure-stop.log',env,90);process.wait(timeout=120)
                except Exception:
                    os.killpg(process.pid,signal.SIGTERM);process.wait(timeout=40)
            if checkpoint.is_file():write(evidence/'stopped-training.bcmc',read(checkpoint))
    stopped=identity(evidence/'stopped-training.bcmc',evidence,'stopped',False,arm);guard()
    assert threshold<=stopped['samples']<=threshold+50000 and first is not None and last is not None
    save(evidence/'last-observed.json',last)
    receipt={'arm':arm,'target':target,'additional_samples':stopped['samples']-BASE_SAMPLES,'stopped':stopped,
             'started':initial,'elapsed_seconds':time.monotonic()-started,'source':SOURCE}
    save(evidence/'completed.json',receipt);print('TRAIN COMPLETE',arm,target,receipt,flush=True)

def main():
    guard();memory();OUT.mkdir()
    for arm in ARMS:
        identity(INPUT/(arm+'-training.bcmc'),OUT,arm+'-input',True,arm)
    assert sha(INPUT/'source-training.bcmc')=='495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855'
    save(OUT/'declaration.json',{'source':SOURCE,'runtime':RUNTIME_SHA,'input':load(INPUT/'experiment.json'),
        'input_files':{p.name:sha(p) for p in INPUT.iterdir() if p.is_file()},'seeds':SEEDS,'tasks':TASKS,'cases':CASES,'relative_margin':RELATIVE_MARGIN,'growth_floor':GROWTH_FLOOR,'input_qualification':'not-required-relative-question',
        'budgets':[250000,1500000],'overshoot':50000,'epoch':time.time(),'runner_sha256':sha(Path(__file__))})
    try:
        evaluate('baseline')
        if not gate('baseline'):return
        for arm in ARMS:train(arm,250000)
        evaluate('early')
        if not gate('early'):return
        for arm in ARMS:train(arm,1500000)
        rows=evaluate('final');retained=gate('final')
        growth=[]
        for seed in SEEDS:
            p=next(r['scores'][11] for r in rows if r['arm']=='protected' and r['seed']==seed)
            c=next(r['scores'][11] for r in rows if r['arm']=='control' and r['seed']==seed)
            growth.append({'seed':seed,'protected':p,'control':c,'passed':p>=GROWTH_FLOOR and p>=c-RELATIVE_MARGIN})
        save(OUT/'completed.json',{'complete':True,'retained':retained,'growth':growth,'useful_pilot':retained and all(g['passed'] for g in growth),
            'deployment':False,'reports':12,'evaluation_trials':12*CASES*len(TASKS),'source':SOURCE,'finished_epoch':time.time()})
        print('STUDY COMPLETE',load(OUT/'completed.json'),flush=True)
    except Exception as error:
        save(OUT/'failure.json',{'error':str(error),'traceback':traceback.format_exc(),'epoch':time.time()});raise

if __name__=='__main__':main()
