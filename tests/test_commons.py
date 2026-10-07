import copy
import json
import os
import subprocess
import sys
import unittest
from pathlib import Path
import commons


def report():
    trials=[]
    for condition in commons.CONDITIONS:
        for case in range(4):
            index=len(trials);isolated=condition=='split-isolated';rooms=[]
            for m in range(2 if isolated else 1):
                inventories=[]
                for member in range(2):
                    active=not isolated or member==m
                    inventories += [3 if active and member==case%2 else 0,
                        2 if active and member==(case%2 if condition=='pooled-shared' else 1-case%2) else 0,0]
                carried=commons.add(inventories[:3],inventories[3:]);decisions=[3 if not isolated or n==m else 0 for n in range(2)]
                rooms.append({'room':index*2+m,'ticks':3000,'end':'horizon','carried':carried,'bank':[0,0,0],'dropped':[0,0,0],
                    'member_carried':inventories,'crafted_picks':0,'actor_ticks':[3000 if d else 0 for d in decisions],
                    'decisions':decisions,'gui_selections':[v for d in decisions for v in [d,0,0,0,0,0]],'chest_observations':[0,0]})
            trials.append({'trial':index,'case':case,'condition':condition,'seed':2026100711+case*104729,'supplies_owner':case%2,
                'mirror':(case//2)%2!=0,'success':False,'elapsed_ticks':3000,'total':[3,2,0],'bank':[0,0,0],'lost_wood_units':0,'rooms':rooms})
    return {'complete':True,'protocol':commons.PROTOCOL,'schema':commons.SCHEMA,'stochastic':True,'new_training_samples':0,
        'cases_per_condition':4,'horizon_ticks':3000,'seed':2026100711,'policy_updates':0,'policy_trained_samples':0,
        'trials':trials,'conditions':[{'condition':c,'cases':4,'passed':0} for c in commons.CONDITIONS]}


class CommonsEvidence(unittest.TestCase):
    def valid(self,r):return commons.validate(r,4,2026100711,3000)
    def reject(self,r):
        with self.assertRaises((ValueError,TypeError)):self.valid(r)
    def test_full_failures_are_complete(self):self.assertEqual([r['passed'] for r in self.valid(report())],[0,0,0])
    def test_delivery_is_derived_from_stock(self):
        r=report();t=r['trials'][0];cell=t['rooms'][0]
        cell.update(end='delivered',carried=[0,0,0],bank=[0,0,1],member_carried=[0]*6,crafted_picks=1)
        t.update(success=True,total=[0,0,1],bank=[0,0,1]);r['conditions'][0]['passed']=1
        self.assertEqual(self.valid(r)[0]['passed'],1)
    def test_bool_not_integer(self):
        for path in [('policy_updates',),('cases_per_condition',),('new_training_samples',),('trials',0,'lost_wood_units'),('conditions',0,'passed')]:
            r=report();cursor=r
            for key in path[:-1]:cursor=cursor[key]
            cursor[path[-1]]=False;self.reject(r)
    def test_nan_and_float(self):
        for value in [float('nan'),float('inf'),3.0,-1]:
            r=report();r['trials'][0]['rooms'][0]['decisions'][0]=value;self.reject(r)
    def test_missing_trial(self):r=report();r['trials'].pop();self.reject(r)
    def test_duplicate_trial(self):r=report();r['trials'][1]=copy.deepcopy(r['trials'][0]);self.reject(r)
    def test_swapped_conditions(self):r=report();r['trials'][0],r['trials'][4]=r['trials'][4],r['trials'][0];self.reject(r)
    def test_bad_seed(self):r=report();r['trials'][5]['seed']+=1;self.reject(r)
    def test_bad_supply_swap(self):r=report();r['trials'][1]['supplies_owner']=0;self.reject(r)
    def test_bad_mirror(self):r=report();r['trials'][2]['mirror']=False;self.reject(r)
    def test_forged_success(self):r=report();r['trials'][0]['success']=True;self.reject(r)
    def test_forged_summary(self):r=report();r['conditions'][0]['passed']=1;self.reject(r)
    def test_forged_bank(self):r=report();r['trials'][0]['bank']=[0,0,1];self.reject(r)
    def test_forged_member_stock(self):r=report();r['trials'][0]['rooms'][0]['member_carried'][0]+=1;self.reject(r)
    def test_stock_creation(self):
        r=report();c=r['trials'][0]['rooms'][0];c['dropped']=[0,0,1];self.reject(r)
    def test_invented_recipe_credit(self):r=report();r['trials'][0]['rooms'][0]['crafted_picks']=2;self.reject(r)
    def test_isolated_foreign_member(self):r=report();r['trials'][8]['rooms'][0]['actor_ticks'][1]=1;self.reject(r)
    def test_isolated_missing_room(self):r=report();r['trials'][8]['rooms'].pop();self.reject(r)
    def test_isolated_creation_cannot_hide_behind_team_total(self):
        r=report();a,b=r['trials'][8]['rooms'];a['member_carried']=[3,2,0,0,0,0];a['carried']=[3,2,0];b['member_carried']=[0]*6;b['carried']=[0]*3;self.reject(r)
    def test_wrong_denominator(self):r=report();r['trials'][0]['rooms'][0]['gui_selections'][0]+=1;self.reject(r)
    def test_ghost_chest_observations(self):r=report();r['trials'][0]['rooms'][0]['chest_observations'][0]=4;self.reject(r)
    def test_premature_horizon(self):r=report();r['trials'][0]['rooms'][0]['ticks']=2999;self.reject(r)
    def test_oversized_actor_effort(self):r=report();r['trials'][0]['rooms'][0]['actor_ticks'][0]=3003;self.reject(r)
    def test_forged_loss(self):r=report();r['trials'][0]['lost_wood_units']=1;self.reject(r)
    def test_loss_remains_a_failure(self):
        r=report();t=r['trials'][0];c=t['rooms'][0];c['carried']=[0,2,0];c['member_carried'][0]=0;t['total']=[0,2,0];t['lost_wood_units']=6;self.assertEqual(self.valid(r)[0]['passed'],0)
    def test_optimization_does_not_disable_validation(self):
        script="import json,sys;from commons import validate;validate(json.loads(sys.stdin.read()),4,2026100711,3000)"
        r=report();r['trials'][0]['success']=True
        for flag in ['-O','-OO']:
            done=subprocess.run([sys.executable,flag,'-c',script],cwd=Path(__file__).parent,input=json.dumps(r),text=True,capture_output=True)
            self.assertNotEqual(done.returncode,0);self.assertIn('Forged completion',done.stderr)
    def test_cli_requires_consent_before_preparation(self):
        old=os.environ.pop('EULA',None)
        try:
            with self.assertRaises(ValueError):commons.arguments(['--fresh-policy-seed','1','--output','.build/no-run'])
        finally:
            if old is not None:os.environ['EULA']=old


if __name__=='__main__':unittest.main()
