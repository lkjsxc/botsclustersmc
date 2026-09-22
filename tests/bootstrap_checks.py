"""Source-only bootstrap failures. These are not network or Minecraft tests."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class Bootstrap(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / 'checkout'
        (self.root / 'scripts').mkdir(parents=True)
        shutil.copy2(ROOT/'scripts/fetch-host-lib.sh', self.root/'scripts/fetch-host-lib.sh')
        self.shims = Path(self.temp.name)/'shims'
        self.shims.mkdir()
        self.env = {**os.environ, 'PATH': str(self.shims)+os.pathsep+os.environ['PATH']}
    def tearDown(self):
        self.temp.cleanup()
    def run_fetch(self):
        return subprocess.run(['bash','scripts/fetch-host-lib.sh'],cwd=self.root,
                              env=self.env,text=True,capture_output=True,timeout=10)
    def curl(self, body):
        p=self.shims/'curl';p.write_text('#!/usr/bin/env bash\nset -eu\n'+body+'\n');p.chmod(0o755)
    def test_network_failure_does_not_install_a_partial_jar(self):
        self.curl('exit 28')
        p=self.run_fetch();self.assertEqual(p.returncode,28,p.stderr)
        self.assertFalse((self.root/'runtime/host-lib/gson.jar').exists())
        self.assertEqual(list((self.root/'runtime/host-lib').glob('gson.download.*')),[])
    def test_corrupt_download_is_rejected_and_removed(self):
        self.curl('printf invalid > "${@: -1}"')
        p=self.run_fetch();self.assertNotEqual(p.returncode,0)
        self.assertIn('pinned checksum',p.stderr)
        self.assertFalse((self.root/'runtime/host-lib/gson.jar').exists())
        self.assertEqual(list((self.root/'runtime/host-lib').glob('gson.download.*')),[])
    def test_existing_bad_jar_is_preserved_and_never_redownloaded(self):
        file=self.root/'runtime/host-lib/gson.jar';file.parent.mkdir(parents=True);file.write_bytes(b'preserve')
        self.curl('echo unexpected_download >&2;exit 99')
        p=self.run_fetch();self.assertNotEqual(p.returncode,0)
        self.assertIn('integrity mismatch',p.stderr);self.assertNotIn('unexpected_download',p.stderr)
        self.assertEqual(file.read_bytes(),b'preserve')
    def test_linked_runtime_does_not_create_directories_outside_checkout(self):
        outside=Path(self.temp.name)/'outside';outside.mkdir()
        (self.root/'runtime').symlink_to(outside,target_is_directory=True)
        p=self.run_fetch();self.assertNotEqual(p.returncode,0)
        self.assertEqual(list(outside.iterdir()),[])
    def test_linked_library_is_rejected(self):
        file=self.root/'runtime/host-lib/gson.jar';file.parent.mkdir(parents=True)
        outside=Path(self.temp.name)/'outside.jar';outside.write_bytes(b'preserve');file.symlink_to(outside)
        p=self.run_fetch();self.assertNotEqual(p.returncode,0)
        self.assertEqual(outside.read_bytes(),b'preserve')
    def test_first_clone_pin_matches_supported_protocol(self):
        lines=(ROOT/'pins/folia.lock').read_text().splitlines()
        self.assertEqual(len(lines),5);self.assertEqual(lines[0],'1.21.11');self.assertEqual(lines[1],'14')
        self.assertEqual(lines[4],'STABLE')
        self.assertRegex(lines[3],r'^[a-f0-9]{64}$')
        self.assertEqual(lines[2],f'https://fill-data.papermc.io/v1/objects/{lines[3]}/folia-1.21.11-14.jar')
    def test_documented_start_and_pin_are_copied_into_academy_and_smoke(self):
        for name in ['academy.sh','smoke.sh']:
            self.assertIn('tests pins', (ROOT/name).read_text())
        self.assertIn('git clone https://github.com/lkjsxc/botsclustersmc.git',(ROOT/'README.md').read_text())
        self.assertIn('git clone https://github.com/lkjsxc/botsclustersmc.git',(ROOT/'README.ja.md').read_text())
    def test_rust_defaults_no_longer_use_old_port_or_invalid_prefix(self):
        s=(ROOT/'app/settings.rs').read_text()
        self.assertIn('127.0.0.1:25565',s);self.assertNotIn('25615',s)
        self.assertIn('unwrap_or("bcmc".into())',s)
        c=(ROOT/'launcher/config.rs').read_text()
        self.assertIn('number("SERVER_PORT",25565,1,65535)',c);self.assertNotIn('25615',c)

if __name__=='__main__':
    unittest.main(verbosity=2)
