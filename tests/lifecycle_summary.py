#!/usr/bin/env python3
"""Compare complete paired lifecycle observations without reading live Academy state."""
import argparse
import json
import math
from collections import Counter
from pathlib import Path


def summarize(report):
    if report.get('complete') is not True or report.get('new_training_samples') != 0 or report.get('order') != [11,12,11]:
        raise ValueError('A completed no-learning lifecycle report is required')
    count=report['cases_per_arm'];trials=report['trials']
    if not isinstance(count,int) or not 1<=count<=64 or len(trials)!=count*6:
        raise ValueError('Incomplete trial count')
    keyed={}
    for trial in trials:
        key=(trial['arm'],trial['phase'],trial['case'])
        if key in keyed or key[0] not in ('EXAM','PROBE') or key[1] not in range(3) or key[2] not in range(count):
            raise ValueError('Invalid/duplicate trial identity')
        if trial['task']!=[11,12,11][key[1]] or type(trial['success']) is not bool:
            raise ValueError('Invalid task/outcome')
        if len(trial['initial'])!=512 or len(trial['initial_mask'])!=233:
            raise ValueError('Unexpected observation/mask shape')
        if any(not math.isfinite(v) for v in trial['initial']) or any(type(v) is not bool for v in trial['initial_mask']):
            raise ValueError('Invalid observation/mask values')
        if trial['elapsed_ticks']<=0 or trial['decisions']<=0:
            raise ValueError('Empty episode')
        keyed[key]=trial
    result={'policy_updates':report['policy_updates'],'policy_samples':report['policy_samples'],'seed':report['seed'],'counts':[],'pairs':[]}
    for arm in ('EXAM','PROBE'):
        for phase in range(3):
            rows=[keyed[(arm,phase,i)] for i in range(count)]
            result['counts'].append({'arm':arm,'phase':phase,'task':[11,12,11][phase],
                'passed':sum(r['success'] for r in rows),'cases':count,
                'mean_transition_ticks':sum(r['sum_transition_ticks'] for r in rows)/sum(r['decisions'] for r in rows),
                'initial_menus':dict(Counter(r['initial'][42] for r in rows)),
                'initial_crafted_nonzero':sum(r['initial'][330]!=0 for r in rows)})
    for left,right in [(('EXAM',0),('PROBE',0)),(('EXAM',2),('PROBE',2)),(('EXAM',0),('EXAM',2)),(('PROBE',0),('PROBE',2))]:
        differences=Counter();examples={};masks=0;discordance=Counter()
        for i in range(count):
            a,b=keyed[(*left,i)],keyed[(*right,i)]
            if a['action_seed']!=b['action_seed']:raise ValueError('Paired action RNG was not matched')
            salt=lambda arm:0x677d815fab35 if arm=='EXAM' else 0x173a6ae017a1
            if a['reset_seed']^salt(left[0]) != b['reset_seed']^salt(right[0]):
                raise ValueError('Paired reset RNG was not matched')
            masks+=a['initial_mask']==b['initial_mask']
            discordance[str(a['success'])+'->'+str(b['success'])]+=1
            for index,(x,y) in enumerate(zip(a['initial'],b['initial'])):
                if abs(x-y)>1e-6:
                    differences[index]+=1;examples.setdefault(index,[x,y])
        result['pairs'].append({'left':left,'right':right,'cases':count,'identical_masks':masks,
            'outcomes':dict(discordance),'initial_differences':[{'index':i,'cases':n,'example':examples[i]} for i,n in sorted(differences.items())]})
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('report',type=Path);args=parser.parse_args()
    if not args.report.is_file() or args.report.stat().st_size>4*1024*1024:
        raise ValueError('Use a completed report of at most 4 MiB')
    print(json.dumps(summarize(json.loads(args.report.read_text())),indent=2))


if __name__=='__main__':main()
