"""Align the independent observer client with the documented render setting."""
from pathlib import Path
p=Path('tests/live-client.rs');s=p.read_text()
old='Event::Spawn=>{s.ready=true;s.ticks=0;},'
new='Event::Spawn=>{bot.set_client_information(azalea::ClientInformation{view_distance:if s.observer{16}else{3},..Default::default()});s.ready=true;s.ticks=0;},'
if old in s:s=s.replace(old,new)
else:assert new in s
p.write_text(s)
print('Diagnostic observer advertises 16-chunk client render distance; actor diagnostic remains at three.')
