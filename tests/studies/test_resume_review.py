import copy,json,os,subprocess,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import Mock,patch
import resume_review as R
S=R.S


def rows(arms):
    return [dict(arm=arm,seed=seed,condition='none',scores=[32]*12+[0],trials=416,new_training_samples=0)
        for arm in arms for seed in R.SEEDS]


def exposure():
    return dict(candidate=dict(early_accepted_coverage=True),control=dict(frontier_only_through_last_observation=True))


def status(samples,review=True,stamp=1000):
    counts=[10]*12+[samples-120]+[0]*6 if review else [0]*12+[samples]+[0]*6
    cells=[n if m==0 else 0 for n in counts for m in range(6)]
    return dict(state='running',schema='bcmc-citizen-egocentric-context',epoch_millis=stamp,
        inference_failed=0,inference_rejected=0,learner_rejected_samples=0,learner_stale_samples=0,retired_agents=0,
        trained_samples=S.BASE+samples,active_agents=512,learned_task_samples_this_process=json.dumps(counts),
        learner_context_scope='accepted-pre-action-observations-this-process',learner_context_layout='task-major',
        learner_context_task_buckets=19,learner_context_menu_buckets=6,learner_context_unlabelled_task=18,
        learner_context_menu_order='closed,inventory,workbench,furnace,chest,unknown',learner_context_base_trained_samples=S.BASE,
        learner_context_samples=samples,learner_context_ticks=5*samples,learner_context_trained_samples=S.BASE+samples,
        learner_context_samples_by_task_and_menu=json.dumps(cells),learner_context_ticks_by_task_and_menu=json.dumps([n*5 for n in cells]))


def startup(review=True):
    return dict(startup_restored_checkpoint=True,startup_observed_agents=512,startup_expected_agents=512,
        startup_review_agents=512 if review else 0,startup_frontier_agents=0 if review else 512,
        startup_training_task_population=json.dumps([42]*11+[50]+[0]*6 if review else [0]*12+[512]+[0]*5))


class ResumeReviewTest(unittest.TestCase):
    def setUp(self):self.base=rows(('parent',));self.current=rows(S.ARMS)
    def supported(self):
        for row in self.current:
            if row['arm']=='control':row['scores'][11]=24
    def test_complete_interpretation(self):
        self.supported();result=R.interpretation(self.base,self.current,exposure())
        self.assertTrue(result['review_preserves_under_reproduced_loss']);self.assertTrue(result['candidate_retained'])
        self.assertFalse(result['control_retained']);self.assertFalse(result['acquisition']);self.assertFalse(result['deployment'])
    def test_each_task_seed_floor(self):
        self.supported()
        for task in range(12):
            for seed in R.SEEDS:
                current=copy.deepcopy(self.current);row=next(r for r in current if r['arm']=='candidate' and r['seed']==seed)
                row['scores'][task]=29 if task<11 else 28
                self.assertTrue(R.interpretation(self.base,current,exposure())['candidate_retained'])
                row['scores'][task]-=1
                self.assertFalse(R.interpretation(self.base,current,exposure())['candidate_retained'])
    def test_control_loss_requires_both_seeds(self):
        self.supported()
        for seed in R.SEEDS:
            current=copy.deepcopy(self.current);next(r for r in current if r['arm']=='control' and r['seed']==seed)['scores'][11]=25
            self.assertFalse(R.interpretation(self.base,current,exposure())['control_pickaxe_loss_reproduced'])
    def test_control_other_skills_must_retain(self):
        self.supported();self.current[0]['scores'][2]=28
        self.assertFalse(R.interpretation(self.base,self.current,exposure())['review_preserves_under_reproduced_loss'])
    def test_acquisition_separate_from_retention(self):
        for row in self.current:row['scores'][12]=8 if row['arm']=='candidate' else 4
        self.assertTrue(R.interpretation(self.base,self.current,exposure())['acquisition'])
        self.current[-1]['scores'][11]=0
        result=R.interpretation(self.base,self.current,exposure());self.assertTrue(result['acquisition']);self.assertFalse(result['candidate_retained'])
        self.current[-1]['scores'][12]=7
        self.assertFalse(R.interpretation(self.base,self.current,exposure())['acquisition'])
    def test_exposure_required_for_support(self):
        self.supported()
        for arm,key in (('candidate','early_accepted_coverage'),('control','frontier_only_through_last_observation')):
            value=exposure();value[arm][key]=False
            self.assertFalse(R.interpretation(self.base,self.current,value)['review_preserves_under_reproduced_loss'])
            value[arm][key]=1
            with self.assertRaises(Exception):R.interpretation(self.base,self.current,value)
    def test_complete_report_matrix_and_types(self):
        for current in (self.current[:-1],self.current+[self.current[0]]):
            with self.assertRaises(Exception):R.interpretation(self.base,current,exposure())
        for field,value in (('seed',float(R.SEEDS[0])),('trials',416.0),('new_training_samples',False)):
            current=copy.deepcopy(self.current);current[0][field]=value
            with self.assertRaises(Exception):R.interpretation(self.base,current,exposure())
        for value in (True,1.0,-1,33):
            current=copy.deepcopy(self.current);current[0]['scores'][11]=value
            with self.assertRaises(Exception):R.interpretation(self.base,current,exposure())
    def test_coverage_uses_actual_accepted_contexts(self):
        result=R.coverage([status(1000),status(50000,stamp=2000)],startup(),'candidate')
        self.assertTrue(result['early_accepted_coverage']);self.assertEqual(result['first_observed_review']['accepted_samples'],1000)
        self.assertFalse(R.coverage([status(50000,False)],startup(),'candidate')['early_accepted_coverage'])
        self.assertTrue(R.coverage([status(50000,False)],startup(False),'control')['frontier_only_through_last_observation'])
    def test_missing_and_late_coverage_fail_without_inventing_samples(self):
        for samples in (49999,75001):
            self.assertFalse(R.coverage([status(samples)],startup(),'candidate')['early_accepted_coverage'])
        self.assertTrue(R.coverage([status(75000)],startup(),'candidate')['early_accepted_coverage'])
        with self.assertRaises(Exception):R.coverage([],startup(),'candidate')
    def test_each_earlier_task_required(self):
        for task in range(12):
            row=status(50000);counts=json.loads(row['learned_task_samples_this_process']);counts[12]+=counts[task];counts[task]=0
            cells=[n if m==0 else 0 for n in counts for m in range(6)]
            row.update(learned_task_samples_this_process=json.dumps(counts),learner_context_samples_by_task_and_menu=json.dumps(cells),
                learner_context_ticks_by_task_and_menu=json.dumps([5*n for n in cells]))
            self.assertFalse(R.coverage([row],startup(),'candidate')['early_accepted_coverage'])
    def test_bad_origin_and_regressing_cells_rejected(self):
        row=status(50000);row['learner_context_base_trained_samples']+=1
        with self.assertRaises(Exception):R.coverage([row],startup(),'candidate')
        row=status(50000);row['learner_context_ticks_by_task_and_menu']=json.dumps([0]*114)
        with self.assertRaises(Exception):R.coverage([row],startup(),'candidate')
        with self.assertRaises(Exception):R.coverage([status(50000),status(40000,stamp=2000)],startup(),'candidate')
        first=status(1000);cells=json.loads(first['learner_context_samples_by_task_and_menu']);ticks=json.loads(first['learner_context_ticks_by_task_and_menu'])
        cells[0]-=1;cells[1]+=1;ticks[0]-=5;ticks[1]+=5
        first.update(learner_context_samples_by_task_and_menu=json.dumps(cells),learner_context_ticks_by_task_and_menu=json.dumps(ticks))
        with self.assertRaises(Exception):R.coverage([first,status(50000,stamp=2000)],startup(),'candidate')
    def test_incomplete_startup_and_population_rejected(self):
        for key,value in (('startup_restored_checkpoint',False),('startup_observed_agents',511),('startup_review_agents',True)):
            start=startup();start[key]=value
            with self.assertRaises(Exception):R.coverage([status(50000)],start,'candidate')
        start=startup();start['startup_training_task_population']=json.dumps([0]*18)
        with self.assertRaises(Exception):R.coverage([status(50000)],start,'candidate')
    def test_exact_module_pins_and_finite_budget(self):
        for module in (S,R.M):
            self.assertEqual(module.SEEDS,R.SEEDS);self.assertEqual(module.OUT,R.OUT)
            self.assertEqual(module.CONTROL_RUNTIME,R.CONTROL_RUNTIME);self.assertEqual(module.TARGETS,(250000,))
        for samples in (S.BASE+250000,S.BASE+300000):S.budget(samples,250000)
        for samples in (S.BASE+249999,S.BASE+300001):
            with self.assertRaises(Exception):S.budget(samples,250000)
        with self.assertRaises(Exception):S.budget(S.BASE+1000000,1000000)
    def test_create_only_and_symlink_rejection(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'receipt.json';S.save(path,dict(deployment=False))
            with self.assertRaises(FileExistsError):S.save(path,dict(deployment=True))
            link=Path(temporary)/'alias.json';link.symlink_to(path)
            with self.assertRaises(Exception):S.read(link)
            self.assertFalse(S.load(path)['deployment'])
    def test_unqualified_input_cannot_train(self):
        with tempfile.TemporaryDirectory() as temporary,patch.object(R,'OUT',Path(temporary)/'evidence'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study=object.__new__(R.Study);study.source='fixture';study.guard=Mock();study.identity=Mock();study.train=Mock()
            self.base[0]['scores'][11]=25;study.evaluate=Mock(return_value=self.base)
            study.run();study.train.assert_not_called();result=S.load(R.OUT/'outcome.json')
            self.assertEqual(result['stage'],'input-rejected');self.assertEqual(result['frozen_trials'],832)
    def test_failed_exposure_still_gets_complete_frozen_reports(self):
        with tempfile.TemporaryDirectory() as temporary,patch.object(R,'OUT',Path(temporary)/'evidence'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study=object.__new__(R.Study);study.source='fixture';study.guard=Mock();study.identity=Mock();study.train=Mock()
            study.evaluate=Mock(side_effect=[self.base,self.current]);value=exposure();value['candidate']['early_accepted_coverage']=False
            study.exposure=Mock(return_value=value);study.run()
            self.assertEqual(study.train.call_count,2);self.assertEqual(study.evaluate.call_count,2)
            result=S.load(R.OUT/'outcome.json');self.assertEqual(result['frozen_trials'],2496)
            self.assertFalse(result['decision']['review_preserves_under_reproduced_loss']);self.assertFalse(result['deployment'])
    def test_cli_does_not_start_implicitly(self):
        env=dict(os.environ);env.pop('EULA',None)
        for arguments,success in (([],True),(['--help'],True),(['--run'],False)):
            before=R.OUT.exists()
            result=subprocess.run([sys.executable,str(R.ROOT/'tests/studies/resume_review.py'),*arguments],env=env,capture_output=True,text=True,timeout=15)
            self.assertEqual(result.returncode==0,success,result.stderr);self.assertEqual(R.OUT.exists(),before)

if __name__=='__main__':unittest.main()
