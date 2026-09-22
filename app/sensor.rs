//! Observations use the local client view. Progress rewards use ONLY vanilla
//! server statistics, never a predicted or partially synchronized inventory.
use std::collections::BTreeMap;
use azalea::{Client,core::position::BlockPos,protocol::packets::game::ClientboundGamePacket};
use azalea_inventory::ItemStack;
use azalea::protocol::packets::game::c_award_stats::Stat;
use azalea::physics::collision::BlockWithShape;
use crate::{learning::{FRAME,OBS,ACTIONS,HEADS},metrics::{hash,now,AgentView}};

pub struct Confirmed {
    owned:Vec<ItemStack>, window:i32, window_len:usize,
    pub health:Option<f32>, pub lost_health:f32, progress:BTreeMap<u32,u32>,
}
impl Default for Confirmed {
    fn default()->Self {Self{owned:vec![ItemStack::Empty;46],window:0,window_len:46,health:None,lost_health:0.0,progress:BTreeMap::new()}}
}
impl Confirmed {
    pub fn packet(&mut self,p:&ClientboundGamePacket){match p {
        ClientboundGamePacket::AwardStats(p)=>{
            for (stat,&count) in &p.stats {
                // Counts are cumulative, authoritative outcomes. Pickup/drop,
                // used-item, distance, idle time and mere requests earn nothing.
                if matches!(stat,Stat::Mined(_)|Stat::Crafted(_))&&count>=0{
                    let key=(hash(&format!("{stat:?}")) as u32).max(1);
                    let entry=self.progress.entry(key).or_default();*entry=(*entry).max(count as u32);
                }
            }
        },
        ClientboundGamePacket::ContainerSetContent(p)=>{
            self.window=p.container_id;self.window_len=p.items.len();
            for (i,item) in p.items.iter().enumerate(){self.set_window_slot(p.container_id,i,item.clone());}
        },
        ClientboundGamePacket::ContainerSetSlot(p)=>{
            self.set_window_slot(p.container_id,p.slot as usize,p.item_stack.clone());
        },
        ClientboundGamePacket::SetPlayerInventory(p)=>{
            let raw=p.slot as usize;
            let mapped=match raw{0..=8=>Some(raw+36),9..=35=>Some(raw),36..=39=>Some(44-raw),40=>Some(45),_=>None};
            if let Some(i)=mapped{self.owned[i]=p.contents.clone();}
        },
        ClientboundGamePacket::SetHealth(p)=>{
            if p.health.is_finite(){if let Some(old)=self.health{self.lost_health+=(old-p.health).max(0.0);}self.health=Some(p.health);}
        },_=>{}
    }}
    fn set_window_slot(&mut self,window:i32,slot:usize,item:ItemStack){
        let target=if window==0 {Some(slot)}
        else if window==self.window && self.window_len>=36 && slot>=self.window_len-36 && slot<self.window_len {
            // Standard player portion of a vanilla container is main inventory,
            // then hotbar. The container's own slots are not possessions.
            Some(slot-(self.window_len-36)+9)
        } else {None};
        if let Some(i)=target{if i<46{self.owned[i]=item;}}
    }
    pub fn progress(&self)->Vec<(u32,u32)>{self.progress.iter().map(|(&k,&v)|(k,v)).collect()}
    /// Diagnostic only: incremental inventory notifications are not a complete
    /// server-authoritative snapshot after client-predicted GUI operations.
    pub fn inventory(&self)->Vec<(u32,u32)>{
        let mut map=BTreeMap::<u32,u32>::new();
        // Player slot zero is a crafting OUTPUT preview, not acquired property.
        for item in &self.owned[1..]{if !item.is_empty(){let id=item_id(item);let count=item.count().max(0) as u32;*map.entry(id).or_default()+=count;}}
        map.into_iter().collect()
    }
    pub fn damage(&mut self)->f32{std::mem::take(&mut self.lost_health)}
}
fn item_id(i:&ItemStack)->u32{(hash(&format!("{:?}",i.kind())) as u32).max(1)}
fn features(h:u64)->[f32;2]{[(h as u16 as f32/32767.5)-1.0,((h>>16) as u16 as f32/32767.5)-1.0]}
fn stack(i:&ItemStack,v:&mut Vec<f32>){if i.is_empty(){v.extend([0.0;3]);}else{v.extend(features(hash(&format!("{:?}",i.kind()))));v.push((i.count().max(0) as f32/64.0).min(1.0));}}

pub struct Observation {pub frame:Vec<f32>,pub obs:Vec<f32>,pub mask:Vec<bool>,pub slots:usize,pub view:AgentView}
pub fn observe(bot:&Client,id:usize,name:&str,previous:&[f32],last_action:&[usize],old_position:Option<[f64;3]>,peers:&[AgentView])->Observation{
    let p=bot.position();let position=[p.x,p.y,p.z];let dir=bot.direction();let hunger=bot.hunger();
    let dim=hash(&format!("{:?}",bot.world_name()));let time=now();let extent=crate::engine::runtime().cfg.radius;let dim_features=features(dim);
    let mut f=Vec::<f32>::with_capacity(FRAME);
    let yaw=dir.y_rot().to_radians();let pitch=dir.x_rot().to_radians();
    let delta=old_position.map(|o|[(p.x-o[0]) as f32,(p.y-o[1]) as f32,(p.z-o[2]) as f32]).unwrap_or([0.0;3]);
    let menu=bot.menu();let slots=menu.slots();let n=slots.len().min(90);
    let hit=bot.hit_result();let block_hit=hit.as_block_hit_result_if_not_miss();
    f.extend([bot.health()/20.0,hunger.food as f32/20.0,hunger.saturation/20.0,yaw.sin(),yaw.cos(),pitch.sin(),pitch.cos(),(p.y as f32/320.0).clamp(-1.0,1.0),delta[0].clamp(-8.0,8.0)/8.0,delta[1].clamp(-8.0,8.0)/8.0,delta[2].clamp(-8.0,8.0)/8.0,bot.selected_hotbar_slot() as f32/8.0,if slots.len()!=46{1.0}else{0.0},if block_hit.is_some(){1.0}else{0.0},if hit.as_entity_hit_result().is_some(){1.0}else{0.0},(p.x as f32/extent).clamp(-1.0,1.0),(p.z as f32/extent).clamp(-1.0,1.0),dim_features[0],dim_features[1],1.0]);
    // A small local state-based voxel sensor, NOT a global map or path planner.
    // Stable hashed categories avoid an enormous one-hot block vocabulary.
    let world=bot.world();let world=world.read();
    for y in -1..=1 {for z in -2..=2 {for x in -2..=2 {
        let pos=BlockPos::new(p.x.floor() as i32+x,p.y.floor() as i32+y,p.z.floor() as i32+z);
        if let Some(b)=world.get_block_state(pos){
            let label=format!("{b:?}");let h=hash(&label);f.push(1.0);f.extend(features(h));f.push(if b.is_collision_shape_empty(){0.0}else{1.0});
        }else{f.extend([0.0;4]);}
    }}}
    drop(world);
    for i in 0..90{if let Some(item)=slots.get(i){stack(item,&mut f);}else{f.extend([0.0;3]);}}
    // component() returns an ECS read guard. Drop it BEFORE invoking any other
    // Client method: recursive reads can deadlock behind a queued ECS writer.
    let carried = {
        let inventory = bot.component::<azalea::entity::inventory::Inventory>();
        inventory.carried.clone()
    };
    stack(&carried, &mut f);
    let mut nearby=peers.iter().filter(|other|other.id!=id&&other.online&&other.dimension==dim&&time.saturating_sub(other.last_seen)<=5)
        .map(|other|{let d=(other.position[0]-p.x).powi(2)+(other.position[1]-p.y).powi(2)+(other.position[2]-p.z).powi(2);(d,other)})
        .filter(|(d,_)|*d<=32.0*32.0).collect::<Vec<_>>();
    nearby.sort_by(|a,b|a.0.total_cmp(&b.0));
    for i in 0..4{if let Some((_,other))=nearby.get(i){
        f.extend([1.0,((other.position[0]-p.x)/32.0) as f32,((other.position[1]-p.y)/32.0) as f32,((other.position[2]-p.z)/32.0) as f32,other.health/20.0,other.id as f32/31.0]);
    }else{f.extend([0.0;6]);}}
    assert!(f.len()<=FRAME);f.resize(FRAME,0.0);
    for v in &mut f{if !v.is_finite(){*v=0.0;}*v=v.clamp(-1.0,1.0);}
    let mut obs=f.clone();if previous.len()==FRAME{obs.extend_from_slice(previous);}else{obs.extend(std::iter::repeat_n(0.0,FRAME));}
    for (i,&n) in HEADS.iter().enumerate(){obs.push(*last_action.get(i).unwrap_or(&0) as f32/(n-1).max(1) as f32);}
    let identity=id as f32*0.618_034;obs.extend([identity.sin(),identity.cos(),(identity*3.0).sin(),(identity*3.0).cos()]);assert_eq!(obs.len(),OBS);
    let mut mask=vec![true;ACTIONS];let offset=ACTIONS-90;
    for i in 0..90{mask[offset+i]=i<n.max(1);}
    // All goals remain policy-controlled. Only mechanically invalid slot indices
    // are masked; no "smart" masks suggest a recipe, block, direction or task.
    Observation{frame:f,obs,mask,slots:n,view:AgentView{id,name:name.into(),online:true,dimension:dim,position,health:bot.health(),food:hunger.food,last_seen:time,..Default::default()}}
}

#[cfg(test)]
mod tests {
    use super::*;
    use azalea::registry::builtin::{BlockKind,ItemKind};
    use azalea::protocol::packets::game::c_award_stats::ClientboundAwardStats;
    #[test]
    fn only_mined_and_crafted_server_stats_can_be_progress() {
        let mut c=Confirmed::default();
        let packet=ClientboundGamePacket::AwardStats(ClientboundAwardStats{stats:[
            (Stat::Mined(BlockKind::Stone),3),(Stat::Crafted(ItemKind::Stick),4),
            (Stat::PickedUp(ItemKind::Diamond),100),(Stat::Dropped(ItemKind::Diamond),100),
            (Stat::Used(ItemKind::Diamond),100)].into_iter().collect()});
        c.packet(&packet);let initial=c.progress();
        assert_eq!(initial.len(),2);c.packet(&packet);assert_eq!(initial,c.progress());
    }
    #[test]
    fn chest_contents_and_crafting_preview_are_not_possessions() {
        let mut c=Confirmed::default();c.window=1;c.window_len=63;
        c.set_window_slot(1,0,ItemStack::new(ItemKind::Diamond,64));
        c.set_window_slot(0,0,ItemStack::new(ItemKind::Diamond,64));
        assert!(c.inventory().is_empty());
        c.set_window_slot(1,27,ItemStack::new(ItemKind::Stick,4));
        assert_eq!(c.inventory().iter().map(|p|p.1).sum::<u32>(),4);
    }
}

/// The prior sensor occupies exactly 617 values; curriculum uses its 23 padded
/// values. The frame stack and recorded behavior distribution include the goal.
pub fn academy_features(o:&mut Observation,features:&[f32;23]) {
    o.frame[617..640].copy_from_slice(features);
    o.obs[..FRAME].copy_from_slice(&o.frame);
}
