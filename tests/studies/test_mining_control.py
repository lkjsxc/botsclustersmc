"""Offline boundaries only. No Minecraft server or learner is started by these tests."""
from copy import deepcopy
from pathlib import Path
from unittest import TestCase, main
from unittest.mock import patch
import json, os, subprocess, sys, tempfile
import mining_control_support as S
import mining_control as R


def rows(arms):
    return [dict(arm=a,seed=s,condition=c,scores=[32]*12+[0],mining={},
        trials=416,new_training_samples=0) for a in arms for s in S.SEEDS for c in S.CONDITIONS]


def status():
    return dict(epoch_millis=100000,state='running',schema='bcmc-citizen-egocentric-context',
        inference_failed=0,inference_rejected=0,learner_rejected_samples=0,learner_stale_samples=0,retired_agents=0,
        trained_samples=S.BASE+100,active_agents=512,learned_task_samples_this_process=json.dumps([0]*12+[100]+[0]*6),
        learner_context_scope='accepted-pre-action-observations-this-process',learner_context_layout='task-major',
        learner_context_task_buckets=19,learner_context_unlabelled_task=18,learner_context_menu_buckets=6,
        learner_context_menu_order='closed,inventory,workbench,furnace,chest,unknown',learner_context_base_trained_samples=S.BASE,
        learner_context_samples=100,learner_context_ticks=400,learner_context_trained_samples=S.BASE+100,
        learner_context_samples_by_task_and_menu=json.dumps([0]*72+[100]+[0]*41),
        learner_context_ticks_by_task_and_menu=json.dumps([0]*72+[400]+[0]*41))


class Evidence(TestCase):
    def reject(self,call):
        with self.assertRaises(AssertionError):call()

    def test_full_matrix(self):
        self.assertTrue(S.qualify_baseline(rows(('parent',))))
        result=S.gate(rows(('parent',)),rows(S.ARMS))
        self.assertTrue(result['retained']);self.assertFalse(result['acquisition'])
        self.assertEqual(result,S.gate(list(reversed(rows(('parent',)))),list(reversed(rows(S.ARMS)))))

    def test_exact_retention_thresholds(self):
        base=rows(('parent',));current=rows(S.ARMS)
        for row in current:row['scores']=[29]*11+[28,0]
        self.assertTrue(S.gate(base,current)['retained'])
        for arm in S.ARMS:
            for seed in S.SEEDS:
                bad=deepcopy(current);row=next(r for r in bad if (r['arm'],r['seed'])==(arm,seed))
                row['scores'][0]-=1;self.assertFalse(S.gate(base,bad)['retained'])
                bad=deepcopy(current);row=next(r for r in bad if (r['arm'],r['seed'])==(arm,seed))
                row['scores'][11]=27;self.assertFalse(S.gate(base,bad)['retained'])

    def test_input_floor_is_per_task_and_seed(self):
        for seed in S.SEEDS:
            bad=rows(('parent',));row=next(r for r in bad if r['seed']==seed);row['scores'][0]=27;self.assertFalse(S.qualify_baseline(bad))
            bad=rows(('parent',));row=next(r for r in bad if r['seed']==seed);row['scores'][11]=25;self.assertFalse(S.qualify_baseline(bad))

    def test_ordinary_gain_requires_both_seeds(self):
        base=rows(('parent',));current=rows(S.ARMS)
        for row in current:row['scores'][12]=8 if row['arm']=='candidate' else 4
        self.assertTrue(S.gate(base,current)['acquisition'])
        for arm,score in [('candidate',7),('control',5)]:
            bad=deepcopy(current);next(r for r in bad if r['seed']==S.SEEDS[1] and r['arm']==arm)['scores'][12]=score
            self.assertFalse(S.gate(base,bad)['acquisition'])

    def test_missing_duplicate_and_bad_types(self):
        good=rows(S.ARMS)
        self.reject(lambda:S.vectors(good[:-1],S.ARMS));self.reject(lambda:S.vectors(good+[good[0]],S.ARMS))
        for key,value in [('arm','other'),('seed',float(S.SEEDS[0])),('condition','pickaxe-grid'),
                ('scores',[32.0]*13),('scores',[True]*13),('scores',[33]*13),('scores',[32]),
                ('trials',416.0),('trials',415),('new_training_samples',False),('new_training_samples',1)]:
            with self.subTest(key=key,value=value):
                bad=deepcopy(good);bad[0][key]=value;self.reject(lambda:S.vectors(bad,S.ARMS))

    def test_sample_boundaries(self):
        for target in S.TARGETS:
            S.budget(S.BASE+target,target);S.budget(S.BASE+target+S.OVERSHOOT,target)
            self.reject(lambda:S.budget(S.BASE+target-1,target))
            self.reject(lambda:S.budget(S.BASE+target+S.OVERSHOOT+1,target))
            self.reject(lambda:S.budget(float(S.BASE+target),target))
        self.reject(lambda:S.budget(S.BASE+123,123))

    def test_fresh_process_status(self):
        current=status();self.assertEqual(S.training_samples(current,S.BASE,100000,101000),S.BASE+100)
        current['epoch_millis']=99999;self.assertIsNone(S.training_samples(current,S.BASE,100000,101000))
        self.reject(lambda:S.training_samples(current,S.BASE,100000,101000,status()))

    def test_health_and_monotonic_counters(self):
        for key,value in [('epoch_millis',100000.0),('schema','bcmc-click-conditioned-slots'),('state','failed'),
                ('active_agents',513),('active_agents',True),('trained_samples',S.BASE-1),('inference_failed',1),
                ('learner_rejected_samples',1),('learned_task_samples_this_process','[0]')]:
            bad=status();bad[key]=value;self.reject(lambda:S.training_samples(bad,S.BASE,100000,101000))
        self.reject(lambda:S.training_samples(status(),S.BASE,100000,150001))
        previous=status();previous['trained_samples']+=1
        self.reject(lambda:S.training_samples(status(),S.BASE,100000,101000,previous))
        previous=status();previous['learned_task_samples_this_process']=json.dumps([1]+[0]*11+[100]+[0]*6)
        self.reject(lambda:S.training_samples(status(),S.BASE,100000,101000,previous))

    def test_context_accounting_rejects_wrong_scope_origin_and_marginals(self):
        good=status(); samples=json.loads(good['learner_context_samples_by_task_and_menu'])
        ticks=json.loads(good['learner_context_ticks_by_task_and_menu'])
        S.context_counts(good,S.BASE)
        cases=[
            ('learner_context_scope','offered-observations'),
            ('learner_context_layout','menu-major'),
            ('learner_context_task_buckets',18),
            ('learner_context_unlabelled_task',17),
            ('learner_context_menu_buckets',5),
            ('learner_context_menu_order','closed,inventory'),
            ('learner_context_base_trained_samples',S.BASE-1),
            ('learner_context_samples',99),
            ('learner_context_ticks',399),
            ('learner_context_trained_samples',S.BASE+99),
        ]
        for key,value in cases:
            with self.subTest(key=key):
                bad=deepcopy(good);bad[key]=value;self.reject(lambda:S.context_counts(bad,S.BASE))
        for key in ('learner_context_samples_by_task_and_menu','learner_context_ticks_by_task_and_menu'):
            bad=deepcopy(good);bad[key]=json.dumps([0]*113);self.reject(lambda:S.context_counts(bad,S.BASE))
            bad=deepcopy(good);value=json.loads(bad[key]);value[72]=True;bad[key]=json.dumps(value)
            self.reject(lambda:S.context_counts(bad,S.BASE))
        bad=deepcopy(good);value=samples.copy();value[72]=99;value[73]=1
        bad['learner_context_samples_by_task_and_menu']=json.dumps(value)
        self.reject(lambda:S.context_counts(bad,S.BASE))
        bad=deepcopy(good);value=ticks.copy();value[72]=100*12000+1
        bad['learner_context_ticks_by_task_and_menu']=json.dumps(value);bad['learner_context_ticks']=value[72]
        self.reject(lambda:S.context_counts(bad,S.BASE))
        bad=deepcopy(good);tasks=json.loads(bad['learned_task_samples_this_process']);tasks[12]=99
        bad['learned_task_samples_this_process']=json.dumps(tasks);self.reject(lambda:S.context_counts(bad,S.BASE))

    def test_file_outputs_create_only(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'file';S.write(path,b'kept')
            with self.assertRaises(FileExistsError):S.write(path,b'changed')
            self.assertEqual(S.read(path),b'kept')

    def test_cli_without_run_starts_nothing(self):
        with tempfile.TemporaryDirectory() as folder:
            for args in ([],['--help']):
                result=subprocess.run([sys.executable,str(Path(R.__file__)),*args],cwd=folder,capture_output=True,text=True,timeout=20)
                self.assertEqual(result.returncode,0);self.assertIn('--run',result.stdout)
                self.assertEqual(list(Path(folder).iterdir()),[])

    def test_no_eula_cannot_construct_study(self):
        with patch.dict(os.environ,{'EULA':'false'}),patch.object(sys,'argv',['mining_control.py','--run']),patch.object(R,'Study') as study:
            self.reject(R.main);study.assert_not_called()


if __name__=='__main__':main()
