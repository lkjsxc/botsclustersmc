"""Diagnostic-only corrections and bounded state traces. No learning changes."""
from pathlib import Path
p=Path('tests/live-client.rs')
s=p.read_text()
old='let mut builder=SwarmBuilder::new_without_plugins().add_plugins((DefaultPlugins,DefaultBotPlugins.build()'
new='''// The pinned Azalea physics does not implement spectator flight. This
            // observer-only probe sends no gameplay movement; retain real server
            // teleports/packets instead of applying survival gravity to its camera.
            // The 18 gameplay fixture clients still use the full physics plugin.
            let defaults=if observer{DefaultPlugins.build().disable::<azalea::physics::PhysicsPlugin>()}else{DefaultPlugins.build()};
            let mut builder=SwarmBuilder::new_without_plugins().add_plugins((defaults,DefaultBotPlugins.build()'''
assert old in s
s=s.replace(old,new)
s=s.replace('if !ok{return Ok(false);}},','''if !ok{
            // Retry a literal click, not forced block interaction. A screen open
            // is acknowledged by the actual menu, never a fixed delay alone.
            if f.tick%16<4{a[4]=2;}return Ok(false);
        }},''')
old='if self.started.elapsed()>Duration::from_secs(200){return Err(format!("fixture {} timed out; pending operation {:?}",self.stage,self.ops.front()));}'
new='''if self.started.elapsed()>Duration::from_secs(200){
            let raw=fs::read_to_string(root().join(format!(".runtime/lab/frame-{}.txt",self.actor))).unwrap_or_default();
            return Err(format!("fixture {} timed out; actor={} pending={:?} episode_started={} token={} raw={raw:?} menu={:?} cursor={:?}",self.stage,self.actor,self.ops.front(),self.episode.is_some(),l.token(),bot.menu(),carried(bot)));
        }
        if self.stage>=5&&self.ticks%100==0{
            let pos=azalea::core::position::BlockPos::new(l.goal[0].floor() as i32,l.goal[1].floor() as i32,l.goal[2].floor() as i32);
            let block={let world=bot.world();let w=world.read();w.get_block_state(pos)};
            eprintln!("DIAGNOSTIC STATE actor={} stage={} episode={} op={:?} client={:?} target={:?} block={block:?} hit={:?} held={:?} selected={} cursor={:?}",self.actor,self.stage,self.episode.is_some(),self.ops.front(),bot.position(),pos,bot.hit_result(),bot.get_held_item(),bot.selected_hotbar_slot(),carried(bot));
        }'''
assert old in s
s=s.replace(old,new)
p.write_text(s)
print('Observer diagnostic no longer applies survival gravity; gameplay traces expose real hit/menu/held-item state. No normal policy changes.')
