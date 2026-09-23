"""Reviewed pinned-Azalea compatibility fixes; removed before final delivery."""
from pathlib import Path
p=Path('learning/src/control.rs')
s=p.read_text().replace('pub policy:Arc<Model>,identity:PolicyId,pub course:Curriculum,','pub policy:Arc<Model>,pub course:Curriculum,')
assert s.count('identity:PolicyId') == 1
p.write_text(s)
for name in ['app/sensor.rs','tests/live-client.rs']:
    p=Path(name);s=p.read_text()
    for menu in ['Crafting','Furnace','Generic9x3','Generic9x6']:
        s=s.replace(f'Menu::{menu}(_)',f'Menu::{menu}{{..}}')
    s=s.replace('Menu::Crafting(Default::default())','Menu::Crafting{result:Default::default(),grid:Default::default(),player:Default::default()}')
    s=s.replace('Menu::Furnace(Default::default())','Menu::Furnace{ingredient:Default::default(),fuel:Default::default(),result:Default::default(),player:Default::default()}')
    old='if s.observer{let text=format!("{packet:?}");if text.starts_with("SetChunkCacheRadius"){eprintln!("OBSERVER {text}");if text.contains("12"){s.view12=true;}if text.contains("16"){s.view16=true;}}}'
    new='if s.observer{if let azalea::protocol::packets::game::ClientboundGamePacket::SetChunkCacheRadius(p)=packet.as_ref(){eprintln!("OBSERVER cache radius={}",p.radius);s.view12|=p.radius==12;s.view16|=p.radius==16;}}'
    s=s.replace(old,new)
    p.write_text(s)
print('Pinned struct menu variants and bounded observer radius diagnostics applied.')
