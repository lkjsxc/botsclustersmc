"""Offline checks for explicit, non-certifying frozen-policy reset diagnostics."""
import contextlib
import copy
import os
import subprocess
import sys
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
                'trials': [{'actor': 0, 'task': 11, 'seed': 11000050, 'elapsed_ticks': 3001, 'success': False, 'distance': 1.0,
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
        result['trials'][0]['seed'] = 17 + 10*1000003
        result['trials'][0]['diagnostics']['observations'] = 10
        self.assertIsNotNone(helper.verify(args, result))
        del result['trials'][0]['diagnostics']['table_crafting']
        with self.assertRaises(AssertionError):
            helper.verify(args, result)


class ReportIntegrityTests(unittest.TestCase):
    def fixture(self, seed=17):
        args, template = ResetDiagnosticTests().fixture('none')
        args.tasks, args.cases, args.seed = [11, 0, 13], 2, seed
        result = dict(template, seed=seed, cases_per_task=2, tasks=[], trials=[])
        for index, task in enumerate(args.tasks):
            result['tasks'].append({'task': task, 'cases': 2, 'passed': 1})
            for case in range(2):
                trial = copy.deepcopy(template['trials'][0])
                value = seed + task*1000003 + case*104729
                trial.update(actor=index*2+case, task=task, success=case == 0,
                             seed=(value + 2**63) % 2**64 - 2**63)
                result['trials'].append(trial)
        return args, result

    def verify(self, args, result):
        return ResetDiagnosticTests().verify(args, result)

    def test_complete_noncanonical_task_order_and_trial_order(self):
        args, result = self.fixture()
        self.assertEqual(self.verify(args, result), result)
        result['trials'].reverse()
        self.assertEqual(self.verify(args, result), result)

    def test_java_signed_long_seeds(self):
        for seed in (-2**63, 2**63-1):
            args, result = self.fixture(seed)
            self.assertEqual(self.verify(args, result), result)
        args, result = self.fixture(2**63-1)
        self.assertEqual(result['trials'][2]['seed'], 9223372036854775807)
        self.assertEqual(result['trials'][3]['seed'], -9223372036854671080)
        result['trials'][3]['seed'] = 9223372036854880536
        with self.assertRaises(AssertionError):
            self.verify(args, result)

    def test_report_flags_and_counts_are_not_truthy_or_coerced(self):
        for key, values in (
            ('complete', [False, 1, 'true', None]), ('stochastic', [False, 1, 'true']),
            ('new_training_samples', [1, False, 0.0, '0']),
            ('seed', [True, 17.0, '17', 18]), ('cases_per_task', [True, 2.0, '2', 1]),
            ('diagnostic_only', [True, 0, 'false']),
            ('reset_intervention', ['pickaxe-grid', None]),
            ('reset_intervention_trials', [1, False, 0.0]),
        ):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result[key] = value; self.verify(args, result)

    def test_every_required_field_is_checked(self):
        for section in ('root', 'summary', 'trial', 'diagnostics'):
            args, original = self.fixture()
            def select(result):
                if section == 'root': return result
                if section == 'summary': return result['tasks'][0]
                if section == 'trial': return result['trials'][0]
                return result['trials'][0]['diagnostics']
            for key in select(original):
                with self.subTest(section=section, key=key), self.assertRaises(AssertionError):
                    changed = copy.deepcopy(original); del select(changed)[key]; self.verify(args, changed)

    def test_shapes_coverage_duplicates_and_unknown_tasks(self):
        for key in ('tasks', 'trials'):
            for value in (None, {}, '', [], [None]):
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result[key] = value; self.verify(args, result)
        for change in ('missing', 'extra', 'duplicate', 'unknown', 'order'):
            with self.subTest(change=change), self.assertRaises(AssertionError):
                args, result = self.fixture()
                if change == 'missing': result['trials'].pop()
                elif change == 'extra': result['trials'].append(copy.deepcopy(result['trials'][0]))
                elif change == 'duplicate': result['trials'][1]['actor'] = 0
                elif change == 'unknown': result['trials'][0]['task'] = 17
                else: result['tasks'].reverse()
                self.verify(args, result)

    def test_actor_task_swap_cannot_hide_behind_equal_totals(self):
        args, result = self.fixture()
        result['trials'][0]['actor'], result['trials'][2]['actor'] = 2, 0
        with self.assertRaisesRegex(AssertionError, 'Actor/task'):
            self.verify(args, result)

    def test_case_seed_swap_cannot_hide_behind_equal_totals(self):
        args, result = self.fixture()
        result['trials'][0]['actor'], result['trials'][1]['actor'] = 1, 0
        with self.assertRaisesRegex(AssertionError, 'Trial seed'):
            self.verify(args, result)

    def test_trial_identifiers_and_success_are_exact_types(self):
        for key, values in (
            ('actor', [False, 0.0, '0', -1, 6]), ('task', [11.0, '11', True]),
            ('seed', [11000050.0, '11000050', True, 11000051]),
            ('success', [1, 0, 'true', None]), ('elapsed_ticks', [True, 0, -1, 1.5]),
            ('distance', [True, -1, float('nan'), float('inf'), 10**400]),
        ):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result['trials'][0][key] = value; self.verify(args, result)

    def test_diagnostics_counts_angles_and_denominators(self):
        fields = ('observations', 'dig_decisions', 'observed_max_target_mining_ticks', 'blocks_broken', 'items_collected')
        for key in fields:
            for value in (True, -1, 1.5, '1', 2**63):
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result['trials'][0]['diagnostics'][key] = value; self.verify(args, result)
        for key in ('mean_abs_yaw_error', 'mean_abs_pitch_error'):
            for value in (True, -1, 181, float('nan'), float('inf'), 10**400):
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result['trials'][0]['diagnostics'][key] = value; self.verify(args, result)
        for key, value in (('observations', 0), ('dig_decisions', 601)):
            with self.subTest(key=key), self.assertRaises(AssertionError):
                args, result = self.fixture(); result['trials'][0]['diagnostics'][key] = value; self.verify(args, result)

    def test_summary_counts_and_success_totals(self):
        for key, values in (('task', [True, 11.0]), ('cases', [True, 2.0, 1]), ('passed', [True, 1.0, -1, 0, 3])):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); result['tasks'][0][key] = value; self.verify(args, result)

    def test_invalid_declarations_fail_closed(self):
        for key, values in (('cases', [0, 257, True]), ('tasks', [[], [11, 11], [True], [18], None]),
                            ('seed', [True, 2**63, -2**63-1]), ('reset_intervention', ['unknown', 'pickaxe-grid'])):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    args, result = self.fixture(); setattr(args, key, value); self.verify(args, result)
        args, result = self.fixture(); args.tasks = list(range(18)); args.cases = 256
        with self.assertRaises(AssertionError): self.verify(args, result)

    def test_json_duplicate_nonfinite_malformed_and_utf8(self):
        invalid = [b'{"complete":false,"complete":true}', b'{"trial":{"actor":0,"actor":1}}',
                   b'{"unused":NaN}', b'{"unused":Infinity}', b'{"unused":-Infinity}',
                   b'{"unused":1e9999}', b'{"unused":"\xff"}', b'{', b'[] trailing', b'['*2000]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'exam-result.json'
            for value in invalid:
                with self.subTest(value=value[:40]), self.assertRaises(AssertionError):
                    path.write_bytes(value); holdout.read_report(path)
        for value in ([], None, True, 'report'):
            args, _ = self.fixture()
            with self.assertRaises(AssertionError): self.verify(args, value)

    def test_bounded_read_uses_limit_plus_one(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'bounded'; path.write_bytes(b'1234')
            self.assertEqual(holdout.read_bounded(path, 4), b'1234')
            with self.assertRaises(AssertionError): holdout.read_bounded(path, 3)
        stream = io.BytesIO(b'12345')
        with patch.object(Path, 'open', return_value=stream), patch.object(stream, 'read', wraps=stream.read) as read:
            with self.assertRaises(AssertionError): holdout.read_bounded(Path('unused'), 3)
            read.assert_called_once_with(4)

    def test_file_boundaries_and_process_failure(self):
        args, result = self.fixture()
        with tempfile.TemporaryDirectory() as directory:
            data = Path(directory)
            (data/'exam-result.json').write_text(json.dumps(result), encoding='utf-8')
            for label in ('process', 'policy', 'policy-growth', 'checkpoint', 'failure-marker'):
                with self.subTest(label=label):
                    (data/'policy.bcmc').write_bytes(b'unchanged')
                    process = SimpleNamespace(returncode=0)
                    if label == 'process': process.returncode = 1
                    elif label == 'policy': (data/'policy.bcmc').write_bytes(b'modified!')
                    elif label == 'policy-growth': (data/'policy.bcmc').write_bytes(b'unchanged!')
                    elif label == 'checkpoint': (data/'training-next.bcmc').write_bytes(b'')
                    else: (data/'exam-failed.txt').write_bytes(b'')
                    with self.assertRaises(AssertionError): holdout.verify_result(args, data, b'unchanged', process)
                    (data/'training-next.bcmc').unlink(missing_ok=True)
                    (data/'exam-failed.txt').unlink(missing_ok=True)


class OptimizationTests(unittest.TestCase):
    def test_real_optimized_interpreters(self):
        for flags, optimize in ((['-O'], '0'), (['-OO'], '0'), ([], '2')):
            with self.subTest(flags=flags, environment=optimize):
                result = subprocess.run([sys.executable, *flags, '-m', 'unittest', '-q',
                    'test_holdout.ResetDiagnosticTests', 'test_holdout.TableTraceTests', 'test_holdout.ReportIntegrityTests'],
                    cwd=Path(__file__).parent, env=dict(os.environ, PYTHONOPTIMIZE=optimize),
                    text=True, capture_output=True, timeout=30)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__=='__main__':
    unittest.main()
