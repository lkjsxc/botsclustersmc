"""Offline corruption and interpretation checks; synthetic scores are not gameplay."""
import copy
import unittest
import control_repeatability as C


def rows():
    result = []
    for model in C.MODELS:
        for seed in C.SEEDS:
            for repeat in C.REPEATS:
                score = 30 if model=='parent' else 20 if model=='control-earlier' else 27
                values = [32]*11+[score, 0]
                result.append(dict(model=model, seed=seed, repeat=repeat, scores=values,
                    outcomes=[[i<n for i in range(32)] for n in values], trials=416, new_training_samples=0))
    return result


def change(row, task, successes):
    row['scores'][task] = successes
    row['outcomes'][task] = [i<successes for i in range(32)]


class MatrixTest(unittest.TestCase):
    def test_exact_scope_and_interpretation(self):
        result=C.analyze(rows())
        self.assertEqual(result['control_separation'], 'later-better')
        self.assertTrue(result['pickaxe_score_repeatable'])
        self.assertTrue(result['pickaxe_case_repeatable'])
        self.assertFalse(result['review_positive_in_every_pair'])
        self.assertEqual(len(result['retention']), 12)
        self.assertFalse(result['deployment'])
    def test_report_missing(self):
        with self.assertRaises(AssertionError):C.analyze(rows()[:-1])
    def test_report_duplicate(self):
        data=rows();data[-1]=copy.deepcopy(data[0])
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_seed_replacement(self):
        data=rows();data[0]['seed']+=500
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_repeat_boolean(self):
        data=rows();data[0]['repeat']=False
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_outcome_boolean_required(self):
        data=rows();data[0]['outcomes'][0][0]=1
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_summary_cannot_hide_failure(self):
        data=rows();data[0]['outcomes'][11][0]=False
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_extra_trial(self):
        data=rows();data[0]['outcomes'][11].append(False)
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_training_forbidden(self):
        data=rows();data[0]['new_training_samples']=1
        with self.assertRaises(AssertionError):C.analyze(data)
    def test_equal_score_different_trials(self):
        data=rows();bits=data[1]['outcomes'][11]
        bits[:2],bits[30:]=[False,False],[True,True]
        result=C.analyze(data)
        self.assertEqual(result['within_policy'][0]['score_deltas'][11],0)
        self.assertEqual(result['within_policy'][0]['outcome_flips'][11],4)
    def test_repeat_instability(self):
        data=rows();change(data[1],11,24)
        result=C.analyze(data)
        self.assertFalse(result['pickaxe_score_repeatable'])
        self.assertFalse(result['pickaxe_case_repeatable'])
    def test_no_cross_seed_averaging(self):
        data=rows();change(data[10],11,19);change(data[11],11,19)
        self.assertEqual(C.analyze(data)['control_separation'],'not-separated')
    def test_reversed_control_direction(self):
        data=rows()
        for row in data:
            if row['model']=='control-earlier':change(row,11,31)
            elif row['model']=='control-later':change(row,11,20)
        self.assertEqual(C.analyze(data)['control_separation'],'earlier-better')
    def test_envelope_margin(self):
        data=rows();change(data[5],11,24)
        self.assertEqual(C.analyze(data)['control_separation'],'not-separated')
    def test_parent_not_qualified(self):
        data=rows();change(data[0],10,27)
        self.assertFalse(C.analyze(data)['parent_qualified'])
    def test_retention_is_per_seed_repeat_task(self):
        data=rows();change(data[-1],10,28)
        failure=C.analyze(data)['retention'][-1]['failures']
        self.assertEqual(failure,[dict(task=10,source=32,score=28,floor=29)])
    def test_fixed_schedule_unique_slots_rotated(self):
        waves=C.schedule();self.assertEqual(len(waves),4)
        identity=[];ports={m:set() for m in C.MODELS}
        for wave in waves:
            self.assertEqual({j['port'] for j in wave},set(range(31710,31714)))
            self.assertEqual({j['model'] for j in wave},set(C.MODELS))
            for job in wave:
                identity.append((job['model'],job['seed'],job['repeat']));ports[job['model']].add(job['port'])
        self.assertEqual(len(set(identity)),16)
        self.assertTrue(all(len(p)==4 for p in ports.values()))
    def test_deterministic_analysis_no_input_mutation(self):
        data=rows();before=copy.deepcopy(data)
        self.assertEqual(C.analyze(data),C.analyze(data));self.assertEqual(data,before)


if __name__ == '__main__':
    unittest.main()
