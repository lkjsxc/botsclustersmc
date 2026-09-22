//! An explicitly custom, Syllabus/learning-progress-inspired scheduler.
//! This is not a reproduction of Syllabus, RePPO, Dreamer or DiscoRL.
//! Exam outcomes are curriculum validation, not an unbiased final benchmark.
mod checkpoint;
use crate::{Result,Rng,tasks::{Task,TASK_COUNT,Session}};

pub const MIN_TRAIN_EPISODES:u64=40;
pub const MIN_FULL_PROBES:u64=8;
pub const CURRENT_EXAM_TRIALS:u32=16;
pub const REVIEW_EXAM_TRIALS:u32=4;
pub const DIFFICULTY_FLOOR:f64=0.2;

#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub struct PolicyId {pub version:u64,pub signature:u64}
#[derive(Clone,Copy,Debug,PartialEq,Eq)]
pub enum Phase {Training,Exam{frozen:PolicyId}}
#[derive(Clone,Debug,PartialEq)]
pub struct Lesson {
    pub session:Session,
    pub task:Task,
    pub difficulty:f64,
    pub seed:u64,
    pub full_probe:bool,
    pub evaluation:bool,
    pub review:bool,
    pub policy:PolicyId,
}
#[derive(Clone,Copy,Debug,PartialEq)]
pub struct SkillStats {
    pub attempts:u64,
    pub fast_success:f64,
    pub slow_success:f64,
    pub difficulty:f64,
    pub full_attempts:u64,
    pub full_success:f64,
    pub validation_weakness:f64,
    window_attempts:u64,
    window_successes:u64,
}
impl Default for SkillStats {
    fn default()->Self {Self{attempts:0,fast_success:0.5,slow_success:0.5,difficulty:0.25,full_attempts:0,
        full_success:0.5,validation_weakness:0.0,window_attempts:0,window_successes:0}}
}
impl SkillStats {
    pub fn priority(self)->f64 {
        // A positive floor and mandatory round-robin draws prevent starvation.
        0.05+4.0*(self.fast_success-self.slow_success).abs()+0.5*(1.0-self.fast_success)+2.0*self.validation_weakness
    }
    fn observe(self,success:bool,full:bool)->Result<Self>{
        let mut s=self;let y=success as u8 as f64;
        s.attempts=s.attempts.checked_add(1).ok_or("skill episode counter exhausted")?;
        s.fast_success+=0.12*(y-s.fast_success);s.slow_success+=0.025*(y-s.slow_success);
        s.window_attempts+=1;s.window_successes+=success as u64;
        if full {s.full_attempts=s.full_attempts.checked_add(1).ok_or("full probe counter exhausted")?;s.full_success+=0.12*(y-s.full_success);}
        if s.window_attempts>=20 {
            let rate=s.window_successes as f64/s.window_attempts as f64;
            if rate>=0.8 {s.difficulty=(s.difficulty+0.1).min(1.0);}
            else if rate<=0.2 {s.difficulty=(s.difficulty-0.1).max(DIFFICULTY_FLOOR);}
            s.window_attempts=0;s.window_successes=0;
        }
        Ok(s)
    }
    fn valid(self)->bool {
        [self.fast_success,self.slow_success,self.full_success,self.validation_weakness].iter().all(|x|x.is_finite() && (0.0..=1.0).contains(x)) &&
            self.difficulty.is_finite() && (DIFFICULTY_FLOOR..=1.0).contains(&self.difficulty) &&
            self.window_attempts<20 && self.window_successes<=self.window_attempts && self.full_attempts<=self.attempts
    }
}
#[derive(Clone,Copy,Debug,Default,PartialEq,Eq)]
pub struct ExamScore {pub trials:u32,pub successes:u32}
#[derive(Clone,Debug)]
struct Actor {
    serial:u64,
    training_choices:u64,
    frontier_choices:u64,
    trained_since_exam:u64,
    full_since_exam:u64,
    skills:[SkillStats;TASK_COUNT],
    scores:[ExamScore;TASK_COUNT],
    exam_cursor:u32,
    pending:Option<Lesson>,
}
impl Default for Actor {fn default()->Self{Self{serial:0,training_choices:0,frontier_choices:0,trained_since_exam:0,full_since_exam:0,
    skills:[SkillStats::default();TASK_COUNT],scores:[ExamScore::default();TASK_COUNT],exam_cursor:0,pending:None}}}
#[derive(Clone,Debug)]
pub struct Curriculum {
    seed:u64,
    run:u64,
    frontier:usize,
    generation:u64,
    phase:Phase,
    actors:Vec<Actor>,
    examinations:u64,
    ever_completed:bool,
    last_exam_passed:bool,
}
impl Curriculum {
    pub fn new(seed:u64,bots:usize,run:u64)->Result<Self>{
        if !(1..=32).contains(&bots){return Err("curriculum supports one to 32 actors");}
        Ok(Self{seed,run,frontier:0,generation:1,phase:Phase::Training,actors:vec![Actor::default();bots],
            examinations:0,ever_completed:false,last_exam_passed:false})
    }
    pub fn frontier(&self)->usize{self.frontier}
    pub fn generation(&self)->u64{self.generation}
    pub fn phase(&self)->Phase{self.phase}
    pub fn bots(&self)->usize{self.actors.len()}
    pub fn examinations(&self)->u64{self.examinations}
    pub fn ever_completed(&self)->bool{self.ever_completed}
    pub fn last_exam_passed(&self)->bool{self.last_exam_passed}
    pub fn stats(&self,actor:usize)->Result<&[SkillStats;TASK_COUNT]>{Ok(&self.actors.get(actor).ok_or("unknown actor")?.skills)}
    pub fn scores(&self,actor:usize)->Result<&[ExamScore;TASK_COUNT]>{Ok(&self.actors.get(actor).ok_or("unknown actor")?.scores)}
    pub fn pending(&self,actor:usize)->Result<Option<&Lesson>>{Ok(self.actors.get(actor).ok_or("unknown actor")?.pending.as_ref())}
    pub fn exam_trials_per_actor(&self)->u32{CURRENT_EXAM_TRIALS+REVIEW_EXAM_TRIALS*self.frontier as u32}
    pub fn exam_ready(&self)->bool{
        self.phase==Phase::Training && self.actors.iter().all(|a|a.trained_since_exam>=MIN_TRAIN_EPISODES &&
            a.full_since_exam>=MIN_FULL_PROBES && a.skills[self.frontier].full_success>=0.70)
    }
    /// Called only at a drained learner/cohort boundary; no live training lesson
    /// may be silently cancelled to begin an exam.
    pub fn begin_exam(&mut self,policy:PolicyId)->Result<()> {
        if !self.exam_ready() {return Err("not ready for a full-difficulty exam");}
        if self.actors.iter().any(|a|a.pending.is_some()){return Err("cannot freeze exam while a lesson is in flight");}
        let generation=self.generation.checked_add(1).ok_or("generation exhausted")?;
        self.phase=Phase::Exam{frozen:policy};self.generation=generation;
        for a in &mut self.actors{a.exam_cursor=0;a.scores.fill(ExamScore::default());}
        Ok(())
    }
    pub fn issue(&mut self,id:usize,policy:PolicyId)->Result<Option<Lesson>>{
        let mut a=self.actors.get(id).ok_or("unknown actor")?.clone();
        if a.pending.is_some(){return Err("actor already owns an uncompleted lesson");}
        let evaluation=matches!(self.phase,Phase::Exam{..});
        if let Phase::Exam{frozen}=self.phase {if frozen!=policy{return Err("exam policy changed");}}
        if evaluation && a.exam_cursor==self.exam_trials_per_actor(){return Ok(None);}
        if evaluation && a.exam_cursor>self.exam_trials_per_actor(){return Err("invalid exam cursor");}
        let serial=a.serial.checked_add(1).ok_or("lesson serial exhausted")?;
        let mut rng=Rng(self.seed ^ (id as u64).wrapping_mul(7919) ^ serial.wrapping_mul(0x9e3779b97f4a7c15) ^ self.generation);
        let (task_index,full)=if evaluation {
            (if a.exam_cursor<CURRENT_EXAM_TRIALS{self.frontier}else{((a.exam_cursor-CURRENT_EXAM_TRIALS)/REVIEW_EXAM_TRIALS) as usize},true)
        }else{
            let review=self.frontier>0 && a.training_choices%5==4;
            let chosen=if review {
                let review_index=a.training_choices/5;
                if review_index%2==0 {((review_index/2+id as u64)%self.frontier as u64) as usize}
                else {let weights=a.skills[..self.frontier].iter().map(|s|s.priority()).collect::<Vec<_>>();rng.weighted(&weights)?}
            }else{self.frontier};
            let full=if chosen==self.frontier {
                let probe=a.frontier_choices%5==4;
                a.frontier_choices=a.frontier_choices.checked_add(1).ok_or("frontier choice counter exhausted")?;probe
            }else{rng.unit()<0.5};
            a.training_choices=a.training_choices.checked_add(1).ok_or("training choice counter exhausted")?;
            (chosen,full)
        };
        let task=Task::from_index(task_index)?;
        let difficulty=if full{1.0}else{a.skills[task_index].difficulty};
        // Distinct seed domains, not a claim that finite rooms can never look alike.
        let seed=(rng.next_u64() & !(1u64<<63)) | ((evaluation as u64)<<63);
        let lesson=Lesson{session:Session{run:self.run,actor:id as u8,generation:self.generation,lesson:serial},
            task,difficulty,seed,full_probe:full,evaluation,review:task_index!=self.frontier,policy};
        a.serial=serial;a.pending=Some(lesson.clone());self.actors[id]=a;Ok(Some(lesson))
    }
    /// Complete exactly the issued lesson with the behaviour/frozen policy ID.
    /// Exam samples must never be sent to the PPO collector, regardless of outcome.
    pub fn record(&mut self,lesson:&Lesson,policy:PolicyId,success:bool)->Result<()> {
        let id=lesson.session.actor as usize;let mut a=self.actors.get(id).ok_or("unknown actor")?.clone();
        if a.pending.as_ref()!=Some(lesson) || lesson.session.run!=self.run || lesson.session.generation!=self.generation {
            return Err("duplicate, foreign or stale lesson result");
        }
        if lesson.policy!=policy {return Err("lesson behaviour policy changed");}
        match self.phase {
            Phase::Exam{frozen}=>{
                if !lesson.evaluation || frozen!=policy{return Err("not the frozen evaluation policy");}
                let score=&mut a.scores[lesson.task as usize];
                score.trials=score.trials.checked_add(1).ok_or("exam score exhausted")?;
                score.successes=score.successes.checked_add(success as u32).ok_or("exam successes exhausted")?;
                a.exam_cursor=a.exam_cursor.checked_add(1).ok_or("exam cursor exhausted")?;
            }
            Phase::Training=>{
                if lesson.evaluation{return Err("evaluation result in training phase");}
                let task=lesson.task as usize;a.skills[task]=a.skills[task].observe(success,lesson.full_probe)?;
                if task==self.frontier {
                    a.trained_since_exam=a.trained_since_exam.checked_add(1).ok_or("training episode counter exhausted")?;
                    a.full_since_exam=a.full_since_exam.checked_add(lesson.full_probe as u64).ok_or("probe counter exhausted")?;
                }
            }
        }
        a.pending=None;self.actors[id]=a;Ok(())
    }
    pub fn exam_finished(&self)->bool{matches!(self.phase,Phase::Exam{..}) && self.actors.iter().all(|a|a.pending.is_none() && a.exam_cursor==self.exam_trials_per_actor())}
    pub fn finish_exam(&mut self,policy:PolicyId)->Result<bool>{
        if self.phase!=(Phase::Exam{frozen:policy}){return Err("not the frozen exam policy");}
        if !self.exam_finished(){return Err("exam has unfinished actors");}
        let mut passed=true;
        for a in &self.actors {
            for stage in 0..=self.frontier {
                let s=a.scores[stage];let n=if stage==self.frontier{CURRENT_EXAM_TRIALS}else{REVIEW_EXAM_TRIALS};
                if s.trials!=n || s.successes>s.trials{return Err("inconsistent per-skill exam score");}
                let needed=if stage==self.frontier{14}else{3};passed&=s.successes>=needed;
            }
        }
        let generation=self.generation.checked_add(1).ok_or("generation exhausted")?;
        let examinations=self.examinations.checked_add(1).ok_or("exam count exhausted")?;
        for a in &mut self.actors {
            for stage in 0..=self.frontier {
                let s=a.scores[stage];a.skills[stage].validation_weakness=1.0-s.successes as f64/s.trials as f64;
            }
            a.trained_since_exam=0;a.full_since_exam=0;a.exam_cursor=0;
        }
        if passed {if self.frontier+1<TASK_COUNT{self.frontier+=1;}else{self.ever_completed=true;}}
        self.examinations=examinations;self.generation=generation;self.phase=Phase::Training;self.last_exam_passed=passed;Ok(passed)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    fn p()->PolicyId{PolicyId{version:4,signature:987}}
    fn ready(c:&mut Curriculum){for _ in 0..200{if c.exam_ready(){return;}for id in 0..c.bots(){let l=c.issue(id,p()).unwrap().unwrap();c.record(&l,p(),true).unwrap();}}panic!("never became exam-ready");}
    fn grade(c:&mut Curriculum,success:impl Fn(usize,&Lesson,u32)->bool)->bool {
        c.begin_exam(p()).unwrap();
        for id in 0..c.bots(){let mut n=0;while let Some(l)=c.issue(id,p()).unwrap(){assert_eq!(l.difficulty,1.0);assert!(l.evaluation);c.record(&l,p(),success(id,&l,n)).unwrap();n+=1;}}
        c.finish_exam(p()).unwrap()
    }
    #[test] fn failed_training_does_not_waste_time_on_unreachable_exams(){
        let mut c=Curriculum::new(1,32,99).unwrap();for _ in 0..100{for id in 0..32{let l=c.issue(id,p()).unwrap().unwrap();c.record(&l,p(),false).unwrap();}}
        assert!(!c.exam_ready());assert!(c.begin_exam(p()).is_err());assert_eq!(c.frontier(),0);assert_eq!(c.stats(0).unwrap()[0].difficulty,DIFFICULTY_FLOOR);
    }
    #[test] fn easy_success_and_full_challenge_validation_are_separate(){
        let mut c=Curriculum::new(7,1,22).unwrap();let mut full=0;
        for _ in 0..100{let l=c.issue(0,p()).unwrap().unwrap();full+=l.full_probe as usize;c.record(&l,p(),!l.full_probe).unwrap();}
        assert_eq!(full,20);assert!(!c.exam_ready());assert!(c.stats(0).unwrap()[0].difficulty>0.25);
    }
    #[test] fn every_actor_must_pass_not_only_the_average(){
        let mut c=Curriculum::new(9,32,1).unwrap();ready(&mut c);
        let pass=grade(&mut c,|id,_,trial|id!=31 || trial>=3);assert!(!pass);assert_eq!(c.frontier(),0);
        assert_eq!(c.scores(31).unwrap()[0].successes,13);assert!(!c.last_exam_passed());assert!(!c.exam_ready());
    }
    #[test] fn prior_skill_failure_blocks_promotion(){
        let mut c=Curriculum::new(9,2,1).unwrap();ready(&mut c);assert!(grade(&mut c,|_,_,_|true));assert_eq!(c.frontier(),1);
        ready(&mut c);assert!(!grade(&mut c,|id,l,trial|id!=1 || !l.review || trial>=18));assert_eq!(c.frontier(),1);
        assert_eq!(c.scores(1).unwrap()[0].successes,2);assert_eq!(c.scores(1).unwrap()[1].successes,16);
    }
    #[test] fn frozen_exams_never_train_and_use_separate_seed_domain(){
        let mut c=Curriculum::new(1,1,4).unwrap();ready(&mut c);let stats=*c.stats(0).unwrap();c.begin_exam(p()).unwrap();
        let wrong=PolicyId{version:5,..p()};assert!(c.issue(0,wrong).is_err());
        while let Some(l)=c.issue(0,p()).unwrap(){assert_eq!(l.seed>>63,1);assert_eq!(l.difficulty,1.0);assert!(c.record(&l,wrong,true).is_err());c.record(&l,p(),true).unwrap();}
        assert_eq!(*c.stats(0).unwrap(),stats);c.finish_exam(p()).unwrap();let l=c.issue(0,p()).unwrap().unwrap();assert_eq!(l.seed>>63,0);
    }
    #[test] fn no_duplicate_or_foreign_results_and_no_inflight_phase_switch(){
        let mut c=Curriculum::new(4,1,6).unwrap();ready(&mut c);let l=c.issue(0,p()).unwrap().unwrap();
        assert!(c.issue(0,p()).is_err());assert!(c.begin_exam(p()).is_err());let mut foreign=l.clone();foreign.session.run+=1;
        assert!(c.record(&foreign,p(),true).is_err());c.record(&l,p(),true).unwrap();assert!(c.record(&l,p(),true).is_err());
    }
    #[test] fn round_robin_review_prevents_aliasing_or_starvation(){
        let mut c=Curriculum::new(456,1,8).unwrap();c.frontier=12;let mut seen=[false;12];let mut reviews=0;
        for _ in 0..120{let l=c.issue(0,p()).unwrap().unwrap();if l.review{reviews+=1;seen[l.task as usize]=true;}c.record(&l,p(),true).unwrap();}
        assert_eq!(reviews,24);assert!(seen.iter().all(|x|*x));
    }
    #[test] fn forgotten_and_failed_validation_skills_get_higher_priority(){
        let good=SkillStats{fast_success:0.98,slow_success:0.98,..Default::default()};
        let forgotten=SkillStats{fast_success:0.2,slow_success:0.9,validation_weakness:0.5,..Default::default()};
        assert!(forgotten.priority()>20.0*good.priority());assert!(good.priority()>0.0);
    }
    #[test] fn serial_overflow_does_not_mutate_pending_state(){
        let mut c=Curriculum::new(1,1,2).unwrap();c.actors[0].serial=u64::MAX;assert!(c.issue(0,p()).is_err());assert!(c.pending(0).unwrap().is_none());
    }
    #[test] fn stages_advance_only_after_exams_and_final_completion_is_historical(){
        let mut c=Curriculum::new(1,1,2).unwrap();
        for stage in 0..TASK_COUNT{assert_eq!(c.frontier(),stage);assert!(!c.ever_completed());ready(&mut c);assert_eq!(c.frontier(),stage);assert!(grade(&mut c,|_,_,_|true));}
        assert!(c.ever_completed());assert_eq!(c.examinations(),TASK_COUNT as u64);assert_eq!(c.frontier(),TASK_COUNT-1);
        ready(&mut c);assert!(!grade(&mut c,|_,_,_|false));assert!(c.ever_completed());assert!(!c.last_exam_passed());
    }
}
