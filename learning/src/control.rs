//! Native coordination shared by the real actors and headless regression tests.
//! One lock owns the published policy, curriculum and collection generation.
//! No actor may infer with a newly published policy in an old collection round.
use std::sync::Arc;
use super::{bundle::{Bundle,policy_id},checkpoint::Checkpoint,network::Model,ppo::{self,Transition,Rollout},next::{cohort::{Cohort,Round,Packet},curriculum::{Curriculum,Lesson,Phase,PolicyId}}};
#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub enum Mode{Train,Eval,Random}
impl Mode{pub fn parse(s:&str)->Result<Self,String>{match s{"train"=>Ok(Self::Train),"eval"=>Ok(Self::Eval),"random"=>Ok(Self::Random),_=>Err("unknown learning mode".into())}}}
#[derive(Clone,Copy,Default,Debug)]
struct Clock{episode:Option<u64>,tick:u64,closed:bool}
pub struct Coordinator{
    pub policy:Arc<Model>,pub course:Curriculum,pub actor_rngs:Vec<u64>,pub mode:Mode,
    identity:PolicyId,cohort:Cohort<Transition>,clocks:Vec<Clock>,quota:usize,optimizing:bool,
    pub local_samples:Vec<usize>,pub pending_actions:Vec<bool>,
}
pub struct Update{pub rollouts:Vec<Rollout>,pub policy:PolicyId,pub generation:u64,pub samples:usize}
impl Coordinator{
    pub fn new(mut b:Bundle,quota:usize,mode:Mode)->Result<Self,String>{
        if mode==Mode::Eval{b.curriculum.begin_evaluation(policy_id(&b.checkpoint)).map_err(str::to_string)?;}
        if b.actor_rngs.len()!=b.curriculum.bots(){return Err("actor RNG population mismatch".into());}
        let n=b.curriculum.bots();let round=Round{policy_version:b.checkpoint.model.version,generation:b.curriculum.generation()};
        let cohort=Cohort::new(round,n,quota,3100).map_err(str::to_string)?;
        let identity=policy_id(&b.checkpoint);
        Ok(Self{identity,policy:Arc::new(b.checkpoint.model),course:b.curriculum,actor_rngs:b.actor_rngs,mode,cohort,
            clocks:vec![Clock::default();n],quota,optimizing:false,local_samples:vec![0;n],pending_actions:vec![false;n]})
    }
    pub fn id(&self)->PolicyId{self.identity}
    pub fn examining(&self)->bool{matches!(self.course.phase(),Phase::Exam{..})}
    pub fn optimizing(&self)->bool{self.optimizing}
    pub fn buffered(&self)->usize{self.cohort.len()}
    pub fn sealed(&self)->usize{self.cohort.progress().iter().filter(|a|a.sealed).count()}
    pub fn outstanding(&self)->Vec<usize>{self.cohort.outstanding_actors()}
    pub fn sequence(&self,id:usize)->Result<u64,String>{self.cohort.progress().get(id).map(|a|a.next_sequence).ok_or("unknown actor".into())}
    pub fn issue(&mut self,id:usize)->Result<Option<(Lesson,Arc<Model>)>,String>{
        if id>=self.course.bots(){return Err("unknown actor".into());}
        if self.optimizing || (self.mode==Mode::Train&&!self.examining()&&self.cohort.progress()[id].sealed){return Ok(None);}
        let policy=self.id();
        Ok(self.course.issue(id,policy).map_err(str::to_string)?.map(|l|(l,self.policy.clone())))
    }
    pub fn remember_rng(&mut self,id:usize,policy:PolicyId,state:u64)->Result<(),String>{
        if policy!=self.id() || self.course.pending(id).map_err(str::to_string)?.is_none(){return Err("RNG update outside the actor's policy lease".into());}
        self.actor_rngs[id]=state;Ok(())
    }
    pub fn local_progress(&mut self,id:usize,samples:usize,pending:bool)->Result<(),String>{
        if id>=self.course.bots() || samples>512{return Err("local rollout buffer exceeded its bound".into());}
        self.local_samples[id]=samples;self.pending_actions[id]=pending;Ok(())
    }
    /// Accept a contiguous fragment from exactly the currently issued episode.
    /// `completion` denotes a real terminal, never a reset acknowledgement.
    pub fn submit(&mut self,lesson:&Lesson,sequence:u64,steps:Vec<Transition>,completion:Option<bool>)->Result<(),String>{
        let id=lesson.session.actor as usize;
        if self.optimizing || lesson.policy!=self.id() || self.course.pending(id).map_err(str::to_string)?!=Some(lesson){return Err("stale or unissued policy/lesson fragment".into());}
        if self.mode!=Mode::Train || lesson.evaluation{return Err("diagnostic/evaluation samples cannot enter the optimizer".into());}
        if steps.is_empty(){return Err("an empty fragment cannot invent a task result".into());}
        let mut clock=*self.clocks.get(id).ok_or("unknown actor")?;
        if clock.episode!=Some(lesson.session.lesson){
            if clock.episode.is_some()&&!clock.closed{return Err("episode changed without a recorded boundary".into());}
            clock=Clock{episode:Some(lesson.session.lesson),tick:steps[0].tick.checked_sub(steps[0].elapsed_ticks as u64).ok_or("tick underflow")?,closed:false};
        }
        for (i,t) in steps.iter().enumerate(){
            ppo::validate_transition(t,&self.policy)?;
            if clock.closed || t.episode!=lesson.session.lesson || t.tick.checked_sub(t.elapsed_ticks as u64)!=Some(clock.tick){return Err("missing, reordered or foreign server transition".into());}
            if t.truncated{return Err("administrative truncation needs an explicit cancellation, not a task result".into());}
            if t.terminal && i+1!=steps.len(){return Err("terminal in the middle of an episode fragment".into());}
            clock.tick=t.tick;clock.closed=t.terminal;
        }
        if clock.closed!=completion.is_some(){return Err("task completion and observed terminal disagree".into());}
        // Prepare course mutation before touching the buffer, so either both
        // validations succeed or neither has any externally visible effect.
        let mut next_course=None;
        if let Some(success)=completion{let mut course=self.course.clone();course.record(lesson,self.id(),success).map_err(str::to_string)?;next_course=Some(course);}
        let seal=completion.is_some() && self.cohort.progress()[id].samples+steps.len()>=self.quota;
        self.cohort.push(Packet{round:Round{policy_version:lesson.policy.version,generation:lesson.session.generation},actor:id,sequence,
            evaluation:false,samples:steps,ends_episode:completion.is_some(),seal},|_|Ok(())).map_err(str::to_string)?;
        if let Some(course)=next_course{self.course=course;}
        self.clocks[id]=clock;self.local_samples[id]=0;self.pending_actions[id]=false;Ok(())
    }
    pub fn complete_without_training(&mut self,lesson:&Lesson,success:bool)->Result<(),String>{
        if self.mode==Mode::Train && !lesson.evaluation{return Err("training completion must include its transitions".into());}
        self.course.record(lesson,self.id(),success).map_err(str::to_string)
    }
    pub fn take_update(&mut self)->Result<Option<Update>,String>{
        if self.mode!=Mode::Train || self.examining() || self.optimizing || !self.cohort.ready(){return Ok(None);}
        for id in 0..self.course.bots(){if self.course.pending(id).map_err(str::to_string)?.is_some(){return Err("sealed actor still owns a pending lesson".into());}}
        let b=self.cohort.take_ready().map_err(str::to_string)?;let samples=b.len();
        self.optimizing=true;
        let rollouts=b.trajectories.into_iter().map(|steps|Rollout{version:b.round.policy_version,steps}).collect();
        Ok(Some(Update{rollouts,policy:self.id(),generation:b.round.generation,samples}))
    }
    /// The caller saves this complete candidate BEFORE making it available.
    pub fn candidate(&self,cp:&Checkpoint)->Result<Bundle,String>{
        if !self.optimizing || Some(cp.model.version)!=self.policy.version.checked_add(1){return Err("no matching completed optimizer transaction".into());}
        let mut course=self.course.clone();
        if course.exam_ready(){course.begin_exam(policy_id(cp)).map_err(str::to_string)?;}
        Ok(Bundle{checkpoint:cp.clone(),curriculum:course,actor_rngs:self.actor_rngs.clone()})
    }
    pub fn publish_update(&mut self,b:&Bundle)->Result<(),String>{
        if !self.optimizing || Some(b.checkpoint.model.version)!=self.policy.version.checked_add(1) || b.actor_rngs!=self.actor_rngs{return Err("policy publication does not match the frozen optimizer transaction".into());}
        self.publish(b)
    }
    fn publish(&mut self,b:&Bundle)->Result<(),String>{
        if b.curriculum.bots()!=self.course.bots(){return Err("population changed during publication".into());}
        let round=Round{policy_version:b.checkpoint.model.version,generation:b.curriculum.generation()};
        let cohort=Cohort::new(round,self.course.bots(),self.quota,3100).map_err(str::to_string)?;
        self.identity=policy_id(&b.checkpoint);self.policy=Arc::new(b.checkpoint.model.clone());self.course=b.curriculum.clone();self.cohort=cohort;
        self.clocks.fill(Clock::default());self.local_samples.fill(0);self.pending_actions.fill(false);self.optimizing=false;Ok(())
    }
    pub fn snapshot(&self,cp:&Checkpoint)->Result<Bundle,String>{
        if policy_id(cp)!=self.id(){return Err("snapshot uses a different published neural policy".into());}
        Ok(Bundle{checkpoint:cp.clone(),curriculum:self.course.clone(),actor_rngs:self.actor_rngs.clone()})
    }
    pub fn exam_candidate(&self,cp:&Checkpoint)->Result<Option<(Bundle,bool)>,String>{
        if self.mode!=Mode::Train || self.optimizing || !self.course.exam_finished(){return Ok(None);}
        let mut b=self.snapshot(cp)?;let passed=b.curriculum.finish_exam(policy_id(cp)).map_err(str::to_string)?;Ok(Some((b,passed)))
    }
    pub fn publish_exam(&mut self,b:&Bundle)->Result<(),String>{
        if self.optimizing || !self.course.exam_finished() || policy_id(&b.checkpoint)!=self.id() || Some(b.curriculum.generation())!=self.course.generation().checked_add(1){return Err("invalid exam transaction".into());}
        self.publish(b)
    }
}
#[cfg(test)] mod tests{
    use super::*;use super::super::{next::curriculum::Curriculum,rng::Rng};
    fn fixture(n:usize)->(Coordinator,Checkpoint){let cp=Checkpoint::new(Model::new(1,3,vec![2],9),7);let b=Bundle{checkpoint:cp.clone(),curriculum:Curriculum::new(99,n,11).unwrap(),actor_rngs:vec![31;n]};(Coordinator::new(b,2,Mode::Train).unwrap(),cp)}
    fn step(m:&Model,l:&Lesson,tick:u64,terminal:bool)->Transition{let d=m.decide(&[0.5],&[true,true],&mut Rng(81),false);Transition{obs:vec![0.5],mask:vec![true,true],actions:d.actions,old_logp:d.logp,value:d.value,next_value:0.,reward:1.,terminal,elapsed_ticks:4,truncated:false,episode:l.session.lesson,tick}}
    #[test]fn every_one_of_64_actors_contributes_before_one_atomic_release(){let(mut c,mut cp)=fixture(64);for id in 0..64{let(l,m)=c.issue(id).unwrap().unwrap();c.submit(&l,0,vec![step(&m,&l,4,false),step(&m,&l,8,true)],Some(false)).unwrap();assert!(c.issue(id).unwrap().is_none());if id<63{assert!(c.take_update().unwrap().is_none());}}let update=c.take_update().unwrap().unwrap();assert_eq!(update.samples,128);assert_eq!(update.rollouts.len(),64);assert!(update.rollouts.iter().all(|r|r.steps.len()==2));assert!(c.issue(63).unwrap().is_none());cp.model.version+=1;cp.samples=128;let candidate=c.candidate(&cp).unwrap();assert_eq!(c.policy.version,0);candidate.encode().unwrap();c.publish_update(&candidate).unwrap();assert_eq!(c.policy.version,1);assert!(c.issue(63).unwrap().is_some());assert_eq!(c.buffered(),0);}
    #[test]fn lost_duplicate_and_foreign_fragments_are_errors_without_mutation(){let(mut c,_)=fixture(1);let(l,m)=c.issue(0).unwrap().unwrap();c.submit(&l,0,vec![step(&m,&l,4,false)],None).unwrap();assert!(c.submit(&l,0,vec![step(&m,&l,8,true)],Some(true)).is_err());assert!(c.submit(&l,1,vec![step(&m,&l,12,true)],Some(true)).is_err());let mut foreign=l.clone();foreign.session.generation+=1;assert!(c.submit(&foreign,1,vec![step(&m,&l,8,true)],Some(true)).is_err());assert_eq!(c.buffered(),1);assert_eq!(c.course.stats(0).unwrap()[0].attempts,0);c.submit(&l,1,vec![step(&m,&l,8,true)],Some(true)).unwrap();assert_eq!(c.course.stats(0).unwrap()[0].attempts,1);}
    #[test]fn terminal_and_success_cannot_be_invented(){let(mut c,_)=fixture(1);let(l,m)=c.issue(0).unwrap().unwrap();assert!(c.submit(&l,0,vec![],Some(true)).is_err());assert!(c.submit(&l,0,vec![step(&m,&l,4,false)],Some(true)).is_err());assert_eq!(c.buffered(),0);assert!(c.course.pending(0).unwrap().is_some());}
    #[test]fn evaluate_never_enters_optimizer_or_advances_policy(){let(_,cp)=fixture(1);let b=Bundle{checkpoint:cp.clone(),curriculum:Curriculum::new(99,1,11).unwrap(),actor_rngs:vec![31]};let mut c=Coordinator::new(b,2,Mode::Eval).unwrap();let(l,m)=c.issue(0).unwrap().unwrap();assert!(l.evaluation);assert!(c.submit(&l,0,vec![step(&m,&l,4,true)],Some(true)).is_err());c.complete_without_training(&l,true).unwrap();assert!(c.take_update().unwrap().is_none());assert!(c.exam_candidate(&cp).unwrap().is_none());assert_eq!(c.policy.version,0);}
}
