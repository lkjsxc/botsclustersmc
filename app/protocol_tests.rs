//! Regression tests execute the real patched packet dispatcher, not a mock.
use azalea::{ecs::world::World,entity::inventory::Inventory,packet::game::process_packet};
use azalea::protocol::packets::game::{ClientboundGamePacket,
    c_set_cursor_item::ClientboundSetCursorItem,c_set_player_inventory::ClientboundSetPlayerInventory,
    c_container_set_content::ClientboundContainerSetContent,c_container_set_slot::ClientboundContainerSetSlot};
use azalea::registry::builtin::ItemKind;
use azalea_inventory::ItemStack;

#[test]
fn server_cursor_correction_clears_predicted_crafted_output(){
    let mut world=World::new();let mut inventory=Inventory::default();
    inventory.carried=ItemStack::new(ItemKind::OakPlanks,4);
    let player=world.spawn(inventory).id();
    process_packet(&mut world,player,&ClientboundGamePacket::SetCursorItem(ClientboundSetCursorItem{contents:ItemStack::Empty}));
    assert!(world.get::<Inventory>(player).unwrap().carried.is_empty());
}
#[test]
fn complete_snapshot_acknowledges_cursor_and_click_state(){
    let mut world=World::new();let player=world.spawn(Inventory::default()).id();
    process_packet(&mut world,player,&ClientboundGamePacket::ContainerSetContent(ClientboundContainerSetContent{
        container_id:0,state_id:37,items:vec![ItemStack::Empty;46],carried_item:ItemStack::new(ItemKind::Stick,4)}));
    let inventory=world.get::<Inventory>(player).unwrap();
    assert_eq!(inventory.state_id,37);assert_eq!(inventory.carried.kind(),ItemKind::Stick);assert_eq!(inventory.carried.count(),4);
}
#[test]
fn player_inventory_indices_are_not_menu_slot_indices(){
    let mut world=World::new();let player=world.spawn(Inventory::default()).id();
    for(raw,slot)in[(0,36),(8,44),(9,9),(35,35),(36,8),(39,5),(40,45)]{
        process_packet(&mut world,player,&ClientboundGamePacket::SetPlayerInventory(ClientboundSetPlayerInventory{slot:raw,contents:ItemStack::new(ItemKind::WoodenPickaxe,1)}));
        assert_eq!(world.get::<Inventory>(player).unwrap().inventory_menu.slot(slot).unwrap().kind(),ItemKind::WoodenPickaxe);
    }
    assert!(world.get::<Inventory>(player).unwrap().inventory_menu.slot(0).unwrap().is_empty());
}
#[test]
fn hotbar_corrections_also_acknowledge_click_state(){
    let mut world=World::new();let player=world.spawn(Inventory::default()).id();
    process_packet(&mut world,player,&ClientboundGamePacket::ContainerSetSlot(ClientboundContainerSetSlot{
        container_id:0,state_id:91,slot:36,item_stack:ItemStack::new(ItemKind::OakLog,1)}));
    let inventory=world.get::<Inventory>(player).unwrap();
    assert_eq!(inventory.state_id,91);assert_eq!(inventory.inventory_menu.slot(36).unwrap().kind(),ItemKind::OakLog);
}
