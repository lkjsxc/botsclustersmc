//! Minecraft task geometry and rewards. This module never chooses an action.
//! Adaptive issuance, frozen exams and task evidence gates live in `next`.
use super::{HEADS,ACTIONS,next::{Rng,math::{potential_shaping,Boundary},tasks::{Evidence,ProgressGate,ITEM_COUNT}}};
pub use super::next::curriculum::{Curriculum,Phase};
pub const STAGES:usize=18;
pub const IDLE:[usize;8]=[0,3,2,0,0,0,0,0];
pub const YAW_RATES:[f32;7]=[-90.,-30.,-10.,0.,10.,30.,90.];
pub const PITCH_RATES:[f32;5]=[-45.,-15.,0.,15.,45.];
pub const NAMES:[&str;STAGES]=["forward-stop","turn-and-stop","look-at-target","navigate-and-stop","step-over","mine-target","collect-log","place-block","craft-planks","craft-sticks","craft-workbench","craft-wooden-pickaxe","mine-cobblestone","craft-stone-pickaxe","smelt-iron","supply-chest","build-platform","log-to-workbench"];
pub fn origin(id:usize)->[f64;2]{assert!(id<64);[((id%8)*16) as f64,((id/8)*16) as f64]}
#[derive(Clone,Debug)]
pub struct Lesson{
    pub choice:super::next::curriculum::Lesson,
    pub stage:usize,pub serial:u64,pub generation:u64,pub exam:bool,pub review:bool,
    pub start:[f64;3],pub goal:[f64;3],pub yaw:f32,pub pitch:f32,pub limit_ticks:u64,
}
fn between(r:&mut Rng,lo:f64,hi:f64)->f64{lo+(hi-lo)*r.unit()}
impl Lesson{
    pub fn from_choice(choice:super::next::curriculum::Lesson)->Self{
        let id=choice.session.actor as usize;let stage=choice.task as usize;
        assert!(id<64 && stage<STAGES);
        let [x,z]=origin(id);let d=choice.difficulty;let mut r=Rng(choice.seed);
        let mut l=Self{stage,serial:choice.session.lesson,generation:choice.session.generation,exam:choice.evaluation,review:choice.review,
            start:[x+7.5,97.,z+5.5],goal:[x+7.5,97.,z+8.5],yaw:0.,pitch:0.,limit_ticks:choice.task.spec().limit_ticks as u64,choice};
        match stage{
            0=>{l.start[0]=x+between(&mut r,5.,10.).floor()+0.5;l.goal[0]=l.start[0];l.goal[2]=z+6.5+between(&mut r,1.,3.5)*d;},
            1|3=>{l.start=[x+7.5,97.,z+7.5];let a=between(&mut r,0.,std::f64::consts::TAU);let n=between(&mut r,2.,4.5)*d.max(0.5);l.goal=[x+7.5+a.cos()*n,97.,z+7.5+a.sin()*n];l.yaw=angles(l.start,0.,0.,l.goal)[0]+between(&mut r,-180.*d,180.*d) as f32;},
            2=>{l.start=[x+7.5,97.,z+7.5];let a=between(&mut r,0.,std::f64::consts::TAU);l.goal=[(x+7.5+a.cos()*4.).floor()+0.5,between(&mut r,97.,101.).floor()+0.5,(z+7.5+a.sin()*4.).floor()+0.5];let a=angles(l.start,0.,0.,l.goal);l.yaw=a[0]+between(&mut r,-180.*d,180.*d) as f32;l.pitch=a[1]+between(&mut r,-20.*d,20.*d) as f32;},
            4=>{l.start=[x+between(&mut r,6.,9.),97.,z+6.5-2.*d];l.goal=[x+between(&mut r,6.,9.),97.,z+11.5];l.yaw=between(&mut r,-20.*d,20.*d) as f32;},
            5|6|12|17=>{l.start=[x+8.5+between(&mut r,-1.,1.)*d,97.,z+9.5-2.*d];l.goal=[x+8.5,98.5,z+11.5];l.yaw=between(&mut r,-25.*d,25.*d) as f32;},
            8..=10=>{l.start=[x+7.5,97.,z+7.5];l.goal=l.start;},
            _=>{l.goal=[x+8.5,97.5,z+10.5];l.start=[x+8.5+between(&mut r,-1.,1.)*d,97.,z+8.5-d];let a=angles(l.start,0.,0.,l.goal);l.yaw=a[0]+between(&mut r,-45.*d,45.*d) as f32;l.pitch=a[1]+between(&mut r,-15.*d,15.*d) as f32;}
        }
        l.goal[0]=l.goal[0].floor()+0.5;l.goal[2]=l.goal[2].floor()+0.5;
        l.yaw=(l.yaw+180.).rem_euclid(360.)-180.;l.pitch=l.pitch.clamp(-80.,80.);l
    }
    pub fn token(&self)->String{format!("{}-{}-{}",self.generation,self.choice.session.actor,self.serial)}
}
pub fn restrict(mask:&mut[bool],stage:usize){
    assert!(stage<STAGES && mask.len()==ACTIONS);
    let task=super::next::tasks::TASKS[stage];let availability=task.mask();let mut off=0;
    for (h,&n) in HEADS.iter().enumerate(){for a in 0..n{mask[off+a]&=availability[h][a];}off+=n;}
}
pub fn angles(p:[f64;3],yaw:f32,pitch:f32,g:[f64;3])->[f32;2]{
    let dx=g[0]-p[0];let dz=g[2]-p[2];let dy=g[1]-(p[1]+1.62);
    let y=(-dx).atan2(dz).to_degrees() as f32;let t=(-dy).atan2((dx*dx+dz*dz).sqrt()).to_degrees() as f32;
    [(y-yaw+180.).rem_euclid(360.)-180.,(t-pitch).clamp(-180.,180.)]
}
#[derive(Clone,Debug)]
pub struct Frame{pub tick:u64,pub position:[f64;3],pub yaw:f32,pub pitch:f32,pub grounded:bool,pub evidence:Evidence}
#[derive(Clone,Debug)]
pub struct Episode{
    pub first_tick:u64,pub previous:Frame,pub held:u64,pub rotation:f64,pub switches:u64,pub decisions:u64,pub speed:f32,pub angular_speed:f32,
    previous_action:[usize;8],gate:ProgressGate,
}
#[derive(Clone,Debug)]
pub struct Outcome{pub reward:f32,pub done:bool,pub success:bool,pub reason:&'static str,pub elapsed_ticks:u32,pub tick:u64}
fn distance(p:[f64;3],g:[f64;3])->f64{((p[0]-g[0]).powi(2)+(p[2]-g[2]).powi(2)).sqrt()}
fn potential(l:&Lesson,f:&Frame)->f64{
    let angular=||{let a=angles(f.position,f.yaw,f.pitch,l.goal);-0.003*(a[0].abs()+a[1].abs()) as f64};
    match l.stage{
        2|5=>angular(),
        0|1|3|4=>-0.15*distance(f.position,l.goal).min(20.),
        6|12|17=>angular()-0.03*distance(f.position,l.goal).min(20.)+0.1*f.evidence.counters.broken.min(1) as f64+0.2*f.evidence.counters.picked_up.min(1) as f64,
        7|16=>angular()+0.1*f.evidence.occupied_targets.count_ones() as f64,
        _=>0.0,
    }
}
impl Episode{
    pub fn new(l:&Lesson,f:Frame)->Result<Self,String>{
        if f.evidence.session!=l.choice.session || f.evidence.tick!=f.tick{return Err("foreign episode baseline".into());}
        let gate=ProgressGate::new(l.choice.task,&f.evidence).map_err(str::to_string)?;
        Ok(Self{first_tick:f.tick,previous:f,held:0,rotation:0.,switches:0,decisions:0,speed:0.,angular_speed:0.,previous_action:IDLE,gate})
    }
    pub fn step(&mut self,id:usize,l:&Lesson,mut f:Frame,action:&[usize],gamma:f32)->Result<Outcome,String>{
        let dt=f.tick.checked_sub(self.previous.tick).filter(|n|*n>0&&*n<=1_000_000).ok_or("invalid elapsed server ticks")?;
        if id!=l.choice.session.actor as usize || action.len()!=8 || f.evidence.tick!=f.tick{return Err("invalid episode action/frame".into());}
        let speed=distance(f.position,self.previous.position)/dt as f64;
        let turn=((f.yaw-self.previous.yaw+180.).rem_euclid(360.)-180.).abs();
        let angular_speed=(turn+(f.pitch-self.previous.pitch).abs())/dt as f32;
        let [x,z]=origin(id);
        let outside=f.position[0]<x+1.7||f.position[0]>x+14.3||f.position[2]<z+1.7||f.position[2]>z+14.3||f.position[1]<96.5||f.position[1]>103.;
        let a=angles(f.position,f.yaw,f.pitch,l.goal);
        let ready=if l.stage==2{a[0].abs()<=8.&&a[1].abs()<=8.&&angular_speed<=0.15}
            else{distance(f.position,l.goal)<=0.65&&speed<=0.025&&angular_speed<=0.15&&f.grounded};
        let held=if ready&&dt<=8{self.held+dt}else{0};
        f.evidence.motor_hold_ticks=held.min(u32::MAX as u64) as u32;
        let mut gate=self.gate.clone();let reached=gate.observe(&f.evidence).map_err(str::to_string)?;
        let success=!outside&&reached;let timeout=f.tick-self.first_tick>=l.limit_ticks;
        let done=success||outside||timeout;
        let shaping=potential_shaping(potential(l,&self.previous),potential(l,&f),gamma as f64,dt as u32,4,
            if done{Boundary::Terminated}else{Boundary::Continuing}).map_err(str::to_string)?;
        let switched=action[0]!=self.previous_action[0];
        let reward=(if success{2.0}else if done{-1.0}else{0.0})+shaping-0.002*dt as f64/4.-0.00005*turn as f64-if switched{0.003}else{0.0};
        if !reward.is_finite(){return Err("nonfinite curriculum reward".into());}
        self.gate=gate;self.held=held;self.speed=speed as f32;self.angular_speed=angular_speed;self.rotation+=turn as f64;
        self.switches+=switched as u64;self.decisions+=1;self.previous_action.copy_from_slice(action);let tick=f.tick;self.previous=f;
        Ok(Outcome{reward:reward as f32,done,success,reason:if success{"success"}else if outside{"outside"}else if timeout{"timeout"}else{"running"},elapsed_ticks:dt as u32,tick})
    }
    /// Goal/state instrumentation, not demonstrations or a teacher action.
    /// The policy sees identical features in training and exams; no exam flag.
    pub fn features(&self,l:&Lesson,f:&Frame)->[f32;87]{
        let mut v=[0.;87];let a=angles(f.position,f.yaw,f.pitch,l.goal);
        v[0]=((l.goal[0]-f.position[0])/12.) as f32;v[1]=((l.goal[1]-f.position[1])/8.) as f32;v[2]=((l.goal[2]-f.position[2])/12.) as f32;
        v[3]=a[0].to_radians().sin();v[4]=a[0].to_radians().cos();v[5]=a[1]/180.;v[6+l.stage]=1.;
        v[24]=(f.tick-self.first_tick) as f32/l.limit_ticks as f32;v[25]=self.held as f32/20.;v[26]=f.grounded as u8 as f32;
        v[27]=f.yaw.to_radians().sin();v[28]=f.yaw.to_radians().cos();v[29]=f.pitch/90.;v[30]=self.speed/0.3;v[31]=self.angular_speed/4.5;
        v[32]=l.choice.difficulty as f32;
        for i in 0..ITEM_COUNT{v[33+i]=f.evidence.stock[i] as f32/64.;}
        let c=f.evidence.counters;for(i,n)in[c.broken,c.picked_up,c.crafted,c.placed,c.smelted,c.deposited].iter().enumerate(){v[46+i]=*n as f32/8.;}
        v[52]=f.evidence.target_stock as f32/64.;for i in 0..3{v[53+i]=((f.evidence.occupied_targets>>i)&1) as f32;}
        if let Some((item,n))=l.choice.task.craft_output(){v[56+item as usize]=1.;v[69]=n as f32/4.;}
        for x in &mut v{*x=x.clamp(-1.,1.);}v
    }
}
#[cfg(test)] mod tests{
    use super::*;use super::super::next::{tasks::{Session,Counters,TASKS},curriculum::{Lesson as Choice,PolicyId}};
    fn lesson(id:usize,stage:usize,seed:u64,d:f64)->Lesson{Lesson::from_choice(Choice{session:Session{run:1,actor:id as u8,generation:1,lesson:seed},task:TASKS[stage],difficulty:d,seed,full_probe:d==1.,evaluation:false,review:false,policy:PolicyId{version:0,signature:1}})}
    fn frame(l:&Lesson,tick:u64,p:[f64;3])->Frame{Frame{tick,position:p,yaw:l.yaw,pitch:l.pitch,grounded:true,evidence:Evidence{session:l.choice.session,tick,stock:[0;ITEM_COUNT],counters:Counters::default(),target_stock:0,occupied_targets:0,motor_hold_ticks:0}}}
    #[test]fn all_18_tasks_and_64_cells_have_finite_bounded_geometry(){for id in 0..64{for stage in 0..STAGES{for seed in 0..20{for d in [0.2,0.5,1.]{let l=lesson(id,stage,seed,d);let[x,z]=origin(id);for p in [l.start,l.goal]{assert!((x+2.0..=x+13.5).contains(&p[0]));assert!((z+2.0..=z+13.5).contains(&p[2]));assert!((97.0..=101.5).contains(&p[1]));}assert!(l.yaw.is_finite()&&l.pitch.is_finite());assert!(l.limit_ticks<=3000);}}}}}
    #[test]fn every_task_mask_has_legal_actions_and_advanced_gui_access(){for stage in 0..STAGES{let mut m=vec![true;ACTIONS];restrict(&mut m,stage);let mut off=0;for n in HEADS{assert!(m[off..off+n].iter().any(|v|*v));off+=n;}if stage>=6{assert!(m[ACTIONS-90..].iter().all(|v|*v));}}}
    #[test]fn arrival_requires_settled_hold_not_drive_by(){let l=lesson(0,0,3,1.);let mut e=Episode::new(&l,frame(&l,0,l.start)).unwrap();assert!(!e.step(0,&l,frame(&l,4,l.goal),&IDLE,0.997).unwrap().success);for t in [8,12,16,20]{assert!(!e.step(0,&l,frame(&l,t,l.goal),&IDLE,0.997).unwrap().success);}assert!(e.step(0,&l,frame(&l,24,l.goal),&IDLE,0.997).unwrap().success);}
    #[test]fn missing_snapshots_never_invent_continuous_holding(){let l=lesson(0,0,3,1.);let mut e=Episode::new(&l,frame(&l,0,l.goal)).unwrap();let r=e.step(0,&l,frame(&l,40,l.goal),&IDLE,0.997).unwrap();assert!(!r.success);assert_eq!(e.held,0);assert_eq!(r.elapsed_ticks,40);}
    #[test]fn rotation_through_aim_is_not_a_hold(){let l=lesson(0,2,8,1.);let mut a=frame(&l,0,l.start);let angles=angles(l.start,0.,0.,l.goal);a.yaw=angles[0]-60.;a.pitch=angles[1];let mut e=Episode::new(&l,a).unwrap();let mut f=frame(&l,4,l.start);f.yaw=angles[0];f.pitch=angles[1];assert!(!e.step(0,&l,f,&IDLE,0.997).unwrap().success);assert_eq!(e.held,0);}
    #[test]fn action_alone_does_not_prove_a_server_break(){let l=lesson(0,5,9,1.);let mut e=Episode::new(&l,frame(&l,0,l.start)).unwrap();let mut action=IDLE;action[4]=1;assert!(!e.step(0,&l,frame(&l,4,l.start),&action,0.997).unwrap().success);let mut f=frame(&l,8,l.start);f.evidence.counters.broken=1;assert!(e.step(0,&l,f,&action,0.997).unwrap().success);}
    #[test]fn stale_evidence_does_not_mutate_episode(){let l=lesson(63,6,7,1.);let mut e=Episode::new(&l,frame(&l,0,l.start)).unwrap();let mut f=frame(&l,4,l.start);f.evidence.session.lesson+=1;assert!(e.step(63,&l,f,&IDLE,0.997).is_err());assert_eq!(e.previous.tick,0);}
    #[test]fn timeout_and_escape_are_not_success(){let l=lesson(0,0,9,1.);let mut e=Episode::new(&l,frame(&l,0,l.start)).unwrap();let r=e.step(0,&l,frame(&l,600,l.start),&IDLE,0.997).unwrap();assert!(r.done&&!r.success);assert_eq!(r.reason,"timeout");let mut e=Episode::new(&l,frame(&l,0,l.start)).unwrap();let r=e.step(0,&l,frame(&l,4,[0.,97.,0.]),&IDLE,0.997).unwrap();assert!(r.done&&!r.success);assert_eq!(r.reason,"outside");}
    #[test]fn features_fit_new_frame_and_do_not_reveal_an_exam_flag(){let l=lesson(0,17,2,1.);let f=frame(&l,0,l.start);let e=Episode::new(&l,f.clone()).unwrap();let a=e.features(&l,&f);let mut exam=l.clone();exam.exam=true;exam.choice.evaluation=true;assert_eq!(a,e.features(&exam,&f));assert_eq!(617+a.len(),super::super::FRAME);assert!(a.iter().all(|v|v.is_finite()&&v.abs()<=1.));}
}
