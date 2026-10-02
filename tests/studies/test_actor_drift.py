"""No server, learning or source-model access in these controller regressions."""
import copy, os, subprocess, sys, tempfile, unittest
from pathlib import Path
import actor_drift as A


def rows(arms=A.ARMS, values=None):
    values = values or {'parent': 30, 'candidate': 12, 'representation': 10, 'output': 30, 'actor': 12}
    return [dict(arm=arm, seed=seed, condition='none', scores=[32] * 11 + [values[arm], 0],
        trials=416, new_training_samples=0) for arm in arms for seed in A.SEEDS]


class ActorDriftTest(unittest.TestCase):
    def test_complete_matrix(self):
        self.assertEqual(len(A.vectors(rows(), A.ARMS)), 10)
        self.assertTrue(A.qualify(rows(('parent', 'candidate'))))

    def test_representation_screen(self):
        result = A.interpret(rows())
        self.assertTrue(result['representation_consistent_loss'])
        self.assertTrue(result['output_only_pickaxe_retained'])
        self.assertFalse(result['output_consistent_loss'])
        self.assertFalse(result['deployment'])
        self.assertEqual(result['new_training_samples'], 0)

    def test_output_screen(self):
        result = A.interpret(rows(values=dict(parent=30, candidate=12, representation=30, output=10, actor=12)))
        self.assertFalse(result['representation_consistent_loss'])
        self.assertTrue(result['output_consistent_loss'])
        self.assertTrue(result['representation_only_pickaxe_retained'])

    def test_both_backgrounds_required(self):
        result = A.interpret(rows(values=dict(parent=30, candidate=12, representation=10, output=15, actor=12)))
        self.assertFalse(result['representation_consistent_loss'])
        self.assertFalse(result['output_consistent_loss'])

    def test_both_seeds_required(self):
        data = rows()
        next(row for row in data if row['arm'] == 'representation' and row['seed'] == A.SEEDS[1])['scores'][11] = 29
        self.assertFalse(A.interpret(data)['representation_consistent_loss'])

    def test_missing_duplicate_extra(self):
        data = rows()
        for invalid in (data[:-1], data + data[:1], data + [dict(data[0], arm='other')]):
            with self.assertRaises(AssertionError):
                A.vectors(invalid, A.ARMS)

    def test_exact_identity(self):
        for change in ({'seed': True}, {'seed': A.SEEDS[0] + 50}, {'arm': 0}, {'condition': 'workbench-open'},
                       {'trials': 415}, {'trials': True}, {'new_training_samples': 1}, {'new_training_samples': False}):
            data = rows()
            data[0].update(change)
            with self.subTest(change=change), self.assertRaises(AssertionError):
                A.vectors(data, A.ARMS)

    def test_score_types_and_ranges(self):
        for bad in (-1, 33, True, 1.0, float('nan'), '32', None):
            data = rows()
            data[0]['scores'][0] = bad
            with self.subTest(bad=bad), self.assertRaises(AssertionError):
                A.vectors(data, A.ARMS)
        for bad in ([32] * 12, [32] * 14, tuple([32] * 13)):
            data = rows()
            data[0]['scores'] = bad
            with self.assertRaises(AssertionError):
                A.vectors(data, A.ARMS)

    def test_source_rejected_without_relabeling(self):
        for arm, task, score in (('parent', 11, 25), ('parent', 2, 27), ('candidate', 2, 27), ('candidate', 11, 23)):
            data = rows(('parent', 'candidate'))
            next(row for row in data if row['arm'] == arm)['scores'][task] = score
            self.assertFalse(A.qualify(data))

    def test_other_retention_not_pooled(self):
        data = rows()
        next(row for row in data if row['arm'] == 'output')['scores'][2] = 20
        result = A.interpret(data)
        self.assertTrue(result['output_only_pickaxe_retained'])
        self.assertFalse(result['details'][0]['retained']['output'])

    def test_inputs_not_mutated(self):
        data = rows()
        original = copy.deepcopy(data)
        A.interpret(data)
        self.assertEqual(data, original)

    def test_create_only_and_symlink_rejection(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / 'result.json'
            A.save(path, {'first': True})
            with self.assertRaises(FileExistsError):
                A.save(path, {'first': False})
            self.assertEqual(A.H.read_report(path), {'first': True})
            linked = Path(temporary) / 'alias'
            linked.symlink_to(path)
            with self.assertRaises(AssertionError):
                A.read(linked)

    def test_cli_has_no_implicit_run(self):
        result = subprocess.run([sys.executable, str(Path(A.__file__)), '--help'], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0)
        self.assertIn('--run', result.stdout)
        environment = dict(os.environ, EULA='false')
        result = subprocess.run([sys.executable, str(Path(A.__file__)), '--run'], env=environment,
            capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Explicit EULA=true', result.stderr)


if __name__ == '__main__':
    unittest.main()
