#!/usr/bin/env python3
"""Prospective protected storage-to-placement comparison; never mutates the live Academy."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib, json, os, re, signal, subprocess, sys, time, traceback, zipfile
from types import SimpleNamespace
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import holdout

ROOT=Path(__file__).resolve().parents[2]
CONTROL_ROOT=ROOT.parent/'botsclustersmc-protected-continuation'
INPUT=CONTROL_ROOT/'.build/focus-input'
PREVIOUS=ROOT/'.build/protected-placement-study'
OUT=ROOT/'.build/protected-placement-recovery'
CACHE=ROOT.parent/'botsclustersmc/.cache/server'
RUNTIME=CONTROL_ROOT/'dist/training.jar'
CANDIDATE=ROOT/'dist/training.jar'
SOURCE=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
RUNTIME_SHA='49280f05ef9384b2aaeaae5815c147060a147828ffdf4cf82e71a0f2bc1246f9'
BASE_SAMPLES=92574669
SEEDS=[2026092981,2026092982]
CASES=64
RELATIVE_MARGIN=4
GROWTH_FLOOR=8
GROWTH_DELTA=4
TASKS=list(range(12))
ARMS=['control','candidate']
CELLS=holdout.RESET_INTERVENTIONS[3:]
CANDIDATE_SHA=None
INPUT_SHAS={}
ENV=dict(os.environ,EULA='true',JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2')
for key in ('GH_TOKEN','GITHUB_TOKEN'): ENV.pop(key,None)


def read(p,maximum=32*1024*1024):
    if any(q.is_symlink() for q in (p,*p.parents)): raise ValueError('symlinked study path')
    with p.open('rb') as f: data=f.read(maximum+1)
    if len(data)>maximum: raise ValueError('bounded file too large')
    return data

def sha(p): return hashlib.sha256(read(p)).hexdigest()
def load(p): return holdout.read_report(p)
def require(condition,message):
    if not condition: raise AssertionError(message)
def write(p,data):
    if any(q.is_symlink() for q in (p,*p.parents)): raise ValueError('symlinked output')
    with p.open('xb') as f: f.write(data);f.flush();os.fsync(f.fileno())
def save(p,data): write(p,(json.dumps(data,indent=2,allow_nan=False)+'\n').encode())
def execute(command,log,env=ENV,timeout=180,cwd=ROOT):
    with log.open('x') as f:
        result=subprocess.run(list(map(str,command)),cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT,timeout=timeout)
    if result.returncode: raise RuntimeError(f'Command failed ({result.returncode}); retained {log}')
    return read(log).decode(errors='replace')
def guard():
    head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    dirty=subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()
    control_head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=CONTROL_ROOT,text=True).strip()
    control_dirty=subprocess.check_output(['git','status','--porcelain'],cwd=CONTROL_ROOT,text=True).strip()
    if head!=SOURCE or dirty or control_dirty or control_head!='e1f8e6567116c02e0af18e64a466f7e0289e5a14' or sha(RUNTIME)!=RUNTIME_SHA:
        raise RuntimeError('source or reference runtime changed during study')
    if CANDIDATE_SHA is not None and sha(CANDIDATE)!=CANDIDATE_SHA:
        raise RuntimeError('candidate runtime changed during study')
    if any(sha(INPUT/name)!=expected for name,expected in INPUT_SHAS.items()):
        raise RuntimeError('declared input changed during study')
def memory():
    lines=Path('/proc/meminfo').read_text().splitlines()
    available=next(int(line.split()[1])*1024 for line in lines if line.startswith('MemAvailable:'))
    maximum=Path('/sys/fs/cgroup/memory.max').read_text().strip()
    if maximum!='max': available=min(available,int(maximum)-int(Path('/sys/fs/cgroup/memory.current').read_text()))
    if available<2*1024**3: raise RuntimeError('below 2 GiB available-memory floor')
    return available

def identity(checkpoint,out,label,initial=False,arm=None):
    if arm not in ARMS: raise ValueError('explicit audit arm required')
    audit=execute(['java','-Xmx256m','-cp',RUNTIME,ROOT/'tests/studies/FocusAudit.java',INPUT,checkpoint,'initial' if initial else 'continued','protected'],out/(label+'-audit.log'))
    text=execute(['java','-Xmx256m','-cp',RUNTIME,'org.botsclustersmc.training.CheckpointTool','export',checkpoint,out/(label+'-policy.bcmc')],out/(label+'-export.log'))
    match=re.search(r'updates=(\d+), trained_samples=(\d+)',text)
    if not match: raise RuntimeError('missing native export counters')
    record={'updates':int(match[1]),'samples':int(match[2]),'checkpoint_sha256':sha(checkpoint),
            'policy_sha256':sha(out/(label+'-policy.bcmc')),'audit':audit.strip()}
    save(out/(label+'-identity.json'),record)
    return record

def evaluate(phase,intervention='none'):
    assisted=intervention!='none'
    require(intervention=='none' or intervention in CELLS,'unplanned intervention')
    require(not assisted or phase in ('baseline','final'),'unplanned diagnostic phase')
    directory=OUT/(('diagnostic-' if assisted else 'evaluation-')+phase+('-'+intervention if assisted else ''));directory.mkdir()
    models={'base':INPUT/'base-policy.bcmc','protected':INPUT/'protected-policy.bcmc'} if phase=='baseline' else {
        arm:OUT/arm/('250000' if phase=='early' else '1500000')/'stopped-policy.bcmc' for arm in ARMS}
    if assisted and phase=='baseline': models.pop('protected')
    tasks=[11] if assisted else TASKS;cases=32 if assisted else CASES
    jobs=[(arm,path,seed) for arm,path in models.items() for seed in SEEDS]
    def one(index,job):
        arm,path,seed=job;memory();guard();name=f'{arm}-{seed}';output=directory/name
        command=['python3','tests/holdout.py','--policy',path,'--output',output,'--runtime',RUNTIME,'--cache',CACHE,
                 '--tasks',*map(str,tasks),'--cases',str(cases),'--seed',str(seed),'--port',str(30960+index),
                 '--reset-intervention',intervention]
        save(directory/(name+'-started.json'),{'command':list(map(str,command)),'epoch':time.time(),'policy':sha(path),'runtime':sha(RUNTIME)})
        execute(command,directory/(name+'.log'),timeout=960)
        args=SimpleNamespace(tasks=tasks,cases=cases,seed=seed,reset_intervention=intervention)
        report=holdout.verify_report(args,load(output/'result.json'));meta=load(output/'metadata.json')
        require(meta['reset_intervention']==intervention and meta['diagnostic_only'] is assisted,'evaluation assistance identity')
        require(meta['policy_sha256']==sha(path) and meta['runtime_jar_sha256']==RUNTIME_SHA,'evaluation model/runtime identity')
        require(meta['tasks']==tasks and meta['cases_per_task']==cases and meta['seed']==seed,'evaluation plan identity')
        require(sha(output/'runtime.jar')==RUNTIME_SHA,'copied runtime changed')
        require(sha(output/'server/plugins/BotsClustersMC/policy.bcmc')==sha(path),'evaluated policy changed')
        require(not list((output/'server/plugins/BotsClustersMC').glob('training*.bcmc')),'holdout wrote training state')
        scores=[row['passed'] for row in report['tasks']]
        receipt={'arm':arm,'seed':seed,'scores':scores,'policy_sha256':sha(path),'report_sha256':sha(output/'result.json'),
                 'path':str(output/'result.json'),'trials':cases*len(tasks),'new_training_samples':0,
                 'tasks':tasks,'cases':cases,'intervention':intervention}
        save(directory/(name+'-receipt.json'),receipt);print('EVALUATED',phase,intervention,arm,seed,scores,flush=True)
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
        if type(row['trials']) is not int or type(row['new_training_samples']) is not int or row['trials']!=CASES*len(TASKS) or row['new_training_samples']!=0 or row.get('intervention')!='none' or row.get('tasks')!=TASKS or type(row.get('cases')) is not int or row.get('cases')!=CASES:
            raise ValueError('incomplete, assisted or training evaluation')
        result[key]=row['scores']
    if set(result)!=expected: raise ValueError('missing model-seed')
    return result

def gate(phase):
    base=vectors(load(OUT/'evaluation-baseline/completed.json'),['base','protected'])
    rows=vectors(load(OUT/('evaluation-'+phase)/'completed.json'),['base','protected'] if phase=='baseline' else ARMS)
    results=[]
    for seed in SEEDS:
        parent=base['base',seed]
        for arm in (['protected'] if phase=='baseline' else ARMS):
            scores=rows[arm,seed]
            retained=relative_retention(parent,scores)
            results.append({'seed':seed,'arm':arm,'parent':parent,'scores':scores,'retained':retained})
    save(OUT/(phase+'-gate.json'),results)
    if not all(r['retained'] for r in results):
        save(OUT/'rejected.json',{'phase':phase,'reason':'prespecified relative retention gate; not an absolute qualification claim','results':results});return False
    return True

def task_counts(encoded):
    # TaskBalance exposes 18 named tasks PLUS its UNLABELLED bucket.
    require(type(encoded) is str,'task counts must be encoded as a string')
    counts=json.loads(encoded)
    require(type(counts) is list and len(counts)==19 and all(type(n) is int and 0<=n<=2**63-1 for n in counts),'invalid per-task counts')
    require(all(n==0 for i,n in enumerate(counts) if i!=11),'non-target or unlabelled accepted samples')
    return counts

def train(arm,target):
    guard();memory();out=OUT/arm;out.mkdir(exist_ok=True);evidence=out/str(target);evidence.mkdir()
    academy=out/'academy';data=academy/'server/plugins/BotsClustersMC';checkpoint=data/'training.bcmc'
    if target==250000:
        data.mkdir(parents=True);write(academy/'.botsclustersmc-academy',b'botsclustersmc-owned-training\n');write(academy/'.gitignore',b'*\n')
        write(checkpoint,read(INPUT/'protected-training.bcmc'))
        require(sha(checkpoint)==sha(INPUT/'protected-training.bcmc'),'initial checkpoint differs')
    else:
        require(target==1500000 and (out/'250000/completed.json').is_file(),'invalid continuation boundary')
        require(sha(checkpoint)==sha(out/'250000/stopped-training.bcmc'),'not the exact early resume')
    initial=identity(checkpoint,evidence,'start',target==250000,arm)
    env=dict(ENV,ACADEMY=str(academy),BOTS='512',HEAP_GB='2',REGION_THREADS='2',INFERENCE_THREADS='1',LEARNER_THREADS='1',
             SEED='7',PORT='30951' if arm=='control' else '30952',BIND_ADDRESS='127.0.0.1',ONLINE_MODE='true',BCMC_SERVER_CACHE=str(CACHE))
    runtime_root=CONTROL_ROOT if arm=='control' else ROOT
    threshold=BASE_SAMPLES+target;epoch=int(time.time()*1000);started=time.monotonic();first=None;last=None;last_print=0
    with (evidence/'training.log').open('x') as log,(evidence/'status.jsonl').open('x') as history:
        process=subprocess.Popen(['./start.sh'],cwd=runtime_root,env=env,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        try:
            while True:
                if time.monotonic()-started>1800: raise TimeoutError('declared 30-minute training-segment cap')
                memory()
                if process.poll() is not None: raise RuntimeError('learner exited before sample boundary')
                if (data/'status.json').is_file():
                    status=load(data/'status.json')
                    if status.get('epoch_millis',0)>=epoch:
                        if status.get('state')!='running' or time.time()*1000-status['epoch_millis']>45000: raise RuntimeError('unhealthy or stale learner')
                        require(status['learning_task_scope']==11 and status['protected_prior_policy'] is True,'unprotected or out-of-scope learner')
                        if any(status.get(k,0) for k in ('inference_failed','inference_rejected','learner_rejected_samples','learner_stale_samples','retired_agents')):
                            raise RuntimeError('runtime error counters')
                        task_counts(status['learned_task_samples_this_process'])
                        if last is None or status['epoch_millis']!=last['epoch_millis']:
                            last=status;history.write(json.dumps(status)+'\n');history.flush()
                            if first is None and status['active_agents']==512:
                                first=status;save(evidence/'first-active.json',first)
                            if time.monotonic()-last_print>30:
                                print('TRAIN',arm,target,status['trained_samples']-BASE_SAMPLES,'scope_skipped',status['learner_scope_skipped_samples'],flush=True);last_print=time.monotonic()
                        if status['trained_samples']>=threshold:break
                time.sleep(2)
            execute(['./stop.sh'],evidence/'stop.log',env,90,cwd=runtime_root);process.wait(timeout=120)
            if process.returncode:raise RuntimeError('unclean learner stop')
        finally:
            if process.poll() is None:
                try:execute(['./stop.sh'],evidence/'failure-stop.log',env,90,cwd=runtime_root);process.wait(timeout=120)
                except Exception:
                    os.killpg(process.pid,signal.SIGTERM);process.wait(timeout=40)
            if checkpoint.is_file():write(evidence/'stopped-training.bcmc',read(checkpoint))
    stopped=identity(evidence/'stopped-training.bcmc',evidence,'stopped',False,arm);guard()
    require(threshold<=stopped['samples']<=threshold+50000 and first is not None and last is not None,'sample boundary or coverage failed')
    save(evidence/'last-observed.json',last)
    receipt={'arm':arm,'target':target,'additional_samples':stopped['samples']-BASE_SAMPLES,'stopped':stopped,
             'started':initial,'elapsed_seconds':time.monotonic()-started,'source':SOURCE,
             'runtime':RUNTIME_SHA if arm=='control' else CANDIDATE_SHA,'both_arms_protected':True}
    save(evidence/'completed.json',receipt);print('TRAIN COMPLETE',arm,target,receipt,flush=True)

def growth_pass(candidate,control):
    require(type(candidate) is int and type(control) is int and 0<=candidate<=CASES and 0<=control<=CASES,'invalid growth count')
    return candidate>=GROWTH_FLOOR and candidate>=control+GROWTH_DELTA

def diagnostic_screen(rows):
    expected={(arm,seed,cell) for arm in ARMS for seed in SEEDS for cell in CELLS};scores={}
    for row in rows:
        key=(row['arm'],row['seed'],row['intervention'])
        require(key in expected and key not in scores,'duplicate/unplanned diagnostic')
        require(row['tasks']==[11] and row['cases']==32 and row['trials']==32 and row['new_training_samples']==0,'invalid diagnostic scope')
        require(len(row['scores'])==1 and type(row['scores'][0]) is int and 0<=row['scores'][0]<=32,'invalid diagnostic score')
        scores[key]=row['scores'][0]
    require(set(scores)==expected,'missing diagnostic')
    result=[]
    for seed in SEEDS:
        candidate=[scores['candidate',seed,cell] for cell in CELLS];control=[scores['control',seed,cell] for cell in CELLS]
        result.append({'seed':seed,'candidate':candidate,'control':control,
                       'placement_gain':min(candidate)>=4 and sum(candidate)>=sum(control)+32})
    return result

def artifact_boundary():
    with zipfile.ZipFile(RUNTIME) as a,zipfile.ZipFile(CANDIDATE) as b:
        require(set(a.namelist())==set(b.namelist()),'runtime entry sets differ')
        changed=sorted(name for name in a.namelist() if a.read(name)!=b.read(name))
    require(changed==['org/botsclustersmc/training/InitialCrafting.class'],'unexpected candidate runtime change')
    require(sha(CONTROL_ROOT/'dist/botsclustersmc.jar')==sha(ROOT/'dist/botsclustersmc.jar'),'inference artifacts differ')
    return changed

def main():
    global CANDIDATE_SHA,INPUT_SHAS
    guard();memory();changed=artifact_boundary();CANDIDATE_SHA=sha(CANDIDATE)
    INPUT_SHAS={p.name:sha(p) for p in INPUT.iterdir() if p.is_file()}
    require(sha(INPUT/'source-training.bcmc')=='495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855','wrong original checkpoint')
    require(sha(INPUT/'source-runtime.jar')=='7e3df712d2b9d9bb5afcf8e5a1010004d60c00a36f71448f7bc0457443764dfc','wrong original runtime')
    OUT.mkdir()
    for arm in ARMS: identity(INPUT/'protected-training.bcmc',OUT,arm+'-input',True,arm)
    save(OUT/'declaration.json',{'source':SOURCE,'reference_runtime':RUNTIME_SHA,'candidate_runtime':CANDIDATE_SHA,'changed_entries':changed,
        'input_files':INPUT_SHAS,'seeds':SEEDS,'tasks':TASKS,'cases':CASES,'relative_margin':RELATIVE_MARGIN,
        'growth_floor':GROWTH_FLOOR,'growth_delta':GROWTH_DELTA,'diagnostic_cells':list(CELLS),'diagnostic_cases':32,
        'budgets':[250000,1500000],'overshoot':50000,'both_arms_protected':True,'epoch':time.time(),'runner_sha256':sha(Path(__file__)),
        'operational_recovery':True,'previous_attempt':str(PREVIOUS),'reused_baseline_reports':14,
        'excluded_operational_samples':4,'new_game_reports':28})
    try:
        from placement_recovery import reuse_baselines
        reuse_baselines(PREVIOUS,OUT,INPUT,RUNTIME_SHA,INPUT_SHAS,SEEDS,TASKS,CASES,CELLS)
        if not gate('baseline'):return
        for arm in ARMS:train(arm,250000)
        evaluate('early')
        if not gate('early'):return
        for arm in ARMS:train(arm,1500000)
        rows=evaluate('final');retained=gate('final');growth=[]
        for seed in SEEDS:
            p=next(r['scores'][11] for r in rows if r['arm']=='candidate' and r['seed']==seed)
            c=next(r['scores'][11] for r in rows if r['arm']=='control' and r['seed']==seed)
            growth.append({'seed':seed,'candidate':p,'control':c,'passed':growth_pass(p,c)})
        diagnostics=[]
        for cell in CELLS:diagnostics.extend(evaluate('final',cell))
        placement=diagnostic_screen(diagnostics)
        save(OUT/'completed.json',{'complete':True,'retained':retained,'growth':growth,'placement':placement,
            'ordinary_gain':retained and all(g['passed'] for g in growth),'deployment':False,'ordinary_reports':12,
            'ordinary_trials':12*CASES*len(TASKS),'assisted_reports':30,'assisted_trials':960,'source':SOURCE,'finished_epoch':time.time()})
        print('STUDY COMPLETE',load(OUT/'completed.json'),flush=True)
    except Exception as error:
        save(OUT/'failure.json',{'error':str(error),'traceback':traceback.format_exc(),'epoch':time.time()});raise

if __name__=='__main__':main()
