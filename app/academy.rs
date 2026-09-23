//! Session-bound server telemetry. Resets are environment operations, never actions.
use std::{fs,path::Path,sync::{Arc,atomic::Ordering},time::Instant,io::Write};
use crate::{engine::{self,Runtime},metrics,learning::{academy::{Lesson,Episode,Frame,Outcome},network::Model,ppo::Transition,
    checkpoint::atomic_write,next::{tasks::{Evidence,Counters,Session,NAMES},curriculum::Phase}}};

pub enum Poll {Reset,Waiting,Frame(Frame)}
pub struct AcademyAgent {pub lesson:Option<Lesson>,pub episode:Option<Episode>,pub policy:Option<Arc<Model>>,token:String,last_tick:u64,requested:Instant,last_frame:Instant}
impl Default for AcademyAgent{fn default()->Self{Self{lesson:None,episode:None,policy:None,token:String::new(),last_tick:0,requested:Instant::now(),last_frame:Instant::now()}}}
fn read(path:&Path,limit:u64)->Result<Option<String>,String>{
    match fs::metadata(path){Ok(m)=>{if !m.is_file()||m.len()>limit{return Err("invalid or oversized bridge file".into());}fs::read_to_string(path).map(Some).map_err(|e|e.to_string())},Err(e) if e.kind()==std::io::ErrorKind::NotFound=>Ok(None),Err(e)=>Err(e.to_string())}
}
fn parse_frame(text:&str,run:&str,token:&str,id:usize,session:Session)->Result<Option<Frame>,String>{
    let f=text.split_whitespace().collect::<Vec<_>>();
    if f.len()!=32||f[0]!="BCMCLAB3"{return Err("invalid extended academy frame envelope".into());}
    if f[1]!=run||f[2]!=token{return Ok(None);}
    let integer=|i:usize|f[i].parse::<u64>().map_err(|_|"invalid bridge integer".to_string());
    if integer(3)?!=id as u64{return Err("bridge frame actor mismatch".into());}
    let num=|i:usize|{let x=f[i].parse::<f64>().map_err(|_|"invalid coordinate")?;if !x.is_finite(){return Err("nonfinite bridge coordinate".to_string());}Ok(x)};
    let tick=integer(4)?;if tick==0||tick>1_000_000{return Err("invalid session tick".into());}
    let grounded=match f[10]{"true"=>true,"false"=>false,_=>return Err("invalid grounded flag".into())};
    let mut stock=[0;13];for i in 0..13{stock[i]=u32::try_from(integer(11+i)?).map_err(|_|"stock overflow")?;if stock[i]>5760{return Err("stock exceeds bounded inventory".into());}}
    let count=|i|u32::try_from(integer(i)?).map_err(|_|"counter overflow".to_string());
    let counters=Counters{broken:count(24)?,picked_up:count(25)?,crafted:count(26)?,placed:count(27)?,smelted:count(28)?,deposited:count(29)?};
    let target_stock=count(30)?;let occupied_targets=u8::try_from(integer(31)?).map_err(|_|"target mask overflow")?;
    if target_stock>5760||occupied_targets>7{return Err("invalid target state".into());}
    let position=[num(5)?,num(6)?,num(7)?];let yaw=num(8)? as f32;let pitch=num(9)? as f32;
    if position.iter().any(|x|x.abs()>30_000_000.)||!yaw.is_finite()||!pitch.is_finite()||pitch.abs()>90.1{return Err("invalid bridge pose".into());}
    Ok(Some(Frame{tick,position,yaw,pitch,grounded,evidence:Evidence{session,tick,stock,counters,target_stock,occupied_targets,motor_hold_ticks:0}}))
}
impl AcademyAgent {
    pub fn clear(&mut self){self.lesson=None;self.episode=None;self.policy=None;self.token.clear();self.last_tick=0;}
    pub fn poll(&mut self,id:usize)->Result<Poll,String>{
        let rt=engine::runtime();let dir=rt.cfg.root.join(".runtime/lab");
        if self.lesson.is_none(){
            if let Some((ticket,policy))=engine::begin(id)?{
                let l=Lesson::new(ticket);self.token=format!("{}-{}",l.generation,l.serial);self.policy=Some(policy);self.last_tick=0;self.episode=None;
                let text=format!("BCMCLAB3 {} {} {} {} {:.8} {:.8} {:.8} {:.5} {:.5} {:.8} {:.8} {:.8} {:.6} {}\n",
                    rt.cfg.run_id,self.token,id,l.stage,l.start[0],l.start[1],l.start[2],l.yaw,l.pitch,l.goal[0],l.goal[1],l.goal[2],l.ticket.difficulty,l.ticket.seed);
                atomic_write(&dir.join(format!("request-{id}.txt")),text.as_bytes(),0).map_err(|e|e.to_string())?;
                self.lesson=Some(l);self.requested=Instant::now();self.last_frame=Instant::now();return Ok(Poll::Reset);
            }return Ok(Poll::Waiting);
        }
        let l=self.lesson.as_ref().unwrap();
        if let Some(text)=read(&dir.join(format!("frame-{id}.txt")),4096)?{
            if let Some(f)=parse_frame(&text,&rt.cfg.run_id,&self.token,id,l.ticket.session)?{
                if f.tick>self.last_tick{self.last_tick=f.tick;self.last_frame=Instant::now();return Ok(Poll::Frame(f));}
            }
        }
        if (self.last_tick==0&&self.requested.elapsed().as_secs()>60)||(self.last_tick>0&&self.last_frame.elapsed().as_secs()>10){return Err(format!("actor {id}: current-session environment telemetry stopped"));}
        Ok(Poll::Waiting)
    }
    pub fn observe(&mut self,id:usize,f:&Frame,action:Option<&[usize]>)->Result<Option<Outcome>,String>{
        let l=self.lesson.as_ref().ok_or("missing lesson")?;
        if self.episode.is_none(){self.episode=Some(Episode::new(l,f.clone()).map_err(str::to_string)?);return Ok(None);}
        if let Some(a)=action{Ok(Some(self.episode.as_mut().unwrap().step(id,l,f.clone(),a,0.997).map_err(str::to_string)?))}else{Ok(None)}
    }
    pub fn features(&self,f:&Frame)->[f32;87]{self.episode.as_ref().unwrap().features(self.lesson.as_ref().unwrap(),f)}
    pub fn finish(&mut self,id:usize,model:&Model,steps:Vec<Transition>,success:bool,reason:&str)->Result<(),String>{
        let rt=engine::runtime();let l=self.lesson.as_ref().ok_or("completed episode has no lesson")?;let e=self.episode.as_ref().ok_or("completed episode has no server evidence")?;
        engine::complete(&l.ticket,model,steps,success)?;
        let _lock=rt.academy_log.lock().unwrap();let path=rt.cfg.root.join("logs/academy-episodes.csv");
        if fs::metadata(&path).map(|m|m.len()>8*1024*1024).unwrap_or(false){
            for i in (1..=3).rev(){let a=rt.cfg.root.join(format!("logs/academy-episodes.csv.{i}"));if a.exists(){fs::rename(a,rt.cfg.root.join(format!("logs/academy-episodes.csv.{}",i+1))).map_err(|e|e.to_string())?;}}
            fs::rename(&path,rt.cfg.root.join("logs/academy-episodes.csv.1")).map_err(|e|e.to_string())?;
        }
        fs::create_dir_all(rt.cfg.root.join("logs")).map_err(|e|e.to_string())?;
        let mut file=fs::OpenOptions::new().create(true).append(true).open(&path).map_err(|e|e.to_string())?;
        if file.metadata().map_err(|e|e.to_string())?.len()==0{writeln!(file,"run_id,time,agent,serial,generation,stage,exam,review,policy_version,success,reason,server_ticks,rotation_degrees,movement_switches").map_err(|e|e.to_string())?;}
        writeln!(file,"{},{},{},{},{},{},{},{},{},{},{},{},{:.5},{}",rt.cfg.run_id,metrics::now(),id,l.serial,l.generation,l.stage,l.exam,l.review,model.version,success,reason,e.previous.tick.saturating_sub(e.first_tick),e.rotation,e.switches).map_err(|e|e.to_string())?;
        self.clear();Ok(())
    }
}
pub fn status(rt:&Runtime)->Result<(),String>{
    let t=rt.training.lock().unwrap().clone();let c=rt.coordination.lock().unwrap();let course=&c.course;
    let frontier=course.frontier();let phase=if matches!(course.phase(),Phase::Exam{..}){"frozen-evaluation"}else{"training"};
    let mut scores=Vec::new();let sealed=c.cohort.progress().iter().filter(|p|p.sealed).count();
    let mut observer=format!("BCMCOBS1 {} {} {} {} {} {} {} {}\n",rt.cfg.run_id,metrics::now(),t.version,t.samples,frontier,phase,sealed,rt.cfg.bots);
    for id in 0..rt.cfg.bots{
        let stats=course.stats(id).map_err(str::to_string)?;let exams=course.scores(id).map_err(str::to_string)?;
        let pending=course.pending(id).map_err(str::to_string)?;let stage=pending.map(|l|l.task as usize).unwrap_or(frontier);
        let difficulty=pending.map(|l|l.difficulty).unwrap_or(stats[stage].difficulty);
        scores.push(format!("{{\"id\":{id},\"task\":{stage},\"difficulty\":{difficulty:.4},\"success_ema\":{:.5},\"attempts\":{},\"exam_trials\":{},\"exam_successes\":{},\"waiting_for_cohort\":{}}}",stats[stage].fast_success,stats[stage].attempts,exams[stage].trials,exams[stage].successes,c.cohort.progress()[id].sealed));
        observer.push_str(&format!("{id} {stage} {difficulty:.4} {:.5} {} {} {} {} {}\n",stats[stage].fast_success,stats[stage].attempts,exams[stage].successes,exams[stage].trials,c.cohort.progress()[id].sealed,NAMES[stage]));
    }
    let data=format!("{{\"schema\":2,\"run_id\":\"{}\",\"updated\":{},\"stage\":{},\"name\":\"{}\",\"phase\":\"{}\",\"generation\":{},\"cycle\":{},\"all_tasks_ever_passed\":{},\"last_pass\":{},\"cohort_quota\":{},\"sealed_actors\":{},\"buffered_completed_samples\":{},\"submitted_samples\":{},\"shutdown_discarded_submitted_samples\":{},\"scores\":[{}]}}\n",
        rt.cfg.run_id,metrics::now(),frontier,NAMES[frontier],phase,course.generation(),course.examinations(),course.ever_completed(),course.last_exam_passed(),c.cohort.quota(),sealed,c.cohort.len(),rt.submitted_samples.load(Ordering::Relaxed),rt.shutdown_discarded_samples.load(Ordering::Relaxed),scores.join(","));
    drop(c);
    atomic_write(&rt.cfg.root.join("state/academy-status.json"),data.as_bytes(),0).map_err(|e|e.to_string())?;
    atomic_write(&rt.cfg.root.join(".runtime/lab/observer.txt"),observer.as_bytes(),0).map_err(|e|e.to_string())?;Ok(())
}
#[cfg(test)]mod tests{
 use super::*;
 #[test]fn frame_requires_current_session_and_bounded_evidence(){
  let session=Session{run:1,actor:63,generation:1,lesson:2};let s=format!("BCMCLAB3 run 1-2 63 4 120 97 120 0 0 true {} {} 0 0",vec!["0";13].join(" "),vec!["0";6].join(" "));
  assert!(parse_frame(&s,"run","1-2",63,session).unwrap().is_some());assert!(parse_frame(&s,"run","1-3",63,session).unwrap().is_none());
  assert!(parse_frame(&s.replace("120 97","NaN 97"),"run","1-2",63,session).is_err());assert!(parse_frame(&s,"run","1-2",62,session).is_err());
 }
}
