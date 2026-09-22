//! Task contracts and evidence gates, not a Minecraft environment implementation.
//! Merely adding an enum/validator does not make a stage playable. The Java/Folia
//! adapter must supply real geometry, reset states and confirmed transaction data.
use crate::Result;

pub const TASK_COUNT:usize=18;
pub const ITEM_COUNT:usize=13;
pub const HEADS:[usize;8]=[9,7,5,3,6,10,6,90];
#[derive(Clone,Copy,Debug,PartialEq,Eq)]
#[repr(usize)]
pub enum Task {ForwardStop,TurnStop,AimHold,NavigateStop,StepOver,BreakLog,CollectLog,PlaceBlock,
    CraftPlanks,CraftSticks,CraftWorkbench,CraftWoodPick,MineCobblestone,CraftStonePick,SmeltIron,SupplyChest,BuildPlatform,LogToWorkbench}
pub const TASKS:[Task;TASK_COUNT]=[Task::ForwardStop,Task::TurnStop,Task::AimHold,Task::NavigateStop,Task::StepOver,
    Task::BreakLog,Task::CollectLog,Task::PlaceBlock,Task::CraftPlanks,Task::CraftSticks,Task::CraftWorkbench,
    Task::CraftWoodPick,Task::MineCobblestone,Task::CraftStonePick,Task::SmeltIron,Task::SupplyChest,Task::BuildPlatform,Task::LogToWorkbench];
pub const NAMES:[&str;TASK_COUNT]=["forward-stop","turn-and-stop","look-at-target","navigate-and-stop","step-over",
    "break-log","collect-log","place-block","craft-planks","craft-sticks","craft-workbench","craft-wooden-pickaxe",
    "mine-cobblestone","craft-stone-pickaxe","smelt-iron","supply-chest","build-three-block-platform","log-to-workbench"];
#[derive(Clone,Copy,Debug,PartialEq,Eq)]
#[repr(usize)]
pub enum Item {Log,Planks,Stick,Workbench,WoodPick,Cobblestone,StonePick,Coal,Furnace,RawIron,IronIngot,Dirt,Chest}
pub const ITEMS:[Item;ITEM_COUNT]=[Item::Log,Item::Planks,Item::Stick,Item::Workbench,Item::WoodPick,Item::Cobblestone,
    Item::StonePick,Item::Coal,Item::Furnace,Item::RawIron,Item::IronIngot,Item::Dirt,Item::Chest];
pub const MATERIAL_NAMES:[&str;ITEM_COUNT]=["OAK_LOG","OAK_PLANKS","STICK","CRAFTING_TABLE","WOODEN_PICKAXE","COBBLESTONE",
    "STONE_PICKAXE","COAL","FURNACE","RAW_IRON","IRON_INGOT","DIRT","CHEST"];
#[derive(Clone,Copy,Debug)]
pub struct TaskSpec {pub task:Task,pub name:&'static str,pub limit_ticks:u32,pub group:&'static str,pub furnished:&'static str}
impl Task {
    pub fn from_index(index:usize)->Result<Self>{TASKS.get(index).copied().ok_or("unknown task")}
    pub fn spec(self)->TaskSpec {
        let (ticks,group,furnished)=match self {
            Self::ForwardStop|Self::TurnStop|Self::AimHold|Self::NavigateStop=>(600,"motor","No items; enclosed room"),
            Self::StepOver=>(900,"motor","One-block obstacle"),
            Self::BreakLog=>(900,"resource","One oak log; breaking alone is the target"),
            Self::CollectLog=>(1200,"resource","One oak log; must break and collect the session-tagged drop"),
            Self::PlaceBlock=>(1200,"building","Planks in a randomized inventory/hotbar slot; one marked target cell"),
            Self::CraftPlanks=>(1600,"crafting","One raw log; personal crafting grid"),
            Self::CraftSticks=>(1600,"crafting","Two raw planks; personal crafting grid"),
            Self::CraftWorkbench=>(2000,"crafting","Four raw planks; personal crafting grid"),
            Self::CraftWoodPick=>(2400,"crafting","A fixed workbench; three planks and two sticks"),
            Self::MineCobblestone=>(1800,"resource","One stone target; wooden pickaxe in randomized slot"),
            Self::CraftStonePick=>(2400,"crafting","A fixed workbench; three cobblestone and two sticks"),
            Self::SmeltIron=>(3000,"processing","A fixed furnace; raw iron and coal, no iron ingot"),
            Self::SupplyChest=>(1800,"logistics","A fixed empty chest; four logs in randomized slots; not multi-agent cooperation"),
            Self::BuildPlatform=>(2000,"building","A bounded stock of planks; three marked cells; mistakes must reset"),
            Self::LogToWorkbench=>(3000,"composed","One log block; no initial items; collect, craft planks, craft workbench"),
        };
        TaskSpec{task:self,name:NAMES[self as usize],limit_ticks:ticks,group,furnished}
    }
    pub fn craft_output(self)->Option<(Item,u32)> {match self {
        Self::CraftPlanks=>Some((Item::Planks,4)),Self::CraftSticks=>Some((Item::Stick,4)),
        Self::CraftWorkbench|Self::LogToWorkbench=>Some((Item::Workbench,1)),Self::CraftWoodPick=>Some((Item::WoodPick,1)),
        Self::CraftStonePick=>Some((Item::StonePick,1)),_=>None}}
    /// Task-wide curriculum restrictions only. Runtime masks may additionally
    /// remove mechanically nonexistent GUI slots, never reveal the correct slot.
    pub fn mask(self)->Vec<Vec<bool>> {
        let neutral=[0,3,2,0,0,0,0,0];
        let mut m=HEADS.iter().enumerate().map(|(h,&n)|{let mut v=vec![false;n];v[neutral[h]]=true;v}).collect::<Vec<_>>();
        match self {
            Self::ForwardStop=>m[0][1]=true,
            Self::TurnStop=>{m[0][1]=true;m[1].fill(true);},
            Self::AimHold=>{m[1].fill(true);m[2].fill(true);},
            Self::NavigateStop=>{m[0].fill(true);m[1].fill(true);},
            Self::StepOver=>{m[0].fill(true);m[1].fill(true);m[3][1]=true;},
            Self::CraftPlanks|Self::CraftSticks|Self::CraftWorkbench=>{m[6].fill(true);m[7].fill(true);},
            _=>{
                m[0].fill(true);m[1].fill(true);m[2].fill(true);m[3].fill(true);m[5].fill(true);
                match self {
                    Self::BreakLog|Self::CollectLog|Self::MineCobblestone=>m[4][1]=true,
                    Self::PlaceBlock|Self::BuildPlatform=>{m[4][1]=true;m[4][2]=true;m[4][3]=true;},
                    _=>{m[4][1]=true;m[4][2]=true;m[4][3]=true;m[6].fill(true);m[7].fill(true);}
                }
            }
        }
        m
    }
}

#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub struct Session {pub run:u64,pub actor:u8,pub generation:u64,pub lesson:u64}
#[derive(Clone,Copy,Debug,Default,PartialEq,Eq)]
pub struct Counters {
    /// Confirmed target blocks removed, not merely BlockBreakEvent predicted.
    pub broken:u32,
    /// Confirmed, session-tagged target drops acquired by this actor.
    pub picked_up:u32,
    /// The goal item's committed vanilla craft statistic delta, not output preview.
    pub crafted:u32,
    pub placed:u32,
    /// Confirmed iron extraction attributed to this actor.
    pub smelted:u32,
    /// Actual designated chest stock increase following this actor's transfer.
    pub deposited:u32,
}
impl Counters {
    fn fields(self)->[u32;6]{[self.broken,self.picked_up,self.crafted,self.placed,self.smelted,self.deposited]}
    fn ge(self,other:Self)->bool{self.fields().iter().zip(other.fields()).all(|(a,b)|*a>=b)}
    fn delta(self,base:Self)->Self {Self{broken:self.broken-base.broken,picked_up:self.picked_up-base.picked_up,
        crafted:self.crafted-base.crafted,placed:self.placed-base.placed,smelted:self.smelted-base.smelted,deposited:self.deposited-base.deposited}}
}
#[derive(Clone,Debug)]
pub struct Evidence {
    pub session:Session,
    pub tick:u64,
    /// Real inventory plus cursor; excludes predicted crafting output slots.
    pub stock:[u32;ITEM_COUNT],
    pub counters:Counters,
    /// Read back actual occupied target cells, not historical place-event count.
    pub occupied_targets:u8,
    /// Motor tasks require a continuous hold established using elapsed ticks,
    /// position, velocity and angle. A single close-to-target frame is insufficient.
    pub motor_hold_ticks:u32,
}
#[derive(Clone,Debug)]
pub struct ProgressGate {session:Session,task:Task,baseline:Counters,last:Counters,last_tick:u64}
impl ProgressGate {
    pub fn new(task:Task,baseline:&Evidence)->Result<Self>{
        if baseline.session.actor>=32 || baseline.stock.iter().any(|x|*x>5760){return Err("invalid evidence baseline");}
        Ok(Self{session:baseline.session,task,baseline:baseline.counters,last:baseline.counters,last_tick:baseline.tick})
    }
    /// Trust only the authenticated, region-safe bridge adapter to construct
    /// evidence. This validates context and temporal consistency, not network auth.
    pub fn observe(&mut self,e:&Evidence)->Result<bool>{
        if e.session!=self.session {return Err("foreign or stale task session");}
        if e.tick<=self.last_tick {return Err("nonmonotonic evidence tick");}
        if e.stock.iter().any(|x|*x>5760) || !e.counters.ge(self.last){return Err("invalid evidence counters");}
        let d=e.counters.delta(self.baseline);
        let have=|item:Item,n:u32|e.stock[item as usize]>=n;
        let ok=match self.task {
            Task::ForwardStop|Task::TurnStop|Task::AimHold|Task::NavigateStop|Task::StepOver=>e.motor_hold_ticks>=20,
            Task::BreakLog=>d.broken>=1,
            Task::CollectLog=>d.broken>=1 && d.picked_up>=1 && have(Item::Log,1),
            Task::PlaceBlock=>d.placed>=1 && e.occupied_targets&1==1,
            Task::MineCobblestone=>d.broken>=1 && d.picked_up>=1 && have(Item::Cobblestone,1),
            Task::SmeltIron=>d.smelted>=1 && have(Item::IronIngot,1),
            Task::SupplyChest=>d.deposited>=1,
            Task::BuildPlatform=>d.placed>=3 && e.occupied_targets&7==7,
            Task::LogToWorkbench=>d.broken>=1 && d.picked_up>=1 && d.crafted>=1 && have(Item::Workbench,1),
            task=>{let (item,n)=task.craft_output().ok_or("task has no success rule")?;d.crafted>=n && have(item,n)}
        };
        self.last=e.counters;self.last_tick=e.tick;Ok(ok)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn e()->Evidence{Evidence{session:Session{run:1,actor:0,generation:1,lesson:1},tick:0,stock:[0;ITEM_COUNT],counters:Counters::default(),occupied_targets:0,motor_hold_ticks:0}}
    #[test] fn all_tasks_have_bounded_specs_and_valid_masks(){
        for (i,task) in TASKS.iter().enumerate(){let s=task.spec();assert_eq!(Task::from_index(i).unwrap(),*task);assert!(!s.name.is_empty());assert!(s.limit_ticks>=600 && s.limit_ticks<=3000);
            let m=task.mask();assert_eq!(m.len(),HEADS.len());for (h,head) in m.iter().enumerate(){assert_eq!(head.len(),HEADS[h]);assert!(head.iter().any(|x|*x));}}
        assert!(Task::from_index(TASK_COUNT).is_err());
    }
    #[test] fn crafting_preview_or_seeded_stock_alone_is_not_success(){
        for task in [Task::CraftPlanks,Task::CraftSticks,Task::CraftWorkbench,Task::CraftWoodPick,Task::CraftStonePick]{
            let mut base=e();let (item,n)=task.craft_output().unwrap();base.stock[item as usize]=n;
            let mut gate=ProgressGate::new(task,&base).unwrap();let mut next=base.clone();next.tick=4;assert!(!gate.observe(&next).unwrap());
            next.tick=8;next.counters.crafted=n;assert!(gate.observe(&next).unwrap());
        }
    }
    #[test] fn crafted_stat_without_actual_acquisition_is_not_success(){
        let mut next=e();let mut gate=ProgressGate::new(Task::CraftWoodPick,&next).unwrap();
        next.tick=4;next.counters.crafted=1;assert!(!gate.observe(&next).unwrap());
        next.tick=8;next.stock[Item::WoodPick as usize]=1;assert!(gate.observe(&next).unwrap());
    }
    #[test] fn old_counters_are_not_new_progress(){
        let mut base=e();base.counters.broken=20;let mut gate=ProgressGate::new(Task::BreakLog,&base).unwrap();
        base.tick=4;assert!(!gate.observe(&base).unwrap());base.tick=8;base.counters.broken=21;assert!(gate.observe(&base).unwrap());
    }
    #[test] fn collection_requires_break_pickup_and_stock(){
        let mut x=e();let mut g=ProgressGate::new(Task::CollectLog,&x).unwrap();
        x.tick=4;x.stock[Item::Log as usize]=1;assert!(!g.observe(&x).unwrap());
        x.tick=8;x.counters.broken=1;assert!(!g.observe(&x).unwrap());
        x.tick=12;x.counters.picked_up=1;assert!(g.observe(&x).unwrap());
    }
    #[test] fn removed_platform_blocks_cannot_pass_on_event_history(){
        let mut x=e();let mut g=ProgressGate::new(Task::BuildPlatform,&x).unwrap();
        x.tick=4;x.counters.placed=3;x.occupied_targets=3;assert!(!g.observe(&x).unwrap());
        x.tick=8;x.occupied_targets=7;assert!(g.observe(&x).unwrap());
    }
    #[test] fn smelting_needs_extraction_and_acquisition(){
        let mut x=e();let mut g=ProgressGate::new(Task::SmeltIron,&x).unwrap();
        x.tick=4;x.stock[Item::IronIngot as usize]=1;assert!(!g.observe(&x).unwrap());
        x.tick=8;x.counters.smelted=1;assert!(g.observe(&x).unwrap());
    }
    #[test] fn composed_task_requires_the_whole_chain(){
        let mut x=e();let mut g=ProgressGate::new(Task::LogToWorkbench,&x).unwrap();
        x.tick=4;x.counters.crafted=1;x.stock[Item::Workbench as usize]=1;assert!(!g.observe(&x).unwrap());
        x.tick=8;x.counters.broken=1;x.counters.picked_up=1;assert!(g.observe(&x).unwrap());
    }
    #[test] fn stale_session_duplicate_tick_and_counter_reversal_fail_closed(){
        let mut x=e();let mut g=ProgressGate::new(Task::BreakLog,&x).unwrap();let before=g.clone();
        x.tick=4;x.session.generation+=1;assert!(g.observe(&x).is_err());assert_eq!(g.last_tick,before.last_tick);
        x.session=before.session;x.counters.broken=1;assert!(g.observe(&x).unwrap());assert!(g.observe(&x).is_err());
        x.tick=8;x.counters.broken=0;assert!(g.observe(&x).is_err());assert_eq!(g.last.broken,1);
    }
    #[test] fn motor_hold_needs_full_interval(){let mut x=e();let mut g=ProgressGate::new(Task::ForwardStop,&x).unwrap();
        x.tick=4;x.motor_hold_ticks=19;assert!(!g.observe(&x).unwrap());x.tick=8;x.motor_hold_ticks=20;assert!(g.observe(&x).unwrap());}
}
