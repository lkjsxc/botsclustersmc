use std::sync::{Arc,Mutex};
use azalea::{Client,Event,ecs::component::Component};
use crate::{act,academy::{AcademyAgent,Poll},engine::{self,runtime},learning::{network::Model,ppo::Transition,rng::Rng,curriculum::IDLE,FRAME},sensor::{self,Confirmed}};

struct Pending {obs:Vec<f32>,mask:Vec<bool>,actions:Vec<usize>,logp:f32,value:f32}
pub struct Agent {
    id:usize,name:String,ready:bool,ticks:u64,steps:u64,
    previous:Vec<f32>,last_action:Vec<usize>,position:Option<[f64;3]>,
    pending:Option<Pending>,trajectory:Vec<Transition>,policy:Option<Arc<Model>>,rng:Rng,
    confirmed:Confirmed,stats_packets:u64,spawns:u64,disconnects:u64,academy:AcademyAgent,
}
impl Default for Agent {
    fn default()->Self{Self{id:0,name:String::new(),ready:false,ticks:0,steps:0,previous:vec![0.;FRAME],last_action:IDLE.to_vec(),position:None,
        pending:None,trajectory:vec![],policy:None,rng:Rng(0),confirmed:Confirmed::default(),stats_packets:0,spawns:0,disconnects:0,academy:AcademyAgent::default()}}
}
#[derive(Component,Clone,Default)]pub struct State(Arc<Mutex<Agent>>);
impl State{pub fn new(id:usize,name:String,seed:u64)->Self{Self(Arc::new(Mutex::new(Agent{id,name,rng:Rng(seed.wrapping_add(id as u64*73_187)),..Agent::default()})))}}
impl Agent {
    fn clear_segment(&mut self){
        if !self.trajectory.is_empty(){runtime().training.lock().unwrap().dropped+=1;}
        self.pending=None;self.trajectory.clear();self.previous.fill(0.);self.position=None;self.last_action=IDLE.to_vec();self.policy=None;
    }
    fn lost_connection(&mut self){
        if !engine::stop_requested(){engine::fail(&format!("{} disconnected; cannot silently replace a missing cohort actor",self.name));}
        self.disconnects+=1;self.clear_segment();self.ready=false;self.academy.clear();self.confirmed=Confirmed::default();
        let mut peers=runtime().peers.lock().unwrap();peers[self.id].online=false;peers[self.id].disconnects=self.disconnects;
    }
    fn decision(&mut self,bot:&Client)->Result<(),String>{
        let rt=runtime();
        let f=match self.academy.poll(self.id)?{
            Poll::Reset=>{act::stop(bot);self.clear_segment();self.policy=self.academy.policy.clone();return Ok(());},
            Poll::Waiting=>{rt.peers.lock().unwrap()[self.id].last_seen=crate::metrics::now();return Ok(());},
            Poll::Frame(f)=>{
                if self.academy.episode.is_none(){let p=bot.position();
                    if (p.x-f.position[0]).abs()>0.75||(p.y-f.position[1]).abs()>0.75||(p.z-f.position[2]).abs()>0.75{act::stop(bot);return Ok(());}}
                f
            }
        };
        let result=self.academy.observe(self.id,&f,self.pending.as_ref().map(|p|p.actions.as_slice()))?;
        let lesson=self.academy.lesson.as_ref().ok_or("missing active lesson")?;let examining=lesson.exam;
        let peers=rt.peers.lock().unwrap().clone();let mut o=sensor::observe(bot,self.id,&self.name,&self.previous,&self.last_action,self.position,&peers);
        sensor::academy_features(&mut o,&self.academy.features(&f));lesson.restrict(&mut o.mask);o.view.position=f.position;
        let model=self.policy.as_ref().ok_or("no policy bound to this episode")?.clone();
        if engine::policy_id(&model)!=lesson.ticket.policy{return Err("actor's cached model differs from issued episode".into());}
        let items=self.confirmed.inventory();let _=self.confirmed.damage();
        let reward=result.as_ref().map(|r|r.reward).unwrap_or(0.);let done=result.as_ref().is_some_and(|r|r.done);
        if let Some(p)=self.pending.take(){
            let outcome=result.as_ref().ok_or("action has no server transition")?;
            if !examining&&rt.cfg.mode=="train"{
                let next_value=if done{0.}else{*model.forward(&o.obs,&o.mask).out.last().unwrap()};
                self.trajectory.push(Transition{obs:p.obs,mask:p.mask,actions:p.actions,old_logp:p.logp,value:p.value,next_value,reward,terminal:done,ticks:outcome.ticks});
                if self.trajectory.len()>1024{return Err("episode exceeded bounded trajectory capacity".into());}
            }
        }
        o.view.stats_packets=self.stats_packets;o.view.spawns=self.spawns;o.view.disconnects=self.disconnects;o.view.policy_version=model.version;
        o.view.last_reward=reward;o.view.inventory_kinds=items.len();o.view.steps=self.steps;
        if done{
            let outcome=result.as_ref().unwrap();let trajectory=std::mem::take(&mut self.trajectory);
            self.academy.finish(self.id,&model,trajectory,outcome.success,outcome.reason)?;
            act::stop(bot);self.clear_segment();rt.peers.lock().unwrap()[self.id]=o.view;return Ok(());
        }
        // No partial-rollout flush or mid-episode policy swap. Other actors may
        // be waiting, but all decisions in this episode use the same model.
        let d=model.decide(&o.obs,&o.mask,&mut self.rng,examining||rt.cfg.mode=="eval");
        let mut actions=d.actions;
        if rt.cfg.mode=="random"{
            let mut off=0;actions=model.heads.iter().map(|&n|{let valid=(0..n).filter(|&i|o.mask[off+i]).collect::<Vec<_>>();let a=valid[self.rng.index(valid.len())];off+=n;a}).collect();
            if !matches!(actions[6],1..=3){actions[7]=0;}
        }
        act::apply(bot,&actions,o.slots);
        self.pending=Some(Pending{obs:o.obs,mask:o.mask,actions:actions.clone(),logp:d.logp,value:d.value});
        self.previous=o.frame;self.last_action=actions;self.position=Some(o.view.position);self.steps+=1;o.view.steps=self.steps;
        rt.peers.lock().unwrap()[self.id]=o.view;Ok(())
    }
}
fn handle_event(bot:Client,event:Event,state:State)->anyhow::Result<()>{
    let mut a=state.0.lock().unwrap();
    match event{
        Event::Packet(packet)=>{if matches!(packet.as_ref(),azalea::protocol::packets::game::ClientboundGamePacket::AwardStats(_)){a.stats_packets+=1;}a.confirmed.packet(&packet);},
        Event::Spawn=>{
            if a.spawns>0&&!engine::stop_requested(){engine::fail("unexpected actor respawn: cannot infer an unobserved terminal");return Ok(());}
            a.spawns+=1;a.clear_segment();a.academy.clear();a.ready=true;a.ticks=0;act::stop(&bot);act::request_stats(&bot);
        },
        Event::Death(_)=>{if a.ready&&!engine::stop_requested(){engine::fail("invulnerable Academy actor died; preserve checkpoint, do not synthesize a terminal");}},
        Event::Disconnect(_)=>a.lost_connection(),
        Event::ConnectionFailed(error)=>{eprintln!("{} connection failed: {error:?}",a.name);a.lost_connection();},
        Event::Tick=>{
            if engine::stop_requested(){act::stop(&bot);bot.exit();return Ok(());}
            a.ticks+=1;if a.ready&&(a.ticks+a.id as u64*3)%40==0{act::request_stats(&bot);}
            if a.ready&&a.ticks>10&&a.ticks%(20/runtime().cfg.hz)==0&&bot.exists()&&bot.health()>0.{a.decision(&bot).map_err(anyhow::Error::msg)?;}
            if a.ready&&a.pending.is_some()&&bot.exists(){act::tick_camera(&bot,&a.last_action);}
        },_=>{}
    }Ok(())
}
pub async fn handler(bot:Client,event:Event,state:State)->anyhow::Result<()>{
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(||handle_event(bot,event,state))){
        Ok(Ok(()))=>{},Ok(Err(e))=>engine::fail(&format!("agent callback: {e}")),Err(_)=>engine::fail("agent callback panicked; no fallback controller"),
    }Ok(())
}
