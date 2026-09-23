//! Fail closed: a matching plugin heartbeat alone is not a built-campus receipt.
use std::{fs,io::Read,path::Path};
use super::{install::Result,json::{self,Json}};
fn bounded(path:&Path)->Result<Option<String>> {
    match fs::File::open(path) {
        Ok(f)=>{let mut s=String::new();f.take(8193).read_to_string(&mut s).map_err(|e|e.to_string())?;if s.len()>8192{return Err("campus receipt too large".into());}Ok(Some(s))},
        Err(e) if e.kind()==std::io::ErrorKind::NotFound=>Ok(None),Err(e)=>Err(e.to_string())
    }
}
pub fn validate(text:&str,run:&str,bots:usize)->Result<()> {
    if !(1..=64).contains(&bots){return Err("invalid campus population".into());}
    let mut expected=format!("BCMCCAMPUS1 {run} {bots} 8 16 96\n");
    for id in 0..bots{expected.push_str(&format!("{id} {} {}\n",id%8*16,id/8*16));}
    if text!=expected{return Err("campus manifest has stale run, wrong population, missing cells or incompatible geometry".into());}
    Ok(())
}
pub fn ready(root:&Path,run:&str,bots:usize)->Result<bool> {
    let lab=root.join(".runtime/lab");
    if let Some(fatal)=bounded(&lab.join("fatal.txt"))?{if fatal.starts_with(&format!("{run} ")){return Err(format!("Academy failed: {fatal}"));}}
    let Some(bridge)=bounded(&lab.join("bridge.ready"))? else{return Ok(false);};
    if bridge!=format!("BCMCLAB3 {run} ready\n"){return Err("stale/incompatible bridge receipt".into());}
    let Some(manifest)=bounded(&lab.join("campus.ready"))? else{return Ok(false);};
    validate(&manifest,run,bots)?;Ok(true)
}
pub fn population(status:&Json,run:&str,bots:usize,now:u64,max_age:u64)->Result<bool> {
    if status.get("run_id")?.text()?!=run{return Err("stale run status".into());}
    let agents=status.get("agents")?.array()?;
    if agents.len()!=bots{return Err("wrong learner population".into());}
    for (id,a) in agents.iter().enumerate(){
        if a.get("id")?.integer()?!=id as u64{return Err("duplicate/missing agent ID".into());}
        if a.get("online")?!=&Json::Bool(true)||a.get("spawns")?.integer()?==0||a.get("steps")?.integer()?==0||now.saturating_sub(a.get("last_seen")?.integer()?)>max_age{return Ok(false);}
    }
    Ok(true)
}
#[cfg(test)] mod tests {
    use super::*;
    #[test] fn exact_manifest_and_all_coordinates(){let s="BCMCCAMPUS1 run 2 8 16 96\n0 0 0\n1 16 0\n";assert!(validate(s,"run",2).is_ok());for bad in [s.replace("run","old"),s.replace("1 16 0","1 32 0"),s.replace("1 16 0\n","")]{assert!(validate(&bad,"run",2).is_err());}assert!(validate(s,"run",32).is_err());}
    #[test] fn every_member_must_have_acted(){let s=json::parse(r#"{"run_id":"r","agents":[{"id":0,"online":true,"spawns":1,"steps":1,"last_seen":50},{"id":1,"online":true,"spawns":1,"steps":0,"last_seen":50}]}"#).unwrap();assert!(!population(&s,"r",2,50,60).unwrap());assert!(population(&s,"old",2,50,60).is_err());}
}
