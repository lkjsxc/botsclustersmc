//! Acceptance checks inspect real run records. Unit fixtures below test rejection
//! logic only; they never establish that Folia or learning ran.
use std::{fs,path::Path};
use super::{install::Result,json::{self,Json},learning::bundle::Bundle};
fn integer(j:&Json,key:&str)->Result<u64>{j.get(key)?.integer()}
fn text<'a>(j:&'a Json,key:&str)->Result<&'a str>{j.get(key)?.text()}
fn read(path:&Path)->Result<Json>{json::parse(&fs::read_to_string(path).map_err(|e|format!("{}: {e}",path.display()))?)}
fn require(ok:bool,message:&str)->Result<()>{if ok{Ok(())}else{Err(message.into())}}

pub fn validate_status(status:&Json,run:&Json,ready:&Json)->Result<()> {
    require(integer(status,"schema")?==2,"status schema mismatch")?;
    require(text(status,"run_id")?==text(run,"run_id")? && text(ready,"run_id")?==text(run,"run_id")?,"stale/mismatched run record")?;
    require(integer(ready,"protocol")?==774,"server protocol not verified")?;
    require(text(status,"mode")?=="train","acceptance requires a training run, not eval/random")?;
    require(text(status,"error")?.is_empty(),"run recorded a learning/agent error")?;
    require(integer(status,"version")?.saturating_sub(integer(status,"initial_version")?)>=2,"fewer than two completed PPO updates in this run")?;
    require(integer(status,"trained_samples")?>integer(status,"initial_samples")?,"no new trained samples")?;
    require(integer(status,"optimizer_steps")?>integer(status,"initial_optimizer_steps")?,"optimizer did not advance")?;
    require(text(status,"fingerprint")?!=text(status,"initial_fingerprint")?,"policy weights did not change")?;
    let agents=status.get("agents")?.array()?;
    require(agents.len() as u64==integer(run,"bots")?,"wrong recorded population")?;
    require(!agents.is_empty(),"no agents")?;
    for (id,agent) in agents.iter().enumerate() {
        require(integer(agent,"id")?==id as u64,"duplicate/missing agent identity")?;
        require(integer(agent,"steps")?>=64,"an agent did not produce 64 decisions")?;
        require(integer(agent,"stats_packets")?>0,"an agent never received server statistics")?;
        require(integer(agent,"spawns")?>0,"an agent never spawned")?;
        require(integer(status,"updated")?.saturating_sub(integer(agent,"last_seen")?)<=120,"an agent observation is stale")?;
    }
    require(integer(status,"last_update")?>=integer(run,"started")?,"no PPO update from this run")?;
    Ok(())
}
fn validate_restart(previous:&Json,current:&Json)->Result<()> {
    require(text(previous,"run_id")?!=text(current,"run_id")?,"restart did not create a new run")?;
    require(current.get("resumed")?==&Json::Bool(true),"restart did not report loading a checkpoint")?;
    for (before,after) in [("version","initial_version"),("trained_samples","initial_samples"),("optimizer_steps","initial_optimizer_steps")] {
        require(integer(previous,before)?==integer(current,after)?,"restart state counters do not match the preceding run")?;
    }
    require(text(previous,"fingerprint")?==text(current,"initial_fingerprint")?,"restart reset/changed the policy weights")
}
pub fn verify(root:&Path,previous:Option<&Path>)->Result<()> {
    let status=read(&root.join("state/status.json"))?;
    let run=read(&root.join(".runtime/run.json"))?;
    let ready=read(&root.join(".runtime/server-ready.json"))?;
    validate_status(&status,&run,&ready)?;
    require(fs::read_to_string(root.join(".runtime/last-exit.txt")).map_err(|e|e.to_string())?.trim()=="clean shutdown","no clean shutdown recorded")?;
    for name in ["supervisor.pid","java.pid","bots.pid"] {
        require(!root.join(".runtime").join(name).exists(),"run still has an owned PID marker")?;
    }
    let cp=Bundle::load(&root.join("state/training.bcmc"),integer(&run,"bots")? as usize,1).map_err(|e|format!("checkpoint: {e}"))?.checkpoint;
    require(cp.model.version==integer(&status,"version")? && cp.samples==integer(&status,"trained_samples")? && cp.adam.step==integer(&status,"optimizer_steps")?,"checkpoint and final status disagree")?;
    require(format!("{:016x}",cp.model.fingerprint())==text(&status,"fingerprint")?,"checkpoint weights and status disagree")?;
    if let Some(path)=previous {validate_restart(&read(path)?,&status)?;}
    println!("PASS: run {}: {} agents, {} new PPO updates, checkpoint valid, clean shutdown{}.",
        text(&status,"run_id")?,status.get("agents")?.array()?.len(),
        integer(&status,"version")?-integer(&status,"initial_version")?,
        if previous.is_some(){", exact prior policy/optimizer counters restored"}else{""});
    println!("This validates recorded execution/learning lifecycle, NOT survival, crafting, construction or cooperation.");
    Ok(())
}
#[cfg(test)] mod tests {
    use super::*;
    fn fixtures()->(Json,Json,Json) {
        let status=json::parse(r#"{"schema":2,"run_id":"r1","mode":"train","error":"","version":4,"initial_version":2,"trained_samples":256,"initial_samples":128,"optimizer_steps":12,"initial_optimizer_steps":6,"fingerprint":"bb","initial_fingerprint":"aa","updated":200,"last_update":190,"agents":[{"id":0,"steps":128,"stats_packets":4,"spawns":1,"last_seen":199}]}"#).unwrap();
        let run=json::parse(r#"{"run_id":"r1","bots":1,"started":100}"#).unwrap();
        let ready=json::parse(r#"{"run_id":"r1","protocol":774}"#).unwrap();(status,run,ready)
    }
    fn replace(j:&mut Json,key:&str,value:Json){if let Json::Object(m)=j{m.insert(key.into(),value);}}
    #[test] fn acceptance_fixture_checks_are_not_integration_evidence(){let (s,r,h)=fixtures();assert!(validate_status(&s,&r,&h).is_ok());}
    #[test] fn rejects_previous_run_status(){let (mut s,r,h)=fixtures();replace(&mut s,"run_id",Json::String("old".into()));assert!(validate_status(&s,&r,&h).is_err());}
    #[test] fn rejects_unchanged_optimizer(){let (mut s,r,h)=fixtures();replace(&mut s,"optimizer_steps",Json::Number("6".into()));assert!(validate_status(&s,&r,&h).is_err());}
    #[test] fn rejects_unchanged_weights(){let (mut s,r,h)=fixtures();replace(&mut s,"fingerprint",Json::String("aa".into()));assert!(validate_status(&s,&r,&h).is_err());}
    #[test] fn rejects_missing_population(){let (mut s,r,h)=fixtures();replace(&mut s,"agents",Json::Array(vec![]));assert!(validate_status(&s,&r,&h).is_err());}
    #[test] fn rejects_reset_restart(){let (s,_,_)=fixtures();let mut current=s.clone();replace(&mut current,"run_id",Json::String("r2".into()));replace(&mut current,"resumed",Json::Bool(true));assert!(validate_restart(&s,&current).is_err());}
}
