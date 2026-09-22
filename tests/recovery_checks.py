"""Regression fixtures for the repaired host layer. Not Minecraft/skill evidence."""
import copy
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ['java', '-Xmx64m', '-XX:ActiveProcessorCount=2', '-cp',
        str(ROOT/'runtime/host-tools.jar')+':'+str(ROOT/'runtime/host-lib/gson.jar'), 'HostTools']

class Recovery(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root/'.runtime/lab').mkdir(parents=True)
        (self.root/'state').mkdir()
    def tearDown(self): self.tmp.cleanup()
    def host(self, *args, data=None):
        return subprocess.run(JAVA+list(map(str,args)), input=data, capture_output=True, timeout=20)
    def config(self, **kwargs):
        env = {k:v for k,v in os.environ.items() if k not in ('BOT_PREFIX','BOTS','SERVER_PORT','EULA','JAVA_HEAP_GB','BIND_ADDRESS','OFFLINE_ACCESS_ACK','RUN_SECONDS','BCMC_ROOT')}
        env.update(kwargs)
        return subprocess.run(['bash','-c','set -e;export BCMC_ROOT="$PWD";source scripts/env.sh;source scripts/runtime-config.sh;bcmc_validate_runtime;echo "$BOT_PREFIX $BOTS $SERVER_PORT $BIND_ADDRESS"'],cwd=ROOT,env=env,capture_output=True,text=True,timeout=10)
    def status(self):
        return {'schema':2,'run_id':'run','updated':int(time.time()),'error':'','version':1,'trained_samples':128,
                'agents':[{'id':i,'name':f'bcmc{i:02}','online':True,'spawns':1,'steps':80,'last_seen':int(time.time())} for i in range(2)]}
    def check(self, state, enforce='true'):
        (self.root/'state/status.json').write_text(json.dumps(state))
        return self.host('status',self.root,'run',2,'bcmc',enforce)
    def campus(self):
        lab=self.root/'.runtime/lab'
        (lab/'bridge.ready').write_text('BCMCLAB2 run ready\n')
        (lab/'campus.ready').write_text('BCMCCAMPUS1 run 32 8 16 96\n'+''.join(f'{i} {i%8*16} {i//8*16}\n' for i in range(32)))
        return lab
    def test_requested_defaults(self):
        q=self.config();self.assertEqual(q.returncode,0,q.stderr);self.assertEqual(q.stdout.strip(),'bcmc 32 25565 0.0.0.0')
    def test_old_invalid_prefix_is_rejected_explicitly(self):
        q=self.config(BOT_PREFIX='botsclustersmc');self.assertNotEqual(q.returncode,0);self.assertIn('BOT_PREFIX',q.stderr)
    def test_prefix_boundary(self):
        self.assertEqual(self.config(BOT_PREFIX='abcdefghijklm').returncode,0)
        self.assertNotEqual(self.config(BOT_PREFIX='abcdefghijklmn').returncode,0)
    def test_prefix_injection_rejected(self):
        self.assertNotEqual(self.config(BOT_PREFIX='x;echo wrong').returncode,0)
    def test_port_range(self):
        for value in ['0','65536','-1','abc']:
            self.assertNotEqual(self.config(SERVER_PORT=value).returncode,0,value)
        self.assertIn('25566',self.config(SERVER_PORT='25566').stdout)
    def test_ipv4_and_offline_ack(self):
        self.assertNotEqual(self.config(BIND_ADDRESS='256.0.0.1').returncode,0)
        self.assertNotEqual(self.config(OFFLINE_ACCESS_ACK='false').returncode,0)
        self.assertEqual(self.config(BIND_ADDRESS='127.0.0.1',OFFLINE_ACCESS_ACK='false').returncode,0)
    def test_available_port_check_does_not_kill_owner(self):
        with socket.socket() as s:
            s.bind(('127.0.0.1',0));s.listen();port=s.getsockname()[1]
            self.assertEqual(self.host('available','127.0.0.1',port).returncode,1)
            self.assertEqual(s.getsockname()[1],port)
        self.assertEqual(self.host('available','127.0.0.1',port).returncode,0)
    def test_complete_campus(self):
        self.campus();self.assertEqual(self.host('campus',self.root,'run',32).returncode,0)
    def test_missing_campus_is_waiting_not_success(self):
        self.assertEqual(self.host('campus',self.root,'run',32).returncode,2)
    def test_partial_campus_rejected(self):
        lab=self.campus();p=lab/'campus.ready';p.write_text(p.read_text().replace('31 112 48\n',''))
        self.assertEqual(self.host('campus',self.root,'run',32).returncode,1)
    def test_stale_campus_rejected(self):
        self.campus();self.assertEqual(self.host('campus',self.root,'new-run',32).returncode,1)
    def test_fatal_receipt_without_io_readiness_is_immediate(self):
        (self.root/'.runtime/lab/fatal.txt').write_text('run agent=-1 BOT_PREFIX invalid\n')
        q=self.host('campus',self.root,'run',32);self.assertEqual(q.returncode,1);self.assertIn(b'BOT_PREFIX',q.stderr)
    def test_all_bot_status(self):
        q=self.check(self.status());self.assertEqual(q.returncode,0,q.stderr);self.assertEqual(json.loads(q.stdout)['acted'],2)
    def test_missing_status_is_waiting(self):
        self.assertEqual(self.host('status',self.root,'run',2,'bcmc','false').returncode,2)
    def test_one_agent_not_acting_is_not_ready(self):
        s=self.status();s['agents'][1]['steps']=0
        self.assertEqual(self.check(s).returncode,2)
    def test_wrong_population(self):
        s=self.status();s['agents'].pop();self.assertEqual(self.check(s).returncode,1)
    def test_wrong_identity(self):
        s=self.status();s['agents'][1]['id']=0;self.assertEqual(self.check(s).returncode,1)
    def test_wrong_name(self):
        s=self.status();s['agents'][1]['name']='other';self.assertEqual(self.check(s).returncode,1)
    def test_old_run(self):
        s=self.status();s['run_id']='old';self.assertEqual(self.check(s).returncode,1)
    def test_old_global_heartbeat(self):
        s=self.status();s['updated']-=121;self.assertEqual(self.check(s).returncode,1)
    def test_old_agent_heartbeat(self):
        s=self.status();s['agents'][1]['last_seen']-=241;self.assertEqual(self.check(s).returncode,1)
    def test_future_heartbeat(self):
        s=self.status();s['updated']+=120;self.assertEqual(self.check(s).returncode,1)
    def test_reported_learner_error(self):
        s=self.status();s['error']='test failure';self.assertEqual(self.check(s).returncode,1)
    def test_bounded_logs_keep_order(self):
        data=b'a'*(8*1024*1024)+b'last-line\n'
        p=self.root/'log.txt';q=self.host('log',p,data=data);self.assertEqual(q.returncode,0,q.stderr)
        self.assertEqual(Path(str(p)+'.1').read_bytes()+p.read_bytes(),data)
        self.assertLessEqual(p.stat().st_size,8*1024*1024)

if __name__=='__main__':
    print('Synthetic host-layer regression fixtures; these do not substitute for live Folia runs.',flush=True)
    unittest.main(verbosity=2)
