"""Reviewed independent diagnostic-client fixes; not used by normal startup."""
from pathlib import Path
p=Path('tests/live-client.rs');s=p.read_text()
old='Event::Spawn=>{s.ready=true;s.ticks=0;},'
new='Event::Spawn=>{bot.set_client_information(azalea::ClientInformation{view_distance:if s.observer{16}else{3},..Default::default()});s.ready=true;s.ticks=0;},'
if old in s:s=s.replace(old,new)
else:assert new in s
s=s.replace('.left_click(53)', '.left_click(53usize)').replace('.left_click(18)', '.left_click(18usize)')
if 'impl Default for State' not in s:
    needle='#[derive(Component,Clone)]struct State(Arc<Mutex<Check>>);'
    assert needle in s
    s=s.replace(needle,needle+'\nimpl Default for State{fn default()->Self{Self(Arc::new(Mutex::new(Check::new(false))))}}')
p.write_text(s)
print('Independent diagnostic state/slot types and observer render negotiation corrected.')
