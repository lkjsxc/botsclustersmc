#!/usr/bin/env python3
"""Synthetic read-only dashboard checks. Does not launch or contact Minecraft.

Requires Python Playwright and Chromium/Chrome (or Playwright's installed browser).
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import time
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / '.build/evidence/crafting-monitor')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    attempts, wins = [0] * 108, [0] * 108
    attempts[66:72] = [40, 30, 20, 10, 5, 2]
    wins[66:72] = [38, 20, 10, 4, 1, 0]
    status = dict(epoch_millis=int(time.time()*1000), state='running',
                  schema='bcmc-citizen-egocentric-context', active_agents=64,
                  learner_samples_per_second=100, trained_samples=12000,
                  course_task_population=json.dumps([0]*11+[64]+[0]*6),
                  practice_success_ema=.7, probe_success_ema=0,
                  station_acquisition_trials=100, station_acquisition_successes=90,
                  station_acquisition_updates_completion_ema=False,
                  crafting_practice_bucket_width=6,
                  crafting_practice_trials_by_task_and_missing=json.dumps(attempts),
                  crafting_practice_successes_by_task_and_missing=json.dumps(wins))
    errors = []
    with sync_playwright() as p:
        executable = os.environ.get('CHROME_BIN')
        if not executable and not Path(p.chromium.executable_path).is_file():
            executable = shutil.which('chromium') or shutil.which('google-chrome')
        browser = p.chromium.launch(headless=True, executable_path=executable)
        page = browser.new_page(viewport={'width': 1280, 'height': 1000})
        page.on('pageerror', lambda error: errors.append(str(error)))
        # Inject read-only synthetic responses; no URL, external host or server is contacted.
        page.context.set_offline(True)
        page.evaluate("""status => {
            window.fetch = async path => ({ok: true, json: async () =>
                path === '/api/status' ? structuredClone(status) :
                path === '/api/history' ? [structuredClone(status)] :
                {result: null, monitor: null}});
        }""", status)
        page.set_content((ROOT/'host/monitor.html').read_text(), wait_until='load')
        page.wait_for_function("document.getElementById('state').textContent === 'RUNNING'")
        assert page.locator('#state').inner_text() == 'RUNNING'
        assert page.locator('#population .row').count() == 18
        assert page.locator('#crafting-practice tbody tr').count() == 5
        assert page.locator('[data-task="11"][data-missing="0"]').inner_text() == '38 / 40'
        assert page.locator('[data-task="11"][data-missing="5"]').inner_text() == '0 / 2'
        assert page.locator('[data-task="8"][data-missing="5"]').inner_text() == '—'
        assert '90 / 100' in page.locator('#station-opening').inner_text()
        assert 'Completion EMA' in page.locator('#practice').inner_text()
        assert 'policy must collect' in page.locator('#crafting-practice').locator('..').inner_text()
        assert 'process only' in page.locator('#crafting-practice').locator('..').inner_text()
        page.screenshot(path=str(args.output/'desktop.png'), full_page=True)
        page.set_viewport_size({'width': 390, 'height': 844})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        page.screenshot(path=str(args.output/'mobile.png'), full_page=True)
        # Both native JSON arrays and the runtime's serialized-array representation work.
        native = status | dict(crafting_practice_trials_by_task_and_missing=attempts,
                               crafting_practice_successes_by_task_and_missing=wins)
        page.evaluate('s => craftingPractice(s)', native)
        assert page.locator('#crafting-practice tbody tr').count() == 5
        invalid = [None, [], [-1]*108, [0.5]*108, [2**54]*108, ['<img src=x onerror=alert(1)>']*108]
        for data in invalid:
            page.evaluate('s => craftingPractice(s)', native | dict(crafting_practice_trials_by_task_and_missing=data))
            assert page.locator('#crafting-practice table').count() == 0
            assert 'unavailable' in page.locator('#crafting-practice').inner_text()
        bad_wins = wins.copy(); bad_wins[66] = 41
        bad_slot = attempts.copy(); bad_slot[0] = 1
        for override in [dict(crafting_practice_successes_by_task_and_missing=bad_wins),
                         dict(crafting_practice_trials_by_task_and_missing=bad_slot),
                         dict(crafting_practice_bucket_width=7)]:
            page.evaluate('s => craftingPractice(s)', native | override)
            assert page.locator('#crafting-practice table').count() == 0
        page.evaluate('s => craftingPractice(s)', native | dict(station_acquisition_successes=101))
        assert 'unavailable' in page.locator('#station-opening').inner_text()
        page.evaluate('s => craftingPractice(s)', native | dict(crafting_practice_trials_by_task_and_missing=[0]*108,
                                                               crafting_practice_successes_by_task_and_missing=[0]*108))
        assert page.locator('[data-task="11"][data-missing="0"]').inner_text() == '0 / 0'
        page.evaluate('s => render(s, [s])', status | dict(epoch_millis=int(time.time()*1000)-60000))
        assert page.locator('#state').inner_text() == 'STALE'
        assert page.locator('#rate').inner_text() == '—'
        assert 'historical' in page.locator('#error').inner_text()
        assert not errors, errors
        browser.close()
    report = dict(passed=True, source='synthetic metrics; no Minecraft server',
                  checks=['bucket mapping', 'opening separated', 'assistance labels', 'desktop/mobile bounds',
                          'invalid counts fail closed', 'zero attempts not a percentage', 'stale metrics warning'],
                  browser_errors=errors)
    (args.output/'result.json').write_text(json.dumps(report, indent=2)+'\n')
    print('PASS synthetic crafting dashboard; desktop/mobile, isolated counts, invalid/stale state; no browser errors')


if __name__ == '__main__':
    main()
