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


MISSING_CONDITIONS = tuple('pickaxe-missing-' + cell for cell in
    ('top-left', 'top-center', 'top-right', 'handle-upper', 'handle-lower'))
CONDITIONS = ('none', 'workbench-open', 'pickaxe-grid') + MISSING_CONDITIONS


class ResetDiagnosticTests(unittest.TestCase):
    def arguments(self, *extra):
        with patch.dict('os.environ', {'EULA': 'true'}), contextlib.redirect_stderr(io.StringIO()):
            return holdout.arguments(['--policy', 'saved.bcmc', '--output', 'unused', '--tasks', '11', *extra])

    def test_arguments(self):
        self.assertEqual(self.arguments().reset_intervention, 'none')
        for condition in CONDITIONS:
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

    def test_missing_cell_argument_boundaries(self):
        self.assertEqual(tuple(holdout.RESET_INTERVENTIONS), CONDITIONS)
        for condition in MISSING_CONDITIONS:
            for task in ('10', '12', '17'):
                with self.subTest(condition=condition, task=task), self.assertRaises(SystemExit):
                    self.arguments('--reset-intervention', condition, '--tasks', task)
            with patch.dict('os.environ', {'EULA': 'true'}), contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                holdout.arguments(['--checkpoint', 'moving.bcmc', '--output', 'unused', '--tasks', '11', '--reset-intervention', condition])
            args, result = self.fixture(condition)
            for other in CONDITIONS:
                if other == condition:
                    continue
                with self.subTest(condition=condition, wrong_label=other), self.assertRaises(AssertionError):
                    changed = dict(result, reset_intervention=other)
                    self.verify(args, changed)

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
        for condition in CONDITIONS:
            with self.subTest(condition=condition):
                args,result=self.fixture(condition)
                self.assertIsNotNone(self.verify(args,result))

    def test_diagnostic_report_rejections(self):
        for condition in CONDITIONS[1:]:
            for field,value in [('diagnostic_only', False), ('diagnostic_only', 'true'), ('reset_intervention', 'none'),
                                ('reset_intervention_trials', 0), ('reset_intervention_trials', True), ('new_training_samples', 1)]:
                with self.subTest(condition=condition,field=field,value=value), self.assertRaises(AssertionError):
                    args,result=self.fixture(condition);result[field]=value;self.verify(args,result)
            for field in ('diagnostic_only', 'reset_intervention', 'reset_intervention_trials'):
                with self.subTest(condition=condition,missing=field), self.assertRaises(AssertionError):
                    args,result=self.fixture(condition);del result[field];self.verify(args,result)

    def test_assistance_never_accepted_as_control(self):
        args,_=self.fixture('none')
        for condition in CONDITIONS[1:]:
            with self.subTest(condition=condition), self.assertRaises(AssertionError):
                _,result=self.fixture(condition);self.verify(args,result)


class TableTraceTests(unittest.TestCase):
    def trace(self):
        return {
            'scope': 'table-inventory-pre-action', 'transitions': 10, 'inventory_states': 8,
            'correct_mask_states': [8] + [0] * 15, 'max_correct_cells': 0, 'max_surplus_units': 0,
            'partial_inventory_exits': 0, 'carried_planks_below_four_without_table_states': 0,
            'compatible_cursor_states_by_correct_cells': [4, 0, 0, 0, 0],
            'fill_probability_sum_by_correct_cells': [2.0, 0, 0, 0, 0],
            'single_unit_fill_probability_sum_by_correct_cells': [1.0, 0, 0, 0, 0],
            'filled_cell_transitions': [0] * 4, 'removed_cell_transitions': [0] * 4,
            'target_preview_states': 0, 'other_preview_states': 0,
            'target_collection_probability_sum': 0.0, 'other_collection_probability_sum': 0.0,
            'chosen_target_result_clicks': 0, 'chosen_other_result_clicks': 0,
            'observed_stick_gain_transitions': 0, 'observed_stick_units_gained': 0,
            'observed_table_units_gained': 0,
        }

    def test_valid_and_required_fields(self):
        holdout.verify_table_trace(self.trace(), 10)
        for key in self.trace():
            with self.subTest(missing=key), self.assertRaises(AssertionError):
                trace = self.trace(); del trace[key]; holdout.verify_table_trace(trace, 10)

    def test_malformed_values(self):
        for key, values in (
            ('scope', ['pickaxe-pre-action-observation', None]),
            ('transitions', [9, 11, True, '10', float('nan')]),
            ('inventory_states', [11, -1, True, 8.0]),
            ('correct_mask_states', [[8] * 15, [8] * 16, [True] + [0] * 15]),
            ('max_correct_cells', [5, -1]), ('max_surplus_units', [253]),
            ('other_collection_probability_sum', [.01, True, float('nan'), float('inf')]),
            ('observed_stick_units_gained', [4]), ('chosen_other_result_clicks', [1]),
            ('compatible_cursor_states_by_correct_cells', [[0, 0, 0, 0, 1], [9, 0, 0, 0, 0]]),
            ('fill_probability_sum_by_correct_cells', [[4.1, 0, 0, 0, 0], [float('nan'), 0, 0, 0, 0]]),
            ('single_unit_fill_probability_sum_by_correct_cells', [[2.1, 0, 0, 0, 0]]),
        ):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    trace = self.trace(); trace[key] = value; holdout.verify_table_trace(trace, 10)

    def test_report_scope_and_missing_trace(self):
        helper = ResetDiagnosticTests()
        args, result = helper.fixture('none')
        result['trials'][0]['diagnostics']['table_crafting'] = self.trace()
        with self.assertRaises(AssertionError):
            helper.verify(args, result)
        args.tasks = [10]; result['tasks'][0]['task'] = 10; result['trials'][0]['task'] = 10
        result['trials'][0]['diagnostics']['observations'] = 10
        self.assertIsNotNone(helper.verify(args, result))
        del result['trials'][0]['diagnostics']['table_crafting']
        with self.assertRaises(AssertionError):
            helper.verify(args, result)


if __name__=='__main__':
    unittest.main()
