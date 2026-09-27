"""Offline checks for explicit, non-certifying frozen-policy reset diagnostics."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import holdout


class ResetDiagnosticTests(unittest.TestCase):
    def arguments(self, *extra):
        with patch.dict('os.environ', {'EULA': 'true'}), contextlib.redirect_stderr(io.StringIO()):
            return holdout.arguments(['--policy', 'saved.bcmc', '--output', 'unused', '--tasks', '11', *extra])

    def test_arguments(self):
        self.assertEqual(self.arguments().reset_intervention, 'none')
        for condition in ('none', 'workbench-open', 'pickaxe-grid'):
            with self.subTest(condition=condition):
                self.assertEqual(self.arguments('--reset-intervention', condition).reset_intervention, condition)
        for extra in (['--reset-intervention', 'unknown'], ['--reset-intervention', 'workbench-open', '--tasks', '10'],
                      ['--reset-intervention', 'pickaxe-grid', '--tasks', '11', '12'], ['--cases', '0']):
            with self.subTest(extra=extra), self.assertRaises(SystemExit):
                self.arguments(*extra)
        with patch.dict('os.environ', {'EULA': 'true'}), contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            holdout.arguments(['--checkpoint', 'moving.bcmc', '--output', 'unused', '--tasks', '11', '--reset-intervention', 'workbench-open'])
        with patch.dict('os.environ', {'EULA': 'false'}), contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            holdout.arguments(['--policy', 'saved.bcmc', '--output', 'unused'])

    def fixture(self, condition):
        args=SimpleNamespace(cases=1, tasks=[11], seed=17, reset_intervention=condition)
        result={'complete': True, 'new_training_samples': 0, 'stochastic': True, 'seed': 17, 'cases_per_task': 1,
                'tasks': [{'task': 11, 'passed': 0, 'cases': 1}],
                'trials': [{'actor': 0, 'task': 11, 'seed': 11000050, 'elapsed_ticks': 3001, 'success': False,
                            'diagnostics': {'observations': 600, 'dig_decisions': 0, 'observed_max_target_mining_ticks': 0,
                                            'mean_abs_yaw_error': 1, 'mean_abs_pitch_error': 1, 'blocks_broken': 0, 'items_collected': 0}}]}
        if condition!='none':
            result.update(diagnostic_only=True, reset_intervention=condition, reset_intervention_trials=1)
        return args,result

    def verify(self, args, result):
        with tempfile.TemporaryDirectory() as directory:
            data=Path(directory)
            (data/'policy.bcmc').write_bytes(b'unchanged')
            (data/'exam-result.json').write_text(json.dumps(result), encoding='utf-8')
            return holdout.verify_result(args, data, b'unchanged', SimpleNamespace(returncode=0))

    def test_valid_reports(self):
        for condition in ('none', 'workbench-open', 'pickaxe-grid'):
            with self.subTest(condition=condition):
                args,result=self.fixture(condition)
                self.assertIsNotNone(self.verify(args,result))

    def test_diagnostic_report_rejections(self):
        for condition in ('workbench-open', 'pickaxe-grid'):
            for field,value in [('diagnostic_only', False), ('diagnostic_only', 'true'), ('reset_intervention', 'none'),
                                ('reset_intervention_trials', 0), ('reset_intervention_trials', True), ('new_training_samples', 1)]:
                with self.subTest(condition=condition,field=field,value=value), self.assertRaises(AssertionError):
                    args,result=self.fixture(condition);result[field]=value;self.verify(args,result)
            for field in ('diagnostic_only', 'reset_intervention', 'reset_intervention_trials'):
                with self.subTest(condition=condition,missing=field), self.assertRaises(AssertionError):
                    args,result=self.fixture(condition);del result[field];self.verify(args,result)

    def test_assistance_never_accepted_as_control(self):
        args,_=self.fixture('none')
        for condition in ('workbench-open', 'pickaxe-grid'):
            with self.subTest(condition=condition), self.assertRaises(AssertionError):
                _,result=self.fixture(condition);self.verify(args,result)


if __name__=='__main__':
    unittest.main()
