#!/usr/bin/env python3
"""Decision-rule tests for the prospectively bounded protected-placement study."""
import ast, copy, json, os, subprocess, sys, tempfile, unittest
from pathlib import Path
from unittest.mock import patch
import protected_placement as p

def ordinary(arms):
    return [dict(arm=arm,seed=seed,scores=[64]*12,trials=768,
                 new_training_samples=0,intervention='none',tasks=list(range(12)),cases=64)
            for arm in arms for seed in p.SEEDS]

def diagnostics():
    return [dict(arm=arm,seed=seed,intervention=cell,tasks=[11],cases=32,trials=32,
                 new_training_samples=0,scores=[9 if arm=='candidate' else 2])
            for arm in p.ARMS for seed in p.SEEDS for cell in p.CELLS]

class ProtectedPlacementTest(unittest.TestCase):
    def test_relative_retention_all_tasks_and_boundaries(self):
        parent=[64]*12
        for task in range(12):
            at=parent.copy();at[task]=60
            self.assertTrue(p.relative_retention(parent,at))
            at[task]=59
            self.assertFalse(p.relative_retention(parent,at))
        for bad in ([64]*11,[True]*12,[65]*12,[-1]*12):
            with self.assertRaises(ValueError):p.relative_retention(parent,bad)

    def test_ordinary_growth_is_not_retention_or_assisted_gain(self):
        self.assertFalse(p.growth_pass(0,0))
        self.assertFalse(p.growth_pass(7,0))
        self.assertTrue(p.growth_pass(8,4))
        self.assertFalse(p.growth_pass(8,5))
        self.assertTrue(p.growth_pass(64,60))
        for a,b in ((True,0),(8,False),(65,0),(-1,0)):
            with self.assertRaises(AssertionError):p.growth_pass(a,b)

    def test_complete_ordinary_vectors(self):
        rows=ordinary(p.ARMS)
        self.assertEqual(len(p.vectors(rows,p.ARMS)),4)
        for altered in (rows[:-1],rows+[rows[0]]):
            with self.assertRaises(ValueError):p.vectors(altered,p.ARMS)
        for key,value in (('intervention',p.CELLS[0]),('new_training_samples',1),
                          ('new_training_samples',False),('cases',32),('tasks',[11])):
            altered=copy.deepcopy(rows);altered[0][key]=value
            with self.assertRaises(ValueError):p.vectors(altered,p.ARMS)

    def test_either_arm_can_fail_the_early_gate(self):
        baseline=ordinary(['base','protected'])
        for failed_arm in p.ARMS:
            rows=ordinary(p.ARMS)
            next(r for r in rows if r['arm']==failed_arm)['scores'][10]=59
            with tempfile.TemporaryDirectory() as tmp,patch.object(p,'OUT',Path(tmp)):
                (p.OUT/'evaluation-baseline').mkdir();(p.OUT/'evaluation-early').mkdir()
                p.save(p.OUT/'evaluation-baseline/completed.json',baseline)
                p.save(p.OUT/'evaluation-early/completed.json',rows)
                self.assertFalse(p.gate('early'))
                rejected=p.load(p.OUT/'rejected.json')
                failures=[r for r in rejected['results'] if not r['retained']]
                self.assertEqual([r['arm'] for r in failures],[failed_arm])

    def test_all_five_cells_and_both_seeds_are_required(self):
        rows=diagnostics()
        self.assertTrue(all(r['placement_gain'] for r in p.diagnostic_screen(rows)))
        for altered in (rows[:-1],rows+[rows[0]]):
            with self.assertRaises(AssertionError):p.diagnostic_screen(altered)
        for cell in p.CELLS:
            altered=copy.deepcopy(rows)
            next(r for r in altered if r['arm']=='candidate' and r['intervention']==cell)['scores']=[0]
            self.assertFalse(p.diagnostic_screen(altered)[0]['placement_gain'])
        altered=copy.deepcopy(rows);altered[0]['intervention']='none'
        with self.assertRaises(AssertionError):p.diagnostic_screen(altered)

    def test_native_task_balance_includes_unlabelled_bucket(self):
        valid=[0]*19;valid[11]=4
        self.assertEqual(p.task_counts(json.dumps(valid)),valid)
        self.assertEqual(p.task_counts(json.dumps([0]*19)),[0]*19)
        for size in (0,18,20):
            with self.assertRaises(AssertionError):p.task_counts(json.dumps([0]*size))
        for index in range(19):
            if index==11:continue
            invalid=valid.copy();invalid[index]=1
            with self.assertRaises(AssertionError):p.task_counts(json.dumps(invalid))

    def test_task_counts_reject_malformed_and_noninteger_counters(self):
        for value in (None,[],{},False):
            with self.assertRaises(AssertionError):p.task_counts(value)
        for value in ('null','{}','false','[0,'):
            with self.assertRaises((AssertionError,ValueError)):p.task_counts(value)
        for value in (True,False,-1,1.0,float('nan'),float('inf'),2**63):
            invalid=[0]*19;invalid[11]=value
            with self.assertRaises(AssertionError):p.task_counts(json.dumps(invalid))
        valid=[0]*19;valid[11]=2**63-1
        self.assertEqual(p.task_counts(json.dumps(valid)),valid)

    def test_optimized_python_keeps_rejection_active(self):
        tree=ast.parse(Path(p.__file__).read_text())
        self.assertFalse(any(isinstance(node,ast.Assert) for node in ast.walk(tree)))
        for flag in ('-O','-OO'):
            result=subprocess.run([sys.executable,flag,'-c',
                'import protected_placement as p; p.growth_pass(True,0)'],
                cwd=Path(p.__file__).parent,env=dict(os.environ,PYTHONDONTWRITEBYTECODE='1'),
                capture_output=True,text=True,timeout=15)
            self.assertNotEqual(result.returncode,0)
            self.assertIn('invalid growth count',result.stderr)

if __name__=='__main__':unittest.main(verbosity=2)
