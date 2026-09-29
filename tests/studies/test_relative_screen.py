#!/usr/bin/env python3
"""Boundary checks for the new relative question, independent of Minecraft scores."""
from pathlib import Path
import importlib.util
import copy
import json
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "study", Path(__file__).with_name("protected_continuation.py"))
study = importlib.util.module_from_spec(spec)
spec.loader.exec_module(study)

class RelativeScreenTest(unittest.TestCase):
    def rows(self, arms):
        return [dict(arm=arm, seed=seed, scores=[120]*11+[0],
                     trials=1536, new_training_samples=0)
                for arm in arms for seed in study.SEEDS]

    def test_imperfect_input_is_not_forgetting(self):
        self.assertTrue(study.relative_retention([0]*12, [0]*12))
        self.assertTrue(study.relative_retention([29]*12, [29]*12))

    def test_each_task_boundary(self):
        for task in range(12):
            with self.subTest(task=task):
                before = [120]*12
                after = before.copy()
                after[task] -= 8
                self.assertTrue(study.relative_retention(before, after))
                after[task] -= 1
                self.assertFalse(study.relative_retention(before, after))

    def test_bad_counts(self):
        for value in (-1, 129, 1.0, True, None):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    study.relative_retention([120]*12, [value]+[120]*11)
        with self.assertRaises(ValueError):
            study.relative_retention([120]*11, [120]*12)

    def test_matrix_requires_every_model_seed_once(self):
        rows = self.rows(study.ARMS)
        self.assertEqual(len(study.vectors(rows, study.ARMS)), 4)
        for bad in (rows[:-1], rows+[rows[0]], rows[:3]+[dict(rows[3], seed=1)],
                    rows[:3]+[dict(rows[3], trials=1535)],
                    rows[:3]+[dict(rows[3], new_training_samples=1)]):
            with self.assertRaises(ValueError):
                study.vectors(bad, study.ARMS)

    def gate(self, deteriorate_control=False, deteriorate_protected=False):
        original = study.OUT
        with tempfile.TemporaryDirectory() as temp:
            study.OUT = Path(temp)
            try:
                for phase, rows in (("baseline", self.rows(["base", "protected"])),
                                    ("early", self.rows(study.ARMS))):
                    directory = study.OUT/("evaluation-"+phase)
                    directory.mkdir()
                    if phase == "early":
                        if deteriorate_control:
                            rows[0]["scores"] = [0]*12
                        if deteriorate_protected:
                            rows[3]["scores"][10] -= 9
                    (directory/"completed.json").write_text(json.dumps(rows))
                return study.gate("early")
            finally:
                study.OUT = original

    def test_control_loss_does_not_stop_protected_arm(self):
        self.assertTrue(self.gate(deteriorate_control=True))

    def test_one_seed_failure_cannot_be_averaged_away(self):
        self.assertFalse(self.gate(deteriorate_protected=True))

if __name__ == "__main__":
    unittest.main()
