#!/usr/bin/env python3
"""Opt-in real observer acceptance; isolated loopback server, never a gameplay actor."""
from __future__ import annotations
import argparse, json, os, subprocess, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = os.environ.get('JAVA_BIN', 'java')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get('EULA') != 'true':
        raise SystemExit('Explicit EULA=true is required for this disposable observer test.')
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    env = os.environ | {'EULA': 'true', 'ACADEMY': str(output/'academy'), 'BOTS': '128',
        'HEAP_GB': '2', 'REGION_THREADS': '2', 'INFERENCE_THREADS': '1', 'LEARNER_THREADS': '1',
        'PORT': '25581', 'BIND_ADDRESS': '127.0.0.1', 'ONLINE_MODE': 'false', 'OFFLINE_ACCESS_ACK': 'true',
        'OBSERVER_TEST_PORT': '25581', 'MONITOR_URL': 'http://127.0.0.1:8766/'}
    data = output/'academy/server/plugins/BotsClustersMC'
    monitor = None
    with (output/'server.log').open('w') as log, (output/'monitor.log').open('w') as web_log:
        server = subprocess.Popen([JAVA, 'host/Host.java', 'start'], cwd=ROOT, env=env,
            stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)
        try:
            deadline = time.monotonic() + 180
            while time.monotonic() < deadline:
                if server.poll() is not None:
                    raise RuntimeError('Observer server exited; inspect server.log')
                try:
                    state = json.loads((data/'status.json').read_text())
                except (FileNotFoundError, json.JSONDecodeError):
                    state = {}
                if state.get('state') == 'failed':
                    raise RuntimeError(f'Observer server failed: {state}')
                if state.get('active_agents') == 128 and state.get('progressed_agents_since_status') == 128:
                    break
                time.sleep(.5)
            else:
                raise TimeoutError('Observer server readiness')
            monitor = subprocess.Popen([JAVA, '-Xmx128m', 'host/Monitor.java', str(data), '127.0.0.1', '8766'],
                cwd=ROOT, env=env, stdin=subprocess.DEVNULL, stdout=web_log, stderr=subprocess.STDOUT)
            time.sleep(3)
            if monitor.poll() is not None:
                raise RuntimeError('Monitor exited; inspect monitor.log')
            for test in ('observer.client.mjs', 'monitor.browser.mjs'):
                with (output/(test+'.log')).open('w') as test_log:
                    subprocess.run(['node', str(ROOT/'tests'/test)], cwd=ROOT, env=env,
                        stdout=test_log, stderr=subprocess.STDOUT, check=True, timeout=120)
            final = json.loads((data/'status.json').read_text())
            assert final['state'] == 'running' and final['active_agents'] == 128
            assert final['burning_agents'] == 0 and final['inference_failed'] == 0
            (output/'observed-status.json').write_text(json.dumps(final, indent=2))
        finally:
            if monitor is not None and monitor.poll() is None:
                monitor.terminate()
                monitor.wait(timeout=15)
            if server.poll() is None:
                subprocess.run([JAVA, 'host/Host.java', 'stop'], cwd=ROOT, env=env,
                    stdout=subprocess.DEVNULL, check=True, timeout=15)
                server.wait(timeout=75)
    print('PASS real non-operator observer, guarded training, private monitor and browser acceptance')


if __name__ == '__main__':
    main()
