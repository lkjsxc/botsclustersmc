"""Native shell/filesystem regression tests. Dummy binaries are NOT Minecraft tests."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import fcntl

ROOT = Path(__file__).resolve().parents[1]

class Packaging(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.p = Path(self.tmp.name) / 'source'
        shutil.copytree(ROOT, self.p, ignore=shutil.ignore_patterns('.git', '.build', 'academy-v2', 'runtime', 'server', 'state', 'logs', 'bin', 'experiments', 'diagnostics', '__pycache__'))
        self.env = {k: v for k, v in os.environ.items() if not k.startswith(('BCMC_', 'VK_')) and k not in ('BOTS','EULA','BIND_ADDRESS','OFFLINE_ACCESS_ACK','RUN_SECONDS','SERVER_PORT','BOT_PREFIX','JAVA_HEAP_GB')}
    def tearDown(self): self.tmp.cleanup()
    def runsh(self, text, **env):
        return subprocess.run(['bash','-c',text], cwd=self.p, env={**self.env,**env}, text=True, capture_output=True, timeout=15)
    def stub(self, path, text):
        f=self.p/path; f.parent.mkdir(parents=True,exist_ok=True); f.write_text('#!/usr/bin/env bash\nset -eu\n'+text+'\n'); f.chmod(0o755)
    def receipt(self):
        self.stub('bin/botsclustersmc-run','echo DUMMY_NATIVE_UTILITY')
        self.stub('scripts/supervise.sh','echo "DUMMY_EXECUTION_ONLY: $PWD $BCMC_CURRICULUM $BOTS $BIND_ADDRESS"')
        self.stub('bin/botsclustersmc-bots','echo DUMMY_NO_MINECRAFT')
        q=self.runsh('export BCMC_ROOT="$PWD"; source scripts/build-state.sh; bcmc_source_fingerprint > bin/source.sha256; printf "%s/%s\n" "$(uname -s)" "$(uname -m)" > bin/target.txt; sha256sum bin/botsclustersmc-run bin/botsclustersmc-bots > bin/artifacts.sha256; bcmc_build_is_current')
        self.assertEqual(q.returncode,0,q.stderr)
    def marker(self, bots=64):
        d=self.p/'academy-v2'; d.mkdir(exist_ok=True)
        (d/'.botsclustersmc-academy-v2').write_text(f'botsclustersmc-academy-v2\nbots={bots}\ncampus=8x16\n')
    def test_shell_syntax(self):
        for f in list(self.p.glob('*.sh'))+list((self.p/'scripts').glob('*.sh')):
            q=subprocess.run(['bash','-n',str(f)],capture_output=True,text=True)
            self.assertEqual(q.returncode,0,str(f)+q.stderr)
    def test_default_dispatch(self):
        self.stub('academy.sh','echo ACADEMY_ENTRY')
        q=self.runsh('./start.sh'); self.assertEqual(q.returncode,0,q.stderr);self.assertIn('ACADEMY_ENTRY',q.stdout)
    def test_no_wilderness_flag_fallback(self):
        self.stub('academy.sh','echo ACADEMY_ENTRY')
        q=self.runsh('./start.sh',BCMC_CURRICULUM='false');self.assertIn('ACADEMY_ENTRY',q.stdout)
    def test_default_64_and_public_bind(self):
        q=self.runsh('export BCMC_ROOT="$PWD";source scripts/env.sh;printf "%s %s %s" "$BOTS" "$BIND_ADDRESS" "$OFFLINE_ACCESS_ACK"')
        self.assertEqual(q.stdout,'64 0.0.0.0 true')
    def test_export_wins_over_operator_config(self):
        (self.p/'.env').write_text('BOTS=4\nBIND_ADDRESS=127.0.0.1\n')
        q=self.runsh('export BCMC_ROOT="$PWD";source scripts/env.sh;echo "$BOTS $BIND_ADDRESS"',BOTS='2')
        self.assertEqual(q.stdout.strip(),'2 127.0.0.1')
    def test_eula_not_implicitly_accepted(self):
        q=self.runsh('./start.sh');self.assertNotEqual(q.returncode,0);self.assertIn('EULA',q.stderr);self.assertFalse((self.p/'academy-v2').exists())
    def test_unowned_world_is_preserved(self):
        (self.p/'academy-v2').mkdir();f=self.p/'academy-v2/world.data';f.write_text('preserve')
        q=self.runsh('./start.sh',EULA='true');self.assertNotEqual(q.returncode,0);self.assertIn('not owned',q.stderr);self.assertEqual(f.read_text(),'preserve')
    def test_symlink_lab_is_refused(self):
        d=Path(self.tmp.name)/'elsewhere';d.mkdir();(self.p/'academy-v2').symlink_to(d,target_is_directory=True)
        q=self.runsh('./start.sh',EULA='true');self.assertNotEqual(q.returncode,0);self.assertEqual(list(d.iterdir()),[])
    def test_changed_population_is_refused(self):
        self.marker(8);q=self.runsh('./start.sh',EULA='true');self.assertNotEqual(q.returncode,0);self.assertIn('population/schema',q.stderr)
    def test_failed_initial_build_is_restartable(self):
        self.stub('scripts/build.sh','echo EXPECTED_BUILD_FAILURE >&2;exit 7')
        for _ in range(2):
            q=self.runsh('./start.sh',EULA='true');self.assertEqual(q.returncode,7,q.stderr);self.assertIn('EXPECTED_BUILD_FAILURE',q.stderr)
    def test_no_world_or_weights_are_imported(self):
        for name in ['server/important-world','state/policy.bcmc']:
            f=self.p/name;f.parent.mkdir(exist_ok=True);f.write_text('preserve')
        self.receipt();q=self.runsh('./start.sh',EULA='true')
        self.assertEqual(q.returncode,0,q.stderr);self.assertIn('DUMMY_EXECUTION_ONLY:',q.stdout)
        self.assertIn('/academy-v2 true 64 0.0.0.0',q.stdout)
        self.assertFalse((self.p/'academy-v2/server/important-world').exists());self.assertFalse((self.p/'academy-v2/state/policy.bcmc').exists())
        self.assertEqual((self.p/'state/policy.bcmc').read_text(),'preserve')
    def test_runtime_helper_upgrade_preserves_existing_state(self):
        self.receipt();self.marker()
        for name,text in [('runtime/host-lib/gson.jar','new-helper'),('runtime/host-tools.jar','new-host'),
                          ('academy-v2/runtime/folia.jar','keep-server'),('academy-v2/state/policy.bcmc','keep-policy'),
                          ('academy-v2/server/important-world','keep-world')]:
            f=self.p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
        q=self.runsh('./start.sh',EULA='true');self.assertEqual(q.returncode,0,q.stderr)
        self.assertEqual((self.p/'academy-v2/runtime/host-lib/gson.jar').read_text(),'new-helper')
        self.assertEqual((self.p/'academy-v2/runtime/host-tools.jar').read_text(),'new-host')
        for name,text in [('runtime/folia.jar','keep-server'),('state/policy.bcmc','keep-policy'),('server/important-world','keep-world')]:
            self.assertEqual((self.p/'academy-v2'/name).read_text(),text)
    def test_stale_binary_receipt_is_rejected(self):
        self.receipt();(self.p/'bin/botsclustersmc-bots').write_text('modified')
        q=self.runsh('export BCMC_ROOT="$PWD";source scripts/build-state.sh;bcmc_build_is_current');self.assertNotEqual(q.returncode,0)
    def test_code_change_requires_rebuild(self):
        self.receipt();self.stub('scripts/build.sh','echo REBUILD_REQUIRED >&2;exit 7')
        with (self.p/'app/main.rs').open('a') as f: f.write('\n// native change\n')
        q=self.runsh('./start.sh',EULA='true');self.assertEqual(q.returncode,7);self.assertIn('REBUILD_REQUIRED',q.stderr)
    def test_live_lab_lock_blocks_copy(self):
        self.marker();d=self.p/'academy-v2/.runtime';d.mkdir()
        with (d/'run.lock').open('w') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
            q=self.runsh('./start.sh',EULA='true');self.assertNotEqual(q.returncode,0);self.assertIn('already running',q.stderr)
    def test_stop_routes_to_owned_lab(self):
        self.marker();(self.p/'academy-v2/.runtime').mkdir();q=self.runsh('./stop.sh')
        self.assertEqual(q.returncode,0,q.stderr);self.assertTrue((self.p/'academy-v2/.runtime/stop').exists());self.assertFalse((self.p/'.runtime/stop').exists())
    def test_status_routes_to_owned_lab(self):
        self.marker();self.stub('academy-v2/bin/botsclustersmc-run','echo "STATUS_ROOT=$BCMC_ROOT"')
        (self.p/'academy-v2/state').mkdir(); (self.p/'academy-v2/state/status.json').write_text('{}')
        q=self.runsh('./status.sh');self.assertEqual(q.returncode,0,q.stderr);self.assertIn('/academy-v2',q.stdout)
    def test_direct_supervisor_script_requires_ownership(self):
        q=self.runsh('./scripts/run.sh',EULA='true');self.assertNotEqual(q.returncode,0);self.assertIn('ownership marker',q.stderr)
    def test_explicit_network_ack_is_documented(self):
        s=(self.p/'.env.example').read_text();self.assertIn('NO account authentication',s);self.assertNotIn('EULA=true\n',s)

if __name__=='__main__':
    print('Shell/filesystem fixtures use explicit dummy executables, never real Minecraft or learning.',flush=True)
    unittest.main(verbosity=2)
