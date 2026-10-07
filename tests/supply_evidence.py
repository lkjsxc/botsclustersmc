#!/usr/bin/env python3
"""Independent finite-inventory replay; synthetic evidence is never a Minecraft policy certificate."""
from __future__ import annotations
import argparse, csv, hashlib, itertools, json, math, re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

SEEDS=(2026100801,2026100802,2026100803)
EVAL=(2026100881,2026100882)
BOUNDARIES=(0,100,300,600)
ARMS=('visible','hidden')
POPULATIONS=(2,8)
FIELDS='arm learning_seed boundary eval_seed population case updates samples policy_sha256 first initial actions orders service_steps consumed bank remaining deposited withdrawn wrong'.split()
UPDATE_FIELDS='arm learning_seed attempt offered accepted updates samples backtracks learning_rate mean_kl max_kl'.split()
@dataclass(frozen=True)
class Profile:
    identity: str
    seeds: tuple[int,...]
    evals: tuple[int,...]
    arms: tuple[str,str]
    evidence_schema: str

SUPPLY=Profile('synthetic-communal-supply-memory-only',SEEDS,EVAL,ARMS,'synthetic-communal-supply-evidence-v1')
BINDING=Profile('synthetic-communal-tensor-binding-memory-only',(2026100811,2026100812,2026100813),(2026100891,2026100892),('tensor','plain'),'synthetic-tensor-binding-evidence-v1')

MASK=(1<<64)-1
MIX=0x9e3779b97f4a7c15


def require(condition: bool, message: str) -> None:
    if not condition: raise ValueError(message)


def integer(text: str) -> int:
    require(isinstance(text,str) and re.fullmatch(r'0|[1-9][0-9]{0,15}',text) is not None,'noncanonical bounded integer')
    return int(text)


def vector(text: str, length: int) -> list[int]:
    values=[integer(s) for s in text.split(',')]
    require(len(values)==length,'vector length')
    return values


class Random:
    def __init__(self,seed: int): self.state=seed&MASK
    def number(self,bound: int) -> int:
        self.state=(self.state+MIX)&MASK
        z=self.state;z=((z^(z>>30))*0xbf58476d1ce4e5b9)&MASK
        z=((z^(z>>27))*0x94d049bb133111eb)&MASK;z^=z>>31
        return math.floor((z>>11)*2**-53*bound)


def key(seed: int,case: int,step: int,member: int) -> int:
    return (seed^(case*MIX)^(step*0xbf58476d1ce4e5b9)^(member*0x94d049bb133111eb))&MASK


def shuffle(values: list[int],rng: Random) -> list[int]:
    for i in range(len(values)-1,0,-1):
        j=rng.number(i+1);values[i],values[j]=values[j],values[i]
    return values


def scenario(seed: int,case: int,n: int) -> tuple[list[int],list[int]]:
    r=Random(key(seed,case,-1,0))
    initial=shuffle([(0 if i<n//2 else 2)+r.number(2) for i in range(n)],r)
    orders=[]
    for step in range(4):orders+=shuffle(list(range(n)),Random(key(seed,case,step,-1)))
    return initial,orders


def replay(initial: list[int],first: int,actions: list[int],orders: list[int], *, check_masks: bool=True) -> dict:
    """Integer reference model, deliberately not using Java/Pocket or reported aggregates."""
    n=len(initial);require(n in POPULATIONS and first in (0,1),'room dimensions')
    require(len(actions)==len(orders)==4*n,'trace dimensions')
    require(all(0<=code<=3 for code in initial+actions),'primitive/initial code')
    require(sum(code//2==0 for code in initial)==n//2,'unbalanced initial resources')
    pockets=[[[None,0] for _ in range(36)] for _ in range(n)]
    for i,code in enumerate(initial):pockets[i][code%2]=[code//2,1]
    bank=[None,0];served=[0,0];service=[0,0];deposited=[0]*(n*2);withdrawn=[0]*(n*2);phase=wrong=0
    for step in range(4):
        order=orders[step*n:(step+1)*n];require(sorted(order)==list(range(n)),'invalid application order')
        demand=(first^phase) if phase<2 else None
        if check_masks:
            for i in range(n):
                a=actions[step*n+i]
                if a in (1,2):
                    k,c=pockets[i][a-1]
                    require(c>0 and (bank[1]==0 or bank[0]==k) and bank[1]<64,'action outside pre-action mechanical mask')
                elif a==3:require(bank[1]>0,'withdrawal outside pre-action mechanical mask')
        for i in order:
            a=actions[step*n+i]
            if a in (1,2):
                item=pockets[i][a-1];k,c=item
                if c>0 and (bank[1]==0 or bank[0]==k) and bank[1]<64:
                    moved=min(c,64-bank[1]);bank=[k,bank[1]+moved];item[1]-=moved
                    if item[1]==0:item[0]=None
                    deposited[i*2+k]+=moved
                    if k!=demand:wrong+=moved
            elif a==3 and bank[1]>0:
                k,remaining=bank
                for mode in (0,1):
                    for item in pockets[i]:
                        if (mode==0 and item[1]>0 and item[0]==k) or (mode==1 and item[1]==0):
                            moved=min(remaining,64-item[1])
                            if moved:item[0]=k;item[1]+=moved;remaining-=moved;withdrawn[i*2+k]+=moved
                bank=[k,remaining] if remaining else [None,0]
            for k in (0,1):
                total=served[k]+(bank[1] if bank[0]==k else 0)+sum(c for p in pockets for kind,c in p if kind==k)
                require(total==n//2,'reference conservation failure')
        if demand is not None and bank==[demand,n//2]:
            served[demand]+=bank[1];bank=[None,0];phase+=1;service[demand]=step+1
    remaining=[sum(c for kind,c in p if kind==k) for p in pockets for k in (0,1)]
    stock=[bank[1] if bank[0]==k else 0 for k in (0,1)]
    return {'service_steps':service,'consumed':served,'bank':stock,'remaining':remaining,
            'deposited':deposited,'withdrawn':withdrawn,'wrong':wrong}


def validate_trial(row: dict,profile: Profile=SUPPLY) -> tuple[tuple,dict,tuple]:
    require(set(row)==set(FIELDS),'trial fields')
    arm=row['arm'];seed=integer(row['learning_seed']);b=integer(row['boundary']);ev=integer(row['eval_seed']);n=integer(row['population']);case=integer(row['case'])
    require(arm in profile.arms and seed in profile.seeds and b in BOUNDARIES and ev in profile.evals and n in POPULATIONS and case<256,'undeclared trial')
    require(re.fullmatch(r'[0-9a-f]{64}',row['policy_sha256']) is not None,'policy digest')
    first=integer(row['first']);require(first==case%2,'unbalanced request schedule')
    initial=vector(row['initial'],n);orders=vector(row['orders'],4*n);actions=vector(row['actions'],4*n)
    require((initial,orders)==scenario(ev,case,n),'scenario/application-order identity')
    actual=replay(initial,first,actions,orders)
    for field,value in actual.items():
        reported=integer(row[field]) if field=='wrong' else vector(row[field],len(value))
        require(reported==value,'forged '+field)
    identity=(row['policy_sha256'],integer(row['updates']),integer(row['samples']))
    return (arm,seed,b,ev,n,case),actual,identity


def records(path: Path,fields: list[str]):
    require(path.is_file() and not path.is_symlink() and path.stat().st_size<=64*1024*1024,'evidence file boundary')
    with path.open(encoding='utf-8',newline='') as f:
        reader=csv.DictReader(f,delimiter='\t');require(reader.fieldnames==fields,'evidence header')
        for row in reader:
            require(None not in row and all(v is not None and len(v)<16384 for v in row.values()),'malformed row')
            yield row


def training(path: Path,profile: Profile=SUPPLY) -> dict:
    seen=set();counts={};snapshots={}
    for row in records(path,UPDATE_FIELDS):
        require(row['arm'] in profile.arms,'unknown update arm');arm=row['arm'];seed=integer(row['learning_seed']);attempt=integer(row['attempt'])
        require(seed in profile.seeds and 1<=attempt<=600,'undeclared update');k=(arm,seed,attempt)
        require(k not in seen,'duplicate update');seen.add(k)
        accepted=integer(row['accepted']);require(integer(row['offered'])==256 and accepted in (0,256),'offered/accepted count')
        old=counts.get((arm,seed),(0,0,0));require(attempt==old[0]+1,'noncontiguous updates')
        updates=old[1]+(accepted>0);samples=old[2]+accepted
        require(integer(row['updates'])==updates and integer(row['samples'])==samples,'optimizer/sample identity')
        backtracks=integer(row['backtracks']);rate=float(row['learning_rate']);mean=float(row['mean_kl']);maximum=float(row['max_kl'])
        require(all(math.isfinite(v) for v in (rate,mean,maximum)),'non-finite update')
        require(0<=mean<=maximum and mean<=.005 and maximum<=.05,'policy guard bounds')
        require((accepted==0 and backtracks==12 and rate==0) or (accepted==256 and backtracks<12 and 0<rate<=.00015),'guard result')
        counts[(arm,seed)]=(attempt,updates,samples)
        if attempt in BOUNDARIES:snapshots[(arm,seed,attempt)]=(updates,samples)
    require(len(seen)==3600 and set(counts)==set(itertools.product(profile.arms,profile.seeds)) and all(v[0]==600 for v in counts.values()),'incomplete learning matrix')
    for arm,seed in itertools.product(profile.arms,profile.seeds):snapshots[(arm,seed,0)]=(0,0)
    return snapshots


def validate(directory: Path,profile: Profile=SUPPLY) -> dict:
    require((directory/'identity.txt').read_text().splitlines()[0]==profile.identity,'synthetic identity')
    require((directory/'completed.txt').read_text()=='All declared trajectories and evaluation cases completed. Learning gates require independent validation.\n','missing completion record')
    checkpoints=training(directory/'updates.tsv',profile);seen=set();identities={};stats={}
    for row in records(directory/'trials.tsv',FIELDS):
        k,result,identity=validate_trial(row,profile);require(k not in seen,'duplicate trial');seen.add(k)
        arm,seed,b,ev,n,case=k;binding=k[:3]
        require(identity[1:]==checkpoints[binding],'evaluation/optimizer checkpoint mismatch')
        require(binding not in identities or identities[binding]==identity,'changed frozen policy inside evaluation');identities[binding]=identity
        group=k[:5]
        s=stats.setdefault(group,{'cases':0,'two_step':0,'four_step':0,'first_step':0,'wrong_deposits':0,'consumed':[0,0],'remaining':[0,0],'bank':[0,0]})
        steps=result['service_steps'];s['cases']+=1;s['two_step']+=all(0<v<=2 for v in steps);s['four_step']+=all(v>0 for v in steps)
        s['first_step']+=steps[case%2]==1;s['wrong_deposits']+=result['wrong']
        for material in (0,1):
            s['consumed'][material]+=result['consumed'][material];s['bank'][material]+=result['bank'][material]
            s['remaining'][material]+=sum(result['remaining'][material::2])
    expected=256*len(profile.arms)*len(profile.seeds)*len(BOUNDARIES)*len(profile.evals)*len(POPULATIONS)
    require(len(seen)==expected and len(stats)==expected//256 and all(s['cases']==256 for s in stats.values()),'incomplete evaluation matrix')
    candidate,control=profile.arms
    for seed in profile.seeds:require(identities[candidate,seed,0]==identities[control,seed,0],'unmatched initial policies')
    gates=[]
    for seed in profile.seeds:
        cells=[]
        for ev in profile.evals:
            visible=stats[candidate,seed,600,ev,2]['two_step']/256
            hidden=stats[control,seed,600,ev,2]['two_step']/256
            larger=stats[candidate,seed,600,ev,8]['two_step']/256
            cells.append({'eval_seed':ev,candidate+'_two_step':visible,control+'_two_step':hidden,'paired_difference':visible-hidden,'eight_member_two_step':larger,'passed':visible>=.9 and visible-hidden>=.2 and larger>=.8})
        gates.append({'learning_seed':seed,'passed':all(c['passed'] for c in cells),'evaluations':cells})
    return {'schema':profile.evidence_schema,'complete_trials':len(seen),'all_material_histories_reconstructed':True,'minecraft':False,
            'all_learning_gates_passed':all(g['passed'] for g in gates),'gates':gates,'metrics':{'/'.join(map(str,k)):v for k,v in stats.items()},
            'evidence_sha256':{name:hashlib.sha256((directory/name).read_bytes()).hexdigest() for name in ('identity.txt','completed.txt','trials.tsv','updates.tsv')}}


def main():
    p=argparse.ArgumentParser();p.add_argument('directory',type=Path);p.add_argument('--output',type=Path,required=True);p.add_argument('--study',choices=('supply','binding'),default='supply');args=p.parse_args()
    result=validate(args.directory,BINDING if args.study=='binding' else SUPPLY)
    with args.output.open('x',encoding='utf-8') as out:json.dump(result,out,indent=2);out.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k!='metrics'},indent=2))

if __name__=='__main__':main()
