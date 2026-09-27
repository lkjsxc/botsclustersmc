import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile
import retention_ci as study

class RetentionInputTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.root=Path(self.tmp.name)
        self.model=b'synthetic-checkpoint-not-executable'
        self.manifest=copy.deepcopy(study.SPEC)
        self.manifest['arms']={arm:{'source':pin,'checkpoint_sha256':hashlib.sha256(self.model).hexdigest(),
            'samples':185000000,'updates':650000,'prefix_accepted_task_samples':[100]*19,
            'prefix_review_ticks':20,'prefix_frontier_ticks':80} for arm,pin in study.PINS.items()}
    def tearDown(self):self.tmp.cleanup()
    def archive(self,manifest=None,extra=None,missing=None):
        path=self.root/('input-'+str(len(list(self.root.iterdir())))+'.zip')
        with warnings.catch_warnings():
            warnings.simplefilter('ignore',UserWarning)
            with zipfile.ZipFile(path,'x') as z:
                entries={'manifest.json':json.dumps(manifest or self.manifest).encode(),
                    'control.bcmc':self.model,'candidate.bcmc':self.model}
                for name,data in entries.items():
                    if name!=missing:z.writestr(name,data)
                if extra:z.writestr(*extra)
        return path,hashlib.sha256(path.read_bytes()).hexdigest()
    def test_exact_input_only(self):
        path,digest=self.archive();manifest,files=study.payload(path,digest)
        self.assertEqual(manifest,self.manifest)
        self.assertEqual(set(files),{'manifest.json','control.bcmc','candidate.bcmc'})
        self.assertEqual(len(list(self.root.iterdir())),1)
    def test_hash_and_archive_shape(self):
        path,digest=self.archive()
        with self.assertRaises(ValueError):study.payload(path,'0'*64)
        for extra in [('control.bcmc',self.model),('../credentials',b'no'),('server.jar',b'no'),('world/',b'')]:
            with self.subTest(extra=extra):
                path,digest=self.archive(extra=extra)
                with self.assertRaises(ValueError):study.payload(path,digest)
        for missing in ['manifest.json','control.bcmc','candidate.bcmc']:
            path,digest=self.archive(missing=missing)
            with self.assertRaises(ValueError):study.payload(path,digest)
    def test_protocol_changes_rejected(self):
        for key in study.SPEC:
            manifest=copy.deepcopy(self.manifest);manifest[key]='altered'
            path,digest=self.archive(manifest)
            with self.subTest(key=key),self.assertRaises(ValueError):study.payload(path,digest)
        for field,value in [('source','0'*40),('checkpoint_sha256','0'*64),('samples',0),
            ('samples',185730639),('updates',True),('prefix_review_ticks',-1),('prefix_accepted_task_samples',[1])]:
            manifest=copy.deepcopy(self.manifest);manifest['arms']['candidate'][field]=value
            path,digest=self.archive(manifest)
            with self.subTest(field=field,value=value),self.assertRaises(ValueError):study.payload(path,digest)
    def test_decoded_bound(self):
        path,digest=self.archive(extra=('unrecognized',b'x'*1048576))
        with self.assertRaises(ValueError):study.payload(path,digest)
        manifest=copy.deepcopy(self.manifest);manifest['padding']='x'*65536
        path,digest=self.archive(manifest)
        with self.assertRaises(ValueError):study.payload(path,digest)
    def test_no_overwrite_and_symlink(self):
        path=self.root/'saved';study.create(path,b'original')
        with self.assertRaises(FileExistsError):study.create(path,b'replacement')
        self.assertEqual(path.read_bytes(),b'original')
        linked=self.root/'linked'
        try:linked.symlink_to(path)
        except OSError:self.skipTest('Symlink not permitted')
        with self.assertRaises(ValueError):study.bounded(linked)
    def test_actual_workload_conservation(self):
        prefix={'prefix_accepted_task_samples':[100]*19,'prefix_review_ticks':200,'prefix_frontier_ticks':800}
        last={'learned_task_samples_this_process':json.dumps([10]*19),
            'review_ticks_this_process':20,'frontier_ticks_this_process':80}
        result=study.workload(prefix,last)
        self.assertEqual(result['combined_final_phase']['accepted_task_samples'],[110]*19)
        self.assertEqual(result['combined_final_phase']['review_ticks'],220)
        self.assertEqual(result['combined_final_phase']['frontier_ticks'],880)
        self.assertEqual(result['ci_segment']['review_fraction'],.2)
        last['learned_task_samples_this_process']=json.dumps([-1]*19)
        with self.assertRaises(ValueError):study.workload(prefix,last)

if __name__=='__main__':unittest.main()
