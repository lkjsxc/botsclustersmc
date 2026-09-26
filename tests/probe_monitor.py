"""Offline browser checks for observed probe-policy provenance, not skill evidence."""
import json
from activation_monitor import FIXTURE_EPOCH_MS, install_fixture


FIELDS = ('single_policy_trials','single_policy_successes','mixed_policy_trials','mixed_policy_successes',
          'behavior_decisions','policy_changes','maximum_policy_span','last_minimum_policy','last_maximum_policy')


def fixture():
    status = dict(epoch_millis=FIXTURE_EPOCH_MS, state='running',
                  probe_policy_scope='completed-probes-applied-behavior-versions-this-process')
    for field in FIELDS:
        status['probe_'+field]=[-1 if field.startswith('last_') else 0]*18
    for field,value in zip(FIELDS,(2,1,3,2,20,5,9,12,17)):
        status['probe_'+field][11]=value
    status['probe_trials_this_process']=[0]*18;status['probe_trials_this_process'][11]=5
    status['probe_successes_this_process']=[0]*18;status['probe_successes_this_process'][11]=3
    return status


def verify_probe_policies(browser, html, output):
    page=browser.new_page(viewport={'width':1280,'height':1000});errors=[]
    page.on('pageerror',lambda error:errors.append(str(error)))
    status=fixture();install_fixture(page,status);page.set_content(html,wait_until='load')
    page.wait_for_function("document.getElementById('probe-policy-section').dataset.state === 'measured'")
    row=page.locator('#probe-policy-table tr[data-task="11"]')
    assert row.locator('[data-metric="0"]').inner_text()=='1 / 2'
    assert row.locator('[data-metric="1"]').inner_text()=='2 / 3'
    assert row.locator('[data-metric="2"]').inner_text()=='5'
    assert row.locator('[data-metric="3"]').inner_text()=='9'
    assert page.locator('#probe-policy-table tbody tr').count()==1
    assert 'not one common policy' in page.locator('#probe-policy-section').inner_text()
    assert 'Neither group certifies' in page.locator('#probe-policy-section').inner_text()
    page.screenshot(path=str(output/'probe-policy-desktop.png'),full_page=True)
    page.set_viewport_size({'width':390,'height':844})
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    page.screenshot(path=str(output/'probe-policy-mobile.png'),full_page=True)
    serialized={k:json.dumps(v) if isinstance(v,list) else v for k,v in status.items()}
    page.evaluate('s => probePolicies(s)',serialized)
    assert row.locator('[data-metric="1"]').inner_text()=='2 / 3'
    for state,age,expected in [('running',0,'measured'),('running',15000,'measured'),('running',15001,'historical'),
                               ('paused',0,'historical'),('failed',0,'historical')]:
        page.evaluate('s => probePolicies(s)',status|dict(state=state,epoch_millis=FIXTURE_EPOCH_MS-age))
        assert page.locator('#probe-policy-section').get_attribute('data-state')==expected
    for field in FIELDS:
        for bad in (None,[],[-2]*18,[0.5]*18,[2**54]*18,['<img src=x onerror=alert(1)>']*18):
            page.evaluate('s => probePolicies(s)',status|{'probe_'+field:bad})
            assert page.locator('#probe-policy-table table').count()==0
            assert page.locator('#probe-policy-section').get_attribute('data-state')=='unavailable'
    for field,value in [('probe_trials_this_process',6),('probe_successes_this_process',2),
                        ('probe_single_policy_successes',3),('probe_mixed_policy_successes',4),
                        ('probe_behavior_decisions',4),('probe_policy_changes',2),
                        ('probe_maximum_policy_span',0),('probe_last_minimum_policy',18)]:
        values=status[field].copy();values[11]=value
        page.evaluate('s => probePolicies(s)',status|{field:values})
        assert page.locator('#probe-policy-table table').count()==0
    page.evaluate('s => probePolicies(s)',{})
    assert 'unavailable' in page.locator('#probe-policy-identity').inner_text()
    zero={k:[-1 if 'last_' in k else 0]*18 if isinstance(v,list) else v for k,v in status.items()}
    page.evaluate('s => probePolicies(s)',zero)
    assert 'No completed probes' in page.locator('#probe-policy-table').inner_text()
    assert 'no success rate' in page.locator('#probe-policy-table').inner_text()
    assert not errors,errors
    page.close()
    print('PASS synthetic probe-policy browser checks: counts, missing/invalid/stale state, atomic partitions and mobile bounds')
