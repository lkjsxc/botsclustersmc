//! A learned action is translated to vanilla input. No target search, route
//! search, best-tool selection, crafting recipe selection, or task scheduling.
use azalea::{Client,WalkDirection};
use azalea::protocol::packets::game::{s_player_action::{Action,ServerboundPlayerAction}};
use azalea::core::{direction::Direction,position::BlockPos};

/// Release the input device, not a task-level action or a chosen target.
fn release_mining(bot: &Client) {
    use azalea::mining::{LeftClickMine, Mining, MiningQueued, MineProgress, MineTicks};
    use azalea::packet::game::SendGamePacketEvent;
    let mut ecs = bot.ecs.write();
    // Capture the existing target only to ABORT it. Never start a chosen target.
    let target = ecs.get::<Mining>(bot.entity).map(|m| (m.pos, m.dir));
    {
        let mut entity = ecs.entity_mut(bot.entity);
        entity.remove::<(LeftClickMine, MiningQueued, Mining)>();
        if let Some(mut p) = entity.get_mut::<MineProgress>() { p.0 = 0.0; }
        if let Some(mut t) = entity.get_mut::<MineTicks>() { t.0 = 0.0; }
    }
    if let Some((pos, direction)) = target {
        ecs.trigger(SendGamePacketEvent::new(bot.entity, ServerboundPlayerAction {
            action: Action::AbortDestroyBlock, pos, direction, seq: 0,
        }));
    }
}
pub fn stop(bot: &Client) {
    if bot.exists() {
        bot.walk(WalkDirection::None);
        bot.set_jumping(false);
        bot.set_crouching(false);
        release_mining(bot);
        player_action(bot, Action::ReleaseUseItem);
    }
}
pub fn apply(bot:&Client,a:&[usize],slot_count:usize){
    debug_assert_eq!(a.len(),8);
    let movement=match a[0]{0=>WalkDirection::None,1=>WalkDirection::Forward,2=>WalkDirection::Backward,3=>WalkDirection::Left,4=>WalkDirection::Right,5=>WalkDirection::ForwardLeft,6=>WalkDirection::ForwardRight,7=>WalkDirection::BackwardLeft,_=>WalkDirection::BackwardRight};
    bot.walk(movement);
    bot.set_jumping(a[3]==1);bot.set_crouching(a[3]==2);
    if a[5]>0 {bot.set_selected_hotbar_slot((a[5]-1) as u8);}
    // Held left mouse respects crosshair and reach; never force-mine a chosen block.
    if a[4] == 1 { bot.left_click_mine(true); } else { release_mining(bot); }
    match a[4]{
        2=>bot.start_use_item(),
        3=>player_action(bot,Action::ReleaseUseItem),
        4=>{let hit=bot.hit_result();if let Some(hit)=hit.as_entity_hit_result(){bot.attack(hit.entity);}},
        5=>player_action(bot,Action::DropItem),
        _=>{}
    }
    let gui=bot.get_inventory();let slot=a[7];
    if slot<slot_count {match a[6]{1=>gui.left_click(slot),2=>gui.right_click(slot),3=>gui.shift_click(slot),4=>{
        // A literal outside left click drops the cursor stack; no gifting macro.
        gui.click(azalea_inventory::operations::PickupClick::Left{slot:None});
    },_=>{}}}
    if a[6]==5{gui.close();}
}
fn player_action(bot:&Client,action:Action){bot.write_packet(ServerboundPlayerAction{action,pos:BlockPos::new(0,0,0),direction:Direction::Down,seq:0});}

/// Telemetry, not a game decision. The vanilla statistics screen uses this
/// ordinary request; it grants no items, powers or privileged world access.
pub fn request_stats(bot:&Client){
    use azalea::protocol::packets::game::s_client_command::{ServerboundClientCommand,Action};
    bot.write_packet(ServerboundClientCommand{action:Action::RequestStats});
}

#[cfg(test)]
mod tests {
    use super::*;
    use azalea::mining::{LeftClickMine, Mining, MiningQueued, MineProgress, MineTicks};
    use std::sync::Arc;
    #[test]
    fn released_left_button_clears_active_and_queued_mining() {
        // Headless ECS input test, NOT a server or network integration test.
        let mut ecs = azalea::ecs::world::World::new();
        let pos = BlockPos::new(1, 64, 2);
        let entity = ecs.spawn((LeftClickMine,
            Mining { pos, dir: Direction::Down, force: true },
            MiningQueued { position: pos, direction: Direction::Down, force: true },
            MineProgress(0.7), MineTicks(5.0))).id();
        let shared = Arc::new(parking_lot::RwLock::new(ecs));
        let bot = Client::new(entity, shared.clone());
        release_mining(&bot);
        let ecs = shared.read();
        assert!(ecs.get::<LeftClickMine>(entity).is_none());
        assert!(ecs.get::<Mining>(entity).is_none());
        assert!(ecs.get::<MiningQueued>(entity).is_none());
        assert_eq!(ecs.get::<MineProgress>(entity).unwrap().0, 0.0);
        assert_eq!(ecs.get::<MineTicks>(entity).unwrap().0, 0.0);
    }
}

/// Zero-order-held angular velocity, applied once per client tick. This is an
/// input-device transformation, not a target tracker or navigation controller.
pub fn tick_camera(bot:&Client,a:&[usize]) {
    if !bot.exists() || a.len()!=8 {return;}
    let d=bot.direction();
    let yaw=(d.y_rot()+crate::learning::curriculum::YAW_RATES[a[1]]/20.+180.).rem_euclid(360.)-180.;
    let pitch=(d.x_rot()+crate::learning::curriculum::PITCH_RATES[a[2]]/20.).clamp(-89.9,89.9);
    bot.set_direction(yaw,pitch);
}
