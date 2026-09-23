//! EXPLICIT SCRIPTED DIAGNOSTICS, never the policy executable.
//! Compiled as a separate Azalea example only by test-live-fixtures.sh.
//! Uses the production literal input adapter and authoritative task gates.
//! No model is initialized, no demonstration is recorded, no training state is saved.
#[allow(dead_code)] #[path="../botsclustersmc/core/lib.rs"] mod learning;
#[path="../botsclustersmc/act.rs"] mod act;
use std::{collections::VecDeque,env,fs,path::PathBuf,sync::{Arc,Mutex,OnceLock,atomic::{AtomicBool,Ordering}},time::{Duration,Instant}};
use azalea::{Client,Event,ecs::component::Component,account::Account,bot::DefaultBotPlugins,DefaultPlugins,swarm::{SwarmBuilder,DefaultSwarmPlugins},app::PluginGroup};
use azalea::registry::builtin::ItemKind;
use azalea_inventory::{Menu,ItemStack};
use learning::{curriculum::{Lesson,Frame,Episode,IDLE,angles},next::{curriculum::{Lesson as Choice,PolicyId},tasks::{Session,Evidence,Counters,TASKS,ITEM_COUNT}}};
static DONE:AtomicBool=AtomicBool::new(false);
static ERROR:OnceLock<Mutex<Option<String>>>=OnceLock::new();
static ROOT:OnceLock<PathBuf>=OnceLock::new();
static RUN:OnceLock<String>=OnceLock::new();
fn root()->&'static PathBuf{ROOT.get().unwrap()}
fn fail(e:String){eprintln!("DIAGNOSTIC FAILURE: {e}");*ERROR.get().unwrap().lock().unwrap()=Some(e);DONE.store(true,Ordering::Relaxed);}
fn carried(bot:&Client)->ItemStack{let v=bot.component::<azalea::entity::inventory::Inventory>();v.carried.clone()}
fn player_range(menu:&Menu)->std::ops::Range<usize>{if matches!(menu,Menu::Player(_)){9..45}else{menu.slots().len()-36..menu.slots().len()}}
#[derive(Clone,Debug)] enum Op{Take(ItemKind),Left(usize),Right(usize),Park,Align([f64;3]),Use,Menu(u8),Output(usize,ItemKind),Close,Select,Wait(u8)}
fn recipe(ops:&mut VecDeque<Op>,items:&[(ItemKind,usize)],output:ItemKind){for &(kind,slot) in items{ops.extend([Op::Take(kind),Op::Right(slot),Op::Park]);}ops.extend([Op::Output(0,output),Op::Park]);}
fn operations(l:&Lesson)->VecDeque<Op>{
    use ItemKind::*;let mut q=VecDeque::new();
    match l.stage{
        7|16=>{q.extend([Op::Take(OakPlanks),Op::Left(36),Op::Select]);for i in 0..if l.stage==16{3}else{1}{q.extend([Op::Align([l.goal[0]+i as f64,96.99,l.goal[2]]),Op::Wait(2),Op::Use,Op::Wait(3)]);}},
        8=>recipe(&mut q,&[(OakLog,1)],OakPlanks),
        9=>recipe(&mut q,&[(OakPlanks,1),(OakPlanks,3)],Stick),
        10=>recipe(&mut q,&[(OakPlanks,1),(OakPlanks,2),(OakPlanks,3),(OakPlanks,4)],CraftingTable),
        11|13=>{let material=if l.stage==11{OakPlanks}else{Cobblestone};let output=if l.stage==11{WoodenPickaxe}else{StonePickaxe};q.extend([Op::Align(l.goal),Op::Wait(2),Op::Use,Op::Menu(1)]);recipe(&mut q,&[(material,1),(material,2),(material,3),(Stick,5),(Stick,8)],output);},
        12=>q.extend([Op::Take(WoodenPickaxe),Op::Left(36),Op::Select]),
        14=>{q.extend([Op::Align(l.goal),Op::Wait(2),Op::Use,Op::Menu(2),Op::Take(RawIron),Op::Right(0),Op::Park,Op::Take(Coal),Op::Right(1),Op::Park,Op::Output(2,IronIngot)]);},
        15=>q.extend([Op::Align(l.goal),Op::Wait(2),Op::Use,Op::Menu(3),Op::Take(OakLog),Op::Left(0)]),
        _=>{}
    }q
}
fn angular(error:f32,pitch:bool)->usize{if pitch{if error.abs()<2. {2}else if error< -20.{0}else if error<0.{1}else if error>20.{4}else{3}}else{if error.abs()<2.{3}else if error< -50.{0}else if error< -15.{1}else if error<0.{2}else if error>50.{6}else if error>15.{5}else{4}}}
fn aim(f:&Frame,g:[f64;3],pitch:bool,a:&mut[usize;8])->bool{let e=angles(f.position,f.yaw,f.pitch,g);a[1]=angular(e[0],false);if pitch{a[2]=angular(e[1],true);}e[0].abs()<3.&&(!pitch||e[1].abs()<3.)}
fn run_op(bot:&Client,f:&Frame,op:&mut Op,a:&mut[usize;8])->Result<bool,String>{
    let menu=bot.menu();let slots=menu.slots();
    match op{
        Op::Take(kind)=>{if !carried(bot).is_empty(){return Err("test attempted to acquire an ingredient with a nonempty cursor".into());}let index=player_range(&menu).find(|&i|slots[i].kind()==*kind&&!slots[i].is_empty()).ok_or_else(||format!("missing raw test ingredient {kind:?}; menu={menu:?}"))?;a[6]=1;a[7]=index;},
        Op::Left(slot)=>{a[6]=1;a[7]=*slot;},Op::Right(slot)=>{a[6]=2;a[7]=*slot;},
        Op::Park=>{if !carried(bot).is_empty(){let index=player_range(&menu).find(|&i|slots[i].is_empty()).ok_or("no empty diagnostic inventory slot")?;a[6]=1;a[7]=index;}},
        Op::Align(g)=>{if !aim(f,*g,true,a){return Ok(false);}},
        Op::Use=>a[4]=2,
        Op::Menu(expected)=>{let ok=match expected{1=>matches!(menu,Menu::Crafting{..}),2=>matches!(menu,Menu::Furnace{..}),3=>matches!(menu,Menu::Generic9x3{..}),_=>false};if !ok{return Ok(false);}},
        Op::Output(slot,kind)=>{if slots.get(*slot).is_none_or(|i|i.is_empty()||i.kind()!=*kind){return Ok(false);}a[6]=1;a[7]=*slot;},
        Op::Close=>a[6]=5,Op::Select=>a[5]=1,
        Op::Wait(n)=>{if *n>0{*n-=1;return Ok(false);}}
    }Ok(true)
}
fn read_frame(l:&Lesson)->Result<Option<Frame>,String>{
    let path=root().join(".runtime/lab/frame-0.txt");let text=match fs::read_to_string(path){Ok(x)=>x,Err(e)if e.kind()==std::io::ErrorKind::NotFound=>return Ok(None),Err(e)=>return Err(e.to_string())};
    let f:Vec<_>=text.split_whitespace().collect();if f.len()!=32||f[0]!="BCMCLAB3"{return Err("malformed diagnostic frame".into());}
    if f[1]!=RUN.get().unwrap()||f[2]!=l.token(){return Ok(None);}
    let number=|i:usize|f[i].parse::<f64>().map_err(|e|e.to_string());let count=|i:usize|f[i].parse::<u32>().map_err(|e|e.to_string());
    if count(3)?!=0{return Err("wrong diagnostic actor".into());}let tick=f[4].parse::<u64>().map_err(|e|e.to_string())?;let mut stock=[0;ITEM_COUNT];for i in 0..ITEM_COUNT{stock[i]=count(11+i)?;}
    Ok(Some(Frame{tick,position:[number(5)?,number(6)?,number(7)?],yaw:number(8)? as f32,pitch:number(9)? as f32,grounded:f[10]=="true",evidence:Evidence{session:l.choice.session,tick,stock,counters:Counters{broken:count(24)?,picked_up:count(25)?,crafted:count(26)?,placed:count(27)?,smelted:count(28)?,deposited:count(29)?},target_stock:count(30)?,occupied_targets:count(31)? as u8,motor_hold_ticks:0}}))
}
struct Check{ticks:u64,ready:bool,stage:usize,lesson:Option<Lesson>,episode:Option<Episode>,last:[usize;8],ops:VecDeque<Op>,chain:bool,started:Instant,observer:bool,view12:bool,view16:bool}
impl Check{
    fn new(observer:bool)->Self{Self{ticks:0,ready:false,stage:0,lesson:None,episode:None,last:IDLE,ops:VecDeque::new(),chain:false,started:Instant::now(),observer,view12:false,view16:false}}
    fn begin(&mut self)->Result<(),String>{
        let choice=Choice{session:Session{run:11,actor:0,generation:1,lesson:self.stage as u64+1},task:TASKS[self.stage],difficulty:1.,seed:8129+self.stage as u64,full_probe:true,evaluation:false,review:false,policy:PolicyId{version:0,signature:0}};
        let l=Lesson::from_choice(choice);let text=format!("BCMCLAB3 {} {} 0 {} {:.8} {:.8} {:.8} {:.5} {:.5} {:.8} {:.8} {:.8} {} 1.0 true\n",RUN.get().unwrap(),l.token(),l.stage,l.start[0],l.start[1],l.start[2],l.yaw,l.pitch,l.goal[0],l.goal[1],l.goal[2],l.choice.seed);
        learning::checkpoint::atomic_write(&root().join(".runtime/lab/request-0.txt"),text.as_bytes(),0).map_err(|e|e.to_string())?;
        eprintln!("DIAGNOSTIC BEGIN stage={} name={} full=true start={:?} goal={:?}",l.stage,l.choice.task.spec().name,l.start,l.goal);
        self.ops=operations(&l);self.lesson=Some(l);self.episode=None;self.chain=false;self.last=IDLE;self.started=Instant::now();Ok(())
    }
    fn fixture(&mut self,bot:&Client)->Result<(),String>{
        if root().join("state/training.bcmc").exists(){return Err("diagnostic refuses to run in a learned-model directory".into());}
        if self.lesson.is_none(){self.begin()?;act::stop(bot);return Ok(());}
        let l=self.lesson.as_ref().unwrap().clone();
        if self.started.elapsed()>Duration::from_secs(200){return Err(format!("fixture {} timed out; pending operation {:?}",self.stage,self.ops.front()));}
        let Some(f)=read_frame(&l)? else{return Ok(());};
        if let Some(e)=&self.episode{if f.tick<=e.previous.tick{return Ok(());}}
        if self.episode.is_none(){let p=bot.position();if(p.x-f.position[0]).abs()>0.5||(p.y-f.position[1]).abs()>0.5||(p.z-f.position[2]).abs()>0.5{return Ok(());}self.episode=Some(Episode::new(&l,f.clone())?);}
        else{
            let outcome=self.episode.as_mut().unwrap().step(0,&l,f.clone(),&self.last,0.997)?;
            if outcome.done{
                act::stop(bot);self.last=IDLE;
                if !outcome.success{return Err(format!("fixture {} failed {}: frame={f:?} operation={:?} menu={:?}",self.stage,outcome.reason,self.ops.front(),bot.menu()));}
                eprintln!("DIAGNOSTIC PASS stage={} ticks={} counters={:?} stock={:?} target_stock={} occupancy={}",self.stage,f.tick,f.evidence.counters,f.evidence.stock,f.evidence.target_stock,f.evidence.occupied_targets);
                self.stage+=1;self.lesson=None;self.episode=None;
                if self.stage==18{eprintln!("PASS: all 18 fixtures reached through scripted literal client inputs; NOT learned behavior");DONE.store(true,Ordering::Relaxed);}return Ok(());
            }
        }
        let mut a=IDLE;
        if let Some(op)=self.ops.front_mut(){if run_op(bot,&f,op,&mut a)?{self.ops.pop_front();}}
        else if self.stage<=4{
            if self.stage==2{aim(&f,l.goal,true,&mut a);}
            else if self.stage==0{if l.goal[2]-f.position[2]>0.55{a[0]=1;}}
            else{
                let dist=(f.position[0]-l.goal[0]).hypot(f.position[2]-l.goal[2]);
                if dist>0.55{if aim(&f,l.goal,false,&mut a){a[0]=1;}if self.stage==4{a[3]=1;}}
            }
        }else if [5,6,12,17].contains(&self.stage){
            let dist=(f.position[0]-l.goal[0]).hypot(f.position[2]-l.goal[2]);
            if f.evidence.counters.broken==0{if aim(&f,l.goal,true,&mut a){if dist>2.2{a[0]=1;}else{a[4]=1;}}}
            else if [6,12,17].contains(&self.stage)&&f.evidence.counters.picked_up==0{if aim(&f,l.goal,false,&mut a)&&dist>0.25{a[0]=1;}}
            else if self.stage==17&&!self.chain{self.chain=true;recipe(&mut self.ops,&[(ItemKind::OakLog,1)],ItemKind::OakPlanks);recipe(&mut self.ops,&[(ItemKind::OakPlanks,1),(ItemKind::OakPlanks,2),(ItemKind::OakPlanks,3),(ItemKind::OakPlanks,4)],ItemKind::CraftingTable);}
        }
        act::apply(bot,&a,bot.menu().slots().len());self.last=a;Ok(())
    }
    fn observer(&mut self,bot:&Client)->Result<(),String>{
        let t=self.ticks;let p=bot.position();
        match t{
            40=>bot.chat("/academy view 12"),80=>bot.chat("/academy watch 63"),
            160=>{if(p.x-120.).abs()>1.||(p.z-120.).abs()>1.||(p.y-109.).abs()>1.{return Err(format!("observer watch63 position: {p:?}"));}bot.chat("/academy next");},
            220=>{if(p.x-8.).abs()>1.||(p.z-8.).abs()>1.{return Err(format!("observer next/wrap position: {p:?}"));}bot.chat("/academy prev");},
            280=>{if(p.x-120.).abs()>1.||(p.z-120.).abs()>1.{return Err("observer previous/wrap failed".into());}bot.chat("/academy overview");},
            340=>{if(p.x-64.).abs()>1.||(p.z-64.).abs()>1.||(p.y-185.).abs()>1.{return Err(format!("observer overview position: {p:?}"));}bot.chat("/academy");},
            400=>{if !matches!(bot.menu(),Menu::Generic9x6{..}){return Err(format!("observer menu did not open: {:?}",bot.menu()));}bot.get_inventory().left_click(53usize);},
            440=>{let menu=bot.menu();let slots=menu.slots();if !matches!(menu,Menu::Generic9x6{..})||slots[18].is_empty()||!slots[19].is_empty(){return Err("observer page2 does not contain exactly the last 19 bots".into());}bot.get_inventory().left_click(18usize);},
            500=>{if(p.x-120.).abs()>1.||(p.z-120.).abs()>1.{return Err("observer menu click did not select bot63".into());}bot.chat("/academy view 16");},
            560=>{if !self.view12||!self.view16{return Err(format!("observer did not receive requested view radius packets: 12={} 16={}",self.view12,self.view16));}bot.chat("/academy tour");},
            840=>{if(p.x-8.).abs()>1.||(p.z-8.).abs()>1.{return Err(format!("observer automatic tour did not advance: {p:?}"));}bot.chat("/academy tour");eprintln!("PASS: real observer menu, page2, click, watch63, next/previous wrap, overview, tour and 12/16-chunk radius packets");DONE.store(true,Ordering::Relaxed);},
            _=>{}
        }Ok(())
    }
}
#[derive(Component,Clone)]struct State(Arc<Mutex<Check>>);
impl Default for State{fn default()->Self{Self(Arc::new(Mutex::new(Check::new(false))))}}
async fn handler(bot:Client,event:Event,state:State)->anyhow::Result<()>{
    let result=(||->Result<(),String>{let mut s=state.0.lock().unwrap();match event{
        Event::Spawn=>{bot.set_client_information(azalea::ClientInformation{view_distance:if s.observer{16}else{3},..Default::default()});s.ready=true;s.ticks=0;},
        Event::Death(_)=>return Err("diagnostic client died".into()),
        Event::Disconnect(reason)=>{if !DONE.load(Ordering::Relaxed){return Err(format!("diagnostic disconnected: {reason:?}"));}},
        Event::ConnectionFailed(e)=>return Err(format!("diagnostic connection failed: {e:?}")),
        Event::Packet(packet)=>{if s.observer{if let azalea::protocol::packets::game::ClientboundGamePacket::SetChunkCacheRadius(p)=packet.as_ref(){eprintln!("OBSERVER cache radius={}",p.radius);s.view12|=p.radius==12;s.view16|=p.radius==16;}}},
        Event::Tick=>{if DONE.load(Ordering::Relaxed){bot.exit();return Ok(());}if s.ready&&bot.exists(){s.ticks+=1;if s.observer{s.observer(&bot)?;}else{if s.ticks%4==0&&s.ticks>12{s.fixture(&bot)?;}act::tick_camera(&bot,&s.last);}}},_=>{}
    }Ok(())})();if let Err(e)=result{fail(e);}Ok(())
}
fn main()->Result<(),Box<dyn std::error::Error>>{
    let root_path=PathBuf::from(env::var("BCMC_ROOT")?);let observer=env::var("BCMC_TEST_MODE").as_deref()==Ok("observer");
    if !observer&&(!root_path.join(".bcmc-diagnostic-only").is_file()||root_path.join("state/training.bcmc").exists()){return Err("fixture diagnostics require their own explicitly marked, untrained directory".into());}
    ROOT.set(root_path).unwrap();RUN.set(env::var("BCMC_RUN_ID")?).unwrap();ERROR.set(Mutex::new(None)).unwrap();
    let server=env::var("BOT_SERVER")?;let name=if observer{"bcmcObserver"}else{"bcmc00"};let state=State(Arc::new(Mutex::new(Check::new(observer))));
    let executor=tokio::runtime::Builder::new_multi_thread().worker_threads(2).enable_all().build()?;
    executor.block_on(async{
        let local=tokio::task::LocalSet::new();local.run_until(async{
            let builder=SwarmBuilder::new_without_plugins().add_plugins((DefaultPlugins,DefaultBotPlugins.build().disable::<azalea::pathfinder::PathfinderPlugin>().disable::<azalea::accept_resource_packs::AcceptResourcePacksPlugin>(),DefaultSwarmPlugins)).set_handler(handler).reconnect_after(None).add_account_with_state(Account::offline(name),state);
            let run=builder.start(server.as_str());tokio::pin!(run);
            loop{tokio::select!{_=&mut run=>{if !DONE.load(Ordering::Relaxed){fail("diagnostic ECS exited early".into());}break;},_=tokio::time::sleep(Duration::from_millis(100))=>{if DONE.load(Ordering::Relaxed){break;}}}}
        }).await;
    });
    if let Some(e)=ERROR.get().unwrap().lock().unwrap().take(){Err(e.into())}else{Ok(())}
}
