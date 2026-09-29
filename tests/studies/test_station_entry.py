"""Offline boundaries only. No Minecraft server or learner is started by these tests."""
from copy import deepcopy
from pathlib import Path
from unittest import TestCase, main
from unittest.mock import patch
import json, os, subprocess, sys, tempfile
import station_entry_support as S
import station_entry as R


def rows(arms):
    return [dict(arm=a,seed=s,condition=c,scores=([32]*11+[1] if c=='none' else [31]),
        trials=384 if c=='none' else 32,new_training_samples=0) for a in arms for s in S.SEEDS for c in S.CONDITIONS]


def status():
    return dict(epoch_millis=100000,state='running',schema='bcmc-citizen-egocentric-context',
        inference_failed=0,inference_rejected=0,learner_rejected_samples=0,learner_stale_samples=0,retired_agents=0,
        trained_samples=S.BASE+100,active_agents=512,learned_task_samples_this_process=json.dumps([0]*11+[100]+[0]*7))


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
        for row in current:row['scores']=[29]*11+[0] if row['condition']=='none' else [27]
        self.assertTrue(S.gate(base,current)['retained'])
        for arm in S.ARMS:
            for seed in S.SEEDS:
                for condition in S.CONDITIONS:
                    bad=deepcopy(current)
                    row=next(r for r in bad if (r['arm'],r['seed'],r['condition'])==(arm,seed,condition))
                    row['scores'][0]-=1
                    self.assertFalse(S.gate(base,bad)['retained'])

    def test_input_floor_is_per_task_and_seed(self):
        for seed in S.SEEDS:
            for condition in S.CONDITIONS:
                bad=rows(('parent',));row=next(r for r in bad if r['seed']==seed and r['condition']==condition)
                row['scores'][0]=27;self.assertFalse(S.qualify_baseline(bad))

    def test_ordinary_gain_requires_both_seeds(self):
        base=rows(('parent',));current=rows(S.ARMS)
        for row in current:
            if row['condition']=='none':row['scores'][11]=8 if row['arm']=='candidate' else 4
        self.assertTrue(S.gate(base,current)['acquisition'])
        for arm,score in [('candidate',7),('control',5)]:
            bad=deepcopy(current);next(r for r in bad if r['seed']==S.SEEDS[1] and r['arm']==arm and r['condition']=='none')['scores'][11]=score
            self.assertFalse(S.gate(base,bad)['acquisition'])

    def test_assistance_cannot_replace_ordinary_gain(self):
        current=rows(S.ARMS)
        for row in current:
            if row['arm']=='candidate' and row['condition']=='workbench-open':row['scores']=[32]
        self.assertFalse(S.gate(rows(('parent',)),current)['acquisition'])

    def test_missing_duplicate_and_bad_types(self):
        good=rows(S.ARMS)
        self.reject(lambda:S.vectors(good[:-1],S.ARMS));self.reject(lambda:S.vectors(good+[good[0]],S.ARMS))
        for key,value in [('arm','other'),('seed',float(S.SEEDS[0])),('condition','pickaxe-grid'),
                ('scores',[32.0]*12),('scores',[True]*12),('scores',[33]*12),('scores',[32]),
                ('trials',384.0),('trials',383),('new_training_samples',False),('new_training_samples',1)]:
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
        previous=status();previous['learned_task_samples_this_process']=json.dumps([1]+[0]*10+[100]+[0]*7)
        self.reject(lambda:S.training_samples(status(),S.BASE,100000,101000,previous))

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
        with patch.dict(os.environ,{'EULA':'false'}),patch.object(sys,'argv',['station_entry.py','--run']),patch.object(R,'Study') as study:
            self.reject(R.main);study.assert_not_called()


if __name__=='__main__':main()
