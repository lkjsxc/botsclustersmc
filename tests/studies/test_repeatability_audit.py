"""Independent auditor corruption fixtures, without real Minecraft runs."""
import copy
from pathlib import Path
import tempfile
import unittest
import audit_control_repeatability as A


def fixture():
    trials=[]
    for actor in range(416):
        task=actor//32
        detail=dict(observations=1,dig_decisions=0,blocks_broken=0,items_collected=0,observed_max_target_mining_ticks=0)
        if task==12:
            detail.update(menu_observations=[1,0,0,0,0],harvest=dict(held_pick_observations=0,target_pick_contact_observations=0,world_dig_selections=0))
        trials.append(dict(actor=actor,task=task,seed=A.SEEDS[0]+task*1000003+(actor%32)*104729,
                           success=task<12,elapsed_ticks=5,diagnostics=detail))
    return dict(complete=True,stochastic=True,seed=A.SEEDS[0],cases_per_task=32,new_training_samples=0,
                trials=trials,tasks=[dict(task=t,cases=32,passed=32 if t<12 else 0) for t in range(13)])


class AuditTest(unittest.TestCase):
    def test_valid_raw_fixture(self):
        result=A.recount(fixture(),A.SEEDS[0]);self.assertEqual(result['scores'],[32]*12+[0]);self.assertEqual(result['mining']['observations'],32)
    def test_duplicate_actor(self):
        data=fixture();data['trials'][-1]=copy.deepcopy(data['trials'][0])
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_wrong_case_seed(self):
        data=fixture();data['trials'][55]['seed']+=1
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_hidden_failed_case(self):
        data=fixture();data['trials'][352]['success']=False
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_wrong_task_assignment(self):
        data=fixture();data['trials'][352]['task']=10
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_boolean_actor_rejected(self):
        data=fixture();data['trials'][0]['actor']=False
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_learning_rejected(self):
        data=fixture();data['new_training_samples']=1
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_menu_denominator(self):
        data=fixture();data['trials'][384]['diagnostics']['menu_observations']=[0]*5
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_false_contact(self):
        data=fixture();data['trials'][384]['diagnostics']['harvest']['target_pick_contact_observations']=1
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])
    def test_duplicate_json_fields(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'fixture.json';p.write_text('{"success":false,"success":true}')
            with self.assertRaises(ValueError):A.load(p)
    def test_nonfinite_json(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'fixture.json'
            for value in ('NaN','Infinity','1e9999'):
                p.write_text('{"value":'+value+'}')
                with self.assertRaises(ValueError):A.load(p)
    def test_complete_trial_denominator(self):
        data=fixture();data['trials'].pop()
        with self.assertRaises(ValueError):A.recount(data,A.SEEDS[0])


if __name__=='__main__':
    unittest.main()
