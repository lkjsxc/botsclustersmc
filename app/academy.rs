//! Local file transport to a Folia-owned environment. Never produces a bot action.
use std::{fs,io::{Read,Write},path::Path,time::{Duration,Instant}};
use crate::{engine::runtime,learning::{checkpoint::atomic_write,curriculum::{self,Episode,Frame,Lesson,Outcome,Phase}},metrics};

#[derive(Default)]
pub struct AcademyAgent {
    pub lesson:Option<Lesson>,pub episode:Option<Episode>,
    token:String,generation:Option<u64>,last_tick:u64,last_frame:Option<Instant>,requested:Option<Instant>,
}
pub enum Poll { Reset, Waiting, Frame(Frame) }
fn bounded(path:&Path,limit:u64)->Result<Option<String>,String>{
    match fs::File::open(path) {
        Ok(f)=>{let mut s=String::new();f.take(limit+1).read_to_string(&mut s).map_err(|e|e.to_string())?;if s.len() as u64>limit{return Err("oversized academy IPC file".into());}Ok(Some(s))},
        Err(e) if e.kind()==std::io::ErrorKind::NotFound=>Ok(None),Err(e)=>Err(e.to_string())
    }
}
fn parse_frame(text:&str,run:&str,token:&str,id:usize)->Result<Option<Frame>,String>{
    let f=text.split_whitespace().collect::<Vec<_>>();
    if f.len()!=12 || f[0]!="BCMCLAB2" {return Err("invalid academy frame envelope".into());}
    if f[1]!=run || f[2]!=token{return Ok(None);}
    let num=|i:usize|f[i].parse::<f64>().map_err(|_|"invalid academy frame number".to_string()).and_then(|v|if v.is_finite(){Ok(v)}else{Err("nonfinite academy observation".into())});
    if f[3].parse::<usize>().map_err(|e|e.to_string())?!=id{return Err("academy agent mismatch".into());}
    let tick=f[4].parse::<u64>().map_err(|e|e.to_string())?;
    let boolean=|i:usize|match f[i]{"true"=>Ok(true),"false"=>Ok(false),_=>Err("invalid academy boolean".to_string())};
    let frame=Frame{tick,position:[num(5)?,num(6)?,num(7)?],yaw:num(8)? as f32,pitch:num(9)? as f32,grounded:boolean(10)?,broken:boolean(11)?};
    if !frame.yaw.is_finite()||!frame.pitch.is_finite()||frame.position.iter().any(|v|v.abs()>30_000_000.)||frame.pitch.abs()>90.1{return Err("out-of-range academy telemetry".into());}
    Ok(Some(frame))
}
impl AcademyAgent {
    pub fn clear(&mut self){*self=Self::default();}
    pub fn poll(&mut self,id:usize)->Result<Poll,String>{
        let rt=runtime();let dir=rt.cfg.root.join(".runtime/lab");
        if let Some(error)=bounded(&dir.join("fatal.txt"),4096)? {
            if error.starts_with(&format!("{} ",rt.cfg.run_id)){return Err(format!("Folia training environment failed: {error}"));}
        }
        let mut c=rt.curriculum.as_ref().ok_or("academy coordinator missing")?.lock().unwrap();
        let changed=self.generation!=Some(c.generation);
        if changed {self.lesson=None;self.episode=None;self.generation=Some(c.generation);self.last_frame=None;self.requested=None;}
        if self.lesson.is_none() {
            if let Some(l)=c.begin(id,rt.cfg.seed) {
                self.token=format!("{}-{}",l.generation,l.serial);
                let text=format!("BCMCLAB2 {} {} {} {} {:.8} {:.8} {:.8} {:.5} {:.5} {:.8} {:.8} {:.8}\n",rt.cfg.run_id,self.token,id,l.stage,l.start[0],l.start[1],l.start[2],l.yaw,l.pitch,l.goal[0],l.goal[1],l.goal[2]);
                atomic_write(&dir.join(format!("request-{id}.txt")),text.as_bytes(),0).map_err(|e|e.to_string())?;
                self.lesson=Some(l);self.episode=None;self.last_tick=0;self.last_frame=None;self.requested=Some(Instant::now());
                return Ok(Poll::Reset);
            }
            return Ok(if changed{Poll::Reset}else{Poll::Waiting});
        }
        drop(c);
        if self.episode.is_none()&&self.requested.is_some_and(|t|t.elapsed()>Duration::from_secs(60)){return Err("academy reset unacknowledged for 60s; inspect Folia/plugin logs".into());}
        if self.last_frame.is_some_and(|t|t.elapsed()>Duration::from_secs(5)){return Err("academy server observation stale for 5s; refusing to score client predictions".into());}
        if let Some(text)=bounded(&dir.join(format!("frame-{id}.txt")),2048)? {
            if let Some(f)=parse_frame(&text,&rt.cfg.run_id,&self.token,id)? {
                if f.tick<=self.last_tick{return Ok(Poll::Waiting);}
                self.last_tick=f.tick;self.last_frame=Some(Instant::now());
                return Ok(Poll::Frame(f));
            }
        }
        Ok(Poll::Waiting)
    }
    pub fn observe(&mut self,id:usize,f:&Frame,action:Option<&[usize]>)->Option<Outcome> {
        if self.episode.is_none(){self.episode=Some(Episode::new(f.clone()));return None;}
        action.map(|a|self.episode.as_mut().unwrap().step(id,self.lesson.as_ref().unwrap(),f.clone(),a,0.997))
    }
    pub fn features(&self,f:&Frame)->[f32;23]{self.episode.as_ref().unwrap().features(self.lesson.as_ref().unwrap(),f)}
    pub fn finish(&mut self,id:usize,version:u64,success:bool,reason:&str)->Result<(),String>{
        let rt=runtime();let l=self.lesson.as_ref().ok_or("missing episode")?;
        if l.exam && version!=rt.policy.read().unwrap().version{return Err("policy changed during a frozen exam".into());}
        rt.curriculum.as_ref().unwrap().lock().unwrap().record(id,l,success);
        let e=self.episode.as_ref();
        let elapsed=e.map(|e|e.previous.tick.saturating_sub(e.first_tick)).unwrap_or(0);
        let path=rt.cfg.root.join("logs/academy-episodes.csv");
        // Per-agent completion callbacks serialize here; only one bounded writer.
        let _guard=rt.academy_log.lock().unwrap();
        if fs::metadata(&path).is_ok_and(|m|m.len()>8*1024*1024){for i in (1..4).rev(){let a=path.with_extension(format!("csv.{i}"));if a.exists(){fs::rename(a,path.with_extension(format!("csv.{}",i+1))).map_err(|e|e.to_string())?;}}fs::rename(&path,path.with_extension("csv.1")).map_err(|e|e.to_string())?;}
        let empty=!path.exists();let mut file=fs::OpenOptions::new().create(true).append(true).open(path).map_err(|e|e.to_string())?;
        if empty {writeln!(file,"run_id,unix_time,agent,serial,generation,stage,exam,review,policy_version,success,reason,server_ticks,rotation_degrees,movement_switches").map_err(|e|e.to_string())?;}
        writeln!(file,"{},{},{},{},{},{},{},{},{},{},{},{},{:.3},{}",rt.cfg.run_id,metrics::now(),id,l.serial,l.generation,l.stage,l.exam,l.review,version,success,reason,elapsed,e.map(|e|e.rotation).unwrap_or(0.),e.map(|e|e.switches).unwrap_or(0)).map_err(|e|e.to_string())?;
        self.lesson=None;self.episode=None;self.requested=None;self.last_frame=None;Ok(())
    }
}
pub fn status(rt:&crate::engine::Runtime)->Result<(),String>{
    let Some(c)=&rt.curriculum else{return Ok(());};let c=c.lock().unwrap();
    let scores=c.scores.iter().enumerate().map(|(i,s)|format!("{{\"id\":{},\"trained\":{},\"exam_current\":{},\"exam_current_success\":{},\"exam_review\":{},\"exam_review_success\":{},\"retained\":{:?}}}",i,s.trained,s.current,s.current_success,s.review,s.review_success,s.retained)).collect::<Vec<_>>().join(",");
    let data=format!("{{\"schema\":1,\"run_id\":\"{}\",\"updated\":{},\"stage\":{},\"name\":\"{}\",\"phase\":\"{}\",\"generation\":{},\"cycle\":{},\"foundations_passed\":{},\"last_pass\":{},\"scores\":[{}]}}\n",rt.cfg.run_id,metrics::now(),c.stage,curriculum::NAMES[c.stage],if c.phase==Phase::Exam{"frozen-evaluation"}else{"training"},c.generation,c.cycle,c.completed,c.last_pass.map(|b|b.to_string()).unwrap_or("null".into()),scores);
    atomic_write(&rt.cfg.root.join("state/academy-status.json"),data.as_bytes(),0).map_err(|e|e.to_string())
}
#[cfg(test)] mod tests {
    use super::*;
    #[test]fn rejects_stale_episode_and_nan(){let s="BCMCLAB2 run 1-3 0 4 8 97 5 0 0 true false";assert!(parse_frame(s,"run","1-3",0).unwrap().is_some());assert!(parse_frame(s,"run","1-4",0).unwrap().is_none());assert!(parse_frame(&s.replace("8 97 5","NaN 97 5"),"run","1-3",0).is_err());assert!(parse_frame(s,"run","1-3",1).is_err());}
}
