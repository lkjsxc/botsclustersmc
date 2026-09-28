"""Offline HTTP-failure and freshness checks for the actual observatory page."""
import json
from pathlib import Path
from activation_monitor import FIXTURE_EPOCH_MS, install_fixture


def verify_availability(browser, html: str, output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    page = browser.new_page(viewport={"width": 1280, "height": 1000}, locale="en-US")
    errors, failures = [], []
    checks = 0

    def check(ok, message):
        nonlocal checks
        checks += 1
        if not ok:
            failures.append(message)

    live = dict(epoch_millis=FIXTURE_EPOCH_MS, state="running", schema="test-schema",
                active_agents=7, learner_samples_per_second=100, trained_samples=12000,
                policy_updates=900, process_cpu_cores=.5, available_processors=2)
    report = dict(complete=True, stochastic=True, new_training_samples=0,
                  schema="test-schema", epoch_millis=FIXTURE_EPOCH_MS,
                  policy_updates=123, tasks=[dict(task=0, cases=2, passed=1)])
    page.on("pageerror", lambda e: errors.append(str(e)))
    install_fixture(page, live)
    page.set_content(html, wait_until="load")
    page.wait_for_function("document.getElementById('state').textContent === 'RUNNING'")
    version = 123

    def apply(status="ok", history="ok", evaluation="ok", data=None):
        nonlocal version
        version += 1
        page.evaluate("""async d => {
            window.fetch = async path => {
                const part=path.slice('/api/'.length),mode=d[part];
                if(mode==='network')throw new Error('Synthetic network failure');
                return {ok:mode!=='http',json:async()=>{
                    if(mode==='json')throw new Error('Synthetic invalid JSON');
                    if(part==='status')return mode==='shape'?null:structuredClone(d.live);
                    if(part==='history')return mode==='shape'?{}:mode==='rows'?[null,7,{},d.live]:[d.live];
                    return {result:mode==='shape'?{}:d.report,monitor:null};
                }};
            };
            await update();
        }""", dict(status=status, history=history, evaluation=evaluation,
                    live=live if data is None else data, report=report | dict(policy_updates=version)))

    def evaluation_current(label):
        check(f"Tested policy #{version}" in page.locator("#evaluation-identity").inner_text(), label + ": evaluation refreshed")
        check(page.locator("#evaluation-results .row").count() == 1, label + ": complete result remains visible")

    apply()
    evaluation_current("initial")
    for mode in ["http", "network", "json", "shape", "rows"]:
        apply(history=mode)
        check(page.locator("#state").inner_text() == "RUNNING", mode + ": history must not hide current status")
        check(page.locator("#rate").inner_text() == "100", mode + ": current throughput retained")
        evaluation_current("history-" + mode)
        if mode != "rows":
            check("history unavailable" in page.locator("#error").inner_text().lower(), mode + ": history error explained")

    for mode in ["http", "network", "json", "shape"]:
        apply()  # Start every failure from a successful observation, not an empty page.
        apply(status=mode)
        check(page.locator("#state").inner_text() == "UNAVAILABLE", mode + ": unavailable status")
        for field in ["agents", "rate", "cpu", "burning", "failures", "heap"]:
            check(page.locator("#" + field).inner_text() == "—", mode + ": clear old " + field)
        evaluation_current("status-" + mode)
        check("Live policy unavailable" in page.locator("#evaluation-identity").inner_text(), mode + ": no fabricated live identity")
    page.screenshot(path=str(output / "availability-unavailable.png"), full_page=True)

    for state in ["stopped", "failed", "starting"]:
        apply(data=live | dict(state=state))
        check(page.locator("#state").inner_text() == state.upper(), state + ": state label")
        for field in ["agents", "rate", "cpu", "burning"]:
            check(page.locator("#" + field).inner_text() == "—", state + ": not live " + field)
        evaluation_current(state)

    # Exact deterministic age boundaries; no widened production threshold or clock waits.
    for age, expected in [(0, "RUNNING"), (15000, "RUNNING"), (15001, "STALE"),
                          (-5000, "RUNNING"), (-5001, "STALE"), (-60000, "STALE")]:
        apply(data=live | dict(epoch_millis=FIXTURE_EPOCH_MS-age))
        check(page.locator("#state").inner_text() == expected, f"age {age}: exact freshness state")
        check(page.locator("#rate").inner_text() == ("100" if expected == "RUNNING" else "—"), f"age {age}: throughput scope")
        evaluation_current("freshness")
    for epoch in [None, 0, -1, str(FIXTURE_EPOCH_MS), 1.5, 2**54]:
        apply(data=live | dict(epoch_millis=epoch))
        check(page.locator("#state").inner_text() == "STALE", repr(epoch) + ": invalid clock is not fresh")
        check(page.locator("#rate").inner_text() == "—", repr(epoch) + ": invalid clock clears rate")

    for mode in ["http", "network", "json", "shape"]:
        apply(evaluation=mode)
        check(page.locator("#state").inner_text() == "RUNNING", mode + ": evaluation independent from status")
        check(page.locator("#evaluation-results .row").count() == 0, mode + ": no previous result after invalid evaluation")
    apply()
    check(page.locator("#state").inner_text() == "RUNNING", "recovery")
    check(page.locator("#error").inner_text() == "", "recovery clears transient error")
    check(page.locator("#agents").inner_text() == "7", "recovery restores current actor count")
    evaluation_current("recovery")
    page.screenshot(path=str(output / "availability-desktop.png"), full_page=True)
    page.set_viewport_size({"width": 390, "height": 844})
    check(page.evaluate("document.documentElement.scrollWidth <= innerWidth"), "mobile width")
    page.screenshot(path=str(output / "availability-mobile.png"), full_page=True)
    check(not errors, "no browser exceptions: " + repr(errors))
    page.close()
    output.mkdir(parents=True, exist_ok=True)
    (output / "availability-result.json").write_text(json.dumps(dict(
        passed=not failures, checks=checks, failures=failures, browser_errors=errors,
        source="offline synthetic telemetry; actual page JavaScript; no Minecraft or policy change"),
        indent=2) + "\n")
    assert not failures, f"{len(failures)} failures / {checks} checks: " + "; ".join(failures)
    print(f"PASS synthetic observatory failure isolation and exact freshness; {checks} checks")
