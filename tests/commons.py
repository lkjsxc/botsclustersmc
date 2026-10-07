#!/usr/bin/env python3
"""Opt-in neural two-citizen exam. Python is a developer-test dependency only."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import zipfile
import acceptance

ROOT=Path(__file__).resolve().parents[1]
CONDITIONS=('split-shared','pooled-shared','split-isolated')
PROTOCOL='commons-fixed-stations-v1'
SCHEMA='bcmc-citizen-egocentric-context'


def require(ok, message):
    if not ok:
        raise ValueError(message)


def integer(value, low=0, high=2**63-1):
    require(type(value) is int and low<=value<=high, 'Invalid bounded integer')
    return value


def vector(value, count, maximum=4096):
    require(type(value) is list and len(value)==count, 'Invalid vector shape')
    return [integer(x,0,maximum) for x in value]


def add(*stocks):
    return [sum(s[i] for s in stocks) for i in range(3)]


def units(stock):
    return 2*stock[0]+stock[1]+8*stock[2]


def validate(report, cases, seed, horizon):
    """Derive every summary from the closed source budget and complete room/member records."""
    require(type(report) is dict and report.get('complete') is True, 'Incomplete commons report')
    require(report.get('protocol')==PROTOCOL and report.get('schema')==SCHEMA, 'Wrong protocol/schema')
    require(report.get('stochastic') is True and type(report.get('new_training_samples')) is int and report['new_training_samples']==0, 'Exam must not train')
    require(integer(report.get('cases_per_condition'),4,32)==cases and cases%4==0, 'Wrong balanced case bank')
    require(integer(report.get('horizon_ticks'),40,3000)==horizon, 'Wrong horizon')
    require(integer(report.get('seed'),-(2**63),2**63-1)==seed, 'Wrong seed')
    integer(report.get('policy_updates'));integer(report.get('policy_trained_samples'))
    trials=report.get('trials');require(type(trials) is list and len(trials)==cases*3,'Missing/duplicate trial bank')
    passed=[0,0,0]
    for trial_index,t in enumerate(trials):
        condition=CONDITIONS[trial_index//cases];case=trial_index%cases
        require(type(t) is dict and integer(t.get('trial'))==trial_index and integer(t.get('case'))==case,'Wrong ordered trial identity')
        require(t.get('condition')==condition and integer(t.get('seed'),-(2**63),2**63-1)==seed+case*104729,'Wrong paired condition seed')
        require(integer(t.get('supplies_owner'),0,1)==case%2 and type(t.get('mirror')) is bool and t['mirror']==((case//2)%2!=0),'Unbalanced supplies/mirror')
        rooms=t.get('rooms');isolated=condition=='split-isolated'
        require(type(rooms) is list and len(rooms)==(2 if isolated else 1),'Incomplete room bank')
        total=[0,0,0];bank=[0,0,0];crafted=0;ticks=[];ends=[]
        for m,r in enumerate(rooms):
            require(type(r) is dict and integer(r.get('room'))==trial_index*2+m,'Wrong room identity')
            rt=integer(r.get('ticks'),1,horizon);ticks.append(rt)
            end=r.get('end');require(end in ('delivered','horizon','escaped'),'Invalid room termination');ends.append(end)
            if end=='horizon':require(rt==horizon,'Premature horizon')
            if end=='delivered':require(not isolated,'Isolated split room cannot deliver a complete tool')
            carried=vector(r.get('carried'),3);stock=vector(r.get('bank'),3);dropped=vector(r.get('dropped'),3)
            inventory=vector(r.get('member_carried'),6)
            require(add(inventory[:3],inventory[3:])==carried,'Member inventories do not conserve carried stock')
            actor_ticks=vector(r.get('actor_ticks'),2,horizon+2)
            decisions=vector(r.get('decisions'),2,horizon);gui=vector(r.get('gui_selections'),12,horizon)
            chest=vector(r.get('chest_observations'),2,horizon)
            for member in range(2):
                require(sum(gui[member*6:member*6+6])==decisions[member] and chest[member]<=decisions[member],'Input/observation denominator mismatch')
                require(decisions[member]<=actor_ticks[member] and actor_ticks[member]<=rt+2,'Actor effort exceeds room horizon')
                if isolated and member!=m:
                    require(inventory[member*3:member*3+3]==[0,0,0] and actor_ticks[member]==decisions[member]==chest[member]==0,'Foreign member in isolated room')
                else:
                    require(actor_ticks[member]>0,'Participant never started')
            made=integer(r.get('crafted_picks'),0,1);crafted+=made
            room_total=add(carried,stock,dropped)
            supply=(6 if m==case%2 else 2) if isolated else 8
            require(units(room_total)<=supply,'Created resource units')
            if isolated:require(made==0 and room_total[2]==0,'Isolated actor fabricated missing recipe ingredients')
            if end=='delivered':require(made==1 and stock==[0,0,1] and room_total==[0,0,1],'Forged delivery termination')
            total=add(total,room_total);bank=add(bank,stock)
        require(crafted<=1 and total[2]<=crafted and units(total)<=8,'Excess team resources/crafts')
        success=crafted==1 and bank==[0,0,1] and total==[0,0,1] and ends==['delivered']
        require(type(t.get('success')) is bool and t['success']==success,'Forged completion')
        require(vector(t.get('total'),3)==total and vector(t.get('bank'),3)==bank,'Forged team stock')
        require(integer(t.get('elapsed_ticks'),1,horizon)==max(ticks),'Wrong team elapsed ticks')
        require(integer(t.get('lost_wood_units'),0,8)==8-units(total),'Wrong source loss accounting')
        passed[trial_index//cases]+=int(success)
    expected=[{'condition':c,'cases':cases,'passed':passed[i]} for i,c in enumerate(CONDITIONS)]
    actual=report.get('conditions');require(type(actual) is list and len(actual)==3,'Missing condition summaries')
    for actual_row,row in zip(actual,expected):
        require(type(actual_row) is dict and actual_row.get('condition')==row['condition'],'Wrong summary condition')
        require(integer(actual_row.get('cases'))==cases and integer(actual_row.get('passed'))==row['passed'],'Forged summary')
    return expected


def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda:f.read(65536),b''):h.update(block)
    return h.hexdigest()


def arguments(argv=None):
    p=argparse.ArgumentParser(description=__doc__)
    source=p.add_mutually_exclusive_group(required=True)
    source.add_argument('--fresh-policy-seed',type=int,help='Explicitly untrained software smoke check, never current-policy qualification')
    source.add_argument('--policy',type=Path,help='Inference-only policy file; no checkpoint exporter or learner is used')
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--cache',type=Path,default=Path(os.environ.get('BCMC_SERVER_CACHE',ROOT/'.cache/server')))
    p.add_argument('--cases',type=int,default=4)
    p.add_argument('--seed',type=int,default=2026100711)
    p.add_argument('--horizon',type=int,default=3000)
    p.add_argument('--port',type=int,default=25586)
    a=p.parse_args(argv)
    require(os.environ.get('EULA')=='true','Explicit Minecraft EULA consent (EULA=true) is required')
    require(4<=a.cases<=32 and a.cases%4==0,'Cases must balance resource swap and station mirror in groups of four')
    require(-(2**63)<=a.seed and a.seed+(a.cases-1)*104729<2**63,'Seed schedule overflow')
    require(40<=a.horizon<=3000 and 1024<=a.port<=65535 and a.port!=25565,'Invalid horizon or non-production port')
    if a.fresh_policy_seed is not None:integer(a.fresh_policy_seed,-(2**63),2**63-1)
    return a


def main(argv=None):
    a=arguments(argv)
    output=a.output.absolute()
    for parent in (output,*output.parents):require(not parent.is_symlink(),'Output path contains a symbolic link')
    output=output.resolve()
    require(output.is_relative_to(ROOT/'.build') and output!=ROOT/'.build','Use a new disposable output inside this checkout .build directory')
    require(not output.exists(),'Output must be new; previous evidence is never overwritten')
    cache=a.cache.resolve(strict=True);runtime=ROOT/'dist/botsclustersmc.jar'
    require(runtime.is_file() and (cache/'server.jar').is_file(),'Build the inference runtime and prepare the pinned server cache first')
    source=None
    if a.policy is not None:
        source=a.policy.resolve(strict=True)
        require(source.is_file() and source.stat().st_size<=8*1024*1024,'Invalid inference policy file')
        require(source.parent not in output.parents and output!=source.parent,'Output cannot be inside the policy directory')
    with socket.socket() as check:check.bind(('127.0.0.1',a.port))
    output.mkdir(parents=True,exist_ok=False)
    server=output/'server';acceptance.server_dir(server,cache,a.port)
    props=server/'server.properties'
    lines=[line for line in props.read_text().splitlines() if not line.startswith(('generator-settings=','level-type='))]
    flat={'layers':[{'block':'minecraft:bedrock','height':1},{'block':'minecraft:dirt','height':2},{'block':'minecraft:grass_block','height':1}],'biome':'minecraft:plains'}
    props.write_text('\n'.join(lines+['level-type=minecraft:flat','generator-settings='+json.dumps(flat),'max-players=0','white-list=true','enforce-whitelist=true'])+'\n')
    (server/'.botsclustersmc-commons').write_text('Disposable neural commons only.\n')
    data=server/'plugins/BotsClustersMC';data.mkdir()
    shutil.copy2(runtime,output/'runtime.jar')
    classes=output/'classes';classes.mkdir()
    java=acceptance.JAVA;javac=str(Path(java).with_name('javac')) if os.path.sep in java else 'javac'
    libraries=[str(f) for name in ('libraries','versions') for f in (cache/name).rglob('*.jar')]
    sources=sorted((ROOT/'tests/commons').rglob('*.java'))
    with (output/'compile.log').open('w') as log:
        subprocess.run([javac,'--release','21','-proc:none','-cp',os.pathsep.join([str(output/'runtime.jar'),*libraries]),'-d',str(classes),*map(str,sources)],check=True,stdout=log,stderr=subprocess.STDOUT)
    policy=data/'policy.bcmc'
    if source is None:
        with (output/'policy-origin.log').open('w') as log:
            subprocess.run([java,'-cp',os.pathsep.join([str(classes),str(output/'runtime.jar')]),'org.botsclustersmc.commons.CommonsPolicy',str(a.fresh_policy_seed),str(policy)],check=True,stdout=log,stderr=subprocess.STDOUT)
    else:
        policy.write_bytes(source.read_bytes())
    with (output/'policy-verify.log').open('w') as log:
        subprocess.run([java,'-cp',str(output/'runtime.jar'),'org.botsclustersmc.core.PolicyTool','verify',str(policy)],check=True,stdout=log,stderr=subprocess.STDOUT)
    original=digest(policy)
    descriptor="name: BotsClustersMC\nversion: 'commons-exam'\nmain: org.botsclustersmc.commons.CommonsExam\napi-version: '1.21'\nfolia-supported: true\ncommands:\n  bots:\n    description: Frozen neural commons diagnostics\npermissions:\n  botsclustersmc.observe:\n    default: op\n  botsclustersmc.admin:\n    default: op\n"
    config=f'max-agents: {a.cases*6}\nmax-loaded-chunks: {max(64,a.cases*8)}\ninference-threads: 1\nseed: {a.seed}\ncases-per-condition: {a.cases}\nhorizon-ticks: {a.horizon}\nworld-edits: false\n'
    target=server/'plugins/commons.jar'
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as jar:
        with zipfile.ZipFile(output/'runtime.jar') as original_jar:
            for entry in original_jar.infolist():
                require('/training/' not in entry.filename and '/commons/' not in entry.filename,'Inference artifact separation failed')
                if entry.filename.startswith('org/'):jar.writestr(entry,original_jar.read(entry.filename))
        for file in sorted(classes.rglob('*.class')):jar.write(file,file.relative_to(classes).as_posix())
        jar.writestr('plugin.yml',descriptor);jar.writestr('config.yml',config)
    metadata={'protocol':PROTOCOL,'cases':a.cases,'seed':a.seed,'horizon':a.horizon,'fresh_policy_seed':a.fresh_policy_seed,
        'policy_origin':'fresh-untrained' if source is None else 'supplied-inference-policy','policy_sha256':original,
        'runtime_sha256':digest(output/'runtime.jar'),'exam_sha256':digest(target),'server_sha256':digest(cache/'server.jar'),
        'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'source_files':{str(f.relative_to(ROOT)):digest(f) for f in [*sources,Path(__file__)]}}
    (output/'metadata.json').write_text(json.dumps(metadata,indent=2)+'\n')
    process=acceptance.direct(server,cache,output/'server.log','-Dbcmc.commons=true')
    started=time.monotonic();history=[];last_epoch=0
    try:
        while process.poll() is None:
            failure=data/'commons-failed.txt'
            require(not failure.exists(),'Commons runtime failure; evidence preserved in '+str(output))
            require(time.monotonic()-started<600,'Commons server timeout; evidence preserved')
            status=acceptance.read_status(data/'status.json')
            if status and status.get('epoch_millis',0)>last_epoch:
                last_epoch=status['epoch_millis'];history.append(status)
                require(len(history)<=200,'Bounded status history exceeded')
                (output/'status-series.json').write_text(json.dumps(history,indent=2)+'\n')
                require(status.get('state')!='failed' and status.get('inference_failed')==0,'Runtime/inference failure')
            time.sleep(.5)
        require(process.returncode==0 and not (data/'commons-failed.txt').exists(),'Commons server exited unsuccessfully')
        report=json.loads((data/'commons-result.json').read_text())
        summary=validate(report,a.cases,a.seed,a.horizon)
        if source is None:require(report['policy_updates']==report['policy_trained_samples']==0,'Fresh smoke policy was trained')
        require(digest(policy)==original,'Frozen policy was changed')
        require(not list(server.rglob('training.bcmc')),'Exam created training state')
        require(history and history[-1]['inference_completed']>0,'No real neural inference occurred')
        (output/'result.json').write_text(json.dumps(report,indent=2)+'\n')
        (output/'integrity.json').write_text(json.dumps({'integrity_passed':True,'new_training_samples':0,'seconds':time.monotonic()-started,'conditions':summary},indent=2)+'\n')
        print(json.dumps({'integrity':'passed','policy_origin':metadata['policy_origin'],'conditions':summary,'output':str(output)},indent=2),flush=True)
    finally:
        acceptance.stop_direct(process)


if __name__=='__main__':
    main()
