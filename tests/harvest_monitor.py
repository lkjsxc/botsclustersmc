"""Synthetic frozen-report rendering checks. Never gameplay or skill evidence."""
from copy import deepcopy
from pathlib import Path
from activation_monitor import FIXTURE_EPOCH_MS, install_fixture


def verify_harvest(browser, html: str, output: Path) -> None:
    status = dict(epoch_millis=FIXTURE_EPOCH_MS, state='running', policy_updates=8,
                  schema='bcmc-citizen-egocentric-context', active_agents=2)
    harvest = dict(state='recorded', scope='decision-boundary-not-every-tick',
                   recorded_trials=2, expected_trials=2, observations=12,
                   menu_focused_selections=1, world_dig_selections=3, held_pick_observations=1,
                   target_pick_contact_observations=1, target_other_contact_observations=2,
                   max_target_pick_ticks=3, max_target_other_ticks=5, interaction_selections=[9, 3, 0, 0],
                   trials_with_pick_contact=1, trials_with_target_contact=2,
                   trials_with_any_block_broken=1, trials_with_any_item_collected=0)
    report = dict(complete=True, stochastic=True, new_training_samples=0,
                  policy_updates=7, epoch_millis=FIXTURE_EPOCH_MS, schema=status['schema'],
                  tasks=[dict(task=12, cases=2, passed=0, harvest=harvest)])
    page = browser.new_page(viewport={'width': 1280, 'height': 1000}, locale='en-US')
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    install_fixture(page, status)
    page.set_content(html, wait_until='load')
    page.wait_for_function("document.getElementById('state').textContent === 'RUNNING'")

    def show(result, monitor=None):
        page.evaluate('''async data => {
            window.fetch = async () => ({ok: true, json: async () => data.payload});
            await renderEvaluation(data.live);
        }''', dict(payload=dict(result=result, monitor=monitor), live=status))

    def invalid(result):
        show(report)  # A rejected refresh must also clear a previous valid display.
        show(result)
        assert 'Invalid' in page.locator('#evaluation-progress').inner_text()
        assert page.locator('#evaluation-harvest table').count() == 0
        assert page.locator('#evaluation-results .row').count() == 0
        assert page.locator('#state').inner_text() == 'RUNNING'

    show(report)
    assert page.locator('#evaluation-harvest tbody tr').count() == 5
    text = page.locator('#evaluation-harvest').inner_text()
    assert '3 / 12' in text and '25.0%' in text
    assert '2/2 trials recorded' in text and 'Any item collected: 0/2' in text
    assert 'not percentages of elapsed ticks' in text and 'not necessarily the task target' in text
    assert page.locator('#evaluation-results .row').inner_text().endswith('0/2')
    assert 'Tested policy #7' in page.locator('#evaluation-identity').inner_text()
    page.locator('#frozen-evaluation').screenshot(path=str(output/'harvest-desktop.png'))
    page.set_viewport_size({'width': 390, 'height': 844})
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    page.locator('#frozen-evaluation').screenshot(path=str(output/'harvest-mobile.png'))
    for key in ['observations', 'world_dig_selections', 'held_pick_observations',
                'target_pick_contact_observations', 'target_other_contact_observations',
                'max_target_pick_ticks', 'trials_with_target_contact']:
        for value in [None, -1, .5, '1', 2**54]:
            bad = deepcopy(report);bad['tasks'][0]['harvest'][key] = value;invalid(bad)
    for override in [dict(scope='every-tick'), dict(state='partial'), dict(recorded_trials=1),
                     dict(expected_trials=3), dict(menu_focused_selections=12), dict(held_pick_observations=0),
                     dict(interaction_selections=[9, 2, 0, 0]), dict(interaction_selections=[9, 3]),
                     dict(max_target_other_ticks=0), dict(trials_with_any_item_collected=3),
                     dict(trials_with_target_contact=0)]:
        bad = deepcopy(report);bad['tasks'][0]['harvest'].update(override);invalid(bad)
    for override in [dict(diagnostic_only=True), dict(diagnostic_only='false'),
                     dict(reset_intervention='workbench-open'), dict(new_training_samples=1)]:
        invalid(report | override)
    # No partial/favorable subset can leave the first table visible after a later failure.
    bad = deepcopy(report);bad['tasks'].append(dict(task=5, cases=2, passed=2, harvest=None));invalid(bad)
    missing = deepcopy(report);missing['tasks'][0].pop('harvest');show(missing)
    assert 'not recorded' in page.locator('#evaluation-harvest').inner_text()
    assert 'not a measured zero' in page.locator('#evaluation-harvest').inner_text()
    assert page.locator('#evaluation-harvest table').count() == 0
    assert page.locator('#evaluation-results .row').count() == 1
    for count, state in [(0, 'not-recorded'), (1, 'partial')]:
        partial = deepcopy(report)
        partial['tasks'][0]['harvest'] = dict(state=state, scope=harvest['scope'], recorded_trials=count, expected_trials=2)
        show(partial)
        assert 'No aggregate is shown' in page.locator('#evaluation-harvest').inner_text()
        assert page.locator('#evaluation-harvest table').count() == 0
        partial['tasks'][0]['harvest']['observations'] = 12;invalid(partial)
    show(report, dict(state='running', epoch_millis=FIXTURE_EPOCH_MS-120000, detail='Synthetic fixture'))
    assert 'stale' in page.locator('#evaluation-progress').inner_text()
    assert page.locator('#evaluation-harvest table').count() == 1
    show(None)
    assert page.locator('#evaluation-harvest table').count() == 0
    assert 'No independent result' in page.locator('#evaluation-identity').inner_text()
    assert not errors, errors
    page.close()
    print('PASS synthetic harvesting UI: denominators, outcomes, missing/partial/malformed reports, stale clearing, desktop/mobile')
