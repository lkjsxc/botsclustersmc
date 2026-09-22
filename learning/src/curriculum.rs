//! Finite-horizon goal tasks, stage-local input availability and held-out gates.
//! This module never emits movement, camera directions, paths, or action labels.
use super::{rng::Rng, checkpoint::{Encoder, Decoder, checksum}, HEADS, ACTIONS};
use std::io;

pub const STAGES: usize = 6;
pub const TRAIN_EPISODES: u32 = 40;
pub const EXAM_CURRENT: u32 = 16;
/// Four held-out trials for EACH earlier stage (a second base set at stage zero).
pub const EXAM_REVIEW: u32 = 4;
pub fn review_total(stage:usize)->u32 { EXAM_REVIEW * stage.max(1) as u32 }
pub const IDLE: [usize; 8] = [0, 3, 2, 0, 0, 0, 0, 0];
pub const YAW_RATES: [f32; 7] = [-90., -30., -10., 0., 10., 30., 90.];
pub const PITCH_RATES: [f32; 5] = [-45., -15., 0., 15., 45.];
pub const NAMES: [&str; STAGES] = ["forward-stop", "turn-and-stop", "look-at-target", "navigate-and-stop", "step-over", "mine-target"];

/// One 16x16 chunk per cell; its 12x12 interior is enclosed in glass.
pub fn origin(id: usize) -> [f64; 2] { [((id % 8) * 16) as f64, ((id / 8) * 16) as f64] }
#[derive(Clone, Debug)]
pub struct Lesson {
    pub stage: usize, pub serial: u64, pub generation: u64, pub exam: bool, pub review: bool,
    pub start: [f64; 3], pub goal: [f64; 3], pub yaw: f32, pub pitch: f32, pub limit_ticks: u64,
}
fn between(r: &mut Rng, lo: f64, hi: f64) -> f64 { lo + (hi-lo) * r.uniform() as f64 }
impl Lesson {
    pub fn sample(id: usize, stage: usize, serial: u64, generation: u64, exam: bool, review: bool, seed: u64) -> Self {
        assert!(id < 32 && stage < STAGES);
        // Evaluation and training have disjoint PRNG domains. Serial counters are persisted.
        let salt = if exam { 0x6578_616d_2d76_3031 } else { 0x7472_6169_6e76_3031 };
        let mut r = Rng(seed ^ salt ^ (id as u64).wrapping_mul(7919) ^ serial.wrapping_mul(0x9e3779b97f4a7c15));
        let [x,z] = origin(id);
        let mut l = Self { stage, serial, generation, exam, review, start:[x+8.,97.,z+5.], goal:[x+8.,97.,z+8.], yaw:0., pitch:0., limit_ticks:600 };
        match stage {
            0 => { l.start[0]=x+between(&mut r,5.,10.).floor()+0.5; l.goal[0]=l.start[0]; l.goal[2]=z+between(&mut r,7.,10.); },
            1 | 3 => {
                l.start=[x+7.5,97.,z+7.5]; let angle=between(&mut r,0.,std::f64::consts::TAU);
                let distance=between(&mut r,2.,4.5); l.goal=[x+7.5+angle.cos()*distance,97.,z+7.5+angle.sin()*distance];
                l.yaw=between(&mut r,-180.,180.) as f32;
            },
            2 => {
                l.start=[x+7.5,97.,z+7.5]; let angle=between(&mut r,0.,std::f64::consts::TAU);
                l.goal=[(x+7.5+angle.cos()*4.).floor()+0.5,between(&mut r,97.,101.).floor()+0.5,(z+7.5+angle.sin()*4.).floor()+0.5];
                l.yaw=between(&mut r,-180.,180.) as f32; l.pitch=between(&mut r,-20.,20.) as f32;
            },
            4 => { l.start=[x+between(&mut r,5.,10.),97.,z+4.5]; l.goal=[x+between(&mut r,5.,10.),97.,z+11.5]; l.yaw=between(&mut r,-20.,20.) as f32; l.limit_ticks=900; },
            5 => {
                l.start=[x+between(&mut r,6.,9.),97.,z+7.5]; l.goal=[x+8.5,98.5,z+11.5];
                l.yaw=between(&mut r,-25.,25.) as f32; l.limit_ticks=900;
            }, _=>unreachable!()
        }
        // Goal blocks and the numeric goal share an unambiguous horizontal center.
        l.goal[0]=l.goal[0].floor()+0.5;l.goal[2]=l.goal[2].floor()+0.5;
        l
    }
}
/// Only task-wide input availability, never target-dependent hints.
pub fn restrict(mask: &mut [bool], stage: usize) {
    assert!(stage<STAGES && mask.len()==ACTIONS);
    let mut off=0;
    for (h,&n) in HEADS.iter().enumerate() {
        for a in 0..n {
            let enabled=match h {
                0=>match stage {0=>a<=1,1=>a<=1,2=>a==0,_=>true},
                1=>stage!=0 || a==IDLE[1],
                2=>matches!(stage,2|5) || a==IDLE[2],
                3=>stage==4 && a<=1 || a==0,
                4=>stage==5 && a==1 || a==0,
                _=>a==IDLE[h],
            };
            mask[off+a]&=enabled;
        }
        off+=n;
    }
}
pub fn angles(position:[f64;3], yaw:f32, pitch:f32, goal:[f64;3])->[f32;2] {
    let dx=goal[0]-position[0]; let dz=goal[2]-position[2]; let dy=goal[1]-(position[1]+1.62);
    let desired_yaw=(-dx).atan2(dz).to_degrees() as f32;
    let desired_pitch=(-dy).atan2((dx*dx+dz*dz).sqrt()).to_degrees() as f32;
    [(desired_yaw-yaw+180.).rem_euclid(360.)-180., (desired_pitch-pitch).clamp(-180.,180.)]
}
#[derive(Clone,Debug)]
pub struct Frame { pub tick:u64, pub position:[f64;3], pub yaw:f32, pub pitch:f32, pub grounded:bool, pub broken:bool }
#[derive(Clone,Debug)]
pub struct Episode { pub first_tick:u64, pub previous:Frame, pub held:u64, pub rotation:f64, pub switches:u64, pub decisions:u64, pub speed:f32, pub angular_speed:f32, pub previous_action:[usize;8] }
#[derive(Clone,Debug)]
pub struct Outcome { pub reward:f32, pub done:bool, pub success:bool, pub reason:&'static str }
fn distance(p:[f64;3],goal:[f64;3])->f64 { ((p[0]-goal[0]).powi(2)+(p[2]-goal[2]).powi(2)).sqrt() }
fn potential(l:&Lesson, f:&Frame)->f32 {
    if matches!(l.stage,2|5) {let a=angles(f.position,f.yaw,f.pitch,l.goal); -0.003*(a[0].abs()+a[1].abs())}
    else {-0.15*distance(f.position,l.goal).min(20.) as f32}
}
impl Episode {
    pub fn new(f:Frame)->Self {Self{first_tick:f.tick,previous:f,held:0,rotation:0.,switches:0,decisions:0,speed:0.,angular_speed:0.,previous_action:IDLE}}
    pub fn step(&mut self,id:usize,l:&Lesson,f:Frame,action:&[usize],gamma:f32)->Outcome {
        let dt=f.tick.saturating_sub(self.previous.tick);
        assert!(dt>0 && action.len()==8);
        let speed=distance(f.position,self.previous.position)/dt as f64;
        let turn=((f.yaw-self.previous.yaw+180.).rem_euclid(360.)-180.).abs();
        let angular_speed=(turn+(f.pitch-self.previous.pitch).abs())/dt as f32;
        self.speed=speed as f32;self.angular_speed=angular_speed;
        let [x,z]=origin(id);
        let outside=f.position[0]<x+1.7||f.position[0]>x+14.3||f.position[2]<z+1.7||f.position[2]>z+14.3||f.position[1]<96.5||f.position[1]>103.;
        let angle=angles(f.position,f.yaw,f.pitch,l.goal);
        let ready=if l.stage==2 { angle[0].abs()<=8. && angle[1].abs()<=8. && angular_speed<=0.15 }
            else {distance(f.position,l.goal)<=0.65 && speed<=0.025 && angular_speed<=0.15 && f.grounded};
        // Missing snapshots must not be interpreted as continuous successful holding.
        if ready && dt<=8 {self.held+=dt;}else{self.held=0;}
        let success=!outside && if l.stage==5 {f.broken} else {self.held>=20};
        let timeout=f.tick.saturating_sub(self.first_tick)>=l.limit_ticks;
        let done=success||outside||timeout;
        let before=potential(l,&self.previous); let after=if done {0.}else{potential(l,&f)};
        self.rotation+=turn as f64;
        let switched=action[0]!=self.previous_action[0]; if switched{self.switches+=1;}
        // Task reward dominates small input regularizers. No reward for mere staying alive.
        let reward=(if success {2.} else if done {-1.} else {0.}) + gamma*after-before -0.002 -0.00005*turn -(if switched {0.003}else{0.});
        self.decisions+=1;self.previous_action.copy_from_slice(action);self.previous=f;
        Outcome{reward,done,success,reason:if success{"success"}else if outside{"outside"}else if timeout{"timeout"}else{"running"}}
    }
    /// 23 slots fit in the existing frame's unused tail. Goal observations are
    /// privileged training instrumentation, not images or human demonstrations.
    pub fn features(&self,l:&Lesson,f:&Frame)->[f32;23] {
        let mut v=[0.;23]; let a=angles(f.position,f.yaw,f.pitch,l.goal);
        v[0]=((l.goal[0]-f.position[0])/12.) as f32;v[1]=((l.goal[1]-f.position[1])/8.) as f32;v[2]=((l.goal[2]-f.position[2])/12.) as f32;
        v[3]=a[0].to_radians().sin();v[4]=a[0].to_radians().cos();v[5]=a[1]/180.;
        v[6+l.stage]=1.;v[12]=(f.tick.saturating_sub(self.first_tick) as f32/l.limit_ticks as f32).min(1.);
        v[13]=(self.held as f32/20.).min(1.);v[14]=if f.grounded{1.}else{0.};
        v[15]=f.yaw.to_radians().sin();v[16]=f.yaw.to_radians().cos();
        v[17]=(f.pitch/90.).clamp(-1.,1.);v[18]=1.;v[19]=(self.speed/0.3).clamp(0.,1.);v[20]=(self.angular_speed/4.5).clamp(0.,1.);v
    }
}
#[derive(Clone,Copy,PartialEq,Eq,Debug)] pub enum Phase { Train, Exam }
#[derive(Clone,Default,Debug)] pub struct Score {pub trained:u32,pub current:u32,pub current_success:u32,pub review:u32,pub review_success:u32,pub retained:[u32;STAGES]}
#[derive(Clone,Debug)] pub struct Curriculum {
    pub stage:usize,pub phase:Phase,pub generation:u64,pub cycle:u64,pub completed:bool,
    pub scores:Vec<Score>,pub serials:Vec<u64>,pub last_pass:Option<bool>,pub exams:u64,
}
impl Curriculum {
    pub fn new(bots:usize)->Self{assert!((1..=32).contains(&bots));Self{stage:0,phase:Phase::Train,generation:0,cycle:0,completed:false,scores:vec![Score::default();bots],serials:vec![0;bots],last_pass:None,exams:0}}
    pub fn begin(&mut self,id:usize,seed:u64)->Option<Lesson> {
        let s=&self.scores[id];let exam=self.phase==Phase::Exam;
        if exam && s.current>=EXAM_CURRENT && s.review>=review_total(self.stage){return None;}
        let serial=self.serials[id];self.serials[id]=serial.checked_add(1).expect("episode counter exhausted");
        let review=if exam{s.current>=EXAM_CURRENT}else{serial%5==4&&self.stage>0};
        let stage=if review&&self.stage>0 {
            if exam {(s.review/EXAM_REVIEW) as usize % self.stage}
            else {((serial/5) as usize+id)%self.stage}
        }else{self.stage};
        Some(Lesson::sample(id,stage,serial,self.generation,exam,review,seed))
    }
    pub fn record(&mut self,id:usize,l:&Lesson,success:bool) {
        if l.generation!=self.generation||l.exam!=(self.phase==Phase::Exam){return;}
        let s=&mut self.scores[id];
        if l.exam {if l.review{s.review+=1;s.review_success+=success as u32;s.retained[l.stage]+=success as u32;}else{s.current+=1;s.current_success+=success as u32;}}
        else if !l.review{s.trained=s.trained.saturating_add(1);}
    }
    /// Called by the learner ONLY between optimizer updates. All evaluators
    /// therefore see one frozen model. No agent can promote itself.
    pub fn advance(&mut self)->Option<&'static str> {
        match self.phase {
            Phase::Train if self.scores.iter().all(|s|s.trained>=TRAIN_EPISODES)=>{
                self.phase=Phase::Exam;self.generation+=1;self.cycle+=1;
                for s in &mut self.scores{s.current=0;s.review=0;s.current_success=0;s.review_success=0;s.retained=[0;STAGES];}
                Some("exam-start")
            },
            Phase::Exam if self.scores.iter().all(|s|s.current>=EXAM_CURRENT && s.review>=review_total(self.stage))=>{
                let passed=self.scores.iter().all(|s|s.current_success>=14 && s.retained[..self.stage.max(1)].iter().all(|&n|n>=3));
                self.last_pass=Some(passed);self.exams+=1;
                if passed {if self.stage+1<STAGES {self.stage+=1;}else{self.completed=true;}}
                self.phase=Phase::Train;self.generation+=1;for s in &mut self.scores{*s=Score::default();}
                Some(if passed{"exam-pass"}else{"exam-fail"})
            },_=>None
        }
    }
    pub fn encode(&self,version:u64,fingerprint:u64)->Vec<u8>{
        let mut e=Encoder(b"BCAC0001".to_vec());
        for v in [version,fingerprint,self.stage as u64,self.cycle,self.completed as u64,self.exams,self.scores.len() as u64]{e.u64(v);}
        for (s,&serial) in self.scores.iter().zip(&self.serials){e.u64(serial);e.u64(s.trained as u64);}
        let c=checksum(&e.0);e.u64(c);e.0
    }
    pub fn decode(bytes:&[u8],bots:usize,version:u64,fingerprint:u64)->io::Result<Self>{
        let bad=||io::Error::new(io::ErrorKind::InvalidData,"academy checkpoint mismatch: restore a complete stopped backup");
        if bytes.len()<72||bytes.len()>4096{return Err(bad());}
        let body=&bytes[..bytes.len()-8];if checksum(body)!=u64::from_le_bytes(bytes[bytes.len()-8..].try_into().unwrap()){return Err(bad());}
        let mut d=Decoder{bytes:body,pos:0};if d.take(8)?!=b"BCAC0001"||d.u64()?!=version||d.u64()?!=fingerprint{return Err(bad());}
        let stage=d.size(STAGES-1)?;let cycle=d.u64()?;let complete=d.size(1)?==1;let exams=d.u64()?;if d.size(32)?!=bots{return Err(bad());}
        let mut c=Self::new(bots);c.stage=stage;c.cycle=cycle;c.completed=complete;c.exams=exams;
        for i in 0..bots{c.serials[i]=d.u64()?;c.scores[i].trained=d.size(u32::MAX as usize)? as u32;}
        if d.pos!=body.len(){return Err(bad());}
        // Never restore partially completed exam scores; retry a fresh frozen exam.
        Ok(c)
    }
}

#[cfg(test)] mod tests {
    use super::*;
    #[test] fn stage_masks_leave_one_legal_action_per_head(){for stage in 0..STAGES{let mut mask=vec![true;ACTIONS];restrict(&mut mask,stage);let mut o=0;for n in HEADS{assert!(mask[o..o+n].iter().any(|x|*x));o+=n;}if stage==0{assert_eq!(mask.iter().filter(|x|**x).count(),9);}}}
    #[test] fn tasks_are_inside_owned_cells(){for id in 0..32{let [x,z]=origin(id);for stage in 0..STAGES{for serial in 0..100{let l=Lesson::sample(id,stage,serial,0,false,false,123);for p in [l.start,l.goal]{assert!(p[0]>x+2.&&p[0]<x+14.&&p[2]>z+2.&&p[2]<z+14.);}}}}}
    #[test] fn exams_do_not_reuse_training_random_domain(){let a=Lesson::sample(0,1,5,0,false,false,1);let b=Lesson::sample(0,1,5,0,true,false,1);assert_ne!(a.goal,b.goal);}
    #[test] fn every_agent_must_qualify(){let mut c=Curriculum::new(2);c.scores[0].trained=40;assert_eq!(c.advance(),None);c.scores[1].trained=40;assert_eq!(c.advance(),Some("exam-start"));assert_eq!(c.stage,0);}
    #[test] fn exam_failure_never_promotes(){let mut c=Curriculum::new(2);c.phase=Phase::Exam;for s in &mut c.scores{s.current=16;s.current_success=16;s.review=4;s.review_success=4;s.retained[0]=4;}c.scores[1].current_success=13;assert_eq!(c.advance(),Some("exam-fail"));assert_eq!(c.stage,0);}
    #[test] fn retention_failure_never_promotes(){let mut c=Curriculum::new(1);c.phase=Phase::Exam;c.scores[0]=Score{current:16,current_success:16,review:4,review_success:2,..Score::default()};assert_eq!(c.advance(),Some("exam-fail"));}
    #[test] fn pass_advances_exactly_one_stage(){let mut c=Curriculum::new(1);c.phase=Phase::Exam;c.scores[0]=Score{current:16,current_success:14,review:4,review_success:3,retained:[3,0,0,0,0,0],..Score::default()};assert_eq!(c.advance(),Some("exam-pass"));assert_eq!(c.stage,1);}
    #[test] fn checkpoint_checks_policy_and_drops_exam_scores(){let mut c=Curriculum::new(2);c.stage=3;c.phase=Phase::Exam;c.serials[0]=79;let bytes=c.encode(9,15);let d=Curriculum::decode(&bytes,2,9,15).unwrap();assert_eq!(d.stage,3);assert_eq!(d.serials[0],79);assert_eq!(d.phase,Phase::Train);assert!(Curriculum::decode(&bytes,2,10,15).is_err());assert!(Curriculum::decode(&bytes,1,9,15).is_err());}
    #[test] fn every_previous_skill_is_examined() {
        let mut c=Curriculum::new(1);c.stage=5;c.phase=Phase::Exam;c.scores[0].current=16;
        let mut counts=[0;STAGES];
        while let Some(l)=c.begin(0,91){counts[l.stage]+=1;c.record(0,&l,true);}
        assert_eq!(counts,[4,4,4,4,4,0]);
    }
    #[test] fn training_review_does_not_alias_modulo_five() {
        let mut c=Curriculum::new(1);c.stage=5;let mut counts=[0;STAGES];
        for _ in 0..125{let l=c.begin(0,1).unwrap();if l.review{counts[l.stage]+=1;}}
        assert_eq!(counts,[5,5,5,5,5,0]);
    }
    #[test] fn aggregate_review_score_cannot_hide_one_lost_skill() {
        let mut c=Curriculum::new(1);c.stage=3;c.phase=Phase::Exam;
        c.scores[0]=Score{current:16,current_success:16,review:12,review_success:10,retained:[4,4,2,0,0,0],..Score::default()};
        assert_eq!(c.advance(),Some("exam-fail"));assert_eq!(c.stage,3);
    }
    fn frame(tick:u64,p:[f64;3])->Frame{Frame{tick,position:p,yaw:0.,pitch:0.,grounded:true,broken:false}}
    #[test] fn arrival_requires_settled_hold_not_drive_by(){let l=Lesson::sample(0,0,0,0,false,false,1);let mut e=Episode::new(frame(0,l.start));let r=e.step(0,&l,frame(4,l.goal),&IDLE,0.997);assert!(!r.done);for t in [8,12,16,20]{assert!(!e.step(0,&l,frame(t,l.goal),&IDLE,0.997).done);}assert!(e.step(0,&l,frame(24,l.goal),&IDLE,0.997).success);}
    #[test] fn looking_through_target_while_spinning_is_not_holding() {
        let mut l=Lesson::sample(0,2,0,0,false,false,1);l.start=[8.,97.,8.];l.goal=[8.,98.62,11.];
        let mut e=Episode::new(frame(0,l.start));let mut f=frame(4,l.start);f.yaw=2.;
        assert!(!e.step(0,&l,f,&IDLE,0.997).success);assert_eq!(e.held,0);
    }
    #[test] fn missing_samples_do_not_fake_continuous_hold(){let l=Lesson::sample(0,0,0,0,false,false,1);let mut e=Episode::new(frame(0,l.goal));assert!(!e.step(0,&l,frame(40,l.goal),&IDLE,0.997).success);assert_eq!(e.held,0);}
    #[test] fn timeout_and_escape_are_failures(){let l=Lesson::sample(0,0,0,0,false,false,1);let mut e=Episode::new(frame(0,l.start));let r=e.step(0,&l,frame(l.limit_ticks,l.start),&IDLE,0.997);assert!(r.done&&!r.success);assert_eq!(r.reason,"timeout");let mut e=Episode::new(frame(0,l.start));assert_eq!(e.step(0,&l,frame(4,[0.,97.,0.]),&IDLE,0.997).reason,"outside");}
    #[test] fn mine_requires_server_break_not_a_mine_action(){let l=Lesson::sample(0,5,0,0,false,false,1);let mut e=Episode::new(frame(0,l.start));let mut a=IDLE;a[4]=1;assert!(!e.step(0,&l,frame(4,l.start),&a,0.997).success);let mut f=frame(8,l.start);f.broken=true;assert!(e.step(0,&l,f,&a,0.997).success);}
}
