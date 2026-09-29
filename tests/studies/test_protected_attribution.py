"""Offline-only evidence checks. Never starts Minecraft or creates study state."""
from copy import deepcopy
from pathlib import Path
from unittest import TestCase, main
from unittest.mock import patch
import os, subprocess, sys, tempfile
import protected_attribution as A


def rows(scores=None):
    scores = scores or {0: 24, 3: 12, 4: 23, 7: 11}
    return [dict(mask=m, seed=s, condition=c,
        scores=[32, 0] if c == 'none' else [scores[m]],
        trials=64 if c == 'none' else 32, new_training_samples=0)
        for m in A.MASKS for s in A.SEEDS for c in A.CONDITIONS]


class Evidence(TestCase):
    def reject(self, operation):
        with self.assertRaises(AssertionError):
            operation()

    def test_complete_matrix(self):
        r = A.matrix(rows())
        self.assertEqual((r['reports'], r['trials'], r['new_training_samples']), (48, 1792, 0))
        self.assertTrue(r['diagnostic_only']); self.assertFalse(r['deployment'])
        for cell in r['cells']:
            self.assertEqual(cell['representation_deltas'], [-12]*4)
            self.assertEqual(cell['actor_deltas'], [-1]*4)
            self.assertTrue(cell['consistent_representation_drop'])
            self.assertFalse(cell['consistent_actor_drop'])
        self.assertEqual(A.matrix(list(reversed(rows())))['cells'], r['cells'])

    def test_other_factor_and_exact_margin(self):
        for scores, factor in [({0:24, 3:16, 4:23, 7:15}, 'representation'),
                ({0:24, 3:23, 4:16, 7:15}, 'actor')]:
            for cell in A.matrix(rows(scores))['cells']:
                self.assertTrue(cell['consistent_'+factor+'_drop'])
                self.assertEqual(cell[factor+'_deltas'], [-8]*4)

    def test_every_background_seed_required(self):
        for mask in (3, 7):
            for seed in A.SEEDS:
                r = rows({0:24, 3:16, 4:23, 7:15})
                for row in r:
                    if row['mask'] == mask and row['seed'] == seed and row['condition'] == A.CONDITIONS[1]:
                        row['scores'][0] += 1
                outcome = A.matrix(r)
                self.assertFalse(outcome['cells'][0]['consistent_representation_drop'])
                self.assertTrue(outcome['cells'][1]['consistent_representation_drop'])

    def test_never_pool_cells_or_zero_baselines(self):
        r = rows({0:24, 3:0, 4:24, 7:0})
        for row in r:
            if 'handle' in row['condition']: row['scores'] = [0]
        outcome = A.matrix(r)
        self.assertTrue(outcome['cells'][0]['consistent_representation_drop'])
        self.assertFalse(outcome['cells'][-1]['consistent_representation_drop'])
        self.assertEqual(outcome['cells'][-1]['representation_deltas'], [0]*4)

    def test_missing_duplicate_and_unknown_conditions(self):
        valid = rows()
        self.reject(lambda: A.matrix(valid[:-1]))
        self.reject(lambda: A.matrix(valid + [valid[0]]))
        for field, value in [('mask', 1), ('seed', 99), ('condition', 'other')]:
            bad = deepcopy(valid); bad[0][field] = value
            self.reject(lambda: A.matrix(bad))

    def test_strict_types_and_counts(self):
        for field, value in [('mask', 0.0), ('mask', False), ('seed', float(A.SEEDS[0])),
                ('condition', []), ('scores', [32.0, 0]), ('scores', [True, 0]),
                ('scores', [33, 0]), ('scores', [-1, 0]), ('scores', [32]),
                ('trials', 64.0), ('trials', 63), ('new_training_samples', 1),
                ('new_training_samples', False), ('new_training_samples', None)]:
            with self.subTest(field=field, value=value):
                bad = rows(); bad[0][field] = value
                self.reject(lambda: A.matrix(bad))

    def test_specification(self):
        self.assertEqual(A.specification(A.SEEDS[0], 'none').tasks, [10, 11])
        self.assertEqual(A.specification(A.SEEDS[1], A.CONDITIONS[-1]).tasks, [11])
        self.reject(lambda: A.specification(float(A.SEEDS[0]), 'none'))
        self.reject(lambda: A.specification(A.SEEDS[0], 'pickaxe-grid'))

    def test_bad_inputs_never_read_source(self):
        for changed in A.PINS:
            with patch.object(A.S, 'sha', side_effect=lambda p: 'changed' if p == changed else A.PINS[p]), \
                    patch.object(A, 'source') as source:
                self.reject(A.qualify); source.assert_not_called()

    def test_dirty_source(self):
        with patch.object(A.subprocess, 'check_output', return_value=b' M tracked-file'):
            self.reject(A.source)

    def test_missing_eula_never_constructs_study(self):
        with patch.dict(os.environ, {'EULA': 'false'}), patch.object(A, 'qualify') as qualify:
            self.reject(A.run); qualify.assert_not_called()

    def test_cli_is_inert(self):
        with tempfile.TemporaryDirectory() as directory:
            for arguments in ([], ['--help']):
                result = subprocess.run([sys.executable, str(Path(A.__file__)), *arguments],
                    cwd=directory, capture_output=True, text=True, timeout=20)
                self.assertEqual(result.returncode, 0)
                self.assertIn('--run', result.stdout)
                self.assertEqual(list(Path(directory).iterdir()), [])

    def test_verifier_metadata_binding(self):
        args = A.specification(A.SEEDS[0], 'none'); policy = Path('/policy'); root = Path('/result')
        good = dict(policy_sha256='p', runtime_jar_sha256=A.PINS[A.RUNTIME], exam_jar_sha256='e',
            tasks=[10, 11], seed=A.SEEDS[0], cases_per_task=32, port=25592, bind='127.0.0.1',
            input_kind='inference-policy', reset_intervention='none', diagnostic_only=False)
        def digest(p):
            return 'p' if p == policy else 'e' if p.name == 'exam.jar' else A.PINS[A.RUNTIME]
        changes = [(None, None), ('policy_sha256', 'changed'), ('runtime_jar_sha256', 'changed'),
            ('exam_jar_sha256', 'changed'), ('tasks', [11, 10]), ('tasks', [10.0, 11]),
            ('seed', float(A.SEEDS[0])), ('cases_per_task', 32.0), ('port', 25592.0),
            ('bind', '0.0.0.0'), ('input_kind', 'canonical-checkpoint'),
            ('reset_intervention', A.CONDITIONS[1]), ('diagnostic_only', 0)]
        for key, value in changes:
            metadata = deepcopy(good)
            if key is not None: metadata[key] = value
            with self.subTest(key=key), patch.object(A.S, 'load', return_value=metadata), \
                    patch.object(A.S.H, 'verify_report', return_value={'tasks':[{'passed':32}, {'passed':0}]}), \
                    patch.object(A.S, 'sha', side_effect=digest), patch.object(A.S, 'read', return_value=b'same'), \
                    patch.object(Path, 'exists', return_value=False), patch.object(Path, 'glob', return_value=iter([])):
                if key is None: self.assertEqual(A.verify(root, policy, 'p', args, 25592), [32, 0])
                else: self.reject(lambda: A.verify(root, policy, 'p', args, 25592))


if __name__ == '__main__': main()
