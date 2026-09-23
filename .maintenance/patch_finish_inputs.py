"""Bounded diagnostic correction and a pin-local self-entity picking guard."""
from pathlib import Path
import difflib,hashlib,urllib.request
p=Path('tests/live-client.rs');s=p.read_text()
old='for i in 0..if l.stage==16{3}else{1}'
new='for i in (0..if l.stage==16{3}else{1}).rev()'
assert old in s;s=s.replace(old,new)
s=s.replace('// Diagnostic control uses the current input-device view;', '// Scripted platform placement proceeds far-to-near so earlier blocks do\n        // not occlude later floor targets. This is NOT the learned policy.\n        // Diagnostic control uses the current input-device view;')
needle='if self.stage>=5&&self.ticks%100==0{'
assert needle in s
s=s.replace(needle,needle+'''
            if let Some(hit)=bot.hit_result().as_entity_hit_result(){
                let ecs=bot.ecs.read();
                eprintln!("DIAGNOSTIC ENTITY actor={} source={:?} source_id={:?} picked={:?} picked_id={:?} picked_position={:?}",self.actor,bot.entity,ecs.get::<azalea::core::entity_id::MinecraftEntityId>(bot.entity),hit.entity,ecs.get::<azalea::core::entity_id::MinecraftEntityId>(hit.entity),ecs.get::<azalea::entity::Position>(hit.entity));
            }
''')
p.write_text(s)
# A remote mirror may already exist when a joining local player is indexed.
# A vanilla player cannot target its own server identity, even when two ECS
# entities temporarily represent it. Do not filter out OTHER player identities.
path='azalea-client/src/plugins/interact/pick.rs';rev='f8ddefa70cc53e6385785fb56e7a688a389cf0ab'
raw=urllib.request.urlopen(f'https://raw.githubusercontent.com/azalea-rs/azalea/{rev}/{path}',timeout=60).read()
assert hashlib.sha1(f'blob {len(raw)}\0'.encode()+raw).hexdigest()=='1fed584abfb85ea405a3f3ea9b65649e4e2b11bc'
original=raw.decode();s=original
s=s.replace('    aabb::Aabb,','    aabb::Aabb,\n    entity_id::MinecraftEntityId,',1)
s=s.replace("(Option<&'a ArmorStandMarker>, Option<&'a InGround>),", "(Option<&'a ArmorStandMarker>, Option<&'a InGround>, Option<&'a MinecraftEntityId>),")
s=s.replace('    let is_pickable = |entity: Entity| {', '''    let source_id = opts.pickable_query.get(opts.source_entity).ok().and_then(|(_, _, id)| id);
    let is_pickable = |entity: Entity| {''')
old='''        if let Ok((armor_stand_marker, arrow_in_ground)) = opts.pickable_query.get(entity) {
            !(armor_stand_marker == Some(&ArmorStandMarker(true))'''
new='''        if let Ok((armor_stand_marker, arrow_in_ground, candidate_id)) = opts.pickable_query.get(entity) {
            // Shared-client joins can briefly retain a remote ECS mirror of this
            // exact local server identity. It is self, not a pickable obstacle.
            // Other identities retain ordinary entity occlusion and reach rules.
            if candidate_id.is_some() && candidate_id == source_id {
                return false;
            }
            !(armor_stand_marker == Some(&ArmorStandMarker(true))'''
assert old in s;s=s.replace(old,new)
patch=''.join(difflib.unified_diff(original.splitlines(True),s.splitlines(True),fromfile='a/'+path,tofile='b/'+path))
p=Path('pins/azalea-client.patch');current=p.read_text();assert path not in current;p.write_text(current+patch)
p=Path('scripts/build.sh');s=p.read_text();old='--worktree -- azalea-client/src/plugins/packet/game/mod.rs';assert old in s;s=s.replace(old,old+' azalea-client/src/plugins/interact/pick.rs');p.write_text(s)
print('Diagnostic builds far-to-near; shared picker excludes only the local player identity, not other entities.')
