#!/usr/bin/env python3
"""Synthetic read-only dashboard checks. Does not launch or contact Minecraft.

Requires Python Playwright and Chromium/Chrome (or Playwright's installed browser).
"""
import argparse
import json
import os
from pathlib import Path
import shutil
from playwright.sync_api import sync_playwright
from activation_monitor import FIXTURE_EPOCH_MS, install_fixture, verify_activation_health
from probe_monitor import verify_probe_policies

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / '.build/evidence/crafting-monitor')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    attempts, wins = [0] * 108, [0] * 108
    attempts[66:72] = [40, 30, 20, 10, 5, 2]
    wins[66:72] = [38, 20, 10, 4, 1, 0]
    status = dict(epoch_millis=FIXTURE_EPOCH_MS, state='running',
                  schema='bcmc-citizen-egocentric-context', active_agents=64,
                  learner_samples_per_second=100, trained_samples=12000,
                  course_task_population=json.dumps([0]*11+[64]+[0]*6),
                  practice_success_ema=.7, probe_success_ema=0,
                  station_success='task-completion',
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
        install_fixture(page, status)
        page.set_content((ROOT/'host/monitor.html').read_text(), wait_until='load')
        page.wait_for_function("document.getElementById('state').textContent === 'RUNNING'")
        assert page.locator('#state').inner_text() == 'RUNNING'
        assert page.locator('#population .row').count() == 18
        assert page.locator('#crafting-practice tbody tr').count() == 5
        assert page.locator('[data-task="11"][data-missing="0"]').inner_text() == '38 / 40'
        assert page.locator('[data-task="11"][data-missing="5"]').inner_text() == '0 / 2'
        assert page.locator('[data-task="8"][data-missing="5"]').inner_text() == '—'
        assert 'Opening alone is never success' in page.locator('#station-goal').inner_text()
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
        for value in [None, 'opening', '<img src=x onerror=alert(1)>', 1, False]:
            page.evaluate('s => craftingPractice(s)', native | dict(station_success=value))
            assert 'unavailable' in page.locator('#station-goal').inner_text()
        page.evaluate('s => craftingPractice(s)', native | dict(crafting_practice_trials_by_task_and_missing=[0]*108,
                                                               crafting_practice_successes_by_task_and_missing=[0]*108))
        assert page.locator('[data-task="11"][data-missing="0"]').inner_text() == '0 / 0'
        # These are exact age boundaries, not wall-clock waits or looser assertions.
        for age, expected in [(0, 'RUNNING'), (14999, 'RUNNING'), (15000, 'RUNNING'),
                              (15001, 'STALE'), (60000, 'STALE')]:
            page.evaluate('s => render(s, [s])', status | dict(epoch_millis=FIXTURE_EPOCH_MS-age))
            assert page.locator('#state').inner_text() == expected, age
            assert page.locator('#rate').inner_text() == ('—' if expected == 'STALE' else '100'), age
            assert ('historical' in page.locator('#error').inner_text()) == (expected == 'STALE'), age
        assert not errors, errors
        verify_activation_health(browser, (ROOT/'host/monitor.html').read_text(), args.output)
        verify_probe_policies(browser, (ROOT/'host/monitor.html').read_text(), args.output)
        browser.close()
    report = dict(passed=True, source='synthetic metrics; no Minecraft server',
                  checks=['bucket mapping', 'one completion goal', 'assistance labels', 'desktop/mobile bounds',
                          'invalid counts fail closed', 'zero attempts not a percentage', 'stale metrics warning',
                          'fixed fixture clock', 'exact 15-second freshness boundary',
                          'probe single/mixed policy partition', 'missing behavior identities never inferred'],
                  browser_errors=errors)
    (args.output/'result.json').write_text(json.dumps(report, indent=2)+'\n')
    print('PASS synthetic crafting dashboard; desktop/mobile, isolated counts, invalid/stale state; no browser errors')


if __name__ == '__main__':
    main()
