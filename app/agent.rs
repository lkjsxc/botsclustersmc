use std::sync::{Arc,Mutex};
use azalea::{Client,Event,ecs::component::Component};
use crate::{act,academy::{AcademyAgent,Poll},engine::{self,runtime},learning::{ppo::Transition,rng::Rng,curriculum::{self,IDLE},FRAME},sensor::{self,Confirmed}};
struct Pending{obs:Vec<f32>,mask:Vec<bool>,actions:Vec<usize>,logp:f32,value:f32,tick:u64}
pub struct Agent{
    id:usize,name:String,ready:bool,ticks:u64,steps:u64,previous:Vec<f32>,last_action:Vec<usize>,position:Option<[f64;3]>,
    pending:Option<Pending>,trajectory:Vec<Transition>,rng:Rng,confirmed:Confirmed,stats_packets:u64,spawns:u64,disconnects:u64,academy:AcademyAgent,
}
impl Default for Agent{fn default()->Self{Self{id:0,name:String::new(),ready:false,ticks:0,steps:0,previous:vec![0.;FRAME],last_action:IDLE.to_vec(),position:None,pending:None,trajectory:vec![],rng:Rng(0),confirmed:Confirmed::default(),stats_packets:0,spawns:0,disconnects:0,academy:AcademyAgent::default()}}}
#[derive(Component,Clone,Default)]pub struct State(Arc<Mutex<Agent>>);
impl State{pub fn new(id:usize,name:String,_seed:u64)->Self{let rng=runtime().control.lock().unwrap().actor_rngs[id];Self(Arc::new(Mutex::new(Agent{id,name,rng:Rng(rng),..Agent::default()})))}}
impl Agent{
    fn clear_inputs(&mut self){self.pending=None;self.previous.fill(0.);self.position=None;self.last_action=IDLE.to_vec();}
    fn flush(&mut self,completion:Option<bool>)->Result<(),String>{
        let rt=runtime();let l=&self.academy.lesson.as_ref().ok_or("flush without an issued lesson")?.choice;
        let mut c=rt.control.lock().unwrap();let sequence=c.sequence(self.id)?;
        c.submit(l,sequence,std::mem::take(&mut self.trajectory),completion)?;Ok(())
    }
    fn lost_connection(&mut self){
        self.disconnects+=1;let active=self.ready||self.pending.is_some()||!self.trajectory.is_empty();self.ready=false;
        {let mut peers=runtime().peers.lock().unwrap();peers[self.id].online=false;peers[self.id].disconnects=self.disconnects;}
        if active&&!engine::stop_requested(){engine::fail(&format!("{} disconnected during its policy cohort; training stopped without a fabricated terminal",self.name));}
    }
    fn heartbeat(&self){runtime().peers.lock().unwrap()[self.id].last_seen=crate::metrics::now();}
    fn decision(&mut self,bot:&Client)->Result<(),String>{
        let rt=runtime();
        let f=match self.academy.poll(self.id)?{
            Poll::Reset=>{
                if self.pending.is_some()||!self.trajectory.is_empty(){return Err("reset attempted with unfinished policy experience".into());}
                act::stop(bot);self.clear_inputs();self.heartbeat();return Ok(());
            },
            Poll::Waiting=>{self.heartbeat();return Ok(());},
            Poll::Frame(f)=>f,
        };
        if self.academy.episode.is_none(){let p=bot.position();if(p.x-f.position[0]).abs()>0.75||(p.y-f.position[1]).abs()>0.75||(p.z-f.position[2]).abs()>0.75{act::stop(bot);self.heartbeat();return Ok(());}}
        let outcome=self.academy.observe(self.id,&f,self.pending.as_ref().map(|p|p.actions.as_slice()))?;
        let lesson=self.academy.lesson.as_ref().ok_or("missing active lesson")?.clone();
        let model=self.academy.policy.as_ref().ok_or("missing frozen policy lease")?.clone();
        let peers=rt.peers.lock().unwrap().clone();let mut o=sensor::observe(bot,self.id,&self.name,&self.previous,&self.last_action,self.position,&peers);
        sensor::academy_features(&mut o,&self.academy.features(&f));curriculum::restrict(&mut o.mask,lesson.stage);o.view.position=f.position;
        let reward=outcome.as_ref().map(|r|r.reward).unwrap_or(0.);let done=outcome.as_ref().is_some_and(|r|r.done);
        if let Some(p)=self.pending.take(){
            let r=outcome.as_ref().ok_or("missing outcome for an executed action")?;
            if p.tick.checked_add(r.elapsed_ticks as u64)!=Some(f.tick){return Err("executed action duration differs from authoritative transition".into());}
            if !lesson.exam&&rt.cfg.mode=="train"{
                let next_value=if done{0.}else{*model.forward(&o.obs,&o.mask).out.last().unwrap()};
                self.trajectory.push(Transition{obs:p.obs,mask:p.mask,actions:p.actions,old_logp:p.logp,value:p.value,next_value,reward,terminal:done,
                    elapsed_ticks:r.elapsed_ticks,truncated:false,episode:lesson.serial,tick:f.tick});
            }
        }
        if done{
            let r=outcome.as_ref().unwrap();
            if !lesson.exam&&rt.cfg.mode=="train"{self.flush(Some(r.success))?;}
            else{rt.control.lock().unwrap().complete_without_training(&lesson.choice,r.success)?;}
            self.academy.finish(self.id,model.version,r.success,r.reason)?;act::stop(bot);self.clear_inputs();
            rt.control.lock().unwrap().local_progress(self.id,0,false)?;
        }else{
            if self.trajectory.len()>=rt.cfg.rollout{self.flush(None)?;}
            let d=if rt.cfg.mode=="random"{
                use crate::learning::next::{Rng as SharedRng,math::{Distribution,Gate}};
                let mut masks=Vec::new();let mut off=0;for &n in &model.heads{masks.push(o.mask[off..off+n].to_vec());off+=n;}
                let logits=model.heads.iter().map(|n|vec![0.;*n]).collect::<Vec<_>>();
                let distribution=Distribution::new(&logits,&masks,Some(Gate{parent:6,child:7,active_bits:0b1110,neutral:0})).map_err(str::to_string)?;
                let mut rng=SharedRng(self.rng.0);let a=distribution.sample(&mut rng,false).map_err(str::to_string)?;self.rng.0=rng.0;
                crate::learning::network::Decision{actions:a.actions,logp:a.log_probability as f32,value:0.}
            }else{model.decide(&o.obs,&o.mask,&mut self.rng,lesson.exam||rt.cfg.mode=="eval")};
            rt.control.lock().unwrap().remember_rng(self.id,lesson.choice.policy,self.rng.0)?;
            act::apply(bot,&d.actions,o.slots);
            self.pending=Some(Pending{obs:o.obs,mask:o.mask,actions:d.actions.clone(),logp:d.logp,value:d.value,tick:f.tick});
            self.previous=o.frame;self.last_action=d.actions;self.position=Some(o.view.position);self.steps+=1;
            rt.control.lock().unwrap().local_progress(self.id,self.trajectory.len(),true)?;
        }
        o.view.stats_packets=self.stats_packets;o.view.spawns=self.spawns;o.view.disconnects=self.disconnects;o.view.policy_version=model.version;
        o.view.steps=self.steps;o.view.last_reward=reward;o.view.inventory_kinds=f.evidence.stock.iter().filter(|&&n|n>0).count();rt.peers.lock().unwrap()[self.id]=o.view;Ok(())
    }
}
fn handle_event(bot:Client,event:Event,state:State)->anyhow::Result<()>{
    let mut a=state.0.lock().unwrap();
    match event{
        Event::Packet(packet)=>{if matches!(packet.as_ref(),azalea::protocol::packets::game::ClientboundGamePacket::AwardStats(_)){a.stats_packets+=1;}a.confirmed.packet(&packet);},
        Event::Spawn=>{
            if a.ready&&!engine::stop_requested(){return Err(anyhow::anyhow!("unexpected respawn during a live Academy episode"));}
            a.spawns+=1;a.clear_inputs();a.academy.clear();a.ready=true;a.ticks=0;act::stop(&bot);act::request_stats(&bot);
        },
        Event::Death(_)=>{if a.ready&&!engine::stop_requested(){return Err(anyhow::anyhow!("Academy actor died; no synthetic timing or reward will be inserted"));}},
        Event::Disconnect(_)=>a.lost_connection(),
        Event::ConnectionFailed(error)=>{eprintln!("{} connection failed: {error:?}",a.name);a.lost_connection();},
        Event::Tick=>{
            if engine::stop_requested(){act::stop(&bot);bot.exit();return Ok(());}
            a.ticks+=1;
            if a.ready&&(a.ticks+a.id as u64*3)%40==0{act::request_stats(&bot);}
            if a.ready&&a.ticks>10&&a.ticks%(20/runtime().cfg.hz)==0&&bot.exists()&&bot.health()>0.{a.decision(&bot).map_err(anyhow::Error::msg)?;}
            if a.ready&&a.pending.is_some()&&bot.exists(){act::tick_camera(&bot,&a.last_action);}
        },_=>{}
    }Ok(())
}
pub async fn handler(bot:Client,event:Event,state:State)->anyhow::Result<()>{
    let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||handle_event(bot,event,state)));
    match result{Ok(Ok(()))=>{},Ok(Err(e))=>engine::fail(&format!("agent callback failed: {e}")),Err(_)=>engine::fail("agent callback panicked; no fallback action/controller was used")};Ok(())
}
