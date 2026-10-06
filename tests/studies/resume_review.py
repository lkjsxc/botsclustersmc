#!/usr/bin/env python3
"""Finite resume-review phase comparison; fixed detached critic and mining reward in both arms."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import argparse,json,os,traceback,unittest,zipfile
import mining_control as M
import mining_control_support as S

ROOT=Path(__file__).resolve().parents[2]
CONTROL_SOURCE='ddcf2f6d24bfa0bbb3a14d3665be2e629eeb8602'
CONTROL_RUNTIME='fef1ef4d73ed320392ab30322afe2233a765e3c4e74e7bb48fc458d3362fd3cd'
CANDIDATE_RUNTIME='82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9'
SEEDS=(2026100401,2026100402)
TARGET=250000
EARLY_COVERAGE=50000
COVERAGE_OVERSHOOT=25000
OUT=ROOT/'.build/resume-review-study'
for module in (M,S):
    module.ROOT=ROOT;module.CONTROL=ROOT.parent/'botsclustersmc-resume-review-control-20261004'
    module.CONTROL_SOURCE=CONTROL_SOURCE;module.CONTROL_RUNTIME=CONTROL_RUNTIME
    module.CANDIDATE_RUNTIME=CANDIDATE_RUNTIME;module.SEEDS=SEEDS
    module.TARGETS=(TARGET,);module.OUT=OUT


def coverage(history,startup,arm):
    """Status-resolution accepted coverage; never invent exact first-gradient timing."""
    S.require(arm in S.ARMS and type(history) is list and len(history)>0,'Complete coverage history')
    S.require(type(startup) is dict and startup.get('startup_restored_checkpoint') is True,'Restored startup')
    for key in ('startup_observed_agents','startup_expected_agents'):
        S.require(S.H.integer(startup.get(key),512,key)==512,'Complete startup actor count')
    expected_review=512 if arm=='candidate' else 0
    population=json.loads(startup.get('startup_training_task_population'))
    S.require(type(population) is list and len(population)==18,'Startup task population')
    for count in population:S.H.integer(count,512,'Startup task count')
    S.require(sum(population)==512,'Startup population denominator')
    startup_match=(startup.get('startup_review_agents')==expected_review
        and startup.get('startup_frontier_agents')==512-expected_review
        and sum(population[:12])==expected_review and population[12]==512-expected_review)
    for key in ('startup_review_agents','startup_frontier_agents'):
        S.H.integer(startup.get(key),512,key)
    previous=None;previous_cells=None;previous_ticks=None;early=None;first_review=None;last=None
    epoch=history[0].get('epoch_millis')
    for row in history:
        S.training_samples(row,S.BASE,epoch,row.get('epoch_millis'),previous)
        cells,ticks=S.context_counts(row,S.BASE);counts=[sum(cells[t*6:t*6+6]) for t in range(19)]
        offset=sum(cells)
        if previous_cells is not None:
            S.require(all(a>=b for a,b in zip(cells,previous_cells)) and all(a>=b for a,b in zip(ticks,previous_ticks)),
                'Context sample/tick cells regressed')
        item=dict(accepted_samples=offset,by_task=counts,epoch_millis=row['epoch_millis'],observed_actor_ticks=sum(ticks))
        if first_review is None and sum(counts[:12])>0:first_review=item
        if early is None and offset>=EARLY_COVERAGE:early=item
        previous=row;previous_cells=cells;previous_ticks=ticks;last=item
    timely=early is not None and early['accepted_samples']<=EARLY_COVERAGE+COVERAGE_OVERSHOOT
    delivered=startup_match and timely and all(n>0 for n in early['by_task'][:13]) if arm=='candidate' else False
    frontier_only=startup_match and sum(last['by_task'][:12])==0 and last['by_task'][12]>0 if arm=='control' else False
    return dict(arm=arm,startup_match=startup_match,early_accepted_coverage=delivered,
        frontier_only_through_last_observation=frontier_only,first_observed_review=first_review,early=early,last=last)


def interpretation(baseline,current,exposure):
    base=S.vectors(baseline,('parent',));actual=S.vectors(current,S.ARMS)
    S.require(type(exposure) is dict and set(exposure)==set(S.ARMS),'Both observed exposure arms required')
    for arm,key in (('candidate','early_accepted_coverage'),('control','frontier_only_through_last_observation')):
        S.require(type(exposure[arm]) is dict and type(exposure[arm].get(key)) is bool,'Measured exposure boolean')
    retained={};earlier={};lost={};rows=[]
    for seed in SEEDS:
        reference=base['parent',seed,'none']
        for arm in S.ARMS:
            scores=actual[arm,seed,'none'];failures=[]
            for task,(source,score) in enumerate(zip(reference,scores)):
                floor=max(28,source-3) if task<11 else max(26,source-4) if task==11 else max(0,source-2)
                if score<floor:failures.append(dict(task=task,source=source,score=score,floor=floor))
            retained[arm,seed]=not failures;earlier[arm,seed]=all(row['task']>=11 for row in failures)
            rows.append(dict(arm=arm,seed=seed,failures=failures,pickaxe=scores[11],mining=scores[12]))
        lost[seed]=reference[11]-actual['control',seed,'none'][11]>=8
    candidate=all(retained['candidate',s] for s in SEEDS)
    reproduced=all(lost.values()) and all(earlier['control',s] for s in SEEDS)
    delivered=exposure['candidate']['early_accepted_coverage'] and exposure['control']['frontier_only_through_last_observation']
    acquisition=all(actual['candidate',s,'none'][12]>=8 and actual['candidate',s,'none'][12]>=actual['control',s,'none'][12]+4 for s in SEEDS)
    return dict(candidate_retained=candidate,control_retained=all(retained['control',s] for s in SEEDS),
        control_pickaxe_loss_reproduced=reproduced,exposure_contrast_delivered=delivered,
        review_preserves_under_reproduced_loss=candidate and reproduced and delivered,
        acquisition=acquisition,details=rows,deployment=False)


class Study(M.Study):
    def guard(self):
        super().guard()
        prefix='training/src/org/botsclustersmc/training/'
        changed=set(S.git(ROOT,'diff','--name-only',CONTROL_SOURCE,'--','core','plugin','training').splitlines())
        S.require(changed=={prefix+'Course.java',prefix+'ReviewEffort.java'},'Only resume allocation may differ')
        payload=[]
        for arm in S.ARMS:
            with zipfile.ZipFile(self.runtimes[arm]) as jar:
                payload.append({n:jar.read(n) for n in jar.namelist()})
        delta={n for n in set(payload[0])|set(payload[1]) if payload[0].get(n)!=payload[1].get(n)}
        S.require(delta=={'org/botsclustersmc/training/'+n+'.class' for n in ('Course','Course$Agent','ReviewEffort')},
            'Unexpected training artifact difference')
        S.require(S.sha(self.roots['control']/'dist/botsclustersmc.jar')==S.sha(ROOT/'dist/botsclustersmc.jar'),
            'Inference artifacts must be byte-identical')

    def exposure(self):
        result={}
        for arm in S.ARMS:
            path=OUT/arm/str(TARGET)
            history=[json.loads(line) for line in S.read(path/'status.jsonl').decode().splitlines()]
            result[arm]=coverage(history,S.load(path/'first-active.json'),arm)
        S.save(OUT/'exposure.json',result);return result

    def run(self):
        S.require(S.ENV.get('EULA')=='true','Explicit EULA=true required');S.memory();OUT.mkdir()
        S.save(OUT/'declaration.json',dict(kind='resume-review-phase',source=self.source,
            control_source=CONTROL_SOURCE,control_runtime=CONTROL_RUNTIME,candidate_runtime=CANDIDATE_RUNTIME,
            input_checkpoint=S.INPUT_SHA,input_policy=S.POLICY_SHA,baseline_samples=S.BASE,baseline_updates=S.BASE_UPDATES,
            seeds=SEEDS,ordinary_tasks=list(range(13)),cases=32,reset_intervention='none',accepted_sample_budget=TARGET,
            overshoot=S.OVERSHOOT,actors=512,learning_seed=7,maximum_training_servers=2,maximum_evaluators=4,
            training_ports=[31681,31682],evaluation_ports=list(range(31690,31694)),
            coverage_boundary=EARLY_COVERAGE,coverage_status_overshoot=COVERAGE_OVERSHOOT,
            intervention='On checkpoint decode only, credit begins at one instead of zero; actual review ticks repay it at the original 4:1 rate.',
            unchanged='Both arms use the preceding rejected detached-critic runtime and stronger mining reward; not unchanged production training.',
            support='All candidate task floors on both seeds, reproduced control pickaxe loss of at least 8/32 with tasks0-10 retained, and validated early accepted review contrast.',
            limitation='One training trajectory per arm; fresh evaluation seeds are not independent learner replications. Coverage is status-resolution, not exact gradient timestamps.',
            deployment=False))
        try:
            for arm in S.ARMS:self.identity(arm,S.INPUT,OUT,arm+'-input',True)
            baseline=self.evaluate('baseline');qualified=S.qualify_baseline(baseline)
            phases=[baseline];decision=None
            if qualified:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    futures=[pool.submit(self.train,arm,TARGET) for arm in S.ARMS]
                    for future in futures:future.result()
                exposure=self.exposure()
                current=self.evaluate('early',TARGET);phases.append(current)
                decision=interpretation(baseline,current,exposure);S.save(OUT/'interpretation.json',decision)
            self.guard()
            result=dict(source=self.source,stage='complete' if qualified else 'input-rejected',input_qualified=qualified,
                reports=sum(map(len,phases)),frozen_trials=sum(row['trials'] for phase in phases for row in phase),
                accepted_sample_budget=TARGET,decision=decision,new_training_after_boundary=False,deployment=False)
            S.save(OUT/'outcome.json',result);print('REVIEW STUDY COMPLETE',result,flush=True)
        except BaseException as error:
            S.save(OUT/'failure.json',dict(error=type(error).__name__,detail=str(error),traceback=traceback.format_exc(),deployment=False));raise


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',action='store_true');args=parser.parse_args()
    if not args.run:parser.print_help();return
    S.require(os.environ.get('EULA')=='true','Explicit EULA=true required before study activity')
    suite=unittest.defaultTestLoader.discover(str(ROOT/'tests/studies'),pattern='test_resume_review.py')
    S.require(suite.countTestCases()>=15 and unittest.TextTestRunner().run(suite).wasSuccessful(),'Offline qualification failed')
    Study().run()

if __name__=='__main__':main()
