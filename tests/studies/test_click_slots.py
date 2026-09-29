"""Offline qualification of the finite click-slot study; no Minecraft is launched."""
import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import click_slots as runner
import click_slots_support as S
import test_holdout as fixtures


def rows():
    return [dict(arm=arm, seed=seed, condition=condition,
        scores=[64] * 11 + [0] if condition == 'none' else [12],
        trials=768 if condition == 'none' else 32, new_training_samples=0)
        for arm in S.ARMS for seed in S.SEEDS for condition in S.CONDITIONS]


def row(collection, arm='candidate', seed=S.SEEDS[0], condition='none'):
    return next(r for r in collection if (r['arm'], r['seed'], r['condition']) == (arm, seed, condition))


def report(args):
    _, fixture = fixtures.ResetDiagnosticTests().fixture('none')
    result = dict(complete=True, new_training_samples=0, stochastic=True,
        seed=args.seed, cases_per_task=args.cases, tasks=[], trials=[])
    if args.reset_intervention != 'none':
        result.update(diagnostic_only=True, reset_intervention=args.reset_intervention,
            reset_intervention_trials=args.cases * len(args.tasks))
    for task in args.tasks:
        result['tasks'].append(dict(task=task, passed=0, cases=args.cases))
        for case in range(args.cases):
            trial = copy.deepcopy(fixture['trials'][0])
            trial.update(actor=len(result['trials']), task=task,
                seed=args.seed + task * 1000003 + case * 104729)
            if task != 11:
                del trial['diagnostics']['crafting']
            if task == 10:
                trial['diagnostics']['observations'] = 10
                trial['diagnostics']['table_crafting'] = fixtures.TableTraceTests().trace()
            result['trials'].append(trial)
    return result


class Gates(unittest.TestCase):
    def test_complete_fixed_denominators(self):
        before = rows()
        self.assertEqual(len(S.vectors(before)), 24)
        self.assertEqual(sum(r['trials'] for r in before), 3712)
        self.assertTrue(S.gate(before, before)['retained'])
        self.assertFalse(S.gate(before, before)['acquisition'])
        args = S.spec(S.SEEDS[0], 'none'); args.tasks.clear()
        self.assertEqual(S.spec(S.SEEDS[0], 'none').tasks, list(range(12)))

    def test_every_condition_required_exactly_once(self):
        for index in range(24):
            for changed in (rows()[:index] + rows()[index+1:], rows() + [rows()[index]]):
                with self.subTest(index=index), self.assertRaises(AssertionError):
                    S.vectors(changed)
        for field, values in (
            ('arm', ['neither']), ('seed', [True, float(S.SEEDS[0]), 7]),
            ('condition', ['pickaxe-grid']), ('scores', [[64]*11, [True]*12, [65]*12, [0.0]*12]),
            ('trials', [767, 769, 768.0, True]), ('new_training_samples', [1, True, 0.0]),
        ):
            for value in values:
                changed = rows(); changed[0][field] = value
                with self.subTest(field=field, value=value), self.assertRaises(AssertionError):
                    S.vectors(changed)

    def test_other_task_retention_is_per_arm_task_and_seed(self):
        before = rows()
        for arm in S.ARMS:
            for seed in S.SEEDS:
                for task in range(11):
                    after = rows(); row(after, arm, seed)['scores'][task] = 60
                    self.assertTrue(S.gate(before, after)['retained'])
                    row(after, arm, seed)['scores'][task] = 59
                    self.assertFalse(S.gate(before, after)['retained'])

    def test_every_candidate_cell_is_retained_without_control_veto(self):
        for seed in S.SEEDS:
            for condition in S.CONDITIONS[1:]:
                after = rows()
                row(after, seed=seed, condition=condition)['scores'] = [8]
                row(after, 'control', seed, condition)['scores'] = [0]
                decision = S.gate(rows(), after)
                self.assertTrue(decision['retained'])
                self.assertFalse(all(d['control_partial_retained'] for d in decision['details']))
                row(after, seed=seed, condition=condition)['scores'] = [7]
                self.assertFalse(S.gate(rows(), after)['retained'])

    def test_acquisition_needs_absolute_and_control_margin_on_each_seed(self):
        after = rows()
        for seed in S.SEEDS:
            row(after, seed=seed)['scores'][11] = 8
            row(after, 'control', seed)['scores'][11] = 4
        self.assertTrue(S.gate(rows(), after)['acquisition'])
        for seed in S.SEEDS:
            for arm, value in (('candidate', 7), ('control', 5)):
                changed = copy.deepcopy(after); row(changed, arm, seed)['scores'][11] = value
                self.assertFalse(S.gate(rows(), changed)['acquisition'])

    def test_assisted_gain_is_not_ordinary_acquisition(self):
        after = rows()
        for seed in S.SEEDS:
            for condition, value in zip(S.CONDITIONS[1:], (4, 22, 22, 22, 22)):
                row(after, seed=seed, condition=condition)['scores'] = [value]
        decision = S.gate(rows(), after)
        self.assertTrue(decision['assisted_gain'])
        self.assertFalse(decision['acquisition'])
        for seed in S.SEEDS:
            changed = copy.deepcopy(after)
            row(changed, seed=seed, condition=S.CONDITIONS[1])['scores'] = [3]
            self.assertFalse(S.gate(rows(), changed)['assisted_gain'])
            changed = copy.deepcopy(after)
            row(changed, seed=seed, condition=S.CONDITIONS[2])['scores'] = [21]
            self.assertFalse(S.gate(rows(), changed)['assisted_gain'])

    def test_task_accounting_and_frozen_inputs_are_strict(self):
        counts = [0]*19; counts[11] = 250000
        self.assertEqual(S.task_counts(json.dumps(counts)), counts)
        for index in range(19):
            for value in (True, -1, 0.0, '0', 2**63):
                changed = counts.copy(); changed[index] = value
                with self.subTest(index=index, value=value), self.assertRaises(AssertionError):
                    S.task_counts(json.dumps(changed))
            if index != 11:
                changed = counts.copy(); changed[index] = 1
                with self.assertRaises(AssertionError): S.task_counts(json.dumps(changed))
        for invalid in ('[]', '[0]' * 200, 'null', '"not a list"', counts):
            with self.assertRaises((AssertionError, ValueError)): S.task_counts(invalid)


class Evidence(unittest.TestCase):
    def fixture(self, directory, condition='pickaxe-missing-top-left'):
        args = S.spec(S.SEEDS[0], condition)
        data = directory/'server/plugins/BotsClustersMC'; data.mkdir(parents=True)
        policy, runtime = directory/'input.bcmc', directory/'input.jar'
        policy.write_bytes(b'fixed policy'); runtime.write_bytes(b'fixed runtime')
        (directory/'runtime.jar').write_bytes(runtime.read_bytes())
        (data/'policy.bcmc').write_bytes(policy.read_bytes())
        (data.parent/'exam.jar').write_bytes(b'compiled exam')
        metadata = dict(policy_sha256=S.sha(policy), runtime_jar_sha256=S.sha(runtime),
            exam_jar_sha256=S.sha(data.parent/'exam.jar'), tasks=args.tasks,
            seed=args.seed, cases_per_task=args.cases, reset_intervention=condition,
            diagnostic_only=condition != 'none', bind='127.0.0.1', port=31060,
            input_kind='inference-policy')
        (directory/'metadata.json').write_text(json.dumps(metadata))
        (directory/'result.json').write_text(json.dumps(report(args)))
        return policy, runtime, args, data

    def verify(self, directory, policy, runtime, args):
        return S.verify_evaluation(directory, policy, runtime, args.seed, args.reset_intervention, 31060)

    def test_complete_ordinary_and_all_assisted_evidence(self):
        for condition in S.CONDITIONS:
            with self.subTest(condition=condition), tempfile.TemporaryDirectory() as d:
                directory = Path(d); policy, runtime, args, _ = self.fixture(directory, condition)
                self.assertEqual(self.verify(directory, policy, runtime, args), [0]*len(args.tasks))

    def test_metadata_types_and_identity_are_bound(self):
        for key, value in (
            ('policy_sha256', '0'*64), ('runtime_jar_sha256', '0'*64),
            ('exam_jar_sha256', '0'*64), ('tasks', [11.0]),
            ('seed', float(S.SEEDS[0])), ('cases_per_task', 32.0), ('port', 31060.0),
            ('seed', S.SEEDS[1]), ('cases_per_task', 31), ('tasks', [10]),
            ('diagnostic_only', False), ('diagnostic_only', 1),
            ('reset_intervention', 'none'), ('bind', '0.0.0.0'), ('port', 25565),
            ('input_kind', 'canonical-checkpoint'),
        ):
            with self.subTest(key=key, value=value), tempfile.TemporaryDirectory() as d:
                directory = Path(d); policy, runtime, args, _ = self.fixture(directory)
                metadata = S.load(directory/'metadata.json'); metadata[key] = value
                (directory/'metadata.json').write_text(json.dumps(metadata))
                with self.assertRaises(AssertionError): self.verify(directory, policy, runtime, args)

    def test_changed_weights_runtime_exam_and_learned_state_are_rejected(self):
        for relative in ('server/plugins/BotsClustersMC/policy.bcmc', 'runtime.jar',
                'server/plugins/exam.jar', 'server/plugins/BotsClustersMC/training.bcmc',
                'server/plugins/BotsClustersMC/training-backup.bcmc', 'server/plugins/BotsClustersMC/exam-failed.txt'):
            with self.subTest(path=relative), tempfile.TemporaryDirectory() as d:
                directory = Path(d); policy, runtime, args, _ = self.fixture(directory)
                (directory/relative).write_bytes(b'changed')
                with self.assertRaises(AssertionError): self.verify(directory, policy, runtime, args)

    def test_partial_duplicate_and_misattributed_trials_are_rejected(self):
        for change in ('partial', 'duplicate', 'seed', 'task', 'summary', 'learning'):
            with self.subTest(change=change), tempfile.TemporaryDirectory() as d:
                directory = Path(d); policy, runtime, args, _ = self.fixture(directory)
                result = S.load(directory/'result.json')
                if change == 'partial': result['trials'].pop()
                if change == 'duplicate': result['trials'][1] = result['trials'][0]
                if change == 'seed': result['trials'][0]['seed'] += 1
                if change == 'task': result['trials'][0]['task'] = 13
                if change == 'summary': result['tasks'][0]['passed'] = 1
                if change == 'learning': result['new_training_samples'] = 1
                (directory/'result.json').write_text(json.dumps(result))
                with self.assertRaises(AssertionError): self.verify(directory, policy, runtime, args)

    def test_create_only_bounded_strict_json_and_symlink_io(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); target = root/'evidence'
            S.write(target, b'abcd'); self.assertEqual(S.read(target, 4), b'abcd')
            with self.assertRaises(AssertionError): S.read(target, 3)
            with self.assertRaises(FileExistsError): S.write(target, b'wrong')
            self.assertEqual(target.read_bytes(), b'abcd')
            for text in ('{"x":1,"x":2}', '{"x":NaN}', '{"x":Infinity}'):
                target.write_text(text)
                with self.assertRaises(AssertionError): S.load(target)
            alias = root/'alias'; alias.symlink_to(target)
            with self.assertRaises(AssertionError): S.read(alias)
            sub = root/'sub'; sub.mkdir(); alias.unlink(); alias.symlink_to(sub, target_is_directory=True)
            with self.assertRaises(AssertionError): S.write(alias/'new', b'no')
            with self.assertRaises(ValueError): S.save(root/'nonfinite', dict(x=float('nan')))
            self.assertFalse((root/'nonfinite').exists())


class Orchestration(unittest.TestCase):
    def run_study(self, directory, evaluations):
        study = runner.Study.__new__(runner.Study)
        study.source = 'source'; study.identities = {}; study.checkpoint = dict(control='c', candidate='x')
        with patch.object(runner, 'OUT', directory), patch.object(runner, 'memory'), \
                patch.object(study, 'identity') as identity, patch.object(study, 'train') as train, \
                patch.object(study, 'evaluate', side_effect=evaluations), patch('builtins.print'):
            study.run()
        self.assertEqual(identity.call_count, 2)
        return train, S.load(directory/'outcome.json')

    def test_baseline_rejection_launches_no_learner(self):
        baseline = rows(); row(baseline)['scores'][0] = 59
        with tempfile.TemporaryDirectory() as d:
            train, outcome = self.run_study(Path(d)/'study', [baseline])
            train.assert_not_called()
            self.assertEqual((outcome['stage'], outcome['reports'], outcome['unique_trials']), ('baseline', 24, 3712))
            self.assertFalse(outcome['full_budget_completed']); self.assertFalse(outcome['deployment'])

    def test_early_failure_preserves_complete_phase_and_prevents_final(self):
        early = rows(); row(early, condition=S.CONDITIONS[-1])['scores'] = [7]
        with tempfile.TemporaryDirectory() as d:
            train, outcome = self.run_study(Path(d)/'study', [rows(), early])
            self.assertEqual(train.call_args_list, [unittest.mock.call(arm, S.TARGETS[0]) for arm in S.ARMS])
            self.assertEqual((outcome['stage'], outcome['reports'], outcome['unique_trials']), ('early', 48, 7424))
            self.assertFalse(outcome['useful_pilot']); self.assertFalse(outcome['deployment'])

    def test_full_retention_without_acquisition_is_not_useful(self):
        with tempfile.TemporaryDirectory() as d:
            train, outcome = self.run_study(Path(d)/'study', [rows(), rows(), rows()])
            self.assertEqual(train.call_args_list, [unittest.mock.call(arm, target) for target in S.TARGETS for arm in S.ARMS])
            self.assertEqual((outcome['reports'], outcome['unique_trials']), (72, 11136))
            self.assertTrue(outcome['full_budget_completed']); self.assertFalse(outcome['useful_pilot'])
            self.assertFalse(outcome['deployment'])

    def test_operational_failure_retains_error_not_outcome(self):
        with tempfile.TemporaryDirectory() as d:
            directory = Path(d)/'study'
            with self.assertRaisesRegex(RuntimeError, 'fixture failure'):
                self.run_study(directory, [RuntimeError('fixture failure')])
            self.assertTrue((directory/'declaration.json').is_file())
            self.assertIn('fixture failure', S.load(directory/'failure.json')['error'])
            self.assertFalse((directory/'outcome.json').exists())

    def test_initial_checkpoint_export_must_equal_evaluated_policy(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); checkpoint = root/'training.bcmc'; checkpoint.write_bytes(b'checkpoint')
            baseline = root/'baseline.bcmc'; baseline.write_bytes(b'different policy')
            study = runner.Study.__new__(runner.Study)
            study.runtime = dict(control=root/'runtime.jar'); study.policy = dict(control=baseline)
            def export(command, log):
                if 'export' in command:
                    Path(command[-1]).write_bytes(b'actual exported policy')
                    return f'updates=332305, trained_samples={S.BASE}'
                return 'native fixture audit'
            with patch.object(runner, 'execute', side_effect=export), self.assertRaises(AssertionError):
                study.identity('control', checkpoint, root, 'input', True)
            self.assertFalse((root/'input-identity.json').exists())


class ProcessBoundaries(unittest.TestCase):
    def status(self):
        counts = [0]*19; counts[11] = 250
        return dict(epoch_millis=100000, state='running', learning_task_scope=11,
            protected_prior_policy=True, inference_failed=0, inference_rejected=0,
            learner_rejected_samples=0, learner_stale_samples=0, retired_agents=0,
            learned_task_samples_this_process=json.dumps(counts), trained_samples=S.BASE+250, active_agents=512)

    def test_status_freshness_counter_and_scope(self):
        status = self.status()
        for now in (95000, 100000, 145000):
            self.assertEqual(S.training_samples(status, S.BASE, 90000, now), S.BASE+250)
        for now in (94999, 145001):
            with self.assertRaises(AssertionError): S.training_samples(status, S.BASE, 90000, now)
        old = dict(status, epoch_millis=89999)
        self.assertIsNone(S.training_samples(old, S.BASE, 90000, 100000))
        with self.assertRaises(AssertionError): S.training_samples(old, S.BASE, 90000, 100000, status)
        for key, values in (
            ('state', ['stopped']), ('learning_task_scope', [10, 11.0, True]),
            ('protected_prior_policy', [False, 1]), ('trained_samples', [S.BASE-1, float(S.BASE)]),
            ('active_agents', [513, -1, True]), ('epoch_millis', [True, 100000.0]),
            ('inference_failed', [1, True]), ('inference_rejected', [1, True]),
            ('learner_rejected_samples', [1, True]), ('learner_stale_samples', [1, True]),
            ('retired_agents', [1, True]),
        ):
            for value in values:
                changed = dict(status, **{key: value})
                with self.subTest(key=key, value=value), self.assertRaises(AssertionError):
                    S.training_samples(changed, S.BASE, 90000, 100000)
        for changed in (dict(status, epoch_millis=99999), dict(status, trained_samples=S.BASE+249),
                dict(status, learned_task_samples_this_process=json.dumps([0]*19))):
            with self.assertRaises(AssertionError): S.training_samples(changed, S.BASE, 90000, 100000, status)
        self.assertEqual(S.training_samples(status, S.BASE, 90000, 100000, status), S.BASE+250)

    def test_exact_accepted_sample_budget_and_overshoot(self):
        for target in S.TARGETS:
            S.budget(S.BASE+target, target); S.budget(S.BASE+target+S.OVERSHOOT, target)
            for samples in (S.BASE+target-1, S.BASE+target+S.OVERSHOOT+1, float(S.BASE+target), True):
                with self.subTest(target=target, samples=samples), self.assertRaises(AssertionError):
                    S.budget(samples, target)
        for target in (True, 1, 250000.0, -1):
            with self.assertRaises(AssertionError): S.budget(S.BASE+250000, target)
        study = runner.Study.__new__(runner.Study)
        for arm, target in (('other', 250000), ('control', 250000.0), ('candidate', 1)):
            with self.assertRaises(AssertionError): study.train(arm, target)

    def test_memory_floor_uses_effective_cgroup_headroom(self):
        files = {'/proc/meminfo': 'MemAvailable: 8388608 kB\n', '/sys/fs/cgroup/memory.max': 'max'}
        with patch.object(Path, 'read_text', autospec=True, side_effect=lambda path: files[str(path)]):
            self.assertEqual(S.memory(), 8*1024**3)
            files['/sys/fs/cgroup/memory.max'] = str(4*1024**3)
            files['/sys/fs/cgroup/memory.current'] = str(2*1024**3)
            self.assertEqual(S.memory(), 2*1024**3)
            files['/sys/fs/cgroup/memory.current'] = str(2*1024**3+1)
            with self.assertRaises(AssertionError): S.memory()

    def test_execute_success_failure_timeout_and_escalation_keep_log(self):
        from unittest.mock import Mock
        for mode in ('success', 'failure', 'timeout', 'escalation'):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as d:
                log = Path(d)/'command.log'; process = Mock(pid=12345)
                process.poll.return_value = None if mode in ('timeout', 'escalation') else 0
                process.wait.side_effect = {'success': [0], 'failure': [7],
                    'timeout': [subprocess.TimeoutExpired('fixture', 1), 0],
                    'escalation': [subprocess.TimeoutExpired('fixture', 1), subprocess.TimeoutExpired('fixture', 45), 0]}[mode]
                with patch.object(S.subprocess, 'Popen', return_value=process) as launch, patch.object(S.os, 'killpg') as kill:
                    if mode == 'success': self.assertEqual(S.execute(['fixture'], log, timeout=1), '')
                    else:
                        with self.assertRaises(AssertionError if mode == 'failure' else subprocess.TimeoutExpired):
                            S.execute(['fixture'], log, timeout=1)
                    self.assertTrue(log.is_file()); self.assertTrue(launch.call_args.kwargs['start_new_session'])
                    expected = [] if mode in ('success', 'failure') else [unittest.mock.call(12345, S.signal.SIGTERM)]
                    if mode == 'escalation': expected.append(unittest.mock.call(12345, S.signal.SIGKILL))
                    self.assertEqual(kill.call_args_list, expected)

    def test_source_and_input_guards_detect_mutation(self):
        with tempfile.TemporaryDirectory() as d:
            artifact = Path(d)/'runtime.jar'; artifact.write_bytes(b'fixed')
            study = runner.Study.__new__(runner.Study); study.source = 'candidate-source'
            study.files = [artifact]; study.identities = {str(artifact): S.sha(artifact)}
            def git(root, *args):
                return '' if args[0] == 'status' else study.source if root == runner.ROOT else S.CONTROL_SOURCE
            with patch.object(runner, 'git', side_effect=git):
                study.guard(); artifact.write_bytes(b'changed')
                with self.assertRaises(AssertionError): study.guard()
            artifact.write_bytes(b'fixed')
            for values in (['wrong'], [study.source, ' M changed'], [study.source, '', 'wrong'],
                    [study.source, '', S.CONTROL_SOURCE, ' M changed']):
                with patch.object(runner, 'git', side_effect=values), self.assertRaises(AssertionError): study.guard()


class EntryPoint(unittest.TestCase):
    def test_help_missing_opt_in_and_unknown_arguments_never_launch(self):
        import contextlib
        import io
        for args in ([], ['--help'], ['--unknown']):
            with patch.object(runner, 'Study') as study, patch.object(runner, 'qualify') as qualify, \
                    contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                runner.main(args)
            study.assert_not_called(); qualify.assert_not_called()

    def test_qualification_failure_prevents_construction(self):
        with patch.object(runner, 'Study') as study, patch.object(runner, 'qualify', side_effect=AssertionError('failed')):
            with self.assertRaisesRegex(AssertionError, 'failed'): runner.main(['--run'])
            study.assert_not_called()
        with patch.object(runner, 'Study') as study, patch.object(runner, 'qualify') as qualify:
            runner.main(['--run'])
            qualify.assert_called_once_with(); study.assert_called_once_with(); study.return_value.run.assert_called_once_with()


if __name__ == '__main__':
    unittest.main()
