"""Synthetic report integrity checks; not Minecraft learning evidence."""
import copy
import unittest
from lifecycle_summary import summarize


def fixture():
    trials=[]
    for arm,salt in [('EXAM',0x677d815fab35),('PROBE',0x173a6ae017a1)]:
        for phase,task in enumerate([11,12,11]):
            for case in range(2):
                initial=[0.0]*512;initial[16+task]=1
                trials.append({'arm':arm,'phase':phase,'case':case,'task':task,'success':case==0,
                    'initial':initial,'initial_mask':[True]*233,'elapsed_ticks':10,'decisions':2,
                    'sum_transition_ticks':10,'action_seed':task*100+case,'reset_seed':(task*13+case)^salt})
    return {'complete':True,'new_training_samples':0,'order':[11,12,11],'cases_per_arm':2,
            'policy_updates':5,'policy_samples':200,'seed':7,'trials':trials}


class LifecycleSummaryTest(unittest.TestCase):
    def test_complete_failed_cases_retained(self):
        report=fixture();before=copy.deepcopy(report);result=summarize(report)
        self.assertEqual(report,before)
        self.assertEqual(len(result['counts']),6)
        self.assertTrue(all(row['passed']==1 and row['cases']==2 for row in result['counts']))
        self.assertTrue(all(pair['identical_masks']==2 and not pair['initial_differences'] for pair in result['pairs']))
        self.assertTrue(all(pair['outcomes']=={'True->True':1,'False->False':1} for pair in result['pairs']))

    def test_state_and_outcome_difference(self):
        report=fixture();trial=report['trials'][5];trial['initial'][330]=0.25;trial['success']=True
        result=summarize(report)
        pair=result['pairs'][2]
        self.assertEqual(pair['initial_differences'],[{'index':330,'cases':1,'example':[0,0.25]}])
        self.assertEqual(pair['outcomes'],{'True->True':1,'False->True':1})

    def test_reject_incomplete(self):
        report=fixture();report['complete']=False
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_training(self):
        report=fixture();report['new_training_samples']=1
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_missing(self):
        report=fixture();report['trials'].pop()
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_duplicate(self):
        report=fixture();report['trials'][1]=copy.deepcopy(report['trials'][0])
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_wrong_task(self):
        report=fixture();report['trials'][0]['task']=12
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_forged_success(self):
        report=fixture();report['trials'][0]['success']='true'
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_nonfinite(self):
        report=fixture();report['trials'][0]['initial'][0]=float('nan')
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_wrong_shape(self):
        report=fixture();report['trials'][0]['initial_mask'].pop()
        with self.assertRaises(ValueError):summarize(report)

    def test_reject_changed_rng(self):
        for field in ('action_seed','reset_seed'):
            report=fixture();report['trials'][0][field]+=1
            with self.assertRaises(ValueError):summarize(report)

    def test_reject_zero_duration(self):
        report=fixture();report['trials'][0]['elapsed_ticks']=0
        with self.assertRaises(ValueError):summarize(report)


if __name__=='__main__':unittest.main()
