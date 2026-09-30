#!/usr/bin/env python3
"""Finite gradient-ablation diagnosis: same mining control reward, one critic-feature change."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse,os,traceback,unittest
import mining_control as M
import mining_control_support as S

ROOT=Path(__file__).resolve().parents[2]
CONTROL_SOURCE='234e0cf5e15b31343f4f1f4b9916ae85c0df180e'
CONTROL_RUNTIME='782fb3bcde791745672b5d1433fa3f3efb190737dc594578bbc796079e8233c3'
CANDIDATE_RUNTIME='fef1ef4d73ed320392ab30322afe2233a765e3c4e74e7bb48fc458d3362fd3cd'
SEEDS=(2026093091,2026093092)
TARGET=250000
OUT=ROOT/'.build/critic-detachment-study'

# Reuse the already-qualified finite runner without duplicating its lifecycle logic.
# Both modules bind constants at import; configure both explicitly before construction.
for module in (M,S):
    module.ROOT=ROOT;module.CONTROL=ROOT.parent/'botsclustersmc-critic-detachment-control'
    module.CONTROL_SOURCE=CONTROL_SOURCE;module.CONTROL_RUNTIME=CONTROL_RUNTIME
    module.CANDIDATE_RUNTIME=CANDIDATE_RUNTIME;module.SEEDS=SEEDS
    module.TARGETS=(TARGET,);module.OUT=OUT


def interpretation(baseline,current):
    base=S.vectors(baseline,('parent',));actual=S.vectors(current,S.ARMS)
    retained={};lost={};rows=[]
    for seed in SEEDS:
        reference=base['parent',seed,'none']
        for arm in S.ARMS:
            scores=actual[arm,seed,'none'];failures=[]
            for task,(source,score) in enumerate(zip(reference,scores)):
                floor=max(28,source-3) if task<11 else max(26,source-4) if task==11 else max(0,source-2)
                if score<floor:failures.append(dict(task=task,source=source,score=score,floor=floor))
            retained[arm,seed]=not failures
            rows.append(dict(arm=arm,seed=seed,failures=failures,mining=scores[12]))
        lost[seed]=all(reference[t]-actual['control',seed,'none'][t]>=8 for t in (2,10,11))
    candidate=all(retained['candidate',s] for s in SEEDS)
    reproduced=all(lost.values())
    acquisition=all(actual['candidate',s,'none'][12]>=8 and actual['candidate',s,'none'][12]>=actual['control',s,'none'][12]+4 for s in SEEDS)
    return dict(candidate_retained=candidate,control_retained=all(retained['control',s] for s in SEEDS),
        control_regression_reproduced=reproduced,detachment_preserves_under_reproduced_loss=candidate and reproduced,
        acquisition=acquisition,details=rows,deployment=False)


class Study(M.Study):
    def guard(self):
        super().guard()
        S.require(not S.git(ROOT,'diff',CONTROL_SOURCE,'--','training/src/org/botsclustersmc/training/MiningControl.java',
            'training/src/org/botsclustersmc/training/TrainingEnvironment.java','training/src/org/botsclustersmc/training/TrainingPlugin.java',
            'training/src/org/botsclustersmc/training/Adam.java','training/src/org/botsclustersmc/training/TaskBalance.java',
            'training/src/org/botsclustersmc/training/VTrace.java','training/src/org/botsclustersmc/training/TrainingState.java'),
            'Reward/reset/optimizer/value-target intervention forbidden')

    def run(self):
        S.require(S.ENV.get('EULA')=='true','Explicit EULA=true required');S.memory();OUT.mkdir()
        S.save(OUT/'declaration.json',dict(kind='critic-feature-gradient-ablation',source=self.source,
            control_source=CONTROL_SOURCE,control_runtime=CONTROL_RUNTIME,candidate_runtime=CANDIDATE_RUNTIME,
            input_checkpoint=S.INPUT_SHA,input_policy=S.POLICY_SHA,baseline_samples=S.BASE,baseline_updates=S.BASE_UPDATES,
            seeds=SEEDS,ordinary_tasks=list(range(13)),cases=32,reset_intervention='none',accepted_sample_budget=TARGET,
            overshoot=S.OVERSHOOT,actors=512,learning_seed=7,maximum_training_servers=2,maximum_evaluators=4,
            unchanged='Both arms use the rejected mining-control reward; only task-12 critic-to-shared-feature gradients differ.',
            limitation='Same inherited Adam state; detached current critic gradients do not erase inherited moments or prevent actor-gradient drift.',
            interpretation='Candidate all-task floors plus control losses of at least 8/32 on tasks2,10,11 on each seed support the bounded detachment intervention, not a complete causal attribution or a general architecture.',
            deployment=False))
        try:
            for arm in S.ARMS:self.identity(arm,S.INPUT,OUT,arm+'-input',True)
            baseline=self.evaluate('baseline');qualified=S.qualify_baseline(baseline)
            phases=[baseline];decision=None
            if qualified:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    futures=[pool.submit(self.train,arm,TARGET) for arm in S.ARMS]
                    for future in futures:future.result()
                current=self.evaluate('early',TARGET);phases.append(current)
                decision=interpretation(baseline,current);S.save(OUT/'interpretation.json',decision)
            self.guard()
            result=dict(source=self.source,stage='complete' if qualified else 'input-rejected',input_qualified=qualified,
                reports=sum(map(len,phases)),frozen_trials=sum(row['trials'] for phase in phases for row in phase),
                accepted_sample_budget=TARGET,decision=decision,new_training_after_boundary=False,deployment=False)
            S.save(OUT/'outcome.json',result);print('DIAGNOSIS COMPLETE',result,flush=True)
        except BaseException as error:
            S.save(OUT/'failure.json',dict(error=type(error).__name__,detail=str(error),traceback=traceback.format_exc(),deployment=False));raise


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',action='store_true');args=parser.parse_args()
    if not args.run:parser.print_help();return
    S.require(os.environ.get('EULA')=='true','Explicit EULA=true required before study activity')
    suite=unittest.defaultTestLoader.discover(str(ROOT/'tests/studies'),pattern='test_critic_detachment.py')
    S.require(suite.countTestCases()>=10 and unittest.TextTestRunner().run(suite).wasSuccessful(),'Offline qualification failed')
    Study().run()

if __name__=='__main__':main()
