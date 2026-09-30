import copy,json,os,subprocess,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import Mock,patch
import critic_detachment as D
S=D.S


def rows(arms):
    return [dict(arm=arm,seed=seed,condition='none',scores=[32]*12+[0],trials=416,new_training_samples=0)
        for arm in arms for seed in D.SEEDS]


class DetachmentTest(unittest.TestCase):
    def setUp(self):self.base=rows(('parent',));self.current=rows(S.ARMS)
    def supported(self):
        for row in self.current:
            if row['arm']=='control':
                for task in (2,10,11):row['scores'][task]=24
    def test_complete_interpretation(self):
        self.supported();result=D.interpretation(self.base,self.current)
        self.assertTrue(result['candidate_retained']);self.assertFalse(result['control_retained'])
        self.assertTrue(result['detachment_preserves_under_reproduced_loss'])
        self.assertFalse(result['acquisition']);self.assertFalse(result['deployment'])
    def test_every_candidate_floor(self):
        self.supported()
        for task in range(12):
            for seed in D.SEEDS:
                current=copy.deepcopy(self.current)
                row=next(x for x in current if x['arm']=='candidate' and x['seed']==seed)
                row['scores'][task]=29 if task<11 else 28
                self.assertTrue(D.interpretation(self.base,current)['candidate_retained'])
                row['scores'][task]-=1
                self.assertFalse(D.interpretation(self.base,current)['candidate_retained'])
    def test_reproduction_requires_each_named_task_each_seed(self):
        self.supported()
        for task in (2,10,11):
            for seed in D.SEEDS:
                current=copy.deepcopy(self.current)
                next(r for r in current if r['arm']=='control' and r['seed']==seed)['scores'][task]=25
                result=D.interpretation(self.base,current)
                self.assertFalse(result['control_regression_reproduced']);self.assertFalse(result['detachment_preserves_under_reproduced_loss'])
    def test_acquisition_is_separate(self):
        for row in self.current:row['scores'][12]=8 if row['arm']=='candidate' else 4
        self.assertTrue(D.interpretation(self.base,self.current)['acquisition'])
        self.current[-1]['scores'][12]=7
        self.assertFalse(D.interpretation(self.base,self.current)['acquisition'])
    def test_no_imputed_missing_reports(self):
        for current in (self.current[:-1],self.current+[self.current[0]],list(reversed(self.current))+[dict(self.current[0],arm='other')]):
            with self.assertRaises(Exception):D.interpretation(self.base,current)
    def test_strict_scores_seeds(self):
        for field,value in (('seed',float(D.SEEDS[0])),('trials',416.0),('new_training_samples',False)):
            current=copy.deepcopy(self.current);current[0][field]=value
            with self.assertRaises(Exception):D.interpretation(self.base,current)
        for invalid in (True,1.0,-1,33):
            current=copy.deepcopy(self.current);current[0]['scores'][2]=invalid
            with self.assertRaises(Exception):D.interpretation(self.base,current)
    def test_common_runtime_and_budget_bindings(self):
        for module in (S,D.M):
            self.assertEqual(module.CONTROL_SOURCE,D.CONTROL_SOURCE);self.assertEqual(module.CONTROL_RUNTIME,D.CONTROL_RUNTIME)
            self.assertEqual(module.SEEDS,D.SEEDS);self.assertEqual(module.TARGETS,(250000,));self.assertEqual(module.OUT,D.OUT)
        self.assertNotEqual(D.CANDIDATE_RUNTIME,D.CONTROL_RUNTIME)
        for samples in (S.BASE+250000,S.BASE+300000):S.budget(samples,250000)
        for samples in (S.BASE+249999,S.BASE+300001):
            with self.assertRaises(Exception):S.budget(samples,250000)
        with self.assertRaises(Exception):S.budget(S.BASE+1000000,1000000)
    def test_create_only_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'receipt.json';S.save(path,{'retained':False})
            with self.assertRaises(FileExistsError):S.save(path,{'retained':True})
            self.assertEqual(S.load(path),{'retained':False})
    def test_no_training_after_unqualified_input(self):
        with tempfile.TemporaryDirectory() as temporary,patch.object(D,'OUT',Path(temporary)/'evidence'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study=object.__new__(D.Study);study.source='fixture';study.guard=Mock();study.identity=Mock();study.train=Mock()
            self.base[0]['scores'][11]=25;study.evaluate=Mock(return_value=self.base)
            study.run();study.train.assert_not_called()
            outcome=S.load(D.OUT/'outcome.json')
            self.assertEqual(outcome['stage'],'input-rejected');self.assertEqual(outcome['reports'],2)
            self.assertEqual(outcome['frozen_trials'],832);self.assertFalse(outcome['deployment'])
    def test_cli_no_implicit_start(self):
        env=dict(os.environ);env.pop('EULA',None)
        for arguments,success in (([],True),(['--help'],True),(['--run'],False)):
            before=D.OUT.exists()
            result=subprocess.run([sys.executable,str(D.ROOT/'tests/studies/critic_detachment.py'),*arguments],env=env,capture_output=True,text=True,timeout=15)
            self.assertEqual(result.returncode==0,success,result.stderr)
            self.assertEqual(D.OUT.exists(),before)

if __name__=='__main__':unittest.main()
