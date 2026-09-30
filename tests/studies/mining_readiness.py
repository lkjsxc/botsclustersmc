#!/usr/bin/env python3
"""Finite matched warm continuation: mining-readiness potential only, no scripted gameplay."""
from concurrent.futures import ThreadPoolExecutor
import re, time, traceback
from mining_readiness_support import *

CANDIDATE_RUNTIME = '4a809f7311b71af967b3d5302b064b365c989de38f22ae26e92aa40fe30f75cc'


class Study:
    def __init__(self):
        self.source = git(ROOT, 'rev-parse', 'HEAD')
        self.roots = dict(control=CONTROL, candidate=ROOT)
        self.runtimes = {arm: root/'dist/training.jar' for arm,root in self.roots.items()}
        self.pins = {INPUT:INPUT_SHA, POLICY:POLICY_SHA, self.runtimes['control']:CONTROL_RUNTIME,
            self.runtimes['candidate']:CANDIDATE_RUNTIME}
        self.guard()

    def guard(self):
        require(git(ROOT,'rev-parse','HEAD') == self.source and not git(ROOT,'status','--porcelain'), 'Candidate source changed')
        require(git(CONTROL,'rev-parse','HEAD') == CONTROL_SOURCE and not git(CONTROL,'status','--porcelain'), 'Control source changed')
        for path,digest in self.pins.items(): require(sha(path) == digest, 'Qualified input/runtime changed: '+str(path))
        require(not git(ROOT,'diff',CONTROL_SOURCE,'--','core','plugin','tests/holdout','tests/holdout.py'), 'Inference/gameplay/evaluation intervention forbidden')

    def identity(self, arm, checkpoint, directory, label, initial):
        policy = directory/(label+'-policy.bcmc'); runtime = self.runtimes[arm]
        text = execute(['java','-Xmx256m','-cp',runtime,'org.botsclustersmc.training.CheckpointTool','export',checkpoint,policy],directory/(label+'-export.log'))
        execute(['java','-Xmx256m','-cp',runtime,'org.botsclustersmc.training.CheckpointTool','verify-export',checkpoint,policy],directory/(label+'-verify.log'))
        match = re.search(r'updates=(\d+), trained_samples=(\d+)',text)
        require(match is not None, 'Missing native checkpoint counters')
        result = dict(updates=int(match[1]), samples=int(match[2]), checkpoint_sha256=sha(checkpoint), policy_sha256=sha(policy))
        if initial:
            require(result['updates']==BASE_UPDATES and result['samples']==BASE and sha(checkpoint)==INPUT_SHA
                and sha(policy)==POLICY_SHA, 'Initial weights, optimizer/course checkpoint and counters must be exact')
        else: require(result['samples'] > BASE, 'No accepted continuation samples')
        save(directory/(label+'-identity.json'),result); return result

    def evaluate(self, phase, target=None):
        self.guard(); directory=OUT/('evaluation-'+phase); directory.mkdir()
        models = {'parent':POLICY} if phase=='baseline' else {a:OUT/a/str(target)/'stopped-policy.bcmc' for a in ARMS}
        jobs = [(arm,seed,condition) for arm in models for seed in SEEDS for condition in CONDITIONS]
        def one(index):
            arm,seed,condition=jobs[index]; memory(); self.guard(); args=spec(seed,condition)
            name=f'{arm}-{seed}-{condition}'; output=directory/name; port=31090+index; policy=models[arm]
            frozen=sha(policy)
            command=['python3',ROOT/'tests/holdout.py','--policy',policy,'--runtime',self.runtimes['control'],
                '--cache',CACHE,'--output',output,'--tasks',*args.tasks,'--cases',32,'--seed',seed,
                '--port',port,'--reset-intervention',condition]
            save(directory/(name+'-started.json'),dict(command=list(map(str,command)), policy_sha256=frozen, epoch=time.time()))
            execute(command,directory/(name+'.log'),timeout=1200)
            require(sha(policy)==frozen,'Evaluated source changed')
            scores,mining=verify_evaluation(output,policy,seed,condition,port)
            receipt=dict(arm=arm,seed=seed,condition=condition,scores=scores,mining=mining,trials=32*len(args.tasks),new_training_samples=0,
                policy_sha256=frozen,report_sha256=sha(output/'result.json'),metadata_sha256=sha(output/'metadata.json'))
            save(directory/(name+'-receipt.json'),receipt);print('EVALUATED',phase,arm,seed,condition,scores,flush=True)
            return receipt
        rows=[]
        for start in range(0,len(jobs),4):
            with ThreadPoolExecutor(max_workers=4) as pool:
                rows.extend(f.result() for f in [pool.submit(one,i) for i in range(start,min(start+4,len(jobs)))])
        self.guard();vectors(rows,tuple(models));save(directory/'completed.json',rows);return rows

    def train(self, arm, target):
        require(arm in ARMS and type(target) is int and target in TARGETS,'Declared training segment')
        self.guard();memory();root=self.roots[arm];directory=OUT/arm;directory.mkdir(exist_ok=True)
        evidence=directory/str(target);evidence.mkdir();academy=directory/'academy';data=academy/'server/plugins/BotsClustersMC';checkpoint=data/'training.bcmc'
        if target==TARGETS[0]:
            data.mkdir(parents=True);write(academy/'.botsclustersmc-academy',b'botsclustersmc-owned-training\n')
            write(academy/'.gitignore',b'*\n');write(checkpoint,read(INPUT))
        else:
            require((directory/str(TARGETS[0])/'completed.json').is_file(),'Missing early state')
            require(sha(checkpoint)==sha(directory/str(TARGETS[0])/'stopped-training.bcmc'),'Exact early checkpoint required for continuation')
        initial=self.identity(arm,checkpoint,evidence,'start',target==TARGETS[0])
        env=dict(ENV,ACADEMY=str(academy),BOTS='512',HEAP_GB='2',REGION_THREADS='2',INFERENCE_THREADS='1',LEARNER_THREADS='1',SEED='7',
            PORT='31281' if arm=='control' else '31282',BIND_ADDRESS='127.0.0.1',ONLINE_MODE='true',BCMC_SERVER_CACHE=str(CACHE))
        first=last=None;threshold=BASE+target;epoch=int(time.time()*1000);started=time.monotonic();printed=0
        with (evidence/'training.log').open('x') as log,(evidence/'status.jsonl').open('x') as history:
            process=subprocess.Popen(['./start.sh'],cwd=root,env=env,stdin=subprocess.DEVNULL,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
            try:
                while True:
                    require(time.monotonic()-started<=1800,'30-minute segment operational cap');memory()
                    require(process.poll() is None,'Learner exited before accepted-sample boundary')
                    if (data/'status.json').is_file():
                        status=load(data/'status.json');samples=training_samples(status,initial['samples'],epoch,time.time()*1000,last)
                        if samples is not None:
                            require(samples<=threshold+OVERSHOOT,'Live accepted-sample budget exceeded')
                            if last is None or status['epoch_millis']!=last['epoch_millis']:
                                last=status;history.write(json.dumps(status,allow_nan=False)+'\n');history.flush()
                                if first is None and status['active_agents']==512 and status.get('startup_observed_agents')==512:
                                    require(status.get('startup_restored_checkpoint') is True and status.get('startup_expected_agents')==512,'Complete warm startup coverage')
                                    first=status;save(evidence/'first-active.json',first)
                                if time.monotonic()-printed>=30:
                                    print('TRAIN',arm,target,samples-BASE,flush=True);printed=time.monotonic()
                            if samples>=threshold:break
                    time.sleep(2)
                execute(['./stop.sh'],evidence/'stop.log',root,env,90);process.wait(timeout=120)
                require(process.returncode==0,'Unclean training stop')
            finally:
                if process.poll() is None:
                    try:execute(['./stop.sh'],evidence/'failure-stop.log',root,env,90);process.wait(timeout=120)
                    except Exception as error:
                        save(evidence/'cleanup-error.json',dict(error=str(error)))
                        if process.poll() is None:os.killpg(process.pid,signal.SIGTERM);process.wait(timeout=45)
                if checkpoint.is_file():write(evidence/'stopped-training.bcmc',read(checkpoint))
        stopped=self.identity(arm,evidence/'stopped-training.bcmc',evidence,'stopped',False);self.guard();budget(stopped['samples'],target)
        require(first is not None and last is not None and task_counts(last['learned_task_samples_this_process'])[12]>0,'Missing full population coverage or frontier samples')
        contexts,_=context_counts(last,initial['samples']);require(sum(contexts[12*6:13*6])>0,'Missing accepted task-12 contexts')
        for name in ('foundation_ticks_this_process','frontier_ticks_this_process','review_ticks_this_process','exam_ticks_this_process'):
            H.integer(last.get(name),2**63-1,name)
        save(evidence/'last-observed.json',last)
        save(evidence/'completed.json',dict(arm=arm,target=target,additional_samples=stopped['samples']-BASE,start=initial,stopped=stopped,
            source=self.source,elapsed_seconds=time.monotonic()-started))
        print('TRAIN COMPLETE',arm,target,stopped['samples']-BASE,flush=True)

    def run(self):
        require(ENV.get('EULA')=='true','Explicit EULA=true required');memory();OUT.mkdir()
        save(OUT/'declaration.json',dict(source=self.source,control_source=CONTROL_SOURCE,input_checkpoint=INPUT_SHA,input_policy=POLICY_SHA,
            candidate_runtime=CANDIDATE_RUNTIME,evaluation_runtime=CONTROL_RUNTIME,baseline_samples=BASE,
            seeds=SEEDS,ordinary_tasks=list(range(13)),conditions=CONDITIONS,cases=32,budgets=TARGETS,overshoot=OVERSHOOT,
            maximum_training_servers=2,maximum_evaluators=4,learning_seed=7,actors=512,
            hypothesis='State-only tool-readiness and real target-progress potential acquires ordinary cobblestone mining while retaining tasks 0-11.',deployment=False))
        try:
            for arm in ARMS:self.identity(arm,INPUT,OUT,arm+'-input',True)
            baseline=self.evaluate('baseline');phases=[baseline];qualified=qualify_baseline(baseline)
            stage='baseline';decision=dict(retained=False,acquisition=False,details=[])
            if qualified:
                for phase,target in zip(('early','final'),TARGETS):
                    with ThreadPoolExecutor(max_workers=2) as pool:
                        for future in [pool.submit(self.train,arm,target) for arm in ARMS]:future.result()
                    current=self.evaluate(phase,target);phases.append(current);stage=phase;decision=gate(baseline,current)
                    save(OUT/(phase+'-gate.json'),decision)
                    if not decision['retained']:break
            save(OUT/'outcome.json',dict(source=self.source,stage=stage,input_qualified=qualified,retained=decision['retained'],
                acquisition=decision['acquisition'],useful_pilot=stage=='final' and decision['retained'] and decision['acquisition'],
                reports=sum(len(rows) for rows in phases),frozen_trials=sum(r['trials'] for rows in phases for r in rows),deployment=False))
            print('STUDY COMPLETE',load(OUT/'outcome.json'),flush=True)
        except BaseException as error:
            save(OUT/'failure.json',dict(error=type(error).__name__,detail=str(error),traceback=traceback.format_exc(),deployment=False));raise


def main():
    import argparse, unittest
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',action='store_true');args=parser.parse_args()
    if not args.run:parser.print_help();return
    require(os.environ.get('EULA')=='true','Explicit EULA=true required before any study activity')
    suite=unittest.defaultTestLoader.discover(str(ROOT/'tests/studies'),pattern='test_mining_readiness.py')
    require(suite.countTestCases()>=10 and unittest.TextTestRunner().run(suite).wasSuccessful(),'Offline qualification failed')
    Study().run()


if __name__=='__main__':main()
