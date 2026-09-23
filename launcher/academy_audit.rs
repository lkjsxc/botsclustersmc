//! Recorded lifecycle acceptance, not an assertion that a policy is competent.
use std::{collections::{BTreeMap,BTreeSet},fs,path::Path};
use super::{campus,audit,install::Result,json,learning::{bundle::Bundle}};

pub fn rows(text:&str,run:&str,bots:usize)->Result<Vec<usize>> {
    let mut counts=vec![0;bots];let mut versions=BTreeMap::<u64,u64>::new();let mut seen=BTreeSet::new();
    for line in text.lines() {
        if line.starts_with("run_id,")||line.is_empty(){continue;}
        let f=line.split(',').collect::<Vec<_>>();
        if f.first().copied()!=Some(run){continue;}
        if f.len()!=14{return Err("malformed academy episode row".into());}
        let number=|i:usize|f[i].parse::<u64>().map_err(|_|"invalid academy episode integer".to_string());
        let id=number(2)? as usize;let serial=number(3)?;let generation=number(4)?;let stage=number(5)?;let version=number(8)?;
        if id>=bots||stage>=18||!seen.insert((id,serial)){return Err("duplicate or invalid academy episode identity".into());}
        if !["true","false"].contains(&f[6])||!["true","false"].contains(&f[7])||!["true","false"].contains(&f[9]){return Err("invalid episode flags".into());}
        if (f[9]=="true")!=(f[10]=="success")||!["success","timeout","outside","death"].contains(&f[10]){return Err("episode result/reason mismatch".into());}
        if f[6]=="true" {
            if let Some(old)=versions.insert(generation,version){if old!=version{return Err("policy changed inside a supposedly frozen academy exam".into());}}
        }
        if number(11)?>0{counts[id]+=1;}
    }
    if counts.iter().any(|&n|n==0){return Err("an agent has no completed server-observed academy episode in this run".into());}
    Ok(counts)
}
pub fn verify(root:&Path,previous:Option<&Path>)->Result<()> {
    // This also establishes two new PPO updates, exact restart weights/counters,
    // server protocol, active population, valid checkpoint and orderly shutdown.
    audit::verify(root,previous)?;
    if !root.join(".botsclustersmc-academy-v2").is_file(){return Err("not an isolated academy".into());}
    let run=json::parse(&fs::read_to_string(root.join(".runtime/run.json")).map_err(|e|e.to_string())?)?;
    let run_id=run.get("run_id")?.text()?;let bots=run.get("bots")?.integer()? as usize;
    if !(1..=64).contains(&bots){return Err("invalid academy population".into());}
    if !campus::ready(root,run_id,bots)?{return Err("campus not verified for this run".into());}
    if let Ok(fatal)=fs::read_to_string(root.join(".runtime/lab/fatal.txt")){
        if fatal.starts_with(&format!("{run_id} ")){return Err(format!("academy environment error: {fatal}"));}
    }
    let state=json::parse(&fs::read_to_string(root.join("state/academy-status.json")).map_err(|e|e.to_string())?)?;
    if state.get("run_id")?.text()?!=run_id{return Err("stale academy status".into());}
    let bundle=Bundle::load(&root.join("state/training.bcmc"),bots,1).map_err(|e|e.to_string())?;
    if state.get("schema")?.integer()?!=3||bundle.curriculum.frontier() as u64!=state.get("stage")?.integer()?||bundle.checkpoint.model.version!=state.get("policy_version")?.integer()?{return Err("atomic checkpoint/academy status mismatch".into());}
    let mut text=String::new();
    for suffix in [".4",".3",".2",".1",""] {
        let path=root.join(format!("logs/academy-episodes.csv{suffix}"));
        if let Ok(m)=fs::metadata(&path){if m.len()>16*1024*1024{return Err("academy log size limit".into());}text.push_str(&fs::read_to_string(path).map_err(|e|e.to_string())?);}
    }
    let counts=rows(&text,run_id,bots)?;
    println!("PASS: academy bridge, atomic policy/curriculum/RNG checkpoint and completed episode records checked: {counts:?}.");
    println!("Frozen exam version consistency is checked when exam records exist. This does NOT imply an exam occurred, a stage was passed, human-like movement or cooperative living.");
    Ok(())
}
#[cfg(test)] mod tests {
    use super::*;
    fn line(id:usize,serial:u64,version:u64)->String{format!("run,1,{id},{serial},1,0,true,false,{version},false,timeout,600,90,12\n")}
    #[test]fn failures_are_valid_experience_not_skill_evidence(){assert_eq!(rows(&line(0,0,3),"run",1).unwrap(),vec![1]);}
    #[test]fn rejects_exam_policy_change(){assert!(rows(&(line(0,0,3)+&line(0,1,4)),"run",1).is_err());}
    #[test]fn rejects_missing_agent_and_old_run(){assert!(rows(&line(0,0,3),"run",2).is_err());assert!(rows(&line(0,0,3),"new-run",1).is_err());}
    #[test]fn rejects_duplicate_episode(){assert!(rows(&(line(0,0,3)+&line(0,0,3)),"run",1).is_err());}
}
