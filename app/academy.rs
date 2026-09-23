//! Run- and lesson-scoped authoritative file transport. Never emits a game action.
use std::{fs,io::{Read,Write},path::Path,sync::Arc,time::{Duration,Instant}};
use crate::{engine::runtime,learning::{checkpoint::atomic_write,network::Model,curriculum::{self,Episode,Frame,Lesson,Outcome},next::tasks::{Evidence,Counters,ITEM_COUNT}},metrics};
#[derive(Default)]
pub struct AcademyAgent{pub lesson:Option<Lesson>,pub episode:Option<Episode>,pub policy:Option<Arc<Model>>,last_tick:u64,last_frame:Option<Instant>,requested:Option<Instant>}
pub enum Poll{Reset,Waiting,Frame(Frame)}
fn bounded(path:&Path,limit:u64)->Result<Option<String>,String>{match fs::File::open(path){
    Ok(f)=>{let mut s=String::new();f.take(limit+1).read_to_string(&mut s).map_err(|e|e.to_string())?;if s.len() as u64>limit{return Err("oversized academy IPC".into());}Ok(Some(s))},
    Err(e)if e.kind()==std::io::ErrorKind::NotFound=>Ok(None),Err(e)=>Err(e.to_string())}}
fn parse_frame(text:&str,run:&str,l:&Lesson)->Result<Option<Frame>,String>{
    let f:Vec<_>=text.split_whitespace().collect();
    if f.len()!=32 || f[0]!="BCMCLAB3"{return Err("invalid v3 authoritative frame envelope".into());}
    if f[1]!=run||f[2]!=l.token(){return Ok(None);}
    let n=|i:usize|f[i].parse::<f64>().map_err(|_|"invalid frame number".to_string()).and_then(|x|if x.is_finite(){Ok(x)}else{Err("nonfinite frame".into())});
    let u=|i:usize|f[i].parse::<u32>().map_err(|_|"invalid frame counter".to_string());
    if u(3)?!=l.choice.session.actor as u32{return Err("frame actor/file mismatch".into());}
    let tick=f[4].parse::<u64>().map_err(|e|e.to_string())?;
    let grounded=match f[10]{"true"=>true,"false"=>false,_=>return Err("invalid grounded flag".into())};
    let mut stock=[0;ITEM_COUNT];for i in 0..ITEM_COUNT{stock[i]=u(11+i)?;}
    let counters=Counters{broken:u(24)?,picked_up:u(25)?,crafted:u(26)?,placed:u(27)?,smelted:u(28)?,deposited:u(29)?};
    let occupied=u(31)?;if occupied>7{return Err("invalid target occupancy".into());}
    let evidence=Evidence{session:l.choice.session,tick,stock,counters,target_stock:u(30)?,occupied_targets:occupied as u8,motor_hold_ticks:0};
    if evidence.stock.iter().any(|&v|v>5760)||evidence.target_stock>5760{return Err("out-of-range inventory evidence".into());}
    let frame=Frame{tick,position:[n(5)?,n(6)?,n(7)?],yaw:n(8)? as f32,pitch:n(9)? as f32,grounded,evidence};
    if !frame.yaw.is_finite()||!frame.pitch.is_finite()||frame.pitch.abs()>90.1||frame.position.iter().any(|v|v.abs()>30_000_000.){return Err("out-of-range telemetry".into());}
    Ok(Some(frame))
}
impl AcademyAgent{
    pub fn clear(&mut self){*self=Self::default();}
    pub fn poll(&mut self,id:usize)->Result<Poll,String>{
        let rt=runtime();let dir=rt.cfg.root.join(".runtime/lab");
        if let Some(e)=bounded(&dir.join("fatal.txt"),4096)?{if e.starts_with(&format!("{} ",rt.cfg.run_id)){return Err(format!("Folia environment failed: {e}"));}}
        if self.lesson.is_none(){
            let lease=rt.control.lock().unwrap().issue(id)?;
            let Some((choice,policy))=lease else{return Ok(Poll::Waiting);};
            let l=Lesson::from_choice(choice);
            let text=format!("BCMCLAB3 {} {} {} {} {:.8} {:.8} {:.8} {:.5} {:.5} {:.8} {:.8} {:.8} {} {:.6} {}\n",
                rt.cfg.run_id,l.token(),id,l.stage,l.start[0],l.start[1],l.start[2],l.yaw,l.pitch,l.goal[0],l.goal[1],l.goal[2],l.choice.seed,l.choice.difficulty,l.choice.full_probe||l.exam);
            atomic_write(&dir.join(format!("request-{id}.txt")),text.as_bytes(),0).map_err(|e|e.to_string())?;
            self.lesson=Some(l);self.policy=Some(policy);self.episode=None;self.last_tick=0;self.last_frame=None;self.requested=Some(Instant::now());return Ok(Poll::Reset);
        }
        if self.episode.is_none()&&self.requested.is_some_and(|t|t.elapsed()>Duration::from_secs(90)){return Err("reset unacknowledged for 90s; inspect Folia logs".into());}
        if self.last_frame.is_some_and(|t|t.elapsed()>Duration::from_secs(10)){return Err("authoritative observation stale for 10s".into());}
        if let Some(text)=bounded(&dir.join(format!("frame-{id}.txt")),4096)?{
            if let Some(f)=parse_frame(&text,&rt.cfg.run_id,self.lesson.as_ref().unwrap())?{
                if f.tick<=self.last_tick{return Ok(Poll::Waiting);}
                self.last_tick=f.tick;self.last_frame=Some(Instant::now());return Ok(Poll::Frame(f));
            }
        }Ok(Poll::Waiting)
    }
    pub fn observe(&mut self,id:usize,f:&Frame,action:Option<&[usize]>)->Result<Option<Outcome>,String>{
        let l=self.lesson.as_ref().ok_or("missing issued lesson")?;
        if self.episode.is_none(){self.episode=Some(Episode::new(l,f.clone())?);return Ok(None);}
        action.map(|a|self.episode.as_mut().unwrap().step(id,l,f.clone(),a,0.997)).transpose()
    }
    pub fn features(&self,f:&Frame)->[f32;87]{self.episode.as_ref().unwrap().features(self.lesson.as_ref().unwrap(),f)}
    /// The coordinator has already accepted the terminal; this is its bounded audit.
    pub fn finish(&mut self,id:usize,version:u64,success:bool,reason:&str)->Result<(),String>{
        let rt=runtime();let l=self.lesson.as_ref().ok_or("missing completed episode")?;
        if l.choice.policy.version!=version{return Err("episode completed with a different behavior model".into());}
        let e=self.episode.as_ref().ok_or("missing authoritative baseline")?;let elapsed=e.previous.tick-e.first_tick;
        let path=rt.cfg.root.join("logs/academy-episodes.csv");let _guard=rt.academy_log.lock().unwrap();
        if fs::metadata(&path).is_ok_and(|m|m.len()>8*1024*1024){for i in (1..4).rev(){let a=path.with_extension(format!("csv.{i}"));if a.exists(){fs::rename(a,path.with_extension(format!("csv.{}",i+1))).map_err(|e|e.to_string())?;}}fs::rename(&path,path.with_extension("csv.1")).map_err(|e|e.to_string())?;}
        let empty=!path.exists();let mut f=fs::OpenOptions::new().create(true).append(true).open(path).map_err(|e|e.to_string())?;
        if empty{writeln!(f,"run_id,unix_time,agent,serial,generation,stage,exam,review,policy_version,success,reason,server_ticks,rotation_degrees,movement_switches").map_err(|e|e.to_string())?;}
        writeln!(f,"{},{},{},{},{},{},{},{},{},{},{},{},{:.3},{}",rt.cfg.run_id,metrics::now(),id,l.serial,l.generation,l.stage,l.exam,l.review,version,success,reason,elapsed,e.rotation,e.switches).map_err(|e|e.to_string())?;
        self.clear();Ok(())
    }
}
pub fn status(rt:&crate::engine::Runtime)->Result<(),String>{
    let t=rt.training.lock().unwrap().clone();let c=rt.control.lock().unwrap();let frontier=c.course.frontier();
    let phase=if c.optimizing(){"updating"}else if c.examining(){"exam"}else{"training"};
    let mut rows=Vec::new();let mut progress=format!("BCMCPROGRESS2 {} {} {} {} {} {} {} {} {}\n",rt.cfg.run_id,metrics::now(),frontier,phase,c.policy.version,t.samples,c.buffered(),c.sealed(),rt.cfg.bots);
    for id in 0..rt.cfg.bots{
        let pending=c.course.pending(id).map_err(str::to_string)?;
        let task=pending.map(|l|l.task as usize).unwrap_or(frontier);
        let s=c.course.stats(id).map_err(str::to_string)?[task];let e=c.course.scores(id).map_err(str::to_string)?[frontier];
        let state=if let Some(l)=pending{if l.evaluation{"exam"}else if l.review{"review"}else if l.full_probe{"probe"}else{"practice"}}else{"waiting"};
        let difficulty=pending.map(|l|l.difficulty).unwrap_or(s.difficulty);
        rows.push(format!("{{\"id\":{},\"task\":{},\"state\":\"{}\",\"attempts\":{},\"success_rate\":{:.5},\"difficulty\":{:.3},\"exam_trials\":{},\"exam_successes\":{}}}",id,task,state,s.attempts,s.fast_success,difficulty,e.trials,e.successes));
        progress.push_str(&format!("{} {} {} {} {:.5} {:.3} {} {}\n",id,task,state,s.attempts,s.fast_success,difficulty,e.trials,e.successes));
    }
    let local=c.local_samples.iter().sum::<usize>();let unfinished=c.pending_actions.iter().filter(|v|**v).count();
    let data=format!("{{\"schema\":3,\"run_id\":\"{}\",\"updated\":{},\"stage\":{},\"name\":\"{}\",\"phase\":\"{}\",\"generation\":{},\"exams\":{},\"course_completed\":{},\"last_pass\":{},\"policy_version\":{},\"cohort_samples\":{},\"sealed_actors\":{},\"local_untrained_samples\":{},\"unfinished_actions\":{},\"scores\":[{}]}}\n",rt.cfg.run_id,metrics::now(),frontier,curriculum::NAMES[frontier],phase,c.course.generation(),c.course.examinations(),c.course.ever_completed(),c.course.last_exam_passed(),c.policy.version,c.buffered(),c.sealed(),local,unfinished,rows.join(","));
    drop(c);
    atomic_write(&rt.cfg.root.join("state/academy-status.json"),data.as_bytes(),0).map_err(|e|e.to_string())?;
    atomic_write(&rt.cfg.root.join(".runtime/lab/progress.txt"),progress.as_bytes(),0).map_err(|e|e.to_string())
}
#[cfg(test)] mod tests{
    use super::*;use crate::learning::next::{curriculum::{Lesson as Choice,PolicyId},tasks::{Session,Task}};
    fn lesson()->Lesson{Lesson::from_choice(Choice{session:Session{run:1,actor:63,generation:1,lesson:3},task:Task::CraftWorkbench,difficulty:1.,seed:8,full_probe:true,evaluation:false,review:false,policy:PolicyId{version:0,signature:0}})}
    fn text()->String{format!("BCMCLAB3 run 1-63-3 63 4 120 97 120 0 0 true {} 0 0 0 0 0 0 0 0",vec!["0";13].join(" "))}
    #[test]fn authoritative_envelope_checks_session_actor_and_finite_numbers(){let l=lesson();let s=text();assert!(parse_frame(&s,"run",&l).unwrap().is_some());assert!(parse_frame(&s,"other",&l).unwrap().is_none());assert!(parse_frame(&s.replace("120 97","NaN 97"),"run",&l).is_err());assert!(parse_frame(&s.replace("1-63-3 63","1-63-3 62"),"run",&l).is_err());assert!(parse_frame(&(s+" 1"),"run",&l).is_err());}
}
