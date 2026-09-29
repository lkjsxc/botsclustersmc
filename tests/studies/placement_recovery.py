"""Reuse every completed baseline after the disclosed count-shape runner failure."""
import hashlib, json, os
from types import SimpleNamespace
import holdout

DECLARATION='fd392dab3dac602f1d6de9a4f64c29f715f7fce022b00380970d0da662fd1ae5'
FAILURE='50e9c982735c6a838ab6ddf09048e8d91ff100c333ea415d3eb0a4dfe93a2049'
STOPPED='e3422c34e7a27e2844668d755bada28043394611ba736c5e89bd9f38453fff79'
SOURCE='ca75ffcc03348083e282406a73d0e14964c47444'

def require(ok,message):
    if not ok:raise AssertionError(message)
def sha(path):
    require(not any(p.is_symlink() for p in (path,*path.parents)),'symlinked recovery input')
    return hashlib.sha256(holdout.read_bounded(path,32*1024*1024)).hexdigest()
def save(path,data):
    require(not any(p.is_symlink() for p in (path,*path.parents)),'symlinked recovery output')
    with path.open('xb') as stream:
        stream.write((json.dumps(data,indent=2,allow_nan=False)+'\n').encode());stream.flush();os.fsync(stream.fileno())

def reuse_baselines(previous,output,inputs,runtime,input_shas,seeds,tasks,cases,cells):
    require(sha(previous/'declaration.json')==DECLARATION,'different original declaration')
    require(sha(previous/'failure.json')==FAILURE,'different operational failure')
    require(sha(previous/'control/250000/stopped-training.bcmc')==STOPPED,'different stopped operational checkpoint')
    declaration=holdout.read_report(previous/'declaration.json')
    require(declaration['source']==SOURCE and declaration['input_files']==input_shas,'different original source/input')
    require(declaration['reference_runtime']==runtime and declaration['seeds']==seeds and declaration['tasks']==tasks and declaration['cases']==cases,'different baseline plan')
    require(holdout.read_report(previous/'failure.json')['error']=='invalid per-task counts','different failure reason')
    require(not (previous/'candidate').exists() and not (previous/'control/250000/completed.json').exists(),'prior continuation progressed')
    require(not (previous/'evaluation-early').exists() and not (previous/'completed.json').exists(),'prior comparison outcomes exist')
    groups=[('evaluation-baseline',['base','protected'],tasks,cases,'none')]
    groups += [('diagnostic-baseline-'+cell,['base'],[11],32,cell) for cell in cells]
    provenance=[]
    for group,models,selected,count,intervention in groups:
        original=previous/group;receipts=holdout.read_report(original/'completed.json')
        expected={(model,seed) for model in models for seed in seeds};seen=set()
        require(type(receipts) is list and len(receipts)==len(expected),'incomplete baseline receipts')
        for receipt in receipts:
            pair=(receipt['arm'],receipt['seed']);require(pair in expected and pair not in seen,'duplicate or unplanned baseline')
            seen.add(pair);model,seed=pair;directory=original/f'{model}-{seed}'
            args=SimpleNamespace(tasks=selected,cases=count,seed=seed,reset_intervention=intervention)
            report=holdout.verify_report(args,holdout.read_report(directory/'result.json'))
            metadata=holdout.read_report(directory/'metadata.json');policy=inputs/(model+'-policy.bcmc')
            require(metadata['policy_sha256']==sha(policy)==sha(directory/'server/plugins/BotsClustersMC/policy.bcmc'),'baseline model changed')
            require(metadata['runtime_jar_sha256']==runtime==sha(directory/'runtime.jar'),'baseline runtime changed')
            require(metadata['tasks']==selected and metadata['cases_per_task']==count and metadata['seed']==seed,'baseline metadata plan')
            require(metadata['reset_intervention']==intervention and metadata['diagnostic_only'] is (intervention!='none'),'baseline assistance identity')
            require(not list((directory/'server/plugins/BotsClustersMC').glob('training*.bcmc')),'baseline wrote training state')
            require(receipt['path']==str(directory/'result.json') and receipt['report_sha256']==sha(directory/'result.json'),'baseline report identity')
            require(receipt['scores']==[row['passed'] for row in report['tasks']] and receipt['tasks']==selected and receipt['cases']==count,'baseline receipt scores/scope')
            require(type(receipt['trials']) is int and receipt['trials']==count*len(selected) and type(receipt['new_training_samples']) is int and receipt['new_training_samples']==0,'baseline receipt sample count')
            require(receipt['intervention']==intervention and receipt['policy_sha256']==sha(policy),'baseline receipt model/assistance')
            provenance.append({'report':str(directory/'result.json'),'report_sha256':sha(directory/'result.json'),'reused_not_rerun':True})
        require(seen==expected,'missing baseline')
        destination=output/group;destination.mkdir();save(destination/'completed.json',receipts)
    require(len(provenance)==14,'incomplete original baseline matrix')
    save(output/'baseline-reuse.json',{'original_source':SOURCE,'declaration_sha256':DECLARATION,'failure_sha256':FAILURE,
        'stopped_checkpoint_sha256':STOPPED,'excluded_operational_samples':4,'reports':provenance,
        'ordinary_reports':4,'ordinary_trials':3072,'assisted_reports':10,'assisted_trials':320})
