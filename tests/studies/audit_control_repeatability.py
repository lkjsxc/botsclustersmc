#!/usr/bin/env python3
"""Independent raw-report recount. Does not import the experiment controller or holdout."""
from pathlib import Path
import argparse
import hashlib
import json
import math

SEEDS = (2026100741, 2026100742)
MODELS = ('parent', 'control-earlier', 'control-later', 'review-later')
DIGESTS = ('ca9215b398f5a7097135af84f03f6857e104fdaf0bfcf79c0529669debdac1bc',
           '7b9f51d605c89d34cd13c3e3cbd4a36ec281afad62156962e2e6b49b3a974af1',
           'd6543d1205b2507c23e15ffe844d4e11cf47656df70840bb185669cd29df433a',
           '57144e26e1af87fa286d7eaf037f928d4fa005e1251bc5155b55e6c87936ea22')
RUNTIME = '82054f998eadb346f40389947f0bfdaee8ee6d70c35fdf082e240197ae5a6fb9'
CHECKS = 0


def need(value, message):
    global CHECKS
    CHECKS += 1
    if not value:
        raise ValueError(message)


def integer(value, high=2**63-1, low=0):
    need(type(value) is int and low <= value <= high, 'Bounded integer, not boolean')
    return value


def read(path):
    path=Path(path)
    need(not any(p.is_symlink() for p in (path,*path.parents)), 'No symlinked evidence')
    with path.open('rb') as source:
        data=source.read(32*1024*1024+1)
    need(len(data)<=32*1024*1024, 'Bounded evidence bytes')
    return data


def digest(path):
    return hashlib.sha256(read(path)).hexdigest()


def load(path):
    def pairs(items):
        value={}
        for key,item in items:
            need(key not in value, 'Duplicate JSON key')
            value[key]=item
        return value
    def finite(value):
        number=float(value)
        need(math.isfinite(number), 'Nonfinite JSON number')
        return number
    return json.loads(read(path),object_pairs_hook=pairs,parse_float=finite,parse_constant=finite)


def recount(report, seed):
    need(report.get('complete') is True and report.get('stochastic') is True, 'Complete stochastic frozen report')
    need(integer(report.get('seed'))==seed and integer(report.get('cases_per_task'))==32, 'Exact report seed/cases')
    need(integer(report.get('new_training_samples'),0)==0, 'No learning')
    need(report.get('reset_intervention','none')=='none' and report.get('diagnostic_only',False) is False
         and integer(report.get('reset_intervention_trials',0),0)==0, 'Ordinary reset, not assisted diagnostic')
    trials=report.get('trials');need(type(trials) is list and len(trials)==416,'All physical cases')
    by_actor={}
    for trial in trials:
        need(type(trial) is dict,'Physical trial object')
        actor=integer(trial.get('actor'),415);need(actor not in by_actor,'Unique physical actor')
        task=integer(trial.get('task'),12);need(task==actor//32,'Fixed actor/task assignment')
        expected=seed+task*1000003+(actor%32)*104729
        expected=(expected+2**63)%2**64-2**63
        need(integer(trial.get('seed'),2**63-1,-2**63)==expected,'Exact physical case seed')
        need(type(trial.get('success')) is bool,'Boolean physical success')
        integer(trial.get('elapsed_ticks'),low=1)
        detail=trial.get('diagnostics');need(type(detail) is dict,'Physical diagnostics')
        observations=integer(detail.get('observations'),low=1)
        integer(detail.get('dig_decisions'),observations)
        for key in ('blocks_broken','items_collected','observed_max_target_mining_ticks'):
            integer(detail.get(key))
        if task==12:
            menu=detail.get('menu_observations');need(type(menu) is list and len(menu)==5,'Five menu contexts')
            for n in menu:integer(n,observations)
            need(sum(menu)==observations,'Physical menu denominator')
            harvest=detail.get('harvest');need(type(harvest) is dict,'Harvest diagnostics')
            for key in ('held_pick_observations','target_pick_contact_observations','world_dig_selections'):
                integer(harvest.get(key),observations)
            need(harvest['target_pick_contact_observations']<=harvest['held_pick_observations'],'Pick contact needs held pick')
        by_actor[actor]=trial
    bits=[[by_actor[task*32+case]['success'] for case in range(32)] for task in range(13)]
    scores=[sum(outcomes) for outcomes in bits]
    summaries=report.get('tasks');need(type(summaries) is list and len(summaries)==13,'Complete task summaries')
    for task,row in enumerate(summaries):
        need(integer(row.get('task'),12)==task and integer(row.get('cases'))==32
             and integer(row.get('passed'),32)==scores[task],'Recounted task summary')
    mining=[by_actor[a]['diagnostics'] for a in range(384,416)]
    diagnostic=dict(observations=sum(d['observations'] for d in mining),
        menu=[sum(d['menu_observations'][m] for d in mining) for m in range(5)],
        held_pick=sum(d['harvest']['held_pick_observations'] for d in mining),
        pick_contact=sum(d['harvest']['target_pick_contact_observations'] for d in mining),
        world_dig=sum(d['harvest']['world_dig_selections'] for d in mining),
        broken=sum(d['blocks_broken'] for d in mining))
    return dict(scores=scores,outcomes=bits,mining=diagnostic,trials=416,new_training_samples=0)


def decision(data):
    within=[]
    for model in MODELS:
        for seed in SEEDS:
            a,b=data[model,seed,0],data[model,seed,1]
            changes=[b['scores'][t]-a['scores'][t] for t in range(13)]
            flips=[sum(a['outcomes'][t][c]!=b['outcomes'][t][c] for c in range(32)) for t in range(13)]
            within.append(dict(model=model,seed=seed,score_deltas=changes,outcome_flips=flips,
                pickaxe_score_repeatable=abs(changes[11])<=3,pickaxe_case_repeatable=flips[11]<=4))
    gaps=[]
    for seed in SEEDS:
        earlier=[data['control-earlier',seed,r]['scores'][11] for r in (0,1)]
        later=[data['control-later',seed,r]['scores'][11] for r in (0,1)]
        gaps.append(dict(seed=seed,earlier=earlier,later=later,
            later_min_minus_earlier_max=min(later)-max(earlier),earlier_min_minus_later_max=min(earlier)-max(later)))
    classification='not-separated'
    if all(row['later_min_minus_earlier_max']>=4 for row in gaps):classification='later-better'
    if all(row['earlier_min_minus_later_max']>=4 for row in gaps):classification='earlier-better'
    retained=[]
    for model in MODELS[1:]:
        for seed in SEEDS:
            for repeat in (0,1):
                parent=data['parent',seed,repeat]['scores'];actual=data[model,seed,repeat]['scores'];failed=[]
                for task in range(13):
                    floor=max(28,parent[task]-3) if task<11 else max(26,parent[task]-4) if task==11 else max(0,parent[task]-2)
                    if actual[task]<floor:failed.append(dict(task=task,source=parent[task],score=actual[task],floor=floor))
                retained.append(dict(model=model,seed=seed,repeat=repeat,failures=failed,pickaxe_loss=parent[11]-actual[11]))
    parent_ok=True
    for seed in SEEDS:
        for repeat in (0,1):
            parent=data['parent',seed,repeat]['scores']
            parent_ok &= min(parent[:11])>=28 and parent[11]>=26
    comparisons=[dict(seed=seed,repeat=repeat,candidate_minus_control=data['review-later',seed,repeat]['scores'][11]-data['control-later',seed,repeat]['scores'][11])
                 for seed in SEEDS for repeat in (0,1)]
    return dict(parent_qualified=parent_ok,within_policy=within,control_state_gaps=gaps,control_separation=classification,
        pickaxe_score_repeatable=all(r['pickaxe_score_repeatable'] for r in within),
        pickaxe_case_repeatable=all(r['pickaxe_case_repeatable'] for r in within),retention=retained,
        review_vs_later_control=comparisons,review_positive_in_every_pair=all(r['candidate_minus_control']>=4 for r in comparisons),
        new_training_samples=0,deployment=False)


def audit(root):
    root=Path(root);declaration=load(root/'declaration.json')
    need(declaration['seeds']==list(SEEDS) and declaration['repeats']==[0,1] and declaration['tasks']==list(range(13)),'Fixed matrix declaration')
    need(declaration['reports']==16 and declaration['trials']==6656 and declaration['new_training_samples']==0,'Finite report/trial budget')
    claimed=load(root/'reports.json');need(type(claimed) is list and len(claimed)==16,'Complete controller receipts')
    lookup={}
    for row in claimed:
        key=row['model'],row['seed'],row['repeat'];need(key not in lookup,'Unique controller receipt');lookup[key]=row
    records={};identities=[]
    for model_index,model in enumerate(MODELS):
        need(declaration['policies'][model]['sha256']==DIGESTS[model_index],'Pinned declared policy')
        need(digest(declaration['policies'][model]['path'])==DIGESTS[model_index],'Original saved policy immutable')
        for seed_index,seed in enumerate(SEEDS):
            for repeat in (0,1):
                wave=repeat*2+seed_index;port=31710+(model_index-wave)%4
                name=f'{model}-{seed}-repeat{repeat}';folder=root/name
                metadata=load(folder/'metadata.json');raw=load(folder/'result.json')
                need(metadata.get('policy_sha256')==DIGESTS[model_index],'Physical policy identity')
                need(metadata.get('runtime_jar_sha256')==digest(folder/'runtime.jar')==RUNTIME,'Physical common runtime')
                need(metadata.get('exam_jar_sha256')==digest(folder/'server/plugins/exam.jar'),'Physical exam payload')
                need(metadata.get('bind')=='127.0.0.1' and integer(metadata.get('port'))==port,'Rotated localhost slot')
                need(metadata.get('tasks')==list(range(13)) and integer(metadata.get('cases_per_task'))==32 and integer(metadata.get('seed'))==seed,'Physical evaluator matrix')
                need(metadata.get('input_kind')=='inference-policy' and metadata.get('reset_intervention')=='none'
                     and metadata.get('diagnostic_only') is False,'Frozen ordinary evaluation')
                start=load(root/(name+'-started.json'))
                need(start['source']==declaration['source'] and start['model']==model and start['seed']==seed
                     and start['repeat']==repeat and start['wave']==wave and start['port']==port,'Exact start receipt')
                data=folder/'server/plugins/BotsClustersMC'
                need(digest(data/'policy.bcmc')==DIGESTS[model_index],'Unchanged evaluated policy')
                need(not (data/'exam-failed.txt').exists() and not list(data.glob('training*.bcmc')),'No failed exam or training checkpoint')
                actual=recount(raw,seed);key=model,seed,repeat;records[key]=actual
                need(key in lookup,'Missing controller receipt identity')
                for field,value in actual.items():need(lookup[key][field]==value,'Independent raw recount: '+field)
                need(lookup[key]['report_sha256']==digest(folder/'result.json') and lookup[key]['metadata_sha256']==digest(folder/'metadata.json'),'Raw receipt binding')
                identities.append(dict(model=model,seed=seed,repeat=repeat,policy_sha256=DIGESTS[model_index],
                    report_sha256=digest(folder/'result.json'),metadata_sha256=digest(folder/'metadata.json'),
                    scores=actual['scores'],mining=actual['mining']))
    conclusion=decision(records)
    need(conclusion==load(root/'outcome.json'),'Independent entire interpretation exactly matches controller')
    return dict(source=declaration['source'],reports=16,frozen_trials=6656,new_training_samples=0,
        verified_report_identities=identities,decision=conclusion,checks=CHECKS,deployment=False,
        limitation='Raw case identity, outcomes, primary scores, secondary mining/menu counts, artifact bindings and full decision recounted. Full high-dimensional crafting probability trace validation is performed by the existing frozen harness, not duplicated here.')


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('directory',type=Path);args=parser.parse_args()
    print(json.dumps(audit(args.directory),indent=2,allow_nan=False))


if __name__=='__main__':
    main()
