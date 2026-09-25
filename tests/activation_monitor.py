"""Synthetic read-only activation telemetry checks; no Minecraft server or policy is used."""
import json
from pathlib import Path


# The same synthetic instant in Python and the page, independent of process startup.
FIXTURE_EPOCH_MS = 1_700_000_000_000


def install_fixture(page, status: dict) -> None:
    """Use only in disposable synthetic pages, never with a live server."""
    page.context.set_offline(True)
    page.evaluate("""s => {
        Date.now = () => s.epoch_millis;
        // Initial update() still runs; later refreshes cannot overwrite an assertion.
        window.setTimeout = () => 0;
        window.fetch = async path => ({ok: true, json: async () =>
            path === '/api/status' ? structuredClone(s) :
            path === '/api/history' ? [structuredClone(s)] : {result: null, monitor: null}});
    }""", status)


def verify_activation_health(browser, html: str, output: Path) -> None:
    now = FIXTURE_EPOCH_MS
    samples = [0] * 19
    samples[0], samples[10], samples[18] = 32, 128, 1
    first, second, slope1, slope2 = ([-1] * 19 for _ in range(4))
    for task, values in {0: (.1, .02, .7, .9), 10: (.25, .8, .4, .1), 18: (.5, .9, .3, .05)}.items():
        first[task], second[task], slope1[task], slope2[task] = values
    status = dict(epoch_millis=now, state='running', schema='bcmc-citizen-egocentric-context', policy_updates=259001,
                  activation_health_scope='last-learner-batch-unweighted', activation_health_policy_updates=259000,
                  activation_health_epoch_millis=now, activation_health_update_accepted=True,
                  activation_health_samples=161, activation_health_hidden_units=96,
                  activation_health_saturation_threshold=.99, activation_health_task_samples=samples,
                  activation_health_first_saturation=first, activation_health_second_saturation=second,
                  activation_health_first_mean_slope=slope1, activation_health_second_mean_slope=slope2)
    page = browser.new_page(viewport={'width': 1280, 'height': 1000}, locale='en-US')
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    install_fixture(page, status)
    page.set_content(html, wait_until='load')
    page.wait_for_function("document.getElementById('activation-section').dataset.state === 'measured'")
    assert page.locator('#activation-table tbody tr').count() == 3
    assert '259,000 (before update)' in page.locator('#activation-identity').inner_text()
    assert page.locator('#activation-table [data-task="10"] [data-metric="2"]').inner_text() == '80.0%'
    assert page.locator('#activation-table [data-task="10"] [data-metric="4"]').inner_text() == '0.1000'
    assert page.locator('#activation-table [data-task="9"]').count() == 0
    assert 'Unclassified observation' in page.locator('#activation-table [data-task="18"]').inner_text()
    assert 'not a census of NPCs' in page.locator('#activation-section').inner_text()
    assert 'not 0%' in page.locator('#activation-table caption').inner_text()
    page.locator('#activation-section').screenshot(path=str(output / 'activation-desktop.png'))
    page.set_viewport_size({'width': 390, 'height': 844})
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    page.locator('#activation-section').screenshot(path=str(output / 'activation-mobile.png'))
    encoded = {k: json.dumps(v) if isinstance(v, list) else v for k, v in status.items()}
    page.evaluate('s => activationHealth(s)', encoded)
    assert page.locator('#activation-table tbody tr').count() == 3
    for override in [dict(activation_health_epoch_millis=now - 60000), dict(epoch_millis=now - 60000),
                     dict(activation_health_epoch_millis=now + 60000), dict(state='stopped')]:
        page.evaluate('s => activationHealth(s)', status | override)
        assert page.locator('#activation-section').get_attribute('data-state') == 'historical'
        assert 'Historical measurement' in page.locator('#activation-identity').inner_text()
    # Freeze the clock rather than widening the production freshness threshold.
    for field in ['epoch_millis', 'activation_health_epoch_millis']:
        for age, expected in [(0, 'measured'), (15000, 'measured'), (15001, 'historical'),
                              (-5000, 'measured'), (-5001, 'historical')]:
            page.evaluate('s => activationHealth(s)', status | {field: now - age})
            assert page.locator('#activation-section').get_attribute('data-state') == expected, (field, age)
    page.evaluate('s => activationHealth(s)', status | dict(activation_health_update_accepted=False))
    assert 'measured, not learned' in page.locator('#activation-identity').inner_text()
    invalid = [dict(activation_health_samples=162), dict(activation_health_samples=0),
               dict(activation_health_policy_updates=-1), dict(activation_health_epoch_millis=0),
               dict(activation_health_update_accepted='false'), dict(activation_health_scope='other'),
               dict(activation_health_hidden_units=0), dict(activation_health_hidden_units=4097),
               dict(activation_health_saturation_threshold=0), dict(activation_health_saturation_threshold=1)]
    for field in ['task_samples', 'first_saturation', 'second_saturation', 'first_mean_slope', 'second_mean_slope']:
        key = 'activation_health_' + field
        for data in [None, [], [0] * 18, [0] * 20, ['<img src=x onerror=alert(1)>'] * 19]:
            invalid.append({key: data})
    for value in [-1, .5, 2**54, None]:
        data = samples.copy(); data[10] = value
        invalid.append(dict(activation_health_task_samples=data))
    for field in ['first_saturation', 'second_saturation', 'first_mean_slope', 'second_mean_slope']:
        key = 'activation_health_' + field
        for index, value in [(10, -1), (10, 1.01), (10, None), (9, 0)]:
            data = status[key].copy(); data[index] = value
            invalid.append({key: data})
    for override in invalid:
        page.evaluate('s => activationHealth(s)', status | override)
        assert page.locator('#activation-table table').count() == 0, override
        assert page.locator('#activation-section').get_attribute('data-state') == 'unavailable', override
    page.evaluate('() => activationHealth({})')
    assert 'unavailable' in page.locator('#activation-identity').inner_text()
    assert not errors, errors
    page.close()
    (output / 'activation-result.json').write_text(json.dumps(dict(
        passed=True, source='synthetic telemetry; no Minecraft server or weights',
        invalid_cases=len(invalid), browser_errors=errors,
        checks=['render wiring', 'raw task counts', 'pre-update identity', 'unclassified bucket',
                'absence is not zero', 'native/string arrays', 'measurement/status freshness', 'exact freshness/future-skew boundaries',
                'rejected is not learned', 'desktop/mobile bounds', 'invalid data fails closed']), indent=2) + '\n')
    print(f'PASS synthetic activation dashboard; {len(invalid)} invalid cases, identity, stale/absent/rejected state and desktop/mobile')
