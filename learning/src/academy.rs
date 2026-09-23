//! Live-task geometry and server-evidence rewards. This module never selects an action.
use super::{curriculum as foundation, next::{self, curriculum::Lesson as Ticket, tasks::{Task, Evidence, ProgressGate}}, rng::Rng};
use std::ops::Deref;

pub const FEATURES:usize=87;
#[derive(Clone,Debug)]
pub struct Lesson {pub geometry:foundation::Lesson,pub ticket:Ticket}
impl Deref for Lesson {type Target=foundation::Lesson;fn deref(&self)->&Self::Target{&self.geometry}}
impl Lesson {
    pub fn new(ticket:Ticket)->Self {
        let id=ticket.session.actor as usize;let stage=ticket.task as usize;
        let [x,z]=foundation::origin(id);let mut rng=Rng(ticket.seed);
        let mut g=if stage<6 {foundation::Lesson::sample(id,stage,ticket.session.lesson,ticket.session.generation,ticket.evaluation,ticket.review,ticket.seed)}
        else {foundation::Lesson{stage,serial:ticket.session.lesson,generation:ticket.session.generation,exam:ticket.evaluation,review:ticket.review,
            start:[x+8.5,97.,z+7.5],goal:[x+8.5,97.5,z+11.5],yaw:0.,pitch:0.,limit_ticks:ticket.task.spec().limit_ticks as u64}};
        let difficulty=ticket.difficulty.clamp(0.2,1.0);
        if stage<6 && difficulty<1.0 {
            match stage {
                0=>{g.start[2]=g.goal[2]-(1.0+3.0*difficulty).min(g.goal[2]-(z+2.6));},
                1|3=>{let dx=g.goal[0]-g.start[0];let dz=g.goal[2]-g.start[2];let distance=(dx*dx+dz*dz).sqrt().max(0.1);
                    let fraction=(1.0+2.5*difficulty).min(distance)/distance;g.start[0]=g.goal[0]-dx*fraction;g.start[2]=g.goal[2]-dz*fraction;
                    let desired=foundation::angles(g.start,0.,0.,g.goal)[0];g.yaw=(desired+rng.symmetric((180.*difficulty) as f32)+180.).rem_euclid(360.)-180.;},
                2=>{let a=foundation::angles(g.start,0.,0.,g.goal);g.yaw=(a[0]+rng.symmetric((100.*difficulty) as f32)+180.).rem_euclid(360.)-180.;g.pitch=(a[1]+rng.symmetric((35.*difficulty) as f32)).clamp(-80.,80.);},
                4=>{g.start[0]=x+8.5;g.goal[0]=x+8.5;g.yaw=0.;},
                5=>{g.start=[x+8.5,97.,z+9.0-difficulty];g.yaw=rng.symmetric((25.*difficulty) as f32);g.pitch=0.;},_=>{}
            }
        }
        if stage>=6 {
            if stage==16{g.goal=[x+6.5,97.5,z+10.5];}
            if matches!(stage,8..=10){g.goal=[x+8.5,97.,z+8.5];g.start=g.goal;}
            else {g.start[0]=x+6.5+4.*rng.uniform() as f64;g.start[2]=z+6.0+2.0*rng.uniform() as f64;
                g.yaw=rng.symmetric((90.*difficulty) as f32);g.pitch=rng.symmetric((20.*difficulty) as f32);
                if difficulty<0.5 {g.start[0]=g.goal[0];g.start[2]=g.goal[2]-2.0;g.yaw=0.;}}
        }
        g.limit_ticks=ticket.task.spec().limit_ticks as u64;
        Self{geometry:g,ticket}
    }
    pub fn restrict(&self,mask:&mut [bool]) {
        let task=self.ticket.task.mask();let mut off=0;
        for head in task {for enabled in head {mask[off]&=enabled;off+=1;}}
        assert_eq!(off,mask.len());
    }
}
#[derive(Clone,Debug)]
pub struct Frame {pub tick:u64,pub position:[f64;3],pub yaw:f32,pub pitch:f32,pub grounded:bool,pub evidence:Evidence}
#[derive(Clone,Debug)]
pub struct Outcome {pub reward:f32,pub done:bool,pub success:bool,pub reason:&'static str,pub ticks:u32}
#[derive(Clone,Debug)]
pub struct Episode {
    pub first_tick:u64,pub previous:Frame,pub held:u32,pub rotation:f64,pub switches:u64,pub decisions:u64,
    pub speed:f32,pub angular_speed:f32,previous_action:[usize;8],gate:ProgressGate,
}
fn distance(a:[f64;3],b:[f64;3])->f64{((a[0]-b[0]).powi(2)+(a[2]-b[2]).powi(2)).sqrt()}
fn potential(l:&Lesson,f:&Frame)->f64 {
    match l.ticket.task {
        Task::AimHold|Task::BreakLog|Task::CollectLog|Task::MineCobblestone=>{
            let a=foundation::angles(f.position,f.yaw,f.pitch,l.goal);
            -0.003*(a[0].abs()+a[1].abs()) as f64 -0.03*distance(f.position,l.goal).min(12.)
        },
        Task::ForwardStop|Task::TurnStop|Task::NavigateStop|Task::StepOver=>-0.15*distance(f.position,l.goal).min(20.),
        // Bounded, real intermediate achievements; discounted potential differences
        // telescope instead of repeatedly paying for juggling the same material.
        Task::LogToWorkbench=>0.15*f.evidence.counters.broken.min(1) as f64+0.15*f.evidence.counters.picked_up.min(1) as f64
            +0.15*(f.evidence.stock[1].min(4) as f64/4.),
        Task::BuildPlatform=>0.1*(f.evidence.occupied_targets&7).count_ones() as f64,
        _=>0.0,
    }
}
impl Episode {
    pub fn new(l:&Lesson,f:Frame)->next::Result<Self>{
        Ok(Self{first_tick:f.tick,gate:ProgressGate::new(l.ticket.task,&f.evidence)?,previous:f,held:0,rotation:0.,switches:0,decisions:0,
            speed:0.,angular_speed:0.,previous_action:foundation::IDLE})
    }
    pub fn step(&mut self,id:usize,l:&Lesson,mut f:Frame,action:&[usize],gamma:f32)->next::Result<Outcome>{
        let dt=f.tick.checked_sub(self.previous.tick).ok_or("nonmonotonic server time")?;
        if dt==0||dt>1_000_000||action.len()!=8{return Err("invalid episode transition");}
        let turn=((f.yaw-self.previous.yaw+180.).rem_euclid(360.)-180.).abs();
        self.speed=(distance(f.position,self.previous.position)/dt as f64) as f32;
        self.angular_speed=(turn+(f.pitch-self.previous.pitch).abs())/dt as f32;
        let [x,z]=foundation::origin(id);
        let outside=f.position[0]<x+1.7||f.position[0]>x+14.3||f.position[2]<z+1.7||f.position[2]>z+14.3||f.position[1]<96.5||f.position[1]>103.;
        let a=foundation::angles(f.position,f.yaw,f.pitch,l.goal);
        let settled=if l.ticket.task==Task::AimHold {a[0].abs()<=8.&&a[1].abs()<=8.&&self.angular_speed<=0.15}
            else{distance(f.position,l.goal)<=0.65&&self.speed<=0.025&&self.angular_speed<=0.15&&f.grounded};
        self.held=if settled&&dt<=8{self.held.saturating_add(dt as u32)}else{0};
        f.evidence.motor_hold_ticks=self.held;
        let success=self.gate.observe(&f.evidence)?&&!outside;
        let timeout=f.tick.saturating_sub(self.first_tick)>=l.limit_ticks;
        let done=success||outside||timeout;
        let shaping=next::math::potential_shaping(potential(l,&self.previous),potential(l,&f),gamma as f64,dt as u32,4,
            if done{next::math::Boundary::Terminated}else{next::math::Boundary::Continuing})?;
        let switched=action[0]!=self.previous_action[0];self.switches+=switched as u64;self.rotation+=turn as f64;
        let reward=(if success{2.0}else if done{-1.0}else{0.0})+shaping-0.002*(dt as f64/4.0)-0.00005*turn as f64-if switched{0.003}else{0.0};
        if !reward.is_finite(){return Err("nonfinite episode reward");}
        self.decisions+=1;self.previous_action.copy_from_slice(action);self.previous=f;
        Ok(Outcome{reward:reward as f32,done,success,reason:if success{"success"}else if outside{"outside"}else if timeout{"timeout"}else{"running"},ticks:dt as u32})
    }
    pub fn features(&self,l:&Lesson,f:&Frame)->[f32;FEATURES]{
        let mut v=[0.;FEATURES];let a=foundation::angles(f.position,f.yaw,f.pitch,l.goal);
        v[0]=((l.goal[0]-f.position[0])/12.) as f32;v[1]=((l.goal[1]-f.position[1])/8.) as f32;v[2]=((l.goal[2]-f.position[2])/12.) as f32;
        v[3]=a[0].to_radians().sin();v[4]=a[0].to_radians().cos();v[5]=a[1]/180.;v[6+l.stage]=1.;
        v[24]=(f.tick.saturating_sub(self.first_tick) as f32/l.limit_ticks as f32).min(1.);v[25]=(self.held as f32/20.).min(1.);v[26]=f.grounded as u8 as f32;
        v[27]=f.yaw.to_radians().sin();v[28]=f.yaw.to_radians().cos();v[29]=f.pitch/90.;v[30]=l.ticket.difficulty as f32;
        for (i,&n) in f.evidence.stock.iter().enumerate(){v[31+i]=(n as f32/64.).min(1.);}
        let c=f.evidence.counters;for (i,n) in [c.broken,c.picked_up,c.crafted,c.placed,c.smelted,c.deposited].iter().enumerate(){v[44+i]=(*n as f32/16.).min(1.);}
        v[50]=(f.evidence.target_stock as f32/64.).min(1.);for i in 0..3{v[51+i]=((f.evidence.occupied_targets>>i)&1) as f32;}
        v[54]=(self.speed/0.3).min(1.);v[55]=(self.angular_speed/4.5).min(1.);v[56]=1.;
        for x in &mut v{*x=x.clamp(-1.,1.);}v
    }
}

#[cfg(test)] mod tests {
    use super::*;use super::super::next::{curriculum::PolicyId,tasks::{TASKS,Session,Counters}};
    fn lesson(id:usize,stage:usize,difficulty:f64)->Lesson{Lesson::new(Ticket{session:Session{run:1,actor:id as u8,generation:1,lesson:1},task:TASKS[stage],difficulty,seed:81,full_probe:difficulty==1.,evaluation:false,review:false,policy:PolicyId{version:0,signature:1}})}
    fn frame(l:&Lesson,t:u64,p:[f64;3])->Frame{Frame{tick:t,position:p,yaw:0.,pitch:0.,grounded:true,evidence:Evidence{session:l.ticket.session,tick:t,stock:[0;13],counters:Counters::default(),target_stock:0,occupied_targets:0,motor_hold_ticks:0}}}
    #[test] fn all_sixty_four_rooms_and_eighteen_tasks_have_valid_goals(){
        for id in 0..64{for stage in 0..18{for d in [0.2,0.5,1.0]{let l=lesson(id,stage,d);let [x,z]=foundation::origin(id);
            assert!(l.start[0]>x+2.&&l.start[0]<x+14.&&l.start[2]>z+2.&&l.start[2]<z+14.);assert_eq!(l.start[1],97.);
            assert!(l.goal[0]>x+2.&&l.goal[0]<x+14.&&l.goal[2]>z+2.&&l.goal[2]<z+14.);assert!(l.limit_ticks<=3000);
            let e=Episode::new(&l,frame(&l,4,l.start)).unwrap();assert_eq!(e.features(&l,&e.previous).iter().filter(|x|!x.is_finite()).count(),0);
        }}}
    }
    #[test] fn drive_by_and_gaps_do_not_complete_motor_tasks(){
        let l=lesson(63,0,1.);let mut e=Episode::new(&l,frame(&l,4,l.start)).unwrap();
        assert!(!e.step(63,&l,frame(&l,8,l.goal),&foundation::IDLE,0.997).unwrap().success);
        assert!(!e.step(63,&l,frame(&l,40,l.goal),&foundation::IDLE,0.997).unwrap().success);
        for t in [44,48,52,56]{assert!(!e.step(63,&l,frame(&l,t,l.goal),&foundation::IDLE,0.997).unwrap().success);}
        assert!(e.step(63,&l,frame(&l,60,l.goal),&foundation::IDLE,0.997).unwrap().success);
    }
    #[test] fn crafting_needs_both_committed_event_and_acquired_stock(){
        let l=lesson(63,8,1.);let mut e=Episode::new(&l,frame(&l,4,l.start)).unwrap();let mut f=frame(&l,8,l.start);f.evidence.stock[1]=4;
        assert!(!e.step(63,&l,f,&foundation::IDLE,0.997).unwrap().success);
        let mut f=frame(&l,12,l.start);f.evidence.stock[1]=4;f.evidence.counters.crafted=4;
        assert!(e.step(63,&l,f,&foundation::IDLE,0.997).unwrap().success);
    }
    #[test] fn timeouts_are_true_finite_horizon_terminals(){let l=lesson(0,14,1.);let mut e=Episode::new(&l,frame(&l,4,l.start)).unwrap();let r=e.step(0,&l,frame(&l,3004,l.start),&foundation::IDLE,0.997).unwrap();assert!(r.done&&!r.success);assert_eq!(r.ticks,3000);}
}
