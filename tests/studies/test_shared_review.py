import copy
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch
import shared_review as R
S = R.S


def reports(arms):
    return [dict(arm=arm, seed=seed, condition='none', scores=[32]*12+[0], trials=416, new_training_samples=0)
            for arm in arms for seed in R.SEEDS]


def startup():
    return dict(startup_restored_checkpoint=True, startup_observed_agents=512, startup_expected_agents=512,
                startup_review_agents=512, startup_frontier_agents=0,
                startup_training_task_population=json.dumps([42]*11+[50]+[0]*6))


def status(total, reviewed, stamp):
    counts = [reviewed//12+(1 if task < reviewed % 12 else 0) for task in range(12)]+[total-reviewed]+[0]*6
    cells = [n if menu == 0 else 0 for n in counts for menu in range(6)]
    return dict(state='running', schema='bcmc-citizen-egocentric-context', epoch_millis=stamp,
                inference_failed=0, inference_rejected=0, learner_rejected_samples=0, learner_stale_samples=0, retired_agents=0,
                trained_samples=S.BASE+total, active_agents=512, learned_task_samples_this_process=json.dumps(counts),
                learner_context_scope='accepted-pre-action-observations-this-process', learner_context_layout='task-major',
                learner_context_task_buckets=19, learner_context_menu_buckets=6, learner_context_unlabelled_task=18,
                learner_context_menu_order='closed,inventory,workbench,furnace,chest,unknown', learner_context_base_trained_samples=S.BASE,
                learner_context_samples=total, learner_context_ticks=5*total, learner_context_trained_samples=S.BASE+total,
                learner_context_samples_by_task_and_menu=json.dumps(cells), learner_context_ticks_by_task_and_menu=json.dumps([5*n for n in cells]))


def history(sustained=True):
    first = dict(status(0, 0, 1000), **startup())
    return [first]+[status(n, n//5 if sustained else min(n//5,10000), (i+2)*1000) for i,n in enumerate(R.BOUNDARIES)]


def exposure():
    result = {}
    for arm in S.ARMS:
        rows = history(arm == 'candidate')
        result[arm] = R.coverage(rows, rows[0], arm)
    return result


class SharedReviewTest(unittest.TestCase):
    def setUp(self):
        self.base = reports(('parent',))
        self.current = reports(S.ARMS)
        for row in self.current:
            if row['arm'] == 'control':
                row['scores'][11] = 24

    def test_complete_supported_fixture(self):
        result = R.interpretation(self.base, self.current, exposure())
        self.assertTrue(result['candidate_retained'])
        self.assertTrue(result['control_pickaxe_loss_reproduced'])
        self.assertTrue(result['sustained_exposure_contrast'])
        self.assertTrue(result['shared_review_retention_supported'])
        self.assertFalse(result['acquisition'])
        self.assertFalse(result['deployment'])

    def test_all_frozen_task_retention_floors(self):
        for task in range(12):
            for seed in R.SEEDS:
                changed = copy.deepcopy(self.current)
                row = next(r for r in changed if r['arm']=='candidate' and r['seed']==seed)
                row['scores'][task] = 29 if task < 11 else 28
                self.assertTrue(R.interpretation(self.base, changed, exposure())['candidate_retained'])
                row['scores'][task] -= 1
                self.assertFalse(R.interpretation(self.base, changed, exposure())['candidate_retained'])

    def test_control_loss_must_reproduce_on_both_seeds(self):
        for seed in R.SEEDS:
            changed = copy.deepcopy(self.current)
            next(r for r in changed if r['arm']=='control' and r['seed']==seed)['scores'][11] = 25
            self.assertFalse(R.interpretation(self.base, changed, exposure())['control_pickaxe_loss_reproduced'])
        self.current[0]['scores'][2] = 28
        self.assertFalse(R.interpretation(self.base, self.current, exposure())['shared_review_retention_supported'])

    def test_acquisition_is_separate(self):
        for row in self.current:
            row['scores'][12] = 8 if row['arm']=='candidate' else 4
        self.current[-1]['scores'][11] = 0
        result = R.interpretation(self.base, self.current, exposure())
        self.assertTrue(result['acquisition'])
        self.assertFalse(result['candidate_retained'])
        self.current[-1]['scores'][12] = 7
        self.assertFalse(R.interpretation(self.base, self.current, exposure())['acquisition'])

    def test_complete_matrix_and_exact_types(self):
        for rows in (self.current[:-1], self.current+[self.current[0]]):
            with self.assertRaises(Exception):
                R.interpretation(self.base, rows, exposure())
        for field,value in (('seed',float(R.SEEDS[0])),('trials',416.0),('new_training_samples',False)):
            rows = copy.deepcopy(self.current); rows[0][field] = value
            with self.assertRaises(Exception):
                R.interpretation(self.base, rows, exposure())
        for n in (True,1.0,-1,33):
            rows = copy.deepcopy(self.current); rows[0]['scores'][11] = n
            with self.assertRaises(Exception):
                R.interpretation(self.base, rows, exposure())

    def test_sustained_actual_coverage(self):
        rows = history(); result = R.coverage(rows,rows[0],'candidate')
        self.assertTrue(result['sustained_accepted_review'])
        self.assertEqual([w['samples'] for w in result['windows']],[50000]*5)
        self.assertEqual(result['late']['samples'],150000)
        self.assertAlmostEqual(result['late']['review_share'],.2)
        self.assertEqual(sum(w['actor_ticks'] for w in result['windows']),1250000)
        self.assertEqual(result['first_observed_review']['accepted_samples'],50000)

    def test_frontloaded_review_fails_even_when_total_looks_positive(self):
        rows = history(False); result = R.coverage(rows,rows[0],'candidate')
        self.assertTrue(result['startup_match'])
        self.assertTrue(result['timely_windows'])
        self.assertFalse(result['sustained_accepted_review'])
        self.assertEqual(result['late']['review_share'],0)
        self.assertGreater(sum(result['last']['by_task'][:12]),0)

    def test_each_late_earlier_task_is_required(self):
        for missing in range(12):
            rows = history()
            for row in rows[3:]:
                cells = json.loads(row['learner_context_samples_by_task_and_menu'])
                prior = json.loads(rows[2]['learner_context_samples_by_task_and_menu'])[missing*6]
                delta = cells[missing*6]-prior
                cells[missing*6] = prior; cells[12*6] += delta
                row.update(learner_context_samples_by_task_and_menu=json.dumps(cells),
                           learner_context_ticks_by_task_and_menu=json.dumps([5*n for n in cells]),
                           learned_task_samples_this_process=json.dumps([sum(cells[t*6:t*6+6]) for t in range(19)]))
            self.assertFalse(R.coverage(rows,rows[0],'candidate')['sustained_accepted_review'])

    def test_missing_and_late_windows_are_explicit_failure(self):
        for rows in (history()[:-1], [history()[0], status(76000,15200,2000), *history()[2:]]):
            result = R.coverage(rows,rows[0],'candidate')
            self.assertFalse(result['timely_windows'])
            self.assertFalse(result['sustained_accepted_review'])
        rows = history(); rows[1] = status(75000,15000,2000)
        self.assertTrue(R.coverage(rows,rows[0],'candidate')['timely_windows'])

    def test_large_observation_gaps_do_not_suppress_frozen_evaluation(self):
        rows = [history()[0],status(130000,26000,2000),status(250000,50000,3000)]
        result = R.coverage(rows,rows[0],'candidate')
        self.assertFalse(result['timely_windows'])
        self.assertFalse(result['sustained_accepted_review'])
        self.assertTrue(any(not w['complete'] for w in result['windows']))
        rows = [history()[0],status(250000,50000,2000)]
        result = R.coverage(rows,rows[0],'candidate')
        self.assertIsNone(result['late'])
        self.assertFalse(result['sustained_accepted_review'])

    def test_invalid_origin_and_context_cells(self):
        for key,value in (('learner_context_base_trained_samples',S.BASE+1),('learner_context_ticks_by_task_and_menu',json.dumps([0]*114))):
            rows = history(); rows[1][key] = value
            with self.assertRaises(Exception):
                R.coverage(rows,rows[0],'candidate')
        rows = history(); rows[3] = status(99000,19800,4000)
        with self.assertRaises(Exception):
            R.coverage(rows,rows[0],'candidate')

    def test_context_partition_regression(self):
        rows = history()
        cells = json.loads(rows[1]['learner_context_samples_by_task_and_menu'])
        cells[0] -= 1; cells[1] += 1
        rows[1].update(learner_context_samples_by_task_and_menu=json.dumps(cells),learner_context_ticks_by_task_and_menu=json.dumps([5*n for n in cells]))
        with self.assertRaises(Exception):
            R.coverage(rows,rows[0],'candidate')

    def test_startup_must_be_actual_complete_population(self):
        for key,value in (('startup_observed_agents',511),('startup_expected_agents',True),('startup_review_agents',True),('startup_restored_checkpoint',False)):
            rows = history(); rows[0][key] = value
            with self.assertRaises(Exception):
                R.coverage(rows,rows[0],'candidate')
        rows = history(); alien = dict(rows[0],epoch_millis=999)
        with self.assertRaises(Exception):
            R.coverage(rows,alien,'candidate')
        rows[0]['startup_training_task_population'] = json.dumps([0]*18)
        with self.assertRaises(Exception):
            R.coverage(rows,rows[0],'candidate')

    def test_changed_initial_distribution_or_missing_contrast_fails(self):
        value = exposure(); value['control']['initial_training_population'][0] += 1; value['control']['initial_training_population'][1] -= 1
        self.assertFalse(R.interpretation(self.base,self.current,value)['sustained_exposure_contrast'])
        value = exposure(); value['control']['late']['review_share'] = .16
        self.assertFalse(R.interpretation(self.base,self.current,value)['shared_review_retention_supported'])
        value['control']['late']['review_share'] = .15
        self.assertTrue(R.interpretation(self.base,self.current,value)['sustained_exposure_contrast'])

    def test_exposure_types_are_not_truthiness(self):
        value = exposure(); value['candidate']['sustained_accepted_review'] = 1
        with self.assertRaises(Exception):
            R.interpretation(self.base,self.current,value)
        for bad in (True,-.1,1.1,float('nan')):
            value = exposure(); value['candidate']['late']['review_share'] = bad
            with self.assertRaises(Exception):
                R.interpretation(self.base,self.current,value)

    def test_shared_context_contract_does_not_assert_individual_cycles(self):
        measured = exposure()
        # No individual actor/task matrix exists in these genuine learner totals.
        self.assertNotIn('per_actor_task_decisions', measured['candidate'])
        self.assertTrue(R.interpretation(self.base, self.current, measured)['shared_review_retention_supported'])
        # Aggregate review alone cannot replace each task's accepted-data floor.
        rows = history()
        for row in rows[1:]:
            counts = json.loads(row['learned_task_samples_this_process'])
            counts[0] += sum(counts[1:12]); counts[1:12] = [0]*11
            cells = [n if menu == 0 else 0 for n in counts for menu in range(6)]
            row.update(learned_task_samples_this_process=json.dumps(counts),
                       learner_context_samples_by_task_and_menu=json.dumps(cells),
                       learner_context_ticks_by_task_and_menu=json.dumps([5*n for n in cells]))
        value = R.coverage(rows, rows[0], 'candidate')
        self.assertAlmostEqual(value['late']['review_share'], .2)
        self.assertFalse(value['sustained_accepted_review'])

    def test_new_endpoint_preserves_previous_failed_evidence(self):
        prior = json.loads((R.ROOT/'docs/verification/20261007-review-admission-results.json').read_text())
        self.assertIs(prior['qualification_passed'], False)
        self.assertEqual(prior['results']['candidate']['long']['passed_scenarios'], 7)
        self.assertEqual(prior['results']['control']['long']['passed_scenarios'], 6)
        self.assertEqual(R.CANDIDATE_RUNTIME, 'd30ef666d97ffa2b5de32729f4171b8bb4c5f81d4c4d431a55feb5f5d13a3430')
        self.assertEqual(R.SEEDS, (2026100721, 2026100722))

    def test_exact_finite_budget(self):
        for samples in (S.BASE+250000,S.BASE+300000):
            S.budget(samples,250000)
        for samples in (S.BASE+249999,S.BASE+300001):
            with self.assertRaises(Exception):
                S.budget(samples,250000)
        with self.assertRaises(Exception):
            S.budget(S.BASE+1000000,1000000)
        self.assertEqual(S.TARGETS,(250000,))
        self.assertEqual(S.SEEDS,R.SEEDS)
        self.assertEqual(R.M.CONTROL_SOURCE,R.CONTROL_SOURCE)
        self.assertEqual(len(R.CANDIDATE_RUNTIME),64)

    def test_create_only_and_symlink_rejection(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'receipt.json'; S.save(path,dict(deployment=False))
            with self.assertRaises(FileExistsError):
                S.save(path,dict(deployment=True))
            link = Path(directory)/'alias'; link.symlink_to(path)
            with self.assertRaises(Exception):
                S.load(link)
            self.assertFalse(S.load(path)['deployment'])

    def fixture_study(self):
        study = object.__new__(R.Study); study.source = 'fixture'
        study.guard = Mock(); study.identity = Mock(); study.train = Mock()
        return study

    def test_failed_parent_stops_before_any_learning(self):
        with tempfile.TemporaryDirectory() as directory,patch.object(R,'OUT',Path(directory)/'out'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study = self.fixture_study(); self.base[0]['scores'][11] = 25
            study.evaluate = Mock(return_value=self.base); study.run()
            study.train.assert_not_called()
            result = S.load(R.OUT/'outcome.json')
            self.assertEqual(result['stage'],'input-rejected'); self.assertEqual(result['frozen_trials'],832)

    def test_failed_exposure_still_finishes_all_reports(self):
        with tempfile.TemporaryDirectory() as directory,patch.object(R,'OUT',Path(directory)/'out'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study = self.fixture_study(); study.evaluate = Mock(side_effect=[self.base,self.current])
            measured = exposure(); measured['candidate']['sustained_accepted_review'] = False
            study.exposure = Mock(return_value=measured); study.run()
            self.assertEqual(study.train.call_count,2); self.assertEqual(study.evaluate.call_count,2)
            result = S.load(R.OUT/'outcome.json')
            self.assertEqual(result['frozen_trials'],2496); self.assertFalse(result['decision']['shared_review_retention_supported'])
            self.assertFalse(result['new_training_after_boundary']); self.assertFalse(result['deployment'])

    def test_operational_failure_is_retained_not_success(self):
        with tempfile.TemporaryDirectory() as directory,patch.object(R,'OUT',Path(directory)/'out'),patch.dict(S.ENV,{'EULA':'true'}),patch.object(S,'memory'):
            study = self.fixture_study(); study.evaluate = Mock(return_value=self.base)
            study.train.side_effect = RuntimeError('fixture interruption')
            with self.assertRaisesRegex(RuntimeError,'fixture interruption'):
                study.run()
            self.assertFalse((R.OUT/'outcome.json').exists())
            self.assertEqual(S.load(R.OUT/'failure.json')['error'],'RuntimeError')

    def test_cli_has_no_implicit_gameplay(self):
        env = dict(os.environ); env.pop('EULA',None)
        for arguments,success in (([],True),(['--help'],True),(['--run'],False)):
            before = R.OUT.exists()
            run = subprocess.run([sys.executable,str(R.ROOT/'tests/studies/shared_review.py'),*arguments],env=env,capture_output=True,text=True,timeout=15)
            self.assertEqual(run.returncode==0,success,run.stderr)
            self.assertEqual(R.OUT.exists(),before)


if __name__ == '__main__':
    unittest.main()
