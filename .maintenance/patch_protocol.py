"""Produce a reviewed pin-local protocol patch. Not a normal build dependency."""
from pathlib import Path
import difflib, hashlib, urllib.request
path='azalea-client/src/plugins/packet/game/mod.rs'
rev='f8ddefa70cc53e6385785fb56e7a688a389cf0ab'
raw=urllib.request.urlopen(f'https://raw.githubusercontent.com/azalea-rs/azalea/{rev}/{path}',timeout=60).read()
# Match the actual immutable Git blob, not an assumed branch tip.
assert hashlib.sha1(f'blob {len(raw)}\0'.encode()+raw).hexdigest()=='9a1c022bb7e5d274f908c66b5153d2548b671e19'
original=raw.decode();s=original
old='''    pub fn set_cursor_item(&mut self, p: &ClientboundSetCursorItem) {
        debug!("Got set cursor item packet {p:?}");
    }'''
new='''    pub fn set_cursor_item(&mut self, p: &ClientboundSetCursorItem) {
        // Server correction, including clearing a cursor during an episode reset.
        // This is packet synchronization, never an inferred inventory operation.
        as_system::<Query<&mut Inventory>>(self.ecs, |mut query| {
            if let Ok(mut inventory) = query.get_mut(self.player) {
                inventory.carried = p.contents.clone();
            }
        });
    }'''
assert s.count(old)==1;s=s.replace(old,new)
old='''            // container id 0 is always the player's inventory
            if p.container_id == 0 {'''
new='''            // A complete snapshot also acknowledges the cursor and click state.
            // Old/closed menu packets must not overwrite the active menu's cursor.
            if p.container_id == inventory.id {
                inventory.carried = p.carried_item.clone();
                inventory.state_id = p.state_id;
            }
            // container id 0 is always the player's inventory
            if p.container_id == 0 {'''
assert s.count(old)==1;s=s.replace(old,new)
old='''            if p.container_id == -1 {
                // -1 means carried item'''
new='''            if p.container_id == inventory.id {
                inventory.state_id = p.state_id;
            }
            if p.container_id == -1 {
                // -1 means carried item'''
assert s.count(old)==1;s=s.replace(old,new)
old='    pub fn set_player_inventory(&mut self, _p: &ClientboundSetPlayerInventory) {}'
new='''    pub fn set_player_inventory(&mut self, p: &ClientboundSetPlayerInventory) {
        as_system::<Query<&mut Inventory>>(self.ecs, |mut query| {
            let Ok(mut inventory) = query.get_mut(self.player) else { return; };
            // This packet uses PlayerInventory indices, not inventory-menu slots.
            let raw = p.slot as usize;
            let slot = match raw {
                0..=8 => raw + 36,
                9..=35 => raw,
                36..=39 => 44 - raw,
                40 => 45,
                _ => return,
            };
            if let Some(item) = inventory.inventory_menu.slot_mut(slot) {
                *item = p.contents.clone();
            }
            // Keep the player portion of an open vanilla container in sync too.
            if raw < 36 {
                if let Some(menu) = inventory.container_menu.as_mut() {
                    let len = menu.slots().len();
                    if len >= 36 {
                        let offset = if raw < 9 { 27 + raw } else { raw - 9 };
                        if let Some(item) = menu.slot_mut(len - 36 + offset) {
                            *item = p.contents.clone();
                        }
                    }
                }
            }
        });
    }'''
assert s.count(old)==1;s=s.replace(old,new)
patch=''.join(difflib.unified_diff(original.splitlines(True),s.splitlines(True),fromfile='a/'+path,tofile='b/'+path))
Path('pins/azalea-client.patch').write_text(patch)
p=Path('scripts/build.sh');s=p.read_text()
needle='git -C "$repo" diff --exit-code -- Cargo.lock Cargo.toml azalea/Cargo.toml\n'
if 'pins/azalea-client.patch' not in s:
    assert needle in s
    s=s.replace(needle,needle+'''# Restore only this declared file in the disposable vendor checkout, then
# apply the repository-owned protocol correction to the exact pinned revision.
# The operator's checkout and data are never reset by this step.
git -C "$repo" restore --source="$AZALEA_REV" --worktree -- azalea-client/src/plugins/packet/game/mod.rs
git -C "$repo" apply --check "$BCMC_ROOT/pins/azalea-client.patch"
git -C "$repo" apply "$BCMC_ROOT/pins/azalea-client.patch"
''')
    s=s.replace('echo "Azalea=$AZALEA_REV"','echo "Azalea=$AZALEA_REV"\n  echo "AzaleaPatch=pins/azalea-client.patch"')
    p.write_text(s)
p=Path('scripts/build-state.sh');s=p.read_text()
s=s.replace("printf '%s\\0' learning/Cargo.toml", "printf '%s\\0' learning/Cargo.toml pins/azalea-client.patch")
p.write_text(s)
p=Path('app/main.rs');s=p.read_text()
if 'mod protocol_tests;' not in s:s+='\n#[cfg(test)] mod protocol_tests;\n'
p.write_text(s)
print('Pinned protocol patch generated: cursor, complete click-state snapshots and player inventory notifications. Normal build uses git apply, not Python.')
