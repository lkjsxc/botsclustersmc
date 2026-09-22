use std::sync::{Arc,Mutex};
use azalea::{Client,Event,ecs::component::Component};
use crate::{act,academy::{AcademyAgent,Poll},engine::{self,runtime},learning::{network::Model,ppo::{Transition,Rollout},rng::Rng,curriculum::{self,IDLE},FRAME},sensor::{self,Confirmed}};

struct Pending {obs:Vec<f32>,mask:Vec<bool>,actions:Vec<usize>,logp:f32,value:f32}
pub struct Agent {
    id:usize,name:String,ready:bool,ticks:u64,steps:u64,
    previous:Vec<f32>,last_action:Vec<usize>,position:Option<[f64;3]>,
    pending:Option<Pending>,trajectory:Vec<Transition>,policy:Option<Arc<Model>>,rng:Rng,
    confirmed:Confirmed, stats_packets:u64, spawns:u64, disconnects:u64, academy:AcademyAgent,
}
impl Default for Agent {
    fn default()->Self { Self{id:0,name:String::new(),ready:false,ticks:0,steps:0,
        previous:vec![0.;FRAME],last_action:IDLE.to_vec(),position:None,pending:None,
        trajectory:vec![],policy:None,rng:Rng(0),confirmed:Confirmed::default(),
        stats_packets:0,spawns:0,disconnects:0,academy:AcademyAgent::default()} }
}
#[derive(Component,Clone,Default)] pub struct State(Arc<Mutex<Agent>>);
impl State {
    pub fn new(id:usize,name:String,seed:u64)->Self {Self(Arc::new(Mutex::new(Agent{id,name,rng:Rng(seed.wrapping_add(id as u64*73_187)),..Agent::default()})))}
}
impl Agent {
    fn flush(&mut self) {
        if let Some(model)=&self.policy {engine::send(runtime(),Rollout{version:model.version,steps:std::mem::take(&mut self.trajectory)});}
        self.policy=Some(runtime().policy.read().unwrap().clone());
    }
    fn clear_segment(&mut self) {
        if !self.trajectory.is_empty(){runtime().training.lock().unwrap().dropped+=1;}
        self.pending=None;self.trajectory.clear();self.previous.fill(0.);self.position=None;
        self.last_action=IDLE.to_vec();self.policy=Some(runtime().policy.read().unwrap().clone());
    }
    fn lost_connection(&mut self) {
        self.disconnects+=1;self.clear_segment();self.ready=false;
        self.academy.clear();self.confirmed=Confirmed::default();
        let mut peers=runtime().peers.lock().unwrap();
        peers[self.id].online=false;peers[self.id].disconnects=self.disconnects;
    }
    fn decision(&mut self,bot:&Client)->Result<(),String> {
        let rt=runtime();
        let course_frame=if rt.cfg.curriculum {
            match self.academy.poll(self.id)? {
                Poll::Reset=>{act::stop(bot);self.clear_segment();return Ok(());},
                Poll::Waiting=>{
                    // Waiting for a reset or the slower exam participants is not a disconnect.
                    let mut peers=rt.peers.lock().unwrap();peers[self.id].last_seen=crate::metrics::now();
                    return Ok(());
                },
                Poll::Frame(f)=>{
                    // A server-side teleport acknowledgement alone is not proof the client
                    // loaded the destination. Never act from pre-reset client observations.
                    if self.academy.episode.is_none() {
                        let p=bot.position();
                        if (p.x-f.position[0]).abs()>0.75||(p.y-f.position[1]).abs()>0.75||(p.z-f.position[2]).abs()>0.75 {act::stop(bot);return Ok(());}
                    }
                    Some(f)
                }
            }
        } else {None};
        let outcome=course_frame.as_ref().and_then(|f|self.academy.observe(self.id,f,self.pending.as_ref().map(|p|p.actions.as_slice())));
        let examining=self.academy.lesson.as_ref().is_some_and(|l|l.exam);
        let peers=rt.peers.lock().unwrap().clone();
        let mut o=sensor::observe(bot,self.id,&self.name,&self.previous,&self.last_action,self.position,&peers);
        if let Some(f)=&course_frame {
            let l=self.academy.lesson.as_ref().unwrap();
            sensor::academy_features(&mut o,&self.academy.features(f));
            curriculum::restrict(&mut o.mask,l.stage);
            o.view.position=f.position;
        }
        if self.policy.is_none(){self.policy=Some(rt.policy.read().unwrap().clone());}
        let items=self.confirmed.inventory();
        let reward=if rt.cfg.curriculum {
            // The open-world novelty/mining/frontier rewards are disabled here.
            // Otherwise walking aimlessly can compete with the actual lesson.
            let _=self.confirmed.damage();outcome.as_ref().map(|r|r.reward).unwrap_or(0.)
        }else {
            let progress=self.confirmed.progress();let damage=self.confirmed.damage();
            let mut book=rt.rewards.lock().unwrap();let r=book.observe(self.id,o.view.dimension,o.view.position,&progress,damage,false);
            o.view.cells=book.agents[self.id].cells;o.view.deaths=book.agents[self.id].deaths;r.total()
        };
        let done=outcome.as_ref().is_some_and(|r|r.done);
        if let Some(p)=self.pending.take() {
            if !examining && rt.cfg.mode=="train" {
                let model=self.policy.as_ref().unwrap();
                let next_value=if done{0.}else{*model.forward(&o.obs,&o.mask).out.last().unwrap()};
                self.trajectory.push(Transition{obs:p.obs,mask:p.mask,actions:p.actions,old_logp:p.logp,value:p.value,next_value,reward,terminal:done});
            }
        }
        if done {
            let result=outcome.as_ref().unwrap();let version=self.policy.as_ref().unwrap().version;
            self.academy.finish(self.id,version,result.success,result.reason)?;
            if !examining{self.flush();}
            act::stop(bot);self.pending=None;self.previous.fill(0.);self.position=None;self.last_action=IDLE.to_vec();
            o.view.steps=self.steps;o.view.last_reward=reward;o.view.stats_packets=self.stats_packets;o.view.spawns=self.spawns;
            rt.peers.lock().unwrap()[self.id]=o.view;
            return Ok(());
        }
        let newest=rt.policy.read().unwrap().version;
        if !examining && (self.trajectory.len()>=rt.cfg.rollout||self.policy.as_ref().is_some_and(|p|p.version!=newest)){self.flush();}
        let model=self.policy.as_ref().unwrap();
        let d=model.decide(&o.obs,&o.mask,&mut self.rng,examining||rt.cfg.mode=="eval");
        let actions=if rt.cfg.mode=="random" {
            let mut off=0;model.heads.iter().map(|&n|{let valid=(0..n).filter(|&i|o.mask[off+i]).collect::<Vec<_>>();let a=valid[self.rng.index(valid.len())];off+=n;a}).collect()
        }else{d.actions};
        act::apply(bot,&actions,o.slots);
        self.pending=Some(Pending{obs:o.obs,mask:o.mask,actions:actions.clone(),logp:d.logp,value:d.value});
        self.previous=o.frame;self.last_action=actions;self.position=Some(o.view.position);self.steps+=1;
        o.view.stats_packets=self.stats_packets;o.view.spawns=self.spawns;o.view.disconnects=self.disconnects;o.view.policy_version=model.version;
        o.view.steps=self.steps;o.view.last_reward=reward;o.view.inventory_kinds=items.len();rt.peers.lock().unwrap()[self.id]=o.view;
        Ok(())
    }
    fn death(&mut self)->Result<(),String> {
        let rt=runtime();
        if rt.cfg.curriculum {
            let examining=self.academy.lesson.as_ref().is_some_and(|l|l.exam);
            if self.academy.lesson.is_some() {
                self.academy.finish(self.id,self.policy.as_ref().map(|p|p.version).unwrap_or(0),false,"death")?;
            }
            if let Some(p)=self.pending.take() {
                if !examining{self.trajectory.push(Transition{obs:p.obs,mask:p.mask,actions:p.actions,old_logp:p.logp,value:p.value,next_value:0.,reward:-1.,terminal:true});}
            }
            self.flush();self.ready=false;self.clear_segment();self.academy.clear();return Ok(());
        }
        let view=rt.peers.lock().unwrap()[self.id].clone();let progress=self.confirmed.progress();let damage=self.confirmed.damage();
        let r={let mut b=rt.rewards.lock().unwrap();b.observe(self.id,view.dimension,view.position,&progress,damage,true).total()};
        if let Some(p)=self.pending.take(){self.trajectory.push(Transition{obs:p.obs,mask:p.mask,actions:p.actions,old_logp:p.logp,value:p.value,next_value:0.,reward:r,terminal:true});}
        self.flush();self.ready=false;self.previous.fill(0.);self.position=None;
        let deaths=rt.rewards.lock().unwrap().agents[self.id].deaths;
        let mut peers=rt.peers.lock().unwrap();peers[self.id].health=0.;peers[self.id].deaths=deaths;peers[self.id].last_reward=r;Ok(())
    }
}
fn handle_event(bot:Client,event:Event,state:State)->anyhow::Result<()> {
    let mut a=state.0.lock().unwrap();
    match event {
        Event::Packet(packet)=>{
            if matches!(packet.as_ref(),azalea::protocol::packets::game::ClientboundGamePacket::AwardStats(_)){a.stats_packets+=1;}
            a.confirmed.packet(&packet);
        },
        Event::Spawn=>{
            a.spawns+=1;a.clear_segment();a.academy.clear();a.ready=true;a.ticks=0;act::stop(&bot);act::request_stats(&bot);
        },
        Event::Death(_)=>{if a.ready{a.death().map_err(anyhow::Error::msg)?;}},
        Event::Disconnect(_)=>a.lost_connection(),
        Event::ConnectionFailed(error)=>{eprintln!("{} connection failed: {error:?}",a.name);a.lost_connection();},
        Event::Tick=>{
            if engine::stop_requested(){act::stop(&bot);bot.exit();return Ok(());}
            a.ticks+=1;
            if a.ready&&(a.ticks+a.id as u64*3)%40==0{act::request_stats(&bot);}
            if a.ready&&a.ticks>10&&a.ticks%(20/runtime().cfg.hz)==0&&bot.exists()&&bot.health()>0. {
                a.decision(&bot).map_err(anyhow::Error::msg)?;
            }
            if a.ready&&a.pending.is_some()&&bot.exists(){act::tick_camera(&bot,&a.last_action);}
        },_=>{}
    }
    Ok(())
}
pub async fn handler(bot:Client,event:Event,state:State)->anyhow::Result<()> {
    let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||handle_event(bot,event,state)));
    match result {
        Ok(Ok(()))=>{},Ok(Err(error))=>engine::fail(&format!("agent callback failed: {error}")),
        Err(_)=>engine::fail("agent callback panicked; inspect the panic log; no fallback policy was used"),
    }
    Ok(())
}
